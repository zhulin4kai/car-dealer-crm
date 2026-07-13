package com.autodealer.crm.enums;

/**
 * 授权事实可承载的数据范围代码。
 */
public enum DataScopeCode {
    SELF,
    DIRECT_REPORTS,
    REPORTING_TREE,
    PRIMARY_ORG,
    ORG_TREE,
    /** 仅保留稳定代码；具体组织关联和执行语义由 Task 14 建立。 */
    CUSTOM_ORGS,
    GLOBAL
}
