package com.autodealer.crm.ai.service.impl;

import com.autodealer.crm.ai.ToolDefinition;
import com.autodealer.crm.ai.ToolRegistry;
import com.autodealer.crm.ai.dto.AiConversationDetailResponse;
import com.autodealer.crm.ai.dto.AiConversationResponse;
import com.autodealer.crm.ai.dto.AiConversationTurnResponse;
import com.autodealer.crm.ai.dto.AiCreateRunCommand;
import com.autodealer.crm.ai.dto.AiExecutionEventCommand;
import com.autodealer.crm.ai.dto.AiMessageCommand;
import com.autodealer.crm.ai.dto.AiRunResponse;
import com.autodealer.crm.ai.dto.AiRunTraceResponse;
import com.autodealer.crm.ai.dto.AiSseEventResponse;
import com.autodealer.crm.ai.dto.CancelAiRunRequest;
import com.autodealer.crm.ai.dto.CreateAiConversationRequest;
import com.autodealer.crm.ai.dto.CreateAiRunRequest;
import com.autodealer.crm.ai.dto.DealerAiEventResponse;
import com.autodealer.crm.ai.dto.DealerAiMessageHistory;
import com.autodealer.crm.ai.dto.DealerAiRunRequest;
import com.autodealer.crm.ai.dto.RenameAiConversationRequest;
import com.autodealer.crm.ai.enums.AiConversationStatus;
import com.autodealer.crm.ai.enums.AiEntryPoint;
import com.autodealer.crm.ai.enums.AiMessageRole;
import com.autodealer.crm.ai.enums.AiResultStatus;
import com.autodealer.crm.ai.enums.AiRunStatus;
import com.autodealer.crm.ai.enums.AiWorkflowStatus;
import com.autodealer.crm.ai.enums.AiWorkflowStepStatus;
import com.autodealer.crm.ai.mapper.TAiWorkflowMapper;
import com.autodealer.crm.ai.mapper.TAiWorkflowStepMapper;
import com.autodealer.crm.ai.model.TAiConversation;
import com.autodealer.crm.ai.model.TAiMessage;
import com.autodealer.crm.ai.model.TAiRun;
import com.autodealer.crm.ai.model.TAiWorkflow;
import com.autodealer.crm.ai.model.TAiWorkflowStep;
import com.autodealer.crm.ai.service.AiConversationService;
import com.autodealer.crm.ai.service.AiProviderConfigService;
import com.autodealer.crm.ai.service.AiRunCancellationRegistry;
import com.autodealer.crm.ai.service.AiRunCancellationToken;
import com.autodealer.crm.ai.service.AiSensitiveDataSanitizer;
import com.autodealer.crm.ai.service.AiTraceService;
import com.autodealer.crm.ai.service.DealerAiClient;
import com.autodealer.crm.exception.BusinessException;
import com.autodealer.crm.result.CodeEnum;
import org.springframework.stereotype.Service;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

@Service
public class AiConversationServiceImpl implements AiConversationService {
    private static final long SSE_TIMEOUT_MS = 60_000L;
    private static final int RECENT_MESSAGE_LIMIT = 8;
    private static final Set<String> TERMINAL_RUN_STATUSES = Set.of(
            AiRunStatus.COMPLETED.name(),
            AiRunStatus.FAILED.name(),
            AiRunStatus.CANCELLED.name(),
            AiRunStatus.EXPIRED.name());
    private static final Set<String> SENSITIVE_PAYLOAD_KEYS = Set.of(
            "apiKey",
            "api_key",
            "providerRuntimeConfig",
            "provider_runtime_config",
            "encryptedApiKey",
            "encrypted_api_key",
            "authorization",
            "x-api-key");

    private final AiTraceService traceService;
    private final DealerAiClient dealerAiClient;
    private final AiProviderConfigService providerConfigService;
    private final AiRunCancellationRegistry cancellationRegistry;
    private final AiSensitiveDataSanitizer sanitizer;
    private final ToolRegistry toolRegistry;
    private final TAiWorkflowMapper workflowMapper;
    private final TAiWorkflowStepMapper workflowStepMapper;

