package com.bjpowernode.config.filter;

import com.bjpowernode.constant.Constants;
import com.bjpowernode.manager.RedisManager;
import com.bjpowernode.model.TUser;
import com.bjpowernode.util.JWTUtils;
import com.bjpowernode.util.JSONUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TokenVerifyFilterTest {

    @InjectMocks
    private TokenVerifyFilter tokenVerifyFilter;

    @Mock
    private RedisManager redisManager;

    @Mock
    private ThreadPoolTaskExecutor threadPoolTaskExecutor;

    @Mock
    private FilterChain filterChain;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        SecurityContextHolder.clearContext();
    }

    @Test
    void testLoginUriShouldPassThrough() throws ServletException, IOException {
        request.setRequestURI(Constants.LOGIN_URI);

        tokenVerifyFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void testNoTokenShouldReturnTokenEmpty() throws ServletException, IOException {
        request.setRequestURI("/api/users");

        tokenVerifyFilter.doFilterInternal(request, response, filterChain);

        String content = response.getContentAsString();
        assertTrue(content.contains("token为空") || content.contains("510"));
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    void testInvalidTokenShouldReturnTokenError() throws ServletException, IOException {
        request.setRequestURI("/api/users");
        request.addHeader("Authorization", "invalid.token.value");

        tokenVerifyFilter.doFilterInternal(request, response, filterChain);

        String content = response.getContentAsString();
        assertTrue(content.contains("token无效") || content.contains("511"));
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    void testExpiredTokenShouldReturnTokenExpired() throws ServletException, IOException {
        try (MockedStatic<JWTUtils> jwtUtils = mockStatic(JWTUtils.class)) {
            request.setRequestURI("/api/users");
            request.addHeader("Authorization", "valid.jwt.token");

            TUser user = new TUser();
            user.setId(1);

            jwtUtils.when(() -> JWTUtils.verifyJWT("valid.jwt.token")).thenReturn(true);
            jwtUtils.when(() -> JWTUtils.parseUserFromJWT("valid.jwt.token")).thenReturn(user);

            when(redisManager.get(Constants.REDIS_JWT_KEY + 1)).thenReturn(null);

            tokenVerifyFilter.doFilterInternal(request, response, filterChain);

            String content = response.getContentAsString();
            assertTrue(content.contains("token已过期") || content.contains("512"));
            verify(filterChain, never()).doFilter(any(), any());
        }
    }

    @Test
    void testTokenMismatchShouldReturnTokenNoneMatch() throws ServletException, IOException {
        try (MockedStatic<JWTUtils> jwtUtils = mockStatic(JWTUtils.class)) {
            request.setRequestURI("/api/users");
            request.addHeader("Authorization", "valid.jwt.token");

            TUser user = new TUser();
            user.setId(1);

            jwtUtils.when(() -> JWTUtils.verifyJWT("valid.jwt.token")).thenReturn(true);
            jwtUtils.when(() -> JWTUtils.parseUserFromJWT("valid.jwt.token")).thenReturn(user);

            when(redisManager.get(Constants.REDIS_JWT_KEY + 1)).thenReturn("different.token");

            tokenVerifyFilter.doFilterInternal(request, response, filterChain);

            String content = response.getContentAsString();
            assertTrue(content.contains("token不匹配") || content.contains("513"));
            verify(filterChain, never()).doFilter(any(), any());
        }
    }

    @Test
    void testValidTokenShouldSetAuthenticationAndProceed() throws ServletException, IOException {
        try (MockedStatic<JWTUtils> jwtUtils = mockStatic(JWTUtils.class)) {
            request.setRequestURI("/api/users");
            request.addHeader("Authorization", "valid.jwt.token");

            TUser user = new TUser();
            user.setId(1);
            user.setLoginAct("admin");
            user.setLoginPwd("password");

            jwtUtils.when(() -> JWTUtils.verifyJWT("valid.jwt.token")).thenReturn(true);
            jwtUtils.when(() -> JWTUtils.parseUserFromJWT("valid.jwt.token")).thenReturn(user);

            when(redisManager.get(Constants.REDIS_JWT_KEY + 1)).thenReturn("valid.jwt.token");

            tokenVerifyFilter.doFilterInternal(request, response, filterChain);

            assertNotNull(SecurityContextHolder.getContext().getAuthentication());
            verify(filterChain).doFilter(request, response);
        }
    }

    @Test
    void testRememberMeHeaderShouldExpireWithLongTime() throws ServletException, IOException {
        try (MockedStatic<JWTUtils> jwtUtils = mockStatic(JWTUtils.class)) {
            request.setRequestURI("/api/users");
            request.addHeader("Authorization", "valid.jwt.token");
            request.addHeader("rememberMe", "true");

            TUser user = new TUser();
            user.setId(1);
            user.setLoginAct("admin");

            jwtUtils.when(() -> JWTUtils.verifyJWT("valid.jwt.token")).thenReturn(true);
            jwtUtils.when(() -> JWTUtils.parseUserFromJWT("valid.jwt.token")).thenReturn(user);

            when(redisManager.get(Constants.REDIS_JWT_KEY + 1)).thenReturn("valid.jwt.token");

            doAnswer(invocation -> {
                Runnable runnable = invocation.getArgument(0);
                runnable.run();
                return null;
            }).when(threadPoolTaskExecutor).execute(any(Runnable.class));

            tokenVerifyFilter.doFilterInternal(request, response, filterChain);

            verify(threadPoolTaskExecutor).execute(any(Runnable.class));
            verify(redisManager).set(eq(Constants.REDIS_JWT_KEY + 1), eq("valid.jwt.token"), eq(Constants.EXPIRE_TIME));
        }
    }

    @Test
    void testNoRememberMeShouldExpireWithDefaultTime() throws ServletException, IOException {
        try (MockedStatic<JWTUtils> jwtUtils = mockStatic(JWTUtils.class)) {
            request.setRequestURI("/api/users");
            request.addHeader("Authorization", "valid.jwt.token");

            TUser user = new TUser();
            user.setId(1);
            user.setLoginAct("admin");

            jwtUtils.when(() -> JWTUtils.verifyJWT("valid.jwt.token")).thenReturn(true);
            jwtUtils.when(() -> JWTUtils.parseUserFromJWT("valid.jwt.token")).thenReturn(user);

            when(redisManager.get(Constants.REDIS_JWT_KEY + 1)).thenReturn("valid.jwt.token");

            doAnswer(invocation -> {
                Runnable runnable = invocation.getArgument(0);
                runnable.run();
                return null;
            }).when(threadPoolTaskExecutor).execute(any(Runnable.class));

            tokenVerifyFilter.doFilterInternal(request, response, filterChain);

            verify(threadPoolTaskExecutor).execute(any(Runnable.class));
            verify(redisManager).set(eq(Constants.REDIS_JWT_KEY + 1), eq("valid.jwt.token"), eq(Constants.DEFAULT_EXPIRE_TIME));
        }
    }

    @Test
    void testExportExcelUriShouldGetTokenFromParameter() throws ServletException, IOException {
        request.setRequestURI(Constants.EXPORT_EXCEL_URI);

        tokenVerifyFilter.doFilterInternal(request, response, filterChain);

        String content = response.getContentAsString();
        assertTrue(content.contains("token为空") || content.contains("510"));
    }
}
