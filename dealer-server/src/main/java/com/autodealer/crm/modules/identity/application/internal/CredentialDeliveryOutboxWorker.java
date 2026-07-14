package com.autodealer.crm.modules.identity.application.internal;

import com.autodealer.crm.modules.fulfillment.delivery.application.api.enums.DeliveryStatus;
import com.autodealer.crm.modules.identity.application.api.security.CredentialDerivationCodec;

import com.autodealer.crm.modules.audit.application.api.AuditActionEnum;
import com.autodealer.crm.modules.audit.application.api.OperationAuditRecorder;
import com.autodealer.crm.modules.identity.application.api.enums.CredentialStatus;
import com.autodealer.crm.modules.identity.application.api.enums.AccountType;
import com.autodealer.crm.modules.identity.persistence.mapper.TAccountCredentialMapper;
import com.autodealer.crm.modules.identity.persistence.mapper.TCredentialDeliveryOutboxMapper;
import com.autodealer.crm.modules.identity.persistence.mapper.TEmployeeMapper;
import com.autodealer.crm.modules.identity.persistence.mapper.TUserMapper;
import com.autodealer.crm.modules.identity.persistence.model.TAccountCredential;
import com.autodealer.crm.modules.identity.persistence.model.TCredentialDeliveryOutbox;
import com.autodealer.crm.modules.identity.persistence.model.TEmployee;
import com.autodealer.crm.modules.identity.application.api.model.TUser;
import com.autodealer.crm.modules.identity.application.api.CredentialDeliveryPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.List;

/** 在业务事务提交后派生并投递凭证；所有重试复用稳定 messageId 和同一原始凭证。 */
@Component
@Profile("!test & !smoke")
public class CredentialDeliveryOutboxWorker {
    private final TCredentialDeliveryOutboxMapper outbox;
    private final TAccountCredentialMapper credentials;
    private final TUserMapper users;
    private final TEmployeeMapper employees;
    private final CredentialDeliveryPort delivery;
    private final CredentialDerivationCodec codec;
    private final CredentialTokenDigester tokenDigester;
    private final OperationAuditRecorder audit;
    private final TransactionTemplate transactions;
    private final int batchSize;
    private final int maxAttempts;
    private final int leaseSeconds;

    public CredentialDeliveryOutboxWorker(TCredentialDeliveryOutboxMapper outbox,
                                          TAccountCredentialMapper credentials,
                                          TUserMapper users,TEmployeeMapper employees,
                                          CredentialDeliveryPort delivery,CredentialDerivationCodec codec,
                                          CredentialTokenDigester tokenDigester,
                                          OperationAuditRecorder audit,PlatformTransactionManager transactionManager,
                                          @Value("${security.credential-delivery.outbox.batch-size:20}") int batchSize,
                                          @Value("${security.credential-delivery.outbox.max-attempts:8}") int maxAttempts,
                                          @Value("${security.credential-delivery.outbox.lease-seconds:120}") int leaseSeconds) {
        this.outbox=outbox;this.credentials=credentials;this.users=users;this.employees=employees;
        this.delivery=delivery;this.codec=codec;this.tokenDigester=tokenDigester;this.audit=audit;
        this.transactions=new TransactionTemplate(transactionManager);
        this.batchSize=Math.max(1,Math.min(batchSize,100));this.maxAttempts=Math.max(1,maxAttempts);
        this.leaseSeconds=Math.max(30,leaseSeconds);
    }

    @Scheduled(fixedDelayString="${security.credential-delivery.outbox.poll-delay-ms:1000}")
    public void processOnce() {
        LocalDateTime now=LocalDateTime.now();
        List<TCredentialDeliveryOutbox> due=outbox.selectDue(now,now.minusSeconds(leaseSeconds),batchSize);
        for(TCredentialDeliveryOutbox candidate:due) process(candidate);
    }

    private void process(TCredentialDeliveryOutbox candidate) {
        LocalDateTime claimedAt=LocalDateTime.now();
        Boolean claimed=transactions.execute(status->outbox.claimByIdAndVersion(
                candidate.getId(),candidate.getVersion(),claimedAt)==1);
        if(!Boolean.TRUE.equals(claimed))return;
        int claimedVersion=candidate.getVersion()+1;
        int attempt=candidate.getAttemptCount()+1;
        try {
            TAccountCredential credential=credentials.selectById(candidate.getCredentialId());
            if(!deliverable(credential,LocalDateTime.now())) {
                fail(candidate,claimedVersion,"CREDENTIAL_NOT_DELIVERABLE");return;
            }
            TUser user=users.selectByPrimaryKey(candidate.getUserId());
            if(user==null){fail(candidate,claimedVersion,"USER_NOT_FOUND");return;}
            TEmployee employee=employees.selectByUserId(candidate.getUserId());
            boolean human=user.getAccountType()==AccountType.HUMAN;
            String phone=human?matchingContact("PHONE",candidate.getPhoneDigest(),employee==null?null:employee.getPhone())
                    :matchingContact("PHONE",candidate.getPhoneDigest(),user.getPhone());
            String email=human?matchingContact("EMAIL",candidate.getEmailDigest(),employee==null?null:employee.getEmail())
                    :matchingContact("EMAIL",candidate.getEmailDigest(),user.getEmail());
            if(phone==null&&email==null){fail(candidate,claimedVersion,"DELIVERY_CONTACT_CHANGED");return;}
            String raw=codec.derive(candidate.getMessageId(),candidate.getPurpose(),candidate.getDerivationNonce());
            String tokenDigest=tokenDigester.digest(raw);
            if(!bindTokenDigest(candidate,credential,tokenDigest)){
                fail(candidate,claimedVersion,"TOKEN_DIGEST_COMMITMENT_MISMATCH");return;
            }
            CredentialDeliveryPort.DeliveryStatus result=delivery.deliver(new CredentialDeliveryPort.DeliveryMessage(
                    candidate.getMessageId(),candidate.getUserId(),user.getLoginAct(),phone,email,
                    candidate.getPurpose(),raw,credential.getExpiresAt()));
            if(deliveryAccepted(result.code()))complete(candidate,claimedVersion,attempt,result.code());
            else if("WEBHOOK_PERMANENT_REJECTED".equals(result.code())
                    ||"NO_DELIVERY_CONTACT".equals(result.code())
                    ||"CHANNEL_NOT_CONFIGURED".equals(result.code()))fail(candidate,claimedVersion,result.code());
            else retryOrFail(candidate,claimedVersion,attempt,result.code());
        }catch(RuntimeException exception){retryOrFail(candidate,claimedVersion,attempt,"DELIVERY_EXCEPTION");}
    }

