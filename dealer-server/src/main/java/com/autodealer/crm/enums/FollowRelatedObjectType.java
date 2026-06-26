package com.autodealer.crm.enums;

import java.util.Locale;

public enum FollowRelatedObjectType {
    CLUE,
    CUSTOMER,
    OPPORTUNITY,
    TEST_DRIVE,
    ORDER;

    public static FollowRelatedObjectType parse(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("关联对象类型不能为空");
        }
        return FollowRelatedObjectType.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
