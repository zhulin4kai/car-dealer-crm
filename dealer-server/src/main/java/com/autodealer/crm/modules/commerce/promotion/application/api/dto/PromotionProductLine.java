package com.autodealer.crm.modules.commerce.promotion.application.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class PromotionProductLine {
    private Long productId;
    private BigDecimal unitPrice;
    private Integer quantity;
}
