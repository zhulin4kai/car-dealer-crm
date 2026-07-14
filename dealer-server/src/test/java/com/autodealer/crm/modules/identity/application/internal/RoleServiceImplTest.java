package com.autodealer.crm.modules.identity.application.internal;

import com.autodealer.crm.modules.identity.application.api.enums.RoleScopeType;
import com.autodealer.crm.modules.identity.persistence.mapper.TAuthorizationGraphLockMapper;
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
import com.autodealer.crm.modules.identity.persistence.mapper.TUserRoleMapper;
import com.autodealer.crm.modules.identity.persistence.model.TRole;
import com.autodealer.crm.modules.identity.application.api.*;

import com.autodealer.crm.modules.identity.application.api.AuthorizationAuditRecorder;
import com.autodealer.crm.modules.identity.application.api.security.CurrentUserProvider;
import com.autodealer.crm.modules.identity.application.api.dto.access.RoleDtos.ChangeRoleStatusRequest;
import com.autodealer.crm.modules.identity.application.api.enums.*;
import com.autodealer.crm.shared.error.BusinessException;
import com.autodealer.crm.modules.identity.persistence.mapper.*;
import com.autodealer.crm.modules.identity.persistence.model.*;
import com.autodealer.crm.modules.identity.application.api.model.*;
import com.autodealer.crm.shared.error.CodeEnum;
import com.autodealer.crm.modules.identity.application.internal.RoleAccessServiceImpl;
import com.autodealer.crm.modules.identity.application.internal.UserSecurityMutationCoordinator;
import com.autodealer.crm.modules.identity.application.internal.UserAuthorizationPolicy;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoleServiceImplTest {
 @Mock TRoleMapper roles; @Mock TPermissionMapper permissions; @Mock TRolePermissionMapper matrices;
 @Mock TRoleOrganizationMapper roleOrgs; @Mock TRolePermissionOrganizationMapper rolePermissionOrgs; @Mock TOrganizationUnitMapper orgs; @Mock TUserRoleMapper userRoles;
 @Mock TUserMapper users; @Mock TEmployeeMapper employees; @Mock TEmployeeAssignmentMapper assignments;
 @Mock TEmployeeReportingMapper reporting; @Mock AuthorizationAuditRecorder audit; @Mock CurrentUserProvider current;
 @Mock TAuthorizationGraphLockMapper graphLock; @Mock UserAuthorizationPolicy authorizationPolicy; @Mock UserSecurityMutationCoordinator securityMutations; RoleAccessServiceImpl service;
 @BeforeEach void setup(){lenient().when(graphLock.lockByName(anyString())).thenAnswer(invocation->invocation.getArgument(0));service=new RoleAccessServiceImpl(roles,permissions,matrices,roleOrgs,rolePermissionOrgs,orgs,userRoles,users,employees,assignments,reporting,audit,current,new ObjectMapper(),graphLock,authorizationPolicy,securityMutations);}
 @Test void protectedRoleCannotBeDisabledEvenBySecurityAdmin(){TRole r=new TRole();r.setId(1);r.setRole("admin");r.setProtectedRole(true);r.setVersion(0);r.setScopeType(RoleScopeType.GLOBAL);when(roles.selectByPrimaryKey(1)).thenReturn(r);ChangeRoleStatusRequest q=new ChangeRoleStatusRequest();q.setExpectedVersion(0);q.setReason("no");BusinessException e=assertThrows(BusinessException.class,()->service.status(1,q,false));assertEquals(CodeEnum.PROTECTED_ROLE_FORBIDDEN,e.getCodeEnum());verify(roles,never()).updateMutableByIdAndVersion(any(),any());}
 @Test void staleRoleVersionIsRejectedBeforeMutation(){TRole r=new TRole();r.setId(2);r.setProtectedRole(false);r.setVersion(2);r.setScopeType(RoleScopeType.ORGANIZATION);r.setAuthorizationLevel(1);when(roles.selectByPrimaryKey(2)).thenReturn(r);when(authorizationPolicy.isGlobalOperator()).thenReturn(true);ChangeRoleStatusRequest q=new ChangeRoleStatusRequest();q.setExpectedVersion(1);q.setReason("stale");BusinessException e=assertThrows(BusinessException.class,()->service.status(2,q,false));assertEquals(CodeEnum.ROLE_VERSION_CONFLICT,e.getCodeEnum());}
 @Test void protectedRecoveryCannotUseRoleCatalogAsSecurityAdministrator(){TRole r=new TRole();r.setId(2);r.setProtectedRole(false);r.setVersion(0);r.setScopeType(RoleScopeType.GLOBAL);r.setAuthorizationLevel(10);when(roles.selectByPrimaryKey(2)).thenReturn(r);when(authorizationPolicy.isGlobalOperator()).thenReturn(false);ChangeRoleStatusRequest q=new ChangeRoleStatusRequest();q.setExpectedVersion(0);q.setReason("recovery denied");BusinessException e=assertThrows(BusinessException.class,()->service.status(2,q,false));assertEquals(CodeEnum.ACCESS_DENIED,e.getCodeEnum());}
 @Test void sharedRoleChangeCannotIndirectlyChangeOwnPermissions(){TRole r=new TRole();r.setId(2);r.setProtectedRole(false);r.setVersion(0);r.setEnabled(1);r.setScopeType(RoleScopeType.ORGANIZATION);r.setAuthorizationLevel(10);when(roles.selectByPrimaryKey(2)).thenReturn(r);when(authorizationPolicy.isGlobalOperator()).thenReturn(true);when(userRoles.selectCurrentAndFutureUserIdsByRoleId(eq(2),any())).thenReturn(java.util.List.of(7));when(current.getCurrentUserId()).thenReturn(7);ChangeRoleStatusRequest q=new ChangeRoleStatusRequest();q.setExpectedVersion(0);q.setReason("self denied");BusinessException e=assertThrows(BusinessException.class,()->service.status(2,q,false));assertEquals(CodeEnum.SELF_MANAGEMENT_FORBIDDEN,e.getCodeEnum());verify(roles,never()).updateMutableByIdAndVersion(any(),any());}
}
