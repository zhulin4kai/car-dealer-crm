package com.autodealer.crm.query;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class CustomerQuery extends BaseQuery {

    private String customerName;

    private Long productId;
}
