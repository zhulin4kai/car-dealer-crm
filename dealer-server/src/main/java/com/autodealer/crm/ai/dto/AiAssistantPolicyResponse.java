package com.autodealer.crm.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class AiAssistantPolicyResponse {
    private Boolean enabledTools;
    private List<String> allowedToolNames;
    private Boolean proposalsEnabled;
    private Integer maxToolCallsPerRun;
    private String safetyMode;
    private String networkMode;
    private Integer contextMessageLimit;
    private Integer summaryMaxChars;
    private Integer maxRunSeconds;
    private Integer version;
    private LocalDateTime editTime;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private Integer editBy;
}
