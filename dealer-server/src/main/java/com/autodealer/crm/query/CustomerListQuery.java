package com.autodealer.crm.query;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 客户列表查询条件，只包含筛选和分页，不包含命令字段。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CustomerListQuery extends BaseQuery {

    private String customerName;

    private Long productId;
}
