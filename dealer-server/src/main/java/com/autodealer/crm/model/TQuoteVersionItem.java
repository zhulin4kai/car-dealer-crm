package com.autodealer.crm.model;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class TQuoteVersionItem {
    private Long id;
    private Long quoteVersionId;
    private Long productId;
    private String productSku;
    private String productName;
    private String productSpecification;
    private BigDecimal guidePrice;
    private BigDecimal unitPrice;
    private Integer quantity;
    private BigDecimal lineAmount;
    private Long promotionId;
    private String promotionName;
    private BigDecimal promotionAmount;
    private LocalDateTime createTime;
    private Integer createBy;
}
