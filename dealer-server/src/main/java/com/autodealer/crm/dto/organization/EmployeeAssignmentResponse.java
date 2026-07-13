package com.autodealer.crm.dto.organization;

import com.autodealer.crm.enums.AssignmentType;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class EmployeeAssignmentResponse {
    private Integer id;
    private Integer organizationUnitId;
    private String organizationUnitName;
    private Integer positionId;
    private String positionName;
    private AssignmentType assignmentType;
    private OffsetDateTime effectiveFrom;
    private OffsetDateTime effectiveTo;
}
