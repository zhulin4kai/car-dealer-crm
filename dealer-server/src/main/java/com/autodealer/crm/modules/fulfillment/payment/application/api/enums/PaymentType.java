package com.autodealer.crm.modules.fulfillment.payment.application.api.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum PaymentType {
    DEPOSIT("定金"),
    INSTALLMENT("分期款"),
    FULL("全款"),
    BALANCE("尾款"),
    REFUND("退款");
    private final String label;
}
