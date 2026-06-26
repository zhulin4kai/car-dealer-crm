package com.autodealer.crm.audit;

/**
 * 审计动作枚举，定义系统中需要审计的所有业务动作。
 *
 * <p>每个枚举值包含稳定的 actionCode、中文描述 actionName 和模块名 moduleName。
 * 枚举 code 为持久化标识，禁止随意变更或依赖 ordinal。
 */
public enum AuditActionEnum {

    USER_CREATE("USER_CREATE", "创建用户", "用户管理"),
    USER_UPDATE("USER_UPDATE", "编辑用户", "用户管理"),
    USER_DELETE("USER_DELETE", "删除用户", "用户管理"),
    USER_STATUS_CHANGE("USER_STATUS_CHANGE", "用户状态变更", "用户管理"),
    USER_HANDOVER("USER_HANDOVER", "用户责任交接", "用户管理"),

    CLUE_CREATE("CLUE_CREATE", "创建线索", "线索管理"),
    CLUE_IMPORT("CLUE_IMPORT", "导入线索", "线索管理"),
    CLUE_TRANSFORM("CLUE_TRANSFORM", "线索转客户", "线索管理"),
    CLUE_DELETE("CLUE_DELETE", "删除线索", "线索管理"),
    CLUE_UPDATE("CLUE_UPDATE", "编辑线索", "线索管理"),
    CLUE_TRANSFER("CLUE_TRANSFER", "线索转派", "线索管理"),
    CLUE_CLOSE("CLUE_CLOSE", "关闭线索", "线索管理"),
    CLUE_RESTORE("CLUE_RESTORE", "恢复线索", "线索管理"),

    ACTIVITY_CREATE("ACTIVITY_CREATE", "创建活动", "市场活动"),
    ACTIVITY_UPDATE("ACTIVITY_UPDATE", "编辑活动", "市场活动"),
    ACTIVITY_STATUS_CHANGE("ACTIVITY_STATUS_CHANGE", "活动状态变更", "市场活动"),
    ACTIVITY_REVIEW("ACTIVITY_REVIEW", "活动复盘", "市场活动"),
    ACTIVITY_DELETE("ACTIVITY_DELETE", "删除活动草稿", "市场活动"),
    ACTIVITY_EXPORT("ACTIVITY_EXPORT", "导出活动ROI", "市场活动"),

    CUSTOMER_CONVERT("CUSTOMER_CONVERT", "线索转客户", "客户管理"),
    CUSTOMER_OWNER_CHANGE("CUSTOMER_OWNER_CHANGE", "客户归属变更", "客户管理"),
    CUSTOMER_MERGE("CUSTOMER_MERGE", "客户合并", "客户管理"),
    CUSTOMER_DELETE("CUSTOMER_DELETE", "删除客户", "客户管理"),

    TRAN_CREATE("TRAN_CREATE", "创建交易", "交易管理"),
    TRAN_SETTLE("TRAN_SETTLE", "交易结算", "交易管理"),
    TRAN_APPROVE("TRAN_APPROVE", "交易审批", "交易管理"),
    TRAN_DELETE("TRAN_DELETE", "删除交易", "交易管理"),
    TRAN_CANCEL("TRAN_CANCEL", "取消交易", "交易履约"),
    TRAN_CLOSE("TRAN_CLOSE", "关闭交易", "交易履约"),
    TRAN_COMPLETE("TRAN_COMPLETE", "交易完成", "交易履约"),
    TRAN_RESUBMIT("TRAN_RESUBMIT", "交易重提", "交易管理"),

    QUOTE_CREATE("QUOTE_CREATE", "创建报价", "报价订单"),
    QUOTE_VERSION_CREATE("QUOTE_VERSION_CREATE", "创建报价版本", "报价订单"),
    QUOTE_STATUS_CHANGE("QUOTE_STATUS_CHANGE", "报价状态变更", "报价订单"),

