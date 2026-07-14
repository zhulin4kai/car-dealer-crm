package com.autodealer.crm.service;

import com.autodealer.crm.audit.AuthorizationAuditRecorder;
import com.autodealer.crm.config.security.CurrentUserProvider;
import com.autodealer.crm.dto.access.RoleDtos.ChangeRoleStatusRequest;
import com.autodealer.crm.enums.*;
import com.autodealer.crm.exception.BusinessException;
import com.autodealer.crm.mapper.*;
import com.autodealer.crm.model.*;
import com.autodealer.crm.result.CodeEnum;
import com.autodealer.crm.service.impl.RoleAccessServiceImpl;
import com.autodealer.crm.service.impl.UserSecurityMutationCoordinator;
import com.autodealer.crm.service.impl.UserAuthorizationPolicy;
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