    public AiConversationServiceImpl(AiTraceService traceService,
                                     DealerAiClient dealerAiClient,
                                     AiProviderConfigService providerConfigService,
                                     AiRunCancellationRegistry cancellationRegistry,
                                     AiSensitiveDataSanitizer sanitizer,
                                     ToolRegistry toolRegistry,
                                     TAiWorkflowMapper workflowMapper,
                                     TAiWorkflowStepMapper workflowStepMapper) {
        this.traceService = traceService;
        this.dealerAiClient = dealerAiClient;
        this.providerConfigService = providerConfigService;
        this.cancellationRegistry = cancellationRegistry;
        this.sanitizer = sanitizer;
        this.toolRegistry = toolRegistry;
        this.workflowMapper = workflowMapper;
        this.workflowStepMapper = workflowStepMapper;
    }

    @Override
    public List<AiConversationResponse> listConversations(boolean includeArchived) {
        return traceService.listOwnedConversations(includeArchived).stream()
                .map(AiConversationResponse::from)
                .toList();
    }

    @Override
    public AiConversationResponse createConversation(CreateAiConversationRequest request) {
        AiEntryPoint entryPoint = AiEntryPoint.valueOf(request.getEntryPoint());
        TAiConversation conversation = traceService.createConversation(entryPoint,
                request.getContextObjectType(),
                request.getContextObjectId(),
                request.getTitle());
        return AiConversationResponse.from(conversation);
    }

    @Override
    public AiConversationDetailResponse getConversation(String conversationNo) {
        TAiConversation conversation = traceService.getOwnedConversation(conversationNo);
        AiConversationDetailResponse response = new AiConversationDetailResponse();
        response.setConversation(AiConversationResponse.from(conversation));
        response.setMessages(traceService.listConversationMessages(conversation.getId()).stream()
                .map(AiRunTraceResponse.MessageTrace::from)
                .toList());
        List<AiRunTraceResponse> runTraces = traceService.listRunsByConversationId(conversation.getId()).stream()
                .map(traceService::getRunTrace)
                .toList();
        response.setTurns(runTraces.stream()
                .map(AiConversationTurnResponse::from)
                .toList());
        TAiRun latestRun = traceService.getLatestRunByConversationId(conversation.getId());
        if (latestRun != null) {
            response.setLatestRun(AiRunResponse.from(latestRun));
            response.setLatestRunTrace(runTraces.stream()
                    .filter(trace -> latestRun.getRunNo().equals(trace.getRun().getRunNo()))
                    .findFirst()
                    .orElseGet(() -> traceService.getRunTrace(latestRun)));
        }
        return response;
    }

    @Override
    public AiConversationResponse renameConversation(String conversationNo, RenameAiConversationRequest request) {
        return AiConversationResponse.from(traceService.renameConversation(conversationNo, request.getTitle()));
    }

    @Override
    public AiConversationResponse archiveConversation(String conversationNo) {
        return AiConversationResponse.from(traceService.archiveConversation(conversationNo));
    }

    @Override
    public AiRunResponse createRun(CreateAiRunRequest request) {
        AiEntryPoint entryPoint = AiEntryPoint.valueOf(request.getEntryPoint());
        TAiConversation conversation = resolveConversation(request, entryPoint);
        if (AiConversationStatus.ARCHIVED.name().equals(conversation.getStatus())) {
            throw new BusinessException(CodeEnum.AI_CONVERSATION_ARCHIVED, "AI 会话已归档，不能继续发送消息");
        }
        TAiRun parentRun = traceService.getLatestRunByConversationId(conversation.getId());
        String contextObjectType = StringUtils.hasText(conversation.getContextObjectType())
                ? conversation.getContextObjectType() : request.getContextObjectType();
        String contextObjectId = StringUtils.hasText(conversation.getContextObjectId())
                ? conversation.getContextObjectId() : request.getContextObjectId();
        TAiRun run = traceService.createRun(new AiCreateRunCommand(
                conversation.getId(),
                parentRun == null ? null : parentRun.getId(),
                traceService.nextTurnNo(conversation.getId()),
                entryPoint,
                contextObjectType,
                contextObjectId,
                request.getPrompt(),
                LocalDateTime.now().plusHours(1)));
        run.setConversationNo(conversation.getConversationNo());
        traceService.appendMessage(new AiMessageCommand(
                conversation.getId(),
                run.getId(),
                AiMessageRole.USER,
                1,
                true,
                request.getPrompt()));
        updateConversationSummary(conversation.getId(), run.getRunNo(), "用户问题：", request.getPrompt());
        return AiRunResponse.from(run);
    }

