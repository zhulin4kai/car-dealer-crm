package com.autodealer.crm.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.Map;

@Data
public class DealerAiEventResponse {
    @JsonProperty("event_id")
    private String eventId;

    @JsonProperty("run_id")
    private String runId;

    private Integer sequence;

    @JsonProperty("event_type")
    private String eventType;

    @JsonProperty("occurred_at")
    private OffsetDateTime occurredAt;

    private Map<String, Object> payload;
}
