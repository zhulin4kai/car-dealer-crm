package com.autodealer.crm.modules.commerce.quote.application.api.model;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class TQuoteVersion {
    private Long id;
    private Long quoteId;
    private Integer versionNo;
    private LocalDateTime validUntil;
    private BigDecimal totalAmount;
    private String remark;
    private LocalDateTime createTime;
    private Integer createBy;
}
