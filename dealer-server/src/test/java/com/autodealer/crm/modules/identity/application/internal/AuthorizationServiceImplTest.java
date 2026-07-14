package com.autodealer.crm.modules.identity.application.internal;

import com.autodealer.crm.modules.audit.application.api.AuditActionEnum;
import com.autodealer.crm.modules.identity.application.api.enums.DataScopeCode;
import com.autodealer.crm.modules.identity.application.api.enums.PermissionEffect;
import com.autodealer.crm.modules.identity.persistence.mapper.TAuthorizationGraphLockMapper;
import com.autodealer.crm.modules.identity.persistence.mapper.TEmployeeAssignmentMapper;
import com.autodealer.crm.modules.identity.persistence.mapper.TEmployeeMapper;
import com.autodealer.crm.modules.identity.persistence.mapper.TOrganizationUnitMapper;
import com.autodealer.crm.modules.identity.persistence.mapper.TPermissionMapper;
import com.autodealer.crm.modules.identity.persistence.mapper.TPositionMapper;
import com.autodealer.crm.modules.identity.persistence.mapper.TRoleMapper;
import com.autodealer.crm.modules.identity.persistence.mapper.TRolePermissionMapper;
import com.autodealer.crm.modules.identity.persistence.mapper.TRolePermissionOrganizationMapper;
import com.autodealer.crm.modules.identity.persistence.mapper.TUserMapper;
import com.autodealer.crm.modules.identity.persistence.mapper.TUserPermissionMapper;
import com.autodealer.crm.modules.identity.persistence.mapper.TUserPermissionOrganizationMapper;
import com.autodealer.crm.modules.identity.persistence.mapper.TUserRoleMapper;
import com.autodealer.crm.modules.identity.persistence.model.TOrganizationUnit;
import com.autodealer.crm.modules.identity.persistence.model.TPermission;
import com.autodealer.crm.modules.identity.application.api.*;

