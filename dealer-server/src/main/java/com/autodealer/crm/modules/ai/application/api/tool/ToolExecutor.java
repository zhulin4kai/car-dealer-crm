package com.autodealer.crm.modules.ai.application.api.tool;

import java.util.Map;

public interface ToolExecutor {
    ToolDefinition definition();

    ToolExecutionResult execute(ToolExecutionContext context, Map<String, Object> arguments);
}
