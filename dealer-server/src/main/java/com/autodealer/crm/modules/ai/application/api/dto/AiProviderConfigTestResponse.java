package com.autodealer.crm.modules.ai.application.api.dto;

import lombok.Data;

@Data
public class AiProviderConfigTestResponse {
    private String configNo;
    private String testStatus;
    private String message;
    private String errorCode;
}
