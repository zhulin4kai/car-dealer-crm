package com.autodealer.crm.modules.audit.application.api;

import com.autodealer.crm.modules.audit.persistence.mapper.TLoginLogMapper;
import com.autodealer.crm.modules.identity.application.api.LoginAuditUserLookup;
import com.autodealer.crm.modules.audit.persistence.model.TLoginLog;
import com.autodealer.crm.modules.identity.application.api.model.TUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LoginAuditRecorder 单元测试")
class LoginAuditRecorderTest {

    private LoginAuditRecorder recorder;

    @Mock
    private TLoginLogMapper loginLogMapper;

    @Mock
    private LoginAuditUserLookup userLookup;

    @BeforeEach
    void setUp() {
        recorder = new LoginAuditRecorder(loginLogMapper, userLookup);
    }

    @Test
    @DisplayName("登录成功应写入成功结果、用户事实和请求环境")
    void recordSuccess_shouldWriteUserAndRequestFacts() {
        when(loginLogMapper.insert(any(TLoginLog.class))).thenReturn(1);
        TUser user = new TUser();
        user.setId(7);
        user.setLoginAct("admin");
        user.setName("管理员");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("User-Agent", "Mozilla/5.0 Chrome/120.0.0.0 Safari/537.36");
        request.addHeader("X-Request-Id", "req-login-success");

        recorder.recordSuccess(user, request);

        ArgumentCaptor<TLoginLog> captor = ArgumentCaptor.forClass(TLoginLog.class);
        verify(loginLogMapper).insert(captor.capture());
        TLoginLog log = captor.getValue();
        assertEquals("admin", log.getLoginAct());
        assertEquals(7, log.getUserId());
        assertEquals("管理员", log.getUserName());
        assertEquals("SUCCESS", log.getResult());
        assertEquals("SUCCESS", log.getReasonCode());
        assertEquals("127.0.0.1", log.getIp());
        assertEquals("Chrome", log.getBrowser());
        assertEquals("req-login-success", log.getRequestId());
        assertNotNull(log.getCreateTime());
    }

    @Test
    @DisplayName("登录失败应写入失败原因并关联已存在用户")
    void recordFailure_shouldWriteReasonAndKnownUser() {
        when(loginLogMapper.insert(any(TLoginLog.class))).thenReturn(1);
        when(userLookup.findByLoginAct("sales"))
                .thenReturn(Optional.of(new LoginAuditUserLookup.UserSummary(8, "销售顾问")));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.8");
        request.addHeader("User-Agent", "Mozilla/5.0 Firefox/120.0");

        recorder.recordFailure("sales", new LockedException("locked"), request);

        ArgumentCaptor<TLoginLog> captor = ArgumentCaptor.forClass(TLoginLog.class);
        verify(loginLogMapper).insert(captor.capture());
        TLoginLog log = captor.getValue();
        assertEquals("sales", log.getLoginAct());
        assertEquals(8, log.getUserId());
        assertEquals("销售顾问", log.getUserName());
        assertEquals("FAILURE", log.getResult());
        assertEquals("ACCOUNT_LOCKED", log.getReasonCode());
        assertEquals("账号已锁定", log.getReasonMessage());
        assertEquals("Firefox", log.getBrowser());
    }

    @Test
    @DisplayName("登录失败审计不得写入密码或异常细节")
    void recordFailure_shouldNotPersistPasswordOrExceptionMessage() {
        when(loginLogMapper.insert(any(TLoginLog.class))).thenReturn(1);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("loginPwd", "secret-password");

        recorder.recordFailure("ghost", new BadCredentialsException("raw password secret-password"), request);

        ArgumentCaptor<TLoginLog> captor = ArgumentCaptor.forClass(TLoginLog.class);
        verify(loginLogMapper).insert(captor.capture());
        TLoginLog log = captor.getValue();
        String combined = log.getLoginAct() + log.getReasonCode() + log.getReasonMessage();
        assertFalse(combined.contains("secret-password"));
        assertEquals("BAD_CREDENTIALS", log.getReasonCode());
    }

    @Test
    @DisplayName("审计插入影响行数不为 1 时应抛出异常")
    void recordSuccess_whenInsertReturnsZero_shouldThrow() {
        when(loginLogMapper.insert(any(TLoginLog.class))).thenReturn(0);
        TUser user = new TUser();
        user.setId(7);
        user.setLoginAct("admin");
        user.setName("管理员");

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> recorder.recordSuccess(user, new MockHttpServletRequest()));
        assertTrue(exception.getMessage().contains("登录审计记录写入失败"));
    }
}
