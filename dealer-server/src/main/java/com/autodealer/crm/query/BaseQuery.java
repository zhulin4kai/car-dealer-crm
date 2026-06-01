package com.autodealer.crm.query;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class BaseQuery {

    private String token; //jwt

    private String filterSQL; //数据权限的SQL过滤条件： tu.id = 2 或者 ta.owner_id = 2

    private Integer current = 1;
    private Integer pageSize = 10;
}
