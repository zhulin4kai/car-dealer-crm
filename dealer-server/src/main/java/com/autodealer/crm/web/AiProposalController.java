package com.autodealer.crm.web;

import com.autodealer.crm.ai.dto.AiProposalConfirmResponse;
import com.autodealer.crm.ai.service.AiProposalService;
import com.autodealer.crm.constant.PermissionCodes;
import com.autodealer.crm.result.R;
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
    public R<AiProposalConfirmResponse> confirm(@PathVariable Long proposalId) {
        return R.OK(proposalService.confirm(proposalId));
    }

    @PostMapping("/{proposalId}/reject")
    @PreAuthorize("hasAuthority('" + PermissionCodes.AI_PROPOSAL_CONFIRM + "')")
    public R<AiProposalConfirmResponse> reject(@PathVariable Long proposalId) {
        return R.OK(proposalService.reject(proposalId));
    }
}
