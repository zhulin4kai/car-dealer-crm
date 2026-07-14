package com.autodealer.crm.modules.ai.application.internal;

import com.autodealer.crm.modules.ai.application.api.AiAssistantPolicyService;
import com.autodealer.crm.modules.ai.application.api.AiProviderConfigService;
import com.autodealer.crm.modules.ai.application.api.AiTraceService;
import com.autodealer.crm.modules.ai.application.api.dto.AiAssistantPolicyResponse;
import com.autodealer.crm.modules.ai.application.api.tool.ToolDefinition;
import com.autodealer.crm.modules.ai.application.api.tool.ToolRegistry;
import com.autodealer.crm.modules.ai.application.api.tool.ToolRiskLevel;
import com.autodealer.crm.modules.ai.application.api.dto.AiExecutionEventCommand;
import com.autodealer.crm.modules.ai.application.api.dto.AiMessageCommand;
import com.autodealer.crm.modules.ai.application.api.dto.AiConversationDetailResponse;
import com.autodealer.crm.modules.ai.application.api.dto.AiRunResponse;
import com.autodealer.crm.modules.ai.application.api.dto.AiRunTraceResponse;
import com.autodealer.crm.modules.ai.application.api.dto.CreateAiRunRequest;
import com.autodealer.crm.modules.ai.application.api.dto.DealerAiEventResponse;
import com.autodealer.crm.modules.ai.application.api.dto.DealerAiRunRequest;
import com.autodealer.crm.modules.ai.application.api.dto.EditAiMessageRequest;
import com.autodealer.crm.modules.ai.application.api.dto.WithdrawAiMessageRequest;
import com.autodealer.crm.modules.ai.application.api.dto.ProviderRuntimeConfig;
import com.autodealer.crm.modules.ai.application.api.enums.AiConversationStatus;
import com.autodealer.crm.modules.ai.application.api.enums.AiEntryPoint;
import com.autodealer.crm.modules.ai.application.api.enums.AiMessageRole;
import com.autodealer.crm.modules.ai.application.api.enums.AiRunStatus;
import com.autodealer.crm.modules.ai.persistence.mapper.TAiWorkflowMapper;
import com.autodealer.crm.modules.ai.persistence.mapper.TAiWorkflowStepMapper;
import com.autodealer.crm.modules.ai.persistence.model.TAiConversation;
import com.autodealer.crm.modules.ai.persistence.model.TAiMessage;
import com.autodealer.crm.modules.ai.persistence.model.TAiRun;
import com.autodealer.crm.modules.ai.persistence.model.TAiToolCall;
import com.autodealer.crm.modules.ai.application.internal.AiConversationServiceImpl;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.ArgumentCaptor;

class AiConversationServiceImplTest {

