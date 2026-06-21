package com.autodealer.crm.constant;

/**
 * Redis Key 的唯一构造入口。
 *
 * <p>所有业务缓存 key 必须通过此类构造，禁止在调用方手工拼接 Redis key 字符串。
 */
public final class RedisKeys {

    private static final String USER_LOGIN_PREFIX = "cdrm:user:login:";
    private static final String OWNER_LIST = "cdrm:user:owner";
    private static final String TRAN_DETAIL_PREFIX = "cdrm:tran:detail:";
    private static final String TRAN_LIST_PREFIX = "cdrm:tran:list:";
    private static final String TRAN_PRODUCTS_PREFIX = "cdrm:tran:products:";
    private static final String TRAN_INVOICES_PREFIX = "cdrm:tran:invoices:";
    private static final String TRAN_PAYMENTS_PREFIX = "cdrm:tran:payments:";
    private static final String DICT_PREFIX = "cdrm:dict:";
    private static final String DICT_TYPE_PREFIX = "cdrm:dict:type:";
    private static final String DICT_TYPE_CODE_PREFIX = "cdrm:dict:type:code:";
    private static final String DICT_VALUE_PREFIX = "cdrm:dict:value:";
    private static final String DICT_VALUES_TYPE_PREFIX = "cdrm:dict:values:type:";
    private static final String DICT_LIST_PREFIX = "cdrm:dict:list:";

    private RedisKeys() {
    }

    public static String userLogin(Integer userId) {
        return USER_LOGIN_PREFIX + userId;
    }

    public static String ownerList() {
        return OWNER_LIST;
    }

    public static String transactionDetail(Integer tranId) {
        return TRAN_DETAIL_PREFIX + tranId;
    }

    public static String transactionListPattern() {
        return TRAN_LIST_PREFIX + "*";
    }

    public static String transactionProducts(Integer tranId) {
        return TRAN_PRODUCTS_PREFIX + tranId;
    }

    public static String transactionInvoices(Integer tranId) {
        return TRAN_INVOICES_PREFIX + tranId;
    }

    public static String transactionPayments(Integer tranId) {
        return TRAN_PAYMENTS_PREFIX + tranId;
    }

    /**
     * 字典类型详情缓存 key。
     */
    public static String dictTypeDetail(Integer typeId) {
        return DICT_TYPE_PREFIX + typeId;
    }

    /**
     * 字典类型按 code 查询缓存 key。
     */
    public static String dictTypeByCode(String typeCode) {
        return DICT_TYPE_CODE_PREFIX + typeCode;
    }

    /**
     * 字典值详情缓存 key。
     */
    public static String dictValueDetail(Integer valueId) {
        return DICT_VALUE_PREFIX + valueId;
    }

    /**
     * 字典值列表（按类型 ID）缓存 key。
     */
    public static String dictValuesByType(Integer typeId) {
        return DICT_VALUES_TYPE_PREFIX + typeId;
    }

    /**
     * 字典类型分页列表缓存 key。
     */
    public static String dictTypeList(String pageKey) {
        return DICT_LIST_PREFIX + "type:" + pageKey;
    }

    /**
     * 字典值分页列表缓存 key。
     */
    public static String dictValueList(String pageKey) {
        return DICT_LIST_PREFIX + "value:" + pageKey;
    }

    /**
     * 字典类型缓存 key（按 typeCode，保留兼容）。
     */
    public static String dictCache(String typeCode) {
        return DICT_PREFIX + typeCode;
    }

    /**
     * 所有字典缓存的匹配模式，用于全量清理。
     */
    public static String dictCachePattern() {
        return DICT_PREFIX + "*";
    }

    /**
     * 字典类型相关缓存的匹配模式。
     */
    public static String dictTypePattern() {
        return DICT_TYPE_PREFIX + "*";
    }

    /**
     * 字典值相关缓存的匹配模式。
     */
    public static String dictValuePattern() {
        return DICT_VALUE_PREFIX + "*";
    }

    /**
     * 字典列表缓存的匹配模式。
     */
    public static String dictListPattern() {
        return DICT_LIST_PREFIX + "*";
    }
}
