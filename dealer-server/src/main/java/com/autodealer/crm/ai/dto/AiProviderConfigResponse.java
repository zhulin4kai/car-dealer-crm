package com.autodealer.crm.ai.dto;

import com.autodealer.crm.ai.model.TAiProviderConfig;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AiProviderConfigResponse {
    private String configNo;
    private String providerName;
    private String providerFormat;
    private String baseUrl;
    private String modelName;
    private String modelDisplayName;
    private Boolean hasApiKey;
    private String maskedApiKey;
    private Boolean enabled;
    private String testStatus;
    private LocalDateTime lastTestTime;
    private String lastTestErrorCode;
    private String lastTestMessage;
    private Integer timeoutSeconds;
    private Integer maxOutputTokens;
    private BigDecimal temperature;
    private LocalDateTime createTime;
    private LocalDateTime editTime;

    public static AiProviderConfigResponse from(TAiProviderConfig config) {
        AiProviderConfigResponse response = new AiProviderConfigResponse();
        response.setConfigNo(config.getConfigNo());
        response.setProviderName(config.getProviderName());
        response.setProviderFormat(config.getProviderFormat());
        response.setBaseUrl(config.getBaseUrl());
        response.setModelName(config.getModelName());
        response.setModelDisplayName(config.getModelDisplayName());
        response.setHasApiKey(config.getEncryptedApiKey() != null && !config.getEncryptedApiKey().isBlank());
        response.setMaskedApiKey(config.getMaskedApiKey());
        response.setEnabled(config.getEnabled());
        response.setTestStatus(config.getTestStatus());
        response.setLastTestTime(config.getLastTestTime());
        response.setLastTestErrorCode(config.getLastTestErrorCode());
        response.setLastTestMessage(config.getLastTestMessage());
        response.setTimeoutSeconds(config.getTimeoutSeconds());
        response.setMaxOutputTokens(config.getMaxOutputTokens());
        response.setTemperature(config.getTemperature());
        response.setCreateTime(config.getCreateTime());
        response.setEditTime(config.getEditTime());
        return response;
    }
}
