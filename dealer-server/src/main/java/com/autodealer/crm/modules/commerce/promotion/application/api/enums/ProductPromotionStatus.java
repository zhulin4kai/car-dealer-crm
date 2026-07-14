package com.autodealer.crm.modules.commerce.promotion.application.api.enums;

import com.autodealer.crm.shared.error.BusinessException;
import com.autodealer.crm.shared.error.CodeEnum;

import java.util.Locale;

/**
 * 促销政策状态稳定编码。
 */
public enum ProductPromotionStatus {
    DRAFT,
    PENDING_EFFECTIVE,
    ACTIVE,
    PAUSED,
    ENDED,
    VOIDED,
    EXHAUSTED;

    public static ProductPromotionStatus parse(String value) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "促销状态不能为空");
        }
        try {
            return ProductPromotionStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "促销状态必须使用稳定编码");
        }
    }

    public boolean terminal() {
        return this == ENDED || this == VOIDED || this == EXHAUSTED;
    }

    public boolean usableForQuote() {
        return this == ACTIVE;
    }
}
