package com.bjpowernode.config.handler;

import com.bjpowernode.result.CodeEnum;
import com.bjpowernode.result.R;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.support.WebExchangeBindException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @InjectMocks
    private GlobalExceptionHandler globalExceptionHandler;

    @Test
    void testHandleGenericException() {
        Exception exception = new Exception("系统异常");

        R result = globalExceptionHandler.handException(exception);

        assertNotNull(result);
        assertEquals("系统繁忙，请稍后重试", result.getMsg());
        assertEquals(500, result.getCode());
    }

    @Test
    void testHandleDataAccessException() {
        DataAccessException exception = mock(DataAccessException.class);

        R result = globalExceptionHandler.handException3(exception);

        assertNotNull(result);
        assertEquals(CodeEnum.DATA_ACCESS_EXCEPTION.getCode(), result.getCode());
        assertEquals(CodeEnum.DATA_ACCESS_EXCEPTION.getMsg(), result.getMsg());
    }

    @Test
    void testHandleAccessDeniedException() {
        AccessDeniedException exception = new AccessDeniedException("权限不足");

        R result = globalExceptionHandler.handException(exception);

        assertNotNull(result);
        assertEquals(CodeEnum.ACCESS_DENIED.getCode(), result.getCode());
        assertEquals(CodeEnum.ACCESS_DENIED.getMsg(), result.getMsg());
    }

    @Test
    void testHandleHttpRequestMethodNotSupportedException() {
        HttpRequestMethodNotSupportedException exception = new HttpRequestMethodNotSupportedException("GET");

        R result = globalExceptionHandler.handException(exception);

        assertNotNull(result);
        assertTrue(result.getMsg().contains("不支持的请求方法"));
    }

    @Test
    void testHandleHttpMessageNotReadableException() {
        HttpMessageNotReadableException exception = mock(HttpMessageNotReadableException.class);

        R result = globalExceptionHandler.handException(exception);

        assertNotNull(result);
        assertEquals("请求体格式错误或无法读取", result.getMsg());
    }
}
