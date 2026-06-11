package com.autodealer.crm.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum PaymentMethod {
    CASH("现金"),
    BANK_TRANSFER("银行转账"),
    WECHAT("微信支付"),
    ALIPAY("支付宝"),
    CHECK("支票"),
    OTHER("其他");
    private final String label;
}
