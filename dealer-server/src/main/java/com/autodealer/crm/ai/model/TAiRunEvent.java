package com.autodealer.crm.ai.model;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class TAiRunEvent implements Serializable {
    private Long id;
    private Long runId;
    private String eventId;
    private Integer sequenceNo;
    private String eventType;
    private String payloadJson;
    private LocalDateTime occurredTime;
    private LocalDateTime createTime;

    private static final long serialVersionUID = 1L;
}
