package com.autodealer.crm.constant;

/**
 * 后端功能权限稳定编码。
 */
public final class PermissionCodes {

    private PermissionCodes() {
    }

    public static final String ACTIVITY_LIST = "activity:list";
    public static final String ACTIVITY_ADD = "activity:add";
    public static final String ACTIVITY_EDIT = "activity:edit";
    public static final String ACTIVITY_VIEW = "activity:view";
    public static final String ACTIVITY_DELETE = "activity:delete";
    public static final String ACTIVITY_REVIEW = "activity:review";
    public static final String ACTIVITY_CLOSE = "activity:close";
    public static final String ACTIVITY_EXPORT = "activity:export";

    public static final String CLUE_LIST = "clue:list";
    public static final String CLUE_ADD = "clue:add";
    public static final String CLUE_EDIT = "clue:edit";
    public static final String CLUE_VIEW = "clue:view";
    public static final String CLUE_DELETE = "clue:delete";
    public static final String CLUE_IMPORT = "clue:import";
    public static final String CLUE_TRANSFER = "clue:transfer";
    public static final String CLUE_CLOSE = "clue:close";
    public static final String CLUE_RESTORE = "clue:restore";

    public static final String CUSTOMER_LIST = "customer:list";
    public static final String CUSTOMER_VIEW = "customer:view";
    public static final String CUSTOMER_EXPORT = "customer:export";
    public static final String CUSTOMER_TRANSFER = "customer:transfer";
    public static final String CUSTOMER_MERGE = "customer:merge";
    public static final String CUSTOMER_DELETE = "customer:delete";
    public static final String CUSTOMER_SENSITIVE_VIEW = "customer:sensitive:view";

    public static final String TRAN_LIST = "tran:list";
    public static final String TRAN_VIEW = "tran:view";
    public static final String TRAN_CREATE = "tran:create";
    public static final String TRAN_EDIT = "tran:edit";
    public static final String TRAN_DELETE = "tran:delete";
    public static final String TRAN_CANCEL = "tran:cancel";
    public static final String TRAN_CLOSE = "tran:close";
    public static final String TRAN_SETTLE = "tran:settle";
    public static final String TRAN_RESUBMIT = "tran:resubmit";
    public static final String TRAN_APPROVE = "tran:approve";
    public static final String TRAN_INVOICE = "tran:invoice";
    public static final String TRAN_INVOICE_SENSITIVE = "tran:invoice:sensitive";
    public static final String TRAN_PAYMENT = "tran:payment";
    public static final String TRAN_PAYMENT_CONFIRM = "tran:payment:confirm";
    public static final String TRAN_REFUND = "tran:refund";
    public static final String TRAN_REFUND_APPROVE = "tran:refund:approve";
    public static final String TRAN_REFUND_EXECUTE = "tran:refund:execute";

    public static final String QUOTE_LIST = "quote:list";
    public static final String QUOTE_VIEW = "quote:view";
    public static final String QUOTE_CREATE = "quote:create";
    public static final String QUOTE_EDIT = "quote:edit";
    public static final String QUOTE_APPROVE = "quote:approve";
    public static final String QUOTE_CONFIRM = "quote:confirm";
    public static final String QUOTE_ORDER = "quote:order";
    public static final String QUOTE_CANCEL = "quote:cancel";

    public static final String DELIVERY_LIST = "delivery:list";
    public static final String DELIVERY_VIEW = "delivery:view";
    public static final String DELIVERY_CREATE = "delivery:create";
    public static final String DELIVERY_CHECK = "delivery:check";
    public static final String DELIVERY_SIGN = "delivery:sign";
    public static final String DELIVERY_EXCEPTION = "delivery:exception";
    public static final String DELIVERY_CANCEL = "delivery:cancel";

    public static final String OPPORTUNITY_LIST = "opportunity:list";
    public static final String OPPORTUNITY_VIEW = "opportunity:view";
    public static final String OPPORTUNITY_CREATE = "opportunity:create";
    public static final String OPPORTUNITY_EDIT = "opportunity:edit";
    public static final String OPPORTUNITY_ADVANCE = "opportunity:advance";
    public static final String OPPORTUNITY_WIN = "opportunity:win";
    public static final String OPPORTUNITY_LOSE = "opportunity:lose";
    public static final String OPPORTUNITY_SHELVE = "opportunity:shelve";
    public static final String OPPORTUNITY_RESTORE = "opportunity:restore";

    public static final String TEST_DRIVE_LIST = "test-drive:list";
    public static final String TEST_DRIVE_VIEW = "test-drive:view";
    public static final String TEST_DRIVE_CREATE = "test-drive:create";
    public static final String TEST_DRIVE_RESCHEDULE = "test-drive:reschedule";
    public static final String TEST_DRIVE_CANCEL = "test-drive:cancel";
    public static final String TEST_DRIVE_CHECK_IN = "test-drive:check-in";
    public static final String TEST_DRIVE_COMPLETE = "test-drive:complete";

