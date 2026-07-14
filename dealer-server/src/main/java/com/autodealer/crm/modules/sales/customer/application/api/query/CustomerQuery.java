package com.autodealer.crm.modules.sales.customer.application.api.query;

import com.autodealer.crm.shared.pagination.BaseQuery;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class CustomerQuery extends BaseQuery {

    private String customerName;

    private Long productId;
}
