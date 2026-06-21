package com.autodealer.crm.dto;

import com.autodealer.crm.query.BaseQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class UserListQuery extends BaseQuery {

    private String loginAct;

    private String name;

    private String phone;

    private String email;
}
