package com.autodealer.crm.dto;

import com.autodealer.crm.query.BaseQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class UserListQuery extends BaseQuery {

    @com.fasterxml.jackson.annotation.JsonIgnore
    private java.util.List<Integer> dataScopeVisibleUserIds;
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Boolean dataScopeDenied;

    @com.fasterxml.jackson.annotation.JsonIgnore
    private String sortColumn;

    @com.fasterxml.jackson.annotation.JsonIgnore
    private String sortOrder;

    private String keyword;

    private Integer organizationUnitId;

    private Integer positionId;

    private Integer managerEmployeeId;

    private Integer roleId;

    private String employmentStatus;

    private String accountStatus;

    private String lockStatus;

    private String sortBy;

    private String sortDirection;

    private String loginAct;

    private String name;

    private String phone;

    private String email;
}
