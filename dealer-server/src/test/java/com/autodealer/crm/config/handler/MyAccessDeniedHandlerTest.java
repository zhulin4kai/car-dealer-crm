package com.autodealer.crm.config.handler;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class MyAccessDeniedHandlerTest {

    @InjectMocks
    private MyAccessDeniedHandler accessDeniedHandler;

    @Test
    void testHandleShouldReturnAccessDeniedMessage() throws IOException, ServletException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        AccessDeniedException exception = new AccessDeniedException("没有访问权限");

        accessDeniedHandler.handle(request, response, exception);

        String content = response.getContentAsString();
        assertTrue(content.contains("没有访问权限"));
        assertTrue(content.contains("520"));
    }

    @Test
    void testHandleShouldSetJsonContentType() throws IOException, ServletException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        AccessDeniedException exception = new AccessDeniedException("权限不足");

        accessDeniedHandler.handle(request, response, exception);

        assertTrue(response.getContentType().contains("application/json"));
    }

    @Test
    void testHandleShouldReturn520Code() throws IOException, ServletException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        AccessDeniedException exception = new AccessDeniedException("无权访问");

        accessDeniedHandler.handle(request, response, exception);

        String content = response.getContentAsString();
        assertTrue(content.contains("520"));
    }
}
