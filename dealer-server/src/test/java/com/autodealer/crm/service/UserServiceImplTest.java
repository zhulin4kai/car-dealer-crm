package com.autodealer.crm.service;

import com.autodealer.crm.config.security.CurrentUserProvider;
import com.autodealer.crm.constant.RedisKeys;
import com.autodealer.crm.dto.*;
import com.autodealer.crm.exception.BusinessException;
import com.autodealer.crm.manager.RedisManager;
import com.autodealer.crm.mapper.TClueOwnerHistoryMapper;
import com.autodealer.crm.mapper.TCustomerOwnerHistoryMapper;
import com.autodealer.crm.mapper.TPermissionMapper;
import com.autodealer.crm.mapper.TRoleMapper;
import com.autodealer.crm.mapper.TUserMapper;
import com.autodealer.crm.model.TClueOwnerHistory;
import com.autodealer.crm.model.TCustomerOwnerHistory;
import com.autodealer.crm.model.TRole;
import com.autodealer.crm.model.TUser;
import com.autodealer.crm.result.CodeEnum;
import com.autodealer.crm.audit.AuditActionEnum;
import com.autodealer.crm.audit.OperationAuditRecorder;
import com.autodealer.crm.service.impl.UserServiceImpl;
import com.github.pagehelper.PageInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock private TUserMapper tUserMapper;
    @Mock private TClueOwnerHistoryMapper clueOwnerHistoryMapper;
    @Mock private TCustomerOwnerHistoryMapper customerOwnerHistoryMapper;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private TRoleMapper tRoleMapper;
    @Mock private TPermissionMapper tPermissionMapper;
    @Mock private RedisManager redisManager;
    @Mock private CurrentUserProvider currentUserProvider;
    @Mock private OperationAuditRecorder auditRecorder;
    @InjectMocks private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        lenient().when(currentUserProvider.getDataScopeUserId()).thenReturn(null);
    }

    @Test
    void testGetUserByPage() {
        TUser user = new TUser(); user.setId(1); user.setLoginAct("admin"); user.setName("Admin");
        when(tUserMapper.selectUserByPage(any(UserListQuery.class))).thenReturn(Collections.singletonList(user));
        PageInfo<UserDetailResponse> result = userService.getUserByPage(new UserListQuery());
        assertNotNull(result); assertEquals(1, result.getList().size());
    }

    @Test
    void testLoadUserByUsernameFound() {
        TUser user = new TUser(); user.setId(1); user.setLoginAct("admin");
        when(tUserMapper.selectByLoginAct("admin")).thenReturn(user);
        assertNotNull(userService.loadUserByUsername("admin"));
    }

    @Test
    void testLoadUserByUsernameNotFound() {
        when(tUserMapper.selectByLoginAct("nonexistent")).thenReturn(null);
        assertThrows(UsernameNotFoundException.class, () -> userService.loadUserByUsername("nonexistent"));
    }

    @Test
    void testGetUserById() {
        TUser user = new TUser(); user.setId(1); user.setLoginAct("admin");
        when(tUserMapper.selectAuthUserById(1)).thenReturn(user);
        assertNotNull(userService.getUserById(1));
    }

    @Test
    void testCreateUser_success() {
        CreateUserRequest r = new CreateUserRequest();
        r.setLoginAct("newuser"); r.setLoginPwd("password123"); r.setName("New User");
        r.setPhone("13800138000"); r.setEmail("new@test.com");
        when(tUserMapper.selectByLoginAct("newuser")).thenReturn(null);
        when(tUserMapper.selectByPhone("13800138000")).thenReturn(null);
        when(tUserMapper.selectByEmail("new@test.com")).thenReturn(null);
        when(currentUserProvider.getCurrentUserId()).thenReturn(10);
        when(passwordEncoder.encode("password123")).thenReturn("encoded");
        when(tUserMapper.insertSelective(any())).thenReturn(1);
        when(redisManager.delete(anyString())).thenReturn(true);
        assertNotNull(userService.createUser(r));
    }

    @Test
    void testCreateUser_duplicateLoginAct() {
        CreateUserRequest r = new CreateUserRequest();
        r.setLoginAct("existing"); r.setLoginPwd("password123"); r.setName("U");
        r.setPhone("13800138000"); r.setEmail("t@t.com");
        when(tUserMapper.selectByLoginAct("existing")).thenReturn(new TUser());
        BusinessException ex = assertThrows(BusinessException.class, () -> userService.createUser(r));
        assertEquals(CodeEnum.FAIL, ex.getCodeEnum());
    }

    @Test
    void testCreateUser_weakPassword() {
        CreateUserRequest r = new CreateUserRequest();
        r.setLoginAct("new"); r.setLoginPwd("12345"); r.setName("U"); r.setPhone("13800138000"); r.setEmail("t@t.com");
        BusinessException ex = assertThrows(BusinessException.class, () -> userService.createUser(r));
        assertEquals(CodeEnum.PARAM_ERROR, ex.getCodeEnum());
    }

    @Test
    void testUpdateUser_success() {
        UpdateUserRequest r = new UpdateUserRequest();
        r.setId(2); r.setLoginAct("upd"); r.setName("Updated"); r.setPhone("13900139000"); r.setEmail("upd@t.com");
        TUser existing = new TUser(); existing.setId(2);
        when(tUserMapper.selectByPrimaryKey(2)).thenReturn(existing);
        when(tUserMapper.selectByLoginActExcludeId("upd", 2)).thenReturn(null);
        when(tUserMapper.selectByPhoneExcludeId("13900139000", 2)).thenReturn(null);
        when(tUserMapper.selectByEmailExcludeId("upd@t.com", 2)).thenReturn(null);
        when(currentUserProvider.getCurrentUserId()).thenReturn(10);
        when(tUserMapper.updateByPrimaryKeySelective(any())).thenReturn(1);
        when(redisManager.delete(anyString())).thenReturn(true);
        TUser updated = new TUser(); updated.setId(2);
        when(tUserMapper.selectAuthUserById(2)).thenReturn(updated);
        assertNotNull(userService.updateUser(r));
    }

    @Test
    void testDisableUser_success() {
        TUser user = new TUser(); user.setId(2); user.setAccountEnabled(1);
        when(tUserMapper.selectByPrimaryKey(2)).thenReturn(user);
        when(tUserMapper.countBusinessReferences(2)).thenReturn(0);
        when(tUserMapper.disableById(2)).thenReturn(1);
        when(redisManager.delete(anyString())).thenReturn(true);
        userService.disableUser(2);
        verify(tUserMapper).disableById(2);
    }

    @Test
    void disableUser_sessionInvalidationFailure_shouldRejectOperation() {
        TUser user = new TUser(); user.setId(2); user.setAccountEnabled(1);
        when(tUserMapper.selectByPrimaryKey(2)).thenReturn(user);
        when(tUserMapper.countBusinessReferences(2)).thenReturn(0);
        when(tUserMapper.disableById(2)).thenReturn(1);
        when(redisManager.delete(RedisKeys.userLogin(2))).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class, () -> userService.disableUser(2));

        assertEquals(CodeEnum.SYSTEM_ERROR, ex.getCodeEnum());
        verify(tUserMapper).disableById(2);
        verify(auditRecorder, never()).record(any(), anyString());
    }

    @Test
    void testDisableUser_selfOperation() {
        when(currentUserProvider.getCurrentUserId()).thenReturn(2);
        BusinessException ex = assertThrows(BusinessException.class, () -> userService.disableUser(2));
        assertEquals(CodeEnum.ACCESS_DENIED, ex.getCodeEnum());
    }

    @Test
    void testDisableUser_builtinAdmin() {
        BusinessException ex = assertThrows(BusinessException.class, () -> userService.disableUser(1));
        assertEquals(CodeEnum.FAIL, ex.getCodeEnum());
    }

    @Test
    void testDisableUser_businessReferences() {
        TUser user = new TUser(); user.setId(3); user.setAccountEnabled(1);
        when(tUserMapper.selectByPrimaryKey(3)).thenReturn(user);
        when(tUserMapper.countBusinessReferences(3)).thenReturn(5);
        BusinessException ex = assertThrows(BusinessException.class, () -> userService.disableUser(3));
        assertEquals(CodeEnum.FAIL, ex.getCodeEnum());
    }

    @Test
    void testEnableUser() {
        TUser user = new TUser(); user.setId(2);
        when(tUserMapper.selectByPrimaryKey(2)).thenReturn(user);
        when(tUserMapper.enableById(2)).thenReturn(1);
        when(redisManager.delete(anyString())).thenReturn(true);
        userService.enableUser(2);
        verify(tUserMapper).enableById(2);
    }

    @Test
    void enableUser_ownerCacheClearFailure_shouldRetryInvalidation() {
        TUser user = new TUser(); user.setId(2);
        when(tUserMapper.selectByPrimaryKey(2)).thenReturn(user);
        when(tUserMapper.enableById(2)).thenReturn(1);
        when(redisManager.delete(RedisKeys.ownerList())).thenReturn(false, true);

        userService.enableUser(2);

        verify(redisManager, times(2)).delete(RedisKeys.ownerList());
        verify(auditRecorder).record(AuditActionEnum.USER_STATUS_CHANGE, "2");
    }

    @Test
    void testLockUser() {
        TUser user = new TUser(); user.setId(2);
        when(tUserMapper.selectByPrimaryKey(2)).thenReturn(user);
        when(tUserMapper.lockById(2)).thenReturn(1);
        when(redisManager.delete(anyString())).thenReturn(true);
        userService.lockUser(2);
        verify(tUserMapper).lockById(2);
        verify(auditRecorder).record(AuditActionEnum.USER_STATUS_CHANGE, "2");
    }

    @Test
    void lockUser_sessionInvalidationException_shouldRejectOperation() {
        TUser user = new TUser(); user.setId(2);
        when(tUserMapper.selectByPrimaryKey(2)).thenReturn(user);
        when(tUserMapper.lockById(2)).thenReturn(1);
        when(redisManager.delete(RedisKeys.userLogin(2))).thenThrow(new IllegalStateException("redis unavailable"));

        BusinessException ex = assertThrows(BusinessException.class, () -> userService.lockUser(2));

        assertEquals(CodeEnum.SYSTEM_ERROR, ex.getCodeEnum());
        verify(tUserMapper).lockById(2);
        verify(auditRecorder, never()).record(any(), anyString());
    }

    @Test
    void lockUser_lastAdmin_shouldRejectBeforeUpdate() {
        TUser user = new TUser(); user.setId(2);
        when(tUserMapper.selectByPrimaryKey(2)).thenReturn(user);
        when(tRoleMapper.selectByUserId(2)).thenReturn(List.of(adminRole()));
        when(tUserMapper.countAdminUsers()).thenReturn(1);

        BusinessException ex = assertThrows(BusinessException.class, () -> userService.lockUser(2));

        assertEquals(CodeEnum.ACCESS_DENIED, ex.getCodeEnum());
        verify(tUserMapper, never()).lockById(anyInt());
    }

    @Test
    void testUnlockUser() {
        TUser user = new TUser(); user.setId(2);
        when(tUserMapper.selectByPrimaryKey(2)).thenReturn(user);
        when(tUserMapper.unlockById(2)).thenReturn(1);
        when(redisManager.delete(anyString())).thenReturn(true);
        userService.unlockUser(2);
        verify(tUserMapper).unlockById(2);
        verify(auditRecorder).record(AuditActionEnum.USER_STATUS_CHANGE, "2");
    }

    @Test
    void testBatchDisableUsers() {
        when(tUserMapper.disableByIds(Arrays.asList(4, 5))).thenReturn(2);
        when(redisManager.delete(anyString())).thenReturn(true);
        userService.batchDisableUsers(Arrays.asList(4, 5));
        verify(tUserMapper).disableByIds(Arrays.asList(4, 5));
    }

    @Test
    void testAssignRoles() {
        AssignUserRolesRequest r = new AssignUserRolesRequest(); r.setUserId(2); r.setRoleIds(Arrays.asList(1));
        when(tUserMapper.selectByPrimaryKey(2)).thenReturn(new TUser());
        when(tUserMapper.deleteUserRoles(2)).thenReturn(1);
        when(tUserMapper.insertUserRoles(eq(2), anyList())).thenReturn(1);
        when(redisManager.delete(anyString())).thenReturn(true);
        userService.assignRoles(r);
        verify(tUserMapper).deleteUserRoles(2);
        verify(redisManager).delete(RedisKeys.userLogin(2));
    }

    @Test
    void assignRoles_sessionInvalidationFailure_shouldRejectOperation() {
        AssignUserRolesRequest request = new AssignUserRolesRequest();
        request.setUserId(2);
        request.setRoleIds(List.of(1));
        when(tUserMapper.selectByPrimaryKey(2)).thenReturn(new TUser());
        when(tUserMapper.deleteUserRoles(2)).thenReturn(1);
        when(tUserMapper.insertUserRoles(2, List.of(1))).thenReturn(1);
        when(redisManager.delete(RedisKeys.userLogin(2))).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class, () -> userService.assignRoles(request));

        assertEquals(CodeEnum.SYSTEM_ERROR, ex.getCodeEnum());
        verify(tUserMapper).deleteUserRoles(2);
        verify(tUserMapper).insertUserRoles(2, List.of(1));
        verify(auditRecorder, never()).record(any(), anyString());
        verify(redisManager, never()).delete(RedisKeys.ownerList());
    }

    @Test
    void assignRoles_lastAdminRemoval_shouldRejectBeforeDelete() {
        AssignUserRolesRequest request = new AssignUserRolesRequest();
        request.setUserId(2);
        request.setRoleIds(Collections.emptyList());
        when(tUserMapper.selectByPrimaryKey(2)).thenReturn(new TUser());
        when(tUserMapper.selectRolesByUserId(2)).thenReturn(List.of(adminRole()));
        when(tUserMapper.countAdminUsers()).thenReturn(1);

        BusinessException ex = assertThrows(BusinessException.class, () -> userService.assignRoles(request));

        assertEquals(CodeEnum.ACCESS_DENIED, ex.getCodeEnum());
        verify(tUserMapper, never()).deleteUserRoles(anyInt());
        verify(tUserMapper, never()).insertUserRoles(anyInt(), anyList());
    }

    @Test
    void assignRoles_keepAdminRole_shouldAllowUpdate() {
        AssignUserRolesRequest request = new AssignUserRolesRequest();
        request.setUserId(2);
        request.setRoleIds(List.of(1));
        when(tUserMapper.selectByPrimaryKey(2)).thenReturn(new TUser());
        when(tUserMapper.selectRolesByUserId(2)).thenReturn(List.of(adminRole()));
        when(tRoleMapper.selectByPrimaryKey(1)).thenReturn(adminRole());
        when(tUserMapper.deleteUserRoles(2)).thenReturn(1);
        when(tUserMapper.insertUserRoles(2, List.of(1))).thenReturn(1);
        when(redisManager.delete(anyString())).thenReturn(true);

        userService.assignRoles(request);

        verify(tUserMapper).deleteUserRoles(2);
        verify(tUserMapper).insertUserRoles(2, List.of(1));
        verify(redisManager).delete(RedisKeys.userLogin(2));
    }

    @Test
    void getOwnerList_cacheHit_shouldNotQueryDatabaseOrRewriteListCache() {
        TUser owner = new TUser();
        owner.setId(2);
        owner.setName("张三");
        List<TUser> cachedOwners = List.of(owner);
        when(redisManager.get(RedisKeys.ownerList())).thenReturn(cachedOwners);

        List<TUser> result = userService.getOwnerList();

        assertEquals(cachedOwners, result);
        verify(tUserMapper, never()).selectByOwner();
        verify(redisManager, never()).set(eq(RedisKeys.ownerList()), any(), anyLong());
    }

    @Test
    void getOwnerList_cacheMiss_shouldStoreSingleValueWithTtl() {
        TUser owner = new TUser();
        owner.setId(2);
        owner.setName("张三");
        List<TUser> owners = List.of(owner);
        when(redisManager.get(RedisKeys.ownerList())).thenReturn(null);
        when(tUserMapper.selectByOwner()).thenReturn(owners);

        List<TUser> result = userService.getOwnerList();

        assertEquals(owners, result);
        verify(redisManager).set(eq(RedisKeys.ownerList()), eq(owners), eq(300L));
    }

    @Test
    void handoverResponsibilities_success_shouldTransferCurrentResponsibilitiesAndWriteHistory() {
        HandoverUserResponsibilitiesRequest request = new HandoverUserResponsibilitiesRequest();
        request.setTargetUserId(30);
        request.setReason("离职交接");
        when(tUserMapper.selectByPrimaryKey(20)).thenReturn(enabledUser(20));
        when(tUserMapper.selectByPrimaryKey(30)).thenReturn(enabledUser(30));
        when(tUserMapper.selectRolesByUserId(30)).thenReturn(List.of(salesOwnerRole()));
        when(currentUserProvider.getCurrentUserId()).thenReturn(10);
        when(tUserMapper.selectOwnedActivityIds(20)).thenReturn(List.of(101, 102));
        when(tUserMapper.selectOwnedClueIds(20)).thenReturn(List.of(201, 202));
        when(tUserMapper.selectOwnedCustomerIds(20)).thenReturn(List.of(301));
        when(tUserMapper.transferOwnedActivities(20, 30, 10)).thenReturn(2);
        when(tUserMapper.transferOwnedClues(20, 30, 10)).thenReturn(2);
        when(tUserMapper.transferOwnedCustomers(20, 30, 10)).thenReturn(1);
        when(clueOwnerHistoryMapper.insert(any(TClueOwnerHistory.class))).thenReturn(1);
        when(customerOwnerHistoryMapper.insert(any(TCustomerOwnerHistory.class))).thenReturn(1);

        HandoverUserResponsibilitiesResponse response = userService.handoverResponsibilities(20, request);

        assertEquals(20, response.getSourceUserId());
        assertEquals(30, response.getTargetUserId());
        assertEquals(2, response.getActivityCount());
        assertEquals(2, response.getClueCount());
        assertEquals(1, response.getCustomerCount());
        verify(clueOwnerHistoryMapper, times(2)).insert(any(TClueOwnerHistory.class));
        verify(customerOwnerHistoryMapper).insert(any(TCustomerOwnerHistory.class));
        verify(auditRecorder).record(eq(AuditActionEnum.USER_HANDOVER), eq("20"),
                eq("SUCCESS"), contains("\"targetUserId\":30"));
    }

    @Test
    void handoverResponsibilities_invalidTargetOwner_shouldRejectBeforeTransfer() {
        HandoverUserResponsibilitiesRequest request = new HandoverUserResponsibilitiesRequest();
        request.setTargetUserId(30);
        request.setReason("离职交接");
        TUser disabledTarget = enabledUser(30);
        disabledTarget.setAccountEnabled(0);
        when(tUserMapper.selectByPrimaryKey(20)).thenReturn(enabledUser(20));
        when(tUserMapper.selectByPrimaryKey(30)).thenReturn(disabledTarget);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.handoverResponsibilities(20, request));

        assertEquals(CodeEnum.ACCESS_DENIED, ex.getCodeEnum());
        verify(tUserMapper, never()).transferOwnedActivities(anyInt(), anyInt(), any());
        verify(tUserMapper, never()).transferOwnedClues(anyInt(), anyInt(), any());
        verify(tUserMapper, never()).transferOwnedCustomers(anyInt(), anyInt(), any());
        verify(auditRecorder, never()).record(eq(AuditActionEnum.USER_HANDOVER), anyString(), anyString(), any());
    }

    @Test
    void handoverResponsibilities_concurrentOwnerChange_shouldRejectWithoutHistoryOrAudit() {
        HandoverUserResponsibilitiesRequest request = new HandoverUserResponsibilitiesRequest();
        request.setTargetUserId(30);
        request.setReason("离职交接");
        when(tUserMapper.selectByPrimaryKey(20)).thenReturn(enabledUser(20));
        when(tUserMapper.selectByPrimaryKey(30)).thenReturn(enabledUser(30));
        when(tUserMapper.selectRolesByUserId(30)).thenReturn(List.of(salesOwnerRole()));
        when(currentUserProvider.getCurrentUserId()).thenReturn(10);
        when(tUserMapper.selectOwnedActivityIds(20)).thenReturn(List.of(101, 102));
        when(tUserMapper.selectOwnedClueIds(20)).thenReturn(Collections.emptyList());
        when(tUserMapper.selectOwnedCustomerIds(20)).thenReturn(Collections.emptyList());
        when(tUserMapper.transferOwnedActivities(20, 30, 10)).thenReturn(1);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.handoverResponsibilities(20, request));

        assertEquals(CodeEnum.OPERATION_FAILED, ex.getCodeEnum());
        verify(tUserMapper, never()).transferOwnedClues(anyInt(), anyInt(), any());
        verify(tUserMapper, never()).transferOwnedCustomers(anyInt(), anyInt(), any());
        verify(clueOwnerHistoryMapper, never()).insert(any());
        verify(customerOwnerHistoryMapper, never()).insert(any());
        verify(auditRecorder, never()).record(eq(AuditActionEnum.USER_HANDOVER), anyString(), anyString(), any());
    }

    @Test
    void testChangePassword() {
        ChangePasswordRequest r = new ChangePasswordRequest(); r.setUserId(2); r.setNewPassword("newpass123");
        when(tUserMapper.selectByPrimaryKey(2)).thenReturn(new TUser());
        when(passwordEncoder.encode("newpass123")).thenReturn("encoded");
        when(tUserMapper.updatePassword(eq(2), eq("encoded"))).thenReturn(1);
        when(redisManager.delete(anyString())).thenReturn(true);
        userService.changePassword(r);
        verify(tUserMapper).updatePassword(2, "encoded");
    }

    @Test
    void changePassword_sessionInvalidationFailure_shouldRejectOperation() {
        ChangePasswordRequest r = new ChangePasswordRequest(); r.setUserId(2); r.setNewPassword("newpass123");
        when(tUserMapper.selectByPrimaryKey(2)).thenReturn(new TUser());
        when(passwordEncoder.encode("newpass123")).thenReturn("encoded");
        when(tUserMapper.updatePassword(eq(2), eq("encoded"))).thenReturn(1);
        when(redisManager.delete(RedisKeys.userLogin(2))).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class, () -> userService.changePassword(r));

        assertEquals(CodeEnum.SYSTEM_ERROR, ex.getCodeEnum());
        verify(tUserMapper).updatePassword(2, "encoded");
        verify(auditRecorder, never()).record(any(), anyString());
    }

    @Test
    void testLockUser_auditFailure_shouldRollback() {
        TUser user = new TUser(); user.setId(2);
        when(tUserMapper.selectByPrimaryKey(2)).thenReturn(user);
        when(tUserMapper.lockById(2)).thenReturn(1);
        when(redisManager.delete(anyString())).thenReturn(true);
        doThrow(new IllegalStateException("审计写入失败"))
                .when(auditRecorder).record(AuditActionEnum.USER_STATUS_CHANGE, "2");
        assertThrows(IllegalStateException.class, () -> userService.lockUser(2));
    }

    @Test
    void testUnlockUser_auditFailure_shouldRollback() {
        TUser user = new TUser(); user.setId(2);
        when(tUserMapper.selectByPrimaryKey(2)).thenReturn(user);
        when(tUserMapper.unlockById(2)).thenReturn(1);
        when(redisManager.delete(anyString())).thenReturn(true);
        doThrow(new IllegalStateException("审计写入失败"))
                .when(auditRecorder).record(AuditActionEnum.USER_STATUS_CHANGE, "2");
        assertThrows(IllegalStateException.class, () -> userService.unlockUser(2));
    }

    private TUser enabledUser(Integer id) {
        TUser user = new TUser();
        user.setId(id);
        user.setAccountEnabled(1);
        user.setAccountNoLocked(1);
        return user;
    }

    private TRole salesOwnerRole() {
        TRole role = new TRole();
        role.setId(2);
        role.setRole("sales_consultant");
        role.setEnabled(1);
        return role;
    }

    private TRole adminRole() {
        TRole role = new TRole();
        role.setId(1);
        role.setRole("admin");
        role.setEnabled(1);
        return role;
    }
}
