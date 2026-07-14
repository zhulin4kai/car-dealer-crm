package com.autodealer.crm.modules.commerce.inventory.application.api.enums;

import java.util.Set;

public enum ProductVehicleStatus {
    PENDING_INBOUND,
    AVAILABLE,
    TEST_DRIVE_RESERVED,
    SALES_LOCKED,
    ORDER_RESERVED,
    PENDING_DELIVERY,
    OUTBOUND,
    DELIVERED,
    INVENTORY_EXCEPTION,
    UNAVAILABLE;

    private static final Set<ProductVehicleStatus> OCCUPIED_STATUSES = Set.of(
            TEST_DRIVE_RESERVED,
            SALES_LOCKED,
            ORDER_RESERVED
    );

    public static ProductVehicleStatus parse(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("库存车辆状态不能为空");
        }
        return ProductVehicleStatus.valueOf(value);
    }

    public boolean isOccupied() {
        return OCCUPIED_STATUSES.contains(this);
    }
}
