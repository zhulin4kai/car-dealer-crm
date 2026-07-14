package com.autodealer.crm.modules.identity.application.api.dto.organization;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.OffsetDateTime;

/** 单条立即生效、有限期限的代理管理关系。 */
@Data
public class ActingReportingInput {
    @NotNull private Integer managerEmployeeId;
    @NotNull @Future private OffsetDateTime effectiveTo;
}
