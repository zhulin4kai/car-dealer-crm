package com.autodealer.crm.modules.ai.application.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProviderRuntimeConfig {
    @JsonProperty("provider_config_no")
    private String providerConfigNo;

    @JsonProperty("provider_format")
    private String providerFormat;

    @JsonProperty("base_url")
    private String baseUrl;

    @JsonProperty("model_name")
    private String modelName;

    @JsonProperty("api_key")
    private String apiKey;

    @JsonProperty("timeout_seconds")
    private Integer timeoutSeconds;

    @JsonProperty("max_output_tokens")
    private Integer maxOutputTokens;

    private BigDecimal temperature;
}
