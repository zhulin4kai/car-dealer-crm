package com.autodealer.crm.modules.ai.application.api.dto;

import com.autodealer.crm.modules.ai.application.api.enums.AiResultStatus;

import java.time.LocalDateTime;

public record AiExecutionEventCommand(
        Long runId,
        Long proposalId,
        String eventType,
        AiResultStatus resultStatus,
        String objectType,
        String objectId,
        String summary,
        String errorCode,
        LocalDateTime occurredTime
) {
}