    @Test
    void createRun_shouldCreateTraceAndAppendUserMessage() {
        AiTraceService traceService = mock(AiTraceService.class);
        DealerAiClient dealerAiClient = mock(DealerAiClient.class);
        ToolRegistry toolRegistry = mock(ToolRegistry.class);
        AiProviderConfigService providerConfigService = mock(AiProviderConfigService.class);
        AiRunCancellationRegistry cancellationRegistry = new AiRunCancellationRegistry();
        TAiWorkflowMapper workflowMapper = mock(TAiWorkflowMapper.class);
        TAiWorkflowStepMapper workflowStepMapper = mock(TAiWorkflowStepMapper.class);
        TAiRun run = new TAiRun();
        run.setId(3L);
        run.setRunNo("AIR202606280001");
        run.setConversationId(11L);
        run.setTurnNo(1);
        run.setEntryPoint(AiEntryPoint.PAGE.name());
        run.setStatus(AiRunStatus.CREATED.name());
        run.setCreateTime(LocalDateTime.of(2026, 6, 28, 11, 0));
        TAiConversation conversation = conversation("AIC202606300001", 11L, null, null);
        conversation.setTitle("新的 AI 会话");
        when(traceService.findOrCreateConversation(any(), any(), any(), any())).thenReturn(conversation);
        when(traceService.lockOwnedConversation("AIC202606300001")).thenReturn(conversation);
        when(traceService.getConversationById(11L)).thenReturn(conversation);
        when(traceService.nextTurnNo(11L)).thenReturn(1);
        when(traceService.createRun(any())).thenReturn(run);
        when(toolRegistry.definitions()).thenReturn(List.of());
        AiConversationServiceImpl service = service(traceService, dealerAiClient, providerConfigService,
                cancellationRegistry, toolRegistry, workflowMapper, workflowStepMapper);
        CreateAiRunRequest request = new CreateAiRunRequest();
        request.setEntryPoint(AiEntryPoint.PAGE.name());
        request.setPrompt("查客户跟进");

        AiRunResponse response = service.createRun(request);

        assertEquals("AIR202606280001", response.getRunNo());
        assertEquals("AIC202606300001", response.getConversationNo());
        assertEquals(1, response.getTurnNo());
        verify(traceService).createRun(org.mockito.ArgumentMatchers.argThat(command ->
                command.conversationId().equals(11L)
                        && command.turnNo() == 1
                        && command.entryPoint() == AiEntryPoint.PAGE));
        verify(traceService).appendMessage(org.mockito.ArgumentMatchers.argThat((AiMessageCommand command) ->
                command.conversationId().equals(11L)
                        && command.runId().equals(3L)
                        && command.role() == AiMessageRole.USER
                        && command.sequenceNo() == 1
                        && Boolean.TRUE.equals(command.visibleToUser())
                        && command.content().equals("查客户跟进")));
        verify(traceService).updateConversationAfterRun(11L, "AIR202606280001", "用户问题：查客户跟进");
        verify(traceService).renameConversation("AIC202606300001", "查客户跟进");
    }

    @Test
    void getConversation_shouldReturnRecoverableTurnsForAllRuns() {
        AiTraceService traceService = mock(AiTraceService.class);
        DealerAiClient dealerAiClient = mock(DealerAiClient.class);
        ToolRegistry toolRegistry = mock(ToolRegistry.class);
        AiProviderConfigService providerConfigService = mock(AiProviderConfigService.class);
        AiRunCancellationRegistry cancellationRegistry = new AiRunCancellationRegistry();
        TAiWorkflowMapper workflowMapper = mock(TAiWorkflowMapper.class);
        TAiWorkflowStepMapper workflowStepMapper = mock(TAiWorkflowStepMapper.class);
        TAiConversation conversation = conversation("AIC202606300010", 11L, null, null);
        TAiRun firstRun = run("AIR202606300001", 11L, 1);
        TAiRun secondRun = run("AIR202606300002", 11L, 2);
        when(traceService.getOwnedConversation("AIC202606300010")).thenReturn(conversation);
        when(traceService.listConversationMessages(11L)).thenReturn(List.of());
        when(traceService.listRunsByConversationId(11L)).thenReturn(List.of(firstRun, secondRun));
        when(traceService.getLatestRunByConversationId(11L)).thenReturn(secondRun);
        when(traceService.getRunTrace(firstRun)).thenReturn(trace(firstRun, "第一轮回答", "get_customer_profile"));
        when(traceService.getRunTrace(secondRun)).thenReturn(trace(secondRun, "第二轮回答", "list_pending_transaction_approvals"));
        AiConversationServiceImpl service = service(traceService, dealerAiClient, providerConfigService,
                cancellationRegistry, toolRegistry, workflowMapper, workflowStepMapper);

        AiConversationDetailResponse detail = service.getConversation("AIC202606300010");

        assertEquals(2, detail.getTurns().size());
        assertEquals("AIR202606300001", detail.getTurns().get(0).getRun().getRunNo());
        assertEquals("第一轮回答", detail.getTurns().get(0).getAssistantMessage().contentSummary());
        assertEquals("list_pending_transaction_approvals", detail.getTurns().get(1).getToolResults().get(0).toolName());
        assertEquals("AIR202606300002", detail.getLatestRun().getRunNo());
        assertEquals("AIR202606300002", detail.getLatestRunTrace().getRun().getRunNo());
    }

