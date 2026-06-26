package com.autodealer.crm.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum RefundRequestStatus {
    PENDING_APPROVAL("待审批"),
    PENDING_EXECUTION("待执行"),
    EXECUTING("执行中"),
    COMPLETED("已完成"),
    REJECTED("已驳回"),
    FAILED("执行失败"),
    CANCELLED("已撤销");

    private final String label;
}
