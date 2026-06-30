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
public class CreateCommunicationRecordProposalToolExecutor implements ToolExecutor {
    private static final ToolDefinition DEFINITION = new ToolDefinition(
            "create_communication_record_proposal",
            "创建沟通记录提议，用户确认前不写入业务沟通记录",
            PermissionCodes.COMMUNICATION_RECORD_CREATE,
            ToolRiskLevel.LOW,
            false,
            true,
            1,
            "AI_PROPOSAL_CREATE_COMMUNICATION_RECORD");

    private final AiToolArgumentBinder argumentBinder;
    private final AiProposalService proposalService;

    public CreateCommunicationRecordProposalToolExecutor(AiToolArgumentBinder argumentBinder,
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
        AiToolDtos.CreateCommunicationRecordProposalRequest request =
                argumentBinder.bind(arguments, AiToolDtos.CreateCommunicationRecordProposalRequest.class);
        AiToolDtos.ProposalCreated proposal = proposalService.createCommunicationRecordProposal(context, request);
        return ToolExecutionResult.of(proposal, "已生成沟通记录提议", "AI_PROPOSAL:" + proposal.proposalId());
    }
}
