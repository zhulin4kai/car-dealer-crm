package com.autodealer.crm.modules.ai.application.internal;

import com.autodealer.crm.modules.ai.application.api.tool.ToolDefinition;
import com.autodealer.crm.modules.ai.application.api.tool.ToolRegistry;
import com.autodealer.crm.modules.ai.application.api.tool.ToolRiskLevel;
import com.autodealer.crm.modules.ai.application.api.dto.UpdateAiAssistantPolicyRequest;
import com.autodealer.crm.modules.ai.persistence.mapper.TAiAssistantPolicyMapper;
import com.autodealer.crm.modules.ai.persistence.model.TAiAssistantPolicy;
import com.autodealer.crm.modules.ai.application.internal.AiAssistantPolicyServiceImpl;
import com.autodealer.crm.modules.identity.application.api.security.CurrentUserProvider;
import com.autodealer.crm.shared.error.BusinessException;
import com.autodealer.crm.shared.error.CodeEnum;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiAssistantPolicyServiceImplTest {
    @Mock private TAiAssistantPolicyMapper mapper;
    @Mock private ToolRegistry toolRegistry;
    @Mock private CurrentUserProvider currentUserProvider;

    private AiAssistantPolicyServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AiAssistantPolicyServiceImpl(
                mapper, toolRegistry, currentUserProvider, new ObjectMapper());
    }

    @Test
    void updatePolicy_registeredToolsAndMatchingVersion_shouldPersist() {
        when(toolRegistry.definitions()).thenReturn(List.of(definition("search_customers")));
        when(currentUserProvider.getCurrentUserId()).thenReturn(1);
        when(mapper.updateIfVersionMatches(any(), any())).thenReturn(1);
        when(mapper.selectSingleton()).thenReturn(policy(2));

        var response = service.updatePolicy(request(List.of("search_customers"), 1));

        assertEquals(2, response.getVersion());
        assertEquals("PROVIDER_ONLY", response.getNetworkMode());
        assertEquals(List.of("search_customers"), response.getAllowedToolNames());
    }

    @Test
    void updatePolicy_unknownTool_shouldReject() {
        when(toolRegistry.definitions()).thenReturn(List.of(definition("search_customers")));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.updatePolicy(request(List.of("run_sql"), 1)));

        assertEquals(CodeEnum.AI_TOOL_NOT_FOUND, exception.getCodeEnum());
    }

    @Test
    void updatePolicy_versionChanged_shouldRejectConflict() {
        when(toolRegistry.definitions()).thenReturn(List.of(definition("search_customers")));
        when(currentUserProvider.getCurrentUserId()).thenReturn(1);
        when(mapper.updateIfVersionMatches(any(), any())).thenReturn(0);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.updatePolicy(request(List.of("search_customers"), 1)));

        assertEquals(CodeEnum.TRAN_STATE_CONFLICT, exception.getCodeEnum());
    }

    private UpdateAiAssistantPolicyRequest request(List<String> allowedTools, int version) {
        UpdateAiAssistantPolicyRequest request = new UpdateAiAssistantPolicyRequest();
        request.setEnabledTools(true);
        request.setAllowedToolNames(allowedTools);
        request.setProposalsEnabled(true);
        request.setMaxToolCallsPerRun(8);
        request.setSafetyMode("STRICT");
        request.setNetworkMode("PROVIDER_ONLY");
        request.setContextMessageLimit(8);
        request.setSummaryMaxChars(2000);
        request.setMaxRunSeconds(120);
        request.setVersion(version);
        return request;
    }

    private TAiAssistantPolicy policy(int version) {
        TAiAssistantPolicy policy = new TAiAssistantPolicy();
        policy.setId(1L);
        policy.setEnabledTools(true);
        policy.setAllowedToolNames("[\"search_customers\"]");
        policy.setProposalsEnabled(true);
        policy.setMaxToolCallsPerRun(8);
        policy.setSafetyMode("STRICT");
        policy.setNetworkMode("PROVIDER_ONLY");
        policy.setContextMessageLimit(8);
        policy.setSummaryMaxChars(2000);
        policy.setMaxRunSeconds(120);
        policy.setVersion(version);
        policy.setEditTime(LocalDateTime.now());
        return policy;
    }

    private ToolDefinition definition(String name) {
        return new ToolDefinition(name, "测试工具", "customer:list", ToolRiskLevel.READONLY,
                true, false, 20, "AI_TOOL_TEST");
    }
}
