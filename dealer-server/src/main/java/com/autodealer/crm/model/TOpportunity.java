package com.autodealer.crm.model;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class TOpportunity {
    private Long id;
    private String opportunityNo;
    private Integer customerId;
    private String customerName;
    private Long clueId;
    private Integer ownerId;
    private String ownerName;
    private Long productId;
    private String productName;
    private String sourceType;
    private String stage;
    private String requirement;
    private BigDecimal expectedAmount;
    private LocalDate expectedCloseDate;
    private LocalDate nextActionTime;
    private LocalDateTime lastFollowTime;
    private String lastFollowSummary;
    private String lostReason;
    private String lostCompetitor;
    private String resultRemark;
    private Integer orderTranId;
    private Integer version;
    private LocalDateTime createTime;
    private Integer createBy;
    private LocalDateTime updateTime;
    private Integer updateBy;
}
