package com.autodealer.crm.enums;

import java.util.Locale;

public enum CommunicationMethod {
    PHONE,
    STORE_VISIT,
    WECHAT,
    SMS,
    EMAIL,
    OTHER;

    public static CommunicationMethod parse(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("沟通方式不能为空");
        }
        return CommunicationMethod.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
