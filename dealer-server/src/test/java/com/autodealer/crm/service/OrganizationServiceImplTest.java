package com.autodealer.crm.service;

import com.autodealer.crm.audit.AuthorizationAuditRecorder;
import com.autodealer.crm.config.security.CurrentUserProvider;
import com.autodealer.crm.config.security.OwnerCandidateCacheInvalidator;
import com.autodealer.crm.constant.PermissionCodes;
import com.autodealer.crm.dto.organization.CreateOrganizationUnitRequest;
import com.autodealer.crm.dto.organization.UpdateEmployeeOrganizationRequest;
import com.autodealer.crm.dto.organization.UpdateOrganizationUnitRequest;
import com.autodealer.crm.dto.organization.ActingReportingInput;
import com.autodealer.crm.dto.organization.ReplaceActingReportingsRequest;
import com.autodealer.crm.enums.AccountType;
import com.autodealer.crm.enums.EmployeeStatus;
import com.autodealer.crm.enums.OrganizationUnitType;
import com.autodealer.crm.exception.BusinessException;
import com.autodealer.crm.mapper.*;
import com.autodealer.crm.model.TEmployee;
import com.autodealer.crm.model.TOrganizationUnit;
import com.autodealer.crm.model.TEmployeeAssignment;
import com.autodealer.crm.model.TUser;
import com.autodealer.crm.model.TEmployeeReporting;
import com.autodealer.crm.enums.ReportingStatus;
import com.autodealer.crm.enums.ReportingType;
import com.autodealer.crm.result.CodeEnum;
import com.autodealer.crm.service.impl.OrganizationServiceImpl;
import com.autodealer.crm.service.impl.DirectManagerPolicy;
import com.autodealer.crm.service.impl.UserAuthorizationPolicy;
import com.autodealer.crm.service.impl.UserSecurityMutationCoordinator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrganizationServiceImplTest {
    @Mock private TOrganizationUnitMapper organizationUnitMapper;
    @Mock private TPositionMapper positionMapper;
    @Mock private TEmployeeMapper employeeMapper;
    @Mock private TEmployeeAssignmentMapper assignmentMapper;
    @Mock private TEmployeeReportingMapper reportingMapper;
    @Mock private TAuthorizationHistoryMapper historyMapper;
    @Mock private TAuthorizationGraphLockMapper graphLockMapper;
    @Mock private TUserMapper userMapper;
    @Mock private AuthorizationAuditRecorder authorizationAuditRecorder;
    @Mock private CurrentUserProvider currentUserProvider;
    @Mock private DirectManagerPolicy directManagerPolicy;
    @Mock private UserAuthorizationPolicy authorizationPolicy;
    @Mock private UserSecurityMutationCoordinator securityMutations;

    private OrganizationServiceImpl service;

    @BeforeEach
    void setUp() {
        lenient().when(graphLockMapper.lockByName(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
        service = new OrganizationServiceImpl(organizationUnitMapper, positionMapper, employeeMapper,
                assignmentMapper, reportingMapper, historyMapper, graphLockMapper, userMapper,
                authorizationAuditRecorder, currentUserProvider, new ObjectMapper(),
                directManagerPolicy, authorizationPolicy, securityMutations);
    }

    @Test
    void employeeCannotUpdateOwnOrganizationMembership() {
        TEmployee self = employee(10, 20);
        TUser account = new TUser();
        account.setId(20);
        account.setAccountType(AccountType.HUMAN);
        account.setProtectedAccount(false);
        when(employeeMapper.selectByPrimaryKey(10)).thenReturn(self);
        when(userMapper.selectByPrimaryKey(20)).thenReturn(account);
        when(currentUserProvider.getCurrentUserId()).thenReturn(20);
        when(employeeMapper.selectByUserId(20)).thenReturn(self);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.updateEmployeeOrganizationMembership(10, new UpdateEmployeeOrganizationRequest()));

        assertEquals(CodeEnum.SELF_MANAGEMENT_FORBIDDEN, exception.getCodeEnum());
        verifyNoInteractions(assignmentMapper, reportingMapper, authorizationAuditRecorder);
    }

    @Test
    void selfReadIsNotExposedThroughManagerOrganizationEndpoint() {
        TEmployee self = employee(10, 20);
        TUser account = new TUser();
        account.setId(20);
        account.setAccountType(AccountType.HUMAN);
        account.setProtectedAccount(false);
        when(employeeMapper.selectByPrimaryKey(10)).thenReturn(self);
        when(currentUserProvider.getCurrentUserId()).thenReturn(20);
        when(employeeMapper.selectByUserId(20)).thenReturn(self);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.getEmployeeOrganizationMembership(10));

        assertEquals(CodeEnum.ACCESS_DENIED, exception.getCodeEnum());
    }

    @Test
    void organizationLeaderAttributeDoesNotCreateReportingRelationship() {
        TEmployee leader = employee(30, 40);
        when(organizationUnitMapper.selectByCode("STORE_A")).thenReturn(null);
        when(organizationUnitMapper.selectByPrimaryKey(1)).thenReturn(company());
        when(employeeMapper.selectEligibleManagerCandidates(anyInt(), any(LocalDateTime.class), any()))
                .thenReturn(List.of(leader));
        when(organizationUnitMapper.insert(any(TOrganizationUnit.class))).thenAnswer(invocation -> {
            invocation.<TOrganizationUnit>getArgument(0).setId(2);
            return 1;
        });
        when(organizationUnitMapper.countEffectiveEmployees(eq(2), any(LocalDateTime.class))).thenReturn(0);
        when(employeeMapper.selectByPrimaryKey(30)).thenReturn(leader);
        TEmployeeAssignment leaderAssignment = new TEmployeeAssignment();
        leaderAssignment.setOrganizationUnitId(1);
        when(assignmentMapper.selectCurrentPrimaryByEmployeeId(eq(30), any(LocalDateTime.class)))
                .thenReturn(leaderAssignment);
        when(currentUserProvider.getCurrentUserId()).thenReturn(1);
        when(authorizationPolicy.isGlobalOperator()).thenReturn(true);

        CreateOrganizationUnitRequest request = new CreateOrganizationUnitRequest();
        request.setCode("STORE_A");
        request.setName("A门店");
        request.setType(OrganizationUnitType.STORE);
        request.setParentId(1);
        request.setLeaderEmployeeId(30);
        request.setOrderNo(1);

        assertEquals(30, service.createOrganizationUnit(request).getLeaderEmployeeId());
        verifyNoInteractions(reportingMapper);
    }

    @Test
    void qualifiedHumanAdminHasGlobalScopeButStillCannotManageSelf() {
        TEmployee adminEmployee = employee(10, 20);
        TEmployee subordinate = employee(11, 21);
        TUser humanAdmin = new TUser();
        humanAdmin.setId(20);
        humanAdmin.setAccountType(AccountType.HUMAN);
        humanAdmin.setProtectedAccount(false);
        when(currentUserProvider.getCurrentUserId()).thenReturn(20);
        when(authorizationPolicy.isGlobalOperator()).thenReturn(true);
        when(employeeMapper.selectByUserId(20)).thenReturn(adminEmployee);
        when(employeeMapper.selectByPrimaryKey(11)).thenReturn(subordinate);
        when(assignmentMapper.selectEffectiveByEmployeeId(eq(11), any(LocalDateTime.class))).thenReturn(List.of());

        assertEquals(11, service.getEmployeeOrganizationMembership(11).getEmployee().getId());

        when(employeeMapper.selectByPrimaryKey(10)).thenReturn(adminEmployee);
        when(userMapper.selectByPrimaryKey(20)).thenReturn(humanAdmin);
        BusinessException selfException = assertThrows(BusinessException.class,
                () -> service.updateEmployeeOrganizationMembership(10, new UpdateEmployeeOrganizationRequest()));
        assertEquals(CodeEnum.SELF_MANAGEMENT_FORBIDDEN, selfException.getCodeEnum());
    }

    @Test
    void roleNameAdminWithoutQualifiedFactsCannotCreateRootOrganization() {
        lenient().when(currentUserProvider.isAdmin()).thenReturn(true);
        when(authorizationPolicy.isGlobalOperator()).thenReturn(false);
        CreateOrganizationUnitRequest request = rootOrganizationRequest("ROOT_BY_ROLE");

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.createOrganizationUnit(request));

        assertEquals(CodeEnum.ACCESS_DENIED, exception.getCodeEnum());
        verify(currentUserProvider, never()).isAdmin();
        verify(organizationUnitMapper, never()).insert(any());
    }

    @Test
    void fixedRecoveryAccountCannotCreateRootOrganizationThroughGlobalShortcut() {
        lenient().when(currentUserProvider.getCurrentUser()).thenReturn(protectedRecoveryAccount());
        when(authorizationPolicy.isGlobalOperator()).thenReturn(false);
        CreateOrganizationUnitRequest request = rootOrganizationRequest("ROOT_BY_RECOVERY");

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.createOrganizationUnit(request));

        assertEquals(CodeEnum.ACCESS_DENIED, exception.getCodeEnum());
        verify(currentUserProvider, never()).getCurrentUser();
        verify(organizationUnitMapper, never()).insert(any());
    }

    @Test
    void organizationEditorCannotUpdateOrganizationOutsideOwnScope() {
        TUser managerAccount = new TUser();
        managerAccount.setId(20);
        managerAccount.setAccountType(AccountType.HUMAN);
        managerAccount.setProtectedAccount(false);
        TEmployee manager = employee(10, 20);
        TOrganizationUnit foreignCompany = company();
        foreignCompany.setId(99);
        foreignCompany.setCode("FOREIGN");
        TEmployeeAssignment scope = new TEmployeeAssignment();
        scope.setOrganizationUnitId(1);
        when(organizationUnitMapper.selectByPrimaryKey(99)).thenReturn(foreignCompany);
        when(currentUserProvider.getCurrentUserId()).thenReturn(20);
        when(employeeMapper.selectByUserId(20)).thenReturn(manager);
        when(assignmentMapper.selectCurrentPrimaryByEmployeeId(eq(10), any(LocalDateTime.class))).thenReturn(scope);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.updateOrganizationUnit(99, new com.autodealer.crm.dto.organization.UpdateOrganizationUnitRequest()));

        assertEquals(CodeEnum.ACCESS_DENIED, exception.getCodeEnum());
    }

    @Test
    void nonGlobalManagerCannotPromoteChildToRootCompanyOrForgeCrossScopeLeader() {
        TUser managerAccount = new TUser();
        managerAccount.setId(20);
        managerAccount.setAccountType(AccountType.HUMAN);
        managerAccount.setProtectedAccount(false);
        TEmployee manager = employee(10, 20);
        TEmployee foreignLeader = employee(30, 40);
        TOrganizationUnit child = company();
        child.setId(2);
        child.setCode("STORE_CHILD");
        child.setType(OrganizationUnitType.STORE);
        child.setParentId(1);
        TOrganizationUnit foreign = company();
        foreign.setId(99);
        foreign.setCode("FOREIGN");
        TEmployeeAssignment managerScope = new TEmployeeAssignment();
        managerScope.setOrganizationUnitId(1);
        TEmployeeAssignment foreignScope = new TEmployeeAssignment();
        foreignScope.setOrganizationUnitId(99);
        when(currentUserProvider.getCurrentUserId()).thenReturn(20);
        when(employeeMapper.selectByUserId(20)).thenReturn(manager);
        when(assignmentMapper.selectCurrentPrimaryByEmployeeId(eq(10), any(LocalDateTime.class)))
                .thenReturn(managerScope);
        when(organizationUnitMapper.selectByPrimaryKey(2)).thenReturn(child);
        when(organizationUnitMapper.selectByPrimaryKey(1)).thenReturn(company());
        when(organizationUnitMapper.selectByPrimaryKey(99)).thenReturn(foreign);

        UpdateOrganizationUnitRequest promote = new UpdateOrganizationUnitRequest();
        promote.setExpectedVersion(0);
        promote.setName("伪造根公司");
        promote.setType(OrganizationUnitType.COMPANY);
        promote.setOrderNo(0);
        BusinessException promoteException = assertThrows(BusinessException.class,
                () -> service.updateOrganizationUnit(2, promote));
        assertEquals(CodeEnum.ACCESS_DENIED, promoteException.getCodeEnum());

        when(organizationUnitMapper.selectByCode("STORE_FORGED")).thenReturn(null);
        when(employeeMapper.selectEligibleManagerCandidates(anyInt(), any(LocalDateTime.class), any()))
                .thenReturn(List.of(foreignLeader));
        when(assignmentMapper.selectCurrentPrimaryByEmployeeId(eq(30), any(LocalDateTime.class)))
                .thenReturn(foreignScope);
        CreateOrganizationUnitRequest create = new CreateOrganizationUnitRequest();
        create.setCode("STORE_FORGED");
        create.setName("伪造负责人门店");
        create.setType(OrganizationUnitType.STORE);
        create.setParentId(1);
        create.setLeaderEmployeeId(30);
        create.setOrderNo(1);
        BusinessException leaderException = assertThrows(BusinessException.class,
                () -> service.createOrganizationUnit(create));
        assertEquals(CodeEnum.ACCESS_DENIED, leaderException.getCodeEnum());
    }

    @Test
    void replacesMultipleActingRelationsWithoutClosingDirectManager() {
        TEmployee target = employee(10, 20);
        TUser targetAccount = new TUser();targetAccount.setId(20);targetAccount.setAccountType(AccountType.HUMAN);targetAccount.setProtectedAccount(false);
        TEmployeeAssignment primary = new TEmployeeAssignment();primary.setOrganizationUnitId(1);
        TEmployeeReporting direct = reporting(50, ReportingType.DIRECT, null);
        when(employeeMapper.selectByPrimaryKey(10)).thenReturn(target);
        when(userMapper.selectByPrimaryKey(20)).thenReturn(targetAccount);
        when(currentUserProvider.getCurrentUserId()).thenReturn(1);
        when(authorizationPolicy.isGlobalOperator()).thenReturn(true);
        when(currentUserProvider.hasAuthority(anyString())).thenReturn(true);
        when(assignmentMapper.selectCurrentPrimaryByEmployeeId(eq(10), any())).thenReturn(primary);
        when(reportingMapper.selectCurrentDirectBySubordinateId(eq(10), any())).thenReturn(direct);
        when(reportingMapper.selectCurrentAndFutureActingBySubordinateId(eq(10), any()))
                .thenReturn(List.of(), List.of(reporting(60, ReportingType.ACTING, LocalDateTime.now().plusDays(2)),
                        reporting(61, ReportingType.ACTING, LocalDateTime.now().plusDays(3))),
                        List.of(reporting(60, ReportingType.ACTING, LocalDateTime.now().plusDays(2)),
                                reporting(61, ReportingType.ACTING, LocalDateTime.now().plusDays(3))));
        when(employeeMapper.incrementVersionByExpected(eq(10), eq(0), any(), eq(1))).thenReturn(1);
        when(reportingMapper.insert(any())).thenAnswer(invocation -> { invocation.<TEmployeeReporting>getArgument(0).setId(100); return 1; });
        when(userMapper.incrementAuthVersion(20)).thenReturn(1);

        ReplaceActingReportingsRequest request = actingRequest(0, 60, 61);
        var response = service.replaceActingReportings(10, request);

        assertEquals(2, response.getRelations().size());
        ArgumentCaptor<TEmployeeReporting> inserted = ArgumentCaptor.forClass(TEmployeeReporting.class);
        verify(reportingMapper, times(2)).insert(inserted.capture());
        assertTrue(inserted.getAllValues().stream().allMatch(value -> value.getRelationType() == ReportingType.ACTING
                && value.getEffectiveTo() != null && value.getActiveDirectMarker() == null));
        verify(reportingMapper).expireElapsedActingMarkers(eq(10), any(), eq(1));
        verify(reportingMapper, never()).expireElapsedMarkers(anyInt(), any(), anyInt());
        verify(reportingMapper, never()).endByIdAndVersion(eq(direct.getId()), anyInt(), any(), any(), anyInt());
        verify(securityMutations).accessChanged(20, "组织任职或汇报关系变化");
    }

    @Test
    void emptyActingCollectionClosesOnlyActingFacts() {
        TEmployee target = employee(10, 20);
        TUser targetAccount = new TUser();targetAccount.setId(20);targetAccount.setAccountType(AccountType.HUMAN);targetAccount.setProtectedAccount(false);
        TEmployeeAssignment primary = new TEmployeeAssignment();primary.setOrganizationUnitId(1);
        TEmployeeReporting direct = reporting(50, ReportingType.DIRECT, null);
        TEmployeeReporting acting = reporting(60, ReportingType.ACTING, LocalDateTime.now().plusDays(2).withNano(0));
        when(employeeMapper.selectByPrimaryKey(10)).thenReturn(target);
        when(userMapper.selectByPrimaryKey(20)).thenReturn(targetAccount);
        when(currentUserProvider.getCurrentUserId()).thenReturn(1);
        when(authorizationPolicy.isGlobalOperator()).thenReturn(true);
        when(currentUserProvider.hasAuthority(PermissionCodes.EMPLOYEE_REPORTING)).thenReturn(true);
        when(assignmentMapper.selectCurrentPrimaryByEmployeeId(eq(10), any())).thenReturn(primary);
        when(reportingMapper.selectCurrentDirectBySubordinateId(eq(10), any())).thenReturn(direct);
        when(reportingMapper.selectCurrentAndFutureActingBySubordinateId(eq(10), any()))
                .thenReturn(List.of(acting), List.of(), List.of());
        when(employeeMapper.incrementVersionByExpected(eq(10), eq(0), any(), eq(1))).thenReturn(1);
        when(reportingMapper.endByIdAndVersion(eq(acting.getId()), eq(0), any(), any(), eq(1))).thenReturn(1);
        when(userMapper.incrementAuthVersion(20)).thenReturn(1);
        ReplaceActingReportingsRequest request = new ReplaceActingReportingsRequest();
        request.setExpectedEmployeeVersion(0);request.setRelations(List.of());request.setReason("代理结束");

        service.replaceActingReportings(10, request);

        verify(reportingMapper).endByIdAndVersion(eq(acting.getId()), eq(0), any(), any(), eq(1));
        verify(reportingMapper, never()).endByIdAndVersion(eq(direct.getId()), anyInt(), any(), any(), anyInt());
        verify(reportingMapper, never()).insert(any());
    }

    @Test
    void actingWriteRequiresReportingCapabilityInsideService() {
        TEmployee target = employee(10, 20);
        TUser targetAccount = new TUser();targetAccount.setId(20);targetAccount.setAccountType(AccountType.HUMAN);targetAccount.setProtectedAccount(false);
        when(employeeMapper.selectByPrimaryKey(10)).thenReturn(target);
        when(userMapper.selectByPrimaryKey(20)).thenReturn(targetAccount);
        when(currentUserProvider.getCurrentUserId()).thenReturn(1);
        when(authorizationPolicy.isGlobalOperator()).thenReturn(true);
        when(currentUserProvider.hasAuthority(PermissionCodes.EMPLOYEE_REPORTING)).thenReturn(false);
        ReplaceActingReportingsRequest request = new ReplaceActingReportingsRequest();
        request.setExpectedEmployeeVersion(0);request.setRelations(List.of());request.setReason("无权限");

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.replaceActingReportings(10, request));

        assertEquals(CodeEnum.ACCESS_DENIED, exception.getCodeEnum());
        verify(reportingMapper, never()).expireElapsedActingMarkers(anyInt(), any(), anyInt());
        verify(employeeMapper, never()).incrementVersionByExpected(anyInt(), anyInt(), any(), anyInt());
    }

    @Test
    void unchangedActingCollectionDoesNotIncreaseEmployeeVersionOrRewriteFacts() {
        TEmployee target = employee(10, 20);
        TUser account = new TUser();account.setId(20);account.setAccountType(AccountType.HUMAN);account.setProtectedAccount(false);
        TEmployeeAssignment primary = new TEmployeeAssignment();primary.setOrganizationUnitId(1);
        LocalDateTime end = LocalDateTime.now().plusDays(2).withNano(0);
        TEmployeeReporting existing = reporting(60, ReportingType.ACTING, end);
        when(employeeMapper.selectByPrimaryKey(10)).thenReturn(target);
        when(userMapper.selectByPrimaryKey(20)).thenReturn(account);
        when(currentUserProvider.getCurrentUserId()).thenReturn(1);
        when(authorizationPolicy.isGlobalOperator()).thenReturn(true);
        when(currentUserProvider.hasAuthority(anyString())).thenReturn(true);
        when(assignmentMapper.selectCurrentPrimaryByEmployeeId(eq(10), any())).thenReturn(primary);
        when(reportingMapper.selectCurrentAndFutureActingBySubordinateId(eq(10), any())).thenReturn(List.of(existing));

        ReplaceActingReportingsRequest request = new ReplaceActingReportingsRequest();request.setExpectedEmployeeVersion(0);
        ActingReportingInput input = new ActingReportingInput();input.setManagerEmployeeId(60);input.setEffectiveTo(end.atZone(java.time.ZoneId.systemDefault()).toOffsetDateTime());
        request.setRelations(List.of(input));request.setReason("无变化");
        service.replaceActingReportings(10, request);

        verify(employeeMapper, never()).incrementVersionByExpected(anyInt(), anyInt(), any(), anyInt());
        verify(reportingMapper, never()).insert(any());
        verify(reportingMapper, never()).endByIdAndVersion(anyInt(), anyInt(), any(), any(), anyInt());
    }

    private TEmployee employee(int id, int userId) {
        TEmployee employee = new TEmployee();
        employee.setId(id);
        employee.setUserId(userId);
        employee.setName("员工" + id);
        employee.setEmploymentStatus(EmployeeStatus.ACTIVE);
        employee.setVersion(0);
        return employee;
    }

    private TOrganizationUnit company() {
        TOrganizationUnit unit = new TOrganizationUnit();
        unit.setId(1);
        unit.setCode("COMPANY");
        unit.setName("公司");
        unit.setType(OrganizationUnitType.COMPANY);
        unit.setEnabled(true);
        unit.setMigrationPlaceholder(false);
        unit.setOrderNo(0);
        unit.setVersion(0);
        return unit;
    }

    private CreateOrganizationUnitRequest rootOrganizationRequest(String code) {
        CreateOrganizationUnitRequest request = new CreateOrganizationUnitRequest();
        request.setCode(code);
        request.setName("根公司");
        request.setType(OrganizationUnitType.COMPANY);
        request.setOrderNo(0);
        return request;
    }

    private TUser protectedRecoveryAccount() {
        TUser user = new TUser();
        user.setId(1);
        user.setLoginAct("admin");
        user.setAccountType(AccountType.SYSTEM);
        user.setProtectedAccount(true);
        return user;
    }

    private ReplaceActingReportingsRequest actingRequest(int version, int... managers) {
        ReplaceActingReportingsRequest request = new ReplaceActingReportingsRequest();
        request.setExpectedEmployeeVersion(version);request.setReason("代理安排");
        List<ActingReportingInput> inputs = new java.util.ArrayList<>();
        for (int index = 0; index < managers.length; index++) {
            ActingReportingInput input = new ActingReportingInput();input.setManagerEmployeeId(managers[index]);
            input.setEffectiveTo(OffsetDateTime.now().plusDays(index + 2));inputs.add(input);
        }
        request.setRelations(inputs);return request;
    }

    private TEmployeeReporting reporting(int managerId, ReportingType type, LocalDateTime effectiveTo) {
        TEmployeeReporting value = new TEmployeeReporting();value.setId(managerId + 1000);
        value.setSubordinateEmployeeId(10);value.setManagerEmployeeId(managerId);value.setRelationType(type);
        value.setStatus(ReportingStatus.ACTIVE);value.setEffectiveFrom(LocalDateTime.now().minusDays(1));
        value.setEffectiveTo(effectiveTo);value.setVersion(0);return value;
    }
}
