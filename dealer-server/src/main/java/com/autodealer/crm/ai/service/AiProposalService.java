package com.autodealer.crm.ai.service;

import com.autodealer.crm.ai.ToolExecutionContext;
import com.autodealer.crm.ai.dto.AiProposalConfirmResponse;
import com.autodealer.crm.ai.dto.tool.AiToolDtos;

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
