package com.autodealer.crm.modules.ai.application.api;

import com.autodealer.crm.modules.ai.application.api.dto.AiToolExecutionResponse;
import com.autodealer.crm.modules.ai.application.api.dto.ExecuteAiToolRequest;

public interface AiInternalToolService {
    AiToolExecutionResponse execute(String toolName, ExecuteAiToolRequest request);
}
