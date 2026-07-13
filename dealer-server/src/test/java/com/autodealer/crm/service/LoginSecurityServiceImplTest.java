package com.autodealer.crm.service;

import com.autodealer.crm.audit.AuditActionEnum;
import com.autodealer.crm.audit.OperationAuditRecorder;
import com.autodealer.crm.mapper.TUserMapper;
import com.autodealer.crm.mapper.TAuthorizationGraphLockMapper;
import com.autodealer.crm.model.TUser;
import com.autodealer.crm.service.impl.LoginSecurityServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoginSecurityServiceImplTest {
    @Mock TUserMapper mapper;
    @Mock OperationAuditRecorder audit;
    @Mock TAuthorizationGraphLockMapper graphLock;
    @Mock UserSessionService sessions;

    @Test
    void fifthFailureUsesAtomicCounterAndWritesAnonymousLockAudit() {
        TUser user = new TUser();
        user.setId(8);
        user.setFailedLoginCount(4);
        user.setManualLocked(false);
        when(mapper.selectByLoginAct("sales01")).thenReturn(user);
        when(graphLock.lockByName(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
        when(mapper.selectByPrimaryKeyForUpdate(8)).thenReturn(user);
        when(mapper.recordLoginFailureByExpected(eq(8), eq(4), any(LocalDateTime.class),eq(true))).thenReturn(1);

        new LoginSecurityServiceImpl(mapper,graphLock,audit,sessions).recordFailure("sales01");

        verify(audit).recordAnonymous(AuditActionEnum.USER_LOGIN_AUTO_LOCK, "8", "SUCCESS",
                "{\"threshold\":5,\"lockMinutes\":15}");
        verify(sessions).revokeAllForSecurityChange(8,null,"登录失败自动锁定");
        InOrder locks = inOrder(graphLock, mapper);
        locks.verify(graphLock).lockByName("REPORTING_GRAPH");
        locks.verify(graphLock).lockByName("AVAILABLE_ADMIN_GUARD");
        locks.verify(mapper).selectByPrimaryKeyForUpdate(8);
    }

    @Test
    void unknownAccountDoesNotCreateOperationAudit() {
        when(mapper.selectByLoginAct("missing")).thenReturn(null);
        new LoginSecurityServiceImpl(mapper,graphLock,audit,sessions).recordFailure("missing");
        verifyNoInteractions(audit);
        verify(mapper, never()).recordLoginFailureByExpected(anyInt(), anyInt(), any(),anyBoolean());
    }

    @Test
    void protectedRecoveryAccountNeverUsesOrdinaryAutoLock() {
        TUser user = new TUser();user.setId(1);user.setFailedLoginCount(4);user.setManualLocked(false);user.setProtectedAccount(true);
        when(mapper.selectByLoginAct("admin")).thenReturn(user);
        when(graphLock.lockByName(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
        when(mapper.selectByPrimaryKeyForUpdate(1)).thenReturn(user);
        when(mapper.recordLoginFailureByExpected(eq(1),eq(4),any(),eq(false))).thenReturn(1);

        new LoginSecurityServiceImpl(mapper,graphLock,audit,sessions).recordFailure("admin");

        verify(audit).recordAnonymous(AuditActionEnum.USER_LOGIN_AUTO_LOCK_BYPASSED,"1","SUCCESS",
                "{\"threshold\":5,\"reason\":\"ADMIN_AVAILABILITY_PROTECTION\"}");
        verifyNoInteractions(sessions);
    }

    @Test
    void soleOrdinaryAdministratorIsKeptAvailable() {
        TUser user = new TUser();user.setId(9);user.setFailedLoginCount(4);user.setManualLocked(false);user.setProtectedAccount(false);
        com.autodealer.crm.model.TRole admin=new com.autodealer.crm.model.TRole();admin.setRole("admin");
        when(mapper.selectByLoginAct("only-admin")).thenReturn(user);
        when(graphLock.lockByName(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
        when(mapper.selectByPrimaryKeyForUpdate(9)).thenReturn(user);
        when(mapper.selectRolesByUserId(9)).thenReturn(java.util.List.of(admin));
        when(mapper.countAvailableAdminUsersExcluding(9)).thenReturn(0);
        when(mapper.recordLoginFailureByExpected(eq(9),eq(4),any(),eq(false))).thenReturn(1);

        new LoginSecurityServiceImpl(mapper,graphLock,audit,sessions).recordFailure("only-admin");

        verify(mapper).recordLoginFailureByExpected(eq(9),eq(4),any(),eq(false));
        verifyNoInteractions(sessions);
    }
}
