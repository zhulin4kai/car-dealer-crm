package com.autodealer.crm.modules.identity.persistence.model;

import com.autodealer.crm.modules.identity.application.api.enums.DataScopeCode;
import lombok.Data;

import java.io.Serializable;

/**
 * 角色权限关系事实。
 */
@Data
public class TRolePermission implements Serializable {
    private Integer roleId;
    private Integer permissionId;
    private Boolean delegable;
    private DataScopeCode dataScopeCode;

    private static final long serialVersionUID = 1L;
}
