package com.autodealer.crm.modules.commerce.quote.application.api.enums;

import com.autodealer.crm.shared.error.BusinessException;
import com.autodealer.crm.shared.error.CodeEnum;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Set;

public enum QuoteStatus {
    DRAFT,
    PENDING_SUBMIT,
    PENDING_APPROVAL,
    REJECTED,
    PENDING_CUSTOMER_CONFIRMATION,
    ACCEPTED,
    REFUSED,
    EXPIRED,
    VOIDED,
    CONVERTED_TO_ORDER;

    private static final EnumMap<QuoteStatus, Set<QuoteStatus>> TRANSITIONS = new EnumMap<>(QuoteStatus.class);

    static {
        TRANSITIONS.put(DRAFT, EnumSet.of(PENDING_SUBMIT, PENDING_APPROVAL, PENDING_CUSTOMER_CONFIRMATION, VOIDED));
        TRANSITIONS.put(PENDING_SUBMIT, EnumSet.of(PENDING_APPROVAL, PENDING_CUSTOMER_CONFIRMATION, VOIDED));
        TRANSITIONS.put(PENDING_APPROVAL, EnumSet.of(REJECTED, PENDING_CUSTOMER_CONFIRMATION, VOIDED));
        TRANSITIONS.put(REJECTED, EnumSet.of(DRAFT, VOIDED));
        TRANSITIONS.put(PENDING_CUSTOMER_CONFIRMATION, EnumSet.of(ACCEPTED, REFUSED, EXPIRED, VOIDED));
        TRANSITIONS.put(ACCEPTED, EnumSet.of(CONVERTED_TO_ORDER, VOIDED));
    }

    public static QuoteStatus parse(String value) {
        try {
            return QuoteStatus.valueOf(value);
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "报价状态编码不合法");
        }
    }

    public boolean canTransitionTo(QuoteStatus target) {
        return TRANSITIONS.getOrDefault(this, Set.of()).contains(target);
    }
}
