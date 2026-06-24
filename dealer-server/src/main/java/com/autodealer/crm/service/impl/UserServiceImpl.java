package com.autodealer.crm.service.impl;

import com.autodealer.crm.config.security.CurrentUserProvider;
import com.autodealer.crm.constant.PaginationConstants;
import com.autodealer.crm.constant.RedisKeys;
import com.autodealer.crm.dto.AssignUserRolesRequest;
import com.autodealer.crm.dto.ChangePasswordRequest;
import com.autodealer.crm.dto.CreateUserRequest;
import com.autodealer.crm.dto.UpdateUserRequest;
import com.autodealer.crm.dto.UserDetailResponse;
import com.autodealer.crm.dto.UserListQuery;
import com.autodealer.crm.audit.AuditActionEnum;
import com.autodealer.crm.audit.OperationAuditRecorder;
import com.autodealer.crm.exception.BusinessException;
import com.autodealer.crm.manager.RedisManager;
import com.autodealer.crm.mapper.TPermissionMapper;
import com.autodealer.crm.mapper.TRoleMapper;
import com.autodealer.crm.mapper.TUserMapper;
import com.autodealer.crm.model.TPermission;
import com.autodealer.crm.model.TRole;
import com.autodealer.crm.model.TUser;
import com.autodealer.crm.result.CodeEnum;
import com.autodealer.crm.service.UserService;
import com.autodealer.crm.util.CacheUtils;
import com.autodealer.crm.util.UserConverter;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);
    private static final String ROLE_ADMIN = "admin";
    private static final int BUILTIN_ADMIN_ID = 1;
    private static final int MIN_PASSWORD_LENGTH = 6;
    private static final int MAX_PASSWORD_LENGTH = 16;

    private final TUserMapper tUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final TRoleMapper tRoleMapper;
    private final TPermissionMapper tPermissionMapper;
    private final RedisManager redisManager;
    private final CurrentUserProvider currentUserProvider;
    private final OperationAuditRecorder auditRecorder;

    public UserServiceImpl(TUserMapper tUserMapper, PasswordEncoder passwordEncoder,
                           TRoleMapper tRoleMapper, TPermissionMapper tPermissionMapper,
                           RedisManager redisManager, CurrentUserProvider currentUserProvider,
                           OperationAuditRecorder auditRecorder) {
        this.tUserMapper = tUserMapper;
        this.passwordEncoder = passwordEncoder;
        this.tRoleMapper = tRoleMapper;
        this.tPermissionMapper = tPermissionMapper;
        this.redisManager = redisManager;
        this.currentUserProvider = currentUserProvider;
        this.auditRecorder = auditRecorder;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        TUser tUser = tUserMapper.selectByLoginAct(username);
        if (tUser == null) {
            throw new UsernameNotFoundException("登录账号不存在");
        }
        loadLoginPermissions(tUser);
        return tUser;
    }

    @Override
    public TUser getLoginUserById(Integer id) {
        TUser tUser = tUserMapper.selectByPrimaryKey(id);
        if (tUser == null) {
            return null;
        }
        loadLoginPermissions(tUser);
        return tUser;
    }

    private void loadLoginPermissions(TUser tUser) {
        List<TRole> tRoleList = tRoleMapper.selectByUserId(tUser.getId());
        List<String> stringRoleList = new ArrayList<>();
        tRoleList.forEach(tRole -> stringRoleList.add(tRole.getRole()));
        tUser.setRoleList(stringRoleList);
        List<TPermission> flatMenuPermissions = tPermissionMapper.selectMenuPermissionByUserId(tUser.getId());
        tUser.setMenuPermissionList(buildMenuTree(flatMenuPermissions));
        List<TPermission> buttonPermissionList = tPermissionMapper.selectButtonPermissionByUserId(tUser.getId());
        List<String> stringPermissionList = new ArrayList<>();
        buttonPermissionList.forEach(tPermission -> stringPermissionList.add(tPermission.getCode()));
        tUser.setPermissionList(stringPermissionList);
    }

    private List<TPermission> buildMenuTree(List<TPermission> flatMenus) {
        Map<Integer, TPermission> byId = new LinkedHashMap<>();
        flatMenus.forEach(permission -> {
            permission.setSubPermissionList(new ArrayList<>());
            byId.put(permission.getId(), permission);
        });

        List<TPermission> roots = new ArrayList<>();
        flatMenus.forEach(permission -> {
            if (permission.getParentId() == null) {
                roots.add(permission);
                return;
            }
            TPermission parent = byId.get(permission.getParentId());
            if (parent != null) {
                parent.getSubPermissionList().add(permission);
            }
        });
        return roots;
    }

    @Override
    public PageInfo<UserDetailResponse> getUserByPage(UserListQuery query) {
        PageHelper.startPage(query.getCurrent(), PaginationConstants.DEFAULT_PAGE_SIZE);
        List<TUser> list = tUserMapper.selectUserByPage(query);
        PageInfo<TUser> rawInfo = new PageInfo<>(list);
        List<UserDetailResponse> responseList = list.stream()
                .map(UserConverter::toDetailResponse).collect(Collectors.toList());
        PageInfo<UserDetailResponse> result = new PageInfo<>();
        result.setList(responseList);
        result.setTotal(rawInfo.getTotal());
        result.setPageNum(rawInfo.getPageNum());
        result.setPageSize(rawInfo.getPageSize());
        result.setPages(rawInfo.getPages());
        return result;
    }

    @Override
    public UserDetailResponse getUserById(Integer id) {
        requireUserAccess(id);
        TUser tUser = tUserMapper.selectAuthUserById(id);
        if (tUser == null) return null;
        return UserConverter.toDetailResponse(tUser);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserDetailResponse createUser(CreateUserRequest request) {
        validatePasswordLength(request.getLoginPwd());
        if (tUserMapper.selectByLoginAct(request.getLoginAct()) != null) {
            throw new BusinessException(CodeEnum.FAIL, "登录账号已存在");
        }
        if (tUserMapper.selectByPhone(request.getPhone()) != null) {
            throw new BusinessException(CodeEnum.FAIL, "手机号已存在");
        }
        if (tUserMapper.selectByEmail(request.getEmail()) != null) {
            throw new BusinessException(CodeEnum.FAIL, "邮箱已存在");
        }
        TUser tUser = new TUser();
        tUser.setLoginAct(request.getLoginAct());
        tUser.setLoginPwd(passwordEncoder.encode(request.getLoginPwd()));
        tUser.setName(request.getName());
        tUser.setPhone(request.getPhone());
        tUser.setEmail(request.getEmail());
        tUser.setAccountNoExpired(1);
        tUser.setCredentialsNoExpired(1);
        tUser.setAccountNoLocked(1);
        tUser.setAccountEnabled(1);
        tUser.setCreateTime(new Date());
        tUser.setCreateBy(currentUserProvider.getCurrentUserId());
        try {
            int rows = tUserMapper.insertSelective(tUser);
            if (rows != 1) throw new BusinessException(CodeEnum.FAIL, "创建用户失败");
        } catch (DuplicateKeyException e) {
            throw new BusinessException(CodeEnum.FAIL, "用户信息重复");
        }
        clearOwnerListCache();
        log.info("event=user_create result=success userId={} loginAct={} operatorId={}",
                tUser.getId(), request.getLoginAct(), currentUserProvider.getCurrentUserId());
        auditRecorder.record(AuditActionEnum.USER_CREATE, String.valueOf(tUser.getId()));
        return toDetailResponse(tUser);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserDetailResponse updateUser(UpdateUserRequest request) {
        requireUserAccess(request.getId());
        TUser existingUser = tUserMapper.selectByPrimaryKey(request.getId());
        if (existingUser == null) throw new BusinessException(CodeEnum.FAIL, "用户不存在");
        validateUniquenessForUpdate(request.getId(), request.getLoginAct(), request.getPhone(), request.getEmail());
        TUser tUser = new TUser();
        tUser.setId(request.getId());
        tUser.setLoginAct(request.getLoginAct());
        tUser.setName(request.getName());
        tUser.setPhone(request.getPhone());
        tUser.setEmail(request.getEmail());
        tUser.setEditTime(new Date());
        tUser.setEditBy(currentUserProvider.getCurrentUserId());
        try {
            int rows = tUserMapper.updateByPrimaryKeySelective(tUser);
            if (rows != 1) throw new BusinessException(CodeEnum.FAIL, "用户更新失败");
        } catch (DuplicateKeyException e) {
            throw new BusinessException(CodeEnum.FAIL, "用户信息重复");
        }
        clearOwnerListCache();
        log.info("event=user_update result=success userId={} operatorId={}",
                request.getId(), currentUserProvider.getCurrentUserId());
        auditRecorder.record(AuditActionEnum.USER_UPDATE, String.valueOf(request.getId()));
        TUser updatedUser = tUserMapper.selectAuthUserById(request.getId());
        return UserConverter.toDetailResponse(updatedUser);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void disableUser(Integer id) {
        validateNotSelfOperation(id, "不能禁用当前登录账号");
        requireUserAccess(id);
        TUser targetUser = tUserMapper.selectByPrimaryKey(id);
        if (targetUser == null) throw new BusinessException(CodeEnum.FAIL, "用户不存在");
        if (targetUser.getAccountEnabled() == 0) return;
        if (id == BUILTIN_ADMIN_ID) throw new BusinessException(CodeEnum.ACCESS_DENIED, "内置管理员不能被禁用");
        validateNotLastAdmin(id);
        if (tUserMapper.countBusinessReferences(id) > 0) {
            throw new BusinessException(CodeEnum.FAIL, "用户仍被业务引用，无法禁用");
        }
        if (tUserMapper.disableById(id) != 1) throw new BusinessException(CodeEnum.FAIL, "禁用操作失败");
        invalidateUserSession(id);
        clearOwnerListCache();
        log.info("event=user_disable result=success userId={} operatorId={}",
                id, currentUserProvider.getCurrentUserId());
        auditRecorder.record(AuditActionEnum.USER_STATUS_CHANGE, String.valueOf(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void enableUser(Integer id) {
        requireUserAccess(id);
        if (tUserMapper.selectByPrimaryKey(id) == null) throw new BusinessException(CodeEnum.FAIL, "用户不存在");
        if (tUserMapper.enableById(id) != 1) throw new BusinessException(CodeEnum.FAIL, "启用操作失败");
        clearOwnerListCache();
        log.info("event=user_enable result=success userId={} operatorId={}",
                id, currentUserProvider.getCurrentUserId());
        auditRecorder.record(AuditActionEnum.USER_STATUS_CHANGE, String.valueOf(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void lockUser(Integer id) {
        validateNotSelfOperation(id, "不能锁定当前登录账号");
        requireUserAccess(id);
        if (tUserMapper.selectByPrimaryKey(id) == null) throw new BusinessException(CodeEnum.FAIL, "用户不存在");
        if (Integer.valueOf(BUILTIN_ADMIN_ID).equals(id)) {
            throw new BusinessException(CodeEnum.ACCESS_DENIED, "内置管理员不能被锁定");
        }
        validateNotLastAdmin(id, "该用户是最后一个有效管理员，不能锁定");
        if (tUserMapper.lockById(id) != 1) throw new BusinessException(CodeEnum.FAIL, "锁定操作失败");
        invalidateUserSession(id);
        clearOwnerListCache();
        auditRecorder.record(AuditActionEnum.USER_STATUS_CHANGE, String.valueOf(id));
        log.info("event=user_lock result=success userId={} operatorId={}",
                id, currentUserProvider.getCurrentUserId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unlockUser(Integer id) {
        requireUserAccess(id);
        if (tUserMapper.selectByPrimaryKey(id) == null) throw new BusinessException(CodeEnum.FAIL, "用户不存在");
        if (tUserMapper.unlockById(id) != 1) throw new BusinessException(CodeEnum.FAIL, "解锁操作失败");
        clearOwnerListCache();
        auditRecorder.record(AuditActionEnum.USER_STATUS_CHANGE, String.valueOf(id));
        log.info("event=user_unlock result=success userId={} operatorId={}",
                id, currentUserProvider.getCurrentUserId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDisableUsers(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) return;
        List<Integer> distinctIds = ids.stream().distinct().sorted().toList();
        for (Integer id : distinctIds) {
            validateNotSelfOperation(id, "不能禁用当前登录账号");
            requireUserAccess(id);
        }
        if (distinctIds.contains(BUILTIN_ADMIN_ID)) {
            throw new BusinessException(CodeEnum.ACCESS_DENIED, "内置管理员不能被禁用");
        }
        for (Integer id : distinctIds) validateNotLastAdmin(id);
        int totalRefs = 0;
        for (Integer id : distinctIds) totalRefs += tUserMapper.countBusinessReferences(id);
        if (totalRefs > 0) throw new BusinessException(CodeEnum.FAIL, "所选用户中存在被业务引用的账号，无法禁用");
        if (tUserMapper.disableByIds(distinctIds) != distinctIds.size()) {
            throw new BusinessException(CodeEnum.FAIL, "批量禁用操作失败");
        }
        for (Integer id : distinctIds) invalidateUserSession(id);
        clearOwnerListCache();
        log.info("event=user_batch_disable result=success count={} operatorId={}",
                distinctIds.size(), currentUserProvider.getCurrentUserId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignRoles(AssignUserRolesRequest request) {
        requireUserAccess(request.getUserId());
        if (tUserMapper.selectByPrimaryKey(request.getUserId()) == null) {
            throw new BusinessException(CodeEnum.FAIL, "用户不存在");
        }
        List<Integer> roleIds = request.getRoleIds() != null ? request.getRoleIds() : Collections.emptyList();
        validateAdminRoleRetention(request.getUserId(), roleIds);
        tUserMapper.deleteUserRoles(request.getUserId());
        if (!roleIds.isEmpty()) {
            if (tUserMapper.insertUserRoles(request.getUserId(), roleIds) != roleIds.size()) {
                throw new BusinessException(CodeEnum.FAIL, "角色分配失败");
            }
        }
        clearOwnerListCache();
        log.info("event=user_role_assign result=success userId={} roleIds={} operatorId={}",
                request.getUserId(), roleIds, currentUserProvider.getCurrentUserId());
        auditRecorder.record(AuditActionEnum.USER_STATUS_CHANGE, String.valueOf(request.getUserId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(ChangePasswordRequest request) {
        requireUserAccess(request.getUserId());
        validatePasswordLength(request.getNewPassword());
        if (tUserMapper.selectByPrimaryKey(request.getUserId()) == null) {
            throw new BusinessException(CodeEnum.FAIL, "用户不存在");
        }
        String encodedPassword = passwordEncoder.encode(request.getNewPassword());
        if (tUserMapper.updatePassword(request.getUserId(), encodedPassword) != 1) {
            throw new BusinessException(CodeEnum.FAIL, "密码修改失败");
        }
        invalidateUserSession(request.getUserId());
        log.info("event=user_password_change result=success userId={} operatorId={}",
                request.getUserId(), currentUserProvider.getCurrentUserId());
        auditRecorder.record(AuditActionEnum.USER_STATUS_CHANGE, String.valueOf(request.getUserId()));
    }

    @Override
    public List<TUser> getOwnerList() {
        return CacheUtils.getCacheData(
                () -> (List<TUser>) redisManager.getList(RedisKeys.ownerList()),
                () -> (List<TUser>) tUserMapper.selectByOwner(),
                (t) -> redisManager.setList(RedisKeys.ownerList(), t));
    }

    @Override
    public UserDetailResponse toDetailResponse(TUser tUser) {
        return UserConverter.toDetailResponse(tUser);
    }

    private void validatePasswordLength(String password) {
        if (password == null || password.length() < MIN_PASSWORD_LENGTH || password.length() > MAX_PASSWORD_LENGTH) {
            throw new BusinessException(CodeEnum.PARAM_ERROR,
                    "密码长度必须为" + MIN_PASSWORD_LENGTH + "-" + MAX_PASSWORD_LENGTH + "位");
        }
    }

    private void validateUniquenessForUpdate(Integer userId, String loginAct, String phone, String email) {
        if (tUserMapper.selectByLoginActExcludeId(loginAct, userId) != null)
            throw new BusinessException(CodeEnum.FAIL, "登录账号已存在");
        if (tUserMapper.selectByPhoneExcludeId(phone, userId) != null)
            throw new BusinessException(CodeEnum.FAIL, "手机号已存在");
        if (tUserMapper.selectByEmailExcludeId(email, userId) != null)
            throw new BusinessException(CodeEnum.FAIL, "邮箱已存在");
    }

    private void validateNotSelfOperation(Integer targetUserId, String message) {
        Integer currentUserId = currentUserProvider.getCurrentUserId();
        if (currentUserId != null && currentUserId.equals(targetUserId)) {
            throw new BusinessException(CodeEnum.ACCESS_DENIED, message);
        }
    }

    private void validateNotLastAdmin(Integer targetUserId) {
        validateNotLastAdmin(targetUserId, "该用户是最后一个有效管理员，不能禁用");
    }

    private void validateNotLastAdmin(Integer targetUserId, String message) {
        TUser targetUser = tUserMapper.selectByPrimaryKey(targetUserId);
        if (targetUser == null) return;
        List<TRole> userRoles = tRoleMapper.selectByUserId(targetUserId);
        if (hasAdminRole(userRoles)) {
            if (tUserMapper.countAdminUsers() <= 1) {
                throw new BusinessException(CodeEnum.ACCESS_DENIED, message);
            }
        }
    }

    private void validateAdminRoleRetention(Integer targetUserId, List<Integer> newRoleIds) {
        List<TRole> currentRoles = tUserMapper.selectRolesByUserId(targetUserId);
        if (!hasAdminRole(currentRoles) || containsAdminRole(newRoleIds)) {
            return;
        }
        if (Integer.valueOf(BUILTIN_ADMIN_ID).equals(targetUserId)) {
            throw new BusinessException(CodeEnum.ACCESS_DENIED, "内置管理员不能被移除管理员角色");
        }
        if (tUserMapper.countAdminUsers() <= 1) {
            throw new BusinessException(CodeEnum.ACCESS_DENIED, "该用户是最后一个有效管理员，不能移除管理员角色");
        }
    }

    private boolean containsAdminRole(List<Integer> roleIds) {
        for (Integer roleId : roleIds) {
            if (roleId == null) {
                continue;
            }
            TRole role = tRoleMapper.selectByPrimaryKey(roleId);
            if (isEnabledRole(role) && ROLE_ADMIN.equals(role.getRole())) {
                return true;
            }
        }
        return false;
    }

    private boolean hasAdminRole(List<TRole> roles) {
        if (roles == null || roles.isEmpty()) {
            return false;
        }
        return roles.stream().anyMatch(role -> isEnabledRole(role) && ROLE_ADMIN.equals(role.getRole()));
    }

    private boolean isEnabledRole(TRole role) {
        return role != null && (role.getEnabled() == null || role.getEnabled() == 1);
    }

    private void requireUserAccess(Integer targetUserId) {
        Integer scopeUserId = currentUserProvider.getDataScopeUserId();
        if (scopeUserId != null && !scopeUserId.equals(targetUserId)) {
            throw new BusinessException(CodeEnum.ACCESS_DENIED, "无权操作该用户");
        }
    }

    private void invalidateUserSession(Integer userId) {
        try { redisManager.delete(RedisKeys.userLogin(userId)); }
        catch (Exception e) { log.warn("event=user_session_invalidate result=failed userId={}", userId, e); }
    }

    private void clearOwnerListCache() {
        try { redisManager.delete(RedisKeys.ownerList()); }
        catch (Exception e) { log.warn("event=owner_list_cache_clear result=failed", e); }
    }
}
