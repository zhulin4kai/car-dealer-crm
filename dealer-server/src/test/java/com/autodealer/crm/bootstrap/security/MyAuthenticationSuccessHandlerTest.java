package com.autodealer.crm.bootstrap.security;

import com.autodealer.crm.modules.audit.application.api.LoginAuditRecorder;
import com.autodealer.crm.modules.identity.application.api.dto.user.UserSessionDtos.Issued;
import com.autodealer.crm.modules.identity.application.api.model.TUser;
import com.autodealer.crm.shared.error.CodeEnum;
import com.autodealer.crm.modules.identity.application.api.LoginSecurityService;
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
class MyAuthenticationSuccessHandlerTest {
    @InjectMocks MyAuthenticationSuccessHandler handler;
    @Mock LoginAuditRecorder audit;
    @Mock LoginSecurityService loginSecurity;
    @Mock UserSessionService sessions;

    @Test void successfulSessionCreationReturnsTokenOnlyAfterAudit() throws Exception {
        TUser user=user();MockHttpServletRequest request=new MockHttpServletRequest();MockHttpServletResponse response=new MockHttpServletResponse();
        when(sessions.create(user,false,request)).thenReturn(new Issued("new.jwt","opaque-sid"));
        handler.onAuthenticationSuccess(request,response,authentication(user));
        assertEquals(200,response.getStatus());assertTrue(response.getContentAsString().contains("new.jwt"));
        verify(loginSecurity).recordSuccess(1);verify(audit).recordSuccess(user,request);
    }

    @Test void rememberMeIsPassedToSessionPolicy() throws Exception {
        TUser user=user();MockHttpServletRequest request=new MockHttpServletRequest();request.setParameter("rememberMe","true");MockHttpServletResponse response=new MockHttpServletResponse();
        when(sessions.create(user,true,request)).thenReturn(new Issued("remember.jwt","sid"));
        handler.onAuthenticationSuccess(request,response,authentication(user));
        verify(sessions).create(user,true,request);assertEquals(200,response.getStatus());
    }

    @Test void sessionInfrastructureFailureNeverReturnsToken() throws Exception {
        TUser user=user();MockHttpServletRequest request=new MockHttpServletRequest();MockHttpServletResponse response=new MockHttpServletResponse();
        when(sessions.create(user,false,request)).thenThrow(new IllegalStateException("redis unavailable"));
        handler.onAuthenticationSuccess(request,response,authentication(user));
        assertEquals(500,response.getStatus());assertTrue(response.getContentAsString().contains("\"code\":"+CodeEnum.SYSTEM_ERROR.getCode()));assertFalse(response.getContentAsString().contains("jwt"));
    }

    @Test void auditFailureRevokesNewSessionAndRejectsLogin() throws Exception {
        TUser user=user();MockHttpServletRequest request=new MockHttpServletRequest();MockHttpServletResponse response=new MockHttpServletResponse();
        when(sessions.create(user,false,request)).thenReturn(new Issued("new.jwt","opaque-sid"));
        doThrow(new IllegalStateException("audit unavailable")).when(audit).recordSuccess(user,request);
        handler.onAuthenticationSuccess(request,response,authentication(user));
        verify(sessions).revokeCurrentForLogout(1,"opaque-sid");assertEquals(500,response.getStatus());assertFalse(response.getContentAsString().contains("new.jwt"));
    }

    @Test void loginSecurityFailurePreventsSessionCreation() throws Exception {
        TUser user=user();MockHttpServletRequest request=new MockHttpServletRequest();MockHttpServletResponse response=new MockHttpServletResponse();
        doThrow(new IllegalStateException("db unavailable")).when(loginSecurity).recordSuccess(1);
        handler.onAuthenticationSuccess(request,response,authentication(user));
        assertEquals(500,response.getStatus());verifyNoInteractions(sessions);
    }

    private TUser user(){TUser user=new TUser();user.setId(1);user.setLoginAct("admin");user.setAuthVersion(7L);return user;}
    private UsernamePasswordAuthenticationToken authentication(TUser user){return new UsernamePasswordAuthenticationToken(user,null,user.getAuthorities());}
}
