package com.autodealer.crm.service;

import com.autodealer.crm.config.security.CurrentUserProvider;
import com.autodealer.crm.config.security.UserManagementAccessGate;
import com.autodealer.crm.audit.SecurityFailureAuditService;
import com.autodealer.crm.audit.AuditActionEnum;
import com.autodealer.crm.dto.credential.CredentialDtos.AdminAccessRecoveryRequest;
import com.autodealer.crm.dto.credential.CredentialDtos.ManagedDeliveryResult;
import com.autodealer.crm.enums.AccountStatus;
import com.autodealer.crm.enums.AccountType;
import com.autodealer.crm.exception.BusinessException;
import com.autodealer.crm.result.CodeEnum;
import com.autodealer.crm.mapper.TAuthorizationGraphLockMapper;
import com.autodealer.crm.mapper.TUserMapper;
import com.autodealer.crm.mapper.TEmployeeMapper;
import com.autodealer.crm.manager.RedisManager;
import com.autodealer.crm.model.TUser;
import com.autodealer.crm.service.impl.AdminAccessRecoveryService;
import com.autodealer.crm.service.impl.CredentialDerivationCodec;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminAccessRecoveryServiceTest {
    @Mock TUserMapper users;@Mock TAuthorizationGraphLockMapper locks;@Mock CurrentUserProvider current;
    @Mock UserManagementAccessGate gate;@Mock CredentialService credentials;@Mock UserSessionService sessions;@Mock SecurityFailureAuditService failureAudit;
    @Mock TEmployeeMapper employees;@Mock RedisManager redis;
    private final CredentialDerivationCodec digester=new CredentialDerivationCodec("test-only-credential-derivation-key-00000001");

    @Test
    void degradedRecoverySelectsTargetServerSideAndUsesFixedLockOrder() {
        String key="recovery-key-for-test-environment-000001";
        when(current.getCurrentUser()).thenReturn(recovery());
        when(locks.lockByName(anyString())).thenAnswer(invocation->invocation.getArgument(0));
        when(gate.state()).thenReturn(UserManagementAccessGate.BootstrapState.DEGRADED);
        TUser target=humanAdmin(8,AccountStatus.ACTIVE);when(users.selectRecoverableAdminCandidatesForUpdate()).thenReturn(List.of(target));
        when(users.recoverOrdinaryAdminSecurityByExpected(eq(8),eq(3),eq(1),any())).thenReturn(1);
        when(credentials.issueDegradedAdminRecovery(8,"恢复唯一管理员")).thenReturn(new ManagedDeliveryResult(true,"QUEUED"));
        AdminAccessRecoveryService service=new AdminAccessRecoveryService(users,locks,current,gate,credentials,sessions,failureAudit,employees,redis,digester,key,false);
        AdminAccessRecoveryRequest request=new AdminAccessRecoveryRequest();request.setRecoveryKey(key);request.setReason("恢复唯一管理员");

        var result=service.recover(request);

        assertEquals(8,result.recoveredUserId());assertEquals("QUEUED",result.deliveryStatus());
        InOrder order=inOrder(locks);order.verify(locks).lockByName("AUTHORIZATION_MEMBERSHIP_GUARD");
        order.verify(locks).lockByName("ORGANIZATION_HIERARCHY");order.verify(locks).lockByName("REPORTING_GRAPH");
        order.verify(locks).lockByName("AVAILABLE_ADMIN_GUARD");
        verify(sessions).revokeAllForSecurityChange(8,1,"降级状态恢复普通管理员入口");
    }

    @Test
    void pendingInvitationRecoversInvalidSecurityFactsBeforeReissue() {
        String key="recovery-key-for-test-environment-000001";when(current.getCurrentUser()).thenReturn(recovery());
        when(locks.lockByName(anyString())).thenAnswer(invocation->invocation.getArgument(0));
        when(gate.state()).thenReturn(UserManagementAccessGate.BootstrapState.PENDING_FIRST_CHANGE);
        when(users.selectRecoverableAdminCandidatesForUpdate()).thenReturn(List.of(humanAdmin(8,AccountStatus.INVITED)));
        when(users.recoverInvitedAdminSecurityByExpected(eq(8),eq(3),eq(1),any())).thenReturn(1);
        when(credentials.issueDegradedAdminRecovery(8,"重发首个邀请")).thenReturn(new ManagedDeliveryResult(true,"QUEUED"));
        AdminAccessRecoveryService service=new AdminAccessRecoveryService(users,locks,current,gate,credentials,sessions,failureAudit,employees,redis,digester,key,false);
        AdminAccessRecoveryRequest request=new AdminAccessRecoveryRequest();request.setRecoveryKey(key);request.setReason("重发首个邀请");

        assertEquals("QUEUED",service.recover(request).deliveryStatus());
        verify(users).recoverInvitedAdminSecurityByExpected(eq(8),eq(3),eq(1),any());
        verify(users,never()).recoverOrdinaryAdminSecurityByExpected(anyInt(),anyInt(),anyInt(),any());
        verify(sessions).revokeAllForSecurityChange(8,1,"降级状态重发管理员邀请");
    }

    @Test
    void noDeliverableCandidateWritesIndependentFailureAuditBeforeAnyMutation() {
        String key="recovery-key-for-test-environment-000001";TUser recovery=recovery();
        when(current.getCurrentUser()).thenReturn(recovery);when(locks.lockByName(anyString())).thenAnswer(invocation->invocation.getArgument(0));
        when(gate.state()).thenReturn(UserManagementAccessGate.BootstrapState.DEGRADED);
        when(users.selectRecoverableAdminCandidatesForUpdate()).thenReturn(List.of());
        AdminAccessRecoveryService service=new AdminAccessRecoveryService(users,locks,current,gate,credentials,sessions,failureAudit,employees,redis,digester,key,false);
        AdminAccessRecoveryRequest request=new AdminAccessRecoveryRequest();request.setRecoveryKey(key);request.setReason("无可投递候选");

        BusinessException error=assertThrows(BusinessException.class,()->service.recover(request));

        assertEquals(CodeEnum.LAST_AVAILABLE_ADMIN_REQUIRED,error.getCodeEnum());
        verify(failureAudit).recordAuthenticated(eq(AuditActionEnum.USER_MANAGEMENT_GATE_REJECTED),eq("ADMIN_ACCESS"),contains("NO_DELIVERABLE_ADMIN_CANDIDATE"),eq(recovery));
        verifyNoInteractions(credentials);verifyNoInteractions(sessions);
    }

    @Test
    void wrongRecoveryKeyWritesIndependentRejectedAudit() {
        String key="recovery-key-for-test-environment-000001";TUser recovery=recovery();
        when(current.getCurrentUser()).thenReturn(recovery);
        AdminAccessRecoveryService service=new AdminAccessRecoveryService(users,locks,current,gate,credentials,sessions,
                failureAudit,employees,redis,digester,key,false);
        AdminAccessRecoveryRequest request=new AdminAccessRecoveryRequest();request.setRecoveryKey("wrong-key-for-test-environment-0000001");request.setReason("错误恢复");

        BusinessException error=assertThrows(BusinessException.class,()->service.recover(request));

        assertEquals(CodeEnum.ACCESS_DENIED,error.getCodeEnum());
        verify(failureAudit).recordAuthenticated(eq(AuditActionEnum.USER_RECOVERY_KEY_REJECTED),eq("ADMIN_ACCESS"),contains("KEY_MISMATCH"),eq(recovery));
        verifyNoInteractions(locks);
    }

    @Test
    void rateInfrastructureFailureFailsClosedBeforeRecoveryKeyAndGraphLocks() {
        String key="recovery-key-for-test-environment-000001";TUser recovery=recovery();
        when(current.getCurrentUser()).thenReturn(recovery);
        when(redis.incrementSlidingWindow(anyString(),eq(3600L))).thenReturn(null);
        AdminAccessRecoveryService service=new AdminAccessRecoveryService(users,locks,current,gate,credentials,sessions,
                failureAudit,employees,redis,digester,key,true);
        AdminAccessRecoveryRequest request=new AdminAccessRecoveryRequest();request.setRecoveryKey(key);request.setReason("恢复入口");

        BusinessException error=assertThrows(BusinessException.class,()->service.recover(request));

        assertEquals(CodeEnum.CREDENTIAL_RATE_LIMITED,error.getCodeEnum());
        verify(failureAudit).recordAuthenticated(eq(AuditActionEnum.USER_CREDENTIAL_RATE_LIMIT),eq("ADMIN_ACCESS"),contains("ADMIN_ACCESS_ACCOUNT"),eq(recovery));
        verifyNoInteractions(locks);
    }

    private TUser recovery(){TUser user=new TUser();user.setId(1);user.setLoginAct("admin");user.setAccountType(AccountType.SYSTEM);user.setProtectedAccount(true);return user;}
    private TUser humanAdmin(int id,AccountStatus status){TUser user=new TUser();user.setId(id);user.setLoginAct("admin"+id);user.setAccountType(AccountType.HUMAN);user.setProtectedAccount(false);user.setAccountStatus(status);user.setVersion(3);return user;}
}
