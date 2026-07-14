package com.autodealer.crm.modules.identity.application.api.dto;

import lombok.Data;

/** 负责人选择的最小只读投影，不暴露账号安全或授权集合。 */
@Data
public class OwnerCandidate {
    private Integer userId;
    private String name;
    private Integer employeeId;
    private String employeeNo;
    private Integer organizationUnitId;
    private String organizationName;
    private Integer positionId;
    private String positionName;
}
