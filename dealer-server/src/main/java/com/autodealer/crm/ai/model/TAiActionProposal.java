package com.autodealer.crm.ai.model;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class TAiActionProposal implements Serializable {
    private Long id;
    private Long runId;
    private String proposalType;
    private String status;
    private String riskLevel;
    private String permissionCode;
    private String relatedObjectType;
    private String relatedObjectId;
    private String normalizedParams;
    private String paramsHash;
    private String paramsSummary;
    private String impactSummary;
    private LocalDateTime expiresTime;
    private LocalDateTime confirmedTime;
    private LocalDateTime executedTime;
    private String resultSummary;
    private String errorCode;
    private LocalDateTime createTime;
    private Integer createBy;

    private static final long serialVersionUID = 1L;
}
