package com.autodealer.crm.modules.identity.application.internal;

import com.autodealer.crm.modules.identity.application.api.enums.AccountType;
import com.autodealer.crm.modules.identity.application.api.enums.DataScopeCode;
import com.autodealer.crm.modules.identity.application.api.enums.PermissionEffect;
import com.autodealer.crm.modules.identity.application.api.enums.PermissionSensitivityLevel;
import com.autodealer.crm.modules.identity.application.api.enums.RoleScopeType;
import com.autodealer.crm.modules.identity.application.api.model.TUser;
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
import com.autodealer.crm.modules.identity.persistence.mapper.TUserPermissionMapper;
import com.autodealer.crm.modules.identity.persistence.mapper.TUserPermissionOrganizationMapper;
import com.autodealer.crm.modules.identity.persistence.mapper.TUserRoleMapper;
import com.autodealer.crm.modules.identity.persistence.model.TEmployee;
import com.autodealer.crm.modules.identity.persistence.model.TEmployeeAssignment;
import com.autodealer.crm.modules.identity.persistence.model.TEmployeeReporting;
import com.autodealer.crm.modules.identity.persistence.model.TOrganizationUnit;
import com.autodealer.crm.modules.identity.persistence.model.TPermission;
import com.autodealer.crm.modules.identity.persistence.model.TRole;
import com.autodealer.crm.modules.identity.persistence.model.TRolePermission;
import com.autodealer.crm.modules.identity.persistence.model.TUserPermission;
import com.autodealer.crm.modules.identity.persistence.model.TUserRole;
import com.autodealer.crm.modules.identity.application.api.security.CurrentUserProvider;
import com.autodealer.crm.modules.identity.application.api.enums.*;
import com.autodealer.crm.shared.error.BusinessException;
import com.autodealer.crm.modules.identity.persistence.mapper.*;
import com.autodealer.crm.modules.identity.persistence.model.*;
import com.autodealer.crm.modules.identity.application.api.model.*;
import com.autodealer.crm.shared.error.CodeEnum;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;

/** 分级授权的唯一服务端策略边界；Controller 和请求数据都不能提供管理范围。 */
@Component
public class UserAuthorizationPolicy {
    private final CurrentUserProvider currentUserProvider;
    private final TUserMapper userMapper;
    private final TEmployeeMapper employeeMapper;
    private final TEmployeeAssignmentMapper assignmentMapper;
    private final TEmployeeReportingMapper reportingMapper;
    private final TOrganizationUnitMapper organizationMapper;
    private final TRoleMapper roleMapper;
    private final TRolePermissionMapper rolePermissionMapper;
    private final TRoleOrganizationMapper roleOrganizationMapper;
    private final TRolePermissionOrganizationMapper rolePermissionOrganizationMapper;
    private final TUserRoleMapper userRoleMapper;
    private final TUserPermissionMapper userPermissionMapper;
    private final TUserPermissionOrganizationMapper userPermissionOrganizationMapper;
    private final TPermissionMapper permissionMapper;

    public UserAuthorizationPolicy(CurrentUserProvider currentUserProvider, TUserMapper userMapper,
                                   TEmployeeMapper employeeMapper, TEmployeeAssignmentMapper assignmentMapper,
                                   TEmployeeReportingMapper reportingMapper, TOrganizationUnitMapper organizationMapper,
                                   TRoleMapper roleMapper, TRolePermissionMapper rolePermissionMapper,
                                   TRoleOrganizationMapper roleOrganizationMapper,
                                   TRolePermissionOrganizationMapper rolePermissionOrganizationMapper,
                                   TUserRoleMapper userRoleMapper, TUserPermissionMapper userPermissionMapper,
                                   TUserPermissionOrganizationMapper userPermissionOrganizationMapper,
                                   TPermissionMapper permissionMapper) {
        this.currentUserProvider = currentUserProvider; this.userMapper = userMapper;
        this.employeeMapper = employeeMapper; this.assignmentMapper = assignmentMapper;
        this.reportingMapper = reportingMapper; this.organizationMapper = organizationMapper;
        this.roleMapper = roleMapper; this.rolePermissionMapper = rolePermissionMapper;
        this.roleOrganizationMapper = roleOrganizationMapper;
        this.rolePermissionOrganizationMapper = rolePermissionOrganizationMapper;
        this.userRoleMapper = userRoleMapper; this.userPermissionMapper = userPermissionMapper;
        this.userPermissionOrganizationMapper = userPermissionOrganizationMapper;
        this.permissionMapper = permissionMapper;
    }