    @Test
    void streamRun_shouldPersistAssistantMessageAndKeepWaitingWhenProposalCreated() {
        AiTraceService traceService = mock(AiTraceService.class);
        DealerAiClient dealerAiClient = mock(DealerAiClient.class);
        ToolRegistry toolRegistry = mock(ToolRegistry.class);
        AiProviderConfigService providerConfigService = mock(AiProviderConfigService.class);
        AiRunCancellationRegistry cancellationRegistry = new AiRunCancellationRegistry();
        TAiWorkflowMapper workflowMapper = mock(TAiWorkflowMapper.class);
        TAiWorkflowStepMapper workflowStepMapper = mock(TAiWorkflowStepMapper.class);
        TAiRun run = new TAiRun();
        run.setId(3L);
        run.setRunNo("AIR202606280002");
        run.setEntryPoint(AiEntryPoint.PAGE.name());
        run.setStatus(AiRunStatus.CREATED.name());
        run.setPromptSummary("创建跟进任务");
        when(traceService.getOwnedRun("AIR202606280002")).thenReturn(run);
        when(traceService.getRunById(3L)).thenReturn(run);
        when(providerConfigService.getEnabledRuntimeConfig()).thenReturn(runtimeConfig());
        when(toolRegistry.definitions()).thenReturn(List.of());
        doAnswer(invocation -> {
            DealerAiEventConsumer consumer = invocation.getArgument(1);
            consumer.accept(event("evt-1", 1, "message_completed", Map.of("content", "已生成提议")));
            consumer.accept(event("evt-2", 2, "proposal_created", Map.of("proposalId", 9)));
            return null;
        }).when(dealerAiClient).streamRunEvents(any(), any(), any());
        AiConversationServiceImpl service = service(traceService, dealerAiClient, providerConfigService,
                cancellationRegistry, toolRegistry, workflowMapper, workflowStepMapper);

        service.streamRun("AIR202606280002", 0);

        verify(traceService, timeout(1000)).appendMessage(org.mockito.ArgumentMatchers.argThat(command ->
                command.runId().equals(3L)
                        && command.role() == AiMessageRole.ASSISTANT
                        && command.sequenceNo() == 2
                        && command.content().equals("已生成提议")));
        verify(traceService, timeout(1000).atLeastOnce()).updateRunStatus(
                3L, AiRunStatus.WAITING_FOR_APPROVAL, null, null);
    }

    @Test
    void streamRun_shouldPersistWorkflowEventsForTraceRecovery() {
        AiTraceService traceService = mock(AiTraceService.class);
        DealerAiClient dealerAiClient = mock(DealerAiClient.class);
        ToolRegistry toolRegistry = mock(ToolRegistry.class);
        AiProviderConfigService providerConfigService = mock(AiProviderConfigService.class);
        AiRunCancellationRegistry cancellationRegistry = new AiRunCancellationRegistry();
        TAiWorkflowMapper workflowMapper = mock(TAiWorkflowMapper.class);
        TAiWorkflowStepMapper workflowStepMapper = mock(TAiWorkflowStepMapper.class);
        TAiRun run = new TAiRun();
        run.setId(3L);
        run.setRunNo("AIR202606280003");
        run.setEntryPoint(AiEntryPoint.PAGE.name());
        run.setStatus(AiRunStatus.CREATED.name());
        run.setPromptSummary("执行工作流");
        when(traceService.getOwnedRun("AIR202606280003")).thenReturn(run);
        when(traceService.getRunById(3L)).thenReturn(run);
        when(providerConfigService.getEnabledRuntimeConfig()).thenReturn(runtimeConfig());
        when(toolRegistry.definitions()).thenReturn(List.of());
        doAnswer(invocation -> {
            DealerAiEventConsumer consumer = invocation.getArgument(1);
            consumer.accept(event("evt-1", 1, "workflow_started", Map.of(
                    "workflowNo", "AIW1",
                    "title", "客户跟进辅助工作流")));
            consumer.accept(event("evt-2", 2, "workflow_completed", Map.of(
                    "workflowNo", "AIW1",
                    "title", "客户跟进辅助工作流")));
            return null;
        }).when(dealerAiClient).streamRunEvents(any(), any(), any());
        AiConversationServiceImpl service = service(traceService, dealerAiClient, providerConfigService,
                cancellationRegistry, toolRegistry, workflowMapper, workflowStepMapper);

        service.streamRun("AIR202606280003", 0);

        verify(traceService, timeout(1000).atLeastOnce())
                .recordExecutionEvent(org.mockito.ArgumentMatchers.argThat((AiExecutionEventCommand command) ->
                        command.runId().equals(3L)
                                && command.objectType().equals("AI_WORKFLOW")
                                && command.objectId().equals("AIW1")));
    }

