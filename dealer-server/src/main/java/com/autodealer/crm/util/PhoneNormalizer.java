package com.autodealer.crm.util;

import java.util.regex.Pattern;

public final class PhoneNormalizer {

    private static final Pattern MAINLAND_MOBILE = Pattern.compile("^1[3-9]\\d{9}$");
    private static final Pattern COMMON_SEPARATORS = Pattern.compile("[\\s\\-()（）]+");

    private PhoneNormalizer() {
    }

    public static String normalizeMainlandMobile(String phone) {
        if (phone == null) {
            return null;
        }
        String trimmed = phone.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return COMMON_SEPARATORS.matcher(trimmed).replaceAll("");
    }

    public static boolean isMainlandMobile(String phone) {
        return phone != null && MAINLAND_MOBILE.matcher(phone).matches();
    }
}
