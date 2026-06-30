package com.autodealer.crm.ai.dto;

import lombok.Data;

@Data
public class AiProviderConfigTestResponse {
    private String configNo;
    private String testStatus;
    private String message;
    private String errorCode;
}
