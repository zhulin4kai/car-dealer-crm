package com.autodealer.crm.modules.ai.application.internal;

import com.autodealer.crm.modules.ai.application.api.tool.ToolExecutionContext;
import com.autodealer.crm.modules.ai.application.api.tool.ToolExecutionResult;
import com.autodealer.crm.modules.ai.application.api.tool.ToolRegistry;
import com.autodealer.crm.modules.ai.application.api.dto.AiToolExecutionResponse;
import com.autodealer.crm.modules.ai.application.api.dto.ExecuteAiToolRequest;
import com.autodealer.crm.modules.ai.persistence.mapper.TAiRunMapper;
import com.autodealer.crm.modules.ai.persistence.mapper.TAiToolCallMapper;
import com.autodealer.crm.modules.ai.persistence.model.TAiRun;
import com.autodealer.crm.modules.ai.application.api.AiInternalToolService;
import com.autodealer.crm.modules.ai.application.api.AiAssistantPolicyService;
import com.autodealer.crm.shared.error.BusinessException;
import com.autodealer.crm.modules.identity.application.api.model.TUser;
import com.autodealer.crm.shared.error.CodeEnum;
import com.autodealer.crm.modules.identity.application.api.UserService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AiInternalToolServiceImpl implements AiInternalToolService {
    private final TAiRunMapper runMapper;
    private final UserService userService;
    private final ToolRegistry toolRegistry;
    private final TAiToolCallMapper toolCallMapper;
    private final AiAssistantPolicyService policyService;

    public AiInternalToolServiceImpl(TAiRunMapper runMapper,
                                     UserService userService,
                                     ToolRegistry toolRegistry,
                                     TAiToolCallMapper toolCallMapper,
                                     AiAssistantPolicyService policyService) {
        this.runMapper = runMapper;
        this.userService = userService;
        this.toolRegistry = toolRegistry;
        this.toolCallMapper = toolCallMapper;
        this.policyService = policyService;
    }

    @Override
    public AiToolExecutionResponse execute(String toolName, ExecuteAiToolRequest request) {
        TAiRun run = runMapper.selectByRunNo(request.getRunNo());
        if (run == null) {
            throw new BusinessException(CodeEnum.AI_RUN_NOT_FOUND, "AI Run 不存在");
        }
        if (!"RUNNING".equals(run.getStatus())
                || !Boolean.TRUE.equals(run.getContextActive())
                || (run.getExpiresTime() != null && run.getExpiresTime().isBefore(LocalDateTime.now()))) {
            throw new BusinessException(CodeEnum.AI_TOOL_FORBIDDEN, "AI Run 当前不允许执行工具");
        }
        var policy = policyService.getPolicy();
        var definition = toolRegistry.getDefinition(toolName);
        if (!Boolean.TRUE.equals(policy.getEnabledTools())
                || !policy.getAllowedToolNames().contains(toolName)
                || (definition.requiresConfirmation() && !Boolean.TRUE.equals(policy.getProposalsEnabled()))) {
            throw new BusinessException(CodeEnum.AI_TOOL_FORBIDDEN, "AI 策略未允许该工具");
        }
        if (toolCallMapper.countByRunId(run.getId()) >= policy.getMaxToolCallsPerRun()) {
            throw new BusinessException(CodeEnum.AI_TOOL_FORBIDDEN, "AI Run 工具调用次数已达上限");
        }
        TUser runUser = userService.getLoginUserById(run.getUserId());
        if (runUser == null || !runUser.isEnabled()) {
            throw new BusinessException(CodeEnum.AI_TOOL_FORBIDDEN, "AI 工具无权限");
        }

        return executeAsRunUser(runUser, () -> {
            ToolExecutionResult result = toolRegistry.execute(
                    new ToolExecutionContext(run),
                    toolName,
                    request.getArguments());
            AiToolExecutionResponse response = new AiToolExecutionResponse();
            response.setToolName(toolName);
            response.setResultStatus("SUCCESS");
            response.setData(result.data());
            response.setOutputSummary(result.outputSummary());
            response.setObjectRefs(result.objectRefs());
            return response;
        });
    }

    private AiToolExecutionResponse executeAsRunUser(TUser user, java.util.function.Supplier<AiToolExecutionResponse> action) {
        SecurityContext previousContext = SecurityContextHolder.getContext();
        SecurityContext runContext = SecurityContextHolder.createEmptyContext();
        Authentication authentication = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        runContext.setAuthentication(authentication);
        try {
            SecurityContextHolder.setContext(runContext);
            return action.get();
        } finally {
            SecurityContextHolder.setContext(previousContext);
        }
    }
}
