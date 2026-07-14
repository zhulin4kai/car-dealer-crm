package com.autodealer.crm.modules.identity.application.api;

import com.autodealer.crm.modules.audit.application.api.AuditActionEnum;
import com.autodealer.crm.modules.audit.application.api.AuditRequestIdProvider;
import com.autodealer.crm.modules.audit.application.api.AuditSensitiveDataSanitizer;
import com.autodealer.crm.modules.audit.application.api.OperationAuditRecorder;
import com.autodealer.crm.modules.identity.application.api.security.CurrentUserProvider;
import com.autodealer.crm.modules.identity.application.api.enums.AuthorizationChangeType;
import com.autodealer.crm.modules.identity.application.api.enums.AuthorizationSubjectType;
import com.autodealer.crm.modules.identity.application.api.enums.PermissionEffect;
import com.autodealer.crm.modules.identity.persistence.mapper.TAuthorizationHistoryMapper;
import com.autodealer.crm.modules.identity.persistence.model.TAuthorizationHistory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 在调用方业务事务内同时落授权历史和关键操作审计。
 *
 * <p>本组件不写授权事实，也不自行开启事务；授权 Service 必须先写业务事实，
 * 再在同一事务中调用本组件。任一历史或审计写入失败都会向调用方抛出异常。
 */
@Component
public class AuthorizationAuditRecorder {
    private final TAuthorizationHistoryMapper historyMapper;
    private final OperationAuditRecorder operationAuditRecorder;
    private final CurrentUserProvider currentUserProvider;
    private final Clock clock;
    private final AuditRequestIdProvider requestIdProvider;

    public AuthorizationAuditRecorder(TAuthorizationHistoryMapper historyMapper,
                                      OperationAuditRecorder operationAuditRecorder,
                                      CurrentUserProvider currentUserProvider,
                                      Clock clock,
                                      AuditRequestIdProvider requestIdProvider) {
        this.historyMapper = historyMapper;
        this.operationAuditRecorder = operationAuditRecorder;
        this.currentUserProvider = currentUserProvider;
        this.clock = clock;
        this.requestIdProvider = requestIdProvider;
    }

    public void record(TAuthorizationHistory history, AuditActionEnum action,
                       String resourceId, String summary) {
        recordAll(List.of(history), action, resourceId, summary);
    }

    public void recordAll(List<TAuthorizationHistory> histories, AuditActionEnum action,
                          String resourceId, String summary) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("授权历史和审计必须在业务事务内写入");
        }
        if (histories == null || histories.isEmpty()) {
            throw new IllegalArgumentException("授权历史不能为空");
        }
        Integer trustedOperatorId = currentUserProvider.getCurrentUserId();
        LocalDateTime trustedOccurredTime = LocalDateTime.now(clock);
        String trustedRequestId = requestIdProvider.currentRequestId();
        for (TAuthorizationHistory history : histories) {
            validateHistory(history);
            history.setSubjectId(AuditSensitiveDataSanitizer.sanitize(history.getSubjectId()));
            history.setBeforeValue(AuditSensitiveDataSanitizer.sanitize(history.getBeforeValue()));
            history.setAfterValue(AuditSensitiveDataSanitizer.sanitize(history.getAfterValue()));
            history.setReason(AuditSensitiveDataSanitizer.sanitize(history.getReason()));
            // 这三个字段属于审计信任边界，调用方传值一律覆盖。
            history.setOperatorId(trustedOperatorId);
            history.setOccurredTime(trustedOccurredTime);
            history.setRequestId(trustedRequestId);
            if (historyMapper.insert(history) != 1) {
                throw new IllegalStateException("授权历史写入失败");
            }
        }
        // 同一 HTTP 请求或事务内 provider 会复用同一可信 ID；保留公开入口也确保测试和故障注入覆盖强一致性。
        operationAuditRecorder.record(action, resourceId, "SUCCESS", summary);
    }

    private void validateHistory(TAuthorizationHistory history) {
        if (history == null || history.getSubjectType() == null || history.getChangeType() == null
                || history.getSubjectId() == null || history.getSubjectId().isBlank()) {
            throw new IllegalArgumentException("授权历史缺少主体类型、主体标识或变化类型");
        }
        AuthorizationSubjectType subjectType = history.getSubjectType();
        switch (subjectType) {
            case ROLE -> requireIds(history.getRoleId(), "角色历史缺少 roleId");
            case ROLE_PERMISSION -> {
                requireIds(history.getRoleId(), "角色权限历史缺少 roleId");
                requireIds(history.getPermissionId(), "角色权限历史缺少 permissionId");
            }
            case USER_ROLE -> {
                requireIds(history.getTargetUserId(), "用户角色历史缺少 targetUserId");
                requireIds(history.getRoleId(), "用户角色历史缺少 roleId");
            }
            case USER_PERMISSION -> validateUserPermissionHistory(history);
            case ORGANIZATION_UNIT, POSITION -> {
                // 目录主体使用稳定 subjectId；不存在目标用户外键。
            }
            case ORGANIZATION_ASSIGNMENT, REPORTING_RELATION -> {
                // 未绑定登录账号的员工也允许拥有组织历史，员工 ID 记录在 subjectId。
            }
        }
    }

    private void validateUserPermissionHistory(TAuthorizationHistory history) {
        requireIds(history.getTargetUserId(), "用户权限历史缺少 targetUserId");
        requireIds(history.getPermissionId(), "用户权限历史缺少 permissionId");
        if (history.getEffect() == null) {
            throw new IllegalArgumentException("用户权限历史缺少 effect");
        }
        if (history.getChangeType() == AuthorizationChangeType.GRANT
                && history.getEffect() != PermissionEffect.GRANT) {
            throw new IllegalArgumentException("GRANT 历史必须使用 GRANT effect");
        }
        if (history.getChangeType() == AuthorizationChangeType.DENY
                && history.getEffect() != PermissionEffect.DENY) {
            throw new IllegalArgumentException("DENY 历史必须使用 DENY effect");
        }
        if (history.getEffect() == PermissionEffect.GRANT && history.getDataScopeCode() == null) {
            throw new IllegalArgumentException("GRANT 用户权限历史必须包含数据范围");
        }
        if (history.getEffect() == PermissionEffect.DENY && history.getDataScopeCode() != null) {
            throw new IllegalArgumentException("DENY 用户权限历史不得包含数据范围");
        }
    }

    private void requireIds(Integer id, String message) {
        if (id == null) throw new IllegalArgumentException(message);
    }

}
