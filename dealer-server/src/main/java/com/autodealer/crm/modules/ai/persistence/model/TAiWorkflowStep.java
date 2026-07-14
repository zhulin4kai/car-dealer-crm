package com.autodealer.crm.modules.ai.persistence.model;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class TAiWorkflowStep implements Serializable {
    private Long id;
    private Long workflowId;
    private Integer stepNo;
    private String stepType;
    private String title;
    private String status;
    private String toolName;
    private Long proposalId;
    private String inputSummary;
    private String outputSummary;
    private String errorCode;
    private LocalDateTime startedTime;
    private LocalDateTime completedTime;
    private LocalDateTime createTime;
    private Integer createBy;
    private LocalDateTime editTime;
    private Integer editBy;

    private static final long serialVersionUID = 1L;
}
