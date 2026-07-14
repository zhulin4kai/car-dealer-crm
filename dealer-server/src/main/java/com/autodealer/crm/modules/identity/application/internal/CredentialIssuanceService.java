package com.autodealer.crm.modules.identity.application.internal;

import com.autodealer.crm.modules.audit.application.api.AuditActionEnum;
import com.autodealer.crm.modules.audit.application.api.SecurityFailureAuditService;
import com.autodealer.crm.modules.identity.application.api.enums.CredentialPurpose;
import com.autodealer.crm.modules.identity.application.api.enums.CredentialStatus;
import com.autodealer.crm.shared.error.BusinessException;
import com.autodealer.crm.modules.identity.persistence.mapper.TAccountCredentialMapper;
import com.autodealer.crm.modules.identity.persistence.mapper.TCredentialDeliveryOutboxMapper;
import com.autodealer.crm.modules.identity.persistence.mapper.TUserMapper;
import com.autodealer.crm.modules.identity.persistence.model.TAccountCredential;
import com.autodealer.crm.modules.identity.persistence.model.TCredentialDeliveryOutbox;
import com.autodealer.crm.shared.error.CodeEnum;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/** 将预交付承诺与待投递消息原子入库，不派生原始凭证，也不执行任何网络调用。 */
@Service
public class CredentialIssuanceService {
    private final TAccountCredentialMapper credentials;
    private final TCredentialDeliveryOutboxMapper outbox;
    private final TUserMapper users;
    private final SecurityFailureAuditService failureAudit;

    public CredentialIssuanceService(TAccountCredentialMapper credentials,
                                     TCredentialDeliveryOutboxMapper outbox,TUserMapper users,
                                     SecurityFailureAuditService failureAudit) {
        this.credentials = credentials;
        this.outbox = outbox;
        this.users = users;
        this.failureAudit = failureAudit;
    }

    @Transactional(rollbackFor = Exception.class)
    public void enqueue(Integer userId, CredentialPurpose purpose, String deliveryCommitment,
                        String messageId, String nonce, String phoneDigest, String emailDigest,
                        Integer operator, String reason, String targetValueDigest,
                        Integer targetProfileVersion, LocalDateTime now, Duration lifetime,Duration cooldown) {
        if(users.selectByPrimaryKeyForUpdate(userId)==null)
            throw new BusinessException(CodeEnum.NOT_FOUND,"凭证目标用户不存在");
        if(cooldown!=null&&!cooldown.isZero()&&!cooldown.isNegative()){
            List<CredentialPurpose> family=(purpose==CredentialPurpose.SELF_RESET||purpose==CredentialPurpose.ADMIN_RESET)
                    ?List.of(CredentialPurpose.SELF_RESET,CredentialPurpose.ADMIN_RESET):List.of(purpose);
            TAccountCredential latest=credentials.selectLatestByUserAndPurposesForUpdate(userId,family);
            if(latest!=null&&latest.getCreateTime()!=null&&latest.getCreateTime().isAfter(now.minus(cooldown))){
                failureAudit.recordAnonymous(AuditActionEnum.USER_CREDENTIAL_RATE_LIMIT,String.valueOf(userId),
                        "{\"scope\":\"ISSUANCE_COOLDOWN\",\"purpose\":\""+purpose.name()+"\"}");
                throw new BusinessException(CodeEnum.CREDENTIAL_RATE_LIMITED,"凭证重发仍在冷却期");
            }
        }
        if (purpose == CredentialPurpose.SELF_RESET || purpose == CredentialPurpose.ADMIN_RESET) {
            credentials.revokeActive(userId, CredentialPurpose.SELF_RESET, now);
            credentials.revokeActive(userId, CredentialPurpose.ADMIN_RESET, now);
        } else {
            credentials.revokeActive(userId, purpose, now);
        }
        TAccountCredential credential = new TAccountCredential();
        credential.setUserId(userId);credential.setPurpose(purpose);credential.setTokenDigest(deliveryCommitment);
        credential.setStatus(CredentialStatus.ISSUED);credential.setActiveMarker(true);
        credential.setExpiresAt(now.plus(lifetime));credential.setIssuedBy(operator);credential.setReason(reason);
        credential.setTargetValueDigest(targetValueDigest);credential.setTargetProfileVersion(targetProfileVersion);
        credential.setVersion(0);credential.setCreateTime(now);
        if (credentials.insert(credential) != 1 || credential.getId() == null) {
            throw new BusinessException(CodeEnum.OPERATION_FAILED, "凭证摘要写入失败");
        }
        TCredentialDeliveryOutbox message = new TCredentialDeliveryOutbox();
        message.setMessageId(messageId);message.setCredentialId(credential.getId());message.setUserId(userId);
        message.setPurpose(purpose);message.setDerivationNonce(nonce);message.setPhoneDigest(phoneDigest);
        message.setEmailDigest(emailDigest);message.setStatus("PENDING");message.setAttemptCount(0);
        message.setNextAttemptAt(now);message.setVersion(0);message.setCreateTime(now);message.setEditTime(now);
        if (outbox.insert(message) != 1) {
            throw new BusinessException(CodeEnum.OPERATION_FAILED, "凭证投递消息写入失败");
        }
    }
}