import com.autodealer.crm.modules.identity.application.api.AuthorizationAuditRecorder;
import com.autodealer.crm.modules.identity.application.api.security.CurrentUserProvider;
import com.autodealer.crm.modules.identity.application.api.dto.access.UserAuthorizationDtos.PermissionChange;
import com.autodealer.crm.modules.identity.application.api.dto.access.UserAuthorizationDtos.PersonalState;
import com.autodealer.crm.modules.identity.application.api.dto.access.UserAuthorizationDtos.BatchRoleOperation;
import com.autodealer.crm.modules.identity.application.api.dto.access.UserAuthorizationDtos.BatchTarget;
import com.autodealer.crm.modules.identity.application.api.dto.access.UserAuthorizationDtos.BatchUpdatePermissionsRequest;
import com.autodealer.crm.modules.identity.application.api.dto.access.UserAuthorizationDtos.BatchUpdateRolesRequest;
import com.autodealer.crm.modules.identity.application.api.dto.access.UserAuthorizationDtos.UpdatePermissionsRequest;
import com.autodealer.crm.modules.identity.application.api.dto.access.UserAuthorizationDtos.UpdateRolesRequest;
import com.autodealer.crm.shared.error.BusinessException;
import com.autodealer.crm.modules.identity.persistence.mapper.*;
import com.autodealer.crm.modules.identity.persistence.model.TRole;
import com.autodealer.crm.modules.identity.persistence.model.TUserPermission;
import com.autodealer.crm.modules.identity.application.api.model.TUser;
import com.autodealer.crm.modules.identity.persistence.model.TUserRole;
import com.autodealer.crm.shared.error.CodeEnum;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthorizationServiceImplTest {
    @Mock TUserMapper userMapper; @Mock TEmployeeMapper employeeMapper;
    @Mock TEmployeeAssignmentMapper assignmentMapper; @Mock TOrganizationUnitMapper organizationMapper;
    @Mock TPositionMapper positionMapper; @Mock TRoleMapper roleMapper;
    @Mock TRolePermissionMapper rolePermissionMapper; @Mock TPermissionMapper permissionMapper;
    @Mock TUserRoleMapper userRoleMapper; @Mock TUserPermissionMapper userPermissionMapper;
    @Mock TUserPermissionOrganizationMapper userPermissionOrganizationMapper;
    @Mock TRolePermissionOrganizationMapper rolePermissionOrganizationMapper;
    @Mock UserAuthorizationPolicy policy; @Mock CurrentUserProvider currentUserProvider;
    @Mock AuthorizationAuditRecorder auditRecorder;
    @Mock TAuthorizationGraphLockMapper graphLockMapper;
    @Mock UserSecurityMutationCoordinator securityMutations;
    @InjectMocks AuthorizationServiceImpl service;

    @BeforeEach
    void setUpGraphLock() {
        lenient().when(graphLockMapper.lockByName(anyString())).thenAnswer(invocation->invocation.getArgument(0));
    }

    @Test
    void selfManagementFailureIsReturnedBeforeAnyAuthorizationWrite() {
        TUser target=user(2,0);when(userMapper.selectByPrimaryKey(2)).thenReturn(target);
        doThrow(new BusinessException(CodeEnum.SELF_MANAGEMENT_FORBIDDEN)).when(policy).requireRoleManage(target,false);
        BusinessException exception=assertThrows(BusinessException.class,
                ()->service.replaceRoles(2,roles(0,List.of(1))));
        assertEquals(CodeEnum.SELF_MANAGEMENT_FORBIDDEN,exception.getCodeEnum());
        verify(userMapper,never()).incrementAuthorizationVersionsByExpected(anyInt(),anyInt());
    }

    @Test
    void personalPermissionWriteLocksMembershipBeforeTargetAndDelegationChecks() {
        TUser target=user(2,0);when(userMapper.selectByPrimaryKey(2)).thenReturn(target);
        com.autodealer.crm.modules.identity.persistence.model.TPermission permission=new com.autodealer.crm.modules.identity.persistence.model.TPermission();
        permission.setId(7);permission.setEnabled(1);
        when(permissionMapper.selectByPrimaryKey(7)).thenReturn(permission);
        when(policy.isGlobalOperator()).thenReturn(true);
        when(userMapper.incrementAuthorizationVersionsByExpected(2,0)).thenReturn(1);
        when(userPermissionMapper.insert(any())).thenReturn(1);
        when(permissionMapper.selectAll()).thenReturn(List.of());

        PermissionChange change=new PermissionChange();change.setPermissionId(7);change.setState(PersonalState.DENY);
        UpdatePermissionsRequest request=new UpdatePermissionsRequest();request.setAuthorizationVersion(0);
        request.setChanges(List.of(change));request.setReason("并发委派边界测试");

        service.updatePermissions(2,request);

        InOrder order=inOrder(graphLockMapper,userMapper,policy,permissionMapper);
        order.verify(graphLockMapper).lockByName("AUTHORIZATION_MEMBERSHIP_GUARD");
        order.verify(graphLockMapper).lockByName("ORGANIZATION_HIERARCHY");
        order.verify(graphLockMapper).lockByName("REPORTING_GRAPH");
        order.verify(userMapper).selectByPrimaryKey(2);
        order.verify(policy).requireAuthorizationManage(target);
        order.verify(permissionMapper).selectByPrimaryKey(7);
        verify(userPermissionMapper).insert(argThat(value->value.getUserId()==2
                &&value.getPermissionId()==7&&value.getEffect()==com.autodealer.crm.modules.identity.application.api.enums.PermissionEffect.DENY));
        verify(securityMutations).accessChanged(2,"授权变化");
    }

    @Test
    void customOrganizationGrantRejectsEnabledOrganizationOutsideDelegableTargetSubset() {
        TUser target=user(2,0);when(userMapper.selectByPrimaryKey(2)).thenReturn(target);
        com.autodealer.crm.modules.identity.persistence.model.TPermission permission=new com.autodealer.crm.modules.identity.persistence.model.TPermission();
        permission.setId(7);permission.setEnabled(1);
        when(permissionMapper.selectByPrimaryKey(7)).thenReturn(permission);
        com.autodealer.crm.modules.identity.persistence.model.TOrganizationUnit organization=new com.autodealer.crm.modules.identity.persistence.model.TOrganizationUnit();
        organization.setId(9);organization.setEnabled(true);organization.setMigrationPlaceholder(false);
        when(organizationMapper.selectByPrimaryKey(9)).thenReturn(organization);
        when(policy.delegableCustomOrganizationIds(permission,target)).thenReturn(List.of(8));
        PermissionChange change=new PermissionChange();change.setPermissionId(7);change.setState(PersonalState.GRANT);
        change.setDataScopeCandidateKey("CUSTOM_ORGS");change.setCustomOrganizationUnitIds(List.of(9));
        UpdatePermissionsRequest request=new UpdatePermissionsRequest();request.setAuthorizationVersion(0);
        request.setChanges(List.of(change));request.setReason("越界指定组织");

        BusinessException exception=assertThrows(BusinessException.class,()->service.updatePermissions(2,request));

        assertEquals(CodeEnum.ACCESS_DENIED,exception.getCodeEnum());
        verify(userMapper,never()).incrementAuthorizationVersionsByExpected(anyInt(),anyInt());
        verify(userPermissionMapper,never()).insert(any());
    }

    @Test
    void futurePermissionPersistsRequestedScheduleInsteadOfForcingCurrentTime() {
        TUser target=user(2,0);when(userMapper.selectByPrimaryKey(2)).thenReturn(target);
        com.autodealer.crm.modules.identity.persistence.model.TPermission permission=new com.autodealer.crm.modules.identity.persistence.model.TPermission();
        permission.setId(7);permission.setEnabled(1);when(permissionMapper.selectByPrimaryKey(7)).thenReturn(permission);
        when(policy.canDelegatePermission(eq(permission),eq(com.autodealer.crm.modules.identity.application.api.enums.DataScopeCode.SELF),eq(List.of()),eq(target))).thenReturn(true);
        when(userMapper.incrementAuthorizationVersionsByExpected(2,0)).thenReturn(1);
        when(userPermissionMapper.insert(any())).thenReturn(1);when(permissionMapper.selectAll()).thenReturn(List.of());
        OffsetDateTime effectiveFrom=OffsetDateTime.now().plusDays(10).withNano(0);
        OffsetDateTime effectiveTo=effectiveFrom.plusDays(20);
        PermissionChange change=new PermissionChange();change.setPermissionId(7);change.setState(PersonalState.GRANT);
        change.setDataScopeCandidateKey("SELF");change.setEffectiveFrom(effectiveFrom);change.setEffectiveTo(effectiveTo);
        UpdatePermissionsRequest request=permissionsRequest(change);

        service.updatePermissions(2,request);

        ArgumentCaptor<TUserPermission> inserted=ArgumentCaptor.forClass(TUserPermission.class);
        verify(userPermissionMapper).insert(inserted.capture());
        assertEquals(LocalDateTime.ofInstant(effectiveFrom.toInstant(),ZoneId.systemDefault()),inserted.getValue().getEffectiveFrom());
        assertEquals(LocalDateTime.ofInstant(effectiveTo.toInstant(),ZoneId.systemDefault()),inserted.getValue().getEffectiveTo());
    }

    @Test
    void permissionScheduleRejectsPastBeyondOneYearAndNonPositivePeriodBeforeCas() {
        TUser target=user(2,0);when(userMapper.selectByPrimaryKey(2)).thenReturn(target);
        com.autodealer.crm.modules.identity.persistence.model.TPermission permission=new com.autodealer.crm.modules.identity.persistence.model.TPermission();
        permission.setId(7);permission.setEnabled(1);when(permissionMapper.selectByPrimaryKey(7)).thenReturn(permission);

        PermissionChange past=denyAt(OffsetDateTime.now().minusMinutes(1),null);
        PermissionChange tooFar=denyAt(OffsetDateTime.now().plusYears(1).plusDays(1),null);
        OffsetDateTime future=OffsetDateTime.now().plusDays(2);
        PermissionChange invalidPeriod=denyAt(future,future);

        assertEquals(CodeEnum.PARAM_ERROR,assertThrows(BusinessException.class,
                ()->service.updatePermissions(2,permissionsRequest(past))).getCodeEnum());
        assertEquals(CodeEnum.PARAM_ERROR,assertThrows(BusinessException.class,
                ()->service.updatePermissions(2,permissionsRequest(tooFar))).getCodeEnum());
        assertEquals(CodeEnum.PARAM_ERROR,assertThrows(BusinessException.class,
                ()->service.updatePermissions(2,permissionsRequest(invalidPeriod))).getCodeEnum());
        verify(userMapper,never()).incrementAuthorizationVersionsByExpected(anyInt(),anyInt());
        verify(userPermissionMapper,never()).insert(any());
    }

    @Test
    void inheritClosesFuturePermissionWithoutDeletingPlanOrOrganizationFacts() {
        TUser target=user(2,0);when(userMapper.selectByPrimaryKey(2)).thenReturn(target);
        com.autodealer.crm.modules.identity.persistence.model.TPermission permission=new com.autodealer.crm.modules.identity.persistence.model.TPermission();
        permission.setId(7);permission.setEnabled(1);when(permissionMapper.selectByPrimaryKey(7)).thenReturn(permission);
        LocalDateTime planned=LocalDateTime.now().plusDays(5).withNano(0);
        TUserPermission before=new TUserPermission();before.setId(91L);before.setUserId(2);before.setPermissionId(7);
        before.setEffect(com.autodealer.crm.modules.identity.application.api.enums.PermissionEffect.GRANT);before.setDataScopeCode(com.autodealer.crm.modules.identity.application.api.enums.DataScopeCode.CUSTOM_ORGS);
        before.setEffectiveFrom(planned);before.setActiveMarker(true);before.setVersion(3);
        when(userPermissionMapper.selectCurrent(2,7)).thenReturn(before);
        when(userPermissionOrganizationMapper.selectOrganizationIds(91L)).thenReturn(List.of(9));
        when(userMapper.incrementAuthorizationVersionsByExpected(2,0)).thenReturn(1);
        when(userPermissionMapper.closeByIdAndVersion(eq(91L),eq(3),any())).thenReturn(1);
        when(permissionMapper.selectAll()).thenReturn(List.of());
        PermissionChange change=new PermissionChange();change.setPermissionId(7);change.setState(PersonalState.INHERIT);

        service.updatePermissions(2,permissionsRequest(change));

        assertEquals(planned,before.getEffectiveFrom());
        verify(userPermissionMapper).closeByIdAndVersion(eq(91L),eq(3),any());
        verify(userPermissionMapper,never()).deleteByUserAndPermission(anyInt(),anyInt());
        verify(userPermissionOrganizationMapper,never()).deleteByUserPermissionId(anyLong());
        verify(auditRecorder).recordAll(argThat(histories->histories.size()==1
                        && planned.equals(histories.get(0).getEffectiveFrom())
                        && histories.get(0).getBeforeValue().contains("\"effectiveFrom\"")),
                eq(com.autodealer.crm.modules.audit.application.api.AuditActionEnum.USER_PERMISSION_CHANGE),eq("2"),contains("\"count\":1"));
    }

    @Test
    void batchRoleAssignmentValidatesAndWritesEveryTargetUnderOneLockAndAudit() {
        TUser first=user(2,1),second=user(3,4);
        when(userMapper.selectByPrimaryKey(2)).thenReturn(first);when(userMapper.selectByPrimaryKey(3)).thenReturn(second);
        when(userRoleMapper.selectCurrentAndFutureByUserId(anyInt(),any())).thenReturn(List.of());
        TRole role=role(7);when(roleMapper.selectByPrimaryKey(7)).thenReturn(role);
        when(policy.canDelegateRole(eq(role),any())).thenReturn(true);
        when(userMapper.incrementAuthorizationVersionsByExpected(2,1)).thenReturn(1);
        when(userMapper.incrementAuthorizationVersionsByExpected(3,4)).thenReturn(1);
        when(userRoleMapper.insert(any())).thenReturn(1);when(currentUserProvider.getCurrentUserId()).thenReturn(9);
        BatchUpdateRolesRequest request=new BatchUpdateRolesRequest();request.setTargets(List.of(batchTarget(2,1),batchTarget(3,4)));
        request.setOperation(BatchRoleOperation.ASSIGN);request.setRoleIds(List.of(7));request.setReason("批量补充分店角色");

        var result=service.batchUpdateRoles(request);

        assertEquals(2,result.getTargetCount());assertEquals(2,result.getChangedTargetCount());
        assertEquals(List.of(2,3),result.getTargets().stream().map(value->value.getUserId()).toList());
        verify(userRoleMapper,times(2)).insert(argThat(value->value.getRoleId()==7));
        verify(auditRecorder).recordAll(argThat(histories->histories.size()==2),
                eq(com.autodealer.crm.modules.audit.application.api.AuditActionEnum.USER_ROLE_CHANGE),eq("batch:ROLE_ASSIGN"),contains("\"totalCount\":2"));
        verify(securityMutations).accessChanged(2,"授权变化");
        verify(securityMutations).accessChanged(3,"授权变化");
        InOrder locks=inOrder(graphLockMapper,userMapper);
        locks.verify(graphLockMapper).lockByName("AUTHORIZATION_MEMBERSHIP_GUARD");
        locks.verify(graphLockMapper).lockByName("ORGANIZATION_HIERARCHY");
        locks.verify(graphLockMapper).lockByName("REPORTING_GRAPH");
        locks.verify(graphLockMapper).lockByName("AVAILABLE_ADMIN_GUARD");
        locks.verify(userMapper).selectByPrimaryKey(2);
    }

    @Test
    void batchPermissionGrantAppliesSameValidatedChangeToEveryTarget() {
        TUser first=user(2,0),second=user(3,2);
        when(userMapper.selectByPrimaryKey(2)).thenReturn(first);when(userMapper.selectByPrimaryKey(3)).thenReturn(second);
        com.autodealer.crm.modules.identity.persistence.model.TPermission permission=new com.autodealer.crm.modules.identity.persistence.model.TPermission();
        permission.setId(7);permission.setEnabled(1);when(permissionMapper.selectByPrimaryKey(7)).thenReturn(permission);
        when(policy.canDelegatePermission(eq(permission),eq(com.autodealer.crm.modules.identity.application.api.enums.DataScopeCode.SELF),eq(List.of()),any())).thenReturn(true);
        when(userMapper.incrementAuthorizationVersionsByExpected(2,0)).thenReturn(1);
        when(userMapper.incrementAuthorizationVersionsByExpected(3,2)).thenReturn(1);
        when(userPermissionMapper.insert(any())).thenReturn(1);
        PermissionChange change=new PermissionChange();change.setPermissionId(7);change.setState(PersonalState.GRANT);change.setDataScopeCandidateKey("SELF");
        BatchUpdatePermissionsRequest request=new BatchUpdatePermissionsRequest();request.setTargets(List.of(batchTarget(2,0),batchTarget(3,2)));
        request.setChanges(List.of(change));request.setReason("批量增加临时查看权限");

        var result=service.batchUpdatePermissions(request);

        assertEquals(2,result.getChangedTargetCount());verify(userPermissionMapper,times(2)).insert(any());
        verify(securityMutations).accessChanged(2,"授权变化");
        verify(securityMutations).accessChanged(3,"授权变化");
        verify(auditRecorder).recordAll(argThat(histories->histories.size()==2),
                eq(com.autodealer.crm.modules.audit.application.api.AuditActionEnum.USER_PERMISSION_CHANGE),eq("batch:PERMISSION_BATCH_UPDATE"),contains("\"totalCount\":2"));
    }

    @Test
    void duplicateBatchTargetIsRejectedBeforeAnyAuthorizationWrite() {
        BatchUpdateRolesRequest request=new BatchUpdateRolesRequest();request.setTargets(List.of(batchTarget(2,0),batchTarget(2,0)));
        request.setOperation(BatchRoleOperation.ASSIGN);request.setRoleIds(List.of(7));request.setReason("重复目标");
        BusinessException exception=assertThrows(BusinessException.class,()->service.batchUpdateRoles(request));
        assertEquals(CodeEnum.PARAM_ERROR,exception.getCodeEnum());verify(userMapper,never()).selectByPrimaryKey(anyInt());
        verify(userRoleMapper,never()).insert(any());
    }

    @Test
    void removingLastAvailableOrdinaryAdminFailsAfterGuardedFinalStateCheck() {
        TUser target=user(2,0);when(userMapper.selectByPrimaryKey(2)).thenReturn(target);
        TUserRole assignment=new TUserRole();assignment.setId(11L);assignment.setRoleId(1);assignment.setVersion(0);assignment.setActiveMarker(true);
        when(userRoleMapper.selectCurrentAndFutureByUserId(eq(2),any())).thenReturn(List.of(assignment));
        TRole admin=role(1);admin.setRole("admin");admin.setProtectedRole(true);when(roleMapper.selectByPrimaryKey(1)).thenReturn(admin);
        when(policy.canRevokeRole(admin,target)).thenReturn(true);when(userMapper.incrementAuthorizationVersionsByExpected(2,0)).thenReturn(1);
        when(userRoleMapper.closeByIdAndVersion(eq(11L),eq(0),any())).thenReturn(1);when(userMapper.countAdminUsers()).thenReturn(0);

        BusinessException exception=assertThrows(BusinessException.class,()->service.replaceRoles(2,roles(0,List.of())));

        assertEquals(CodeEnum.LAST_AVAILABLE_ADMIN_REQUIRED,exception.getCodeEnum());
        verify(graphLockMapper).lockByName("AVAILABLE_ADMIN_GUARD");verify(userMapper).countAdminUsers();
        verify(auditRecorder,never()).recordAll(anyList(),any(),anyString(),anyString());
    }

    @Test
    void roleReplacementClosesOnlyRemovedFactAndInsertsOnlyAddedFact() {
        TUser target=user(2,3);when(userMapper.selectByPrimaryKey(2)).thenReturn(target);
        TUserRole old=new TUserRole();old.setId(11L);old.setRoleId(1);old.setVersion(4);old.setActiveMarker(true);
        when(userRoleMapper.selectCurrentAndFutureByUserId(eq(2),any())).thenReturn(List.of(old));
        when(roleMapper.selectByPrimaryKey(1)).thenReturn(role(1));when(roleMapper.selectByPrimaryKey(2)).thenReturn(role(2));
        when(policy.canRevokeRole(any(),eq(target))).thenReturn(true);when(policy.canDelegateRole(any(),eq(target))).thenReturn(true);
        when(userMapper.incrementAuthorizationVersionsByExpected(2,3)).thenReturn(1);
        when(userRoleMapper.closeByIdAndVersion(eq(11L),eq(4),any())).thenReturn(1);
        when(userRoleMapper.insert(any())).thenReturn(1);when(currentUserProvider.getCurrentUserId()).thenReturn(9);
        when(userRoleMapper.selectEffectiveByUserId(eq(2),any())).thenReturn(List.of());
        when(policy.roleCandidates(target)).thenReturn(List.of());when(permissionMapper.selectAll()).thenReturn(List.of());

        service.replaceRoles(2,roles(3,List.of(2)));

        verify(userRoleMapper).closeByIdAndVersion(eq(11L),eq(4),any());
        ArgumentCaptor<TUserRole> inserted=ArgumentCaptor.forClass(TUserRole.class);
        verify(userRoleMapper).insert(inserted.capture());
        assertEquals(2,inserted.getValue().getRoleId());assertTrue(inserted.getValue().getActiveMarker());
        assertNotNull(inserted.getValue().getEffectiveFrom());
        verify(securityMutations).accessChanged(2,"授权变化");
    }

    @Test
    void closedRoleCanBeGrantedAgainBecauseOnlyActiveFactsParticipateInDiff() {
        TUser target=user(2,4);when(userMapper.selectByPrimaryKey(2)).thenReturn(target);
        when(userRoleMapper.selectCurrentAndFutureByUserId(eq(2),any())).thenReturn(List.of());
        when(roleMapper.selectByPrimaryKey(1)).thenReturn(role(1));when(policy.canDelegateRole(any(),eq(target))).thenReturn(true);
        when(userMapper.incrementAuthorizationVersionsByExpected(2,4)).thenReturn(1);when(userRoleMapper.insert(any())).thenReturn(1);
        when(userRoleMapper.selectEffectiveByUserId(eq(2),any())).thenReturn(List.of());when(policy.roleCandidates(target)).thenReturn(List.of());
        when(permissionMapper.selectAll()).thenReturn(List.of());
        service.replaceRoles(2,roles(4,List.of(1)));
        verify(userRoleMapper).insert(argThat(value->value.getRoleId()==1&&Boolean.TRUE.equals(value.getActiveMarker())));
    }

    @Test
    void targetVersionConflictLeavesRoleFactsUntouched() {
        TUser target=user(2,5);when(userMapper.selectByPrimaryKey(2)).thenReturn(target);
        when(userRoleMapper.selectCurrentAndFutureByUserId(eq(2),any())).thenReturn(List.of());
        when(roleMapper.selectByPrimaryKey(1)).thenReturn(role(1));when(policy.canDelegateRole(any(),eq(target))).thenReturn(true);
        when(userMapper.incrementAuthorizationVersionsByExpected(2,5)).thenReturn(0);
        BusinessException exception=assertThrows(BusinessException.class,
                ()->service.replaceRoles(2,roles(5,List.of(1))));
        assertEquals(CodeEnum.ROLE_VERSION_CONFLICT,exception.getCodeEnum());
        verify(userRoleMapper,never()).insert(any());verify(auditRecorder,never()).recordAll(anyList(),any(),anyString(),anyString());
    }

    private UpdateRolesRequest roles(int version,List<Integer>ids){UpdateRolesRequest request=new UpdateRolesRequest();request.setAuthorizationVersion(version);request.setRoleIds(ids);request.setReason("授权测试");return request;}
    private UpdatePermissionsRequest permissionsRequest(PermissionChange change){UpdatePermissionsRequest request=new UpdatePermissionsRequest();request.setAuthorizationVersion(0);request.setChanges(List.of(change));request.setReason("预约授权测试");return request;}
    private PermissionChange denyAt(OffsetDateTime from,OffsetDateTime to){PermissionChange change=new PermissionChange();change.setPermissionId(7);change.setState(PersonalState.DENY);change.setEffectiveFrom(from);change.setEffectiveTo(to);return change;}
    private BatchTarget batchTarget(int userId,int version){BatchTarget target=new BatchTarget();target.setUserId(userId);target.setAuthorizationVersion(version);return target;}
    private TUser user(int id,int version){TUser user=new TUser();user.setId(id);user.setAuthorizationVersion(version);user.setAccountEnabled(1);user.setProtectedAccount(false);return user;}
    private TRole role(int id){TRole role=new TRole();role.setId(id);role.setEnabled(1);role.setProtectedRole(false);return role;}
}
