package com.autodealer.crm.modules.identity.application.internal;

import com.autodealer.crm.modules.identity.application.api.CredentialService;
import com.autodealer.crm.modules.identity.application.api.model.TUser;
import com.autodealer.crm.modules.identity.persistence.mapper.TAuthorizationGraphLockMapper;
import com.autodealer.crm.modules.identity.persistence.mapper.TEmployeeAssignmentMapper;
import com.autodealer.crm.modules.identity.persistence.mapper.TEmployeeMapper;
import com.autodealer.crm.modules.identity.persistence.mapper.TEmployeeReportingMapper;
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
import com.autodealer.crm.modules.identity.persistence.model.TEmployee;
import com.autodealer.crm.modules.identity.persistence.model.TOrganizationUnit;
import com.autodealer.crm.modules.identity.persistence.model.TPermission;
import com.autodealer.crm.modules.identity.persistence.model.TRole;
import com.autodealer.crm.modules.identity.persistence.model.TRolePermission;
import com.autodealer.crm.modules.identity.persistence.model.TUserPermission;
import com.autodealer.crm.modules.identity.persistence.model.TUserRole;
import com.autodealer.crm.modules.identity.application.api.*;

import com.autodealer.crm.modules.audit.application.api.OperationAuditRecorder;
import com.autodealer.crm.modules.identity.application.api.security.CurrentUserProvider;
import com.autodealer.crm.modules.identity.application.api.dto.profile.ProfileDtos.Profile;
import com.autodealer.crm.modules.identity.application.api.dto.profile.ProfileDtos.PermissionSourceDetail;
import com.autodealer.crm.modules.identity.application.api.enums.AccountType;
import com.autodealer.crm.modules.identity.application.api.enums.DataScopeCode;
import com.autodealer.crm.modules.identity.application.api.enums.PermissionEffect;
import com.autodealer.crm.modules.identity.application.api.enums.EmployeeStatus;
import com.autodealer.crm.modules.identity.persistence.mapper.*;
import com.autodealer.crm.modules.identity.persistence.model.*;
import com.autodealer.crm.modules.identity.application.api.model.*;
import com.autodealer.crm.modules.identity.application.internal.ProfileServiceImpl;
import com.autodealer.crm.modules.identity.application.internal.UserSecurityMutationCoordinator;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ProfileAuthorizationSourceTest {
    @Test
    void keepsEachAuthorizationSourcePairedWithItsOwnDataScope() {
        CurrentUserProvider current=mock(CurrentUserProvider.class);TUserMapper users=mock(TUserMapper.class);
        TEmployeeMapper employees=mock(TEmployeeMapper.class);TEmployeeAssignmentMapper assignments=mock(TEmployeeAssignmentMapper.class);
        TEmployeeReportingMapper reporting=mock(TEmployeeReportingMapper.class);TOrganizationUnitMapper organizations=mock(TOrganizationUnitMapper.class);
        TPositionMapper positions=mock(TPositionMapper.class);TRoleMapper roles=mock(TRoleMapper.class);
        TUserRoleMapper userRoles=mock(TUserRoleMapper.class);TRolePermissionMapper rolePermissions=mock(TRolePermissionMapper.class);
        TRolePermissionOrganizationMapper rolePermissionOrganizations=mock(TRolePermissionOrganizationMapper.class);
        TUserPermissionMapper userPermissions=mock(TUserPermissionMapper.class);TUserPermissionOrganizationMapper userPermissionOrganizations=mock(TUserPermissionOrganizationMapper.class);
        TPermissionMapper permissions=mock(TPermissionMapper.class);OperationAuditRecorder audit=mock(OperationAuditRecorder.class);
        CredentialService credentials=mock(CredentialService.class);TAuthorizationGraphLockMapper graphLocks=mock(TAuthorizationGraphLockMapper.class);
        ProfileServiceImpl service=new ProfileServiceImpl(current,users,employees,assignments,reporting,organizations,positions,roles,userRoles,rolePermissions,rolePermissionOrganizations,userPermissions,userPermissionOrganizations,permissions,audit,credentials,graphLocks,mock(UserSecurityMutationCoordinator.class));

        when(current.getCurrentUserId()).thenReturn(7);
        TUser user=new TUser();user.setId(7);user.setLoginAct("sales07");user.setName("销售七");user.setAccountType(AccountType.HUMAN);when(users.selectByPrimaryKey(7)).thenReturn(user);
        TEmployee employee=new TEmployee();employee.setId(70);employee.setUserId(7);employee.setName("销售七");employee.setEmployeeNo("E007");employee.setEmploymentStatus(EmployeeStatus.ACTIVE);employee.setProfileVersion(1);when(employees.selectByUserId(7)).thenReturn(employee);
        when(assignments.selectCurrentPrimaryByEmployeeId(eq(70),any())).thenReturn(null);when(reporting.selectCurrentDirectBySubordinateId(eq(70),any())).thenReturn(null);

        LocalDateTime salesFrom=LocalDateTime.of(2026,1,1,9,0);LocalDateTime managerFrom=LocalDateTime.of(2026,2,1,9,0);LocalDateTime managerTo=LocalDateTime.of(2026,12,31,23,59);
        TRole sales=role(1,"销售角色");TRole manager=role(2,"主管角色");when(roles.selectByUserId(7)).thenReturn(List.of(sales,manager));
        when(userRoles.selectEffectiveByUserId(eq(7),any())).thenReturn(List.of(userRole(1,salesFrom,null),userRole(2,managerFrom,managerTo)));
        when(rolePermissions.selectByRoleId(1)).thenReturn(List.of(rolePermission(99,DataScopeCode.SELF)));
        when(rolePermissions.selectByRoleId(2)).thenReturn(List.of(rolePermission(99,DataScopeCode.CUSTOM_ORGS)));
        when(rolePermissionOrganizations.selectOrganizationIds(2,99)).thenReturn(List.of(10,11));
        TUserPermission personal=new TUserPermission();personal.setId(501L);personal.setPermissionId(99);personal.setEffect(PermissionEffect.GRANT);personal.setDataScopeCode(DataScopeCode.CUSTOM_ORGS);personal.setEffectiveFrom(LocalDateTime.of(2026,3,1,9,0));personal.setEffectiveTo(LocalDateTime.of(2026,11,30,23,59));when(userPermissions.selectEffectiveByUserId(eq(7),any())).thenReturn(List.of(personal));
        when(userPermissionOrganizations.selectOrganizationIds(501L)).thenReturn(List.of(12));
        when(organizations.selectByIds(List.of(10,11))).thenReturn(List.of(organization(10,"STORE_SH","上海门店"),organization(11,"STORE_HZ","杭州门店")));
        when(organizations.selectByIds(List.of(12))).thenReturn(List.of(organization(12,"TEAM_EAST","华东团队")));
        TPermission permission=new TPermission();permission.setId(99);permission.setCode("customer:view");permission.setName("查看客户");when(permissions.selectMenuPermissionByUserId(7)).thenReturn(List.of());when(permissions.selectButtonPermissionByUserId(7)).thenReturn(List.of(permission));

        Profile profile=service.getOwn();
        List<PermissionSourceDetail> actual=profile.getEffectivePermissions().get(0).getSources();
        assertEquals(3,actual.size());
        PermissionSourceDetail salesSource=actual.stream().filter(value->value.getSourceName().equals("销售角色")).findFirst().orElseThrow();
        assertEquals("SELF",salesSource.getDataScopeCode());assertEquals("本人",salesSource.getDataScopeLabel());assertEquals(salesFrom,salesSource.getEffectiveFrom());assertNull(salesSource.getEffectiveTo());assertTrue(salesSource.getOrganizations().isEmpty());
        PermissionSourceDetail managerSource=actual.stream().filter(value->value.getSourceName().equals("主管角色")).findFirst().orElseThrow();
        assertEquals("CUSTOM_ORGS",managerSource.getDataScopeCode());assertEquals(managerFrom,managerSource.getEffectiveFrom());assertEquals(managerTo,managerSource.getEffectiveTo());assertEquals(List.of(10,11),managerSource.getOrganizations().stream().map(value->value.getId()).toList());assertEquals(List.of("STORE_SH","STORE_HZ"),managerSource.getOrganizations().stream().map(value->value.getCode()).toList());assertEquals(List.of("上海门店","杭州门店"),managerSource.getOrganizations().stream().map(value->value.getName()).toList());
        PermissionSourceDetail personalSource=actual.stream().filter(value->value.getSourceType().equals("PERSONAL_GRANT")).findFirst().orElseThrow();
        assertEquals("CUSTOM_ORGS",personalSource.getDataScopeCode());assertEquals(LocalDateTime.of(2026,3,1,9,0),personalSource.getEffectiveFrom());assertEquals(LocalDateTime.of(2026,11,30,23,59),personalSource.getEffectiveTo());assertEquals(12,personalSource.getOrganizations().get(0).getId());assertEquals("TEAM_EAST",personalSource.getOrganizations().get(0).getCode());assertEquals("华东团队",personalSource.getOrganizations().get(0).getName());
    }

    private static TRole role(int id,String name){TRole role=new TRole();role.setId(id);role.setRole("role_"+id);role.setRoleName(name);return role;}
    private static TUserRole userRole(int roleId,LocalDateTime effectiveFrom,LocalDateTime effectiveTo){TUserRole value=new TUserRole();value.setRoleId(roleId);value.setEffectiveFrom(effectiveFrom);value.setEffectiveTo(effectiveTo);return value;}
    private static TRolePermission rolePermission(int permissionId,DataScopeCode scope){TRolePermission value=new TRolePermission();value.setPermissionId(permissionId);value.setDataScopeCode(scope);return value;}
    private static TOrganizationUnit organization(int id,String code,String name){TOrganizationUnit value=new TOrganizationUnit();value.setId(id);value.setCode(code);value.setName(name);return value;}
}
