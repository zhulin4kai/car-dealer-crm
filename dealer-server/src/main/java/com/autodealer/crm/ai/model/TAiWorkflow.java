package com.autodealer.crm.ai.model;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class TAiWorkflow implements Serializable {
    private Long id;
    private String workflowNo;
    private Long runId;
    private Integer userId;
    private String workflowType;
    private String title;
    private String status;
    private Integer currentStepNo;
    private String contextObjectType;
    private String contextObjectId;
    private String pauseReason;
    private String errorCode;
    private String errorMessage;
    private LocalDateTime startedTime;
    private LocalDateTime pausedTime;
    private LocalDateTime resumedTime;
    private LocalDateTime completedTime;
    private LocalDateTime expiresTime;
    private LocalDateTime createTime;
    private Integer createBy;
    private LocalDateTime editTime;
    private Integer editBy;

    private static final long serialVersionUID = 1L;
}
