package com.autodealer.crm.modules.ai.application.internal;

import com.autodealer.crm.modules.ai.application.api.AiAssistantPolicyService;
import com.autodealer.crm.modules.ai.application.api.AiTraceService;
import com.autodealer.crm.modules.ai.application.api.dto.AiAssistantPolicyResponse;
import com.autodealer.crm.modules.ai.application.api.tool.ToolDefinition;
import com.autodealer.crm.modules.ai.application.api.tool.ToolExecutionResult;
import com.autodealer.crm.modules.ai.application.api.tool.ToolExecutor;
import com.autodealer.crm.modules.ai.application.api.tool.ToolRegistry;
import com.autodealer.crm.modules.ai.application.api.tool.ToolRiskLevel;
import com.autodealer.crm.modules.ai.application.api.dto.ExecuteAiToolRequest;
import com.autodealer.crm.modules.ai.application.api.enums.AiResultStatus;
import com.autodealer.crm.modules.ai.persistence.mapper.TAiRunMapper;
import com.autodealer.crm.modules.ai.persistence.mapper.TAiToolCallMapper;
import com.autodealer.crm.modules.ai.persistence.model.TAiRun;
import com.autodealer.crm.modules.ai.application.internal.AiInternalToolServiceImpl;
import com.autodealer.crm.modules.identity.application.api.security.CurrentUserProvider;
import com.autodealer.crm.shared.error.BusinessException;
import com.autodealer.crm.modules.identity.application.api.model.TUser;
import com.autodealer.crm.shared.error.CodeEnum;
import com.autodealer.crm.modules.identity.application.api.UserService;
import tools.jackson.databind.ObjectMapper;
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
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiInternalToolServiceImplTest {
    @Mock private TAiRunMapper runMapper;
    @Mock private UserService userService;
    @Mock private CurrentUserProvider currentUserProvider;
    @Mock private AiTraceService traceService;
    @Mock private ToolExecutor executor;
    @Mock private TAiToolCallMapper toolCallMapper;
    @Mock private AiAssistantPolicyService policyService;

    private AiInternalToolServiceImpl service;

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
        ToolRegistry registry = new ToolRegistry(
                List.of(executor),
                currentUserProvider,
                traceService,
                new AiSensitiveDataSanitizer(),
                new ObjectMapper());
        com.autodealer.crm.modules.ai.application.api.dto.AiAssistantPolicyResponse policy =
                new com.autodealer.crm.modules.ai.application.api.dto.AiAssistantPolicyResponse();
        policy.setEnabledTools(true);
        policy.setAllowedToolNames(List.of("search_customers"));
        policy.setProposalsEnabled(true);
        policy.setMaxToolCallsPerRun(8);
        when(policyService.getPolicy()).thenReturn(policy);
        when(toolCallMapper.countByRunId(1L)).thenReturn(0);
        service = new AiInternalToolServiceImpl(
                runMapper, userService, registry, toolCallMapper, policyService);
    }

    @Test
    void execute_shouldWriteSuccessToolCallTrace() {
        when(runMapper.selectByRunNo("AIR1")).thenReturn(run());
        when(userService.getLoginUserById(7)).thenReturn(enabledUser());
        when(currentUserProvider.hasAuthority("customer:list")).thenReturn(true);
        when(executor.execute(any(), any())).thenReturn(ToolExecutionResult.of(
                Map.of("items", List.of()), "返回客户 0 条", "CUSTOMER"));

        ExecuteAiToolRequest request = new ExecuteAiToolRequest();
        request.setRunNo("AIR1");
        request.setArguments(Map.of("keyword", "王"));

        assertEquals("SUCCESS", service.execute("search_customers", request).getResultStatus());
        verify(traceService).recordToolCall(argThat(command ->
                command.runId().equals(1L)
                        && command.toolName().equals("search_customers")
                        && command.resultStatus() == AiResultStatus.SUCCESS
                        && command.inputSummary().contains("keyword")
                        && command.outputSummary().contains("返回客户")));
    }

    @Test
    void execute_shouldWriteFailedToolCallTrace() {
        when(runMapper.selectByRunNo("AIR1")).thenReturn(run());
        when(userService.getLoginUserById(7)).thenReturn(enabledUser());
        when(currentUserProvider.hasAuthority("customer:list")).thenReturn(true);
        when(executor.execute(any(), any())).thenThrow(
                new BusinessException(CodeEnum.AI_TOOL_ARGUMENT_INVALID, "AI 工具参数错误"));

        ExecuteAiToolRequest request = new ExecuteAiToolRequest();
        request.setRunNo("AIR1");
        request.setArguments(Map.of("keyword", "王"));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.execute("search_customers", request));

        assertEquals(CodeEnum.AI_TOOL_ARGUMENT_INVALID, ex.getCodeEnum());
        verify(traceService).recordToolCall(argThat(command ->
                command.runId().equals(1L)
                        && command.toolName().equals("search_customers")
                        && command.resultStatus() == AiResultStatus.FAILED
                        && CodeEnum.AI_TOOL_ARGUMENT_INVALID.name().equals(command.errorCode())));
    }

    private TAiRun run() {
        TAiRun run = new TAiRun();
        run.setId(1L);
        run.setRunNo("AIR1");
        run.setUserId(7);
        run.setStatus("RUNNING");
        run.setContextActive(true);
        return run;
    }

    private TUser enabledUser() {
        TUser user = new TUser();
        user.setId(7);
        user.setAccountEnabled(1);
        user.setAccountNoExpired(1);
        user.setAccountNoLocked(1);
        user.setCredentialsNoExpired(1);
        user.setPermissionList(List.of("customer:list"));
        return user;
    }
}
