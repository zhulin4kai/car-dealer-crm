package com.autodealer.crm.ai.model;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class TAiProactiveSubscription implements Serializable {
    private Long id;
    private String subscriptionNo;
    private Integer userId;
    private String subscriptionType;
    private String status;
    private String frequency;
    private String quietStartTime;
    private String quietEndTime;
    private Integer dailyLimit;
    private Integer maxResults;
    private Integer duplicateWindowMinutes;
    private String configSummary;
    private LocalDateTime lastTriggeredTime;
    private LocalDateTime nextTriggerTime;
    private LocalDateTime createTime;
    private Integer createBy;
    private LocalDateTime editTime;
    private Integer editBy;

    private static final long serialVersionUID = 1L;
}
