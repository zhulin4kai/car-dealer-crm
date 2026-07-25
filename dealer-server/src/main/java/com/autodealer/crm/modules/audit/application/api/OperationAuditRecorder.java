package com.autodealer.crm.modules.audit.application.api;

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.autodealer.crm.modules.audit.persistence.mapper.TOperationLogMapper;
import com.autodealer.crm.modules.audit.persistence.model.TOperationLog;
import com.autodealer.crm.modules.identity.application.api.security.CurrentUserProvider;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * 操作审计记录器，业务模块 Service 调用审计的统一入口。
 *
 * <p>模块 Agent 只能通过本 Recorder 记录审计，不得直接依赖
 * {@link TOperationLogMapper}。
 *
 * <p>本组件不声明 {@code @Transactional}，审计写入在调用方 Service 的事务内执行，
 * 确保业务回滚时审计也回滚。
 *
 * <p>审计操作者从 {@link CurrentUserProvider} 获取，IP 从当前请求
 * {@link ServletRequestAttributes} 提取，禁止业务模块自行传入操作者或 IP。
 */
@Component
public class OperationAuditRecorder {

    private static final Logger log = LoggerFactory.getLogger(OperationAuditRecorder.class);
    private static final int DETAIL_MAX_LENGTH = 2048;
    private static final ObjectMapper AUDIT_OBJECT_MAPPER = new ObjectMapper();

    private final CurrentUserProvider currentUserProvider;
    private final TOperationLogMapper tOperationLogMapper;
    private final AuditRequestIdProvider requestIdProvider;

    /**
     * 构造器注入。
     *
     * @param currentUserProvider 当前用户提供者
     * @param tOperationLogMapper 操作日志 Mapper
     */
    @Autowired
    public OperationAuditRecorder(CurrentUserProvider currentUserProvider,
                                  TOperationLogMapper tOperationLogMapper,
                                  AuditRequestIdProvider requestIdProvider) {
        this.currentUserProvider = currentUserProvider;
        this.tOperationLogMapper = tOperationLogMapper;
        this.requestIdProvider = requestIdProvider;
    }

    /** 兼容不启动 Spring 的单元测试。生产注入始终使用统一 Provider。 */
    OperationAuditRecorder(CurrentUserProvider currentUserProvider,
                           TOperationLogMapper tOperationLogMapper) {
        this(currentUserProvider, tOperationLogMapper, new AuditRequestIdProvider());
    }

    /**
     * 记录一条成功审计（无额外摘要）。
     *
     * @param action     审计动作枚举
     * @param resourceId 业务资源 ID
     */
    public void record(AuditActionEnum action, String resourceId) {
        record(action, resourceId, "SUCCESS", null);
    }

    /**
     * 记录一条审计，显式指定结果和结构化摘要。
     *
     * <p>调用方仍应只传最小必要摘要；本方法会再次清理密码、哈希、Token、
     * 完整手机号和完整邮箱，并在清理后执行统一长度限制。
     *
     * @param action     审计动作枚举
     * @param resourceId 业务资源 ID
     * @param result     结果标识，通常为 "SUCCESS" 或 "FAILURE"
     * @param summary    结构化 JSON 摘要（可空），不序列化完整请求体和敏感字段
     */
    public void record(AuditActionEnum action, String resourceId, String result, String summary) {
        TOperationLog logEntry = buildLogEntry(action, resourceId, result, summary,
                requestIdProvider.currentRequestId());
        insert(logEntry, action, resourceId);
    }

    /** 公开凭证入口没有登录主体，但仍需记录不含凭证明文的安全审计。 */
    public void recordAnonymous(AuditActionEnum action, String resourceId, String result, String summary) {
        TOperationLog logEntry = buildBaseLogEntry(action, resourceId, result, summary,
                requestIdProvider.currentRequestId());
        logEntry.setUserId(null);
        logEntry.setUserName("ANONYMOUS_CREDENTIAL_FLOW");
        insert(logEntry, action, resourceId);
    }

    /** 安全失败事件只在 IP 列保存不可逆来源摘要，禁止落原始 remoteAddr。 */
    public void recordAnonymousSecurityFailure(AuditActionEnum action, String resourceId, String result,
                                               String summary, String sourceDigest) {
        TOperationLog logEntry = buildBaseLogEntry(action, resourceId, result, summary,
                requestIdProvider.currentRequestId());
        logEntry.setIp(sourceDigest);
        logEntry.setUserId(null);
        logEntry.setUserName("ANONYMOUS_CREDENTIAL_FLOW");
        insert(logEntry, action, resourceId);
    }

    /**
     * 记录已认证主体触发、但执行阶段 SecurityContext 已被框架清除的安全动作。
     *
     * <p>当前仅用于 Spring Security LogoutSuccessHandler 链路。操作者 ID 与名称必须来自
     * 数据库重新读取的用户事实，不能直接使用请求参数。
     */
    public void recordAuthenticatedActor(AuditActionEnum action, String resourceId, String result,
                                         String summary, Integer actorUserId, String actorUserName) {
        if (actorUserId == null || actorUserName == null || actorUserName.isBlank()) {
            throw new IllegalArgumentException("已认证审计主体不能为空");
        }
        TOperationLog logEntry = buildBaseLogEntry(action, resourceId, result, summary,
                requestIdProvider.currentRequestId());
        logEntry.setUserId(actorUserId);
        logEntry.setUserName(AuditSensitiveDataSanitizer.sanitize(actorUserName));
        insert(logEntry, action, resourceId);
    }

