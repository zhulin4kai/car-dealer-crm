package com.autodealer.crm.modules.fulfillment.delivery.application.api.enums;

public enum DeliveryStatus {
    PENDING_PREPARE,
    PREPARING,
    WAITING_CUSTOMER,
    WAITING_DELIVERY,
    DELIVERING,
    SIGNED,
    COMPLETED,
    EXCEPTION,
    CANCELLED;

    public static DeliveryStatus parse(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("交付状态不能为空");
        }
        return DeliveryStatus.valueOf(value);
    }
}
