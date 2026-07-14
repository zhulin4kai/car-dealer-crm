package com.autodealer.crm.modules.ai.application.api.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateAiProviderConfigRequest {
    @NotBlank
    @Size(max = 64)
    private String providerName;

    @NotBlank
    @Pattern(regexp = "OPENAI_COMPATIBLE|ANTHROPIC")
    private String providerFormat;

    @NotBlank
    @Size(max = 255)
    private String baseUrl;

    @NotBlank
    @Size(max = 128)
    private String modelName;

    @NotBlank
    @Size(max = 128)
    private String modelDisplayName;

    @NotBlank
    @Size(max = 500)
    private String apiKey;

    @Min(1)
    @Max(60)
    private Integer timeoutSeconds = 15;

    @Min(1)
    @Max(4096)
    private Integer maxOutputTokens = 512;

    @DecimalMin("0.0")
    @DecimalMax("2.0")
    private BigDecimal temperature = BigDecimal.valueOf(0.7);
}
