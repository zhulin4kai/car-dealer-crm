package com.bjpowernode.constant;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConstantsTest {

    @Test
    void testLoginUri() {
        assertEquals("/api/login", Constants.LOGIN_URI);
    }

    @Test
    void testRedisJwtKey() {
        assertEquals("cdrm:user:login:", Constants.REDIS_JWT_KEY);
    }

    @Test
    void testRedisOwnerKey() {
        assertEquals("cdrm:user:owner", Constants.REDIS_OWNER_KEY);
    }

    @Test
    void testExpireTime() {
        assertEquals(7 * 24 * 60 * 60L, Constants.EXPIRE_TIME);
        assertEquals(604800L, Constants.EXPIRE_TIME);
    }

    @Test
    void testDefaultExpireTime() {
        assertEquals(30 * 60L, Constants.DEFAULT_EXPIRE_TIME);
        assertEquals(1800L, Constants.DEFAULT_EXPIRE_TIME);
    }

    @Test
    void testPageSize() {
        assertEquals(10, Constants.PAGE_SIZE);
    }

    @Test
    void testTokenName() {
        assertEquals("Authorization", Constants.TOKEN_NAME);
    }

    @Test
    void testEmpty() {
        assertEquals("", Constants.EMPTY);
    }

    @Test
    void testExportExcelUri() {
        assertEquals("/api/exportExcel", Constants.EXPORT_EXCEL_URI);
    }

    @Test
    void testExcelFileName() {
        assertEquals("客户信息数据", Constants.EXCEL_FILE_NAME);
    }

    @Test
    void testCacheExpireTime() {
        assertEquals(24 * 60 * 60L, Constants.CACHE_EXPIRE_TIME);
        assertEquals(86400L, Constants.CACHE_EXPIRE_TIME);
    }

    @Test
    void testCacheKeyTran() {
        assertEquals("cdrm:tran:detail:", Constants.CACHE_KEY_TRAN);
    }

    @Test
    void testCacheKeyTranList() {
        assertEquals("cdrm:tran:list:", Constants.CACHE_KEY_TRAN_LIST);
    }

    @Test
    void testCacheKeyTranProducts() {
        assertEquals("cdrm:tran:products:", Constants.CACHE_KEY_TRAN_PRODUCTS);
    }

    @Test
    void testCacheKeyTranProduction() {
        assertEquals("cdrm:tran:production:", Constants.CACHE_KEY_TRAN_PRODUCTION);
    }

    @Test
    void testCacheKeyTranInvoices() {
        assertEquals("cdrm:tran:invoices:", Constants.CACHE_KEY_TRAN_INVOICES);
    }

    @Test
    void testRedisSystemKey() {
        assertEquals("cdrm:system:", Constants.REDIS_SYSTEM_KEY);
    }

    @Test
    void testRedisSystemListKey() {
        assertEquals("cdrm:system:list", Constants.REDIS_SYSTEM_LIST_KEY);
    }

    @Test
    void testRedisSystemDetailKey() {
        assertEquals("cdrm:system:detail:", Constants.REDIS_SYSTEM_DETAIL_KEY);
    }

    @Test
    void testSystemCacheExpireTime() {
        assertEquals(24 * 60 * 60L, Constants.SYSTEM_CACHE_EXPIRE_TIME);
        assertEquals(86400L, Constants.SYSTEM_CACHE_EXPIRE_TIME);
    }

    @Test
    void testTranStage01() {
        assertEquals(41, Constants.TRAN_STAGE_01);
    }

    @Test
    void testConstantTypes() {
        assertInstanceOf(String.class, Constants.LOGIN_URI);
        assertInstanceOf(String.class, Constants.REDIS_JWT_KEY);
        assertInstanceOf(Long.class, Constants.EXPIRE_TIME);
        assertInstanceOf(Long.class, Constants.DEFAULT_EXPIRE_TIME);
        assertInstanceOf(Integer.class, Constants.PAGE_SIZE);
        assertInstanceOf(String.class, Constants.TOKEN_NAME);
        assertInstanceOf(String.class, Constants.EMPTY);
        assertInstanceOf(String.class, Constants.EXPORT_EXCEL_URI);
        assertInstanceOf(String.class, Constants.EXCEL_FILE_NAME);
        assertInstanceOf(Long.class, Constants.CACHE_EXPIRE_TIME);
        assertInstanceOf(Integer.class, Constants.TRAN_STAGE_01);
    }
}
