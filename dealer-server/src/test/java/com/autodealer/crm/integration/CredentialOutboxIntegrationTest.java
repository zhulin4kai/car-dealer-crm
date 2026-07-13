package com.autodealer.crm.integration;

import com.autodealer.crm.enums.CredentialPurpose;
import com.autodealer.crm.service.impl.CapturingCredentialDeliveryAdapter;
import com.autodealer.crm.service.impl.CredentialIssuanceService;
import com.autodealer.crm.mapper.TAccountCredentialMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import com.autodealer.crm.exception.BusinessException;
import com.autodealer.crm.result.CodeEnum;

import static org.junit.jupiter.api.Assertions.*;

class CredentialOutboxIntegrationTest extends BackendIntegrationTestBase {
    @Autowired CredentialIssuanceService issuance;
    @Autowired TAccountCredentialMapper credentialMapper;
    @Autowired CapturingCredentialDeliveryAdapter delivery;
    @Autowired PlatformTransactionManager transactionManager;

    @Test
    void rollbackBeforeCommitLeavesNeitherCredentialNorOutboxAndNeverDelivers() {
        String messageId=UUID.randomUUID().toString();String digest="a".repeat(64);
        delivery.clear();TransactionTemplate transaction=new TransactionTemplate(transactionManager);
        assertThrows(IllegalStateException.class,()->transaction.executeWithoutResult(status->{
            issuance.enqueue(2,CredentialPurpose.BREAK_GLASS,digest,messageId,"nonce-for-rollback",
                    "b".repeat(64),null,null,"rollback probe",null,null,
                    LocalDateTime.now(),Duration.ofMinutes(15),Duration.ZERO);
            throw new IllegalStateException("force rollback");
        }));
        assertEquals(0,jdbcTemplate.queryForObject("SELECT COUNT(*) FROM t_account_credential WHERE token_digest=?",Integer.class,digest));
        assertEquals(0,jdbcTemplate.queryForObject("SELECT COUNT(*) FROM t_credential_delivery_outbox WHERE message_id=?",Integer.class,messageId));
        assertNull(delivery.latestFor(2));
    }

    @Test
    void commitPersistsCommitmentAndPendingMessageWithoutCallingExternalDelivery() {
        String messageId=UUID.randomUUID().toString();String digest="c".repeat(64);delivery.clear();
        TransactionTemplate transaction=new TransactionTemplate(transactionManager);
        try{
            transaction.executeWithoutResult(status->issuance.enqueue(2,CredentialPurpose.BREAK_GLASS,digest,
                    messageId,"nonce-for-commit","d".repeat(64),null,null,"commit probe",null,null,
                    LocalDateTime.now(),Duration.ofMinutes(15),Duration.ZERO));
            assertEquals("PENDING",jdbcTemplate.queryForObject("SELECT status FROM t_credential_delivery_outbox WHERE message_id=?",String.class,messageId));
            assertEquals("nonce-for-commit",jdbcTemplate.queryForObject("SELECT derivation_nonce FROM t_credential_delivery_outbox WHERE message_id=?",String.class,messageId));
            assertEquals(64,jdbcTemplate.queryForObject("SELECT LENGTH(token_digest) FROM t_account_credential WHERE token_digest=?",Integer.class,digest));
            assertNull(delivery.latestFor(2));
        }finally{
            jdbcTemplate.update("DELETE FROM t_credential_delivery_outbox WHERE message_id=?",messageId);
            jdbcTemplate.update("DELETE FROM t_account_credential WHERE token_digest=?",digest);
        }
    }

    @Test
    void workerCanBindCommitmentToActualTokenDigestOnlyOnce() {
        String messageId=UUID.randomUUID().toString();String commitment="6".repeat(64);
        String tokenDigest="7".repeat(64);delivery.clear();
        try{
            issuance.enqueue(2,CredentialPurpose.BREAK_GLASS,commitment,messageId,"nonce-for-binding",
                    "8".repeat(64),null,null,"binding probe",null,null,
                    LocalDateTime.now(),Duration.ofMinutes(15),Duration.ZERO);
            Long credentialId=jdbcTemplate.queryForObject(
                    "SELECT credential_id FROM t_credential_delivery_outbox WHERE message_id=?",Long.class,messageId);

            assertEquals(1,credentialMapper.bindTokenDigest(credentialId,commitment,tokenDigest,LocalDateTime.now()));
            assertEquals(tokenDigest,jdbcTemplate.queryForObject(
                    "SELECT token_digest FROM t_account_credential WHERE id=?",String.class,credentialId));
            assertEquals(0,credentialMapper.bindTokenDigest(credentialId,commitment,"9".repeat(64),LocalDateTime.now()));
        }finally{
            jdbcTemplate.update("DELETE FROM t_credential_delivery_outbox WHERE message_id=?",messageId);
            jdbcTemplate.update("DELETE FROM t_account_credential WHERE token_digest IN (?,?)",commitment,tokenDigest);
        }
    }

