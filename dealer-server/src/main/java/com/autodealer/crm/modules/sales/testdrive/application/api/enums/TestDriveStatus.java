package com.autodealer.crm.modules.sales.testdrive.application.api.enums;

public enum TestDriveStatus {
    PENDING_CONFIRM,
    SCHEDULED,
    RESCHEDULED,
    CHECKED_IN,
    COMPLETED,
    CANCELED,
    NO_SHOW,
    EXCEPTION_CLOSED;

    public static TestDriveStatus parse(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("试驾状态不能为空");
        }
        try {
            return TestDriveStatus.valueOf(value);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("未知试驾状态: " + value, ex);
        }
    }

    public boolean terminal() {
        return this == COMPLETED || this == CANCELED || this == NO_SHOW || this == EXCEPTION_CLOSED;
    }

    public boolean activeSchedule() {
        return this == PENDING_CONFIRM || this == SCHEDULED || this == RESCHEDULED || this == CHECKED_IN;
    }

    public boolean canReschedule() {
        return this == PENDING_CONFIRM || this == SCHEDULED || this == RESCHEDULED;
    }

    public boolean canCancel() {
        return this == PENDING_CONFIRM || this == SCHEDULED || this == RESCHEDULED;
    }

    public boolean canCheckIn() {
        return this == SCHEDULED || this == RESCHEDULED;
    }
}
