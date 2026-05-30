package com.bjpowernode.config.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.AuthenticationException;

import jakarta.servlet.ServletException;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MyAuthenticationFailureHandlerTest {

    @InjectMocks
    private MyAuthenticationFailureHandler failureHandler;

    @Test
    void testOnAuthenticationFailureShouldReturnErrorMessage() throws IOException, ServletException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        AuthenticationException exception = mock(AuthenticationException.class);
        when(exception.getMessage()).thenReturn("用户名或密码错误");

        failureHandler.onAuthenticationFailure(request, response, exception);

        String content = response.getContentAsString();
        assertTrue(content.contains("用户名或密码错误"));
        assertTrue(content.contains("500"));
    }

    @Test
    void testOnAuthenticationFailureShouldSetJsonContentType() throws IOException, ServletException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        AuthenticationException exception = mock(AuthenticationException.class);
        when(exception.getMessage()).thenReturn("登录失败");

        failureHandler.onAuthenticationFailure(request, response, exception);

        assertTrue(response.getContentType().contains("application/json"));
    }

    @Test
    void testOnAuthenticationFailureWithDifferentMessages() throws IOException, ServletException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        AuthenticationException exception = mock(AuthenticationException.class);
        when(exception.getMessage()).thenReturn("账户已被锁定");

        failureHandler.onAuthenticationFailure(request, response, exception);

        String content = response.getContentAsString();
        assertTrue(content.contains("账户已被锁定"));
    }
}