    public boolean isGlobalOperator() {
        return isQualifiedSecurityAdministrator();
    }

    public boolean isBootstrapRecoveryOperator() {
        TUser current = currentUserProvider.getCurrentUser();
        return current.getAccountType() == AccountType.SYSTEM
                && Boolean.TRUE.equals(current.getProtectedAccount())
                && Objects.equals(current.getId(), 1)
                && "admin".equals(current.getLoginAct())
                && userMapper.countAdminUsers() == 0
                && userMapper.countPendingAdminUsers() == 0
                && organizationMapper.countInitializedRootOrganizations() == 0;
    }

    public boolean canView(TUser target) {
        return Objects.equals(target.getId(), currentUserProvider.getCurrentUserId()) || canManage(target);
    }

    public boolean canManage(TUser target) {
        if (target == null || Objects.equals(target.getId(), currentUserProvider.getCurrentUserId())) return false;
        if (target.getAccountType() == AccountType.SYSTEM || Boolean.TRUE.equals(target.getProtectedAccount())) return false;
        if (isQualifiedSecurityAdministrator()
                && operatorAuthorizationLevel() > authorizationLevel(target.getId())) return true;
        LocalDateTime now = LocalDateTime.now();
        TEmployee operator = employeeMapper.selectByUserId(currentUserProvider.getCurrentUserId());
        TEmployee employee = employeeMapper.selectByUserId(target.getId());
        if (operator == null || employee == null || !isManagerOf(operator.getId(), employee.getId(), now)) return false;
        TEmployeeAssignment operatorPrimary = assignmentMapper.selectCurrentPrimaryByEmployeeId(operator.getId(), now);
        TEmployeeAssignment targetPrimary = assignmentMapper.selectCurrentPrimaryByEmployeeId(employee.getId(), now);
        return operatorPrimary != null && targetPrimary != null
                && isDescendant(targetPrimary.getOrganizationUnitId(), operatorPrimary.getOrganizationUnitId());
    }

    public void requireView(TUser target) {
        if (!canView(target)) throw new BusinessException(CodeEnum.ACCESS_DENIED, "目标用户超出授权查看范围");
    }

    public void requireManage(TUser target) {
        if (target != null && Objects.equals(target.getId(), currentUserProvider.getCurrentUserId())) {
            throw new BusinessException(CodeEnum.SELF_MANAGEMENT_FORBIDDEN, "任何用户都不能调整自己的授权");
        }
        if (!canManage(target)) throw new BusinessException(CodeEnum.ACCESS_DENIED, "目标用户超出管理链、组织范围或属于受保护账号");
    }

    /**
     * 授权写入比普通资料、状态管理更严格：操作者除了必须真实管理目标，
     * 其当前生效授权级别还必须严格高于目标，禁止同级或下级管理员互相改权。
     */
    public boolean canManageAuthorization(TUser target) {
        return canManage(target)
                && operatorAuthorizationLevel() > authorizationLevel(target.getId());
    }

    public void requireAuthorizationManage(TUser target) {
        if (target != null && Objects.equals(target.getId(), currentUserProvider.getCurrentUserId())) {
            throw new BusinessException(CodeEnum.SELF_MANAGEMENT_FORBIDDEN, "任何用户都不能调整自己的授权");
        }
        if (!canManageAuthorization(target)) {
            throw new BusinessException(CodeEnum.ACCESS_DENIED, "目标用户超出授权管理范围或授权级别不低于操作者");
        }
    }

    /**
     * 角色成员关系的专用管理边界。普通角色仍要求操作者严格高于目标；唯一的同级例外是：
     * 两个均满足安全管理员资格的普通账号之间，只能调整受保护 admin 成员关系。
     */
    public void requireRoleManage(TUser target, boolean protectedAdministratorOnly) {
        if (target != null && Objects.equals(target.getId(), currentUserProvider.getCurrentUserId())) {
            throw new BusinessException(CodeEnum.SELF_MANAGEMENT_FORBIDDEN, "任何用户都不能调整自己的授权");
        }
        if (canManageAuthorization(target)) return;
        if (protectedAdministratorOnly && canManageProtectedAdministratorTarget(target)) return;
        throw new BusinessException(CodeEnum.ACCESS_DENIED, "目标用户超出角色管理级别、管理范围或属于受保护账号");
    }

