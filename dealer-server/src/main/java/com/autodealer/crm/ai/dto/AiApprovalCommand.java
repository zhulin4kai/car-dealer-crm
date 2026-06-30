package com.autodealer.crm.ai.dto;

import com.autodealer.crm.ai.enums.AiResultStatus;
import com.autodealer.crm.ai.enums.AiApprovalDecision;

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
