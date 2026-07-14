package com.autodealer.crm.modules.ai.application.api.tool;

import java.util.Map;

public record ToolDefinition(
        String name,
        String description,
        String permissionCode,
        ToolRiskLevel riskLevel,
        boolean readOnly,
        boolean requiresConfirmation,
        int maxResults,
        String auditAction
) {
    public Map<String, Object> inputSchema() {
        return ToolSchemas.forTool(name);
    }
}
