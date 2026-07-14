package com.autodealer.crm.modules.identity.application.api.dto.organization;

import com.autodealer.crm.modules.identity.application.api.enums.ReportingType;
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
