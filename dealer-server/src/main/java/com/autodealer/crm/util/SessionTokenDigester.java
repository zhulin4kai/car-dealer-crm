package com.autodealer.crm.util;

import org.springframework.stereotype.Component;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

@Component
public class SessionTokenDigester {
    private final byte[] secret;
    public SessionTokenDigester() {
        String configured = System.getenv("JWT_SECRET");
        if (configured == null || configured.isBlank()) {
            throw new IllegalStateException("JWT_SECRET 环境变量未配置，无法生成会话摘要");
        }
        this.secret = configured.getBytes(StandardCharsets.UTF_8);
    }
    public String digest(String token) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret,"HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(("session-token:"+token).getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) { throw new IllegalStateException("会话摘要失败",exception); }
    }
    public boolean matches(String token,String expectedDigest) {
        if (token == null || expectedDigest == null) return false;
        return MessageDigest.isEqual(digest(token).getBytes(StandardCharsets.US_ASCII),
                expectedDigest.getBytes(StandardCharsets.US_ASCII));
    }
}