    @Override
    public AiRunResponse getRun(String runNo) {
        return AiRunResponse.from(traceService.getOwnedRun(runNo));
    }

    @Override
    public AiRunTraceResponse getRunTrace(String runNo) {
        return traceService.getOwnedRunTrace(runNo);
    }

    @Override
    public SseEmitter streamRun(String runNo) {
        TAiRun run = traceService.getOwnedRun(runNo);
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        AiRunCancellationToken token = cancellationRegistry.register(runNo);
        emitter.onCompletion(token::cancel);
        emitter.onTimeout(token::cancel);
        emitter.onError(error -> token.cancel());
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CompletableFuture.runAsync(() -> runWithSecurityContext(authentication, () -> streamRunInternal(run, emitter, token)));
        return emitter;
    }

    @Override
    public AiRunResponse cancelRun(String runNo, CancelAiRunRequest request) {
        TAiRun run = traceService.getOwnedRun(runNo);
        if (!TERMINAL_RUN_STATUSES.contains(run.getStatus())
                && !AiRunStatus.WAITING_FOR_APPROVAL.name().equals(run.getStatus())) {
            traceService.cancelRunIfCancellable(run.getId(), request == null ? null : request.getReason());
        }
        cancellationRegistry.cancel(runNo);
        return AiRunResponse.from(traceService.getOwnedRun(runNo));
    }

    private void streamRunInternal(TAiRun run, SseEmitter emitter, AiRunCancellationToken token) {
        StreamRunState state = new StreamRunState();
        try {
            if (TERMINAL_RUN_STATUSES.contains(run.getStatus())) {
                sendCancelledIfNeeded(emitter, run);
                emitter.complete();
                return;
            }
            traceService.updateRunStatusIfNotTerminal(run.getId(), AiRunStatus.RUNNING, null, null);
            dealerAiClient.streamRunEvents(toDealerAiRequest(run), event -> {
                if (token.isCancelled()) {
                    return;
                }
                state.lastSequence = event.getSequence() == null ? state.lastSequence : event.getSequence();
                state.waitingForApproval = sendEvent(emitter, run, event, state) || state.waitingForApproval;
            }, token);
            if (token.isCancelled() || isRunCancelled(run.getId())) {
                savePartialAssistant(run, state);
                traceService.cancelRunIfCancellable(run.getId(), "用户停止生成");
                sendCancelledIfNeeded(emitter, run);
                emitter.complete();
                return;
            }
            savePartialAssistant(run, state);
            traceService.updateRunStatusIfNotTerminal(run.getId(),
                    state.waitingForApproval ? AiRunStatus.WAITING_FOR_APPROVAL : AiRunStatus.COMPLETED,
                    null, null);
            emitter.complete();
        } catch (BusinessException ex) {
            if (token.isCancelled() || isRunCancelled(run.getId())) {
                savePartialAssistant(run, state);
                traceService.cancelRunIfCancellable(run.getId(), "用户停止生成");
                sendCancelledIfNeeded(emitter, run);
                emitter.complete();
                return;
            }
            traceService.updateRunStatusIfNotTerminal(run.getId(), AiRunStatus.FAILED,
                    ex.getCodeEnum().name(), ex.getMessage());
            sendError(emitter, run, ex.getCodeEnum().name(), ex.getMessage());
            emitter.complete();
        } catch (RuntimeException ex) {
            if (token.isCancelled() || isRunCancelled(run.getId())) {
                savePartialAssistant(run, state);
                traceService.cancelRunIfCancellable(run.getId(), "用户停止生成");
                sendCancelledIfNeeded(emitter, run);
                emitter.complete();
                return;
            }
            traceService.updateRunStatusIfNotTerminal(run.getId(), AiRunStatus.FAILED,
                    CodeEnum.AI_SSE_FAILED.name(), "AI 事件连接失败");
            sendError(emitter, run, CodeEnum.AI_SSE_FAILED.name(), "AI 事件连接失败");
            emitter.complete();
        } finally {
            cancellationRegistry.unregister(run.getRunNo(), token);
        }
    }

