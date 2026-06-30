package com.autodealer.crm.ai.dto;

import com.autodealer.crm.ai.enums.AiResultStatus;

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
