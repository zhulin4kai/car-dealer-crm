package com.autodealer.crm.modules.identity.application.api.dto.organization;

import com.autodealer.crm.modules.identity.application.api.enums.ReportingType;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class EmployeeReportingResponse {
    private Integer managerEmployeeId;
    private String managerEmployeeName;
    private ReportingType relationType;
    private OffsetDateTime effectiveFrom;
    private OffsetDateTime effectiveTo;
}
