package com.autodealer.crm.bootstrap.security;

import com.autodealer.crm.modules.identity.application.api.security.SessionAuthenticationDetails;
import com.autodealer.crm.shared.infrastructure.cache.RedisKeys;
import com.autodealer.crm.shared.infrastructure.cache.RedisManager;
import com.autodealer.crm.modules.identity.application.api.model.TUser;
import com.autodealer.crm.shared.error.CodeEnum;
import com.autodealer.crm.modules.identity.application.api.UserSessionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MyLogoutSuccessHandlerTest {
    @InjectMocks MyLogoutSuccessHandler handler;
    @Mock UserSessionService sessions;
    @Mock RedisManager redis;

    @Test void logoutRevokesOnlyCurrentSessionWithoutChangingAuthVersion() throws Exception {
        TUser user=user();UsernamePasswordAuthenticationToken auth=auth(user,new SessionAuthenticationDetails("sid-current",false));
        MockHttpServletResponse response=new MockHttpServletResponse();handler.onLogoutSuccess(new MockHttpServletRequest(),response,auth);
        verify(sessions).revokeCurrentForLogout(1,"sid-current");assertEquals(200,response.getStatus());
    }

    @Test void currentSessionRevocationFailureDoesNotReturnSuccess() throws Exception {
        TUser user=user();UsernamePasswordAuthenticationToken auth=auth(user,new SessionAuthenticationDetails("sid",false));
        doThrow(new IllegalStateException("redis unavailable")).when(sessions).revokeCurrentForLogout(1,"sid");
        MockHttpServletResponse response=new MockHttpServletResponse();handler.onLogoutSuccess(new MockHttpServletRequest(),response,auth);
        assertEquals(500,response.getStatus());
    }

    @Test void legacyLogoutDeletesOnlyLegacyKey() throws Exception {
        TUser user=user();UsernamePasswordAuthenticationToken auth=auth(user,new SessionAuthenticationDetails(null,true));when(redis.delete(RedisKeys.userLogin(1))).thenReturn(true);
        MockHttpServletResponse response=new MockHttpServletResponse();handler.onLogoutSuccess(new MockHttpServletRequest(),response,auth);
        verify(redis).delete(RedisKeys.userLogin(1));verifyNoInteractions(sessions);assertEquals(200,response.getStatus());
    }

    @Test void legacyDeleteFalseReturnsUnifiedSessionCacheFailure() throws Exception {
        TUser user=user();UsernamePasswordAuthenticationToken auth=auth(user,new SessionAuthenticationDetails(null,true));
        when(redis.delete(RedisKeys.userLogin(1))).thenReturn(false);
        MockHttpServletResponse response=new MockHttpServletResponse();handler.onLogoutSuccess(new MockHttpServletRequest(),response,auth);
        assertEquals(503,response.getStatus());
        assertTrue(response.getContentAsString().contains("\"code\":"+CodeEnum.SESSION_CACHE_FAILED.getCode()));
    }

    @Test void missingAuthenticationIsIdempotent() throws Exception {MockHttpServletResponse response=new MockHttpServletResponse();handler.onLogoutSuccess(new MockHttpServletRequest(),response,null);assertEquals(200,response.getStatus());verifyNoInteractions(sessions,redis);}

    private TUser user(){TUser u=new TUser();u.setId(1);return u;}
    private UsernamePasswordAuthenticationToken auth(TUser user,SessionAuthenticationDetails details){UsernamePasswordAuthenticationToken auth=new UsernamePasswordAuthenticationToken(user,null);auth.setDetails(details);return auth;}
}
