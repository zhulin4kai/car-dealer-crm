package com.autodealer.crm.model;

import com.autodealer.crm.enums.DataScopeCode;
import com.autodealer.crm.enums.RoleScopeType;
import lombok.Data;

import java.io.Serializable;

/**
 * 角色表
 * t_role
 */
@Data
public class TRole implements Serializable {
    private Integer id;

    private String role;

    private String roleName;

    private String description;

    private Boolean protectedRole;

    private Integer authorizationLevel;

    private DataScopeCode defaultDataScope;

    private RoleScopeType scopeType;

    private Integer enabled;

    private Integer version;

    private static final long serialVersionUID = 1L;
}
