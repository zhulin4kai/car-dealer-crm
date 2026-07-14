package com.autodealer.crm.modules.fulfillment.transaction.application.api.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class SettlementPreviewResponse {

    private Integer tranId;
    private Long promotionId;
    private BigDecimal originalAmount;
    private BigDecimal discountAmount;
    private BigDecimal finalAmount;
    private Integer transactionVersion;
    private String pricingFingerprint;
    private PromotionInfo promotion;

    @Data
    public static class PromotionInfo {
        private Long id;
        private String code;
        private String name;
        private String type;
        private BigDecimal discount;
        private String ruleSummary;
        private Long productId;
        private String startTime;
        private String endTime;
        private String updateTime;
    }
}
