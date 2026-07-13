package com.autodealer.crm.dto.organization;

import com.autodealer.crm.enums.AssignmentType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class AssignmentInput {
    @NotNull private Integer organizationUnitId;
    @NotNull private Integer positionId;
    @NotNull private AssignmentType assignmentType;
    @NotNull private OffsetDateTime effectiveFrom;
    private OffsetDateTime effectiveTo;
}
