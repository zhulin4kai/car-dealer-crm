package com.autodealer.crm.model;

import lombok.Data;

@Data
public class TRolePermissionOrganization {
    private Integer roleId;
    private Integer permissionId;
    private Integer organizationUnitId;
}
