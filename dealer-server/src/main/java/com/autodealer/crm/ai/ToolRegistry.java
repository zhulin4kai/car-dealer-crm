package com.autodealer.crm.ai;

import com.autodealer.crm.ai.dto.AiToolCallCommand;
import com.autodealer.crm.ai.enums.AiResultStatus;
import com.autodealer.crm.ai.service.AiSensitiveDataSanitizer;
import com.autodealer.crm.ai.service.AiTraceService;
import com.autodealer.crm.config.security.CurrentUserProvider;
import com.autodealer.crm.exception.BusinessException;
import com.autodealer.crm.result.CodeEnum;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ToolRegistry {
    private static final Set<String> TRUSTED_CONTEXT_FIELDS = Set.of(
            "userId", "user_id", "roles", "role",
            "permissions", "permission", "dataScope", "data_scope",
            "orgScope", "organizationScope", "createBy", "updateBy",
            "operatorId", "operator_id", "auditUserId", "audit_user_id");

    private final Map<String, ToolExecutor> executors;
    private final CurrentUserProvider currentUserProvider;
    private final AiTraceService traceService;
    private final AiSensitiveDataSanitizer sanitizer;
    private final ObjectMapper objectMapper;

    public ToolRegistry(List<ToolExecutor> executors,
                        CurrentUserProvider currentUserProvider,
                        AiTraceService traceService,
                        AiSensitiveDataSanitizer sanitizer,
                        ObjectMapper objectMapper) {
        this.executors = executors.stream()
                .collect(Collectors.toUnmodifiableMap(executor -> executor.definition().name(), Function.identity()));
        this.currentUserProvider = currentUserProvider;
        this.traceService = traceService;
        this.sanitizer = sanitizer;
        this.objectMapper = objectMapper;
    }

    public List<ToolDefinition> definitions() {
        return executors.values().stream()
                .map(ToolExecutor::definition)
                .sorted((left, right) -> left.name().compareTo(right.name()))
                .toList();
    }

    public List<ToolDefinition> definitionsForCurrentUser() {
        return definitions().stream()
                .filter(definition -> currentUserProvider.hasAuthority(definition.permissionCode()))
                .toList();
    }

    public ToolDefinition getDefinition(String toolName) {
        ToolExecutor executor = executors.get(toolName);
        if (executor == null) {
            throw new BusinessException(CodeEnum.AI_TOOL_NOT_FOUND, "AI 工具不存在");
        }
        return executor.definition();
    }

    public ToolExecutionResult execute(ToolExecutionContext context, String toolName, Map<String, Object> arguments) {
        ToolExecutor executor = executors.get(toolName);
        if (executor == null) {
            throw new BusinessException(CodeEnum.AI_TOOL_NOT_FOUND, "AI 工具不存在");
        }
        ToolDefinition definition = executor.definition();
        LocalDateTime startedTime = LocalDateTime.now();
        try {
            rejectTrustedContext(arguments);
            if (!currentUserProvider.hasAuthority(definition.permissionCode())) {
                throw new BusinessException(CodeEnum.AI_TOOL_FORBIDDEN, "AI 工具无权限");
            }
            ToolExecutionResult result = executor.execute(context, safeArguments(arguments));
            LocalDateTime completedTime = LocalDateTime.now();
            traceService.recordToolCall(new AiToolCallCommand(
                    context.runId(),
                    definition.name(),
                    definition.permissionCode(),
                    definition.riskLevel().toAiRiskLevel(),
                    toSummary(arguments, 1000),
                    sanitizer.sanitize(result.outputSummary(), 1000),
                    sanitizer.sanitize(result.objectRefs(), 1000),
                    toSummary(result.data(), 6000),
                    AiResultStatus.SUCCESS,
                    null,
                    durationMs(startedTime, completedTime),
                    startedTime,
                    completedTime));
            return result;
        } catch (BusinessException ex) {
            recordFailure(context, definition, arguments, startedTime, ex.getCodeEnum().name());
            throw ex;
        } catch (RuntimeException ex) {
            recordFailure(context, definition, arguments, startedTime, CodeEnum.AI_TOOL_ARGUMENT_INVALID.name());
            throw new BusinessException(CodeEnum.AI_TOOL_ARGUMENT_INVALID, "AI 工具参数错误", ex);
        }
    }

    private void rejectTrustedContext(Object arguments) {
        if (arguments == null) {
            return;
        }
        if (arguments instanceof Map<?, ?> map) {
            rejectTrustedContextMap(map);
            return;
        }
        if (arguments instanceof Iterable<?> iterable) {
            for (Object item : iterable) {
                rejectTrustedContext(item);
            }
        }
    }

    private void rejectTrustedContextMap(Map<?, ?> arguments) {
        for (Map.Entry<?, ?> entry : arguments.entrySet()) {
            Object key = entry.getKey();
            if (key instanceof String field && TRUSTED_CONTEXT_FIELDS.contains(field)) {
                throw new BusinessException(CodeEnum.AI_TOOL_ARGUMENT_INVALID, "AI 工具参数包含不可信上下文字段");
            }
            rejectTrustedContext(entry.getValue());
        }
    }

    private Map<String, Object> safeArguments(Map<String, Object> arguments) {
        if (arguments == null || arguments.isEmpty()) {
            return Map.of();
        }
        return new LinkedHashMap<>(arguments);
    }

    private void recordFailure(ToolExecutionContext context,
                               ToolDefinition definition,
                               Map<String, Object> arguments,
                               LocalDateTime startedTime,
                               String errorCode) {
        LocalDateTime completedTime = LocalDateTime.now();
        traceService.recordToolCall(new AiToolCallCommand(
                context.runId(),
                definition.name(),
                definition.permissionCode(),
                definition.riskLevel().toAiRiskLevel(),
                toSummary(arguments, 1000),
                null,
                null,
                null,
                AiResultStatus.FAILED,
                errorCode,
                durationMs(startedTime, completedTime),
                startedTime,
                completedTime));
    }

    private String toSummary(Object value, int maxLength) {
        try {
            return sanitizer.sanitize(objectMapper.writeValueAsString(value), maxLength);
        } catch (JsonProcessingException ex) {
            return sanitizer.sanitize(String.valueOf(value), maxLength);
        }
    }

    private Integer durationMs(LocalDateTime startedTime, LocalDateTime completedTime) {
        return Math.toIntExact(Duration.between(startedTime, completedTime).toMillis());
    }
}
