package com.autodealer.crm.audit;

import com.autodealer.crm.model.TUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import com.autodealer.crm.service.impl.CredentialDerivationCodec;

/**
 * 使用独立事务保存安全拒绝事实，避免调用方随后抛错导致审计随业务事务回滚。
 * 调用方只能传不可逆资源摘要和枚举化摘要，禁止传原始凭证、恢复密钥或完整联系方式。
 */
@Service
public class SecurityFailureAuditService {
    private static final Logger log = LoggerFactory.getLogger(SecurityFailureAuditService.class);
    private final OperationAuditRecorder recorder;
    private final TransactionTemplate requiresNew;
    private final CredentialDerivationCodec digester;

    public SecurityFailureAuditService(OperationAuditRecorder recorder,
                                       PlatformTransactionManager transactionManager,
                                       CredentialDerivationCodec digester) {
        this.recorder = recorder;
        this.digester = digester;
        this.requiresNew = new TransactionTemplate(transactionManager);
        this.requiresNew.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public void recordAnonymous(AuditActionEnum action, String resourceDigest, String summary) {
        try {
            requiresNew.executeWithoutResult(status ->
                    recorder.recordAnonymousSecurityFailure(action, resourceDigest, "REJECTED", summary, sourceDigest()));
        } catch (RuntimeException exception) {
            log.error("安全失败审计写入失败 actionCode={} resourceDigest={}",
                    action.getActionCode(), resourceDigest, exception);
        }
    }

    public void recordAuthenticated(AuditActionEnum action, String resourceId,
                                    String summary, TUser actor) {
        if (actor == null || actor.getId() == null) {
            recordAnonymous(action, resourceId, summary);
            return;
        }
        String actorName = actor.getName();
        if (actorName == null || actorName.isBlank()) actorName = actor.getLoginAct();
        final String safeActorName = actorName == null || actorName.isBlank() ? "AUTHENTICATED_USER" : actorName;
        try {
            requiresNew.executeWithoutResult(status -> recorder.recordAuthenticatedSecurityFailure(
                    action, resourceId, "REJECTED", summary, actor.getId(), safeActorName, sourceDigest()));
        } catch (RuntimeException exception) {
            log.error("安全失败审计写入失败 actionCode={} resourceId={} actorUserId={}",
                    action.getActionCode(), resourceId, actor.getId(), exception);
        }
    }

    private String sourceDigest() {
        String source="INTERNAL";
        if(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes){
            String remote=attributes.getRequest().getRemoteAddr();if(remote!=null&&!remote.isBlank())source=remote;
        }
        return "hmac:"+digester.securityAuditSourceDigest(source).substring(0,32);
    }
}
