package com.autodealer.crm.ai.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiSensitiveDataSanitizerTest {

    private final AiSensitiveDataSanitizer sanitizer = new AiSensitiveDataSanitizer();

    @Test
    void sanitize_sensitiveTokenPhoneAndBankNumber_masksOriginalValues() {
        String text = "Authorization: Bearer abc.secret 13812345678 6222021234567890123 Cookie: sid=raw";

        String result = sanitizer.sanitize(text, 500);

        assertFalse(result.contains("abc.secret"));
        assertFalse(result.contains("13812345678"));
        assertFalse(result.contains("6222021234567890123"));
        assertFalse(result.contains("sid=raw"));
        assertTrue(result.contains("138****5678"));
        assertTrue(result.contains("6222********0123"));
    }

    @Test
    void sanitizeDisplayText_preservesMarkdownLineBreaksAndMasksSensitiveValues() {
        String text = "# 标题\n\n- 客户电话 13812345678\n- Authorization: Bearer abc.secret";

        String result = sanitizer.sanitizeDisplayText(text, 500);

        assertTrue(result.contains("# 标题\n\n- 客户电话 138****5678\n- Authorization: Bearer [REDACTED]"));
        assertFalse(result.contains("13812345678"));
        assertFalse(result.contains("abc.secret"));
    }
}
