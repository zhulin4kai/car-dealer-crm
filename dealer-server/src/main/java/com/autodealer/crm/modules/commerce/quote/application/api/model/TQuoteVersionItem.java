package com.autodealer.crm.modules.commerce.quote.application.api.model;

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
    private String promotionCode;
    private String promotionName;
    private String promotionRuleSummary;
    private BigDecimal promotionAmount;
    private String promotionSnapshot;
    private LocalDateTime createTime;
    private Integer createBy;
}
