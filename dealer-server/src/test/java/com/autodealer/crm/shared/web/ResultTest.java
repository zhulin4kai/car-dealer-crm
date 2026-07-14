package com.autodealer.crm.shared.web;

import com.autodealer.crm.bootstrap.security.MyLogoutSuccessHandler;
import com.autodealer.crm.shared.error.CodeEnum;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Factory contract tests for the response wrapper. The full wrapper is exercised
 * by real Controller integration tests; this file only locks in the factory
 * methods that the rest of the codebase relies on for shape (not just the
 * static fields they read from).
 */
class ResultTest {

    @Test
    void okFactoryReturns200WithDefaultMessage() {
        Result<Object> result = Result.OK();
        assertEquals(200, result.getCode());
        assertEquals("\u64cd\u4f5c\u6210\u529f", result.getMsg());
        assertNull(result.getData());
    }

    @Test
    void okFactoryAttachesData() {
        Result<String> result = Result.OK("payload");
        assertEquals(200, result.getCode());
        assertEquals("payload", result.getData());
    }

    @Test
    void failFactoryUsesCodeEnumCodeAndMessage() {
        Result<Object> result = Result.FAIL(CodeEnum.TOKEN_IS_EMPTY);
        assertEquals(CodeEnum.TOKEN_IS_EMPTY.getCode(), result.getCode());
        assertEquals(CodeEnum.TOKEN_IS_EMPTY.getMsg(), result.getMsg());
    }

    @Test
    void failFactoryAcceptsCustomMessage() {
        Result<Object> result = Result.FAIL("boom");
        assertEquals(500, result.getCode());
        assertEquals("boom", result.getMsg());
    }

    @Test
    void failFactoryAcceptsExplicitCode() {
        Result<Object> result = Result.FAIL(404, "missing");
        assertEquals(404, result.getCode());
        assertEquals("missing", result.getMsg());
    }

    @Test
    void logoutCodeEnumIs200() {
        // MyLogoutSuccessHandler returns Result.OK(CodeEnum.USER_LOGOUT). The
        // controller contract depends on this being a 200, not an error.
        assertEquals(200, CodeEnum.USER_LOGOUT.getCode());
        assertNotNull(CodeEnum.USER_LOGOUT.getMsg());
    }
}
