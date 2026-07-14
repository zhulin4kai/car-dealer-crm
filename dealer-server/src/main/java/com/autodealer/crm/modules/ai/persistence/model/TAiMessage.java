package com.autodealer.crm.modules.ai.persistence.model;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class TAiMessage implements Serializable {
    private Long id;
    private String messageNo;
    private Long conversationId;
    private Long runId;
    private String role;
    private Integer sequenceNo;
    private Boolean visibleToUser;
    private String status;
    private Integer revisionNo;
    private Long supersedesMessageId;
    private Boolean includedInContext;
    private Integer version;
    private String contentSummary;
    private LocalDateTime createTime;
    private Integer createBy;
    private LocalDateTime editTime;
    private Integer editBy;
    private LocalDateTime withdrawnTime;
    private Integer withdrawnBy;

    private static final long serialVersionUID = 1L;
}
