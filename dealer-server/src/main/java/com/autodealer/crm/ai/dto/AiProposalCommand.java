package com.autodealer.crm.ai.dto;

import com.autodealer.crm.ai.enums.AiProposalType;
import com.autodealer.crm.ai.enums.AiRiskLevel;

import java.time.LocalDateTime;

public record AiProposalCommand(
        Long runId,
        AiProposalType proposalType,
        AiRiskLevel riskLevel,
        String permissionCode,
        String relatedObjectType,
        String relatedObjectId,
        String normalizedParams,
        String paramsHash,
        String paramsSummary,
        String impactSummary,
        LocalDateTime expiresTime
) {
}
