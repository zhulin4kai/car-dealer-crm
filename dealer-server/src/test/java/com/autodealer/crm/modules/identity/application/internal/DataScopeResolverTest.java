package com.autodealer.crm.modules.identity.application.internal;

import com.autodealer.crm.modules.identity.application.api.enums.DataScopeCode;
import com.autodealer.crm.modules.identity.application.api.enums.PermissionEffect;
import com.autodealer.crm.modules.identity.application.api.enums.RoleScopeType;
import com.autodealer.crm.modules.identity.persistence.mapper.TEmployeeAssignmentMapper;
import com.autodealer.crm.modules.identity.persistence.mapper.TEmployeeMapper;
import com.autodealer.crm.modules.identity.persistence.mapper.TEmployeeReportingMapper;
import com.autodealer.crm.modules.identity.persistence.mapper.TOrganizationUnitMapper;
import com.autodealer.crm.modules.identity.persistence.mapper.TPermissionMapper;
import com.autodealer.crm.modules.identity.persistence.mapper.TRoleMapper;
import com.autodealer.crm.modules.identity.persistence.mapper.TRoleOrganizationMapper;
import com.autodealer.crm.modules.identity.persistence.mapper.TRolePermissionMapper;
import com.autodealer.crm.modules.identity.persistence.mapper.TRolePermissionOrganizationMapper;
import com.autodealer.crm.modules.identity.persistence.mapper.TUserPermissionMapper;
import com.autodealer.crm.modules.identity.persistence.mapper.TUserPermissionOrganizationMapper;
import com.autodealer.crm.modules.identity.persistence.mapper.TUserRoleMapper;
import com.autodealer.crm.modules.identity.persistence.model.TEmployee;
import com.autodealer.crm.modules.identity.persistence.model.TEmployeeAssignment;
import com.autodealer.crm.modules.identity.persistence.model.TEmployeeReporting;
import com.autodealer.crm.modules.identity.persistence.model.TPermission;
import com.autodealer.crm.modules.identity.persistence.model.TRole;
import com.autodealer.crm.modules.identity.persistence.model.TRolePermission;
import com.autodealer.crm.modules.identity.persistence.model.TUserPermission;
import com.autodealer.crm.modules.identity.persistence.model.TUserRole;
import com.autodealer.crm.modules.identity.application.api.*;

