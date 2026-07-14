package com.autodealer.crm.modules.ai.web;

import com.autodealer.crm.modules.ai.application.api.dto.AiAssistantPolicyResponse;
import com.autodealer.crm.modules.ai.application.api.dto.UpdateAiAssistantPolicyRequest;
import com.autodealer.crm.modules.ai.application.api.AiAssistantPolicyService;
import com.autodealer.crm.shared.security.PermissionCodes;
import com.autodealer.crm.shared.web.Result;
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
    public Result<AiAssistantPolicyResponse> getPolicy() {
        return Result.OK(policyService.getPolicy());
    }

    @PutMapping
    @PreAuthorize("hasAuthority('" + PermissionCodes.AI_POLICY_MANAGE + "')")
    public Result<AiAssistantPolicyResponse> updatePolicy(
            @Valid @RequestBody UpdateAiAssistantPolicyRequest request) {
        return Result.OK(policyService.updatePolicy(request));
    }
}