    public boolean canManageRoleAssignments(TUser target) {
        return canManageAuthorization(target) || canManageProtectedAdministratorTarget(target);
    }

    public boolean canDelegateRole(TRole role, TUser target) {
        if (role == null || role.getEnabled() == null || role.getEnabled() != 1) return false;
        boolean protectedAdministrator = Boolean.TRUE.equals(role.getProtectedRole()) && "admin".equals(role.getRole());
        boolean protectedAdministratorAllowed = protectedAdministrator
                && canManageProtectedAdministratorMembership(role, target);
        if (Boolean.TRUE.equals(role.getProtectedRole()) && !protectedAdministratorAllowed) return false;
        if (!protectedAdministratorAllowed && !canManageAuthorization(target)) return false;
        if (role.getScopeType() == RoleScopeType.GLOBAL && !isGlobalOperator() && !protectedAdministratorAllowed) return false;
        if (!roleAppliesToTarget(role, target)) return false;
        if (protectedAdministratorAllowed) return true;
        int ceiling = operatorAuthorizationLevel();
        if (role.getAuthorizationLevel() == null || role.getAuthorizationLevel() >= ceiling
                || role.getDefaultDataScope() == DataScopeCode.GLOBAL) return false;
        Map<Integer, Set<DataScopeCode>> delegable = operatorDelegablePermissions();
        for (TRolePermission item : rolePermissionMapper.selectByRoleId(role.getId())) {
            TPermission permission = permissionMapper.selectByPrimaryKey(item.getPermissionId());
            if (permission == null || permission.getEnabled() == null || permission.getEnabled() != 1
                    || !Boolean.TRUE.equals(permission.getDelegable())
                    || permission.getSensitivityLevel() != PermissionSensitivityLevel.NORMAL
                    || !Boolean.TRUE.equals(item.getDelegable())) return false;
            Set<DataScopeCode> scopes = delegable.get(item.getPermissionId());
            if (!scopeCovered(scopes, item.getDataScopeCode(), target)) return false;
        }
        return true;
    }

    /** 撤销不依赖角色当前 enabled/delegable，避免停用后无法降权；仍保护恢复角色和级别边界。 */
    public boolean canRevokeRole(TRole role, TUser target) {
        if (role == null) return false;
        if (Boolean.TRUE.equals(role.getProtectedRole())) {
            return canManageProtectedAdministratorMembership(role, target);
        }
        if (!canManageAuthorization(target)) return false;
        return role.getAuthorizationLevel() != null && role.getAuthorizationLevel() < operatorAuthorizationLevel()
                && (isGlobalOperator() || role.getScopeType() == RoleScopeType.ORGANIZATION);
    }

    /**
     * 受保护角色的目录、矩阵和安全语义仍由角色域冻结；这里只开放 admin 角色的普通账号成员关系。
     * 受保护恢复账号本身继续由 requireManage 永久排除，任何操作者也不能通过此分支修改本人。
     */
    private boolean canManageProtectedAdministratorMembership(TRole role, TUser target) {
        if (!"admin".equals(role.getRole())) return false;
        if (target == null) {
            return isBootstrapRecoveryOperator() || (isQualifiedSecurityAdministrator()
                    && role.getAuthorizationLevel() != null
                    && operatorAuthorizationLevel() >= role.getAuthorizationLevel());
        }
        return canManageProtectedAdministratorTarget(target);
    }

    private boolean canManageProtectedAdministratorTarget(TUser target) {
        if (target == null || Objects.equals(target.getId(), currentUserProvider.getCurrentUserId())
                || target.getAccountType() == AccountType.SYSTEM || Boolean.TRUE.equals(target.getProtectedAccount())
                || !isQualifiedSecurityAdministrator()) return false;
        return operatorAuthorizationLevel() >= authorizationLevel(target.getId());
    }

    public boolean isQualifiedSecurityAdministrator() {
        TUser current = currentUserProvider.getCurrentUser();
        if (current.getAccountType() != AccountType.HUMAN
                || Boolean.TRUE.equals(current.getProtectedAccount())
                || userMapper.countQualifiedSecurityAdministrator(current.getId()) != 1) return false;
        TRole protectedAdministrator = roleMapper.selectByCode("admin");
        return protectedAdministrator != null && Boolean.TRUE.equals(protectedAdministrator.getProtectedRole())
                && protectedAdministrator.getEnabled() != null && protectedAdministrator.getEnabled() == 1
                && protectedAdministrator.getAuthorizationLevel() != null
                && operatorAuthorizationLevel() >= protectedAdministrator.getAuthorizationLevel();
    }