    private void runWithSecurityContext(Authentication authentication, Runnable action) {
        SecurityContext previousContext = SecurityContextHolder.getContext();
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        try {
            SecurityContextHolder.setContext(context);
            action.run();
        } finally {
            SecurityContextHolder.setContext(previousContext);
        }
    }

    private DealerAiRunRequest toDealerAiRequest(TAiRun run) {
        DealerAiRunRequest request = new DealerAiRunRequest();
        request.setRunId(run.getRunNo());
        request.setUserPrompt(run.getPromptSummary());
        if (run.getConversationId() != null) {
            TAiConversation conversation = traceService.getConversationById(run.getConversationId());
            request.setConversationNo(conversation.getConversationNo());
            request.setConversationSummary(conversation.getSummaryText());
            request.setMessageHistory(traceService.listRecentVisibleMessages(
                            conversation.getId(), run.getId(), RECENT_MESSAGE_LIMIT).stream()
                    .map(this::toMessageHistory)
                    .toList());
        } else {
            request.setMessageHistory(List.of());
        }
        if (StringUtils.hasText(run.getContextObjectType()) && StringUtils.hasText(run.getContextObjectId())) {
            request.setContext(Map.of(
                    "object_type", run.getContextObjectType(),
                    "object_id", run.getContextObjectId()));
        }
        request.setToolSchemas(toolRegistry.definitions().stream()
                .map(this::toToolSchema)
                .toList());
        request.setAllowProposals(true);
        request.setProviderRuntimeConfig(providerConfigService.getEnabledRuntimeConfig());
        return request;
    }

    private Map<String, Object> toToolSchema(ToolDefinition definition) {
        Map<String, Object> schema = new HashMap<>();
        schema.put("name", definition.name());
        schema.put("description", definition.description());
        schema.put("permission_code", definition.permissionCode());
        schema.put("risk_level", definition.riskLevel().name());
        schema.put("requires_confirmation", definition.requiresConfirmation());
        schema.put("read_only", definition.readOnly());
        schema.put("max_results", definition.maxResults());
        schema.put("input_schema", definition.inputSchema());
        return schema;
    }

    private boolean sendEvent(SseEmitter emitter, TAiRun run, DealerAiEventResponse event, StreamRunState state) {
        Map<String, Object> payload = filterPayload(event.getPayload());
        AiSseEventResponse response = new AiSseEventResponse();
        response.setEventId(sanitizer.sanitize(event.getEventId(), 64));
        response.setRunNo(run.getRunNo());
        response.setSequence(event.getSequence());
        response.setType(sanitizer.sanitize(event.getEventType(), 64));
        response.setOccurredAt(event.getOccurredAt() == null
                ? LocalDateTime.now() : event.getOccurredAt().toLocalDateTime());
        response.setPayload(payload);
        recordEventTrace(run, response.getType(), response.getSequence(), payload, state);
        try {
            emitter.send(SseEmitter.event()
                    .id(response.getEventId())
                    .name(response.getType())
                    .data(response));
        } catch (IOException ex) {
            throw new BusinessException(CodeEnum.AI_SSE_FAILED, "AI 事件发送失败", ex);
        }
        return "proposal_created".equals(response.getType());
    }

