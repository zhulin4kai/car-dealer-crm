package com.autodealer.crm.modules.commerce.quote.application.api.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TQuoteStatusHistory {
    private Long id;
    private Long quoteId;
    private String fromStatus;
    private String toStatus;
    private String reason;
    private String confirmedByName;
    private LocalDateTime confirmedAt;
    private String confirmationMethod;
    private String confirmationEvidence;
    private String proxyConfirmReason;
    private LocalDateTime createTime;
    private Integer createBy;
}
