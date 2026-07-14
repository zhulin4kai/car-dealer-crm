package com.autodealer.crm.modules.identity.application.api.dto.organization;

import com.autodealer.crm.modules.identity.application.api.enums.EmployeeStatus;
import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
public class EmployeeSummaryResponse {
    private Integer id;
    private Integer userId;
    private String employeeNo;
    private String name;
    private EmployeeStatus employmentStatus;
    private Integer organizationUnitId;
    private String organizationUnitName;
    private Integer positionId;
    private String positionName;
    private Integer managerEmployeeId;
    private String managerEmployeeName;
    private Integer version;
    private List<String> allowedActions = new ArrayList<>();
    private Map<String, String> unavailableReasons = new LinkedHashMap<>();
}