    private void sendError(SseEmitter emitter, TAiRun run, String code, String message) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("code", sanitizer.sanitize(code, 64));
        payload.put("message", sanitizer.sanitize(message, 255));
        DealerAiEventResponse event = new DealerAiEventResponse();
        event.setEventId("error-" + System.nanoTime());
        event.setRunId(run.getRunNo());
        event.setSequence(0);
        event.setEventType("error");
        event.setPayload(payload);
        sendEvent(emitter, run, event, new StreamRunState());
    }

    private void recordEventTrace(TAiRun run,
                                  String eventType,
                                  Integer sequence,
                                  Map<String, Object> payload,
                                  StreamRunState state) {
        if ("message_delta".equals(eventType)) {
            Object delta = payload.getOrDefault("content_delta", payload.get("content"));
            if (delta instanceof String text && !text.isBlank()) {
                state.assistantBuffer.append(text);
            }
            return;
        }
        if ("message_completed".equals(eventType)) {
            Object content = payload.get("content");
            if (state.assistantBuffer.isEmpty() && content instanceof String text && !text.isBlank()) {
                state.assistantBuffer.append(text);
            }
            if (!state.assistantSaved && !state.assistantBuffer.isEmpty()) {
                traceService.appendMessage(new AiMessageCommand(
                        run.getConversationId(),
                        run.getId(),
                        AiMessageRole.ASSISTANT,
                        assistantSequence(sequence, state),
                        true,
                        state.assistantBuffer.toString()));
                if (run.getConversationId() != null) {
                    updateConversationSummary(run.getConversationId(), run.getRunNo(),
                            "AI 回答：", state.assistantBuffer.toString());
                }
                state.assistantSaved = true;
            }
            return;
        }
        if ("proposal_created".equals(eventType)) {
            traceService.updateRunStatus(run.getId(), AiRunStatus.WAITING_FOR_APPROVAL, null, null);
            return;
        }
        if (eventType != null && eventType.startsWith("workflow_")) {
            persistWorkflowEvent(run, eventType, payload);
            traceService.recordExecutionEvent(new AiExecutionEventCommand(
                    run.getId(),
                    null,
                    eventType,
                    "workflow_failed".equals(eventType) ? AiResultStatus.FAILED : AiResultStatus.SUCCESS,
                    "AI_WORKFLOW",
                    String.valueOf(payload.getOrDefault("workflowNo", run.getRunNo())),
                    String.valueOf(payload.getOrDefault("title", eventType)),
                    payload.get("errorCode") == null ? null : String.valueOf(payload.get("errorCode")),
                    LocalDateTime.now()));
        }
    }

    private void persistWorkflowEvent(TAiRun run, String eventType, Map<String, Object> payload) {
        if ("workflow_started".equals(eventType)) {
            upsertWorkflow(run, payload, AiWorkflowStatus.RUNNING);
            return;
        }
        TAiWorkflow workflow = workflowMapper.selectByWorkflowNo(text(payload, "workflowNo", run.getRunNo()));
        if (workflow == null) {
            return;
        }
        if ("workflow_step_started".equals(eventType)) {
            upsertWorkflowStep(workflow, payload, AiWorkflowStepStatus.RUNNING);
            updateWorkflowStatus(workflow, AiWorkflowStatus.RUNNING, number(payload, "stepNo"),
                    null, null, null);
            return;
        }
        if ("workflow_step_completed".equals(eventType)) {
            updateWorkflowStep(workflow, payload, statusFromPayload(payload, AiWorkflowStepStatus.COMPLETED));
            return;
        }
        if ("workflow_waiting_user_confirmation".equals(eventType)) {
            updateWorkflowStatus(workflow, AiWorkflowStatus.WAITING_USER_CONFIRMATION,
                    number(payload, "stepNo"), null, null, null);
            upsertWorkflowStep(workflow, payload, AiWorkflowStepStatus.WAITING_USER_CONFIRMATION);
            return;
        }
        if ("workflow_cancelled".equals(eventType)) {
            updateWorkflowStatus(workflow, AiWorkflowStatus.CANCELLED,
                    workflow.getCurrentStepNo(), null, null, null);
            workflowStepMapper.updateUnfinishedByWorkflowId(workflow.getId(),
                    AiWorkflowStepStatus.CANCELLED.name(), null, run.getUserId());
            return;
        }
        if ("workflow_failed".equals(eventType)) {
            updateWorkflowStatus(workflow, AiWorkflowStatus.FAILED, workflow.getCurrentStepNo(),
                    null, text(payload, "errorCode", CodeEnum.AI_WORKFLOW_STATE_CONFLICT.name()),
                    text(payload, "message", "AI 工作流执行失败"));
            workflowStepMapper.updateUnfinishedByWorkflowId(workflow.getId(),
                    AiWorkflowStepStatus.FAILED.name(), CodeEnum.AI_WORKFLOW_STATE_CONFLICT.name(), run.getUserId());
            return;
        }
        if ("workflow_completed".equals(eventType)) {
            updateWorkflowStatus(workflow, AiWorkflowStatus.COMPLETED,
                    workflow.getCurrentStepNo(), null, null, null);
            workflowStepMapper.updateUnfinishedByWorkflowId(workflow.getId(),
                    AiWorkflowStepStatus.COMPLETED.name(), null, run.getUserId());
        }
    }

    private void upsertWorkflow(TAiRun run, Map<String, Object> payload, AiWorkflowStatus status) {
        String workflowNo = text(payload, "workflowNo", run.getRunNo());
        if (workflowMapper.selectByWorkflowNo(workflowNo) != null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        TAiWorkflow workflow = new TAiWorkflow();
        workflow.setWorkflowNo(workflowNo);
        workflow.setRunId(run.getId());
        workflow.setUserId(run.getUserId());
        workflow.setWorkflowType(text(payload, "workflowType", "CUSTOMER_FOLLOW_UP"));
        workflow.setTitle(text(payload, "title", "AI 受控工作流"));
        workflow.setStatus(status.name());
        workflow.setCurrentStepNo(number(payload, "stepNo"));
        workflow.setContextObjectType(run.getContextObjectType());
        workflow.setContextObjectId(run.getContextObjectId());
        workflow.setStartedTime(now);
        workflow.setExpiresTime(run.getExpiresTime());
        workflow.setCreateTime(now);
        workflow.setCreateBy(run.getUserId());
        workflow.setEditTime(now);
        workflow.setEditBy(run.getUserId());
        workflowMapper.insert(workflow);
    }

    private void upsertWorkflowStep(TAiWorkflow workflow,
                                    Map<String, Object> payload,
                                    AiWorkflowStepStatus status) {
        Integer stepNo = number(payload, "stepNo");
        if (stepNo == null) {
            return;
        }
        TAiWorkflowStep step = workflowStepMapper.selectByWorkflowIdAndStepNo(workflow.getId(), stepNo);
        if (step != null) {
            updateWorkflowStep(workflow, payload, status);
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        step = new TAiWorkflowStep();
        step.setWorkflowId(workflow.getId());
        step.setStepNo(stepNo);
        step.setStepType(text(payload, "stepType", "UNKNOWN"));
        step.setTitle(text(payload, "title", "AI 工作流步骤"));
        step.setStatus(status.name());
        step.setToolName(text(payload, "toolName", null));
        step.setProposalId(longNumber(payload, "proposalId"));
        step.setOutputSummary(text(payload, "outputSummary", null));
        step.setErrorCode(text(payload, "errorCode", null));
        step.setStartedTime(now);
        if (status == AiWorkflowStepStatus.COMPLETED
                || status == AiWorkflowStepStatus.FAILED
                || status == AiWorkflowStepStatus.CANCELLED
                || status == AiWorkflowStepStatus.EXPIRED) {
            step.setCompletedTime(now);
        }
        step.setCreateTime(now);
        step.setCreateBy(workflow.getUserId());
        step.setEditTime(now);
        step.setEditBy(workflow.getUserId());
        workflowStepMapper.insert(step);
    }

    private void updateWorkflowStep(TAiWorkflow workflow,
                                    Map<String, Object> payload,
                                    AiWorkflowStepStatus status) {
        Integer stepNo = number(payload, "stepNo");
        if (stepNo == null) {
            return;
        }
        TAiWorkflowStep step = workflowStepMapper.selectByWorkflowIdAndStepNo(workflow.getId(), stepNo);
        if (step == null) {
            upsertWorkflowStep(workflow, payload, status);
            return;
        }
        workflowStepMapper.updateStepStatus(step.getId(), status.name(), longNumber(payload, "proposalId"),
                text(payload, "outputSummary", null), text(payload, "errorCode", null), workflow.getUserId());
    }

    private void updateWorkflowStatus(TAiWorkflow workflow,
                                      AiWorkflowStatus status,
                                      Integer currentStepNo,
                                      String pauseReason,
                                      String errorCode,
                                      String errorMessage) {
        workflowMapper.updateStatus(workflow.getId(), status.name(), currentStepNo,
                sanitizer.sanitize(pauseReason, 500),
                sanitizer.sanitize(errorCode, 64),
                sanitizer.sanitize(errorMessage, 255),
                workflow.getUserId());
    }

    private AiWorkflowStepStatus statusFromPayload(Map<String, Object> payload, AiWorkflowStepStatus fallback) {
        String status = text(payload, "status", fallback.name());
        try {
            return AiWorkflowStepStatus.valueOf(status);
        } catch (RuntimeException ex) {
            return fallback;
        }
    }

    private String text(Map<String, Object> payload, String key, String fallback) {
        Object value = payload.get(key);
        if (value == null) {
            return fallback;
        }
        return sanitizer.sanitize(String.valueOf(value), 500);
    }

    private Integer number(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            try {
                return Integer.valueOf(text);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private Long longNumber(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            try {
                return Long.valueOf(text);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private Map<String, Object> filterPayload(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> filtered = new HashMap<>();
        payload.forEach((key, value) -> {
            String safeKey = sanitizer.sanitize(key, 64);
            if (SENSITIVE_PAYLOAD_KEYS.contains(safeKey)
                    || SENSITIVE_PAYLOAD_KEYS.contains(safeKey.toLowerCase())) {
                return;
            }
            Object safeValue = value instanceof String text ? sanitizer.sanitizeDisplayText(text, 1000) : value;
            filtered.put(safeKey, safeValue);
        });
        return filtered;
    }

    private void savePartialAssistant(TAiRun run, StreamRunState state) {
        if (state.assistantSaved || state.assistantBuffer.isEmpty()) {
            return;
        }
        traceService.appendMessage(new AiMessageCommand(
                run.getConversationId(),
                run.getId(),
                AiMessageRole.ASSISTANT,
                assistantSequence(null, state),
                true,
                state.assistantBuffer.toString()));
        if (run.getConversationId() != null) {
            updateConversationSummary(run.getConversationId(), run.getRunNo(),
                    "AI 回答：", state.assistantBuffer.toString());
        }
        state.assistantSaved = true;
    }

    private int assistantSequence(Integer sequence, StreamRunState state) {
        int candidate = sequence == null ? state.lastSequence + 1 : sequence;
        return Math.max(2, candidate);
    }

    private boolean isRunCancelled(Long runId) {
        return AiRunStatus.CANCELLED.name().equals(traceService.getRunById(runId).getStatus());
    }

    private TAiConversation resolveConversation(CreateAiRunRequest request, AiEntryPoint entryPoint) {
        if (StringUtils.hasText(request.getConversationNo())) {
            return traceService.getOwnedConversation(request.getConversationNo());
        }
        return traceService.findOrCreateConversation(entryPoint,
                request.getContextObjectType(),
                request.getContextObjectId(),
                request.getPrompt());
    }

    private void updateConversationSummary(Long conversationId, String runNo, String label, String content) {
        if (conversationId == null) {
            return;
        }
        TAiConversation conversation = traceService.getConversationById(conversationId);
        String currentSummary = conversation.getSummaryText();
        String safeCurrent = StringUtils.hasText(currentSummary) ? sanitizer.sanitize(currentSummary, 1400) : "";
        String safeContent = sanitizer.sanitize(content, 500);
        String nextSummary = StringUtils.hasText(safeCurrent)
                ? safeCurrent + "\n" + label + safeContent
                : label + safeContent;
        traceService.updateConversationAfterRun(conversationId, runNo, sanitizer.sanitize(nextSummary, 2000));
    }

    private DealerAiMessageHistory toMessageHistory(TAiMessage message) {
        DealerAiMessageHistory history = new DealerAiMessageHistory();
        String role = switch (message.getRole() == null ? "" : message.getRole()) {
            case "USER" -> "user";
            case "ASSISTANT" -> "assistant";
            default -> "system";
        };
        history.setRole(role);
        history.setContentSummary(message.getContentSummary());
        return history;
    }

    private void sendCancelledIfNeeded(SseEmitter emitter, TAiRun run) {
        DealerAiEventResponse event = new DealerAiEventResponse();
        event.setEventId("cancelled-" + System.nanoTime());
        event.setRunId(run.getRunNo());
        event.setSequence(0);
        event.setEventType("run_cancelled");
        event.setPayload(Map.of("status", AiRunStatus.CANCELLED.name(), "message", "已停止生成"));
        try {
            sendEvent(emitter, run, event, new StreamRunState());
        } catch (BusinessException ignored) {
            // 客户端主动断开时不把取消事件发送失败转成 Run 失败。
        }
    }

    private static final class StreamRunState {
        private final StringBuilder assistantBuffer = new StringBuilder();
        private boolean assistantSaved;
        private boolean waitingForApproval;
        private int lastSequence = 1;
    }
}