    public static final String FOLLOW_TASK_LIST = "follow-task:list";
    public static final String FOLLOW_TASK_VIEW = "follow-task:view";
    public static final String FOLLOW_TASK_CREATE = "follow-task:create";
    public static final String FOLLOW_TASK_UPDATE = "follow-task:update";
    public static final String FOLLOW_TASK_CANCEL = "follow-task:cancel";
    public static final String FOLLOW_TASK_COMPLETE = "follow-task:complete";
    public static final String COMMUNICATION_RECORD_LIST = "communication-record:list";
    public static final String COMMUNICATION_RECORD_CREATE = "communication-record:create";
    public static final String COMMUNICATION_RECORD_CORRECT = "communication-record:correct";
    public static final String COMMUNICATION_RECORD_VOID = "communication-record:void";

    public static final String PRODUCT_LIST = "product:list";
    public static final String PRODUCT_VIEW = "product:view";
    public static final String PRODUCT_ADD = "product:add";
    public static final String PRODUCT_EDIT = "product:edit";
    public static final String PRODUCT_DELETE = "product:delete";
    public static final String PRODUCT_CATEGORY_LIST = "product:category:list";
    public static final String PRODUCT_CATEGORY_VIEW = "product:category:view";
    public static final String PRODUCT_CATEGORY_ADD = "product:category:add";
    public static final String PRODUCT_CATEGORY_EDIT = "product:category:edit";
    public static final String PRODUCT_CATEGORY_DELETE = "product:category:delete";
    public static final String PRODUCT_PROMOTION_LIST = "product:promotion:list";
    public static final String PRODUCT_PROMOTION_VIEW = "product:promotion:view";
    public static final String PRODUCT_PROMOTION_ADD = "product:promotion:add";
    public static final String PRODUCT_PROMOTION_EDIT = "product:promotion:edit";
    public static final String PRODUCT_PROMOTION_DELETE = "product:promotion:delete";
    public static final String PRODUCT_PROMOTION_STATUS = "product:promotion:status";
    public static final String PRODUCT_STOCK_VIEW = "product:stock:view";
    public static final String PRODUCT_STOCK_ADJUST = "product:stock:adjust";

    public static final String DICT_TYPE_LIST = "dict:type:list";
    public static final String DICT_TYPE_VIEW = "dict:type:view";
    public static final String DICT_TYPE_ADD = "dict:type:add";
    public static final String DICT_TYPE_EDIT = "dict:type:edit";
    public static final String DICT_TYPE_DELETE = "dict:type:delete";
    public static final String DICT_VALUE_LIST = "dict:value:list";
    public static final String DICT_VALUE_VIEW = "dict:value:view";
    public static final String DICT_VALUE_ADD = "dict:value:add";
    public static final String DICT_VALUE_EDIT = "dict:value:edit";
    public static final String DICT_VALUE_DELETE = "dict:value:delete";
    public static final String DICT_CACHE_REFRESH = "dict:cache:refresh";

    public static final String AUDIT_LOGIN_LIST = "audit:login:list";
    public static final String AUDIT_LOGIN_DETAIL = "audit:login:detail";
    public static final String AUDIT_LOGIN_EXPORT = "audit:login:export";
    public static final String AUDIT_OPERATION_LIST = "audit:operation:list";
    public static final String AUDIT_OPERATION_DETAIL = "audit:operation:detail";
    public static final String AUDIT_OPERATION_EXPORT = "audit:operation:export";

    public static final String USER_LIST = "user:list";
    public static final String USER_VIEW = "user:view";
    public static final String USER_ADD = "user:add";
    public static final String USER_EDIT = "user:edit";
    public static final String USER_DELETE = "user:delete";
    public static final String USER_STATUS = "user:status";
    public static final String USER_ROLE = "user:role";
    public static final String USER_PASSWORD = "user:password";

    public static final String STATISTIC_VIEW = "statistic:view";

    public static final String AI_USE = "ai:assistant:use";
    public static final String AI_RUN_VIEW = "ai:run:view";
    public static final String AI_TOOL_EXECUTE = "ai:tool:execute";
    public static final String AI_PROPOSAL_CONFIRM = "ai:proposal:confirm";
    public static final String AI_WORKFLOW_VIEW = "ai:workflow:view";
    public static final String AI_WORKFLOW_MANAGE = "ai:workflow:manage";
    public static final String AI_PROACTIVE_VIEW = "ai:proactive:view";
    public static final String AI_PROACTIVE_USE = "ai:proactive:use";
    public static final String AI_PROVIDER_CONFIG_VIEW = "ai:provider-config:view";
    public static final String AI_PROVIDER_CONFIG_MANAGE = "ai:provider-config:manage";
    public static final String AI_PROVIDER_CONFIG_ROTATE_KEY = "ai:provider-config:rotate-key";
    public static final String AI_POLICY_VIEW = "ai:policy:view";
    public static final String AI_POLICY_MANAGE = "ai:policy:manage";
}
