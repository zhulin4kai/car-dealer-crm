package com.autodealer.crm.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum RefundType {
    ORDER_CANCEL("订单取消退款"),
    OVERPAY("多收退款"),
    PRICE_ADJUSTMENT("价格调整退款"),
    CUSTOMER_BREACH("客户违约部分退款"),
    INTERNAL_CORRECTION("内部纠错退款");

    private final String label;
}
