package com.autodealer.crm.modules.ai.application.api.tool;

import com.autodealer.crm.modules.ai.application.api.enums.AiResultStatus;
import com.autodealer.crm.modules.ai.persistence.model.TAiRun;
import com.autodealer.crm.modules.ai.application.internal.AiSensitiveDataSanitizer;
import com.autodealer.crm.modules.ai.application.api.AiTraceService;
import com.autodealer.crm.modules.identity.application.api.security.CurrentUserProvider;
import com.autodealer.crm.shared.error.BusinessException;
import com.autodealer.crm.shared.error.CodeEnum;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ToolRegistryTest {
    @Mock private CurrentUserProvider currentUserProvider;
    @Mock private AiTraceService traceService;
    @Mock private ToolExecutor executor;
    private ToolRegistry registry;
    private TAiRun run;

    @BeforeEach
    void setUp() {
        ToolDefinition definition = new ToolDefinition(
                "search_customers",
                "查询客户",
                "customer:list",
                ToolRiskLevel.READONLY,
                true,
                false,
                20,
                "AI_TOOL_SEARCH_CUSTOMERS");
        when(executor.definition()).thenReturn(definition);
        registry = new ToolRegistry(
                List.of(executor),
                currentUserProvider,
                traceService,
                new AiSensitiveDataSanitizer(),
                new ObjectMapper());
        run = new TAiRun();
        run.setId(1L);
        run.setRunNo("AIR1");
    }

    @Test
    void unregisteredTool_shouldReject() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> registry.execute(new ToolExecutionContext(run), "unregistered_business_tool", Map.of()));

        assertEquals(CodeEnum.AI_TOOL_NOT_FOUND, ex.getCodeEnum());
        verify(executor, never()).execute(any(), any());
    }

    @Test
    void trustedContextFields_shouldRejectBeforeExecutor() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> registry.execute(new ToolExecutionContext(run), "search_customers", Map.of("userId", 8)));

        assertEquals(CodeEnum.AI_TOOL_ARGUMENT_INVALID, ex.getCodeEnum());
        verify(executor, never()).execute(any(), any());
        verify(traceService).recordToolCall(org.mockito.ArgumentMatchers.argThat(command ->
                command.runId().equals(1L)
                        && command.toolName().equals("search_customers")
                        && command.resultStatus() == AiResultStatus.FAILED
                        && CodeEnum.AI_TOOL_ARGUMENT_INVALID.name().equals(command.errorCode())));
    }

    @Test
    void nestedTrustedContextFields_shouldRejectBeforeExecutor() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> registry.execute(
                        new ToolExecutionContext(run),
                        "search_customers",
                        Map.of("filter", Map.of("dataScope", "ALL"))));

        assertEquals(CodeEnum.AI_TOOL_ARGUMENT_INVALID, ex.getCodeEnum());
        verify(executor, never()).execute(any(), any());
    }

    @Test
    void missingPermission_shouldReject() {
        when(currentUserProvider.hasAuthority("customer:list")).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> registry.execute(new ToolExecutionContext(run), "search_customers", Map.of("keyword", "王")));

        assertEquals(CodeEnum.AI_TOOL_FORBIDDEN, ex.getCodeEnum());
        verify(executor, never()).execute(any(), any());
        verify(traceService).recordToolCall(org.mockito.ArgumentMatchers.argThat(command ->
                command.runId().equals(1L)
                        && command.toolName().equals("search_customers")
                        && command.resultStatus() == AiResultStatus.FAILED
                        && CodeEnum.AI_TOOL_FORBIDDEN.name().equals(command.errorCode())));
    }

    @Test
    void success_shouldRecordToolCall() {
        when(currentUserProvider.hasAuthority("customer:list")).thenReturn(true);
        when(executor.execute(any(), any())).thenReturn(ToolExecutionResult.of(
                Map.of("items", List.of()), "返回客户 0 条", "CUSTOMER"));

        ToolExecutionResult result = registry.execute(
                new ToolExecutionContext(run),
                "search_customers",
                Map.of("keyword", "王"));

        assertEquals("返回客户 0 条", result.outputSummary());
        verify(traceService).recordToolCall(org.mockito.ArgumentMatchers.argThat(command ->
                command.runId().equals(1L)
                        && command.toolName().equals("search_customers")
                        && command.resultStatus() == AiResultStatus.SUCCESS
                        && command.displayPayloadJson().contains("\"items\"")));
    }
}