    DELIVERY_CREATE("DELIVERY_CREATE", "创建交付记录", "交付管理"),
    DELIVERY_CHECK("DELIVERY_CHECK", "更新交付准备项", "交付管理"),
    DELIVERY_SIGN("DELIVERY_SIGN", "客户交付签收", "交付管理"),
    DELIVERY_COMPLETE("DELIVERY_COMPLETE", "交付完成", "交付管理"),
    DELIVERY_EXCEPTION("DELIVERY_EXCEPTION", "登记交付异常", "交付管理"),
    DELIVERY_CANCEL("DELIVERY_CANCEL", "取消交付", "交付管理"),

    OPPORTUNITY_CREATE("OPPORTUNITY_CREATE", "创建商机", "商机管理"),
    OPPORTUNITY_UPDATE("OPPORTUNITY_UPDATE", "编辑商机", "商机管理"),
    OPPORTUNITY_STAGE_CHANGE("OPPORTUNITY_STAGE_CHANGE", "商机阶段推进", "商机管理"),
    OPPORTUNITY_WIN("OPPORTUNITY_WIN", "商机赢单", "商机管理"),
    OPPORTUNITY_LOSE("OPPORTUNITY_LOSE", "商机输单", "商机管理"),
    OPPORTUNITY_SHELVE("OPPORTUNITY_SHELVE", "商机搁置", "商机管理"),
    OPPORTUNITY_RESTORE("OPPORTUNITY_RESTORE", "商机恢复", "商机管理"),

    TEST_DRIVE_CREATE("TEST_DRIVE_CREATE", "创建试驾预约", "试驾管理"),
    TEST_DRIVE_RESCHEDULE("TEST_DRIVE_RESCHEDULE", "试驾改期", "试驾管理"),
    TEST_DRIVE_CANCEL("TEST_DRIVE_CANCEL", "取消试驾", "试驾管理"),
    TEST_DRIVE_NO_SHOW("TEST_DRIVE_NO_SHOW", "标记试驾爽约", "试驾管理"),
    TEST_DRIVE_CHECK_IN("TEST_DRIVE_CHECK_IN", "试驾签到", "试驾管理"),
    TEST_DRIVE_COMPLETE("TEST_DRIVE_COMPLETE", "试驾完成", "试驾管理"),

    FOLLOW_TASK_CREATE("FOLLOW_TASK_CREATE", "创建跟进任务", "跟进任务"),
    FOLLOW_TASK_START("FOLLOW_TASK_START", "开始跟进任务", "跟进任务"),
    FOLLOW_TASK_POSTPONE("FOLLOW_TASK_POSTPONE", "延期跟进任务", "跟进任务"),
    FOLLOW_TASK_CANCEL("FOLLOW_TASK_CANCEL", "取消跟进任务", "跟进任务"),
    FOLLOW_TASK_COMPLETE("FOLLOW_TASK_COMPLETE", "完成跟进任务", "跟进任务"),
    COMMUNICATION_RECORD_CREATE("COMMUNICATION_RECORD_CREATE", "新增沟通记录", "跟进任务"),
    COMMUNICATION_RECORD_CORRECT("COMMUNICATION_RECORD_CORRECT", "更正沟通记录", "跟进任务"),
    COMMUNICATION_RECORD_VOID("COMMUNICATION_RECORD_VOID", "作废沟通记录", "跟进任务"),

