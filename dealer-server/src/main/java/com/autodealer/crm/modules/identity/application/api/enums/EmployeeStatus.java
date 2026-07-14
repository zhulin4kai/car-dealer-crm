package com.autodealer.crm.modules.identity.application.api.enums;

import java.util.Locale;

/**
 * 员工任职生命周期状态。
 */
public enum EmployeeStatus {
    PENDING,
    ACTIVE,
    HANDOVER,
    LEFT;

    public static EmployeeStatus parse(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("员工状态不能为空");
        }
        return EmployeeStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
