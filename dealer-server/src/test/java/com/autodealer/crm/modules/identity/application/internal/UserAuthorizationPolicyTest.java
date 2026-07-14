package com.autodealer.crm.modules.identity.application.internal;

import com.autodealer.crm.modules.identity.application.api.model.TUser;
import com.autodealer.crm.modules.identity.persistence.mapper.TEmployeeAssignmentMapper;
import com.autodealer.crm.modules.identity.persistence.mapper.TEmployeeMapper;
import com.autodealer.crm.modules.identity.persistence.mapper.TEmployeeReportingMapper;
import com.autodealer.crm.modules.identity.persistence.mapper.TOrganizationUnitMapper;
import com.autodealer.crm.modules.identity.persistence.mapper.TPermissionMapper;
import com.autodealer.crm.modules.identity.persistence.mapper.TRoleMapper;
import com.autodealer.crm.modules.identity.persistence.mapper.TRoleOrganizationMapper;
import com.autodealer.crm.modules.identity.persistence.mapper.TRolePermissionMapper;
import com.autodealer.crm.modules.identity.persistence.mapper.TRolePermissionOrganizationMapper;
import com.autodealer.crm.modules.identity.persistence.mapper.TUserMapper;
import com.autodealer.crm.modules.identity.persistence.mapper.TUserPermissionMapper;
import com.autodealer.crm.modules.identity.persistence.mapper.TUserPermissionOrganizationMapper;
import com.autodealer.crm.modules.identity.persistence.mapper.TUserRoleMapper;
import com.autodealer.crm.modules.identity.persistence.model.TEmployee;
import com.autodealer.crm.modules.identity.persistence.model.TEmployeeAssignment;
import com.autodealer.crm.modules.identity.persistence.model.TEmployeeReporting;
import com.autodealer.crm.modules.identity.persistence.model.TOrganizationUnit;
import com.autodealer.crm.modules.identity.persistence.model.TPermission;
import com.autodealer.crm.modules.identity.persistence.model.TRole;
import com.autodealer.crm.modules.identity.persistence.model.TRolePermission;
import com.autodealer.crm.modules.identity.persistence.model.TUserPermission;
import com.autodealer.crm.modules.identity.persistence.model.TUserRole;
import com.autodealer.crm.modules.identity.application.api.*;

