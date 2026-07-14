package com.autodealer.crm.modules.ai.persistence.model;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class TAiRun implements Serializable {
    private Long id;
    private String runNo;
    private Long conversationId;
    private String conversationNo;
    private Long parentRunId;
    private Integer turnNo;
    private Integer userId;
    private String userName;
    private String entryPoint;
    private String contextObjectType;
    private String contextObjectId;
    private String promptSummary;
    private String status;
    private String errorCode;
    private String errorMessage;
    private LocalDateTime startedTime;
    private LocalDateTime completedTime;
    private LocalDateTime expiresTime;
    private Boolean contextActive;
    private String invalidationReason;
    private LocalDateTime createTime;
    private Integer createBy;

    private static final long serialVersionUID = 1L;
}
