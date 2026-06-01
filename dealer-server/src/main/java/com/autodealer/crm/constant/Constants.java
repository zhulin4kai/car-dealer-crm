package com.autodealer.crm.constant;

/**
 * 常量类
 *
 */
public class Constants {

    public static final String LOGIN_URI = "/api/login";

    //redis的key的命名规范： 项目名:模块名:功能名:唯一业务参数(比如用户id)
    public static final String REDIS_JWT_KEY = "cdrm:user:login:";

    //redis中负责人的key
    public static final String REDIS_OWNER_KEY = "cdrm:user:owner";

    //jwt过期时间7天
    public static final Long EXPIRE_TIME = 7 * 24 * 60 * 60L;

    //jwt过期时间30分钟
    public static final Long DEFAULT_EXPIRE_TIME = 30 * 60L;

    //分页时每页显示10条数据
    public static final int PAGE_SIZE = 10;

    //请求token的名称
    public static final String TOKEN_NAME = "Authorization";

    public static final String EMPTY = "";

    //导出Excel的接口路径
    public static final String EXPORT_EXCEL_URI = "/api/exportExcel";

    public static final String EXCEL_FILE_NAME = "客户信息数据";

    /** 缓存过期时间（1天） */
    public static final long CACHE_EXPIRE_TIME = 24 * 60 * 60L;

    /** 交易缓存相关常量 */
    public static final String CACHE_KEY_TRAN = "cdrm:tran:detail:";
    public static final String CACHE_KEY_TRAN_LIST = "cdrm:tran:list:";
    public static final String CACHE_KEY_TRAN_PRODUCTS = "cdrm:tran:products:";
    public static final String CACHE_KEY_TRAN_PRODUCTION = "cdrm:tran:production:";
    public static final String CACHE_KEY_TRAN_INVOICES = "cdrm:tran:invoices:";

    /** 系统信息缓存相关常量 */
    public static final String REDIS_SYSTEM_KEY = "cdrm:system:";
    public static final String REDIS_SYSTEM_LIST_KEY = REDIS_SYSTEM_KEY + "list";
    public static final String REDIS_SYSTEM_DETAIL_KEY = REDIS_SYSTEM_KEY + "detail:";

    /** 系统缓存过期时间（1天） */
    public static final long SYSTEM_CACHE_EXPIRE_TIME = 24 * 60 * 60L;

    public static final Integer TRAN_STAGE_01 = 41;

    /** 批量操作最大数量限制 */
    public static final int MAX_BATCH_SIZE = 100;
}

