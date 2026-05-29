package com.bjpowernode.query;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class TranProductQuery extends BaseQuery {
    private Integer id;
    private Integer tranId;
    private Integer productId;
    private String productName;
    private Integer quantity;
    private java.math.BigDecimal price;
    private java.util.Date createTime;
    private Integer createBy;
} 