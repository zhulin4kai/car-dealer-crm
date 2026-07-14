package com.autodealer.crm.modules.ai.application.internal;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class AiSensitiveDataSanitizer {
    private static final Pattern AUTHORIZATION_BEARER_PATTERN = Pattern.compile(
            "(?i)(Authorization\\s*[:=]\\s*Bearer\\s+)([^\\s,;]+)");
    private static final Pattern AUTHORIZATION_PATTERN = Pattern.compile(
            "(?i)(Authorization\\s*[:=]\\s*)(?!Bearer\\s+)([^\\s,;]+)");
    private static final Pattern BEARER_PATTERN = Pattern.compile("(?i)Bearer\\s+[A-Za-z0-9._\\-]+");
    private static final Pattern COOKIE_PATTERN = Pattern.compile("(?i)(Cookie\\s*[:=]\\s*)([^\\n\\r]+)");
    private static final Pattern PHONE_PATTERN = Pattern.compile("(?<!\\d)(1[3-9]\\d)\\d{4}(\\d{4})(?!\\d)");
    private static final Pattern LONG_NUMBER_PATTERN = Pattern.compile("(?<!\\d)(\\d{4})\\d{8,}(\\d{4})(?!\\d)");

    public String sanitize(String text, int maxLength) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        String sanitized = text.replaceAll("[\\r\\n\\t]+", " ").trim();
        return sanitizePatterns(sanitized, maxLength);
    }

    public String sanitizeDisplayText(String text, int maxLength) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        String sanitized = text
                .replace("\r\n", "\n")
                .replace('\r', '\n')
                .replace('\t', ' ');
        return sanitizePatterns(sanitized, maxLength);
    }

    private String sanitizePatterns(String text, int maxLength) {
        String sanitized = text;
        sanitized = AUTHORIZATION_BEARER_PATTERN.matcher(sanitized).replaceAll("$1[REDACTED]");
        sanitized = AUTHORIZATION_PATTERN.matcher(sanitized).replaceAll("$1[REDACTED]");
        sanitized = BEARER_PATTERN.matcher(sanitized).replaceAll("Bearer [REDACTED]");
        sanitized = COOKIE_PATTERN.matcher(sanitized).replaceAll("$1[REDACTED]");
        sanitized = maskPhones(sanitized);
        sanitized = maskLongNumbers(sanitized);
        return truncate(sanitized, maxLength);
    }

    private String maskPhones(String value) {
        Matcher matcher = PHONE_PATTERN.matcher(value);
        return matcher.replaceAll("$1****$2");
    }

    private String maskLongNumbers(String value) {
        Matcher matcher = LONG_NUMBER_PATTERN.matcher(value);
        return matcher.replaceAll("$1********$2");
    }

    private String truncate(String value, int maxLength) {
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, Math.max(0, maxLength - 3)) + "...";
    }
}
