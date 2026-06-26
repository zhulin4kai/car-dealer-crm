package com.autodealer.crm.constant;

/**
 * Redis Key 的唯一构造入口。
 *
 * <p>所有业务缓存 key 必须通过此类构造，禁止在调用方手工拼接 Redis key 字符串。
 */
public final class RedisKeys {

    private static final String USER_LOGIN_PREFIX = "cdrm:user:login:";
    private static final String OWNER_LIST = "cdrm:user:owner";
    private static final String TRAN_PRODUCTS_PREFIX = "cdrm:tran:products:";
    private static final String TRAN_INVOICES_PREFIX = "cdrm:tran:invoices:";
    private static final String DICT_TYPE_PREFIX = "cdrm:dict:type:";
    private static final String DICT_TYPE_CODE_PREFIX = "cdrm:dict:type:code:";
    private static final String DICT_VALUE_PREFIX = "cdrm:dict:value:";
    private static final String DICT_VALUES_TYPE_PREFIX = "cdrm:dict:values:type:";

    private RedisKeys() {
    }

    public static String userLogin(Integer userId) {
        return USER_LOGIN_PREFIX + userId;
    }

    public static String ownerList() {
        return OWNER_LIST;
    }

    public static String transactionProducts(Integer tranId) {
        return TRAN_PRODUCTS_PREFIX + tranId;
    }

    public static String transactionInvoices(Integer tranId) {
        return TRAN_INVOICES_PREFIX + tranId;
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
     * 字典值列表（按类型 ID）缓存匹配模式。
     */
    public static String dictValuesByTypePattern() {
        return DICT_VALUES_TYPE_PREFIX + "*";
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

}
