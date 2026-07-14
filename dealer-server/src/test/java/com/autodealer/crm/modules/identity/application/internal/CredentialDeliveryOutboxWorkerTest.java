package com.autodealer.crm.modules.identity.application.internal;

import com.autodealer.crm.modules.fulfillment.delivery.application.api.enums.DeliveryStatus;
import com.autodealer.crm.modules.identity.application.api.CredentialDeliveryPort;
import com.autodealer.crm.modules.identity.application.api.model.TUser;
import com.autodealer.crm.modules.identity.persistence.mapper.TAccountCredentialMapper;
import com.autodealer.crm.modules.identity.persistence.mapper.TCredentialDeliveryOutboxMapper;
import com.autodealer.crm.modules.identity.persistence.mapper.TEmployeeMapper;
import com.autodealer.crm.modules.identity.persistence.mapper.TUserMapper;
import com.autodealer.crm.modules.identity.persistence.model.TAccountCredential;
import com.autodealer.crm.modules.identity.persistence.model.TCredentialDeliveryOutbox;
import com.autodealer.crm.modules.identity.persistence.model.TEmployee;
import com.autodealer.crm.modules.identity.application.api.*;

import com.autodealer.crm.modules.audit.application.api.OperationAuditRecorder;
import com.autodealer.crm.modules.identity.application.api.enums.CredentialPurpose;
import com.autodealer.crm.modules.identity.application.api.enums.CredentialStatus;
import com.autodealer.crm.modules.identity.application.api.enums.AccountType;
import com.autodealer.crm.modules.identity.persistence.mapper.*;
import com.autodealer.crm.modules.identity.persistence.model.*;
import com.autodealer.crm.modules.identity.application.api.model.*;
import com.autodealer.crm.modules.identity.application.internal.CredentialDeliveryOutboxWorker;
import com.autodealer.crm.modules.identity.application.api.security.CredentialDerivationCodec;
import com.autodealer.crm.modules.identity.application.internal.CredentialTokenDigester;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CredentialDeliveryOutboxWorkerTest {
    private static final String DERIVATION_KEY="test-only-credential-derivation-key-00000001";
    private static final String DIGEST_KEY="test-secret-key-for-unit-tests-only-2024";
    @Mock TCredentialDeliveryOutboxMapper outbox;@Mock TAccountCredentialMapper credentials;
    @Mock TUserMapper users;@Mock TEmployeeMapper employees;@Mock OperationAuditRecorder audit;
    @Mock PlatformTransactionManager transactionManager;

    @Test
    void committedMessageDerivesRawOnlyInWorkerAndUsesStableMessageId() {
        CredentialDerivationCodec codec=spy(new CredentialDerivationCodec(DERIVATION_KEY));
        CredentialTokenDigester tokenDigester=new CredentialTokenDigester(DIGEST_KEY);
        AtomicReference<CredentialDeliveryPort.DeliveryMessage> delivered=new AtomicReference<>();
        CredentialDeliveryPort port=message->{delivered.set(message);return new CredentialDeliveryPort.DeliveryStatus("CAPTURED");};
        when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        TCredentialDeliveryOutbox message=message(codec);when(outbox.selectDue(any(),any(),eq(20))).thenReturn(List.of(message));
        when(outbox.claimByIdAndVersion(eq(10L),eq(0),any())).thenReturn(1);when(outbox.markDelivered(eq(10L),eq(1),any())).thenReturn(1);
        TAccountCredential credential=new TAccountCredential();credential.setId(20L);credential.setStatus(CredentialStatus.ISSUED);
        credential.setActiveMarker(true);credential.setExpiresAt(LocalDateTime.now().plusHours(1));
        credential.setTokenDigest(codec.deliveryCommitment("message-2",CredentialPurpose.INVITATION,"nonce-2"));
        when(credentials.selectById(20L)).thenReturn(credential);
        when(credentials.bindTokenDigest(eq(20L),eq(credential.getTokenDigest()),anyString(),any())).thenReturn(1);
        TUser user=new TUser();user.setId(2);user.setLoginAct("user2");user.setPhone("13800000002");when(users.selectByPrimaryKey(2)).thenReturn(user);
        CredentialDeliveryOutboxWorker worker=new CredentialDeliveryOutboxWorker(outbox,credentials,users,employees,port,codec,tokenDigester,audit,
                transactionManager,20,8,120);

        worker.processOnce();

        assertNotNull(delivered.get());assertEquals("message-2",delivered.get().messageId());
        assertEquals(new CredentialDerivationCodec(DERIVATION_KEY).derive(
                "message-2",CredentialPurpose.INVITATION,"nonce-2"),delivered.get().rawCredential());
        assertNotEquals(message.getDerivationNonce(),delivered.get().rawCredential());
        verify(credentials).bindTokenDigest(eq(20L),eq(credential.getTokenDigest()),
                eq(tokenDigester.digest(delivered.get().rawCredential())),any());
        verify(codec,times(1)).derive("message-2",CredentialPurpose.INVITATION,"nonce-2");
        verify(outbox).markDelivered(eq(10L),eq(1),any());verify(credentials,never()).revokeIssuedById(anyLong(),any());
    }

    @Test
    void transientFailureRetriesWithSameMessageIdAndRawCredential() {
        CredentialDerivationCodec codec=new CredentialDerivationCodec(DERIVATION_KEY);
        AtomicInteger calls=new AtomicInteger();
        AtomicReference<CredentialDeliveryPort.DeliveryMessage> first=new AtomicReference<>();
        AtomicReference<CredentialDeliveryPort.DeliveryMessage> second=new AtomicReference<>();
        CredentialDeliveryPort port=deliveryMessage->{
            if(calls.getAndIncrement()==0){first.set(deliveryMessage);throw new IllegalStateException("temporary");}
            second.set(deliveryMessage);return new CredentialDeliveryPort.DeliveryStatus("WEBHOOK_DELIVERED");
        };
        configureTransactionManager();
        TCredentialDeliveryOutbox pending=message(codec);
        TCredentialDeliveryOutbox retry=message(codec);retry.setStatus("RETRY");retry.setAttemptCount(1);retry.setVersion(2);
        when(outbox.selectDue(any(),any(),eq(20))).thenReturn(List.of(pending),List.of(retry));
        when(outbox.claimByIdAndVersion(eq(10L),eq(0),any())).thenReturn(1);
        when(outbox.claimByIdAndVersion(eq(10L),eq(2),any())).thenReturn(1);
        when(outbox.markRetry(eq(10L),eq(1),any(),eq("DELIVERY_EXCEPTION"),any())).thenReturn(1);
        when(outbox.markDelivered(eq(10L),eq(3),any())).thenReturn(1);
        TAccountCredential firstCredential=deliverableCredential(codec);
        TAccountCredential boundCredential=deliverableCredential(codec);
        boundCredential.setTokenDigest(new CredentialTokenDigester(DIGEST_KEY).digest(
                codec.derive("message-2",CredentialPurpose.INVITATION,"nonce-2")));
        when(credentials.selectById(20L)).thenReturn(firstCredential,boundCredential);
        when(credentials.bindTokenDigest(eq(20L),anyString(),anyString(),any())).thenReturn(1);
        stubDeliverableUser();
        CredentialDeliveryOutboxWorker worker=worker(port,codec,8);

        worker.processOnce();worker.processOnce();

        assertNotNull(first.get());assertNotNull(second.get());
        assertEquals(first.get().messageId(),second.get().messageId());
        assertEquals(first.get().rawCredential(),second.get().rawCredential());
        verify(credentials,times(1)).bindTokenDigest(eq(20L),anyString(),
                eq(new CredentialTokenDigester(DIGEST_KEY).digest(first.get().rawCredential())),any());
        verify(outbox).markRetry(eq(10L),eq(1),any(),eq("DELIVERY_EXCEPTION"),any());
        verify(outbox).markDelivered(eq(10L),eq(3),any());
    }

    @Test
    void permanentDeliveryFailureRevokesCredentialAndClearsOutboxThroughFailedTransition() {
        CredentialDerivationCodec codec=new CredentialDerivationCodec(DERIVATION_KEY);
        CredentialDeliveryPort port=message->new CredentialDeliveryPort.DeliveryStatus("WEBHOOK_PERMANENT_REJECTED");
        configureTransactionManager();
        TCredentialDeliveryOutbox message=message(codec);when(outbox.selectDue(any(),any(),eq(20))).thenReturn(List.of(message));
        when(outbox.claimByIdAndVersion(eq(10L),eq(0),any())).thenReturn(1);
        when(outbox.markFailed(eq(10L),eq(1),any(),eq("WEBHOOK_PERMANENT_REJECTED"))).thenReturn(1);
        stubDeliverableCredentialAndUser(codec);

        worker(port,codec,8).processOnce();

        verify(credentials).revokeIssuedById(eq(20L),any());
        verify(outbox).markFailed(eq(10L),eq(1),any(),eq("WEBHOOK_PERMANENT_REJECTED"));
        verify(outbox,never()).markRetry(anyLong(),anyInt(),any(),anyString(),any());
    }

    @Test
    void finalTransientAttemptFailsInsteadOfSchedulingAnotherRetry() {
        CredentialDerivationCodec codec=new CredentialDerivationCodec(DERIVATION_KEY);
        CredentialDeliveryPort port=message->{throw new IllegalStateException("still unavailable");};
        configureTransactionManager();
        TCredentialDeliveryOutbox message=message(codec);message.setAttemptCount(7);
        when(outbox.selectDue(any(),any(),eq(20))).thenReturn(List.of(message));
        when(outbox.claimByIdAndVersion(eq(10L),eq(0),any())).thenReturn(1);
        when(outbox.markFailed(eq(10L),eq(1),any(),eq("MAX_ATTEMPTS_DELIVERY_EXCEPTION"))).thenReturn(1);
        stubDeliverableCredentialAndUser(codec);

        worker(port,codec,8).processOnce();

        verify(credentials).revokeIssuedById(eq(20L),any());
        verify(outbox).markFailed(eq(10L),eq(1),any(),eq("MAX_ATTEMPTS_DELIVERY_EXCEPTION"));
        verify(outbox,never()).markRetry(anyLong(),anyInt(),any(),anyString(),any());
    }

    @Test
    void expiredProcessingLeaseCanBeClaimedAgainByVersion() {
        CredentialDerivationCodec codec=new CredentialDerivationCodec(DERIVATION_KEY);
        CredentialDeliveryPort port=message->new CredentialDeliveryPort.DeliveryStatus("CAPTURED");
        configureTransactionManager();
        TCredentialDeliveryOutbox leased=message(codec);leased.setStatus("PROCESSING");leased.setAttemptCount(2);leased.setVersion(4);
        when(outbox.selectDue(any(),any(),eq(20))).thenReturn(List.of(leased));
        when(outbox.claimByIdAndVersion(eq(10L),eq(4),any())).thenReturn(1);
        when(outbox.markDelivered(eq(10L),eq(5),any())).thenReturn(1);
        stubDeliverableCredentialAndUser(codec);

        worker(port,codec,8).processOnce();

        verify(outbox).claimByIdAndVersion(eq(10L),eq(4),any());
        verify(outbox).markDelivered(eq(10L),eq(5),any());
    }

    @Test
    void humanDeliveryNeverFallsBackToStaleUserContactProjection() {
        CredentialDerivationCodec codec=new CredentialDerivationCodec(DERIVATION_KEY);
        CredentialDeliveryPort port=mock(CredentialDeliveryPort.class);
        configureTransactionManager();
        TCredentialDeliveryOutbox message=message(codec);
        when(outbox.selectDue(any(),any(),eq(20))).thenReturn(List.of(message));
        when(outbox.claimByIdAndVersion(eq(10L),eq(0),any())).thenReturn(1);
        when(outbox.markFailed(eq(10L),eq(1),any(),eq("DELIVERY_CONTACT_CHANGED"))).thenReturn(1);
        TAccountCredential credential=new TAccountCredential();credential.setId(20L);credential.setStatus(CredentialStatus.ISSUED);
        credential.setActiveMarker(true);credential.setExpiresAt(LocalDateTime.now().plusHours(1));
        when(credentials.selectById(20L)).thenReturn(credential);
        TUser user=new TUser();user.setId(2);user.setLoginAct("user2");user.setAccountType(AccountType.HUMAN);
        user.setPhone("13800000002");when(users.selectByPrimaryKey(2)).thenReturn(user);
        TEmployee employee=new TEmployee();employee.setUserId(2);employee.setPhone("13800000003");
        when(employees.selectByUserId(2)).thenReturn(employee);

        worker(port,codec,8).processOnce();

        verify(port,never()).deliver(any());
        verify(credentials).revokeIssuedById(eq(20L),any());
        verify(outbox).markFailed(eq(10L),eq(1),any(),eq("DELIVERY_CONTACT_CHANGED"));
    }

    @Test
    void mismatchedPreDeliveryCommitmentFailsClosedWithoutExternalDelivery() {
        CredentialDerivationCodec codec=new CredentialDerivationCodec(DERIVATION_KEY);
        CredentialDeliveryPort port=mock(CredentialDeliveryPort.class);
        configureTransactionManager();
        TCredentialDeliveryOutbox message=message(codec);
        when(outbox.selectDue(any(),any(),eq(20))).thenReturn(List.of(message));
        when(outbox.claimByIdAndVersion(eq(10L),eq(0),any())).thenReturn(1);
        when(outbox.markFailed(eq(10L),eq(1),any(),eq("TOKEN_DIGEST_COMMITMENT_MISMATCH"))).thenReturn(1);
        TAccountCredential credential=deliverableCredential(codec);credential.setTokenDigest("f".repeat(64));
        when(credentials.selectById(20L)).thenReturn(credential);
        stubDeliverableUser();

        worker(port,codec,8).processOnce();

        verify(credentials,never()).bindTokenDigest(anyLong(),anyString(),anyString(),any());
        verify(credentials).revokeIssuedById(eq(20L),any());
        verify(outbox).markFailed(eq(10L),eq(1),any(),eq("TOKEN_DIGEST_COMMITMENT_MISMATCH"));
        verify(port,never()).deliver(any());
    }

    private CredentialDeliveryOutboxWorker worker(CredentialDeliveryPort port,CredentialDerivationCodec codec,int maxAttempts){
        return new CredentialDeliveryOutboxWorker(outbox,credentials,users,employees,port,codec,
                new CredentialTokenDigester(DIGEST_KEY),audit,
                transactionManager,20,maxAttempts,120);
    }

    private void configureTransactionManager(){
        when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
    }

    private void stubDeliverableCredentialAndUser(CredentialDerivationCodec codec){
        when(credentials.selectById(20L)).thenReturn(deliverableCredential(codec));
        when(credentials.bindTokenDigest(eq(20L),anyString(),anyString(),any())).thenReturn(1);
        stubDeliverableUser();
    }

    private TAccountCredential deliverableCredential(CredentialDerivationCodec codec){
        TAccountCredential credential=new TAccountCredential();credential.setId(20L);credential.setStatus(CredentialStatus.ISSUED);
        credential.setActiveMarker(true);credential.setExpiresAt(LocalDateTime.now().plusHours(1));
        credential.setTokenDigest(codec.deliveryCommitment("message-2",CredentialPurpose.INVITATION,"nonce-2"));
        return credential;
    }

    private void stubDeliverableUser(){
        TUser user=new TUser();user.setId(2);user.setLoginAct("user2");user.setAccountType(AccountType.HUMAN);user.setPhone("13800000002");
        when(users.selectByPrimaryKey(2)).thenReturn(user);
        TEmployee employee=new TEmployee();employee.setUserId(2);employee.setPhone("13800000002");
        when(employees.selectByUserId(2)).thenReturn(employee);
    }

    private TCredentialDeliveryOutbox message(CredentialDerivationCodec codec){
        TCredentialDeliveryOutbox value=new TCredentialDeliveryOutbox();value.setId(10L);value.setMessageId("message-2");
        value.setCredentialId(20L);value.setUserId(2);value.setPurpose(CredentialPurpose.INVITATION);
        value.setDerivationNonce("nonce-2");value.setPhoneDigest(codec.contactDigest("PHONE","13800000002"));
        value.setStatus("PENDING");value.setAttemptCount(0);value.setVersion(0);return value;
    }
}
