package com.autodealer.crm.ai;

import java.util.Map;

public interface ToolExecutor {
    ToolDefinition definition();

    ToolExecutionResult execute(ToolExecutionContext context, Map<String, Object> arguments);
}
