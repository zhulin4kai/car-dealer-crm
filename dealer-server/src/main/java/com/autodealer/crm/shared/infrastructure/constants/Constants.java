package com.autodealer.crm.shared.infrastructure.constants;

/**
 * 常量类
 *
 */
public class Constants {

    public static final String LOGIN_URI = "/api/login";

    // jwt 过期时间 7 天
    public static final Long EXPIRE_TIME = 7 * 24 * 60 * 60L;

    // jwt 过期时间 4 个小时
    public static final Long DEFAULT_EXPIRE_TIME = 4 * 60 * 60L;

    // 分页时每页显示 10 条数据
    public static final int PAGE_SIZE = 10;

    // 请求 token 的名称
    public static final String TOKEN_NAME = "Authorization";

    public static final String EMPTY = "";

    // 导出 Excel 的接口路径
    public static final String EXPORT_EXCEL_URI = "/api/exportExcel";

    public static final String EXCEL_FILE_NAME = "客户信息数据";

    // 缓存过期时间 1 天
    public static final long CACHE_EXPIRE_TIME = 24 * 60 * 60L;

    public static final Integer TRAN_STAGE_01 = 41;

    // 批量操作最大数量限制
    public static final int MAX_BATCH_SIZE = 100;
}