    public boolean canDelegatePermission(TPermission permission, DataScopeCode requestedScope, TUser target) {
        return canDelegatePermission(permission, requestedScope, List.of(), target);
    }

    public boolean canDelegatePermission(TPermission permission, DataScopeCode requestedScope,
                                         List<Integer> requestedOrganizationIds, TUser target) {
        if (permission == null || permission.getEnabled() == null || permission.getEnabled() != 1
                || !Boolean.TRUE.equals(permission.getDelegable())) return false;
        if (permission.getSensitivityLevel() != PermissionSensitivityLevel.NORMAL || requestedScope == DataScopeCode.GLOBAL) return false;
        if (requestedScope == DataScopeCode.CUSTOM_ORGS) {
            List<Integer> requested = requestedOrganizationIds == null ? List.of() : requestedOrganizationIds;
            if (requested.isEmpty()) return false;
            return new HashSet<>(delegableCustomOrganizationIds(permission, target)).containsAll(requested);
        }
        if (requestedOrganizationIds != null && !requestedOrganizationIds.isEmpty()) return false;
        return scopeCovered(operatorDelegablePermissions().get(permission.getId()), requestedScope, target);
    }

    public List<DataScopeCode> delegableScopes(TPermission permission, TUser target) {
        if (permission == null || permission.getSensitivityLevel() != PermissionSensitivityLevel.NORMAL
                || !Boolean.TRUE.equals(permission.getDelegable())) return List.of();
        Set<DataScopeCode> owned = operatorDelegablePermissions().getOrDefault(permission.getId(), Set.of());
        boolean customOrganizationsAvailable = !delegableCustomOrganizationIds(permission, target).isEmpty();
        return Arrays.stream(DataScopeCode.values())
                .filter(scope -> scope != DataScopeCode.GLOBAL)
                .filter(scope -> scope == DataScopeCode.CUSTOM_ORGS
                        ? customOrganizationsAvailable : scopeCovered(owned, scope, target)).toList();
    }

    /** 返回同时落在操作者该权限可委派组织范围和目标主要组织树内的明确组织。 */
    public List<Integer> delegableCustomOrganizationIds(TPermission permission, TUser target) {
        if (permission == null || permission.getEnabled() == null || permission.getEnabled() != 1
                || !Boolean.TRUE.equals(permission.getDelegable())) return List.of();
        LocalDateTime now = LocalDateTime.now();
        if (permission.getSensitivityLevel() != PermissionSensitivityLevel.NORMAL) return List.of();
        TEmployee operator = employeeMapper.selectByUserId(currentUserProvider.getCurrentUserId());
        TEmployee targetEmployee = target == null ? null : employeeMapper.selectByUserId(target.getId());
        TEmployeeAssignment operatorPrimary = operator == null ? null
                : assignmentMapper.selectCurrentPrimaryByEmployeeId(operator.getId(), now);
        TEmployeeAssignment targetPrimary = targetEmployee == null ? null
                : assignmentMapper.selectCurrentPrimaryByEmployeeId(targetEmployee.getId(), now);
        if (operatorPrimary == null || targetPrimary == null) return List.of();
        Set<Integer> owned = operatorDelegableOrganizationIds(permission.getId(), operatorPrimary, now);
        return owned.stream().filter(id -> {
            TOrganizationUnit organization = organizationMapper.selectByPrimaryKey(id);
            return availableOrganization(organization)
                    && isDescendant(id, targetPrimary.getOrganizationUnitId());
        }).sorted().toList();
    }

    public List<TRole> roleCandidates(TUser target) {
        if (isGlobalOperator() || canManageRoleAssignments(target)) return roleMapper.selectAll().stream()
                .filter(role -> canDelegateRole(role,target)).toList();
        TEmployee operator = employeeMapper.selectByUserId(currentUserProvider.getCurrentUserId());
        TEmployeeAssignment primary = operator == null ? null
                : assignmentMapper.selectCurrentPrimaryByEmployeeId(operator.getId(), LocalDateTime.now());
        if (primary == null) return List.of();
        List<Integer> visibleOrganizations = organizationMapper.selectDescendantIds(primary.getOrganizationUnitId());
        if (visibleOrganizations.isEmpty()) return List.of();
        return roleMapper.selectFilteredVisible(null, null, operatorAuthorizationLevel(), visibleOrganizations).stream()
                .filter(role -> canDelegateRole(role,target)).toList();
    }

