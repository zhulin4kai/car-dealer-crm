package com.autodealer.crm.config.handler;

import com.autodealer.crm.constant.Constants;
import com.autodealer.crm.model.TUser;
import com.autodealer.crm.service.RedisService;
import com.autodealer.crm.util.JWTUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;

import jakarta.servlet.ServletException;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MyAuthenticationSuccessHandlerTest {

    @InjectMocks
    private MyAuthenticationSuccessHandler successHandler;

    @Mock
    private RedisService redisService;

    @Test
    void testOnAuthenticationSuccessShouldReturnJwtToken() throws IOException, ServletException {
        try (MockedStatic<JWTUtils> jwtUtils = mockStatic(JWTUtils.class)) {
            MockHttpServletRequest request = new MockHttpServletRequest();
            MockHttpServletResponse response = new MockHttpServletResponse();

            TUser user = new TUser();
            user.setId(1);
            user.setLoginAct("admin");

            Authentication authentication = mock(Authentication.class);
            when(authentication.getPrincipal()).thenReturn(user);

            jwtUtils.when(() -> JWTUtils.createJWT(anyString())).thenReturn("generated.jwt.token");

            successHandler.onAuthenticationSuccess(request, response, authentication);

            String content = response.getContentAsString();
            assertTrue(content.contains("generated.jwt.token"));
            assertTrue(content.contains("200"));

            verify(redisService).setValue(Constants.REDIS_JWT_KEY + 1, "generated.jwt.token");
        }
    }

    @Test
    void testOnAuthenticationSuccessWithRememberMeShouldSetLongExpiry() throws IOException, ServletException {
        try (MockedStatic<JWTUtils> jwtUtils = mockStatic(JWTUtils.class)) {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setParameter("rememberMe", "true");
            MockHttpServletResponse response = new MockHttpServletResponse();

            TUser user = new TUser();
            user.setId(1);

            Authentication authentication = mock(Authentication.class);
            when(authentication.getPrincipal()).thenReturn(user);

            jwtUtils.when(() -> JWTUtils.createJWT(anyString())).thenReturn("jwt.token");

            successHandler.onAuthenticationSuccess(request, response, authentication);

            verify(redisService).expire(Constants.REDIS_JWT_KEY + 1, Constants.EXPIRE_TIME, TimeUnit.SECONDS);
        }
    }

    @Test
    void testOnAuthenticationSuccessWithoutRememberMeShouldSetDefaultExpiry() throws IOException, ServletException {
        try (MockedStatic<JWTUtils> jwtUtils = mockStatic(JWTUtils.class)) {
            MockHttpServletRequest request = new MockHttpServletRequest();
            MockHttpServletResponse response = new MockHttpServletResponse();

            TUser user = new TUser();
            user.setId(1);

            Authentication authentication = mock(Authentication.class);
            when(authentication.getPrincipal()).thenReturn(user);

            jwtUtils.when(() -> JWTUtils.createJWT(anyString())).thenReturn("jwt.token");

            successHandler.onAuthenticationSuccess(request, response, authentication);

            verify(redisService).expire(Constants.REDIS_JWT_KEY + 1, Constants.DEFAULT_EXPIRE_TIME, TimeUnit.SECONDS);
        }
    }

    @Test
    void testOnAuthenticationSuccessShouldSetContentType() throws IOException, ServletException {
        try (MockedStatic<JWTUtils> jwtUtils = mockStatic(JWTUtils.class)) {
            MockHttpServletRequest request = new MockHttpServletRequest();
            MockHttpServletResponse response = new MockHttpServletResponse();

            TUser user = new TUser();
            user.setId(1);

            Authentication authentication = mock(Authentication.class);
            when(authentication.getPrincipal()).thenReturn(user);

            jwtUtils.when(() -> JWTUtils.createJWT(anyString())).thenReturn("jwt.token");

            successHandler.onAuthenticationSuccess(request, response, authentication);

            assertTrue(response.getContentType().contains("application/json"));
        }
    }
}
