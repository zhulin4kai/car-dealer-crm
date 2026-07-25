package com.autodealer.crm.modules.identity.application.internal;

import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.autodealer.crm.modules.identity.application.api.CredentialDeliveryPort;

import tools.jackson.databind.ObjectMapper;

/**
 * 生产凭证交付适配器。
 *
 * <p>原始凭证只发送给部署方显式配置的受信 HTTPS 通知服务，不进入数据库、HTTP 业务响应或日志。
 * 本机 HTTP 仅供人工联调，必须额外打开开关且目标必须解析为回环地址。
 */
@Component
@Profile("!dev & !test")
public class ProductionCredentialDeliveryAdapter implements CredentialDeliveryPort {
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Value("${security.credential-delivery.webhook-url:${CREDENTIAL_DELIVERY_WEBHOOK_URL:}}")
    private String webhookUrl;
    @Value("${security.credential-delivery.bearer-token:${CREDENTIAL_DELIVERY_BEARER_TOKEN:}}")
    private String bearerToken;
    @Value("${security.credential-delivery.allow-insecure-loopback:false}")
    private boolean allowInsecureLoopback;

    @Autowired
    public ProductionCredentialDeliveryAdapter(ObjectMapper objectMapper) {
        this(objectMapper, HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build());
    }

    ProductionCredentialDeliveryAdapter(ObjectMapper objectMapper, HttpClient httpClient) {
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
    }

    @Override
    public DeliveryStatus validateTarget(String phone, String email) {
        URI target = configuredTarget();
        if (target == null || bearerToken == null || bearerToken.length() < 32) {
            return new DeliveryStatus("CHANNEL_NOT_CONFIGURED");
        }
        if ((email == null || email.isBlank()) && (phone == null || phone.isBlank())) {
            return new DeliveryStatus("NO_DELIVERY_CONTACT");
        }
        return new DeliveryStatus("QUEUED");
    }

    @Override
    public DeliveryStatus deliver(DeliveryMessage message) {
        DeliveryStatus validation = validateTarget(message.phone(), message.email());
        if (!"QUEUED".equals(validation.code())) return validation;
        URI target = configuredTarget();
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("messageId", message.messageId());
            payload.put("userId", message.userId());
            payload.put("loginAct", message.loginAct());
            payload.put("phone", message.phone());
            payload.put("email", message.email());
            payload.put("purpose", message.purpose().name());
            payload.put("credential", message.rawCredential());
            payload.put("expiresAt", message.expiresAt().toString());
            HttpRequest request = HttpRequest.newBuilder(target)
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + bearerToken)
                    .header("Idempotency-Key", message.messageId())
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                    .build();
            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return new DeliveryStatus("WEBHOOK_DELIVERED");
            }
            if (response.statusCode() >= 400 && response.statusCode() < 500
                    && response.statusCode() != 408 && response.statusCode() != 429) {
                return new DeliveryStatus("WEBHOOK_PERMANENT_REJECTED");
            }
            throw new IllegalStateException("受信通知服务暂时拒绝凭证投递，HTTP " + response.statusCode());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("凭证投递被中断", exception);
        } catch (Exception exception) {
            throw new IllegalStateException("凭证投递失败", exception);
        }
    }

    private URI configuredTarget() {
        if (webhookUrl == null || webhookUrl.isBlank()) return null;
        try {
            URI target = URI.create(webhookUrl.trim());
            if ("https".equalsIgnoreCase(target.getScheme()) && target.getHost() != null) return target;
            if (!allowInsecureLoopback || !"http".equalsIgnoreCase(target.getScheme())
                    || target.getHost() == null) return null;
            return InetAddress.getByName(target.getHost()).isLoopbackAddress() ? target : null;
        } catch (Exception exception) {
            return null;
        }
    }
}