    /** 新建用户尚无 userId，按所选组织计算可委派初始角色，不能复用“可见事实角色”。 */
    public List<TRole> assignableRoleCandidates(Integer organizationUnitId) {
        if (isBootstrapRecoveryOperator()) {
            return roleMapper.selectAll().stream()
                    .filter(role -> role.getEnabled() != null && role.getEnabled() == 1)
                    .filter(role -> Boolean.TRUE.equals(role.getProtectedRole()) && "admin".equals(role.getRole()))
                    .toList();
        }
        boolean globalOperator = isGlobalOperator();
        boolean qualifiedSecurityAdministrator = isQualifiedSecurityAdministrator();
        List<TRole> roles = roleMapper.selectAll().stream()
                .filter(role -> role.getEnabled() != null && role.getEnabled() == 1)
                .filter(role -> !Boolean.TRUE.equals(role.getProtectedRole())
                        || ((globalOperator || qualifiedSecurityAdministrator)
                        && "admin".equals(role.getRole())
                        && canManageProtectedAdministratorMembership(role, null)))
                .toList();
        if (globalOperator || qualifiedSecurityAdministrator) {
            return roles.stream().filter(role -> role.getScopeType() == RoleScopeType.GLOBAL
                    || roleAppliesToOrganization(role, organizationUnitId)).toList();
        }
        if (organizationUnitId == null || !isOrganizationWithinOperatorScope(organizationUnitId)) return List.of();
        int ceiling = operatorAuthorizationLevel();
        Map<Integer, Set<DataScopeCode>> delegable = operatorDelegablePermissions();
        return roles.stream()
                .filter(role -> role.getScopeType() == RoleScopeType.ORGANIZATION)
                .filter(role -> role.getAuthorizationLevel() != null && role.getAuthorizationLevel() < ceiling)
                .filter(role -> role.getDefaultDataScope() != DataScopeCode.GLOBAL)
                .filter(role -> roleAppliesToOrganization(role, organizationUnitId))
                .filter(role -> rolePermissionSetDelegable(role, delegable))
                .toList();
    }

    private boolean rolePermissionSetDelegable(TRole role, Map<Integer, Set<DataScopeCode>> delegable) {
        for (TRolePermission item : rolePermissionMapper.selectByRoleId(role.getId())) {
            TPermission permission = permissionMapper.selectByPrimaryKey(item.getPermissionId());
            if (permission == null || permission.getEnabled() == null || permission.getEnabled() != 1
                    || permission.getSensitivityLevel() != PermissionSensitivityLevel.NORMAL
                    || !Boolean.TRUE.equals(permission.getDelegable()) || !Boolean.TRUE.equals(item.getDelegable())
                    || !scopeMatrixCovers(delegable.get(item.getPermissionId()), item.getDataScopeCode(), true)) {
                return false;
            }
        }
        return true;
    }

    private boolean roleAppliesToOrganization(TRole role, Integer organizationUnitId) {
        if (role.getScopeType() == RoleScopeType.GLOBAL) return isGlobalOperator();
        if (organizationUnitId == null) return false;
        return roleOrganizationMapper.selectByRoleId(role.getId()).stream()
                .anyMatch(scope -> isDescendant(organizationUnitId, scope.getOrganizationUnitId()));
    }

    private boolean isOrganizationWithinOperatorScope(Integer organizationUnitId) {
        TEmployee operator = employeeMapper.selectByUserId(currentUserProvider.getCurrentUserId());
        TEmployeeAssignment primary = operator == null ? null
                : assignmentMapper.selectCurrentPrimaryByEmployeeId(operator.getId(), LocalDateTime.now());
        return primary != null && isDescendant(organizationUnitId, primary.getOrganizationUnitId());
    }

    public boolean roleAppliesToTarget(TRole role, TUser target) {
        if (role.getScopeType() == RoleScopeType.GLOBAL) return true;
        TEmployee targetEmployee = employeeMapper.selectByUserId(target.getId());
        TEmployeeAssignment primary = targetEmployee == null ? null
                : assignmentMapper.selectCurrentPrimaryByEmployeeId(targetEmployee.getId(), LocalDateTime.now());
        if (primary == null) return false;
        return roleOrganizationMapper.selectByRoleId(role.getId()).stream()
                .anyMatch(scope -> isDescendant(primary.getOrganizationUnitId(), scope.getOrganizationUnitId()));
    }

