package com.autodealer.crm.enums;

import java.util.Locale;

public enum FollowTaskStatus {
    PENDING,
    IN_PROGRESS,
    POSTPONED,
    OVERDUE,
    COMPLETED,
    CANCELLED,
    CLOSED;

    public static FollowTaskStatus parse(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("跟进任务状态不能为空");
        }
        return FollowTaskStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }

    public boolean terminal() {
        return this == COMPLETED || this == CANCELLED || this == CLOSED;
    }

    public boolean processable() {
        return !terminal();
    }
}
