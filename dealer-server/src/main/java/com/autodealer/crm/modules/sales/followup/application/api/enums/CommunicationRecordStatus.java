package com.autodealer.crm.modules.sales.followup.application.api.enums;

import java.util.Locale;

public enum CommunicationRecordStatus {
    ACTIVE,
    CORRECTED,
    VOIDED;

    public static CommunicationRecordStatus parse(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("沟通记录状态不能为空");
        }
        return CommunicationRecordStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