    private boolean bindTokenDigest(TCredentialDeliveryOutbox message,TAccountCredential credential,String tokenDigest){
        String commitment=codec.deliveryCommitment(message.getMessageId(),message.getPurpose(),message.getDerivationNonce());
        if(constantTimeEquals(credential.getTokenDigest(),tokenDigest))return true;
        if(!constantTimeEquals(credential.getTokenDigest(),commitment))return false;
        LocalDateTime now=LocalDateTime.now();
        Boolean bound=transactions.execute(status->credentials.bindTokenDigest(
                credential.getId(),commitment,tokenDigest,now)==1);
        if(Boolean.TRUE.equals(bound))return true;
        TAccountCredential latest=credentials.selectById(credential.getId());
        return deliverable(latest,now)&&constantTimeEquals(latest.getTokenDigest(),tokenDigest);
    }

    private boolean deliverable(TAccountCredential credential,LocalDateTime now){
        return credential!=null&&credential.getStatus()==CredentialStatus.ISSUED
                &&Boolean.TRUE.equals(credential.getActiveMarker())
                &&credential.getExpiresAt()!=null&&credential.getExpiresAt().isAfter(now);
    }

    private boolean constantTimeEquals(String left,String right){
        return left!=null&&right!=null&&MessageDigest.isEqual(left.getBytes(StandardCharsets.US_ASCII),
                right.getBytes(StandardCharsets.US_ASCII));
    }

    private String matchingContact(String channel,String expectedDigest,String...candidates){
        if(expectedDigest==null)return null;
        for(String candidate:candidates)if(candidate!=null&&expectedDigest.equals(codec.contactDigest(channel,candidate)))return candidate;
        return null;
    }
    private boolean deliveryAccepted(String code){return "CAPTURED".equals(code)||"WEBHOOK_DELIVERED".equals(code)
            ||"EMAIL_SENT".equals(code)||"SMS_SENT".equals(code);}

    private void complete(TCredentialDeliveryOutbox message,int version,int attempt,String code){
        LocalDateTime now=LocalDateTime.now();
        transactions.executeWithoutResult(status->{
            if(outbox.markDelivered(message.getId(),version,now)!=1)throw new IllegalStateException("凭证投递完成状态冲突");
            audit.recordAnonymous(AuditActionEnum.USER_CREDENTIAL_DELIVERY_SUCCESS,String.valueOf(message.getUserId()),"SUCCESS",
                    detail(message,attempt,code));
        });
    }
    private void retryOrFail(TCredentialDeliveryOutbox message,int version,int attempt,String code){
        if(attempt>=maxAttempts){fail(message,version,"MAX_ATTEMPTS_"+code);return;}
        long delaySeconds=Math.min(3600L,30L*(1L<<Math.min(attempt-1,7)));
        LocalDateTime now=LocalDateTime.now();
        transactions.executeWithoutResult(status->{if(outbox.markRetry(message.getId(),version,now.plusSeconds(delaySeconds),code,now)!=1)
            throw new IllegalStateException("凭证投递重试状态冲突");});
    }
    private void fail(TCredentialDeliveryOutbox message,int version,String code){
        LocalDateTime now=LocalDateTime.now();
        transactions.executeWithoutResult(status->{
            credentials.revokeIssuedById(message.getCredentialId(),now);
            if(outbox.markFailed(message.getId(),version,now,code)!=1)throw new IllegalStateException("凭证投递失败状态冲突");
            audit.recordAnonymous(AuditActionEnum.USER_CREDENTIAL_DELIVERY_FAILURE,String.valueOf(message.getUserId()),"FAILED",
                    detail(message,message.getAttemptCount()+1,code));
        });
    }
    private String detail(TCredentialDeliveryOutbox message,int attempt,String code){return "{\"messageId\":\""+
            message.getMessageId()+"\",\"purpose\":\""+message.getPurpose().name()+"\",\"attempt\":"+attempt+
            ",\"status\":\""+code+"\"}";}
}
