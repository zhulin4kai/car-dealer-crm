package com.autodealer.crm.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum PaymentStatus {
    PENDING("待确认"),
    COMPLETED("已到账"),
    FAILED("失败"),
    REFUNDED("已退款");
    private final String label;
}
