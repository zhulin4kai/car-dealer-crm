package com.autodealer.crm.modules.commerce.promotion.application.api.model;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class TProductPromotion {
    private Long id;
    private Long productId;
    private String code;
    private String name;
    private String type;
    private BigDecimal discount;
    private String ruleSummary;
    private String applicableStore;
    private String customerType;
    private String applicableChannel;
    private String inventoryScope;
    private Boolean stackable;
    private Integer priority;
    private BigDecimal budgetLimit;
    private BigDecimal usedBudget;
    private Integer usageLimit;
    private Integer usedCount;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;
    private String pauseReason;
    private String endReason;
    private String voidReason;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
