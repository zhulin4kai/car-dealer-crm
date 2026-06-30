package com.autodealer.crm.ai.service;

import com.autodealer.crm.ai.dto.AiToolExecutionResponse;
import com.autodealer.crm.ai.dto.ExecuteAiToolRequest;

public interface AiInternalToolService {
    AiToolExecutionResponse execute(String toolName, ExecuteAiToolRequest request);
}
