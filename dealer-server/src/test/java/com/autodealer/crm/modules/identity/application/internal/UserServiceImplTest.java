package com.autodealer.crm.modules.identity.application.internal;

import com.autodealer.crm.modules.identity.application.api.DataScopeResolver;
import com.autodealer.crm.modules.identity.application.api.UserService;
import com.autodealer.crm.modules.identity.application.api.dto.AssignUserRolesRequest;
import com.autodealer.crm.modules.identity.application.api.dto.ChangePasswordRequest;
import com.autodealer.crm.modules.identity.application.api.dto.CreateUserRequest;
import com.autodealer.crm.modules.identity.application.api.dto.HandoverUserResponsibilitiesRequest;
import com.autodealer.crm.modules.identity.application.api.dto.UpdateUserRequest;
import com.autodealer.crm.modules.identity.application.api.dto.UserDetailResponse;
import com.autodealer.crm.modules.identity.application.api.dto.UserListQuery;
import com.autodealer.crm.modules.identity.persistence.mapper.TPermissionMapper;
import com.autodealer.crm.modules.identity.persistence.mapper.TRoleMapper;
import com.autodealer.crm.modules.identity.persistence.mapper.TUserMapper;
import com.autodealer.crm.modules.identity.application.api.*;

import com.autodealer.crm.modules.identity.application.api.security.CurrentUserProvider;
import com.autodealer.crm.modules.identity.application.api.security.PrincipalEligibilityPolicy;
import com.autodealer.crm.modules.identity.application.api.dto.*;
import com.autodealer.crm.shared.error.BusinessException;
import com.autodealer.crm.shared.infrastructure.cache.RedisManager;
import com.autodealer.crm.modules.identity.persistence.mapper.*;
import com.autodealer.crm.modules.identity.application.api.model.TUser;
import com.autodealer.crm.shared.error.CodeEnum;
import com.autodealer.crm.modules.identity.application.internal.UserServiceImpl;
import com.github.pagehelper.PageInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** 旧 UserService 只保留读取与认证失效；所有用户资料、状态、授权、密码和交接写入必须 fail-close。 */
@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {
    @Mock TUserMapper users; @Mock TRoleMapper roles; @Mock TPermissionMapper permissions;
    @Mock RedisManager redis; @Mock CurrentUserProvider current;
    @Mock DataScopeResolver dataScopes; @Mock PrincipalEligibilityPolicy principalEligibility;
    @Mock com.autodealer.crm.modules.identity.application.internal.UserSecurityMutationCoordinator securityMutations;
    @InjectMocks UserServiceImpl service;

    @BeforeEach void setUp(){lenient().when(current.getDataScopeUserId()).thenReturn(null);lenient().when(principalEligibility.isEligible(any())).thenReturn(true);}

    @Test void pageReadStillWorks(){TUser user=new TUser();user.setId(2);when(users.selectUserByPage(any())).thenReturn(List.of(user));
        PageInfo<UserDetailResponse> page=service.getUserByPage(new UserListQuery());assertEquals(1,page.getList().size());}

    @Test void authenticationLookupStillWorksAndMissingUserFails(){TUser user=new TUser();user.setId(2);when(users.selectByLoginAct("user")).thenReturn(user);
        assertSame(user,service.loadUserByUsername("user"));when(users.selectByLoginAct("missing")).thenReturn(null);
        assertThrows(UsernameNotFoundException.class,()->service.loadUserByUsername("missing"));}

    @Test void ineligiblePrincipalFailsBeforeRoleOrPermissionLoadingForLoginAndTokenRefresh(){TUser user=new TUser();user.setId(2);
        when(users.selectByLoginAct("ineligible")).thenReturn(user);when(users.selectByPrimaryKey(2)).thenReturn(user);
        when(principalEligibility.isEligible(user)).thenReturn(false);
        assertThrows(UsernameNotFoundException.class,()->service.loadUserByUsername("ineligible"));
        assertNull(service.getLoginUserById(2));verifyNoInteractions(roles,permissions);}

    @Test void detailReadStillWorks(){TUser user=new TUser();user.setId(2);when(users.selectAuthUserById(2)).thenReturn(user);assertNotNull(service.getUserById(2));}

    @Test void everyLegacyWriteEntryFailsClosedBeforeMapperAuditOrSessionSideEffects(){
        CreateUserRequest create=new CreateUserRequest();UpdateUserRequest update=new UpdateUserRequest();update.setId(2);
        AssignUserRolesRequest roleRequest=new AssignUserRolesRequest();roleRequest.setUserId(2);roleRequest.setRoleIds(List.of());
        ChangePasswordRequest password=new ChangePasswordRequest();password.setUserId(2);
        HandoverUserResponsibilitiesRequest handover=new HandoverUserResponsibilitiesRequest();handover.setTargetUserId(3);

        assertDenied(()->service.createUser(create));assertDenied(()->service.updateUser(update));
        assertDenied(()->service.disableUser(2));assertDenied(()->service.enableUser(2));
        assertDenied(()->service.lockUser(2));assertDenied(()->service.unlockUser(2));
        assertDenied(()->service.batchDisableUsers(List.of(2,3)));assertDenied(()->service.assignRoles(roleRequest));
        assertDenied(()->service.changePassword(password));assertDenied(()->service.handoverResponsibilities(2,handover));

        verifyNoInteractions(roles,permissions,dataScopes,securityMutations);
        verifyNoMoreInteractions(users,redis,current);
    }

    @Test void revokeAuthenticationDatabaseFailureRejectsAndSuccessDelegatesCleanup(){
        when(users.incrementAuthVersion(2)).thenReturn(0);BusinessException failure=assertThrows(BusinessException.class,()->service.revokeAuthentication(2));
        assertEquals(CodeEnum.SYSTEM_ERROR,failure.getCodeEnum());verifyNoInteractions(securityMutations);
        reset(users,securityMutations);when(users.incrementAuthVersion(2)).thenReturn(1);
        assertDoesNotThrow(()->service.revokeAuthentication(2));
        verify(securityMutations).authenticationChanged(2,"账号安全状态变化");
    }

    private void assertDenied(org.junit.jupiter.api.function.Executable action){BusinessException error=assertThrows(BusinessException.class,action);assertEquals(CodeEnum.ACCESS_DENIED,error.getCodeEnum());}
}
