package com.autodealer.crm.dto.organization;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class OrganizationChangeHistoryResponse {
    private Long id;
    private String changeType;
    private String beforeSummary;
    private String afterSummary;
    private String reason;
    private String operatorName;
    private OffsetDateTime createTime;
}