import com.autodealer.crm.modules.identity.application.api.enums.*;
import com.autodealer.crm.modules.identity.persistence.mapper.*;
import com.autodealer.crm.modules.identity.persistence.model.*;
import com.autodealer.crm.modules.identity.application.api.model.*;
import com.autodealer.crm.modules.identity.application.api.AuthorizationDataScope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DataScopeResolverTest {
    @Mock TPermissionMapper permissionMapper; @Mock TUserPermissionMapper userPermissionMapper;
    @Mock TUserRoleMapper userRoleMapper; @Mock TRoleMapper roleMapper;
    @Mock TRolePermissionMapper rolePermissionMapper; @Mock TRoleOrganizationMapper roleOrganizationMapper;
    @Mock TRolePermissionOrganizationMapper rolePermissionOrganizationMapper;
    @Mock TUserPermissionOrganizationMapper userPermissionOrganizationMapper;
    @Mock TEmployeeMapper employeeMapper; @Mock TEmployeeAssignmentMapper assignmentMapper;
    @Mock TEmployeeReportingMapper reportingMapper; @Mock TOrganizationUnitMapper organizationMapper;
    @InjectMocks DataScopeResolverImpl resolver;

    @BeforeEach void permission(){TPermission permission=new TPermission();permission.setId(10);permission.setCode("user:list");permission.setEnabled(1);lenient().when(permissionMapper.selectByCode("user:list")).thenReturn(permission);}

    @Test void personalDenyOverridesEveryRoleSource(){
        TUserPermission deny=new TUserPermission();deny.setEffect(PermissionEffect.DENY);
        when(userPermissionMapper.selectCurrentEffective(eq(1),eq(10),any())).thenReturn(deny);
        AuthorizationDataScope scope=resolver.resolve(1,"user:list");
        assertFalse(scope.global());assertTrue(scope.visibleUserIds().isEmpty());verify(userRoleMapper,never()).selectEffectiveByUserId(anyInt(),any());
    }

    @Test void directAndReportingTreeUseOnlyEffectiveReportingFacts(){
        configureRoleScope(DataScopeCode.REPORTING_TREE);TEmployee manager=employee(100,1),direct=employee(200,2),indirect=employee(300,3);
        when(employeeMapper.selectByUserId(1)).thenReturn(manager);when(employeeMapper.selectByPrimaryKey(200)).thenReturn(direct);when(employeeMapper.selectByPrimaryKey(300)).thenReturn(indirect);
        TEmployeeReporting first=reporting(200),second=reporting(300);
        when(reportingMapper.selectEffectiveSubordinates(eq(100),any())).thenReturn(List.of(first));
        when(reportingMapper.selectEffectiveSubordinates(eq(200),any())).thenReturn(List.of(second));
        when(reportingMapper.selectEffectiveSubordinates(eq(300),any())).thenReturn(List.of());
        AuthorizationDataScope scope=resolver.resolve(1,"user:list");
        assertEquals(java.util.Set.of(2,3),scope.visibleUserIds());
    }

    @Test void organizationTreeIsExpandedByRecursiveSqlBeforeUserQuery(){
        configureRoleScope(DataScopeCode.ORG_TREE);TEmployee manager=employee(100,1);when(employeeMapper.selectByUserId(1)).thenReturn(manager);
        TEmployeeAssignment primary=new TEmployeeAssignment();primary.setOrganizationUnitId(5);when(assignmentMapper.selectCurrentPrimaryByEmployeeId(eq(100),any())).thenReturn(primary);
        when(organizationMapper.selectDescendantIds(5)).thenReturn(List.of(5,6,7));
        when(employeeMapper.selectUserIdsByOrganizationUnitIds(eq(List.of(5,6,7)),any())).thenReturn(List.of(1,2,3));
        AuthorizationDataScope scope=resolver.resolve(1,"user:list");
        assertEquals(java.util.Set.of(5,6,7),scope.visibleOrganizationIds());assertEquals(java.util.Set.of(1,2,3),scope.visibleUserIds());
        verify(organizationMapper,never()).selectAll();
    }

    @Test void personalCustomOrganizationsComeFromDedicatedSourceTable(){
        TUserPermission grant=new TUserPermission();grant.setId(99L);grant.setEffect(PermissionEffect.GRANT);grant.setDataScopeCode(DataScopeCode.CUSTOM_ORGS);
        when(userPermissionMapper.selectCurrentEffective(eq(1),eq(10),any())).thenReturn(grant);when(userRoleMapper.selectEffectiveByUserId(eq(1),any())).thenReturn(List.of());
        when(userPermissionOrganizationMapper.selectOrganizationIds(99L)).thenReturn(List.of(7,8));
        when(employeeMapper.selectByUserId(1)).thenReturn(employee(100,1));when(employeeMapper.selectUserIdsByOrganizationUnitIds(eq(List.of(7,8)),any())).thenReturn(List.of(2,3));
        AuthorizationDataScope scope=resolver.resolve(1,"user:list");
        assertEquals(java.util.Set.of(7,8),scope.visibleOrganizationIds());assertEquals(java.util.Set.of(2,3),scope.visibleUserIds());
        verify(roleOrganizationMapper,never()).selectByRoleId(anyInt());
    }

    private void configureRoleScope(DataScopeCode scope){TUserRole assignment=new TUserRole();assignment.setRoleId(1);when(userRoleMapper.selectEffectiveByUserId(eq(1),any())).thenReturn(List.of(assignment));TRole role=new TRole();role.setId(1);role.setEnabled(1);role.setScopeType(RoleScopeType.GLOBAL);when(roleMapper.selectByPrimaryKey(1)).thenReturn(role);TRolePermission permission=new TRolePermission();permission.setRoleId(1);permission.setPermissionId(10);permission.setDataScopeCode(scope);when(rolePermissionMapper.selectByRoleId(1)).thenReturn(List.of(permission));}
    private TEmployee employee(int id,int userId){TEmployee value=new TEmployee();value.setId(id);value.setUserId(userId);return value;}
    private TEmployeeReporting reporting(int subordinateId){TEmployeeReporting value=new TEmployeeReporting();value.setSubordinateEmployeeId(subordinateId);return value;}
}
