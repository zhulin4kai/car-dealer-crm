package com.autodealer.crm.ai.dto;

import lombok.Data;

@Data
public class AiProposalConfirmResponse {
    private Long proposalId;
    private String status;
    private String resultSummary;
    private String objectType;
    private String objectId;
}