    public void recordAuthenticatedSecurityFailure(AuditActionEnum action, String resourceId, String result,
                                                   String summary, Integer actorUserId, String actorUserName,
                                                   String sourceDigest) {
        if (actorUserId == null || actorUserName == null || actorUserName.isBlank()) {
            throw new IllegalArgumentException("已认证审计主体不能为空");
        }
        TOperationLog logEntry = buildBaseLogEntry(action, resourceId, result, summary,
                requestIdProvider.currentRequestId());
        logEntry.setIp(sourceDigest);
        logEntry.setUserId(actorUserId);
        logEntry.setUserName(AuditSensitiveDataSanitizer.sanitize(actorUserName));
        insert(logEntry, action, resourceId);
    }

    public void recordAnonymousQuietly(AuditActionEnum action, String resourceId,
                                       String result, String summary) {
        try {
            recordAnonymous(action, resourceId, result, summary);
        } catch (Exception exception) {
            log.warn("匿名审计记录静默失败 actionCode={} resourceId={} result={}",
                    action.getActionCode(), resourceId, result, exception);
        }
    }

    private void insert(TOperationLog logEntry, AuditActionEnum action, String resourceId) {
        int rows = tOperationLogMapper.insert(logEntry);
        if (rows != 1) {
            throw new IllegalStateException("审计记录写入失败，影响行数: " + rows
                    + "，actionCode=" + action.getActionCode() + " resourceId=" + resourceId);
        }
    }

    /**
     * 记录一条审计，仅当插入失败时记录应用日志（非关键审计场景）。
     *
     * <p>仅用于非核心业务动作的辅助审计，失败不回滚业务事务，不抛出异常。
     *
     * @param action     审计动作枚举
     * @param resourceId 业务资源 ID
     * @param result     结果标识
     * @param summary    结构化 JSON 摘要（可空）
     */
    public void recordQuietly(AuditActionEnum action, String resourceId, String result, String summary) {
        try {
            record(action, resourceId, result, summary);
        } catch (Exception e) {
            log.warn("审计记录静默失败 actionCode={} resourceId={} result={}",
                    action.getActionCode(), resourceId, result, e);
        }
    }

    private TOperationLog buildLogEntry(AuditActionEnum action, String resourceId,
                                        String result, String summary, String requestId) {
        TOperationLog logEntry = buildBaseLogEntry(action, resourceId, result, summary, requestId);
        Integer userId = currentUserProvider.getCurrentUserId();
        String userName = currentUserProvider.getCurrentUser().getName();
        logEntry.setUserId(userId);
        logEntry.setUserName(userName);
        return logEntry;
    }

    private TOperationLog buildBaseLogEntry(AuditActionEnum action, String resourceId,
                                            String result, String summary, String requestId) {
        TOperationLog logEntry = new TOperationLog();
        logEntry.setActionCode(action.getActionCode());
        logEntry.setModuleName(action.getModuleName());
        logEntry.setObjectType(resolveObjectType(action));
        logEntry.setResourceId(AuditSensitiveDataSanitizer.sanitize(resourceId));
        logEntry.setResult(normalizeResult(result));
        logEntry.setDetail(buildDetail(logEntry.getResult(), summary));
        logEntry.setIp(extractClientIp());
        logEntry.setRequestId(requestId);
        logEntry.setCreateTime(new Date());
        return logEntry;
    }

    private String resolveObjectType(AuditActionEnum action) {
        String actionCode = action.getActionCode();
        int index = actionCode.indexOf('_');
        return index > 0 ? actionCode.substring(0, index) : actionCode;
    }

    private String normalizeResult(String result) {
        return result == null || result.isBlank() ? "SUCCESS" : result;
    }

    private String buildDetail(String result, String summary) {
        ObjectNode detail = AUDIT_OBJECT_MAPPER.createObjectNode();
        detail.put("result", result == null ? "" : result);
        if (summary != null && !summary.isEmpty()) {
            String sanitized = AuditSensitiveDataSanitizer.sanitize(summary);
            try {
                JsonNode summaryNode = AUDIT_OBJECT_MAPPER.readTree(sanitized);
                detail.set("summary", summaryNode == null ? AUDIT_OBJECT_MAPPER.nullNode() : summaryNode);
            } catch (JacksonException e) {
                detail.put("summary", sanitized);
            }
        }
        String serialized = serializeDetail(detail);
        if (serialized.length() <= DETAIL_MAX_LENGTH) return serialized;

        // 禁止直接截断 JSON；超长摘要降级为可反序列化的最小元数据。
        ObjectNode truncated = AUDIT_OBJECT_MAPPER.createObjectNode();
        truncated.put("result", result == null ? "" : result);
        truncated.put("summary", "[TRUNCATED]");
        truncated.put("originalLength", serialized.length());
        return serializeDetail(truncated);
    }

    private String serializeDetail(ObjectNode detail) {
        try {
            return AUDIT_OBJECT_MAPPER.writeValueAsString(detail);
        } catch (JacksonException e) {
            throw new IllegalStateException("审计明细序列化失败", e);
        }
    }

    private String extractClientIp() {
        try {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                String ip = attributes.getRequest().getRemoteAddr();
                return ip != null ? ip : "unknown";
            }
        } catch (Exception e) {
            log.debug("获取请求 IP 失败", e);
        }
        return "unknown";
    }

}
