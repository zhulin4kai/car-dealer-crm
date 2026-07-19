package com.autodealer.crm.modules.identity.persistence.model;

import com.autodealer.crm.modules.identity.application.api.enums.OrganizationUnitType;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 组织单元持久化对象，对应 t_organization_unit。
 */
@Data
public class TOrganizationUnit implements Serializable {
    private Integer id;
    private String code;
    private String name;
    private OrganizationUnitType type;
    private Integer parentId;
    private Integer leaderEmployeeId;
    private Integer orderNo;
    private Boolean placeholder;
    private Boolean enabled;
    private Integer version;
    private LocalDateTime createTime;
    private Integer createBy;
    private LocalDateTime editTime;
    private Integer editBy;

    private static final long serialVersionUID = 1L;
}
