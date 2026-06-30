package com.autodealer.crm.ai.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
public class AiSseEventResponse {
    private String eventId;
    private String runNo;
    private Integer sequence;
    private String type;
    private LocalDateTime occurredAt;
    private Map<String, Object> payload;
}
