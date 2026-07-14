package com.autodealer.crm.modules.sales.activity.application.api.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ActivityRoiResponse {
    private Integer activityId;
    private String activityName;
    private String status;
    private BigDecimal plannedCost;
    private BigDecimal actualCost;
    private Integer clueCount;
    private Integer validClueCount;
    private Integer customerCount;
    private Integer opportunityCount;
    private Integer testDriveCount;
    private Integer quoteCount;
    private Integer orderCount;
    private BigDecimal dealAmount;
    private BigDecimal roi;
}
