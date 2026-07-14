package com.autodealer.crm.modules.identity.application.api.dto.organization;

import com.autodealer.crm.modules.identity.application.api.enums.OrganizationUnitType;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class OrganizationUnitResponse {
    private Integer id;
    private String code;
    private String name;
    private OrganizationUnitType type;
    private Integer parentId;
    private Integer leaderEmployeeId;
    private String leaderEmployeeName;
    private Integer orderNo;
    private Boolean enabled;
    private Integer version;
    private Integer employeeCount;
    private List<OrganizationUnitResponse> children = new ArrayList<>();
}
