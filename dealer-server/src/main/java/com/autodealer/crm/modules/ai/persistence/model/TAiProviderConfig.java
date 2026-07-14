package com.autodealer.crm.modules.ai.persistence.model;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class TAiProviderConfig implements Serializable {
    private Long id;
    private String configNo;
    private String providerName;
    private String providerFormat;
    private String baseUrl;
    private String modelName;
    private String modelDisplayName;
    private String encryptedApiKey;
    private String apiKeyNonce;
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
    private Integer createBy;
    private LocalDateTime editTime;
    private Integer editBy;

    private static final long serialVersionUID = 1L;
}
