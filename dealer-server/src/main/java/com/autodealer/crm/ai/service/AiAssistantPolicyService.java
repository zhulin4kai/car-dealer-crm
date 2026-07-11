package com.autodealer.crm.ai.service;

import com.autodealer.crm.ai.dto.AiAssistantPolicyResponse;
import com.autodealer.crm.ai.dto.UpdateAiAssistantPolicyRequest;

public interface AiAssistantPolicyService {
    AiAssistantPolicyResponse getPolicy();

    AiAssistantPolicyResponse updatePolicy(UpdateAiAssistantPolicyRequest request);
}
