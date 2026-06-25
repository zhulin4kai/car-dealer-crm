package com.autodealer.crm.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum RefundRequestStatus {
    PENDING_APPROVAL("待审批"),
    APPROVED("已审批"),
    REJECTED("已驳回"),
    EXECUTED("已退款");

    private final String label;
}