import com.autodealer.crm.modules.identity.application.api.enums.DataScopeCode;
import com.autodealer.crm.modules.identity.application.api.enums.AccountType;
import com.autodealer.crm.modules.identity.application.api.enums.PermissionEffect;
import com.autodealer.crm.modules.identity.application.api.enums.PermissionSensitivityLevel;
import com.autodealer.crm.modules.identity.application.api.enums.RoleScopeType;
import com.autodealer.crm.modules.identity.application.api.security.CurrentUserProvider;
import com.autodealer.crm.shared.error.BusinessException;
import com.autodealer.crm.modules.identity.persistence.mapper.*;
import com.autodealer.crm.modules.identity.persistence.model.*;
import com.autodealer.crm.modules.identity.application.api.model.*;
import com.autodealer.crm.shared.error.CodeEnum;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserAuthorizationPolicyTest {
    @Mock CurrentUserProvider currentUserProvider; @Mock TUserMapper userMapper;
    @Mock TEmployeeMapper employeeMapper; @Mock TEmployeeAssignmentMapper assignmentMapper;
    @Mock TEmployeeReportingMapper reportingMapper; @Mock TOrganizationUnitMapper organizationMapper;
    @Mock TRoleMapper roleMapper; @Mock TRolePermissionMapper rolePermissionMapper;
    @Mock TRoleOrganizationMapper roleOrganizationMapper;
    @Mock TRolePermissionOrganizationMapper rolePermissionOrganizationMapper;
    @Mock TUserRoleMapper userRoleMapper; @Mock TUserPermissionMapper userPermissionMapper;
    @Mock TUserPermissionOrganizationMapper userPermissionOrganizationMapper;
    @Mock TPermissionMapper permissionMapper;
    @InjectMocks UserAuthorizationPolicy policy;
    @Test
    void scopeNarrowingMatrixAllowsOnlyProvenSubsets() {
        assertTrue(UserAuthorizationPolicy.scopeMatrixCovers(Set.of(DataScopeCode.ORG_TREE), DataScopeCode.PRIMARY_ORG, true));
        assertFalse(UserAuthorizationPolicy.scopeMatrixCovers(Set.of(DataScopeCode.PRIMARY_ORG), DataScopeCode.ORG_TREE, true));
        assertTrue(UserAuthorizationPolicy.scopeMatrixCovers(Set.of(DataScopeCode.REPORTING_TREE), DataScopeCode.DIRECT_REPORTS, true));
        assertFalse(UserAuthorizationPolicy.scopeMatrixCovers(Set.of(DataScopeCode.DIRECT_REPORTS), DataScopeCode.REPORTING_TREE, true));
        assertTrue(UserAuthorizationPolicy.scopeMatrixCovers(Set.of(DataScopeCode.DIRECT_REPORTS), DataScopeCode.SELF, true));
        assertFalse(UserAuthorizationPolicy.scopeMatrixCovers(Set.of(DataScopeCode.DIRECT_REPORTS), DataScopeCode.SELF, false));
        assertFalse(UserAuthorizationPolicy.scopeMatrixCovers(Set.of(DataScopeCode.CUSTOM_ORGS), DataScopeCode.CUSTOM_ORGS, true));
        assertTrue(UserAuthorizationPolicy.scopeMatrixCovers(Set.of(DataScopeCode.GLOBAL), DataScopeCode.CUSTOM_ORGS, true));
    }

    @Test void directAndIndirectSubordinatesWithinOrganizationTreeAreManageable() {
        TUser operator=human(1),directUser=human(2),indirectUser=human(3);baseOperator(operator);
        TEmployee operatorEmployee=employee(10,1),direct=employee(20,2),indirect=employee(30,3);
        when(employeeMapper.selectByUserId(1)).thenReturn(operatorEmployee);
        when(employeeMapper.selectByUserId(2)).thenReturn(direct);when(employeeMapper.selectByUserId(3)).thenReturn(indirect);
        when(reportingMapper.selectEffectiveManagers(eq(20),any())).thenReturn(List.of(reporting(20,10)));
        when(reportingMapper.selectEffectiveManagers(eq(30),any())).thenReturn(List.of(reporting(30,20)));
        TEmployeeAssignment operatorOrg=assignment(1),directOrg=assignment(2),indirectOrg=assignment(3);
        when(assignmentMapper.selectCurrentPrimaryByEmployeeId(eq(10),any())).thenReturn(operatorOrg);
        when(assignmentMapper.selectCurrentPrimaryByEmployeeId(eq(20),any())).thenReturn(directOrg);
        when(assignmentMapper.selectCurrentPrimaryByEmployeeId(eq(30),any())).thenReturn(indirectOrg);
        when(organizationMapper.selectByPrimaryKey(2)).thenReturn(org(2,1));when(organizationMapper.selectByPrimaryKey(3)).thenReturn(org(3,2));
        assertTrue(policy.canManage(directUser));assertTrue(policy.canManage(indirectUser));
    }

    @Test void sameLevelUpperAndCrossOrganizationTargetsAreRejected() {
        TUser operator=human(1),same=human(2),upper=human(3),cross=human(4);baseOperator(operator);
        when(employeeMapper.selectByUserId(1)).thenReturn(employee(10,1));
        when(employeeMapper.selectByUserId(2)).thenReturn(employee(20,2));
        when(employeeMapper.selectByUserId(3)).thenReturn(employee(30,3));
        when(employeeMapper.selectByUserId(4)).thenReturn(employee(40,4));
        when(reportingMapper.selectEffectiveManagers(eq(20),any())).thenReturn(List.of());
        when(reportingMapper.selectEffectiveManagers(eq(30),any())).thenReturn(List.of(reporting(30,99)));
        when(reportingMapper.selectEffectiveManagers(eq(99),any())).thenReturn(List.of());
        when(reportingMapper.selectEffectiveManagers(eq(40),any())).thenReturn(List.of(reporting(40,10)));
        when(assignmentMapper.selectCurrentPrimaryByEmployeeId(eq(10),any())).thenReturn(assignment(1));
        when(assignmentMapper.selectCurrentPrimaryByEmployeeId(eq(40),any())).thenReturn(assignment(9));
        when(organizationMapper.selectByPrimaryKey(9)).thenReturn(org(9,null));
        assertFalse(policy.canManage(same));assertFalse(policy.canManage(upper));assertFalse(policy.canManage(cross));
    }

    @Test void reportingManagerCannotWriteAuthorizationUnlessStrictlyAboveTargetLevel() {
        TUser operator=human(1),target=human(2);baseOperator(operator);
        when(employeeMapper.selectByUserId(1)).thenReturn(employee(10,1));
        when(employeeMapper.selectByUserId(2)).thenReturn(employee(20,2));
        when(reportingMapper.selectEffectiveManagers(eq(20),any())).thenReturn(List.of(reporting(20,10)));
        when(assignmentMapper.selectCurrentPrimaryByEmployeeId(eq(10),any())).thenReturn(assignment(1));
        when(assignmentMapper.selectCurrentPrimaryByEmployeeId(eq(20),any())).thenReturn(assignment(2));
        when(organizationMapper.selectByPrimaryKey(2)).thenReturn(org(2,1));
        TRole managerRole=role(10,50),sameLevelRole=role(20,50),lowerRole=role(30,30);
        when(userRoleMapper.selectEffectiveByUserId(eq(1),any())).thenReturn(List.of(userRole(10)));
        when(userRoleMapper.selectEffectiveByUserId(eq(2),any())).thenReturn(List.of(userRole(20)));
        when(roleMapper.selectByPrimaryKey(10)).thenReturn(managerRole);
        when(roleMapper.selectByPrimaryKey(20)).thenReturn(sameLevelRole);

        assertTrue(policy.canManage(target));
        assertFalse(policy.canManageAuthorization(target));
        assertEquals(CodeEnum.ACCESS_DENIED,
                assertThrows(BusinessException.class,()->policy.requireAuthorizationManage(target)).getCodeEnum());

        when(userRoleMapper.selectEffectiveByUserId(eq(2),any())).thenReturn(List.of(userRole(30)));
        when(roleMapper.selectByPrimaryKey(30)).thenReturn(lowerRole);
        assertTrue(policy.canManageAuthorization(target));
    }

    @Test void selfAndProtectedTargetsRemainForbiddenEvenForGlobalAdministrator() {
        TUser administrator=human(1);when(currentUserProvider.getCurrentUserId()).thenReturn(1);
        TUser protectedTarget=human(2);protectedTarget.setProtectedAccount(true);protectedTarget.setAccountType(AccountType.SYSTEM);
        assertFalse(policy.canManage(administrator));assertFalse(policy.canManage(protectedTarget));
        assertEquals(CodeEnum.SELF_MANAGEMENT_FORBIDDEN,
                assertThrows(BusinessException.class,()->policy.requireManage(administrator)).getCodeEnum());
        assertEquals(CodeEnum.ACCESS_DENIED,
                assertThrows(BusinessException.class,()->policy.requireManage(protectedTarget)).getCodeEnum());
    }

    @Test void qualifiedSecurityAdministratorCanManageLowerTargetAndAdminMembership() {
        TUser operator=human(1),target=human(2);
        when(currentUserProvider.getCurrentUserId()).thenReturn(1);
        when(currentUserProvider.getCurrentUser()).thenReturn(operator);
        when(userMapper.countQualifiedSecurityAdministrator(1)).thenReturn(1);
        TRole admin=protectedRole("admin");admin.setId(10);
        TRole lower=new TRole();lower.setId(20);lower.setRole("sales");lower.setEnabled(1);
        lower.setProtectedRole(false);lower.setScopeType(RoleScopeType.GLOBAL);lower.setAuthorizationLevel(10);
        when(userRoleMapper.selectEffectiveByUserId(eq(1),any())).thenReturn(List.of(userRole(10)));
        when(userRoleMapper.selectEffectiveByUserId(eq(2),any())).thenReturn(List.of(userRole(20)));
        when(roleMapper.selectByPrimaryKey(10)).thenReturn(admin);
        when(roleMapper.selectByPrimaryKey(20)).thenReturn(lower);
        when(roleMapper.selectByCode("admin")).thenReturn(admin);

        assertTrue(policy.canManage(target));
        assertTrue(policy.canManageAuthorization(target));
        assertTrue(policy.canDelegateRole(admin,target));
        assertTrue(policy.canRevokeRole(admin,target));
    }

    @Test void adminAuthorityNameAloneDoesNotGrantGlobalUserGovernance() {
        TUser operator=human(1),target=human(2);
        operator.setRoleList(List.of("admin"));
        when(currentUserProvider.getCurrentUserId()).thenReturn(1);
        when(currentUserProvider.getCurrentUser()).thenReturn(operator);

        assertTrue(operator.getAuthorities().stream().anyMatch(authority->"admin".equals(authority.getAuthority())));
        assertFalse(policy.isGlobalOperator());
        assertFalse(policy.canManage(target));
        assertFalse(policy.canDelegateRole(protectedRole("admin"),target));
    }

    @Test void qualifiedPeerCanOnlyUseProtectedAdministratorMembershipException() {
        TUser operator=human(1),target=human(2);
        when(currentUserProvider.getCurrentUserId()).thenReturn(1);
        when(currentUserProvider.getCurrentUser()).thenReturn(operator);
        when(userMapper.countQualifiedSecurityAdministrator(1)).thenReturn(1);
        TRole admin=protectedRole("admin");admin.setId(10);
        when(userRoleMapper.selectEffectiveByUserId(eq(1),any())).thenReturn(List.of(userRole(10)));
        when(userRoleMapper.selectEffectiveByUserId(eq(2),any())).thenReturn(List.of(userRole(10)));
        when(roleMapper.selectByPrimaryKey(10)).thenReturn(admin);
        when(roleMapper.selectByCode("admin")).thenReturn(admin);
        TRole sameLevelOrdinary=new TRole();sameLevelOrdinary.setId(11);sameLevelOrdinary.setRole("same_level");
        sameLevelOrdinary.setEnabled(1);sameLevelOrdinary.setProtectedRole(false);sameLevelOrdinary.setScopeType(RoleScopeType.GLOBAL);
        sameLevelOrdinary.setAuthorizationLevel(100);sameLevelOrdinary.setDefaultDataScope(DataScopeCode.SELF);
        when(roleMapper.selectAll()).thenReturn(List.of(admin,sameLevelOrdinary));

        assertFalse(policy.canManage(target));
        assertTrue(policy.canManageRoleAssignments(target));
        assertDoesNotThrow(()->policy.requireRoleManage(target,true));
        assertEquals(CodeEnum.ACCESS_DENIED,
                assertThrows(BusinessException.class,()->policy.requireRoleManage(target,false)).getCodeEnum());
        assertTrue(policy.canDelegateRole(admin,target));
        assertTrue(policy.canRevokeRole(admin,target));
        assertEquals(List.of(10),policy.roleCandidates(target).stream().map(TRole::getId).toList());
    }

    @Test void protectedRecoveryCanSeeOnlyAdminAmongProtectedInitialRoleCandidates() {
        TUser recovery = new TUser();recovery.setId(1);recovery.setAccountType(AccountType.SYSTEM);
        recovery.setProtectedAccount(true);recovery.setLoginAct("admin");
        when(currentUserProvider.getCurrentUser()).thenReturn(recovery);
        TRole admin = protectedRole("admin");admin.setId(1);admin.setRoleName("系统管理员");
        TRole recoveryPolicy = protectedRole("recovery_policy");recoveryPolicy.setId(2);
        TRole ordinary = new TRole();ordinary.setId(3);ordinary.setRole("sales");ordinary.setRoleName("销售");
        ordinary.setEnabled(1);ordinary.setProtectedRole(false);ordinary.setScopeType(RoleScopeType.GLOBAL);
        when(roleMapper.selectAll()).thenReturn(List.of(admin, recoveryPolicy, ordinary));

        assertEquals(List.of(1), policy.assignableRoleCandidates(9).stream().map(TRole::getId).toList());
        when(organizationMapper.countInitializedRootOrganizations()).thenReturn(1);
        assertEquals(List.of(),policy.assignableRoleCandidates(9));
    }

    @Test void nonGlobalOperatorAndOtherProtectedRolesCannotChangeProtectedMembership() {
        TUser operator=human(1),target=human(2);
        when(currentUserProvider.getCurrentUser()).thenReturn(operator);
        TRole admin=protectedRole("admin"),otherProtected=protectedRole("recovery_policy");

        assertFalse(policy.canDelegateRole(admin,target));
        assertFalse(policy.canRevokeRole(admin,target));
        assertFalse(policy.isGlobalOperator());
        assertFalse(policy.canDelegateRole(otherProtected,target));
        assertFalse(policy.canRevokeRole(otherProtected,target));
    }

    @Test void customOrganizationsMustBeWithinOperatorPermissionCoverageAndTargetOrganizationTree() {
        TUser operator=human(1),target=human(2);baseOperator(operator);
        TEmployee operatorEmployee=employee(10,1),targetEmployee=employee(20,2);
        when(employeeMapper.selectByUserId(1)).thenReturn(operatorEmployee);
        when(employeeMapper.selectByUserId(2)).thenReturn(targetEmployee);
        when(assignmentMapper.selectCurrentPrimaryByEmployeeId(eq(10),any())).thenReturn(assignment(1));
        when(assignmentMapper.selectCurrentPrimaryByEmployeeId(eq(20),any())).thenReturn(assignment(2));
        when(organizationMapper.selectDescendantIds(1)).thenReturn(List.of(1,2,3,4));
        when(organizationMapper.selectByPrimaryKey(1)).thenReturn(org(1,null));
        when(organizationMapper.selectByPrimaryKey(2)).thenReturn(org(2,1));
        when(organizationMapper.selectByPrimaryKey(3)).thenReturn(org(3,2));
        when(organizationMapper.selectByPrimaryKey(4)).thenReturn(org(4,1));
        TUserRole assignment=new TUserRole();assignment.setRoleId(5);
        when(userRoleMapper.selectEffectiveByUserId(eq(1),any())).thenReturn(List.of(assignment));
        TRole sourceRole=new TRole();sourceRole.setId(5);sourceRole.setEnabled(1);sourceRole.setScopeType(RoleScopeType.GLOBAL);sourceRole.setAuthorizationLevel(10);
        when(roleMapper.selectByPrimaryKey(5)).thenReturn(sourceRole);
        TRolePermission source=new TRolePermission();source.setRoleId(5);source.setPermissionId(7);
        source.setDelegable(true);source.setDataScopeCode(DataScopeCode.ORG_TREE);
        when(rolePermissionMapper.selectByRoleId(5)).thenReturn(List.of(source));
        TPermission permission=new TPermission();permission.setId(7);permission.setEnabled(1);
        permission.setDelegable(true);permission.setSensitivityLevel(PermissionSensitivityLevel.NORMAL);

        assertEquals(List.of(2,3),policy.delegableCustomOrganizationIds(permission,target));
        assertTrue(policy.canDelegatePermission(permission,DataScopeCode.CUSTOM_ORGS,List.of(2,3),target));
        assertFalse(policy.canDelegatePermission(permission,DataScopeCode.CUSTOM_ORGS,List.of(4),target));
        assertFalse(policy.canDelegatePermission(permission,DataScopeCode.CUSTOM_ORGS,List.of(9),target));
        assertFalse(policy.canDelegatePermission(permission,DataScopeCode.GLOBAL,List.of(),target));
    }

    @Test void customOrganizationCoverageUsesRoleAndPersonalExplicitSources() {
        TUser operator=human(1),target=human(2);baseOperator(operator);
        when(employeeMapper.selectByUserId(1)).thenReturn(employee(10,1));
        when(employeeMapper.selectByUserId(2)).thenReturn(employee(20,2));
        when(assignmentMapper.selectCurrentPrimaryByEmployeeId(eq(10),any())).thenReturn(assignment(1));
        when(assignmentMapper.selectCurrentPrimaryByEmployeeId(eq(20),any())).thenReturn(assignment(2));
        when(organizationMapper.selectByPrimaryKey(1)).thenReturn(org(1,null));
        when(organizationMapper.selectByPrimaryKey(2)).thenReturn(org(2,1));
        when(organizationMapper.selectByPrimaryKey(3)).thenReturn(org(3,2));
        when(organizationMapper.selectByPrimaryKey(4)).thenReturn(org(4,1));

        TUserRole roleAssignment=new TUserRole();roleAssignment.setRoleId(5);
        when(userRoleMapper.selectEffectiveByUserId(eq(1),any())).thenReturn(List.of(roleAssignment));
        TRole role=new TRole();role.setId(5);role.setEnabled(1);role.setScopeType(RoleScopeType.GLOBAL);role.setAuthorizationLevel(10);
        when(roleMapper.selectByPrimaryKey(5)).thenReturn(role);
        TRolePermission rolePermission=new TRolePermission();rolePermission.setRoleId(5);rolePermission.setPermissionId(7);
        rolePermission.setDelegable(true);rolePermission.setDataScopeCode(DataScopeCode.CUSTOM_ORGS);
        when(rolePermissionMapper.selectByRoleId(5)).thenReturn(List.of(rolePermission));
        when(rolePermissionOrganizationMapper.selectOrganizationIds(5,7)).thenReturn(List.of(2,4));

        TUserPermission personal=new TUserPermission();personal.setId(99L);personal.setPermissionId(7);
        personal.setEffect(PermissionEffect.GRANT);personal.setDataScopeCode(DataScopeCode.CUSTOM_ORGS);
        when(userPermissionMapper.selectEffectiveByUserId(eq(1),any())).thenReturn(List.of(personal));
        when(userPermissionOrganizationMapper.selectOrganizationIds(99L)).thenReturn(List.of(3));

        TPermission permission=new TPermission();permission.setId(7);permission.setEnabled(1);
        permission.setDelegable(true);permission.setSensitivityLevel(PermissionSensitivityLevel.NORMAL);

        assertEquals(List.of(2,3),policy.delegableCustomOrganizationIds(permission,target));
        assertTrue(policy.canDelegatePermission(permission,DataScopeCode.CUSTOM_ORGS,List.of(2,3),target));
        assertFalse(policy.canDelegatePermission(permission,DataScopeCode.CUSTOM_ORGS,List.of(4),target));
        verify(rolePermissionOrganizationMapper,atLeastOnce()).selectOrganizationIds(5,7);
        verify(userPermissionOrganizationMapper,atLeastOnce()).selectOrganizationIds(99L);
    }

    private void baseOperator(TUser operator){when(currentUserProvider.getCurrentUserId()).thenReturn(operator.getId());lenient().when(currentUserProvider.getCurrentUser()).thenReturn(operator);}
    private TUser human(int id){TUser value=new TUser();value.setId(id);value.setAccountType(AccountType.HUMAN);value.setProtectedAccount(false);return value;}
    private TEmployee employee(int id,int userId){TEmployee value=new TEmployee();value.setId(id);value.setUserId(userId);return value;}
    private TEmployeeAssignment assignment(int orgId){TEmployeeAssignment value=new TEmployeeAssignment();value.setOrganizationUnitId(orgId);return value;}
    private TEmployeeReporting reporting(int subordinate,int manager){TEmployeeReporting value=new TEmployeeReporting();value.setSubordinateEmployeeId(subordinate);value.setManagerEmployeeId(manager);return value;}
    private TUserRole userRole(int roleId){TUserRole value=new TUserRole();value.setRoleId(roleId);return value;}
    private TOrganizationUnit org(int id,Integer parent){TOrganizationUnit value=new TOrganizationUnit();value.setId(id);value.setParentId(parent);value.setEnabled(true);value.setMigrationPlaceholder(false);return value;}
    private TRole protectedRole(String code){TRole value=new TRole();value.setRole(code);value.setEnabled(1);value.setProtectedRole(true);value.setScopeType(RoleScopeType.GLOBAL);value.setAuthorizationLevel(100);value.setDefaultDataScope(DataScopeCode.GLOBAL);return value;}
    private TRole role(int id,int level){TRole value=new TRole();value.setId(id);value.setRole("role_"+id);value.setEnabled(1);value.setProtectedRole(false);value.setScopeType(RoleScopeType.GLOBAL);value.setAuthorizationLevel(level);return value;}
}
