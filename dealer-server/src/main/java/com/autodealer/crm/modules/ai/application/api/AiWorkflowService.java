package com.autodealer.crm.modules.ai.application.api;

import com.autodealer.crm.modules.ai.application.api.dto.AiWorkflowActionRequest;
import com.autodealer.crm.modules.ai.application.api.dto.AiWorkflowResponse;
import com.autodealer.crm.modules.ai.application.api.dto.CreateAiWorkflowRequest;

import java.util.List;

public interface AiWorkflowService {
    AiWorkflowResponse create(CreateAiWorkflowRequest request);

    AiWorkflowResponse get(String workflowNo);

    List<AiWorkflowResponse> listByRun(String runNo);

    AiWorkflowResponse pause(String workflowNo, AiWorkflowActionRequest request);

    AiWorkflowResponse resume(String workflowNo);

    AiWorkflowResponse cancel(String workflowNo, AiWorkflowActionRequest request);

    AiWorkflowResponse complete(String workflowNo);

    AiWorkflowResponse fail(String workflowNo, AiWorkflowActionRequest request);
}
