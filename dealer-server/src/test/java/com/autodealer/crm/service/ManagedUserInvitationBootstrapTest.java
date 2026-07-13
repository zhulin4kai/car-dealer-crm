package com.autodealer.crm.service;

import com.autodealer.crm.audit.AuthorizationAuditRecorder;
import com.autodealer.crm.audit.OperationAuditRecorder;
import com.autodealer.crm.config.security.CurrentUserProvider;
import com.autodealer.crm.dto.credential.CredentialDtos.ManagedDeliveryResult;
import com.autodealer.crm.dto.user.ManagedUserDtos.CreateRequest;
import com.autodealer.crm.dto.user.ManagedUserDtos.Detail;
import com.autodealer.crm.enums.AccountType;
import com.autodealer.crm.enums.OrganizationUnitType;
import com.autodealer.crm.exception.BusinessException;
import com.autodealer.crm.mapper.*;
import com.autodealer.crm.model.*;
import com.autodealer.crm.result.CodeEnum;
import com.autodealer.crm.service.impl.DirectManagerPolicy;
import com.autodealer.crm.service.impl.ManagedUserInvitationServiceImpl;
import com.autodealer.crm.service.impl.UserAuthorizationPolicy;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.mockito.InOrder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ManagedUserInvitationBootstrapTest {
    private final TUserMapper users = mock(TUserMapper.class);
    private final TLoginIdentifierMapper loginIdentifiers = mock(TLoginIdentifierMapper.class);
    private final TEmployeeMapper employees = mock(TEmployeeMapper.class);
    private final TEmployeeAssignmentMapper assignments = mock(TEmployeeAssignmentMapper.class);
    private final TEmployeeReportingMapper reporting = mock(TEmployeeReportingMapper.class);
    private final TOrganizationUnitMapper organizations = mock(TOrganizationUnitMapper.class);
    private final TPositionMapper positions = mock(TPositionMapper.class);
    private final TRoleMapper roles = mock(TRoleMapper.class);
    private final TUserRoleMapper userRoles = mock(TUserRoleMapper.class);
    private final CurrentUserProvider current = mock(CurrentUserProvider.class);
    private final UserAuthorizationPolicy policy = mock(UserAuthorizationPolicy.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final CredentialService credentials = mock(CredentialService.class);
    private final ManagedUserAccountService accountService = mock(ManagedUserAccountService.class);
    private final OperationAuditRecorder audit = mock(OperationAuditRecorder.class);
    private final AuthorizationAuditRecorder authorizationAudit = mock(AuthorizationAuditRecorder.class);
    private final TAuthorizationGraphLockMapper graphLock = mock(TAuthorizationGraphLockMapper.class);
    private final DirectManagerPolicy directManagerPolicy = mock(DirectManagerPolicy.class);
    private ManagedUserInvitationServiceImpl service;
    private TOrganizationUnit root;
    private TRole adminRole;

    @BeforeEach
    void setUp() {
        service = new ManagedUserInvitationServiceImpl(users, loginIdentifiers, employees, assignments,
                reporting, organizations, positions, roles, userRoles, current, policy, encoder,
                credentials, accountService, audit, authorizationAudit, new ObjectMapper(), graphLock,
                directManagerPolicy);
        ReflectionTestUtils.setField(service, "bootstrapGateEnabled", true);
        when(graphLock.lockByName(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
        when(users.countAdminUsers()).thenReturn(0);
        when(encoder.encode(anyString())).thenReturn("encoded");

        root = new TOrganizationUnit();
        root.setId(9);root.setCode("ROOT");root.setName("集团公司");
        root.setType(OrganizationUnitType.COMPANY);root.setEnabled(true);
        root.setMigrationPlaceholder(false);root.setVersion(4);
        when(organizations.selectByPrimaryKey(9)).thenReturn(root);
        when(organizations.selectRoots()).thenReturn(List.of(root));

        TPosition position = new TPosition();
        position.setId(2);position.setCode("GENERAL_MANAGER");position.setName("总经理");position.setEnabled(true);
        when(positions.selectByPrimaryKey(2)).thenReturn(position);

        adminRole = new TRole();
        adminRole.setId(1);adminRole.setRole("admin");adminRole.setRoleName("系统管理员");
        adminRole.setEnabled(1);adminRole.setProtectedRole(true);
        when(roles.selectByPrimaryKey(1)).thenReturn(adminRole);

        TUser recovery = new TUser();
        recovery.setId(1);recovery.setLoginAct("admin");recovery.setAccountType(AccountType.SYSTEM);
        recovery.setProtectedAccount(true);
        when(current.getCurrentUser()).thenReturn(recovery);
        when(current.getCurrentUserId()).thenReturn(1);
    }

    @Test
    void bootstrapsRootLeaderAndAdminUnderOneFixedLockOrder() {
        when(users.insert(any())).thenAnswer(invocation -> {
            ((TUser) invocation.getArgument(0)).setId(2);return 1;
        });
        when(loginIdentifiers.insert(any())).thenReturn(1);
        when(employees.insert(any())).thenAnswer(invocation -> {
            ((TEmployee) invocation.getArgument(0)).setId(10);return 1;
        });
        when(assignments.insert(any())).thenReturn(1);
        when(organizations.assignInitialRootLeader(eq(9), eq(10), eq(4), eq(1), any())).thenReturn(1);
        when(directManagerPolicy.validate(eq(10), eq(9), isNull(), any())).thenReturn(null);
        when(policy.canDelegateRole(eq(adminRole), any())).thenReturn(true);
        when(userRoles.insert(any())).thenReturn(1);
        Detail detail = new Detail();detail.setId(2);
        when(accountService.getDetail(2)).thenReturn(detail);
        when(credentials.issueInvitation(2, "首次根公司负责人初始化"))
                .thenReturn(new ManagedDeliveryResult(true, "CAPTURED"));

        var result = service.create(bootstrapRequest());

        assertEquals(2, result.user().getId());
        assertTrue(result.credentialDelivery().accepted());
        InOrder locks = inOrder(graphLock);
        locks.verify(graphLock).lockByName("LOGIN_IDENTIFIER_GUARD");
        locks.verify(graphLock).lockByName("AUTHORIZATION_MEMBERSHIP_GUARD");
        locks.verify(graphLock).lockByName("ORGANIZATION_HIERARCHY");
        locks.verify(graphLock).lockByName("REPORTING_GRAPH");
        locks.verify(graphLock).lockByName("AVAILABLE_ADMIN_GUARD");
        verify(organizations).assignInitialRootLeader(eq(9), eq(10), eq(4), eq(1), any());
        verify(reporting, never()).insert(any());
        verify(userRoles).insert(argThat(value -> value.getUserId() == 2 && value.getRoleId() == 1));
    }

    @Test
    void noAvailableAdminRequiresExplicitBootstrapCommand() {
        CreateRequest request = bootstrapRequest();
        request.setBootstrapRootLeader(false);
        request.setExpectedRootOrganizationVersion(null);
        request.setManagerEmployeeId(3);

        BusinessException exception = assertThrows(BusinessException.class, () -> service.create(request));

        assertEquals(CodeEnum.ADMIN_BOOTSTRAP_REQUIRED, exception.getCodeEnum());
        verify(users, never()).insert(any());
    }

    @Test
    void rootLeaderCasConflictUsesOrganizationVersionCodeAndStopsDelivery() {
        when(users.insert(any())).thenAnswer(invocation -> {((TUser) invocation.getArgument(0)).setId(2);return 1;});
        when(loginIdentifiers.insert(any())).thenReturn(1);
        when(employees.insert(any())).thenAnswer(invocation -> {((TEmployee) invocation.getArgument(0)).setId(10);return 1;});
        when(assignments.insert(any())).thenReturn(1);
        when(organizations.assignInitialRootLeader(eq(9), eq(10), eq(4), eq(1), any())).thenReturn(0);

        BusinessException exception = assertThrows(BusinessException.class, () -> service.create(bootstrapRequest()));

        assertEquals(CodeEnum.ORGANIZATION_VERSION_CONFLICT, exception.getCodeEnum());
        verifyNoInteractions(credentials);
    }

    private CreateRequest bootstrapRequest() {
        CreateRequest request = new CreateRequest();
        request.setLoginAct("leader01");request.setName("集团负责人");request.setEmployeeNo("E00001");
        request.setOrganizationUnitId(9);request.setPositionId(2);request.setRoleIds(List.of(1));
        request.setBootstrapRootLeader(true);request.setExpectedRootOrganizationVersion(4);
        return request;
    }
}
