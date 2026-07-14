package com.autodealer.crm.modules.identity.application.api.dto.organization;

import com.autodealer.crm.modules.identity.application.api.enums.ReportingStatus;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class ActingReportingRelationResponse {
    private Integer id;
    private Integer version;
    private Integer managerEmployeeId;
    private String managerEmployeeNo;
    private String managerEmployeeName;
    private ReportingStatus status;
    private OffsetDateTime effectiveFrom;
    private OffsetDateTime effectiveTo;
}
