package com.autodealer.crm.modules.ai.application.api.dto;

import com.autodealer.crm.modules.ai.application.api.enums.AiResultStatus;
import com.autodealer.crm.modules.ai.application.api.enums.AiApprovalDecision;

import java.time.LocalDateTime;

public record AiApprovalCommand(
        Long runId,
        Long proposalId,
        AiApprovalDecision decision,
        String permissionSummary,
        String reason,
        AiResultStatus resultStatus,
        LocalDateTime approvedTime
) {
}
