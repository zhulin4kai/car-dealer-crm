package com.bjpowernode.config.handler;

import com.bjpowernode.constant.Constants;
import com.bjpowernode.model.TUser;
import com.bjpowernode.service.RedisService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;

import java.io.IOException;

import jakarta.servlet.ServletException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MyLogoutSuccessHandlerTest {

    @InjectMocks
    private MyLogoutSuccessHandler logoutSuccessHandler;

    @Mock
    private RedisService redisService;

    @Test
    void testOnLogoutSuccessShouldRemoveTokenFromRedis() throws IOException, ServletException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        TUser user = new TUser();
        user.setId(1);

        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(user);

        logoutSuccessHandler.onLogoutSuccess(request, response, authentication);

        verify(redisService).removeValue(Constants.REDIS_JWT_KEY + 1);
    }

    @Test
    void testOnLogoutSuccessShouldReturnSuccessMessage() throws IOException, ServletException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        TUser user = new TUser();
        user.setId(1);

        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(user);

        logoutSuccessHandler.onLogoutSuccess(request, response, authentication);

        String content = response.getContentAsString();
        assertTrue(content.contains("200"));
        assertTrue(content.contains("操作成功"));
    }

    @Test
    void testOnLogoutSuccessShouldSetJsonContentType() throws IOException, ServletException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        TUser user = new TUser();
        user.setId(1);

        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(user);

        logoutSuccessHandler.onLogoutSuccess(request, response, authentication);

        assertTrue(response.getContentType().contains("application/json"));
    }
}
