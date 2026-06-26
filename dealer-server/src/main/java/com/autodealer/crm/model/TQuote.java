package com.autodealer.crm.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TQuote {
    private Long id;
    private String quoteNo;
    private Integer customerId;
    private Long opportunityId;
    private Long currentVersionId;
    private String status;
    private String remark;
    private LocalDateTime createTime;
    private Integer createBy;
    private LocalDateTime updateTime;
    private Integer updateBy;
}
