package com.autodealer.crm.modules.ai.application.api;

import com.autodealer.crm.modules.ai.application.api.dto.AiAssistantPolicyResponse;
import com.autodealer.crm.modules.ai.application.api.dto.UpdateAiAssistantPolicyRequest;

public interface AiAssistantPolicyService {
    AiAssistantPolicyResponse getPolicy();

    AiAssistantPolicyResponse updatePolicy(UpdateAiAssistantPolicyRequest request);
}
