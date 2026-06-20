package com.autodealer.crm.config.handler;

import com.autodealer.crm.constant.Constants;
import com.autodealer.crm.manager.RedisManager;
import com.autodealer.crm.model.TUser;
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

        logoutSuccessHandler.onLogoutSuccess(request, response, authentication);

        verify(redisManager).delete(Constants.REDIS_JWT_KEY + 1);
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