    PAYMENT_CREATE("PAYMENT_CREATE", "登记收款", "支付管理"),
    PAYMENT_CONFIRM("PAYMENT_CONFIRM", "确认收款", "支付管理"),
    PAYMENT_REJECT("PAYMENT_REJECT", "退回收款", "支付管理"),
    PAYMENT_REFUND_REQUEST("PAYMENT_REFUND_REQUEST", "申请退款", "退款管理"),
    PAYMENT_REFUND_APPROVE("PAYMENT_REFUND_APPROVE", "审批退款", "退款管理"),
    PAYMENT_REFUND("PAYMENT_REFUND", "执行退款", "退款管理"),
    PAYMENT_REFUND_FAILED("PAYMENT_REFUND_FAILED", "退款执行失败", "退款管理"),
    INVOICE_CREATE("INVOICE_CREATE", "创建发票", "发票管理"),
    INVOICE_REISSUE("INVOICE_REISSUE", "重开发票", "发票管理"),
    INVOICE_RED_REVERSE("INVOICE_RED_REVERSE", "发票红冲", "发票管理"),
    INVOICE_STATUS("INVOICE_STATUS", "发票状态变更", "发票管理"),

    PRODUCT_STOCK_IN("PRODUCT_STOCK_IN", "商品入库", "商品管理"),
    PRODUCT_STOCK_RESERVE("PRODUCT_STOCK_RESERVE", "库存占用", "库存管理"),
    PRODUCT_STOCK_RELEASE("PRODUCT_STOCK_RELEASE", "库存释放", "库存管理"),
    PRODUCT_STOCK_OUT("PRODUCT_STOCK_OUT", "库存出库", "库存管理"),
    PRODUCT_STOCK_ADJUST("PRODUCT_STOCK_ADJUST", "库存调整", "商品管理"),
    PRODUCT_STATUS_CHANGE("PRODUCT_STATUS_CHANGE", "商品状态变更", "商品管理"),
    PRODUCT_PROMOTION_CREATE("PRODUCT_PROMOTION_CREATE", "创建促销", "促销政策"),
    PRODUCT_PROMOTION_UPDATE("PRODUCT_PROMOTION_UPDATE", "编辑促销", "促销政策"),
    PRODUCT_PROMOTION_STATUS_CHANGE("PRODUCT_PROMOTION_STATUS_CHANGE", "促销状态变更", "促销政策"),
    PRODUCT_PROMOTION_DELETE("PRODUCT_PROMOTION_DELETE", "删除促销草稿", "促销政策"),
    PRODUCT_PROMOTION_USE("PRODUCT_PROMOTION_USE", "促销引用核销", "促销政策"),

    DICT_TYPE_SAVE("DICT_TYPE_SAVE", "保存字典类型", "字典管理"),
    DICT_TYPE_DELETE("DICT_TYPE_DELETE", "删除字典类型", "字典管理"),
    DICT_VALUE_SAVE("DICT_VALUE_SAVE", "保存字典值", "字典管理"),
    DICT_VALUE_DELETE("DICT_VALUE_DELETE", "删除字典值", "字典管理"),

    AUDIT_LOGIN_EXPORT("AUDIT_LOGIN_EXPORT", "导出登录记录", "审计日志"),
    AUDIT_OPERATION_EXPORT("AUDIT_OPERATION_EXPORT", "导出操作记录", "审计日志"),

    EXPORT_ALL_CUSTOMER("EXPORT_ALL_CUSTOMER", "导出全部客户", "客户管理");

    private final String actionCode;
    private final String actionName;
    private final String moduleName;

    AuditActionEnum(String actionCode, String actionName, String moduleName) {
        this.actionCode = actionCode;
        this.actionName = actionName;
        this.moduleName = moduleName;
    }

    public String getActionCode() {
        return actionCode;
    }

    public String getActionName() {
        return actionName;
    }

    public String getModuleName() {
        return moduleName;
    }

    /**
     * 根据 actionCode 查找枚举值，未知 code 必须显式失败。
     *
     * @param actionCode 审计动作编码
     * @return 对应的枚举值
     * @throws IllegalArgumentException 如果 actionCode 未知
     */
    public static AuditActionEnum fromActionCode(String actionCode) {
        for (AuditActionEnum value : values()) {
            if (value.actionCode.equals(actionCode)) {
                return value;
            }
        }
        throw new IllegalArgumentException("未知的审计动作编码: " + actionCode);
    }
}
