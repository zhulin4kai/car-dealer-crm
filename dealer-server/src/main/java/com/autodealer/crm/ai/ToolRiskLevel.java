package com.autodealer.crm.ai;

import com.autodealer.crm.ai.enums.AiRiskLevel;

public enum ToolRiskLevel {
    READONLY,
    LOW,
    MEDIUM,
    HIGH;

    public AiRiskLevel toAiRiskLevel() {
        return AiRiskLevel.valueOf(name());
    }
}
