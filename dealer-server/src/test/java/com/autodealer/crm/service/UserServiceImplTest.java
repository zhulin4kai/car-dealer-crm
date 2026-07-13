package com.autodealer.crm.service;

import com.autodealer.crm.audit.OperationAuditRecorder;
import com.autodealer.crm.config.security.CurrentUserProvider;
import com.autodealer.crm.config.security.PrincipalEligibilityPolicy;
import com.autodealer.crm.constant.RedisKeys;
import com.autodealer.crm.dto.*;
import com.autodealer.crm.exception.BusinessException;
import com.autodealer.crm.manager.RedisManager;
import com.autodealer.crm.mapper.*;
import com.autodealer.crm.model.TUser;
import com.autodealer.crm.result.CodeEnum;
import com.autodealer.crm.service.impl.UserServiceImpl;
import com.github.pagehelper.PageInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** 旧 UserService 只保留读取与认证失效；所有用户资料、状态、授权、密码和交接写入必须 fail-close。 */
@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {
    @Mock TUserMapper users; @Mock TClueOwnerHistoryMapper clueHistory; @Mock TCustomerOwnerHistoryMapper customerHistory;
    @Mock PasswordEncoder passwords; @Mock TRoleMapper roles; @Mock TPermissionMapper permissions;
    @Mock RedisManager redis; @Mock CurrentUserProvider current; @Mock OperationAuditRecorder audit;
    @Mock DataScopeResolver dataScopes; @Mock PrincipalEligibilityPolicy principalEligibility;
    @Mock com.autodealer.crm.config.security.OwnerCandidateCacheInvalidator ownerCandidateCacheInvalidator;
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

    @Test void ownerListCacheHitAndMissRemainReadOnly(){TUser owner=new TUser();owner.setId(2);when(redis.get(RedisKeys.ownerList())).thenReturn(List.of(owner));
        assertEquals(List.of(owner),service.getOwnerList());verify(users,never()).selectByOwner();
        reset(redis,users);when(redis.get(RedisKeys.ownerList())).thenReturn(null);when(users.selectByOwner()).thenReturn(List.of(owner));
        assertEquals(List.of(owner),service.getOwnerList());verify(redis).set(RedisKeys.ownerList(),List.of(owner),300L);}

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

        verifyNoInteractions(clueHistory,customerHistory,passwords,roles,permissions,audit,dataScopes);
        verifyNoMoreInteractions(users,redis,current);
    }

    @Test void revokeAuthenticationDatabaseFailureRejectsAndSuccessKeepsSecurityVersionEvenIfCacheCleanupFails(){
        when(users.incrementAuthVersion(2)).thenReturn(0);BusinessException failure=assertThrows(BusinessException.class,()->service.revokeAuthentication(2));
        assertEquals(CodeEnum.SYSTEM_ERROR,failure.getCodeEnum());verifyNoInteractions(redis);
        reset(users,redis);when(users.incrementAuthVersion(2)).thenReturn(1);when(redis.delete(RedisKeys.userLogin(2))).thenReturn(false);
        assertDoesNotThrow(()->service.revokeAuthentication(2));verify(redis,times(2)).delete(RedisKeys.userLogin(2));
    }

    private void assertDenied(org.junit.jupiter.api.function.Executable action){BusinessException error=assertThrows(BusinessException.class,action);assertEquals(CodeEnum.ACCESS_DENIED,error.getCodeEnum());}
}