    @Test
    void streamRun_shouldOmitBlankContextAndUseDealerAiToolSchemaContract() {
        AiTraceService traceService = mock(AiTraceService.class);
        DealerAiClient dealerAiClient = mock(DealerAiClient.class);
        ToolRegistry toolRegistry = mock(ToolRegistry.class);
        AiProviderConfigService providerConfigService = mock(AiProviderConfigService.class);
        AiRunCancellationRegistry cancellationRegistry = new AiRunCancellationRegistry();
        TAiWorkflowMapper workflowMapper = mock(TAiWorkflowMapper.class);
        TAiWorkflowStepMapper workflowStepMapper = mock(TAiWorkflowStepMapper.class);
        TAiRun run = new TAiRun();
        run.setId(3L);
        run.setRunNo("AIR202606280004");
        run.setEntryPoint(AiEntryPoint.SIDE_PANEL.name());
        run.setStatus(AiRunStatus.CREATED.name());
        run.setPromptSummary("查询库存预警");
        run.setContextObjectType("");
        run.setContextObjectId("");
        ToolDefinition definition = new ToolDefinition(
                "get_inventory_alerts",
                "查询当前库存预警商品",
                "product:stock:view",
                ToolRiskLevel.READONLY,
                true,
                false,
                20,
                "AI_TOOL_GET_INVENTORY_ALERTS");
        when(traceService.getOwnedRun("AIR202606280004")).thenReturn(run);
        when(traceService.getRunById(3L)).thenReturn(run);
        when(providerConfigService.getEnabledRuntimeConfig()).thenReturn(runtimeConfig());
        when(toolRegistry.definitions()).thenReturn(List.of(definition));
        doAnswer(invocation -> {
            DealerAiEventConsumer consumer = invocation.getArgument(1);
            consumer.accept(event("evt-1", 1, "run_completed", Map.of("status", "COMPLETED")));
            return null;
        }).when(dealerAiClient).streamRunEvents(any(), any(), any());
        AiConversationServiceImpl service = service(traceService, dealerAiClient, providerConfigService,
                cancellationRegistry, toolRegistry, workflowMapper, workflowStepMapper);

        service.streamRun("AIR202606280004", 0);

        verify(dealerAiClient, timeout(1000)).streamRunEvents(org.mockito.ArgumentMatchers.argThat(
                (DealerAiRunRequest request) -> {
                    assertNull(request.getContext());
                    assertEquals("AIPC202606290001", request.getProviderRuntimeConfig().getProviderConfigNo());
                    Map<String, Object> toolSchema = request.getToolSchemas().get(0);
                    assertEquals("READONLY", toolSchema.get("risk_level"));
                    assertEquals(false, toolSchema.get("requires_confirmation"));
                    Map<String, Object> inputSchema = (Map<String, Object>) toolSchema.get("input_schema");
                    assertEquals("object", inputSchema.get("type"));
                    assertEquals(false, inputSchema.get("additionalProperties"));
                    assertTrue(((Map<?, ?>) inputSchema.get("properties")).containsKey("sku"));
                    return true;
                }), any(), any());
    }

