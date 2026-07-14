package com.autodealer.crm.modules.ai.application.internal;

import com.autodealer.crm.modules.ai.application.api.tool.ToolRegistry;
import com.autodealer.crm.modules.ai.application.api.dto.AiAssistantPolicyResponse;
import com.autodealer.crm.modules.ai.application.api.dto.UpdateAiAssistantPolicyRequest;
import com.autodealer.crm.modules.ai.persistence.mapper.TAiAssistantPolicyMapper;
import com.autodealer.crm.modules.ai.persistence.model.TAiAssistantPolicy;
import com.autodealer.crm.modules.ai.application.api.AiAssistantPolicyService;
import com.autodealer.crm.modules.identity.application.api.security.CurrentUserProvider;
import com.autodealer.crm.shared.error.BusinessException;
import com.autodealer.crm.shared.error.CodeEnum;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class AiAssistantPolicyServiceImpl implements AiAssistantPolicyService {
    private final TAiAssistantPolicyMapper mapper;
    private final ToolRegistry toolRegistry;
    private final CurrentUserProvider currentUserProvider;
    private final ObjectMapper objectMapper;

    public AiAssistantPolicyServiceImpl(TAiAssistantPolicyMapper mapper,
                                        ToolRegistry toolRegistry,
                                        CurrentUserProvider currentUserProvider,
                                        ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.toolRegistry = toolRegistry;
        this.currentUserProvider = currentUserProvider;
        this.objectMapper = objectMapper;
    }

    @Override
    public AiAssistantPolicyResponse getPolicy() {
        return toResponse(requirePolicy());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiAssistantPolicyResponse updatePolicy(UpdateAiAssistantPolicyRequest request) {
        Set<String> registeredTools = toolRegistry.definitions().stream()
                .map(definition -> definition.name())
                .collect(java.util.stream.Collectors.toSet());
        LinkedHashSet<String> allowedTools = new LinkedHashSet<>(request.getAllowedToolNames());
        if (!registeredTools.containsAll(allowedTools)) {
            throw new BusinessException(CodeEnum.AI_TOOL_NOT_FOUND, "AI 策略包含未注册工具");
        }

        TAiAssistantPolicy policy = new TAiAssistantPolicy();
        policy.setEnabledTools(request.getEnabledTools());
        policy.setAllowedToolNames(writeTools(List.copyOf(allowedTools)));
        policy.setProposalsEnabled(request.getProposalsEnabled());
        policy.setMaxToolCallsPerRun(request.getMaxToolCallsPerRun());
        policy.setSafetyMode(request.getSafetyMode());
        policy.setNetworkMode(request.getNetworkMode());
        policy.setContextMessageLimit(request.getContextMessageLimit());
        policy.setSummaryMaxChars(request.getSummaryMaxChars());
        policy.setMaxRunSeconds(request.getMaxRunSeconds());
        policy.setEditBy(currentUserProvider.getCurrentUserId());
        int rows = mapper.updateIfVersionMatches(policy, request.getVersion());
        if (rows != 1) {
            throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "AI 策略已被其他管理员修改");
        }
        return toResponse(requirePolicy());
    }

    private TAiAssistantPolicy requirePolicy() {
        TAiAssistantPolicy policy = mapper.selectSingleton();
        if (policy != null) {
            return policy;
        }
        // 初始化脚本之外的空库也使用同一套安全默认值，避免工具被意外全部开放。
        LocalDateTime now = LocalDateTime.now();
        policy = new TAiAssistantPolicy();
        policy.setId(1L);
        policy.setEnabledTools(true);
        policy.setAllowedToolNames(writeTools(toolRegistry.definitions().stream()
                .map(definition -> definition.name()).toList()));
        policy.setProposalsEnabled(true);
        policy.setMaxToolCallsPerRun(8);
        policy.setSafetyMode("STRICT");
        policy.setNetworkMode("PROVIDER_ONLY");
        policy.setContextMessageLimit(8);
        policy.setSummaryMaxChars(2000);
        policy.setMaxRunSeconds(120);
        policy.setVersion(1);
        policy.setCreateTime(now);
        policy.setCreateBy(currentUserProvider.getCurrentUserId());
        policy.setEditTime(now);
        policy.setEditBy(currentUserProvider.getCurrentUserId());
        mapper.insert(policy);
        return policy;
    }

    private AiAssistantPolicyResponse toResponse(TAiAssistantPolicy policy) {
        AiAssistantPolicyResponse response = new AiAssistantPolicyResponse();
        response.setEnabledTools(policy.getEnabledTools());
        response.setAllowedToolNames(readTools(policy.getAllowedToolNames()));
        response.setProposalsEnabled(policy.getProposalsEnabled());
        response.setMaxToolCallsPerRun(policy.getMaxToolCallsPerRun());
        response.setSafetyMode(policy.getSafetyMode());
        response.setNetworkMode(policy.getNetworkMode());
        response.setContextMessageLimit(policy.getContextMessageLimit());
        response.setSummaryMaxChars(policy.getSummaryMaxChars());
        response.setMaxRunSeconds(policy.getMaxRunSeconds());
        response.setVersion(policy.getVersion());
        response.setEditTime(policy.getEditTime());
        response.setEditBy(policy.getEditBy());
        return response;
    }

    private List<String> readTools(String value) {
        try {
            return objectMapper.readValue(value == null ? "[]" : value, new TypeReference<>() { });
        } catch (JsonProcessingException ex) {
            throw new BusinessException(CodeEnum.SYSTEM_ERROR, "AI 策略工具配置损坏", ex);
        }
    }

    private String writeTools(List<String> tools) {
        try {
            return objectMapper.writeValueAsString(tools);
        } catch (JsonProcessingException ex) {
            throw new BusinessException(CodeEnum.SYSTEM_ERROR, "AI 策略工具配置失败", ex);
        }
    }
}
