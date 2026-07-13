package com.autodealer.crm.enums;

import java.util.Locale;

/**
 * 员工任职记录状态。
 */
public enum AssignmentStatus {
    PLANNED,
    ACTIVE,
    ENDED,
    CANCELLED;

    public static AssignmentStatus parse(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("任职记录状态不能为空");
        }
        return AssignmentStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
