package com.autodealer.crm.modules.sales.followup.application.api.enums;

import java.util.Locale;

public enum FollowTaskType {
    FIRST_CONTACT,
    PHONE_FOLLOW_UP,
    STORE_INVITATION,
    TEST_DRIVE_CONFIRM,
    QUOTE_COMMUNICATION,
    PRICE_NEGOTIATION,
    CONTRACT_SIGN_REMINDER,
    PAYMENT_REMINDER,
    DELIVERY_CONFIRM,
    POST_DELIVERY_FOLLOW_UP,
    LONG_TERM_MAINTENANCE;

    public static FollowTaskType parse(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("跟进任务类型不能为空");
        }
        return FollowTaskType.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
