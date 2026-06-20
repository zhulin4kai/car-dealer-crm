package com.autodealer.crm.config.handler;

import com.autodealer.crm.result.CodeEnum;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;

import jakarta.servlet.ServletException;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MyAuthenticationFailureHandlerTest {

    private final MyAuthenticationFailureHandler failureHandler = new MyAuthenticationFailureHandler();

    @Test
    void testOnAuthenticationFailureShouldReturnErrorMessage() throws IOException, ServletException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        request.setParameter("loginAct", "missing-user");

        failureHandler.onAuthenticationFailure(
                request, response, new BadCredentialsException("登录账号不存在"));

        String content = response.getContentAsString();
        assertEquals(401, response.getStatus());
        assertTrue(content.contains("\"code\":" + CodeEnum.AUTH_LOGIN_FAILED.getCode()));
        assertTrue(content.contains(CodeEnum.AUTH_LOGIN_FAILED.getMsg()));
        assertFalse(content.contains("登录账号不存在"));
    }

    @Test
    void testOnAuthenticationFailureShouldSetJsonContentType() throws IOException, ServletException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        failureHandler.onAuthenticationFailure(
                request, response, new BadCredentialsException("登录失败"));

        assertTrue(response.getContentType().contains("application/json"));
    }

    @Test
    void testOnAuthenticationFailureShouldHideAccountStatus() throws IOException, ServletException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        failureHandler.onAuthenticationFailure(
                request, response, new LockedException("账户已被锁定"));

        String content = response.getContentAsString();
        assertEquals(401, response.getStatus());
        assertTrue(content.contains(CodeEnum.AUTH_LOGIN_FAILED.getMsg()));
        assertFalse(content.contains("账户已被锁定"));
    }
}
