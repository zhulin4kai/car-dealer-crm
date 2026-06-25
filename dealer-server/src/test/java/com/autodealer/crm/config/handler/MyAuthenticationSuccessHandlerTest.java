package com.autodealer.crm.config.handler;

import com.autodealer.crm.constant.Constants;
import com.autodealer.crm.manager.RedisManager;
import com.autodealer.crm.model.TUser;
import com.autodealer.crm.result.CodeEnum;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MyAuthenticationSuccessHandlerTest {

    @InjectMocks
    private MyAuthenticationSuccessHandler successHandler;

    @Mock
    private RedisManager redisManager;

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

            jwtUtils.when(() -> JWTUtils.createJWT(1, "admin", Constants.DEFAULT_EXPIRE_TIME)).thenReturn("generated.jwt.token");
            when(redisManager.set(Constants.REDIS_JWT_KEY + 1, "generated.jwt.token", Constants.DEFAULT_EXPIRE_TIME))
                    .thenReturn(true);

            successHandler.onAuthenticationSuccess(request, response, authentication);

            String content = response.getContentAsString();
            assertEquals(200, response.getStatus());
            assertTrue(content.contains("generated.jwt.token"));
            assertTrue(content.contains("200"));

            verify(redisManager).set(Constants.REDIS_JWT_KEY + 1, "generated.jwt.token", Constants.DEFAULT_EXPIRE_TIME);
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
            user.setLoginAct("admin");

            Authentication authentication = mock(Authentication.class);
            when(authentication.getPrincipal()).thenReturn(user);

            jwtUtils.when(() -> JWTUtils.createJWT(1, "admin", Constants.EXPIRE_TIME)).thenReturn("jwt.token");
            when(redisManager.set(Constants.REDIS_JWT_KEY + 1, "jwt.token", Constants.EXPIRE_TIME))
                    .thenReturn(true);

            successHandler.onAuthenticationSuccess(request, response, authentication);

            verify(redisManager).set(Constants.REDIS_JWT_KEY + 1, "jwt.token", Constants.EXPIRE_TIME);
        }
    }

    @Test
    void testOnAuthenticationSuccessWithoutRememberMeShouldSetDefaultExpiry() throws IOException, ServletException {
        try (MockedStatic<JWTUtils> jwtUtils = mockStatic(JWTUtils.class)) {
            MockHttpServletRequest request = new MockHttpServletRequest();
            MockHttpServletResponse response = new MockHttpServletResponse();

            TUser user = new TUser();
            user.setId(1);
            user.setLoginAct("admin");

            Authentication authentication = mock(Authentication.class);
            when(authentication.getPrincipal()).thenReturn(user);

            jwtUtils.when(() -> JWTUtils.createJWT(1, "admin", Constants.DEFAULT_EXPIRE_TIME)).thenReturn("jwt.token");
            when(redisManager.set(Constants.REDIS_JWT_KEY + 1, "jwt.token", Constants.DEFAULT_EXPIRE_TIME))
                    .thenReturn(true);

            successHandler.onAuthenticationSuccess(request, response, authentication);

            verify(redisManager).set(Constants.REDIS_JWT_KEY + 1, "jwt.token", Constants.DEFAULT_EXPIRE_TIME);
        }
    }

    @Test
    void testOnAuthenticationSuccessShouldSetContentType() throws IOException, ServletException {
        try (MockedStatic<JWTUtils> jwtUtils = mockStatic(JWTUtils.class)) {
            MockHttpServletRequest request = new MockHttpServletRequest();
            MockHttpServletResponse response = new MockHttpServletResponse();

            TUser user = new TUser();
            user.setId(1);
            user.setLoginAct("admin");

            Authentication authentication = mock(Authentication.class);
            when(authentication.getPrincipal()).thenReturn(user);

            jwtUtils.when(() -> JWTUtils.createJWT(1, "admin", Constants.DEFAULT_EXPIRE_TIME)).thenReturn("jwt.token");
            when(redisManager.set(Constants.REDIS_JWT_KEY + 1, "jwt.token", Constants.DEFAULT_EXPIRE_TIME))
                    .thenReturn(true);

            successHandler.onAuthenticationSuccess(request, response, authentication);

            assertTrue(response.getContentType().contains("application/json"));
        }
    }

    @Test
    void redisWriteFailureShouldRejectLoginWithoutReturningJwt() throws IOException, ServletException {
        try (MockedStatic<JWTUtils> jwtUtils = mockStatic(JWTUtils.class)) {
            MockHttpServletRequest request = new MockHttpServletRequest();
            MockHttpServletResponse response = new MockHttpServletResponse();

            TUser user = new TUser();
            user.setId(1);
            user.setLoginAct("admin");

            Authentication authentication = mock(Authentication.class);
            when(authentication.getPrincipal()).thenReturn(user);

            jwtUtils.when(() -> JWTUtils.createJWT(1, "admin", Constants.DEFAULT_EXPIRE_TIME)).thenReturn("jwt.token");
            when(redisManager.set(Constants.REDIS_JWT_KEY + 1, "jwt.token", Constants.DEFAULT_EXPIRE_TIME))
                    .thenReturn(false);

            successHandler.onAuthenticationSuccess(request, response, authentication);

            String content = response.getContentAsString();
            assertEquals(500, response.getStatus());
            assertTrue(content.contains("\"code\":" + CodeEnum.SYSTEM_ERROR.getCode()));
            assertFalse(content.contains("jwt.token"));
        }
    }

    @Test
    void redisWriteExceptionShouldRejectLoginWithoutReturningJwt() throws IOException, ServletException {
        try (MockedStatic<JWTUtils> jwtUtils = mockStatic(JWTUtils.class)) {
            MockHttpServletRequest request = new MockHttpServletRequest();
            MockHttpServletResponse response = new MockHttpServletResponse();

            TUser user = new TUser();
            user.setId(1);
            user.setLoginAct("admin");

            Authentication authentication = mock(Authentication.class);
            when(authentication.getPrincipal()).thenReturn(user);

            jwtUtils.when(() -> JWTUtils.createJWT(1, "admin", Constants.DEFAULT_EXPIRE_TIME)).thenReturn("jwt.token");
            when(redisManager.set(Constants.REDIS_JWT_KEY + 1, "jwt.token", Constants.DEFAULT_EXPIRE_TIME))
                    .thenThrow(new IllegalStateException("redis unavailable"));

            successHandler.onAuthenticationSuccess(request, response, authentication);

            String content = response.getContentAsString();
            assertEquals(500, response.getStatus());
            assertTrue(content.contains("\"code\":" + CodeEnum.SYSTEM_ERROR.getCode()));
            assertFalse(content.contains("jwt.token"));
        }
    }
}
