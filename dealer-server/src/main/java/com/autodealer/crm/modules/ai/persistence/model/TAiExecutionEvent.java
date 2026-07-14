package com.autodealer.crm.modules.ai.persistence.model;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class TAiExecutionEvent implements Serializable {
    private Long id;
    private Long runId;
    private Long proposalId;
    private String eventType;
    private String resultStatus;
    private String objectType;
    private String objectId;
    private String summary;
    private String errorCode;
    private LocalDateTime occurredTime;
    private Integer createBy;

    private static final long serialVersionUID = 1L;
}
