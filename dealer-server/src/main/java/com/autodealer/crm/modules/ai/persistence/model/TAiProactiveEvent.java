package com.autodealer.crm.modules.ai.persistence.model;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class TAiProactiveEvent implements Serializable {
    private Long id;
    private String eventNo;
    private Long subscriptionId;
    private Integer userId;
    private String eventType;
    private String status;
    private String title;
    private String summary;
    private String detailSummary;
    private String objectType;
    private String objectId;
    private String severity;
    private LocalDateTime generatedTime;
    private LocalDateTime deliveredTime;
    private String errorCode;
    private LocalDateTime createTime;
    private Integer createBy;

    private static final long serialVersionUID = 1L;
}
