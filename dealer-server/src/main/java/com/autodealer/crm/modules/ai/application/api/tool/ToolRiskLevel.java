package com.autodealer.crm.modules.ai.application.api.tool;

import com.autodealer.crm.modules.ai.application.api.enums.AiRiskLevel;

public enum ToolRiskLevel {
    READONLY,
    LOW,
    MEDIUM,
    HIGH;

    public AiRiskLevel toAiRiskLevel() {
        return AiRiskLevel.valueOf(name());
    }
}