    private int operatorAuthorizationLevel() {
        return authorizationLevel(currentUserProvider.getCurrentUserId());
    }

    private int authorizationLevel(Integer userId) {
        LocalDateTime now = LocalDateTime.now();
        return userRoleMapper.selectEffectiveByUserId(userId, now).stream()
                .map(value -> roleMapper.selectByPrimaryKey(value.getRoleId()))
                .filter(Objects::nonNull).filter(role -> role.getEnabled() != null && role.getEnabled() == 1)
                .filter(role -> roleAppliesToUser(role, userId, now))
                .map(TRole::getAuthorizationLevel).filter(Objects::nonNull).max(Integer::compareTo).orElse(-1);
    }

    private Map<Integer, Set<DataScopeCode>> operatorDelegablePermissions() {
        Integer userId = currentUserProvider.getCurrentUserId();
        LocalDateTime now = LocalDateTime.now();
        Map<Integer, Set<DataScopeCode>> result = new HashMap<>();
        for (TUserRole assignment : userRoleMapper.selectEffectiveByUserId(userId, now)) {
            TRole role = roleMapper.selectByPrimaryKey(assignment.getRoleId());
            if (role == null || role.getEnabled() == null || role.getEnabled() != 1
                    || !roleAppliesToUser(role, userId, now)) continue;
            for (TRolePermission item : rolePermissionMapper.selectByRoleId(role.getId())) {
                TPermission permission = permissionMapper.selectByPrimaryKey(item.getPermissionId());
                if (permission != null && permission.getEnabled() != null && permission.getEnabled() == 1
                        && permission.getSensitivityLevel() == PermissionSensitivityLevel.NORMAL
                        && Boolean.TRUE.equals(permission.getDelegable()) && Boolean.TRUE.equals(item.getDelegable())) {
                    result.computeIfAbsent(item.getPermissionId(), ignored -> new HashSet<>()).add(item.getDataScopeCode());
                }
            }
        }
        for (TUserPermission personal : userPermissionMapper.selectEffectiveByUserId(userId, now)) {
            if (personal.getEffect() == PermissionEffect.DENY) result.remove(personal.getPermissionId());
            else {
                TPermission permission = permissionMapper.selectByPrimaryKey(personal.getPermissionId());
                if (permission != null && permission.getEnabled() != null && permission.getEnabled() == 1
                        && permission.getSensitivityLevel() == PermissionSensitivityLevel.NORMAL
                        && Boolean.TRUE.equals(permission.getDelegable())) {
                    result.computeIfAbsent(personal.getPermissionId(), ignored -> new HashSet<>()).add(personal.getDataScopeCode());
                }
            }
        }
        return result;
    }

    private Set<Integer> operatorDelegableOrganizationIds(Integer permissionId,
                                                          TEmployeeAssignment operatorPrimary,
                                                          LocalDateTime now) {
        Integer userId = currentUserProvider.getCurrentUserId();
        Set<Integer> result = new LinkedHashSet<>();
        for (TUserRole assignment : userRoleMapper.selectEffectiveByUserId(userId, now)) {
            TRole role = roleMapper.selectByPrimaryKey(assignment.getRoleId());
            if (role == null || role.getEnabled() == null || role.getEnabled() != 1
                    || !roleAppliesToUser(role, userId, now)) continue;
            for (TRolePermission item : rolePermissionMapper.selectByRoleId(role.getId())) {
                if (!Objects.equals(item.getPermissionId(), permissionId) || !Boolean.TRUE.equals(item.getDelegable())) continue;
                List<Integer> explicitOrganizations = item.getDataScopeCode() == DataScopeCode.CUSTOM_ORGS
                        ? rolePermissionOrganizationMapper.selectOrganizationIds(role.getId(), permissionId) : List.of();
                addOrganizationCoverage(result, item.getDataScopeCode(), operatorPrimary.getOrganizationUnitId(),
                        explicitOrganizations);
            }
        }
        for (TUserPermission personal : userPermissionMapper.selectEffectiveByUserId(userId, now)) {
            if (!Objects.equals(personal.getPermissionId(), permissionId)) continue;
            if (personal.getEffect() == PermissionEffect.DENY) return Set.of();
            addOrganizationCoverage(result, personal.getDataScopeCode(), operatorPrimary.getOrganizationUnitId(),
                    personal.getDataScopeCode() == DataScopeCode.CUSTOM_ORGS
                            ? userPermissionOrganizationMapper.selectOrganizationIds(personal.getId()) : List.of());
        }
        return result;
    }

