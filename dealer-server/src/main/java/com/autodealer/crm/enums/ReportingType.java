package com.autodealer.crm.enums;

import java.util.Locale;

/**
 * 员工汇报关系类型。
 */
public enum ReportingType {
    DIRECT,
    ACTING;

    public static ReportingType parse(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("汇报关系类型不能为空");
        }
        return ReportingType.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
