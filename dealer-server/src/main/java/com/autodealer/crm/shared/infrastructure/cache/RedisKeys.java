package com.autodealer.crm.shared.infrastructure.cache;

/**
 * Redis Key 的唯一构造入口。
 *
 * <p>所有业务缓存 key 必须通过此类构造，禁止在调用方手工拼接 Redis key 字符串。
 */
public final class RedisKeys {

    private static final String USER_LOGIN_PREFIX = "cdrm:user:login:";
    private static final String USER_SESSION_PREFIX = "cdrm:session:";
    private static final String USER_SESSION_INDEX_PREFIX = "cdrm:user:sessions:";
    private static final String OWNER_LIST = "cdrm:user:owner";
    private static final String TRAN_PRODUCTS_PREFIX = "cdrm:tran:products:";
    private static final String TRAN_INVOICES_PREFIX = "cdrm:tran:invoices:";
    private static final String DICT_TYPE_PREFIX = "cdrm:dict:type:";
    private static final String DICT_TYPE_CODE_PREFIX = "cdrm:dict:type:code:";
    private static final String DICT_VALUE_PREFIX = "cdrm:dict:value:";
    private static final String DICT_VALUES_TYPE_PREFIX = "cdrm:dict:values:type:";
    private static final String CREDENTIAL_RATE_LIMIT_PREFIX = "cdrm:security:credential-rate:";

    private RedisKeys() {
    }

    public static String userLogin(Integer userId) {
        return USER_LOGIN_PREFIX + userId;
    }

    public static String userSession(String sessionId) { return USER_SESSION_PREFIX + sessionId; }

    public static String userSessionIndex(Integer userId) { return USER_SESSION_INDEX_PREFIX + userId; }

    public static String ownerList(Integer operatorUserId, String permissionCode, String qualificationContext) {
        return OWNER_LIST + ":" + operatorUserId + ":" + permissionCode + ":" + qualificationContext;
    }

    public static String ownerListPattern() {
        return OWNER_LIST + "*";
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

    /** 公开凭证限流只接受不可逆摘要，禁止把账号、联系方式或来源地址明文放入 key。 */
    public static String credentialRateLimit(String scope, String subjectDigest) {
        if (scope == null || subjectDigest == null) throw new IllegalArgumentException("限流 key 参数不能为空");
        return CREDENTIAL_RATE_LIMIT_PREFIX + scope + ":" + subjectDigest;
    }

}
