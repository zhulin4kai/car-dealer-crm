package com.autodealer.crm.web;

import com.autodealer.crm.ai.dto.AiAssistantPolicyResponse;
import com.autodealer.crm.ai.dto.UpdateAiAssistantPolicyRequest;
import com.autodealer.crm.ai.service.AiAssistantPolicyService;
import com.autodealer.crm.constant.PermissionCodes;
import com.autodealer.crm.result.R;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai/policy")
public class AiAssistantPolicyController {
    private final AiAssistantPolicyService policyService;

    public AiAssistantPolicyController(AiAssistantPolicyService policyService) {
        this.policyService = policyService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('" + PermissionCodes.AI_POLICY_VIEW + "')")
    public R<AiAssistantPolicyResponse> getPolicy() {
        return R.OK(policyService.getPolicy());
    }

    @PutMapping
    @PreAuthorize("hasAuthority('" + PermissionCodes.AI_POLICY_MANAGE + "')")
    public R<AiAssistantPolicyResponse> updatePolicy(
            @Valid @RequestBody UpdateAiAssistantPolicyRequest request) {
        return R.OK(policyService.updatePolicy(request));
    }
}
