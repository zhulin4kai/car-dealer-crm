package com.autodealer.crm.dto.organization;

import com.autodealer.crm.enums.ReportingType;
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
