package com.autodealer.crm.ai.model;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class TAiApproval implements Serializable {
    private Long id;
    private Long runId;
    private Long proposalId;
    private String decision;
    private String permissionSummary;
    private String reason;
    private String resultStatus;
    private LocalDateTime approvedTime;
    private Integer approvedBy;

    private static final long serialVersionUID = 1L;
}
