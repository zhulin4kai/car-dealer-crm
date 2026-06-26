package com.autodealer.crm.config.handler;

import com.autodealer.crm.constant.RedisKeys;
import com.autodealer.crm.manager.RedisManager;
import com.autodealer.crm.model.TUser;
import com.autodealer.crm.result.CodeEnum;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MyLogoutSuccessHandlerTest {

    @InjectMocks
    private MyLogoutSuccessHandler logoutSuccessHandler;

    @Mock
    private RedisManager redisManager;

    @Test
    void testOnLogoutSuccessShouldRemoveTokenFromRedis() throws IOException, ServletException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        TUser user = new TUser();
        user.setId(1);

        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(user);
        when(redisManager.delete(RedisKeys.userLogin(1))).thenReturn(true);

        logoutSuccessHandler.onLogoutSuccess(request, response, authentication);

        verify(redisManager).delete(RedisKeys.userLogin(1));
    }

    @Test
    void testOnLogoutSuccessShouldReturnSuccessMessage() throws IOException, ServletException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        TUser user = new TUser();
        user.setId(1);

        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(user);
        when(redisManager.delete(RedisKeys.userLogin(1))).thenReturn(true);

        logoutSuccessHandler.onLogoutSuccess(request, response, authentication);

        String content = response.getContentAsString();
        assertEquals(200, response.getStatus());
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
        when(redisManager.delete(RedisKeys.userLogin(1))).thenReturn(true);

        logoutSuccessHandler.onLogoutSuccess(request, response, authentication);

        assertTrue(response.getContentType().contains("application/json"));
    }

    @Test
    void redisDeleteFailureShouldRejectLogoutSuccess() throws IOException, ServletException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        TUser user = new TUser();
        user.setId(1);

        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(user);
        when(redisManager.delete(RedisKeys.userLogin(1))).thenReturn(false);

        logoutSuccessHandler.onLogoutSuccess(request, response, authentication);

        String content = response.getContentAsString();
        assertEquals(500, response.getStatus());
        assertTrue(content.contains("\"code\":" + CodeEnum.SYSTEM_ERROR.getCode()));
        assertFalse(content.contains("USER_LOGOUT"));
    }

    @Test
    void redisDeleteExceptionShouldRejectLogoutSuccess() throws IOException, ServletException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        TUser user = new TUser();
        user.setId(1);

        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(user);
        when(redisManager.delete(RedisKeys.userLogin(1))).thenThrow(new IllegalStateException("redis unavailable"));

        logoutSuccessHandler.onLogoutSuccess(request, response, authentication);

        String content = response.getContentAsString();
        assertEquals(500, response.getStatus());
        assertTrue(content.contains("\"code\":" + CodeEnum.SYSTEM_ERROR.getCode()));
        assertFalse(content.contains("USER_LOGOUT"));
    }
}
