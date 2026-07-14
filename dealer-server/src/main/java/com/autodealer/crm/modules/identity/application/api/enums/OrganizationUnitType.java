package com.autodealer.crm.modules.identity.application.api.enums;

import java.util.Locale;

/**
 * 组织单元类型。
 */
public enum OrganizationUnitType {
    COMPANY,
    STORE,
    DEPARTMENT,
    TEAM;

    public static OrganizationUnitType parse(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("组织类型不能为空");
        }
        return OrganizationUnitType.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
