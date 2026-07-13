package com.autodealer.crm.enums;

import java.util.Locale;

/**
 * 员工汇报关系状态。
 */
public enum ReportingStatus {
    PLANNED,
    ACTIVE,
    ENDED,
    CANCELLED;

    public static ReportingStatus parse(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("汇报关系状态不能为空");
        }
        return ReportingStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
