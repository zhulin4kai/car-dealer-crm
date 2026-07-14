package com.autodealer.crm.modules.identity.application.internal;

import com.autodealer.crm.modules.audit.application.api.AuditRequestIdProvider;
import com.autodealer.crm.modules.audit.application.api.OperationAuditRecorder;
import com.autodealer.crm.modules.identity.application.api.AuthorizationAuditRecorder;
import com.autodealer.crm.modules.identity.application.api.CredentialService;
import com.autodealer.crm.modules.identity.application.api.DataScopeResolver;
import com.autodealer.crm.modules.identity.application.api.enums.AccountStatus;
import com.autodealer.crm.modules.identity.application.api.enums.AccountType;
import com.autodealer.crm.modules.identity.application.api.enums.EmployeeStatus;
import com.autodealer.crm.modules.identity.application.api.enums.OrganizationUnitType;
import com.autodealer.crm.modules.identity.application.api.model.TUser;
import com.autodealer.crm.modules.identity.persistence.mapper.TAuthorizationGraphLockMapper;
import com.autodealer.crm.modules.identity.persistence.mapper.TEmployeeAssignmentMapper;
import com.autodealer.crm.modules.identity.persistence.mapper.TEmployeeMapper;
import com.autodealer.crm.modules.identity.persistence.mapper.TEmployeeReportingMapper;
import com.autodealer.crm.modules.identity.persistence.mapper.TOrganizationUnitMapper;
import com.autodealer.crm.modules.identity.persistence.mapper.TPermissionMapper;
import com.autodealer.crm.modules.identity.persistence.mapper.TPositionMapper;
import com.autodealer.crm.modules.identity.persistence.mapper.TRoleMapper;
import com.autodealer.crm.modules.identity.persistence.mapper.TUserLifecycleMapper;
import com.autodealer.crm.modules.identity.persistence.mapper.TUserMapper;
import com.autodealer.crm.modules.identity.persistence.mapper.TUserPermissionMapper;
import com.autodealer.crm.modules.identity.persistence.mapper.TUserRoleMapper;
import com.autodealer.crm.modules.identity.persistence.model.TEmployee;
import com.autodealer.crm.modules.identity.persistence.model.TOrganizationUnit;
import com.autodealer.crm.modules.identity.persistence.model.TPosition;
import com.autodealer.crm.modules.identity.persistence.model.TRole;
import com.autodealer.crm.modules.sales.customer.persistence.mapper.TCustomerOwnerHistoryMapper;
import com.autodealer.crm.modules.sales.lead.persistence.mapper.TClueOwnerHistoryMapper;
import com.autodealer.crm.modules.identity.application.api.*;

