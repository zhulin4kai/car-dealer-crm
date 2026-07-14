package com.autodealer.crm.modules.ai.application.api;

import com.autodealer.crm.modules.ai.application.api.tool.ToolExecutionContext;
import com.autodealer.crm.modules.ai.application.api.dto.AiProposalConfirmResponse;
import com.autodealer.crm.modules.ai.application.api.dto.tool.AiToolDtos;

public interface AiProposalService {
    AiToolDtos.ProposalCreated createCommunicationRecordProposal(
            ToolExecutionContext context,
            AiToolDtos.CreateCommunicationRecordProposalRequest request);

    AiToolDtos.ProposalCreated createFollowTaskProposal(
            ToolExecutionContext context,
            AiToolDtos.CreateFollowTaskProposalRequest request);

    AiProposalConfirmResponse confirm(Long proposalId);

    AiProposalConfirmResponse reject(Long proposalId);
}
