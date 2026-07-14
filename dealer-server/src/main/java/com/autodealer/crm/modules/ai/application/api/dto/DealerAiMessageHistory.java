package com.autodealer.crm.modules.ai.application.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class DealerAiMessageHistory {
    private String role;

    @JsonProperty("content_summary")
    private String contentSummary;
}
