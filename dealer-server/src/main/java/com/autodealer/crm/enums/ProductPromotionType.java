package com.autodealer.crm.enums;

import com.autodealer.crm.exception.BusinessException;
import com.autodealer.crm.result.CodeEnum;

import java.util.Locale;
import java.util.Set;

/**
 * 促销政策类型稳定编码。
 */
public enum ProductPromotionType {
    AMOUNT,
    PERCENTAGE,
    EXCHANGE_SUBSIDY,
    FINANCE_SUBSIDY,
    GIFT,
    MAINTENANCE,
    INSURANCE_SUBSIDY,
    LIMITED_TIME,
    INVENTORY_CLEARANCE;

    private static final Set<ProductPromotionType> MONETARY_TYPES = Set.of(
            AMOUNT,
            EXCHANGE_SUBSIDY,
            FINANCE_SUBSIDY,
            INSURANCE_SUBSIDY,
            LIMITED_TIME,
            INVENTORY_CLEARANCE
    );

    public static ProductPromotionType parse(String value) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "促销类型不能为空");
        }
        try {
            return ProductPromotionType.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "促销类型必须使用稳定编码");
        }
    }

    public boolean monetary() {
        return MONETARY_TYPES.contains(this);
    }

    public boolean benefitOnly() {
        return this == GIFT || this == MAINTENANCE;
    }
}
