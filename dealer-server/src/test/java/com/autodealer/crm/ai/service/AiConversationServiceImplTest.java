package com.autodealer.crm.ai.service;

import com.autodealer.crm.ai.ToolDefinition;
import com.autodealer.crm.ai.ToolRegistry;
import com.autodealer.crm.ai.ToolRiskLevel;
import com.autodealer.crm.ai.dto.AiExecutionEventCommand;
import com.autodealer.crm.ai.dto.AiMessageCommand;
import com.autodealer.crm.ai.dto.AiConversationDetailResponse;
import com.autodealer.crm.ai.dto.AiRunResponse;
import com.autodealer.crm.ai.dto.AiRunTraceResponse;
import com.autodealer.crm.ai.dto.CreateAiRunRequest;
import com.autodealer.crm.ai.dto.DealerAiEventResponse;
import com.autodealer.crm.ai.dto.DealerAiRunRequest;
import com.autodealer.crm.ai.dto.ProviderRuntimeConfig;
import com.autodealer.crm.ai.enums.AiConversationStatus;
import com.autodealer.crm.ai.enums.AiEntryPoint;
import com.autodealer.crm.ai.enums.AiMessageRole;
import com.autodealer.crm.ai.enums.AiRunStatus;
import com.autodealer.crm.ai.mapper.TAiWorkflowMapper;
import com.autodealer.crm.ai.mapper.TAiWorkflowStepMapper;
import com.autodealer.crm.ai.model.TAiConversation;
import com.autodealer.crm.ai.model.TAiMessage;
import com.autodealer.crm.ai.model.TAiRun;
import com.autodealer.crm.ai.model.TAiToolCall;
import com.autodealer.crm.ai.service.impl.AiConversationServiceImpl;
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
        when(traceService.findOrCreateConversation(any(), any(), any(), any())).thenReturn(conversation);
        when(traceService.getConversationById(11L)).thenReturn(conversation);
        when(traceService.nextTurnNo(11L)).thenReturn(1);
        when(traceService.createRun(any())).thenReturn(run);
        when(toolRegistry.definitions()).thenReturn(List.of());
        AiConversationServiceImpl service = new AiConversationServiceImpl(
                traceService, dealerAiClient, providerConfigService, cancellationRegistry,
                new AiSensitiveDataSanitizer(), toolRegistry, workflowMapper, workflowStepMapper);
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
        AiConversationServiceImpl service = new AiConversationServiceImpl(
                traceService, dealerAiClient, providerConfigService, cancellationRegistry,
                new AiSensitiveDataSanitizer(), toolRegistry, workflowMapper, workflowStepMapper);

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
        AiConversationServiceImpl service = new AiConversationServiceImpl(
                traceService, dealerAiClient, providerConfigService, cancellationRegistry,
                new AiSensitiveDataSanitizer(), toolRegistry, workflowMapper, workflowStepMapper);

        service.streamRun("AIR202606280002");

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
        AiConversationServiceImpl service = new AiConversationServiceImpl(
                traceService, dealerAiClient, providerConfigService, cancellationRegistry,
                new AiSensitiveDataSanitizer(), toolRegistry, workflowMapper, workflowStepMapper);

        service.streamRun("AIR202606280003");

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
        AiConversationServiceImpl service = new AiConversationServiceImpl(
                traceService, dealerAiClient, providerConfigService, cancellationRegistry,
                new AiSensitiveDataSanitizer(), toolRegistry, workflowMapper, workflowStepMapper);

        service.streamRun("AIR202606280004");

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
