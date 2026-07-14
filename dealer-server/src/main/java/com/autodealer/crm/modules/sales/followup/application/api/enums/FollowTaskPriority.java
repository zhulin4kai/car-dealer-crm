package com.autodealer.crm.modules.sales.followup.application.api.enums;

import java.util.Locale;

public enum FollowTaskPriority {
    LOW,
    NORMAL,
    HIGH,
    URGENT;

    public static FollowTaskPriority parseOrDefault(String value) {
        if (value == null || value.trim().isEmpty()) {
            return NORMAL;
        }
        return FollowTaskPriority.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
