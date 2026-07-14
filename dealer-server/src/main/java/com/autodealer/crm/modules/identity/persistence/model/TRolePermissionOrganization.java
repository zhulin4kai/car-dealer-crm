package com.autodealer.crm.modules.identity.persistence.model;

import lombok.Data;

@Data
public class TRolePermissionOrganization {
    private Integer roleId;
    private Integer permissionId;
    private Integer organizationUnitId;
}
