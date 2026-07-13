package com.autodealer.crm.service.impl;

import com.autodealer.crm.config.security.CurrentUserProvider;
import com.autodealer.crm.config.security.PrincipalEligibilityPolicy;
import com.autodealer.crm.config.security.OwnerCandidateCacheInvalidator;
import com.autodealer.crm.constant.PaginationConstants;
import com.autodealer.crm.constant.PermissionCodes;
import com.autodealer.crm.constant.RedisKeys;
import com.autodealer.crm.dto.AssignUserRolesRequest;
import com.autodealer.crm.dto.ChangePasswordRequest;
import com.autodealer.crm.dto.CreateUserRequest;
import com.autodealer.crm.dto.HandoverUserResponsibilitiesRequest;
import com.autodealer.crm.dto.HandoverUserResponsibilitiesResponse;
import com.autodealer.crm.dto.UpdateUserRequest;
import com.autodealer.crm.dto.UserDetailResponse;
import com.autodealer.crm.dto.UserListQuery;
import com.autodealer.crm.dto.OwnerCandidate;
import com.autodealer.crm.enums.OwnerQualificationContext;
import com.autodealer.crm.mapper.TEmployeeMapper;
import com.autodealer.crm.mapper.TEmployeeAssignmentMapper;
import com.autodealer.crm.mapper.TOrganizationUnitMapper;
import com.autodealer.crm.mapper.TPositionMapper;
import com.autodealer.crm.model.TEmployee;
import com.autodealer.crm.model.TEmployeeAssignment;
import com.autodealer.crm.model.TOrganizationUnit;
import com.autodealer.crm.model.TPosition;
import com.autodealer.crm.audit.AuditActionEnum;
import com.autodealer.crm.audit.OperationAuditRecorder;
import com.autodealer.crm.exception.BusinessException;
import com.autodealer.crm.manager.RedisManager;
import com.autodealer.crm.mapper.TClueOwnerHistoryMapper;
import com.autodealer.crm.mapper.TCustomerOwnerHistoryMapper;
import com.autodealer.crm.mapper.TPermissionMapper;
import com.autodealer.crm.mapper.TRoleMapper;
import com.autodealer.crm.mapper.TUserMapper;
import com.autodealer.crm.model.TClueOwnerHistory;
import com.autodealer.crm.model.TCustomerOwnerHistory;
import com.autodealer.crm.model.TPermission;
import com.autodealer.crm.model.TRole;
import com.autodealer.crm.model.TUser;
import com.autodealer.crm.result.CodeEnum;
import com.autodealer.crm.service.UserService;
import com.autodealer.crm.service.UserSessionService;
import jakarta.annotation.Resource;
import com.autodealer.crm.service.DataScopeResolver;
import com.autodealer.crm.service.AuthorizationDataScope;
import com.autodealer.crm.util.UserConverter;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);
    private static final String ROLE_ADMIN = "admin";
    private static final String ROLE_SALES_CONSULTANT = "sales_consultant";
    private static final String ROLE_SALES_MANAGER = "sales_manager";
    private static final int BUILTIN_ADMIN_ID = 1;
    private static final int MIN_PASSWORD_LENGTH = 6;
    private static final int MAX_PASSWORD_LENGTH = 16;
    private static final long OWNER_LIST_CACHE_EXPIRE_SECONDS = 300L;
    private static final int SESSION_CLEANUP_RETRY_TIMES = 2;

    private final TUserMapper tUserMapper;
    private final TClueOwnerHistoryMapper clueOwnerHistoryMapper;
    private final TCustomerOwnerHistoryMapper customerOwnerHistoryMapper;
    private final PasswordEncoder passwordEncoder;
    private final TRoleMapper tRoleMapper;
    private final TPermissionMapper tPermissionMapper;
    private final RedisManager redisManager;
    private final CurrentUserProvider currentUserProvider;
    private final OperationAuditRecorder auditRecorder;
    private final DataScopeResolver dataScopeResolver;
    private final TEmployeeMapper employeeMapper;
    private final TEmployeeAssignmentMapper employeeAssignmentMapper;
    private final TOrganizationUnitMapper organizationUnitMapper;
    private final TPositionMapper positionMapper;
    private final PrincipalEligibilityPolicy principalEligibilityPolicy;
    private final OwnerCandidateCacheInvalidator ownerCandidateCacheInvalidator;
    @Resource private UserSessionService userSessionService;

    public UserServiceImpl(TUserMapper tUserMapper, PasswordEncoder passwordEncoder,
                           TRoleMapper tRoleMapper, TPermissionMapper tPermissionMapper,
                           RedisManager redisManager, CurrentUserProvider currentUserProvider,
                           OperationAuditRecorder auditRecorder,
                           TClueOwnerHistoryMapper clueOwnerHistoryMapper,
                           TCustomerOwnerHistoryMapper customerOwnerHistoryMapper,
                           DataScopeResolver dataScopeResolver,
                           TEmployeeMapper employeeMapper,
                           TEmployeeAssignmentMapper employeeAssignmentMapper,
                           TOrganizationUnitMapper organizationUnitMapper,
                           TPositionMapper positionMapper,
                           PrincipalEligibilityPolicy principalEligibilityPolicy,
                           OwnerCandidateCacheInvalidator ownerCandidateCacheInvalidator) {
        this.tUserMapper = tUserMapper;
        this.clueOwnerHistoryMapper = clueOwnerHistoryMapper;
        this.customerOwnerHistoryMapper = customerOwnerHistoryMapper;
        this.passwordEncoder = passwordEncoder;
        this.tRoleMapper = tRoleMapper;
        this.tPermissionMapper = tPermissionMapper;
        this.redisManager = redisManager;
        this.currentUserProvider = currentUserProvider;
        this.dataScopeResolver = dataScopeResolver;
        this.auditRecorder = auditRecorder;
        this.employeeMapper = employeeMapper;
        this.employeeAssignmentMapper = employeeAssignmentMapper;
        this.organizationUnitMapper = organizationUnitMapper;
        this.positionMapper = positionMapper;
        this.principalEligibilityPolicy = principalEligibilityPolicy;
        this.ownerCandidateCacheInvalidator = ownerCandidateCacheInvalidator;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        TUser tUser = tUserMapper.selectByLoginAct(username);
        if (tUser == null) {
            throw new UsernameNotFoundException("登录账号不存在");
        }
        if (!principalEligibilityPolicy.isEligible(tUser)) {
            throw new UsernameNotFoundException("登录账号当前不具备有效人员任职资格");
        }
        loadLoginPermissions(tUser);
        return tUser;
    }

    @Override
    public TUser getLoginUserById(Integer id) {
        TUser tUser = tUserMapper.selectByPrimaryKey(id);
        if (tUser == null || !principalEligibilityPolicy.isEligible(tUser)) {
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
        if (Boolean.TRUE.equals(tUser.getProtectedAccount())) {
            flatMenuPermissions = flatMenuPermissions.stream().filter(this::isRecoveryGovernancePermission).toList();
        }
        tUser.setMenuPermissionList(buildMenuTree(flatMenuPermissions));
        List<TPermission> buttonPermissionList = tPermissionMapper.selectButtonPermissionByUserId(tUser.getId());
        if (Boolean.TRUE.equals(tUser.getProtectedAccount())) {
            buttonPermissionList = buttonPermissionList.stream().filter(this::isRecoveryGovernancePermission).toList();
        }
        List<String> stringPermissionList = new ArrayList<>();
        buttonPermissionList.forEach(tPermission -> stringPermissionList.add(tPermission.getCode()));
        tUser.setPermissionList(stringPermissionList);
    }

    private boolean isRecoveryGovernancePermission(TPermission permission) {
        String code = permission.getCode();
        if (code == null) return false;
        return code.startsWith("menu:user") || code.startsWith("page:user") || code.startsWith("user:")
                || code.startsWith("menu:organization") || code.startsWith("page:organization")
                || code.startsWith("organization:") || code.startsWith("position:") || code.startsWith("employee:")
                || code.startsWith("menu:access") || code.startsWith("page:role") || code.startsWith("page:permission")
                || code.startsWith("role:") || code.startsWith("permission:")
                || code.startsWith("menu:audit") || code.startsWith("page:audit") || code.startsWith("audit:");
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
        Integer operatorId = currentUserProvider.getCurrentUserId();
        if (operatorId != null && dataScopeResolver != null) {
            AuthorizationDataScope scope = dataScopeResolver.resolve(operatorId, PermissionCodes.USER_LIST);
            if (scope != null) {
                query.setDataScopeUserId(null);
                query.setDataScopeDenied(!scope.global() && scope.visibleUserIds().isEmpty());
                if (!scope.global()) query.setDataScopeVisibleUserIds(new ArrayList<>(scope.visibleUserIds()));
            }
        }
        int page = query.getCurrent() == null || query.getCurrent() < 1 ? 1 : query.getCurrent();
        int pageSize = query.getPageSize() == null || query.getPageSize() < 1
                ? PaginationConstants.DEFAULT_PAGE_SIZE : query.getPageSize();
        if (pageSize > 100) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "分页大小不能超过100");
        }
        PageHelper.startPage(page, pageSize);
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
        failLegacyWrite();
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
        ownerCandidateCacheInvalidator.invalidateAfterCommit();
        log.info("event=user_create result=success userId={} loginAct={} operatorId={}",
                tUser.getId(), request.getLoginAct(), currentUserProvider.getCurrentUserId());
        auditRecorder.record(AuditActionEnum.USER_CREATE, String.valueOf(tUser.getId()));
        return toDetailResponse(tUser);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserDetailResponse updateUser(UpdateUserRequest request) {
        failLegacyWrite();
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
        boolean loginActChanged = !Objects.equals(existingUser.getLoginAct(), request.getLoginAct());
        if (loginActChanged && tUserMapper.incrementAuthVersion(request.getId()) != 1) {
            throw new BusinessException(CodeEnum.FAIL, "登录账号安全版本更新失败");
        }
        ownerCandidateCacheInvalidator.invalidateAfterCommit();
        log.info("event=user_update result=success userId={} operatorId={}",
                request.getId(), currentUserProvider.getCurrentUserId());
        auditRecorder.record(AuditActionEnum.USER_UPDATE, String.valueOf(request.getId()));
        if (loginActChanged) {
            scheduleSessionCleanup(request.getId());
        }
        TUser updatedUser = tUserMapper.selectAuthUserById(request.getId());
        return UserConverter.toDetailResponse(updatedUser);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void disableUser(Integer id) {
        failLegacyWrite();
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
        ownerCandidateCacheInvalidator.invalidateAfterCommit();
        log.info("event=user_disable result=success userId={} operatorId={}",
                id, currentUserProvider.getCurrentUserId());
        auditRecorder.record(AuditActionEnum.USER_STATUS_CHANGE, String.valueOf(id));
        scheduleSessionCleanup(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void enableUser(Integer id) {
        failLegacyWrite();
        requireUserAccess(id);
        if (tUserMapper.selectByPrimaryKey(id) == null) throw new BusinessException(CodeEnum.FAIL, "用户不存在");
        if (tUserMapper.enableById(id) != 1) throw new BusinessException(CodeEnum.FAIL, "启用操作失败");
        ownerCandidateCacheInvalidator.invalidateAfterCommit();
        log.info("event=user_enable result=success userId={} operatorId={}",
                id, currentUserProvider.getCurrentUserId());
        auditRecorder.record(AuditActionEnum.USER_STATUS_CHANGE, String.valueOf(id));
        scheduleSessionCleanup(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void lockUser(Integer id) {
        failLegacyWrite();
        validateNotSelfOperation(id, "不能锁定当前登录账号");
        requireUserAccess(id);
        if (tUserMapper.selectByPrimaryKey(id) == null) throw new BusinessException(CodeEnum.FAIL, "用户不存在");
        if (Integer.valueOf(BUILTIN_ADMIN_ID).equals(id)) {
            throw new BusinessException(CodeEnum.ACCESS_DENIED, "内置管理员不能被锁定");
        }
        validateNotLastAdmin(id, "该用户是最后一个有效管理员，不能锁定");
        if (tUserMapper.lockById(id) != 1) throw new BusinessException(CodeEnum.FAIL, "锁定操作失败");
        ownerCandidateCacheInvalidator.invalidateAfterCommit();
        auditRecorder.record(AuditActionEnum.USER_STATUS_CHANGE, String.valueOf(id));
        scheduleSessionCleanup(id);
        log.info("event=user_lock result=success userId={} operatorId={}",
                id, currentUserProvider.getCurrentUserId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unlockUser(Integer id) {
        failLegacyWrite();
        requireUserAccess(id);
        if (tUserMapper.selectByPrimaryKey(id) == null) throw new BusinessException(CodeEnum.FAIL, "用户不存在");
        if (tUserMapper.unlockById(id) != 1) throw new BusinessException(CodeEnum.FAIL, "解锁操作失败");
        ownerCandidateCacheInvalidator.invalidateAfterCommit();
        auditRecorder.record(AuditActionEnum.USER_STATUS_CHANGE, String.valueOf(id));
        scheduleSessionCleanup(id);
        log.info("event=user_unlock result=success userId={} operatorId={}",
                id, currentUserProvider.getCurrentUserId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDisableUsers(List<Integer> ids) {
        failLegacyWrite();
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
        ownerCandidateCacheInvalidator.invalidateAfterCommit();
        log.info("event=user_batch_disable result=success count={} operatorId={}",
                distinctIds.size(), currentUserProvider.getCurrentUserId());
        distinctIds.forEach(this::scheduleSessionCleanup);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignRoles(AssignUserRolesRequest request) {
        // 旧接口没有版本、原因、管理链和委派上限契约，继续开放会允许任意提权。
        // Task 13 的 /api/users/{id}/authorization/roles 是唯一写入口。
        throw new BusinessException(CodeEnum.ACCESS_DENIED, "旧角色分配入口已停用，请使用分级授权入口");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(ChangePasswordRequest request) {
        failLegacyWrite();
        requireUserAccess(request.getUserId());
        validatePasswordLength(request.getNewPassword());
        if (tUserMapper.selectByPrimaryKey(request.getUserId()) == null) {
            throw new BusinessException(CodeEnum.FAIL, "用户不存在");
        }
        String encodedPassword = passwordEncoder.encode(request.getNewPassword());
        if (tUserMapper.updatePassword(request.getUserId(), encodedPassword) != 1) {
            throw new BusinessException(CodeEnum.FAIL, "密码修改失败");
        }
        log.info("event=user_password_change result=success userId={} operatorId={}",
                request.getUserId(), currentUserProvider.getCurrentUserId());
        auditRecorder.record(AuditActionEnum.USER_PASSWORD_CHANGE, String.valueOf(request.getUserId()));
        scheduleSessionCleanup(request.getUserId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void revokeAuthentication(Integer userId) {
        if (userId == null || tUserMapper.incrementAuthVersion(userId) != 1) {
            throw new BusinessException(CodeEnum.SYSTEM_ERROR, "会话撤销事实保存失败");
        }
        scheduleSessionCleanup(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public HandoverUserResponsibilitiesResponse handoverResponsibilities(
            Integer sourceUserId, HandoverUserResponsibilitiesRequest request) {
        failLegacyWrite();
        if (sourceUserId == null) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "原负责人不能为空");
        }
        if (sourceUserId.equals(request.getTargetUserId())) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "目标负责人不能与原负责人相同");
        }
        requireUserAccess(sourceUserId);
        if (tUserMapper.selectByPrimaryKey(sourceUserId) == null) {
            throw new BusinessException(CodeEnum.NOT_FOUND, "原负责人不存在");
        }
        requireValidOwnerUser(request.getTargetUserId());

        Integer operatorId = currentUserProvider.getCurrentUserId();
        List<Integer> activityIds = safeIdList(tUserMapper.selectOwnedActivityIds(sourceUserId));
        List<Integer> clueIds = safeIdList(tUserMapper.selectOwnedClueIds(sourceUserId));
        List<Integer> customerIds = safeIdList(tUserMapper.selectOwnedCustomerIds(sourceUserId));

        int activityRows = tUserMapper.transferOwnedActivities(sourceUserId, request.getTargetUserId(), operatorId);
        assertHandoverRows("活动", activityIds.size(), activityRows);
        int clueRows = tUserMapper.transferOwnedClues(sourceUserId, request.getTargetUserId(), operatorId);
        assertHandoverRows("线索", clueIds.size(), clueRows);
        int customerRows = tUserMapper.transferOwnedCustomers(sourceUserId, request.getTargetUserId(), operatorId);
        assertHandoverRows("客户", customerIds.size(), customerRows);

        Date transferTime = new Date();
        for (Integer clueId : clueIds) {
            insertClueOwnerHistory(clueId, sourceUserId, request.getTargetUserId(),
                    operatorId, request.getReason(), transferTime);
        }
        for (Integer customerId : customerIds) {
            insertCustomerOwnerHistory(customerId, sourceUserId, request.getTargetUserId(),
                    operatorId, request.getReason(), transferTime);
        }

        HandoverUserResponsibilitiesResponse response = new HandoverUserResponsibilitiesResponse(
                sourceUserId, request.getTargetUserId(), activityRows, clueRows, customerRows);
        auditRecorder.record(AuditActionEnum.USER_HANDOVER, String.valueOf(sourceUserId),
                "SUCCESS", buildHandoverSummary(response));
        log.info("event=user_handover result=success sourceUserId={} targetUserId={} activityCount={} clueCount={} customerCount={} operatorId={}",
                sourceUserId, request.getTargetUserId(), activityRows, clueRows, customerRows, operatorId);
        return response;
    }

    @Override
    public List<TUser> getOwnerList() {
        Integer operatorId = currentUserProvider.getCurrentUserId();
        AuthorizationDataScope scope = dataScopeResolver.resolve(operatorId, PermissionCodes.USER_LIST);
        if (scope == null) {
            List<TUser> legacy = redisManager.get(RedisKeys.ownerList());
            if (legacy != null && !legacy.isEmpty()) return legacy;
            List<TUser> owners = tUserMapper.selectByOwner();
            if (owners != null && !owners.isEmpty()) redisManager.set(RedisKeys.ownerList(), owners, OWNER_LIST_CACHE_EXPIRE_SECONDS);
            return owners;
        }
        if (!scope.global() && scope.visibleUserIds().isEmpty()) return List.of();
        String cacheKey = RedisKeys.ownerList(operatorId);
        List<TUser> cachedOwners = redisManager.get(cacheKey);
        if (cachedOwners != null && !cachedOwners.isEmpty()) {
            return cachedOwners;
        }

        List<Integer> visibleUserIds = scope.global() ? null : new ArrayList<>(scope.visibleUserIds());
        List<TUser> owners = tUserMapper.selectEligibleOwners(visibleUserIds, PermissionCodes.USER_LIST);
        if (owners != null && !owners.isEmpty()) {
            boolean cached = redisManager.set(cacheKey, owners, OWNER_LIST_CACHE_EXPIRE_SECONDS);
            if (!cached) {
                log.warn("event=owner_list_cache_write result=failed ttlSeconds={}", OWNER_LIST_CACHE_EXPIRE_SECONDS);
            }
        }
        return owners;
    }

    @Override
    public List<OwnerCandidate> getOwnerCandidates(String permissionCode, String qualificationContext) {
        OwnerQualificationContext context;
        try { context = OwnerQualificationContext.valueOf(qualificationContext); }
        catch (RuntimeException exception) { throw new BusinessException(CodeEnum.PARAM_ERROR, "未知负责人资格场景"); }
        if (!allowedOwnerPermissions(context).contains(permissionCode)) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "负责人资格场景与权限动作不匹配");
        }
        if (!currentUserProvider.hasAuthority(permissionCode)) {
            throw new BusinessException(CodeEnum.ACCESS_DENIED, "当前操作者不具备该业务动作权限");
        }
        Integer operatorId=currentUserProvider.getCurrentUserId();
        AuthorizationDataScope scope=dataScopeResolver.resolve(operatorId,permissionCode);
        if(scope==null||(!scope.global()&&scope.visibleUserIds().isEmpty()))return List.of();
        String cacheKey=RedisKeys.ownerList(operatorId,permissionCode,context.name());
        List<OwnerCandidate> cached=redisManager.get(cacheKey);if(cached!=null&&!cached.isEmpty())return cached;
        List<TUser> users=tUserMapper.selectEligibleOwners(scope.global()?null:new ArrayList<>(scope.visibleUserIds()),permissionCode);
        List<OwnerCandidate> result=users.stream().map(this::toOwnerCandidate).toList();
        if(!result.isEmpty())redisManager.set(cacheKey,result,OWNER_LIST_CACHE_EXPIRE_SECONDS);
        return result;
    }

    private Set<String> allowedOwnerPermissions(OwnerQualificationContext context) {
        return switch(context) {
            case ACTIVITY_OWNER -> Set.of(PermissionCodes.ACTIVITY_ADD,PermissionCodes.ACTIVITY_EDIT);
            case CLUE_OWNER -> Set.of(PermissionCodes.CLUE_ADD,PermissionCodes.CLUE_EDIT,PermissionCodes.CLUE_TRANSFER);
            case CUSTOMER_OWNER -> Set.of(PermissionCodes.CUSTOMER_TRANSFER);
            case TRANSACTION_OWNER -> Set.of(PermissionCodes.TRAN_CREATE,PermissionCodes.TRAN_EDIT);
        };
    }

    private OwnerCandidate toOwnerCandidate(TUser user){OwnerCandidate value=new OwnerCandidate();value.setUserId(user.getId());value.setName(user.getName());TEmployee employee=employeeMapper.selectByUserId(user.getId());if(employee==null)return value;value.setEmployeeId(employee.getId());value.setEmployeeNo(employee.getEmployeeNo());TEmployeeAssignment primary=employeeAssignmentMapper.selectCurrentPrimaryByEmployeeId(employee.getId(),java.time.LocalDateTime.now());if(primary==null)return value;value.setOrganizationUnitId(primary.getOrganizationUnitId());value.setPositionId(primary.getPositionId());TOrganizationUnit org=organizationUnitMapper.selectByPrimaryKey(primary.getOrganizationUnitId());TPosition position=positionMapper.selectByPrimaryKey(primary.getPositionId());value.setOrganizationName(org==null?null:org.getName());value.setPositionName(position==null?null:position.getName());return value;}

    @Override
    public UserDetailResponse toDetailResponse(TUser tUser) {
        return UserConverter.toDetailResponse(tUser);
    }

    private void requireValidOwnerUser(Integer targetUserId) {
        TUser targetUser = tUserMapper.selectByPrimaryKey(targetUserId);
        if (targetUser == null) {
            throw new BusinessException(CodeEnum.NOT_FOUND, "目标负责人不存在");
        }
        if (!Integer.valueOf(1).equals(targetUser.getAccountEnabled())
                || !Integer.valueOf(1).equals(targetUser.getAccountNoLocked())) {
            throw new BusinessException(CodeEnum.ACCESS_DENIED, "目标负责人不可用");
        }
        List<TRole> roles = tUserMapper.selectRolesByUserId(targetUserId);
        boolean canOwnBusiness = roles != null && roles.stream()
                .anyMatch(role -> isEnabledRole(role)
                        && (ROLE_SALES_CONSULTANT.equals(role.getRole())
                        || ROLE_SALES_MANAGER.equals(role.getRole())));
        if (!canOwnBusiness) {
            throw new BusinessException(CodeEnum.ACCESS_DENIED, "目标负责人不具备业务负责人资格");
        }
    }

    private List<Integer> safeIdList(List<Integer> ids) {
        return ids == null ? Collections.emptyList() : ids;
    }

    private void assertHandoverRows(String objectName, int expectedRows, int actualRows) {
        if (actualRows != expectedRows) {
            throw new BusinessException(CodeEnum.OPERATION_FAILED,
                    objectName + "交接对象已变化，请刷新后重试");
        }
    }

    private void insertClueOwnerHistory(Integer clueId, Integer sourceUserId, Integer targetUserId,
                                        Integer operatorId, String reason, Date transferTime) {
        TClueOwnerHistory history = new TClueOwnerHistory();
        history.setClueId(clueId);
        history.setFromOwnerId(sourceUserId);
        history.setToOwnerId(targetUserId);
        history.setAssignedBy(operatorId);
        history.setReason(reason);
        history.setAssignedTime(transferTime);
        if (clueOwnerHistoryMapper.insert(history) != 1) {
            throw new BusinessException(CodeEnum.OPERATION_FAILED, "线索交接历史写入失败");
        }
    }

    private void insertCustomerOwnerHistory(Integer customerId, Integer sourceUserId, Integer targetUserId,
                                            Integer operatorId, String reason, Date transferTime) {
        TCustomerOwnerHistory history = new TCustomerOwnerHistory();
        history.setCustomerId(customerId);
        history.setFromOwnerId(sourceUserId);
        history.setToOwnerId(targetUserId);
        history.setOperatorId(operatorId);
        history.setReason(reason);
        history.setTransferTime(transferTime);
        if (customerOwnerHistoryMapper.insert(history) != 1) {
            throw new BusinessException(CodeEnum.OPERATION_FAILED, "客户交接历史写入失败");
        }
    }

    private String buildHandoverSummary(HandoverUserResponsibilitiesResponse response) {
        return "{\"targetUserId\":" + response.getTargetUserId()
                + ",\"activityCount\":" + response.getActivityCount()
                + ",\"clueCount\":" + response.getClueCount()
                + ",\"customerCount\":" + response.getCustomerCount() + "}";
    }

    private void validatePasswordLength(String password) {
        if (password == null || password.length() < MIN_PASSWORD_LENGTH || password.length() > MAX_PASSWORD_LENGTH) {
            throw new BusinessException(CodeEnum.PARAM_ERROR,
                    "密码长度必须为" + MIN_PASSWORD_LENGTH + "-" + MAX_PASSWORD_LENGTH + "位");
        }
    }

    private void failLegacyWrite() {
        throw new BusinessException(CodeEnum.ACCESS_DENIED,
                "旧用户写入口缺少版本、原因或完整管理边界，已停用，请使用受控用户域命令");
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

    private void scheduleSessionCleanup(Integer userId) {
        if (userSessionService != null) {
            userSessionService.revokeAllForSecurityChange(userId,currentUserProvider.getCurrentUserId(),"账号安全状态变化");
            return;
        }
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    clearSessionWithRetry(userId);
                }
            });
            return;
        }
        clearSessionWithRetry(userId);
    }

    private void clearSessionWithRetry(Integer userId) {
        for (int attempt = 1; attempt <= SESSION_CLEANUP_RETRY_TIMES; attempt++) {
            try {
                if (redisManager.delete(RedisKeys.userLogin(userId))) {
                    return;
                }
                log.warn("event=user_session_cleanup result=not_deleted userId={} attempt={}", userId, attempt);
            } catch (RuntimeException exception) {
                log.warn("event=user_session_cleanup result=failed userId={} attempt={}",
                        userId, attempt, exception);
            }
        }
        log.error("event=user_session_cleanup result=retry_exhausted userId={} attempts={}",
                userId, SESSION_CLEANUP_RETRY_TIMES);
    }

}
