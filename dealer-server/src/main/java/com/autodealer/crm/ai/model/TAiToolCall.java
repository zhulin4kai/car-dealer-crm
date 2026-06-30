package com.autodealer.crm.ai.model;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class TAiToolCall implements Serializable {
    private Long id;
    private Long runId;
    private String toolName;
    private String permissionCode;
    private String riskLevel;
    private String inputSummary;
    private String outputSummary;
    private String objectRefs;
    private String displayPayloadJson;
    private String resultStatus;
    private String errorCode;
    private Integer durationMs;
    private LocalDateTime startedTime;
    private LocalDateTime completedTime;
    private Integer createBy;

    private static final long serialVersionUID = 1L;
}
