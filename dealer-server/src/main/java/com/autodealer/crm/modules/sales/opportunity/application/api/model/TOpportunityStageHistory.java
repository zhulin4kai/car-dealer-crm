package com.autodealer.crm.modules.sales.opportunity.application.api.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TOpportunityStageHistory {
    private Long id;
    private Long opportunityId;
    private String fromStage;
    private String toStage;
    private String reason;
    private Integer operateBy;
    private LocalDateTime operateTime;
}