    @Test
    void streamRun_runtimeCompletedWithFailedStatus_shouldKeepRunFailed() {
        AiTraceService traceService = mock(AiTraceService.class);
        DealerAiClient dealerAiClient = mock(DealerAiClient.class);
        ToolRegistry toolRegistry = mock(ToolRegistry.class);
        AiProviderConfigService providerConfigService = mock(AiProviderConfigService.class);
        AiRunCancellationRegistry cancellationRegistry = new AiRunCancellationRegistry();
        TAiWorkflowMapper workflowMapper = mock(TAiWorkflowMapper.class);
        TAiWorkflowStepMapper workflowStepMapper = mock(TAiWorkflowStepMapper.class);
        TAiRun run = run("AIR-FAILED", null, 1);
        run.setStatus(AiRunStatus.CREATED.name());
        run.setPromptSummary("测试失败");
        when(traceService.getOwnedRun("AIR-FAILED")).thenReturn(run);
        when(traceService.getRunById(run.getId())).thenReturn(run);
        when(providerConfigService.getEnabledRuntimeConfig()).thenReturn(runtimeConfig());
        when(toolRegistry.definitions()).thenReturn(List.of());
        doAnswer(invocation -> {
            DealerAiEventConsumer consumer = invocation.getArgument(1);
            consumer.accept(event("evt-failed", 2, "run_completed", Map.of(
                    "status", "FAILED",
                    "error_code", "PROVIDER_TIMEOUT",
                    "message", "模型请求超时")));
            return null;
        }).when(dealerAiClient).streamRunEvents(any(), any(), any());
        AiConversationServiceImpl service = service(traceService, dealerAiClient, providerConfigService,
                cancellationRegistry, toolRegistry, workflowMapper, workflowStepMapper);

        service.streamRun("AIR-FAILED", 0);

        verify(traceService, timeout(1000)).updateRunStatusIfNotTerminal(
                run.getId(), AiRunStatus.FAILED, "PROVIDER_TIMEOUT", "模型请求超时");
    }

    @Test
    void streamRun_errorEventThenFailedTerminal_shouldKeepToolFailureClassification() {
        AiTraceService traceService = mock(AiTraceService.class);
        DealerAiClient dealerAiClient = mock(DealerAiClient.class);
        ToolRegistry toolRegistry = mock(ToolRegistry.class);
        AiProviderConfigService providerConfigService = mock(AiProviderConfigService.class);
        AiRunCancellationRegistry cancellationRegistry = new AiRunCancellationRegistry();
        TAiWorkflowMapper workflowMapper = mock(TAiWorkflowMapper.class);
        TAiWorkflowStepMapper workflowStepMapper = mock(TAiWorkflowStepMapper.class);
        TAiRun run = run("AIR-TOOL-FAILED", null, 1);
        run.setStatus(AiRunStatus.CREATED.name());
        run.setPromptSummary("越权客户测试");
        when(traceService.getOwnedRun("AIR-TOOL-FAILED")).thenReturn(run);
        when(traceService.getRunById(run.getId())).thenReturn(run);
        when(providerConfigService.getEnabledRuntimeConfig()).thenReturn(runtimeConfig());
        when(toolRegistry.definitions()).thenReturn(List.of());
        doAnswer(invocation -> {
            DealerAiEventConsumer consumer = invocation.getArgument(1);
            consumer.accept(event("evt-error", 2, "error", Map.of(
                    "code", "AI_TOOL_EXECUTION_FAILED",
                    "message", "AI 工具调用失败，请稍后重试")));
            consumer.accept(event("evt-failed", 3, "run_completed", Map.of("status", "FAILED")));
            return null;
        }).when(dealerAiClient).streamRunEvents(any(), any(), any());
        AiConversationServiceImpl service = service(traceService, dealerAiClient, providerConfigService,
                cancellationRegistry, toolRegistry, workflowMapper, workflowStepMapper);

        service.streamRun("AIR-TOOL-FAILED", 0);

        verify(traceService, timeout(1000)).updateRunStatusIfNotTerminal(
                run.getId(), AiRunStatus.FAILED,
                "AI_TOOL_EXECUTION_FAILED", "AI 工具调用失败，请稍后重试");
    }

