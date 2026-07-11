package com.autodealer.crm.ai.service.impl;

import com.autodealer.crm.ai.dto.AiExecutionEventCommand;
import com.autodealer.crm.ai.enums.AiProposalStatus;
import com.autodealer.crm.ai.enums.AiResultStatus;
import com.autodealer.crm.ai.mapper.TAiActionProposalMapper;
import com.autodealer.crm.ai.model.TAiActionProposal;
import com.autodealer.crm.ai.service.AiTraceService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AiProposalFailureRecorder {
    private final TAiActionProposalMapper proposalMapper;
    private final AiTraceService traceService;

    public AiProposalFailureRecorder(TAiActionProposalMapper proposalMapper, AiTraceService traceService) {
        this.proposalMapper = proposalMapper;
        this.traceService = traceService;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void recordAfterRollback(TAiActionProposal proposal, String errorCode, String errorMessage) {
        int rows = proposalMapper.updateStatusIfCurrent(
                proposal.getId(),
                AiProposalStatus.PENDING_CONFIRMATION.name(),
                AiProposalStatus.FAILED.name(),
                errorCode,
                errorMessage);
        if (rows != 1) {
            return;
        }
        traceService.recordExecutionEvent(new AiExecutionEventCommand(
                proposal.getRunId(), proposal.getId(), "PROPOSAL_EXECUTE_FAILED",
                AiResultStatus.FAILED, proposal.getRelatedObjectType(), proposal.getRelatedObjectId(),
                "Proposal 执行失败", errorCode, LocalDateTime.now()));
    }
}
