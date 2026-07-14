package com.autodealer.crm.modules.ai.web;

import com.autodealer.crm.modules.ai.application.api.dto.AiProposalConfirmResponse;
import com.autodealer.crm.modules.ai.application.api.AiProposalService;
import com.autodealer.crm.shared.security.PermissionCodes;
import com.autodealer.crm.shared.web.Result;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai/proposals")
public class AiProposalController {
    private final AiProposalService proposalService;

    public AiProposalController(AiProposalService proposalService) {
        this.proposalService = proposalService;
    }

    @PostMapping("/{proposalId}/confirm")
    @PreAuthorize("hasAuthority('" + PermissionCodes.AI_PROPOSAL_CONFIRM + "')")
    public Result<AiProposalConfirmResponse> confirm(@PathVariable Long proposalId) {
        return Result.OK(proposalService.confirm(proposalId));
    }

    @PostMapping("/{proposalId}/reject")
    @PreAuthorize("hasAuthority('" + PermissionCodes.AI_PROPOSAL_CONFIRM + "')")
    public Result<AiProposalConfirmResponse> reject(@PathVariable Long proposalId) {
        return Result.OK(proposalService.reject(proposalId));
    }
}
