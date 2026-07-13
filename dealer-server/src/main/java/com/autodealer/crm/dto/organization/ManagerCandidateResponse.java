package com.autodealer.crm.dto.organization;

import lombok.Data;

@Data
public class ManagerCandidateResponse {
    private Integer employeeId;
    private String employeeNo;
    private String name;
    private String organizationUnitName;
    private String positionName;
}
