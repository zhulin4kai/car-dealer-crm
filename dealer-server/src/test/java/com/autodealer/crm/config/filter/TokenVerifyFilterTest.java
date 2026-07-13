package com.autodealer.crm.config.filter;

import com.autodealer.crm.constant.RedisKeys;
import com.autodealer.crm.manager.RedisManager;
import com.autodealer.crm.model.TUser;
import com.autodealer.crm.service.UserService;
import com.autodealer.crm.service.UserSessionService;
import com.autodealer.crm.config.security.UserManagementAccessGate;
import com.autodealer.crm.audit.SecurityFailureAuditService;
import com.autodealer.crm.audit.AuditActionEnum;
import com.autodealer.crm.result.CodeEnum;
import com.autodealer.crm.util.JWTUtils;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TokenVerifyFilterTest {
 @InjectMocks TokenVerifyFilter filter;@Mock RedisManager redis;@Mock UserService users;@Mock UserSessionService sessions;@Mock UserManagementAccessGate gate;@Mock SecurityFailureAuditService failureAudit;@Mock FilterChain chain;
 MockHttpServletRequest request;MockHttpServletResponse response;
 @BeforeEach void setup(){request=new MockHttpServletRequest();response=new MockHttpServletResponse();SecurityContextHolder.clearContext();ReflectionTestUtils.setField(filter,"legacyAcceptUntil","2099-01-01T00:00:00Z");lenient().when(gate.evaluate(any(),any())).thenReturn(UserManagementAccessGate.Decision.allow());}
 @Test void publicLoginPasses() throws Exception {request.setMethod("POST");request.setRequestURI("/api/login");filter.doFilterInternal(request,response,chain);verify(chain).doFilter(request,response);}
 @Test void loginFreeRequiresToken(){request.setRequestURI("/api/login/free");assertDoesNotThrow(()->filter.doFilterInternal(request,response,chain));assertEquals(401,response.getStatus());}
 @Test void missingTokenFailsClosed(){request.setRequestURI("/api/users");assertDoesNotThrow(()->filter.doFilterInternal(request,response,chain));assertEquals(401,response.getStatus());}
 @Test void validNewSessionSetsAuthentication() throws Exception {try(MockedStatic<JWTUtils> jwt=newJwt(1,"sid",0L)){TUser user=usable();when(sessions.validateAndTouch("valid",1,"sid",0L)).thenReturn(true);when(users.getLoginUserById(1)).thenReturn(user);invoke("/api/users");assertSame(user,SecurityContextHolder.getContext().getAuthentication().getPrincipal());verify(chain).doFilter(request,response);verifyNoInteractions(redis);}}
 @Test void ineligiblePrincipalFromAuthoritativeUserLookupFailsClosedAndClearsSession() throws Exception {try(MockedStatic<JWTUtils> jwt=newJwt(1,"sid",0L)){when(sessions.validateAndTouch("valid",1,"sid",0L)).thenReturn(true);when(users.getLoginUserById(1)).thenReturn(null);when(redis.delete(RedisKeys.userSession("sid"))).thenReturn(true);invoke("/api/users");assertEquals(401,response.getStatus());assertNull(SecurityContextHolder.getContext().getAuthentication());verify(redis).delete(RedisKeys.userSession("sid"));verifyNoInteractions(gate,chain);}}
 @Test void newSessionFailureNeverFallsBackToLegacyKey() throws Exception {try(MockedStatic<JWTUtils> jwt=newJwt(1,"sid",0L)){when(sessions.validateAndTouch("valid",1,"sid",0L)).thenReturn(false);invoke("/api/users");assertEquals(401,response.getStatus());verify(redis,never()).get(anyString());}}
 @Test void authVersionMismatchFailsAndClearsExactSessionKey() throws Exception {try(MockedStatic<JWTUtils> jwt=newJwt(1,"sid",0L)){TUser user=usable();user.setAuthVersion(1L);when(sessions.validateAndTouch("valid",1,"sid",0L)).thenReturn(true);when(users.getLoginUserById(1)).thenReturn(user);when(redis.delete(RedisKeys.userSession("sid"))).thenReturn(true);invoke("/api/users");assertEquals(401,response.getStatus());verify(redis).delete(RedisKeys.userSession("sid"));}}
 @Test void legacySessionWorksOnlyBeforeConfiguredCutoff() throws Exception {try(MockedStatic<JWTUtils> jwt=newJwt(1,null,0L)){TUser user=usable();when(redis.get(RedisKeys.userLogin(1))).thenReturn("valid");when(users.getLoginUserById(1)).thenReturn(user);invoke("/api/users");verify(chain).doFilter(request,response);}SecurityContextHolder.clearContext();reset(chain,redis,users);ReflectionTestUtils.setField(filter,"legacyAcceptUntil","2000-01-01T00:00:00Z");try(MockedStatic<JWTUtils> jwt=newJwt(1,null,0L)){invoke("/api/users");assertEquals(401,response.getStatus());verifyNoInteractions(redis,users,chain);}}
 @Test void mustChangePasswordCannotOpenProfileButCanOpenNecessarySessions() throws Exception {try(MockedStatic<JWTUtils> jwt=newJwt(1,"sid",0L)){TUser user=usable();user.setMustChangePassword(true);when(sessions.validateAndTouch("valid",1,"sid",0L)).thenReturn(true);when(users.getLoginUserById(1)).thenReturn(user);invoke("/api/profile");assertEquals(403,response.getStatus());verifyNoInteractions(chain);}SecurityContextHolder.clearContext();request=new MockHttpServletRequest();response=new MockHttpServletResponse();try(MockedStatic<JWTUtils> jwt=newJwt(1,"sid",0L)){TUser user=usable();user.setMustChangePassword(true);when(sessions.validateAndTouch("valid",1,"sid",0L)).thenReturn(true);when(users.getLoginUserById(1)).thenReturn(user);invoke("/api/me/sessions");verify(chain).doFilter(request,response);}}
 @Test void userManagementGateRejectionWritesIndependentSecurityAudit() throws Exception {try(MockedStatic<JWTUtils> jwt=newJwt(1,"sid",0L)){TUser user=usable();when(sessions.validateAndTouch("valid",1,"sid",0L)).thenReturn(true);when(users.getLoginUserById(1)).thenReturn(user);when(gate.evaluate(eq(user),any())).thenReturn(UserManagementAccessGate.Decision.deny(CodeEnum.ADMIN_BOOTSTRAP_REQUIRED,"初始化未完成"));invoke("/api/users");assertEquals(403,response.getStatus());verify(failureAudit).recordAuthenticated(eq(AuditActionEnum.USER_MANAGEMENT_GATE_REJECTED),eq("1"),contains("641"),eq(user));verifyNoInteractions(chain);}}
 @Test void redisOrSessionValidationExceptionFailsClosed() throws Exception {try(MockedStatic<JWTUtils> jwt=newJwt(1,"sid",0L)){when(sessions.validateAndTouch("valid",1,"sid",0L)).thenThrow(new IllegalStateException("redis"));invoke("/api/users");assertEquals(401,response.getStatus());assertNull(SecurityContextHolder.getContext().getAuthentication());}}
 private void invoke(String path)throws Exception{request.setRequestURI(path);request.addHeader("Authorization","Bearer valid");filter.doFilterInternal(request,response,chain);}
 private MockedStatic<JWTUtils> newJwt(Integer uid,String sid,Long av){MockedStatic<JWTUtils> jwt=mockStatic(JWTUtils.class);jwt.when(()->JWTUtils.verifyJWT("valid")).thenReturn(true);jwt.when(()->JWTUtils.parseUserIdFromJWT("valid")).thenReturn(uid);jwt.when(()->JWTUtils.parseAuthVersionFromJWT("valid")).thenReturn(av);jwt.when(()->JWTUtils.parseSessionIdFromJWT("valid")).thenReturn(sid);return jwt;}
 private TUser usable(){TUser u=new TUser();u.setId(1);u.setAuthVersion(0L);u.setAccountEnabled(1);u.setAccountNoExpired(1);u.setAccountNoLocked(1);u.setCredentialsNoExpired(1);u.setManualLocked(false);return u;}
}