import com.autodealer.crm.modules.audit.application.api.*;
import com.autodealer.crm.modules.identity.application.api.security.CurrentUserProvider;
import com.autodealer.crm.modules.identity.application.api.dto.credential.CredentialDtos.ManagedDeliveryResult;
import com.autodealer.crm.modules.identity.application.api.dto.user.UserLifecycleDtos.*;
import com.autodealer.crm.modules.identity.application.api.enums.*;
import com.autodealer.crm.shared.error.BusinessException;
import com.autodealer.crm.modules.identity.persistence.mapper.*;
import com.autodealer.crm.modules.identity.persistence.model.*;
import com.autodealer.crm.modules.identity.application.api.model.*;
import com.autodealer.crm.shared.error.CodeEnum;
import com.autodealer.crm.modules.identity.application.internal.UserAuthorizationPolicy;
import com.autodealer.crm.modules.identity.application.internal.UserLifecycleServiceImpl;
import com.autodealer.crm.modules.identity.application.internal.UserSecurityMutationCoordinator;
import com.autodealer.crm.modules.identity.application.internal.DirectManagerPolicy;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserLifecycleServiceImplTest {
    @Mock TUserLifecycleMapper lifecycle; @Mock TUserMapper users; @Mock TEmployeeMapper employees;
    @Mock TEmployeeAssignmentMapper assignments; @Mock TEmployeeReportingMapper reporting;
    @Mock TOrganizationUnitMapper organizations; @Mock TPositionMapper positions;
    @Mock TUserRoleMapper userRoles; @Mock TUserPermissionMapper userPermissions;
    @Mock TRoleMapper roles; @Mock TPermissionMapper permissions;
    @Mock TClueOwnerHistoryMapper clueHistory; @Mock TCustomerOwnerHistoryMapper customerHistory;
    @Mock TAuthorizationGraphLockMapper graphLocks; @Mock UserAuthorizationPolicy policy;
    @Mock CurrentUserProvider current; @Mock DataScopeResolver dataScopes;
    @Mock CredentialService credentials;
    @Mock OperationAuditRecorder operationAudit; @Mock AuditRequestIdProvider requestIds;
    @Mock AuthorizationAuditRecorder authorizationAudit;
    @Mock DirectManagerPolicy directManagerPolicy;
    @Mock UserSecurityMutationCoordinator securityMutations;
    UserLifecycleServiceImpl service;
    final Clock clock=Clock.fixed(Instant.parse("2026-07-12T10:00:00Z"),ZoneId.of("Asia/Shanghai"));

    @BeforeEach void setUp(){
        ObjectMapper json=new ObjectMapper().registerModule(new JavaTimeModule());
        service=new UserLifecycleServiceImpl(lifecycle,users,employees,assignments,reporting,organizations,positions,
                userRoles,userPermissions,roles,permissions,clueHistory,customerHistory,graphLocks,policy,current,
                dataScopes,credentials,operationAudit,requestIds,authorizationAudit,json,clock,
                directManagerPolicy,securityMutations);
        lenient().when(current.getCurrentUserId()).thenReturn(1);
        lenient().when(current.hasAuthority(anyString())).thenReturn(true);
        lenient().when(policy.isGlobalOperator()).thenReturn(true);
        lenient().when(graphLocks.lockByName(anyString())).thenAnswer(invocation->invocation.getArgument(0));
        lenient().when(lifecycle.insertSnapshot(any())).thenAnswer(invocation->{SnapshotFact fact=invocation.getArgument(0);fact.setId(99L);return 1;});
    }

    @Test void precheckPersistsOpaqueSnapshotAndBlocksEnabledOrganizationLeader(){
        TUser target=user(2);TEmployee employee=employee(10,2,EmployeeStatus.ACTIVE,3);
        when(users.selectByPrimaryKey(2)).thenReturn(target);when(employees.selectByUserId(2)).thenReturn(employee);
        when(lifecycle.countEnabledLedOrganizations(10)).thenReturn(1);
        DeparturePrecheckRequest request=new DeparturePrecheckRequest();request.setEmployeeVersion(3);request.setReason("离职交接");

        DeparturePrecheck result=service.precheckDeparture(2,request);

        assertNotNull(result.getSnapshotToken());assertFalse(result.getSnapshotToken().contains("离职交接"));
        assertEquals(8,result.getResponsibilities().size());assertFalse(result.isReadyToComplete());
        assertTrue(result.getBlockingReasons().stream().anyMatch(value->value.contains("组织的负责人")));
        assertTrue(result.getAllowedActions().contains("DEPARTURE_START"),"阻断责任不应阻止先进入待交接");
        verify(lifecycle).insertSnapshot(argThat(value->value.getUserId()==2&&value.getEmployeeId()==10
                && value.getEmployeeVersion()==3&&value.getReasonDigest().length()==64&&value.getFactDigest().length()==64));
    }

    @Test void startDepartureRejectsLastAvailableOrdinaryAdministratorBeforeHandover(){
        TUser target=user(2);TEmployee employee=employee(10,2,EmployeeStatus.ACTIVE,3);
        when(users.selectByPrimaryKey(2)).thenReturn(target);when(employees.selectByUserId(2)).thenReturn(employee);
        AtomicReference<SnapshotFact> snapshot=new AtomicReference<>();
        doAnswer(invocation->{SnapshotFact fact=invocation.getArgument(0);fact.setId(99L);snapshot.set(fact);return 1;})
                .when(lifecycle).insertSnapshot(any());
        DeparturePrecheckRequest precheckRequest=new DeparturePrecheckRequest();precheckRequest.setEmployeeVersion(3);precheckRequest.setReason("管理员离职");
        DeparturePrecheck precheck=service.precheckDeparture(2,precheckRequest);
        when(lifecycle.lockUserById(2)).thenReturn(target);when(lifecycle.lockEmployeeByUserId(2)).thenReturn(employee);
        when(lifecycle.lockSnapshotByDigest(anyString())).thenAnswer(invocation->snapshot.get());
        when(lifecycle.consumeSnapshot(eq(99L),eq(0),eq(now()))).thenReturn(1);
        TRole admin=new TRole();admin.setRole("admin");when(users.selectRolesByUserId(2)).thenReturn(List.of(admin));
        when(users.countAvailableAdminUsersExcluding(2)).thenReturn(0);
        StartDepartureRequest request=new StartDepartureRequest();request.setEmployeeVersion(3);request.setReason("管理员离职");request.setSnapshotToken(precheck.getSnapshotToken());

        BusinessException exception=assertThrows(BusinessException.class,()->service.startDeparture(2,request));

        assertEquals(CodeEnum.ACCESS_DENIED,exception.getCodeEnum());
        verify(lifecycle,never()).transitionEmployee(anyInt(),anyInt(),eq("ACTIVE"),eq("HANDOVER"),any(),anyInt(),anyBoolean(),anyBoolean());
    }

    @Test void transferRejectsMissingManagerUnlessTargetIsSelectedRootCompanyLeader(){
        TUser target=user(2);TEmployee employee=employee(10,2,EmployeeStatus.ACTIVE,3);
        when(graphLocks.lockByName(anyString())).thenAnswer(invocation->invocation.getArgument(0));
        when(lifecycle.lockUserById(2)).thenReturn(target);when(lifecycle.lockEmployeeByUserId(2)).thenReturn(employee);
        TOrganizationUnit company=new TOrganizationUnit();company.setId(7);company.setCode("ROOT");company.setName("根公司");
        company.setType(OrganizationUnitType.COMPANY);company.setEnabled(true);company.setMigrationPlaceholder(false);company.setParentId(null);company.setLeaderEmployeeId(999);
        TPosition position=new TPosition();position.setId(8);position.setCode("MANAGER");position.setName("经理");position.setEnabled(true);position.setBuiltIn(false);
        when(organizations.selectByPrimaryKey(7)).thenReturn(company);when(positions.selectByPrimaryKey(8)).thenReturn(position);
        doThrow(new BusinessException(CodeEnum.INVALID_MANAGER)).when(directManagerPolicy)
                .validate(10,7,null,now());
        AssignmentCommand request=new AssignmentCommand();request.setEmployeeVersion(3);request.setOrganizationUnitId(7);request.setPositionId(8);
        request.setEffectiveFrom(OffsetDateTime.ofInstant(clock.instant(),clock.getZone()));request.setReason("跨店调岗");

        BusinessException exception=assertThrows(BusinessException.class,()->service.transfer(2,request));

        assertEquals(CodeEnum.INVALID_MANAGER,exception.getCodeEnum());
        verify(assignments,never()).insert(any());verify(reporting,never()).insert(any());
    }

    @Test void transferRejectsCycleIntroducedByFutureDirectReportingFact(){
        TUser target=user(2);TEmployee employee=employee(10,2,EmployeeStatus.ACTIVE,3);
        when(lifecycle.lockUserById(2)).thenReturn(target);when(lifecycle.lockEmployeeByUserId(2)).thenReturn(employee);
        TOrganizationUnit organization=new TOrganizationUnit();organization.setId(7);organization.setCode("STORE");organization.setName("门店");organization.setType(OrganizationUnitType.STORE);organization.setEnabled(true);organization.setMigrationPlaceholder(false);organization.setParentId(1);
        TPosition position=new TPosition();position.setId(8);position.setCode("SALES");position.setName("销售");position.setEnabled(true);position.setBuiltIn(false);
        when(organizations.selectByPrimaryKey(7)).thenReturn(organization);when(positions.selectByPrimaryKey(8)).thenReturn(position);
        doThrow(new BusinessException(CodeEnum.INVALID_MANAGER)).when(directManagerPolicy)
                .validate(10,7,20,now());
        AssignmentCommand request=new AssignmentCommand();request.setEmployeeVersion(3);request.setOrganizationUnitId(7);request.setPositionId(8);request.setManagerEmployeeId(20);
        request.setEffectiveFrom(OffsetDateTime.ofInstant(clock.instant(),clock.getZone()));request.setReason("未来汇报环");

        BusinessException exception=assertThrows(BusinessException.class,()->service.transfer(2,request));

        assertEquals(CodeEnum.INVALID_MANAGER,exception.getCodeEnum());verify(assignments,never()).insert(any());verify(reporting,never()).insert(any());
    }

    @Test void rehireInviteReplacesExpiredCredentialButStillRequiresIndependentAccountUnlock(){
        TUser target=rehireUser(2);target.setCredentialsNoExpired(0);target.setPasswordExpiresAt(now().minusDays(1));
        TEmployee employee=employee(10,2,EmployeeStatus.LEFT,3);stubRehire(target,employee);
        when(credentials.issueInvitation(2,"返聘重新邀请")).thenReturn(new ManagedDeliveryResult(true,"CAPTURED"));

        RehireResult result=service.rehire(2,rehireRequest(AccountActivationMode.INVITE));

        assertEquals("CAPTURED",result.getCredentialDeliveryStatus());assertEquals(0,result.getRestoredLegacyAuthorizationCount());
        verify(credentials).issueInvitation(2,"返聘重新邀请");
        verify(users).updateAccountStatusByExpected(2,0,"INVITED",false,1);
        verify(securityMutations).ownerEligibilityChanged();
    }

    @Test void rehireRecoverRejectsExpiredExistingCredentialBeforeCreatingAssignment(){
        TUser target=rehireUser(2);target.setPasswordExpiresAt(now().minusSeconds(1));
        TEmployee employee=employee(10,2,EmployeeStatus.LEFT,3);
        when(lifecycle.lockUserById(2)).thenReturn(target);when(lifecycle.lockEmployeeByUserId(2)).thenReturn(employee);

        BusinessException exception=assertThrows(BusinessException.class,()->service.rehire(2,rehireRequest(AccountActivationMode.RECOVER)));

        assertEquals(CodeEnum.USER_LIFECYCLE_CONFLICT,exception.getCodeEnum());
        verify(assignments,never()).insert(any());verify(users,never()).updateAccountStatusByExpected(anyInt(),anyInt(),anyString(),anyBoolean(),anyInt());
    }

    @Test void rehireRejectsExpiredAccountTimeBeforeCreatingAssignment(){
        TUser target=rehireUser(2);target.setAccountExpiresAt(now().minusSeconds(1));
        TEmployee employee=employee(10,2,EmployeeStatus.LEFT,3);
        when(lifecycle.lockUserById(2)).thenReturn(target);when(lifecycle.lockEmployeeByUserId(2)).thenReturn(employee);

        BusinessException exception=assertThrows(BusinessException.class,()->service.rehire(2,rehireRequest(AccountActivationMode.INVITE)));

        assertEquals(CodeEnum.USER_LIFECYCLE_CONFLICT,exception.getCodeEnum());
        verify(assignments,never()).insert(any());verify(users,never()).updateAccountStatusByExpected(anyInt(),anyInt(),anyString(),anyBoolean(),anyInt());
    }

    private void stubRehire(TUser target,TEmployee employee){
        when(lifecycle.lockUserById(2)).thenReturn(target);when(lifecycle.lockEmployeeByUserId(2)).thenReturn(employee);
        TOrganizationUnit company=new TOrganizationUnit();company.setId(7);company.setCode("ROOT");company.setName("根公司");company.setType(OrganizationUnitType.COMPANY);
        company.setEnabled(true);company.setMigrationPlaceholder(false);company.setParentId(null);company.setLeaderEmployeeId(employee.getId());
        TPosition position=new TPosition();position.setId(8);position.setCode("MANAGER");position.setName("经理");position.setEnabled(true);position.setBuiltIn(false);
        when(organizations.selectByPrimaryKey(7)).thenReturn(company);when(positions.selectByPrimaryKey(8)).thenReturn(position);
        when(assignments.insert(any())).thenReturn(1);when(users.updateAccountStatusByExpected(2,0,"INVITED",false,1)).thenReturn(1);
        when(lifecycle.transitionEmployee(10,3,"LEFT","ACTIVE",now(),1,true,false)).thenReturn(1);
        when(lifecycle.insertEvent(any())).thenReturn(1);when(organizations.selectAll()).thenReturn(List.of(company));when(positions.selectManageable()).thenReturn(List.of(position));
        when(lifecycle.selectQualifiedCandidates(eq(2),anyList(),eq(now()))).thenReturn(List.of());
    }

    private RehireRequest rehireRequest(AccountActivationMode mode){RehireRequest request=new RehireRequest();request.setEmployeeVersion(3);request.setOrganizationUnitId(7);request.setPositionId(8);
        request.setEffectiveFrom(OffsetDateTime.ofInstant(clock.instant(),clock.getZone()));request.setReason("返聘重新邀请");request.setAccountActivationMode(mode);return request;}
    private TUser rehireUser(int id){TUser value=user(id);value.setAccountStatus(AccountStatus.DISABLED);value.setAccountNoLocked(1);value.setAccountNoExpired(1);value.setCredentialsNoExpired(1);
        value.setManualLocked(false);return value;}
    private LocalDateTime now(){return LocalDateTime.ofInstant(clock.instant(),clock.getZone());}

    private TUser user(int id){TUser value=new TUser();value.setId(id);value.setAccountType(AccountType.HUMAN);value.setProtectedAccount(false);value.setVersion(0);value.setName("员工账号");return value;}
    private TEmployee employee(int id,int userId,EmployeeStatus status,int version){TEmployee value=new TEmployee();value.setId(id);value.setUserId(userId);value.setEmployeeNo("E"+id);value.setName("员工"+id);value.setEmploymentStatus(status);value.setVersion(version);return value;}
}
