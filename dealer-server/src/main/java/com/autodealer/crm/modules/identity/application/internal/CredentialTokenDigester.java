package com.autodealer.crm.modules.identity.application.internal;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

/** 一次性凭证的统一 HMAC 摘要器；签发后的绑定与消费查询必须共用同一算法和密钥。 */
@Component
public class CredentialTokenDigester {
    private final byte[] key;

    public CredentialTokenDigester(
            @Value("${security.credential-token.digest-key:${JWT_SECRET:}}") String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalStateException("凭证摘要密钥未配置");
        }
        this.key = key.getBytes(StandardCharsets.UTF_8);
    }

    public String digest(String rawCredential) {
        if (rawCredential == null) {
            throw new IllegalArgumentException("原始凭证不能为空");
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(rawCredential.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("凭证摘要失败", exception);
        }
    }
}
