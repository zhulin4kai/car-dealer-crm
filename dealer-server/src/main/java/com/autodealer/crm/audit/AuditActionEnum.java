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

    CLUE_CREATE("CLUE_CREATE", "创建线索", "线索管理"),
    CLUE_IMPORT("CLUE_IMPORT", "导入线索", "线索管理"),
    CLUE_TRANSFORM("CLUE_TRANSFORM", "线索转客户", "线索管理"),
    CLUE_DELETE("CLUE_DELETE", "删除线索", "线索管理"),
    CLUE_UPDATE("CLUE_UPDATE", "编辑线索", "线索管理"),

    CUSTOMER_CONVERT("CUSTOMER_CONVERT", "线索转客户", "客户管理"),
    CUSTOMER_OWNER_CHANGE("CUSTOMER_OWNER_CHANGE", "客户归属变更", "客户管理"),

    TRAN_CREATE("TRAN_CREATE", "创建交易", "交易管理"),
    TRAN_SETTLE("TRAN_SETTLE", "交易结算", "交易管理"),
    TRAN_APPROVE("TRAN_APPROVE", "交易审批", "交易管理"),
    TRAN_DELETE("TRAN_DELETE", "删除交易", "交易管理"),
    TRAN_RESUBMIT("TRAN_RESUBMIT", "交易重提", "交易管理"),

    PAYMENT_CREATE("PAYMENT_CREATE", "创建收款", "支付管理"),
    PAYMENT_REFUND("PAYMENT_REFUND", "退款", "支付管理"),
    INVOICE_CREATE("INVOICE_CREATE", "创建发票", "发票管理"),
    INVOICE_STATUS("INVOICE_STATUS", "发票状态变更", "发票管理"),

    PRODUCT_STOCK_IN("PRODUCT_STOCK_IN", "商品入库", "商品管理"),
    PRODUCT_STOCK_ADJUST("PRODUCT_STOCK_ADJUST", "库存调整", "商品管理"),
    PRODUCT_STATUS_CHANGE("PRODUCT_STATUS_CHANGE", "商品状态变更", "商品管理"),

    DICT_TYPE_SAVE("DICT_TYPE_SAVE", "保存字典类型", "字典管理"),
    DICT_TYPE_DELETE("DICT_TYPE_DELETE", "删除字典类型", "字典管理"),
    DICT_VALUE_SAVE("DICT_VALUE_SAVE", "保存字典值", "字典管理"),
    DICT_VALUE_DELETE("DICT_VALUE_DELETE", "删除字典值", "字典管理"),

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
