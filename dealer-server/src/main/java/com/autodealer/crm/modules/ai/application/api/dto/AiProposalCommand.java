package com.autodealer.crm.modules.ai.application.api.dto;

import com.autodealer.crm.modules.ai.application.api.enums.AiProposalType;
import com.autodealer.crm.modules.ai.application.api.enums.AiRiskLevel;

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
