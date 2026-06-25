package com.autodealer.crm.model;

import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 交易产品关联表
 * t_tran_product
 */
@Data
public class TTranProduct implements Serializable {
    /**
     * 主键，自动增长，交易产品ID
     */
    private Integer id;

    /**
     * 交易ID
     */
    private Integer tranId;

    /**
     * 产品ID
     */
    private Long productId;

    /**
     * 产品名称（关联查询字段）
     */
    private String productName;

    /**
     * 产品编码快照
     */
    private String productSku;

    /**
     * 产品规格快照
     */
    private String productSpecification;

    /**
     * 指导价快照
     */
    private BigDecimal guidePrice;

    /**
     * 数量
     */
    private Integer quantity;

    /**
     * 单价
     */
    private BigDecimal price;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 创建人
     */
    private Integer createBy;

    private static final long serialVersionUID = 1L;
}
