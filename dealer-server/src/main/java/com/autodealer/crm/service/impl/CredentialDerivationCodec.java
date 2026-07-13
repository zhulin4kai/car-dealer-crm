package com.autodealer.crm.service.impl;

import com.autodealer.crm.enums.CredentialPurpose;
import com.autodealer.crm.exception.BusinessException;
import com.autodealer.crm.result.CodeEnum;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/** 使用独立部署密钥从随机 nonce 确定性派生一次性凭证，支持幂等投递重试。 */
@Component
public class CredentialDerivationCodec {
    private static final SecureRandom RANDOM = new SecureRandom();
    private final String key;

    public CredentialDerivationCodec(
            @Value("${security.credential-delivery.derivation-key:${CREDENTIAL_DERIVATION_KEY:}}") String key) {
        this.key = key == null ? "" : key;
    }

    public void requireConfigured() {
        if (key.length() < 32) {
            throw new BusinessException(CodeEnum.CREDENTIAL_DELIVERY_FAILED,
                    "凭证派生密钥未配置或强度不足");
        }
    }

    public String newNonce() {
        requireConfigured();
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public String derive(String messageId, CredentialPurpose purpose, String nonce) {
        requireConfigured();
        byte[] bytes = hmac("CREDENTIAL:" + messageId + ":" + purpose.name() + ":" + nonce);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * 在业务事务中生成不可逆预交付承诺，不派生原始凭证。
     * 承诺与原始凭证使用不同的域分隔，Worker 会在提交后校验并将其 CAS 绑定为实际凭证摘要。
     */
    public String deliveryCommitment(String messageId, CredentialPurpose purpose, String nonce) {
        requireConfigured();
        return HexFormat.of().formatHex(hmac(
                "DELIVERY_COMMITMENT:" + messageId + ":" + purpose.name() + ":" + nonce));
    }

    public String contactDigest(String channel, String value) {
        if (value == null || value.isBlank()) return null;
        return HexFormat.of().formatHex(hmac("DELIVERY_CONTACT:" + channel + ":" + value.trim()));
    }

    public String securityAuditSourceDigest(String source) {
        requireConfigured();
        String value=source==null||source.isBlank()?"INTERNAL":source.trim();
        return HexFormat.of().formatHex(hmac("SECURITY_AUDIT_SOURCE:"+value));
    }

    private byte[] hmac(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
        } catch (Exception exception) {
            throw new IllegalStateException("凭证派生失败", exception);
        }
    }
}
