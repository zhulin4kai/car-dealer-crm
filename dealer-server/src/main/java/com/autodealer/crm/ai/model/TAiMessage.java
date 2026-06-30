package com.autodealer.crm.ai.model;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class TAiMessage implements Serializable {
    private Long id;
    private Long conversationId;
    private Long runId;
    private String role;
    private Integer sequenceNo;
    private Boolean visibleToUser;
    private String contentSummary;
    private LocalDateTime createTime;
    private Integer createBy;

    private static final long serialVersionUID = 1L;
}