    @Test
    void editMessage_shouldInvalidateFollowingContextAndCreateImmutableRevision() {
        AiTraceService traceService = mock(AiTraceService.class);
        DealerAiClient dealerAiClient = mock(DealerAiClient.class);
        ToolRegistry toolRegistry = mock(ToolRegistry.class);
        AiProviderConfigService providerConfigService = mock(AiProviderConfigService.class);
        AiRunCancellationRegistry cancellationRegistry = new AiRunCancellationRegistry();
        TAiWorkflowMapper workflowMapper = mock(TAiWorkflowMapper.class);
        TAiWorkflowStepMapper workflowStepMapper = mock(TAiWorkflowStepMapper.class);
        TAiConversation conversation = conversation("AIC-EDIT", 11L, null, null);
        TAiMessage original = new TAiMessage();
        original.setId(21L);
        original.setMessageNo("AIM-OLD");
        original.setConversationId(11L);
        original.setRunId(2L);
        original.setRole(AiMessageRole.USER.name());
        original.setStatus("ACTIVE");
        original.setRevisionNo(1);
        original.setVersion(1);
        TAiRun oldRun = run("AIR-OLD", 11L, 2);
        oldRun.setContextActive(true);
        TAiRun replacement = run("AIR-NEW", 11L, 3);
        replacement.setStatus(AiRunStatus.CREATED.name());
        replacement.setContextActive(true);
        when(traceService.lockOwnedConversation("AIC-EDIT")).thenReturn(conversation);
        when(traceService.getOwnedUserMessage(11L, "AIM-OLD")).thenReturn(original);
        when(traceService.getRunById(2L)).thenReturn(oldRun);
        when(traceService.invalidateContextFromTurn(11L, 2, "用户编辑历史消息"))
                .thenReturn(List.of(oldRun));
        when(traceService.nextTurnNo(11L)).thenReturn(3);
        when(traceService.createRun(any())).thenReturn(replacement);
        when(traceService.listActiveContextMessages(11L)).thenReturn(List.of());
        AiConversationServiceImpl service = service(traceService, dealerAiClient, providerConfigService,
                cancellationRegistry, toolRegistry, workflowMapper, workflowStepMapper);
        EditAiMessageRequest request = new EditAiMessageRequest();
        request.setContent("修改后的问题");
        request.setExpectedVersion(1);

        AiRunResponse response = service.editMessage("AIC-EDIT", "AIM-OLD", request);

        assertEquals("AIR-NEW", response.getRunNo());
        verify(traceService).supersedeMessage(original, 1);
        ArgumentCaptor<AiMessageCommand> messageCaptor = ArgumentCaptor.forClass(AiMessageCommand.class);
        verify(traceService).appendMessage(messageCaptor.capture());
        assertEquals(21L, messageCaptor.getValue().supersedesMessageId());
        assertEquals(2, messageCaptor.getValue().revisionNo());
        assertEquals("修改后的问题", messageCaptor.getValue().content());
    }