    private void addOrganizationCoverage(Set<Integer> result, DataScopeCode scope,
                                         Integer primaryOrganizationId, List<Integer> explicitOrganizationIds) {
        if (scope == null) return;
        switch (scope) {
            case PRIMARY_ORG -> result.add(primaryOrganizationId);
            case ORG_TREE -> {
                List<Integer> descendants = organizationMapper.selectDescendantIds(primaryOrganizationId);
                if (descendants != null) result.addAll(descendants);
            }
            case CUSTOM_ORGS -> {
                if (explicitOrganizationIds != null) result.addAll(explicitOrganizationIds);
            }
            case GLOBAL -> organizationMapper.selectAll().stream().filter(this::availableOrganization)
                    .map(TOrganizationUnit::getId).forEach(result::add);
            default -> { /* 用户范围不能转换成明确组织授权。 */ }
        }
    }

    private boolean availableOrganization(TOrganizationUnit organization) {
        return organization != null && Boolean.TRUE.equals(organization.getEnabled())
                && !Boolean.TRUE.equals(organization.getMigrationPlaceholder());
    }

    private boolean roleAppliesToUser(TRole role, Integer userId, LocalDateTime at) {
        if (role.getScopeType() == RoleScopeType.GLOBAL) return true;
        TEmployee employee = employeeMapper.selectByUserId(userId);
        TEmployeeAssignment primary = employee == null ? null
                : assignmentMapper.selectCurrentPrimaryByEmployeeId(employee.getId(), at);
        return primary != null && roleOrganizationMapper.selectByRoleId(role.getId()).stream()
                .anyMatch(scope -> isDescendant(primary.getOrganizationUnitId(), scope.getOrganizationUnitId()));
    }

    private boolean scopeCovered(Set<DataScopeCode> owned, DataScopeCode requested, TUser target) {
        return scopeMatrixCovers(owned, requested, target != null && canManageAuthorization(target));
    }

    static boolean scopeMatrixCovers(Set<DataScopeCode> owned, DataScopeCode requested, boolean targetManaged) {
        if (owned == null || owned.isEmpty() || requested == null) return false;
        if (owned.contains(DataScopeCode.GLOBAL)) return true;
        if (requested == DataScopeCode.CUSTOM_ORGS) return false;
        if (requested == DataScopeCode.SELF) {
            return targetManaged && owned.stream().anyMatch(scope -> scope == DataScopeCode.DIRECT_REPORTS
                    || scope == DataScopeCode.REPORTING_TREE || scope == DataScopeCode.PRIMARY_ORG || scope == DataScopeCode.ORG_TREE);
        }
        return owned.contains(requested)
                || (requested == DataScopeCode.PRIMARY_ORG && owned.contains(DataScopeCode.ORG_TREE))
                || (requested == DataScopeCode.DIRECT_REPORTS && owned.contains(DataScopeCode.REPORTING_TREE));
    }

    private boolean isManagerOf(Integer managerId, Integer subordinateId, LocalDateTime now) {
        Deque<Integer> queue = new ArrayDeque<>(); Set<Integer> visited = new HashSet<>(); queue.add(subordinateId);
        while (!queue.isEmpty()) {
            Integer current = queue.removeFirst(); if (!visited.add(current)) continue;
            for (TEmployeeReporting relation : reportingMapper.selectEffectiveManagers(current, now)) {
                if (managerId.equals(relation.getManagerEmployeeId())) return true;
                queue.addLast(relation.getManagerEmployeeId());
            }
        }
        return false;
    }

    private boolean isDescendant(Integer descendant, Integer ancestor) {
        Set<Integer> visited = new HashSet<>(); Integer current = descendant;
        while (current != null && visited.add(current)) {
            if (current.equals(ancestor)) return true;
            TOrganizationUnit unit = organizationMapper.selectByPrimaryKey(current);
            current = unit == null ? null : unit.getParentId();
        }
        return false;
    }
}
