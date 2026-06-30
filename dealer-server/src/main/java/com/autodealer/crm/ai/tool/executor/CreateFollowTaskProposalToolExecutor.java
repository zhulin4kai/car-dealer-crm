package com.autodealer.crm.ai.tool.executor;

import com.autodealer.crm.ai.ToolDefinition;
import com.autodealer.crm.ai.ToolExecutionContext;
import com.autodealer.crm.ai.ToolExecutionResult;
import com.autodealer.crm.ai.ToolExecutor;
import com.autodealer.crm.ai.ToolRiskLevel;
import com.autodealer.crm.ai.dto.tool.AiToolDtos;
import com.autodealer.crm.ai.service.AiProposalService;
import com.autodealer.crm.ai.tool.AiToolArgumentBinder;
import com.autodealer.crm.constant.PermissionCodes;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class CreateFollowTaskProposalToolExecutor implements ToolExecutor {
    private static final ToolDefinition DEFINITION = new ToolDefinition(
            "create_follow_task_proposal",
            "创建跟进任务提议，用户确认前不写入业务跟进任务",
            PermissionCodes.FOLLOW_TASK_CREATE,
            ToolRiskLevel.LOW,
            false,
            true,
            1,
            "AI_PROPOSAL_CREATE_FOLLOW_TASK");

    private final AiToolArgumentBinder argumentBinder;
    private final AiProposalService proposalService;

    public CreateFollowTaskProposalToolExecutor(AiToolArgumentBinder argumentBinder,
                                                AiProposalService proposalService) {
        this.argumentBinder = argumentBinder;
        this.proposalService = proposalService;
    }

    @Override
    public ToolDefinition definition() {
        return DEFINITION;
    }

    @Override
    public ToolExecutionResult execute(ToolExecutionContext context, Map<String, Object> arguments) {
        AiToolDtos.CreateFollowTaskProposalRequest request =
                argumentBinder.bind(arguments, AiToolDtos.CreateFollowTaskProposalRequest.class);
        AiToolDtos.ProposalCreated proposal = proposalService.createFollowTaskProposal(context, request);
        return ToolExecutionResult.of(proposal, "已生成跟进任务提议", "AI_PROPOSAL:" + proposal.proposalId());
    }
}
