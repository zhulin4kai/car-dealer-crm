package com.autodealer.crm.exception;

import com.autodealer.crm.result.CodeEnum;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BusinessExceptionTest {

    @Test
    void constructorWithCodeEnumOnly() {
        BusinessException ex = new BusinessException(CodeEnum.PARAM_ERROR);
        assertEquals(CodeEnum.PARAM_ERROR, ex.getCodeEnum());
        assertEquals(CodeEnum.PARAM_ERROR.getMsg(), ex.getMessage());
    }

    @Test
    void constructorWithCodeEnumAndMessage() {
        BusinessException ex = new BusinessException(CodeEnum.FAIL, "客户不存在");
        assertEquals(CodeEnum.FAIL, ex.getCodeEnum());
        assertEquals("客户不存在", ex.getMessage());
    }

    @Test
    void constructorWithCodeEnumMessageAndCause() {
        Throwable cause = new IllegalArgumentException("原始异常");
        BusinessException ex = new BusinessException(CodeEnum.FAIL, "手机号重复", cause);
        assertEquals(CodeEnum.FAIL, ex.getCodeEnum());
        assertEquals("手机号重复", ex.getMessage());
        assertSame(cause, ex.getCause());
    }

    @Test
    void isRuntimeException() {
        BusinessException ex = new BusinessException(CodeEnum.SYSTEM_ERROR);
        assertTrue(ex instanceof RuntimeException);
    }

    @Test
    void allNewCodeEnumsAccessible() {
        assertNotNull(CodeEnum.PARAM_ERROR);
        assertNotNull(CodeEnum.FAIL);
        assertNotNull(CodeEnum.SYSTEM_ERROR);
        assertNotNull(CodeEnum.AUTH_LOGIN_FAILED);
        assertNotNull(CodeEnum.UNAUTHORIZED_ERROR);
        assertNotNull(CodeEnum.TOKEN_ERROR);
        assertNotNull(CodeEnum.TOKEN_EXPIRED);
        assertNotNull(CodeEnum.TOKEN_IS_EMPTY);
        assertNotNull(CodeEnum.TOKEN_IS_ERROR);
        assertNotNull(CodeEnum.TOKEN_IS_EXPIRED);
        assertNotNull(CodeEnum.TOKEN_IS_NONE_MATCH);
        assertNotNull(CodeEnum.ACCESS_DENIED);
        assertNotNull(CodeEnum.DATA_ACCESS_EXCEPTION);
        assertNotNull(CodeEnum.USER_LOGOUT);
        assertNotNull(CodeEnum.OK);
    }
}