    @Test
    void revokedCredentialStillKeepsPasswordResetFamilyInCooldown() {
        cleanupPurpose("SELF_RESET");cleanupPurpose("ADMIN_RESET");
        String firstMessage=UUID.randomUUID().toString();String secondMessage=UUID.randomUUID().toString();
        try{
            issuance.enqueue(2,CredentialPurpose.SELF_RESET,"e".repeat(64),firstMessage,"nonce-first",
                    "f".repeat(64),null,null,"first reset",null,null,LocalDateTime.now(),
                    Duration.ofHours(1),Duration.ofMinutes(5));
            jdbcTemplate.update("UPDATE t_account_credential SET status='REVOKED',active_marker=NULL WHERE token_digest=?","e".repeat(64));

            BusinessException error=assertThrows(BusinessException.class,()->issuance.enqueue(2,
                    CredentialPurpose.ADMIN_RESET,"1".repeat(64),secondMessage,"nonce-second","2".repeat(64),
                    null,null,"second reset",null,null,LocalDateTime.now(),Duration.ofHours(1),Duration.ofMinutes(5)));

            assertEquals(CodeEnum.CREDENTIAL_RATE_LIMITED,error.getCodeEnum());
            assertEquals(0,jdbcTemplate.queryForObject("SELECT COUNT(*) FROM t_credential_delivery_outbox WHERE message_id=?",Integer.class,secondMessage));
        }finally{cleanupPurpose("SELF_RESET");cleanupPurpose("ADMIN_RESET");}
    }

    @Test
    void concurrentVerificationIssuanceCreatesOnlyOneCredentialAndOutbox() throws Exception {
        cleanupPurpose("PHONE_VERIFY");AtomicInteger sequence=new AtomicInteger();
        ExecutorService pool=Executors.newFixedThreadPool(2);CyclicBarrier barrier=new CyclicBarrier(2);
        Callable<Boolean> command=()->{int n=sequence.incrementAndGet();barrier.await(5,TimeUnit.SECONDS);try{
            issuance.enqueue(2,CredentialPurpose.PHONE_VERIFY,String.valueOf(n).repeat(64),UUID.randomUUID().toString(),
                    "nonce-"+n,"a".repeat(64),null,2,"verify","b".repeat(64),0,LocalDateTime.now(),
                    Duration.ofMinutes(30),Duration.ofMinutes(5));return true;
        }catch(BusinessException exception){assertEquals(CodeEnum.CREDENTIAL_RATE_LIMITED,exception.getCodeEnum());return false;}};
        try{
            Future<Boolean> first=pool.submit(command);Future<Boolean> second=pool.submit(command);
            int successes=(first.get(10,TimeUnit.SECONDS)?1:0)+(second.get(10,TimeUnit.SECONDS)?1:0);
            assertEquals(1,successes);
            assertEquals(1,jdbcTemplate.queryForObject("SELECT COUNT(*) FROM t_account_credential WHERE user_id=2 AND purpose='PHONE_VERIFY'",Integer.class));
            assertEquals(1,jdbcTemplate.queryForObject("SELECT COUNT(*) FROM t_credential_delivery_outbox WHERE user_id=2 AND purpose='PHONE_VERIFY'",Integer.class));
        }finally{pool.shutdownNow();cleanupPurpose("PHONE_VERIFY");}
    }

    private void cleanupPurpose(String purpose){jdbcTemplate.update("DELETE FROM t_credential_delivery_outbox WHERE credential_id IN (SELECT id FROM t_account_credential WHERE user_id=2 AND purpose=?)",purpose);jdbcTemplate.update("DELETE FROM t_account_credential WHERE user_id=2 AND purpose=?",purpose);}
}
