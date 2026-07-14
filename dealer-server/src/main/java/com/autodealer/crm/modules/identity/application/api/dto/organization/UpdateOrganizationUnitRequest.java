package com.autodealer.crm.modules.identity.application.api.dto.organization;

import com.autodealer.crm.modules.identity.application.api.enums.OrganizationUnitType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateOrganizationUnitRequest {
    @NotBlank @Size(max = 64) private String name;
    @NotNull private OrganizationUnitType type;
    private Integer parentId;
    private Integer leaderEmployeeId;
    @NotNull @Min(0) private Integer orderNo;
    @NotNull @Min(0) private Integer expectedVersion;
}
