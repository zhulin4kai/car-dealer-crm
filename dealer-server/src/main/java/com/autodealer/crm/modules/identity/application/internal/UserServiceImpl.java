package com.autodealer.crm.modules.identity.application.internal;

import com.autodealer.crm.modules.identity.application.api.security.CurrentUserProvider;
import com.autodealer.crm.modules.identity.application.api.security.PrincipalEligibilityPolicy;
import com.autodealer.crm.shared.pagination.PaginationConstants;
import com.autodealer.crm.shared.security.PermissionCodes;
import com.autodealer.crm.shared.infrastructure.cache.RedisKeys;
import com.autodealer.crm.modules.identity.application.api.dto.AssignUserRolesRequest;
import com.autodealer.crm.modules.identity.application.api.dto.ChangePasswordRequest;
import com.autodealer.crm.modules.identity.application.api.dto.CreateUserRequest;
import com.autodealer.crm.modules.identity.application.api.dto.HandoverUserResponsibilitiesRequest;
import com.autodealer.crm.modules.identity.application.api.dto.HandoverUserResponsibilitiesResponse;
import com.autodealer.crm.modules.identity.application.api.dto.UpdateUserRequest;
import com.autodealer.crm.modules.identity.application.api.dto.UserDetailResponse;
import com.autodealer.crm.modules.identity.application.api.dto.UserListQuery;
import com.autodealer.crm.modules.identity.application.api.dto.OwnerCandidate;
import com.autodealer.crm.modules.identity.application.api.enums.OwnerQualificationContext;
import com.autodealer.crm.modules.identity.persistence.mapper.TEmployeeMapper;
import com.autodealer.crm.modules.identity.persistence.mapper.TEmployeeAssignmentMapper;
import com.autodealer.crm.modules.identity.persistence.mapper.TOrganizationUnitMapper;
import com.autodealer.crm.modules.identity.persistence.mapper.TPositionMapper;
import com.autodealer.crm.modules.identity.persistence.model.TEmployee;
import com.autodealer.crm.modules.identity.persistence.model.TEmployeeAssignment;
import com.autodealer.crm.modules.identity.persistence.model.TOrganizationUnit;
import com.autodealer.crm.modules.identity.persistence.model.TPosition;
import com.autodealer.crm.shared.error.BusinessException;
import com.autodealer.crm.shared.infrastructure.cache.RedisManager;
import com.autodealer.crm.modules.identity.persistence.mapper.TPermissionMapper;
import com.autodealer.crm.modules.identity.persistence.mapper.TRoleMapper;
import com.autodealer.crm.modules.identity.persistence.mapper.TUserMapper;
import com.autodealer.crm.modules.identity.persistence.model.TPermission;
import com.autodealer.crm.modules.identity.persistence.model.TRole;
import com.autodealer.crm.modules.identity.application.api.model.TUser;
import com.autodealer.crm.shared.error.CodeEnum;
import com.autodealer.crm.modules.identity.application.api.UserService;
import com.autodealer.crm.modules.identity.application.api.DataScopeResolver;
import com.autodealer.crm.modules.identity.application.api.AuthorizationDataScope;
import com.autodealer.crm.modules.identity.application.internal.UserConverter;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {

    private static final long OWNER_LIST_CACHE_EXPIRE_SECONDS = 300L;

    private final TUserMapper tUserMapper;
    private final TRoleMapper tRoleMapper;
    private final TPermissionMapper tPermissionMapper;
    private final RedisManager redisManager;
    private final CurrentUserProvider currentUserProvider;
    private final DataScopeResolver dataScopeResolver;
    private final TEmployeeMapper employeeMapper;
    private final TEmployeeAssignmentMapper employeeAssignmentMapper;
    private final TOrganizationUnitMapper organizationUnitMapper;
    private final TPositionMapper positionMapper;
    private final PrincipalEligibilityPolicy principalEligibilityPolicy;
    private final UserSecurityMutationCoordinator securityMutations;

    public UserServiceImpl(TUserMapper tUserMapper,
                           TRoleMapper tRoleMapper, TPermissionMapper tPermissionMapper,
                           RedisManager redisManager, CurrentUserProvider currentUserProvider,
                           DataScopeResolver dataScopeResolver,
                           TEmployeeMapper employeeMapper,
                           TEmployeeAssignmentMapper employeeAssignmentMapper,
                           TOrganizationUnitMapper organizationUnitMapper,
                           TPositionMapper positionMapper,
                           PrincipalEligibilityPolicy principalEligibilityPolicy,
                           UserSecurityMutationCoordinator securityMutations) {
        this.tUserMapper = tUserMapper;
        this.tRoleMapper = tRoleMapper;
        this.tPermissionMapper = tPermissionMapper;
        this.redisManager = redisManager;
        this.currentUserProvider = currentUserProvider;
        this.dataScopeResolver = dataScopeResolver;
        this.employeeMapper = employeeMapper;
        this.employeeAssignmentMapper = employeeAssignmentMapper;
        this.organizationUnitMapper = organizationUnitMapper;
        this.positionMapper = positionMapper;
        this.principalEligibilityPolicy = principalEligibilityPolicy;
        this.securityMutations = securityMutations;
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
    public UserDetailResponse createUser(CreateUserRequest request) {
        throw legacyWriteDisabled();
    }

    @Override
    public UserDetailResponse updateUser(UpdateUserRequest request) {
        throw legacyWriteDisabled();
    }

    @Override
    public void disableUser(Integer id) {
        throw legacyWriteDisabled();
    }

    @Override
    public void enableUser(Integer id) {
        throw legacyWriteDisabled();
    }

    @Override
    public void lockUser(Integer id) {
        throw legacyWriteDisabled();
    }

    @Override
    public void unlockUser(Integer id) {
        throw legacyWriteDisabled();
    }

    @Override
    public void batchDisableUsers(List<Integer> ids) {
        throw legacyWriteDisabled();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignRoles(AssignUserRolesRequest request) {
        // 旧接口没有版本、原因、管理链和委派上限契约，继续开放会允许任意提权。
        // Task 13 的 /api/users/{id}/authorization/roles 是唯一写入口。
        throw new BusinessException(CodeEnum.ACCESS_DENIED, "旧角色分配入口已停用，请使用分级授权入口");
    }

    @Override
    public void changePassword(ChangePasswordRequest request) {
        throw legacyWriteDisabled();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void revokeAuthentication(Integer userId) {
        if (userId == null || tUserMapper.incrementAuthVersion(userId) != 1) {
            throw new BusinessException(CodeEnum.SYSTEM_ERROR, "会话撤销事实保存失败");
        }
        securityMutations.authenticationChanged(userId,"账号安全状态变化");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public HandoverUserResponsibilitiesResponse handoverResponsibilities(
            Integer sourceUserId, HandoverUserResponsibilitiesRequest request) {
        throw legacyWriteDisabled();
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

    private BusinessException legacyWriteDisabled() {
        return new BusinessException(CodeEnum.ACCESS_DENIED,
                "旧用户写入口缺少版本、原因或完整管理边界，已停用，请使用受控用户域命令");
    }

    private void requireUserAccess(Integer targetUserId) {
        Integer scopeUserId = currentUserProvider.getDataScopeUserId();
        if (scopeUserId != null && !scopeUserId.equals(targetUserId)) {
            throw new BusinessException(CodeEnum.ACCESS_DENIED, "无权操作该用户");
        }
    }

}
