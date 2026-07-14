package com.autodealer.crm.modules.ai.application.api.dto;

import com.autodealer.crm.modules.ai.application.api.enums.AiResultStatus;
import com.autodealer.crm.modules.ai.application.api.enums.AiRiskLevel;

import java.time.LocalDateTime;

public record AiToolCallCommand(
        Long runId,
        String toolName,
        String permissionCode,
        AiRiskLevel riskLevel,
        String inputSummary,
        String outputSummary,
        String objectRefs,
        String displayPayloadJson,
        AiResultStatus resultStatus,
        String errorCode,
        Integer durationMs,
        LocalDateTime startedTime,
        LocalDateTime completedTime
) {
}