    @Test
    void withdrawMessage_shouldRemoveTurnAndFollowingRunsFromActiveConversation() {
        AiTraceService traceService = mock(AiTraceService.class);
        DealerAiClient dealerAiClient = mock(DealerAiClient.class);
        ToolRegistry toolRegistry = mock(ToolRegistry.class);
        AiProviderConfigService providerConfigService = mock(AiProviderConfigService.class);
        AiRunCancellationRegistry cancellationRegistry = new AiRunCancellationRegistry();
        TAiWorkflowMapper workflowMapper = mock(TAiWorkflowMapper.class);
        TAiWorkflowStepMapper workflowStepMapper = mock(TAiWorkflowStepMapper.class);
        TAiConversation conversation = conversation("AIC-WITHDRAW", 11L, null, null);
        TAiMessage message = new TAiMessage();
        message.setId(22L);
        message.setMessageNo("AIM-WITHDRAW");
        message.setConversationId(11L);
        message.setRunId(2L);
        message.setRole(AiMessageRole.USER.name());
        message.setStatus("ACTIVE");
        message.setVersion(1);
        TAiRun run = run("AIR-WITHDRAW", 11L, 2);
        run.setContextActive(true);
        when(traceService.lockOwnedConversation("AIC-WITHDRAW")).thenReturn(conversation);
        when(traceService.getOwnedConversation("AIC-WITHDRAW")).thenReturn(conversation);
        when(traceService.getOwnedUserMessage(11L, "AIM-WITHDRAW")).thenReturn(message);
        when(traceService.getRunById(2L)).thenReturn(run);
        when(traceService.invalidateContextFromTurn(11L, 2, "用户撤回历史消息"))
                .thenReturn(List.of(run));
        when(traceService.listActiveContextMessages(11L)).thenReturn(List.of());
        when(traceService.listConversationMessages(11L)).thenReturn(List.of());
        when(traceService.listRunsByConversationId(11L)).thenReturn(List.of());
        AiConversationServiceImpl service = service(traceService, dealerAiClient, providerConfigService,
                cancellationRegistry, toolRegistry, workflowMapper, workflowStepMapper);
        WithdrawAiMessageRequest request = new WithdrawAiMessageRequest();
        request.setExpectedVersion(1);

        AiConversationDetailResponse response = service.withdrawMessage(
                "AIC-WITHDRAW", "AIM-WITHDRAW", request);

        assertEquals(0, response.getTurns().size());
        verify(traceService).withdrawMessage(message, 1);
        verify(traceService).updateConversationAfterRun(11L, null, "");
    }

    private ProviderRuntimeConfig runtimeConfig() {
        ProviderRuntimeConfig config = new ProviderRuntimeConfig();
        config.setProviderConfigNo("AIPC202606290001");
        config.setProviderFormat("OPENAI_COMPATIBLE");
        config.setBaseUrl("https://api.deepseek.com");
        config.setModelName("deepseek-chat");
        config.setApiKey("test-api-key");
        config.setTimeoutSeconds(15);
        config.setMaxOutputTokens(64);
        config.setTemperature(BigDecimal.valueOf(0.2));
        return config;
    }

    private AiConversationServiceImpl service(AiTraceService traceService,
                                              DealerAiClient dealerAiClient,
                                              AiProviderConfigService providerConfigService,
                                              AiRunCancellationRegistry cancellationRegistry,
                                              ToolRegistry toolRegistry,
                                              TAiWorkflowMapper workflowMapper,
                                              TAiWorkflowStepMapper workflowStepMapper) {
        AiAssistantPolicyService policyService = mock(AiAssistantPolicyService.class);
        AiRunEventStore eventStore = mock(AiRunEventStore.class);
        when(policyService.getPolicy()).thenReturn(policy());
        when(eventStore.listAfter(any(), any(), any(Integer.class))).thenReturn(List.of());
        when(traceService.startRunIfCreated(any())).thenReturn(true);
        when(toolRegistry.definitionsForCurrentUser()).thenAnswer(invocation -> toolRegistry.definitions());
        return new AiConversationServiceImpl(
                traceService, dealerAiClient, providerConfigService, cancellationRegistry,
                new AiSensitiveDataSanitizer(), toolRegistry, workflowMapper, workflowStepMapper,
                policyService, eventStore);
    }

