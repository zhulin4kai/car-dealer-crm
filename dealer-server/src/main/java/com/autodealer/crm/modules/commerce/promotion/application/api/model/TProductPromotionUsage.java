package com.autodealer.crm.modules.commerce.promotion.application.api.model;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class TProductPromotionUsage {
    private Long id;
    private Long promotionId;
    private String sourceType;
    private Long sourceId;
    private BigDecimal discountAmount;
    private LocalDateTime createTime;
    private Integer createBy;
}
