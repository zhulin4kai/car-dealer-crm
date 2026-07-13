package com.autodealer.crm.enums;

import java.util.Locale;

/**
 * 登录账号类型。
 */
public enum AccountType {
    SYSTEM,
    HUMAN;

    public static AccountType parse(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("账号类型不能为空");
        }
        return AccountType.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
