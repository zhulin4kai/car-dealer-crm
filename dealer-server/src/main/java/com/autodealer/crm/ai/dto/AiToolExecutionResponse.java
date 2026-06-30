package com.autodealer.crm.ai.dto;

import lombok.Data;

@Data
public class AiToolExecutionResponse {
    private String toolName;
    private String resultStatus;
    private Object data;
    private String outputSummary;
    private String objectRefs;
}
