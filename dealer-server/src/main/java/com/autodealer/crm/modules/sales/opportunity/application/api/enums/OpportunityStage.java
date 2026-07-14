package com.autodealer.crm.modules.sales.opportunity.application.api.enums;

public enum OpportunityStage {
    INITIAL_CONTACT,
    NEEDS_ANALYSIS,
    VEHICLE_MATCHING,
    TEST_DRIVE_INVITED,
    QUOTING,
    NEGOTIATION,
    PENDING_APPROVAL,
    WON,
    LOST,
    SHELVED,
    CLOSED;

    public static OpportunityStage parse(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("商机阶段不能为空");
        }
        try {
            return OpportunityStage.valueOf(value);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("未知商机阶段: " + value, ex);
        }
    }

    public boolean terminal() {
        return this == WON || this == LOST || this == CLOSED;
    }

    public boolean resultStage() {
        return this == WON || this == LOST || this == SHELVED || this == CLOSED;
    }
}
