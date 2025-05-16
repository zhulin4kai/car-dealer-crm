package com.bjpowernode.constant;

/**
 * 常量类
 *
 */
public class Constants {

    public static final String LOGIN_URI = "/api/login";

    //redis的key的命名规范： 项目名:模块名:功能名:唯一业务参数(比如用户id)
    public static final String REDIS_JWT_KEY = "dlyk:user:login:";

    //redis中负责人的key
    public static final String REDIS_OWNER_KEY = "dlyk:user:owner";

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

    /** 交易缓存key前缀 */
    public static final String REDIS_TRAN_KEY = "dlyk:tran:";
    
    /** 交易列表缓存key */
    public static final String REDIS_TRAN_LIST_KEY = REDIS_TRAN_KEY + "list:";
    
    /** 交易详情缓存key */
    public static final String REDIS_TRAN_DETAIL_KEY = REDIS_TRAN_KEY + "detail:";
    
    /** 交易生产状态缓存key */
    public static final String REDIS_TRAN_PRODUCTION_KEY = REDIS_TRAN_KEY + "production:";
    
    /** 交易发票缓存key */
    public static final String REDIS_TRAN_INVOICE_KEY = REDIS_TRAN_KEY + "invoice:";
    
    /** 缓存过期时间（1天） */
    public static final long CACHE_EXPIRE_TIME = 24 * 60 * 60L;

    /** 交易缓存相关常量 */
    public static final String CACHE_KEY_TRAN = "dlyk:tran:detail:";
    public static final String CACHE_KEY_TRAN_LIST = "dlyk:tran:list:";
    public static final String CACHE_KEY_TRAN_PRODUCTS = "dlyk:tran:products:";
    public static final String CACHE_KEY_TRAN_PRODUCTION = "dlyk:tran:production:";
    public static final String CACHE_KEY_TRAN_INVOICES = "dlyk:tran:invoices:";

    /** 交易状态相关常量 */
    public static final String TRAN_STATUS_PENDING = "pending";
    public static final String TRAN_STATUS_IN_PROGRESS = "in_progress";
    public static final String TRAN_STATUS_COMPLETED = "completed";
    public static final String TRAN_STATUS_CANCELLED = "cancelled";
}

