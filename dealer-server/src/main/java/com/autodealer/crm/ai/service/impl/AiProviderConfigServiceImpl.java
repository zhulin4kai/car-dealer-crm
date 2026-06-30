package com.autodealer.crm.ai.service.impl;

import com.autodealer.crm.ai.dto.AiProviderConfigResponse;
import com.autodealer.crm.ai.dto.AiProviderConfigTestResponse;
import com.autodealer.crm.ai.dto.CreateAiProviderConfigRequest;
import com.autodealer.crm.ai.dto.ProviderRuntimeConfig;
import com.autodealer.crm.ai.dto.RotateAiProviderKeyRequest;
import com.autodealer.crm.ai.dto.UpdateAiProviderConfigRequest;
import com.autodealer.crm.ai.enums.AiProviderFormat;
import com.autodealer.crm.ai.enums.AiProviderTestStatus;
import com.autodealer.crm.ai.mapper.TAiProviderConfigMapper;
import com.autodealer.crm.ai.model.TAiProviderConfig;
import com.autodealer.crm.ai.service.AiProviderConfigService;
import com.autodealer.crm.ai.service.AiProviderKeyCipher;
import com.autodealer.crm.ai.service.AiSensitiveDataSanitizer;
import com.autodealer.crm.config.security.CurrentUserProvider;
import com.autodealer.crm.exception.BusinessException;
import com.autodealer.crm.result.CodeEnum;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AiProviderConfigServiceImpl implements AiProviderConfigService {
    private static final int TEST_MAX_OUTPUT_TOKENS = 16;
    private static final int TEST_TIMEOUT_SECONDS = 15;

    private final TAiProviderConfigMapper mapper;
    private final AiProviderKeyCipher keyCipher;
    private final CurrentUserProvider currentUserProvider;
    private final AiSensitiveDataSanitizer sanitizer;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public AiProviderConfigServiceImpl(TAiProviderConfigMapper mapper,
                                       AiProviderKeyCipher keyCipher,
                                       CurrentUserProvider currentUserProvider,
                                       AiSensitiveDataSanitizer sanitizer,
                                       ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.keyCipher = keyCipher;
        this.currentUserProvider = currentUserProvider;
        this.sanitizer = sanitizer;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();
    }

    @Override
    public List<AiProviderConfigResponse> list() {
        return mapper.selectAll().stream().map(AiProviderConfigResponse::from).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiProviderConfigResponse create(CreateAiProviderConfigRequest request) {
        validateBaseUrl(request.getBaseUrl());
        AiProviderKeyCipher.EncryptedApiKey encryptedApiKey = keyCipher.encrypt(request.getApiKey());
        LocalDateTime now = LocalDateTime.now();
        Integer userId = currentUserProvider.getCurrentUserId();
        TAiProviderConfig config = new TAiProviderConfig();
        config.setConfigNo("AIPC" + UUID.randomUUID().toString().replace("-", ""));
        config.setProviderName(sanitizer.sanitize(request.getProviderName(), 64));
        config.setProviderFormat(request.getProviderFormat());
        config.setBaseUrl(normalizeBaseUrl(request.getBaseUrl()));
        config.setModelName(sanitizer.sanitize(request.getModelName(), 128));
        config.setModelDisplayName(sanitizer.sanitize(request.getModelDisplayName(), 128));
        config.setEncryptedApiKey(encryptedApiKey.encryptedApiKey());
        config.setApiKeyNonce(encryptedApiKey.apiKeyNonce());
        config.setMaskedApiKey(maskApiKey(request.getApiKey()));
        config.setEnabled(false);
        config.setTestStatus(AiProviderTestStatus.UNTESTED.name());
        config.setTimeoutSeconds(safeTimeout(request.getTimeoutSeconds()));
        config.setMaxOutputTokens(safeMaxOutputTokens(request.getMaxOutputTokens()));
        config.setTemperature(safeTemperature(request.getTemperature()));
        config.setCreateTime(now);
        config.setCreateBy(userId);
        config.setEditTime(now);
        config.setEditBy(userId);
        requireOne(mapper.insert(config), "AI 模型配置写入失败");
        return AiProviderConfigResponse.from(config);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiProviderConfigResponse update(String configNo, UpdateAiProviderConfigRequest request) {
        validateBaseUrl(request.getBaseUrl());
        TAiProviderConfig config = getConfig(configNo);
        config.setProviderName(sanitizer.sanitize(request.getProviderName(), 64));
        config.setProviderFormat(request.getProviderFormat());
        config.setBaseUrl(normalizeBaseUrl(request.getBaseUrl()));
        config.setModelName(sanitizer.sanitize(request.getModelName(), 128));
        config.setModelDisplayName(sanitizer.sanitize(request.getModelDisplayName(), 128));
        config.setTimeoutSeconds(safeTimeout(request.getTimeoutSeconds()));
        config.setMaxOutputTokens(safeMaxOutputTokens(request.getMaxOutputTokens()));
        config.setTemperature(safeTemperature(request.getTemperature()));
        config.setEditBy(currentUserProvider.getCurrentUserId());
        requireOne(mapper.updateBaseFields(config), "AI 模型配置更新失败");
        return AiProviderConfigResponse.from(getConfig(configNo));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiProviderConfigResponse rotateKey(String configNo, RotateAiProviderKeyRequest request) {
        TAiProviderConfig config = getConfig(configNo);
        AiProviderKeyCipher.EncryptedApiKey encryptedApiKey = keyCipher.encrypt(request.getApiKey());
        requireOne(mapper.updateApiKey(config.getId(),
                encryptedApiKey.encryptedApiKey(),
                encryptedApiKey.apiKeyNonce(),
                maskApiKey(request.getApiKey()),
                currentUserProvider.getCurrentUserId()), "AI 模型密钥轮换失败");
        return AiProviderConfigResponse.from(getConfig(configNo));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiProviderConfigTestResponse test(String configNo) {
        TAiProviderConfig config = getConfig(configNo);
        AiProviderConfigTestResponse response = new AiProviderConfigTestResponse();
        response.setConfigNo(config.getConfigNo());
        try {
            testProviderConnection(config);
            mapper.updateTestResult(config.getId(), AiProviderTestStatus.SUCCESS.name(),
                    null, "连接成功", currentUserProvider.getCurrentUserId());
            response.setTestStatus(AiProviderTestStatus.SUCCESS.name());
            response.setMessage("连接成功");
            return response;
        } catch (BusinessException ex) {
            mapper.updateTestResult(config.getId(), AiProviderTestStatus.FAILED.name(),
                    ex.getCodeEnum().name(), ex.getMessage(), currentUserProvider.getCurrentUserId());
            response.setTestStatus(AiProviderTestStatus.FAILED.name());
            response.setErrorCode(ex.getCodeEnum().name());
            response.setMessage(ex.getMessage());
            return response;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiProviderConfigResponse activate(String configNo) {
        TAiProviderConfig config = mapper.selectByConfigNoForUpdate(configNo);
        if (config == null) {
            throw new BusinessException(CodeEnum.AI_PROVIDER_CONFIG_NOT_FOUND, "AI 模型配置不存在");
        }
        Integer userId = currentUserProvider.getCurrentUserId();
        mapper.disableAll(userId);
        requireOne(mapper.updateEnabled(config.getId(), true, userId), "AI 模型配置启用失败");
        return AiProviderConfigResponse.from(getConfig(configNo));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiProviderConfigResponse disable(String configNo) {
        TAiProviderConfig config = getConfig(configNo);
        requireOne(mapper.updateEnabled(config.getId(), false, currentUserProvider.getCurrentUserId()),
                "AI 模型配置停用失败");
        return AiProviderConfigResponse.from(getConfig(configNo));
    }

    @Override
    public ProviderRuntimeConfig getEnabledRuntimeConfig() {
        TAiProviderConfig config = mapper.selectEnabled();
        if (config == null) {
            throw new BusinessException(CodeEnum.AI_PROVIDER_CONFIG_REQUIRED, "AI 模型配置缺失");
        }
        if (!Boolean.TRUE.equals(config.getEnabled())) {
            throw new BusinessException(CodeEnum.AI_PROVIDER_CONFIG_DISABLED, "AI 模型配置未启用");
        }
        ProviderRuntimeConfig runtimeConfig = new ProviderRuntimeConfig();
        runtimeConfig.setProviderConfigNo(config.getConfigNo());
        runtimeConfig.setProviderFormat(config.getProviderFormat());
        runtimeConfig.setBaseUrl(config.getBaseUrl());
        runtimeConfig.setModelName(config.getModelName());
        runtimeConfig.setApiKey(keyCipher.decrypt(config.getEncryptedApiKey(), config.getApiKeyNonce()));
        runtimeConfig.setTimeoutSeconds(config.getTimeoutSeconds());
        runtimeConfig.setMaxOutputTokens(config.getMaxOutputTokens());
        runtimeConfig.setTemperature(config.getTemperature());
        return runtimeConfig;
    }

    private TAiProviderConfig getConfig(String configNo) {
        TAiProviderConfig config = mapper.selectByConfigNo(configNo);
        if (config == null) {
            throw new BusinessException(CodeEnum.AI_PROVIDER_CONFIG_NOT_FOUND, "AI 模型配置不存在");
        }
        return config;
    }

    private void testProviderConnection(TAiProviderConfig config) {
        String apiKey = keyCipher.decrypt(config.getEncryptedApiKey(), config.getApiKeyNonce());
        int timeoutSeconds = Math.min(safeTimeout(config.getTimeoutSeconds()), TEST_TIMEOUT_SECONDS);
        int maxTokens = Math.min(safeMaxOutputTokens(config.getMaxOutputTokens()), TEST_MAX_OUTPUT_TOKENS);
        try {
            HttpRequest request = buildTestRequest(config, apiKey, timeoutSeconds, maxTokens);
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BusinessException(CodeEnum.AI_PROVIDER_CONFIG_TEST_FAILED, "AI 模型配置测试失败");
            }
        } catch (IOException ex) {
            throw new BusinessException(CodeEnum.AI_PROVIDER_CONFIG_TEST_FAILED, "AI 模型配置测试失败", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new BusinessException(CodeEnum.AI_PROVIDER_CONFIG_TEST_FAILED, "AI 模型配置测试失败", ex);
        }
    }

    private HttpRequest buildTestRequest(TAiProviderConfig config,
                                         String apiKey,
                                         int timeoutSeconds,
                                         int maxTokens) throws JsonProcessingException {
        AiProviderFormat format = AiProviderFormat.valueOf(config.getProviderFormat());
        if (format == AiProviderFormat.OPENAI_COMPATIBLE) {
            Map<String, Object> body = Map.of(
                    "model", config.getModelName(),
                    "messages", List.of(Map.of("role", "user", "content", "ping")),
                    "stream", false,
                    "max_tokens", maxTokens,
                    "temperature", config.getTemperature());
            return HttpRequest.newBuilder(endpoint(config.getBaseUrl(), "/chat/completions"))
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();
        }
        if (format == AiProviderFormat.ANTHROPIC) {
            Map<String, Object> body = Map.of(
                    "model", config.getModelName(),
                    "messages", List.of(Map.of("role", "user", "content", "ping")),
                    "max_tokens", maxTokens,
                    "temperature", config.getTemperature());
            return HttpRequest.newBuilder(endpoint(config.getBaseUrl(), "/v1/messages"))
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .header("Content-Type", "application/json")
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", "2023-06-01")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();
        }
        throw new BusinessException(CodeEnum.AI_PROVIDER_UNSUPPORTED_FORMAT, "AI 模型协议不支持");
    }

    private URI endpoint(String baseUrl, String path) {
        return URI.create(normalizeBaseUrl(baseUrl) + path);
    }

    private void validateBaseUrl(String value) {
        URI uri = URI.create(value);
        String scheme = uri.getScheme();
        if (!("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "AI 模型 Base URL 必须是 HTTP 或 HTTPS");
        }
    }

    private String normalizeBaseUrl(String value) {
        String trimmed = value.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    private int safeTimeout(Integer value) {
        return Math.max(1, Math.min(value == null ? 15 : value, 60));
    }

    private int safeMaxOutputTokens(Integer value) {
        return Math.max(1, Math.min(value == null ? 512 : value, 4096));
    }

    private BigDecimal safeTemperature(BigDecimal value) {
        if (value == null) {
            return BigDecimal.valueOf(0.7);
        }
        return value.max(BigDecimal.ZERO).min(BigDecimal.valueOf(2));
    }

    private String maskApiKey(String apiKey) {
        if (!StringUtils.hasText(apiKey)) {
            return "";
        }
        String trimmed = apiKey.trim();
        if (trimmed.length() <= 8) {
            return "****";
        }
        return trimmed.substring(0, 4) + "****" + trimmed.substring(trimmed.length() - 4);
    }

    private void requireOne(int rows, String message) {
        if (rows != 1) {
            throw new BusinessException(CodeEnum.OPERATION_FAILED, message);
        }
    }
}
