package com.autodealer.crm.enums;

/**
 * 交易阶段。数据库存储枚举名，避免业务状态依赖历史字典数字。
 */
public enum TranStage {
    QUOTATION("待报价"),
    PENDING("待审批"),
    APPROVED("已审批"),
    PAYMENT("待收款"),
    DELIVERY("待交付"),
    COMPLETED("已完成"),
    LOST("丢失关闭"),
    CLOSED("已关闭"),
    CANCELLED("已取消");

    private final String label;

    TranStage(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
