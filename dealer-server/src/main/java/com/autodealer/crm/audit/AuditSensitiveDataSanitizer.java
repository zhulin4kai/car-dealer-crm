package com.autodealer.crm.audit;

import java.util.regex.Pattern;

/**
 * 审计文本的最后一道敏感数据清理边界。
 */
public final class AuditSensitiveDataSanitizer {
    private static final Pattern SENSITIVE_KEY_VALUE = Pattern.compile(
            "(?i)([\\\"']?(?:password|loginPwd|login_pwd|passwordHash|password_hash|pwdHash|pwd_hash|hash|"
                    + "token|accessToken|access_token|refreshToken|refresh_token|jwt|authorization|"
                    + "phone|mobile|email|secret|clientSecret|client_secret|apiKey|api_key|"
                    + "credential|credentials|credentialHash|credential_hash|digest|key|signature|salt|nonce)[\\\"']?\\s*[:=]\\s*)"
                    + "(\\\"(?:\\\\.|[^\\\"])*\\\"|'(?:\\\\.|[^'])*'|[^,}\\s]+)");
    private static final Pattern BCRYPT_HASH = Pattern.compile("\\$2[aby]\\$\\d{2}\\$[./A-Za-z0-9]{53}");
    private static final Pattern BEARER_TOKEN = Pattern.compile("(?i)Bearer\\s+[^\\s,}\\\"]+");
    private static final Pattern JWT_TOKEN = Pattern.compile("eyJ[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+");
    private static final Pattern FULL_PHONE = Pattern.compile("(?<!\\d)1[3-9]\\d{9}(?!\\d)");
    private static final Pattern FULL_EMAIL = Pattern.compile(
            "(?i)(?<![A-Z0-9._%+-])[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}(?![A-Z0-9._%+-])");
    private static final Pattern IPV4 = Pattern.compile(
            "(?<![\\d.])(?:25[0-5]|2[0-4]\\d|1?\\d?\\d)(?:\\.(?:25[0-5]|2[0-4]\\d|1?\\d?\\d)){3}(?![\\d.])");
    private static final Pattern IPV6 = Pattern.compile(
            "(?i)(?<![0-9a-f:])(?:[0-9a-f]{1,4}:){2,7}[0-9a-f]{0,4}(?![0-9a-f:])");
    private static final Pattern HEX_DIGEST = Pattern.compile("(?i)(?<![0-9a-f])[0-9a-f]{32,}(?![0-9a-f])");

    private AuditSensitiveDataSanitizer() {
    }

    public static String sanitize(String value) {
        if (value == null) {
            return null;
        }
        String sanitized = SENSITIVE_KEY_VALUE.matcher(value).replaceAll("$1\\\"[REDACTED]\\\"");
        sanitized = BCRYPT_HASH.matcher(sanitized).replaceAll("[REDACTED_HASH]");
        sanitized = BEARER_TOKEN.matcher(sanitized).replaceAll("Bearer [REDACTED]");
        sanitized = JWT_TOKEN.matcher(sanitized).replaceAll("[REDACTED_TOKEN]");
        sanitized = HEX_DIGEST.matcher(sanitized).replaceAll("[REDACTED_DIGEST]");
        sanitized = FULL_PHONE.matcher(sanitized).replaceAll("[REDACTED_PHONE]");
        sanitized = FULL_EMAIL.matcher(sanitized).replaceAll("[REDACTED_EMAIL]");
        sanitized = IPV4.matcher(sanitized).replaceAll("[REDACTED_IP]");
        return IPV6.matcher(sanitized).replaceAll("[REDACTED_IP]");
    }
}
