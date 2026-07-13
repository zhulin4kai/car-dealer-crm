package com.autodealer.crm.dto.organization;

import com.autodealer.crm.enums.ReportingType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class ReportingInput {
    @NotNull private Integer managerEmployeeId;
    @NotNull private ReportingType relationType;
    @NotNull private OffsetDateTime effectiveFrom;
    private OffsetDateTime effectiveTo;
}
