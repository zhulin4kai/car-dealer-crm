package com.autodealer.crm.audit;

import com.autodealer.crm.config.security.CurrentUserProvider;
import com.autodealer.crm.mapper.TOperationLogMapper;
import com.autodealer.crm.model.TOperationLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Date;
import java.util.UUID;

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
    private static final int DETAIL_MAX_LENGTH = 512;

    private final CurrentUserProvider currentUserProvider;
    private final TOperationLogMapper tOperationLogMapper;

    /**
     * 构造器注入。
     *
     * @param currentUserProvider 当前用户提供者
     * @param tOperationLogMapper 操作日志 Mapper
     */
    public OperationAuditRecorder(CurrentUserProvider currentUserProvider,
                                  TOperationLogMapper tOperationLogMapper) {
        this.currentUserProvider = currentUserProvider;
        this.tOperationLogMapper = tOperationLogMapper;
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
     * <p>摘要必须预先脱敏，不得包含密码、JWT、手机号全值、Token 等敏感信息。
     * 本方法仅做长度截断，不修改调用方传入的摘要内容。
     *
     * @param action     审计动作枚举
     * @param resourceId 业务资源 ID
     * @param result     结果标识，通常为 "SUCCESS" 或 "FAILURE"
     * @param summary    结构化 JSON 摘要（可空），不序列化完整请求体和敏感字段
     */
    public void record(AuditActionEnum action, String resourceId, String result, String summary) {
        TOperationLog logEntry = buildLogEntry(action, resourceId, result, summary);
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
                                        String result, String summary) {
        TOperationLog logEntry = new TOperationLog();
        Integer userId = currentUserProvider.getCurrentUserId();
        String userName = currentUserProvider.getCurrentUser().getName();
        logEntry.setUserId(userId);
        logEntry.setUserName(userName);
        logEntry.setActionCode(action.getActionCode());
        logEntry.setModuleName(action.getModuleName());
        logEntry.setObjectType(resolveObjectType(action));
        logEntry.setResourceId(resourceId);
        logEntry.setResult(normalizeResult(result));
        logEntry.setDetail(buildDetail(logEntry.getResult(), summary));
        logEntry.setIp(extractClientIp());
        logEntry.setRequestId(resolveRequestId());
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
        StringBuilder sb = new StringBuilder();
        sb.append("{\"result\":\"").append(escapeJson(result)).append("\"");
        if (summary != null && !summary.isEmpty()) {
            sb.append(",\"summary\":").append(summary);
        }
        sb.append("}");
        String detail = sb.toString();
        return detail.length() <= DETAIL_MAX_LENGTH ? detail : detail.substring(0, DETAIL_MAX_LENGTH);
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
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

    private String resolveRequestId() {
        try {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                String requestId = attributes.getRequest().getHeader("X-Request-Id");
                if (requestId != null && !requestId.isBlank()) {
                    return requestId.length() <= 64 ? requestId : requestId.substring(0, 64);
                }
            }
        } catch (Exception e) {
            log.debug("获取请求标识失败", e);
        }
        return UUID.randomUUID().toString();
    }
}
