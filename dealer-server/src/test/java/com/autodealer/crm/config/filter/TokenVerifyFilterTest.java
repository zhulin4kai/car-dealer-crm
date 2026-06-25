package com.autodealer.crm.config.filter;

import com.autodealer.crm.constant.Constants;
import com.autodealer.crm.manager.RedisManager;
import com.autodealer.crm.model.TUser;
import com.autodealer.crm.result.CodeEnum;
import com.autodealer.crm.service.UserService;
import com.autodealer.crm.util.JWTUtils;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TokenVerifyFilterTest {

    @InjectMocks
    private TokenVerifyFilter filter;
    @Mock
    private RedisManager redisManager;
    @Mock
    private UserService userService;
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
    void publicPathShouldPassThrough() throws Exception {
        request.setMethod("POST");
        request.setRequestURI(Constants.LOGIN_URI);
        filter.doFilterInternal(request, response, filterChain);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void missingTokenShouldFailClosed() throws Exception {
        request.setRequestURI("/api/users");
        filter.doFilterInternal(request, response, filterChain);
        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString().contains("510"));
        verifyNoInteractions(filterChain);
    }

    @Test
    void invalidTokenShouldFailClosed() throws Exception {
        request.setRequestURI("/api/users");
        request.addHeader("Authorization", "bad-token");
        filter.doFilterInternal(request, response, filterChain);
        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString().contains("511"));
        verifyNoInteractions(filterChain);
    }

    @Test
    void missingRedisSessionShouldBeExpired() throws Exception {
        try (MockedStatic<JWTUtils> jwt = mockJwt(1)) {
            invokeProtectedPath();
            assertEquals(401, response.getStatus());
            assertTrue(response.getContentAsString().contains("512"));
            verifyNoInteractions(filterChain);
        }
    }

    @Test
    void mismatchedRedisTokenShouldBeRejected() throws Exception {
        try (MockedStatic<JWTUtils> jwt = mockJwt(1)) {
            when(redisManager.get(Constants.REDIS_JWT_KEY + 1)).thenReturn("other-token");
            invokeProtectedPath();
            assertEquals(401, response.getStatus());
            assertTrue(response.getContentAsString().contains("513"));
            verifyNoInteractions(filterChain);
        }
    }

    @Test
    void validTokenShouldSetAuthenticationWithoutRefreshingRedis() throws Exception {
        try (MockedStatic<JWTUtils> jwt = mockJwt(1)) {
            TUser user = usableUser();
            when(redisManager.get(Constants.REDIS_JWT_KEY + 1)).thenReturn("valid-token");
            when(userService.getLoginUserById(1)).thenReturn(user);

            invokeProtectedPath();

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            assertSame(user, authentication.getPrincipal());
            assertNull(authentication.getCredentials());
            verify(filterChain).doFilter(request, response);
            verify(redisManager, never()).set(any(), any(), anyLong());
        }
    }

    @Test
    void disabledUserShouldBeRejectedAndSessionRevoked() throws Exception {
        try (MockedStatic<JWTUtils> jwt = mockJwt(1)) {
            TUser user = usableUser();
            user.setAccountEnabled(0);
            when(redisManager.get(Constants.REDIS_JWT_KEY + 1)).thenReturn("valid-token");
            when(userService.getLoginUserById(1)).thenReturn(user);
            when(redisManager.delete(Constants.REDIS_JWT_KEY + 1)).thenReturn(true);

            invokeProtectedPath();

            verify(redisManager).delete(Constants.REDIS_JWT_KEY + 1);
            verifyNoInteractions(filterChain);
            assertNull(SecurityContextHolder.getContext().getAuthentication());
            assertEquals(401, response.getStatus());
            assertTrue(response.getContentAsString().contains("511"));
        }
    }

    @Test
    void disabledUserSessionRevokeFailureShouldFailWithSystemError() throws Exception {
        try (MockedStatic<JWTUtils> jwt = mockJwt(1)) {
            TUser user = usableUser();
            user.setAccountEnabled(0);
            when(redisManager.get(Constants.REDIS_JWT_KEY + 1)).thenReturn("valid-token");
            when(userService.getLoginUserById(1)).thenReturn(user);
            when(redisManager.delete(Constants.REDIS_JWT_KEY + 1)).thenReturn(false);

            invokeProtectedPath();

            verify(redisManager).delete(Constants.REDIS_JWT_KEY + 1);
            verifyNoInteractions(filterChain);
            assertNull(SecurityContextHolder.getContext().getAuthentication());
            assertEquals(500, response.getStatus());
            assertTrue(response.getContentAsString().contains("\"code\":" + CodeEnum.SYSTEM_ERROR.getCode()));
        }
    }

    private void invokeProtectedPath() throws Exception {
        request.setRequestURI("/api/users");
        request.addHeader("Authorization", "Bearer valid-token");
        filter.doFilterInternal(request, response, filterChain);
    }

    private MockedStatic<JWTUtils> mockJwt(Integer userId) {
        MockedStatic<JWTUtils> jwt = mockStatic(JWTUtils.class);
        jwt.when(() -> JWTUtils.verifyJWT("valid-token")).thenReturn(true);
        jwt.when(() -> JWTUtils.parseUserIdFromJWT("valid-token")).thenReturn(userId);
        return jwt;
    }

    private TUser usableUser() {
        TUser user = new TUser();
        user.setId(1);
        user.setLoginAct("admin");
        user.setAccountEnabled(1);
        user.setAccountNoExpired(1);
        user.setAccountNoLocked(1);
        user.setCredentialsNoExpired(1);
        return user;
    }
}
