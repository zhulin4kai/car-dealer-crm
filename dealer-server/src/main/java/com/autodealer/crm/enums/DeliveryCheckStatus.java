package com.autodealer.crm.enums;

public enum DeliveryCheckStatus {
    PENDING,
    COMPLETED,
    BLOCKED;

    public static DeliveryCheckStatus parse(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("交付准备项状态不能为空");
        }
        return DeliveryCheckStatus.valueOf(value);
    }
}