    private com.autodealer.crm.modules.ai.application.api.dto.AiAssistantPolicyResponse policy() {
        com.autodealer.crm.modules.ai.application.api.dto.AiAssistantPolicyResponse policy =
                new com.autodealer.crm.modules.ai.application.api.dto.AiAssistantPolicyResponse();
        policy.setEnabledTools(true);
        policy.setAllowedToolNames(List.of(
                "get_inventory_alerts", "get_customer_profile", "list_pending_transaction_approvals"));
        policy.setProposalsEnabled(true);
        policy.setMaxToolCallsPerRun(8);
        policy.setSafetyMode("STRICT");
        policy.setNetworkMode("PROVIDER_ONLY");
        policy.setContextMessageLimit(8);
        policy.setSummaryMaxChars(2000);
        policy.setMaxRunSeconds(120);
        policy.setVersion(1);
        return policy;
    }

    private DealerAiEventResponse event(String eventId,
                                        int sequence,
                                        String eventType,
                                        Map<String, Object> payload) {
        DealerAiEventResponse event = new DealerAiEventResponse();
        event.setEventId(eventId);
        event.setRunId("AIR202606280002");
        event.setSequence(sequence);
        event.setEventType(eventType);
        event.setPayload(payload);
        return event;
    }

    private TAiConversation conversation(String conversationNo,
                                         Long id,
                                         String contextObjectType,
                                         String contextObjectId) {
        TAiConversation conversation = new TAiConversation();
        conversation.setId(id);
        conversation.setConversationNo(conversationNo);
        conversation.setTitle("测试会话");
        conversation.setStatus(AiConversationStatus.ACTIVE.name());
        conversation.setEntryPoint(AiEntryPoint.PAGE.name());
        conversation.setContextObjectType(contextObjectType);
        conversation.setContextObjectId(contextObjectId);
        conversation.setSummaryText("");
        return conversation;
    }

    private TAiRun run(String runNo, Long conversationId, int turnNo) {
        TAiRun run = new TAiRun();
        run.setId((long) turnNo);
        run.setRunNo(runNo);
        run.setConversationId(conversationId);
        run.setTurnNo(turnNo);
        run.setEntryPoint(AiEntryPoint.PAGE.name());
        run.setStatus(AiRunStatus.COMPLETED.name());
        run.setCreateTime(LocalDateTime.of(2026, 6, 30, 10, turnNo));
        return run;
    }

    private AiRunTraceResponse trace(TAiRun run, String assistantContent, String toolName) {
        TAiMessage userMessage = new TAiMessage();
        userMessage.setId(run.getId() * 10);
        userMessage.setRole(AiMessageRole.USER.name());
        userMessage.setSequenceNo(1);
        userMessage.setVisibleToUser(true);
        userMessage.setContentSummary("用户问题");
        TAiMessage assistantMessage = new TAiMessage();
        assistantMessage.setId(run.getId() * 10 + 1);
        assistantMessage.setRole(AiMessageRole.ASSISTANT.name());
        assistantMessage.setSequenceNo(2);
        assistantMessage.setVisibleToUser(true);
        assistantMessage.setContentSummary(assistantContent);
        TAiToolCall toolCall = new TAiToolCall();
        toolCall.setId(run.getId() * 10 + 2);
        toolCall.setToolName(toolName);
        toolCall.setPermissionCode("ai:test");
        toolCall.setRiskLevel("READONLY");
        toolCall.setInputSummary("{}");
        toolCall.setOutputSummary("工具摘要");
        toolCall.setResultStatus("SUCCESS");
        toolCall.setDisplayPayloadJson("{\"items\":[{\"customerName\":\"张伟\"}]}");
        return AiRunTraceResponse.from(
                run,
                List.of(userMessage, assistantMessage),
                List.of(toolCall),
                List.of(),
                List.of(),
                List.of(),
                List.of());
    }
}
