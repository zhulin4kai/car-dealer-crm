package com.autodealer.crm.modules.identity.application.api.enums;

import java.util.Locale;

/**
 * 员工任职类型。
 */
public enum AssignmentType {
    PRIMARY,
    SECONDARY,
    ACTING;

    public static AssignmentType parse(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("任职类型不能为空");
        }
        return AssignmentType.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
