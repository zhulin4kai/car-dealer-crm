package com.autodealer.crm.modules.ai.application.api.dto;

import lombok.Data;

@Data
public class AiProposalConfirmResponse {
    private Long proposalId;
    private String status;
    private String resultSummary;
    private String objectType;
    private String objectId;
}
