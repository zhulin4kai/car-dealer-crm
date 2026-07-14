package com.autodealer.crm.service.impl;

import com.autodealer.crm.audit.AuditActionEnum;
import com.autodealer.crm.audit.OperationAuditRecorder;
import com.autodealer.crm.config.security.CurrentUserProvider;
import com.autodealer.crm.constant.PermissionCodes;
import com.autodealer.crm.dto.user.ManagedUserDtos.Detail;
import com.autodealer.crm.dto.user.ManagedUserDtos.FilterOption;
import com.autodealer.crm.dto.user.ManagedUserDtos.FilterOptions;
import com.autodealer.crm.dto.user.ManagedUserDtos.ProfileRequest;
import com.autodealer.crm.dto.user.ManagedUserDtos.LoginAccountRequest;
import com.autodealer.crm.dto.user.ManagedUserDtos.SecurityExpirationRequest;
import com.autodealer.crm.dto.user.ManagedUserDtos.RoleNameRow;
import com.autodealer.crm.dto.user.ManagedUserDtos.StatusCommandOption;
import com.autodealer.crm.dto.user.ManagedUserDtos.StatusRequest;
import com.autodealer.crm.dto.user.ManagedUserDtos.Summary;
import com.autodealer.crm.dto.UserListQuery;
import com.autodealer.crm.enums.AccountType;
import com.autodealer.crm.enums.AccountStatus;
import com.autodealer.crm.enums.EmployeeStatus;
import com.autodealer.crm.enums.OrganizationUnitType;
import com.autodealer.crm.exception.BusinessException;
import com.autodealer.crm.mapper.TEmployeeAssignmentMapper;
import com.autodealer.crm.mapper.TEmployeeMapper;
import com.autodealer.crm.mapper.TEmployeeReportingMapper;
import com.autodealer.crm.mapper.TAuthorizationGraphLockMapper;
import com.autodealer.crm.mapper.TOrganizationUnitMapper;
import com.autodealer.crm.mapper.TPositionMapper;
import com.autodealer.crm.mapper.TRoleMapper;
import com.autodealer.crm.mapper.TUserMapper;
import com.autodealer.crm.mapper.TLoginIdentifierMapper;
import com.autodealer.crm.model.TEmployee;
import com.autodealer.crm.model.TEmployeeAssignment;
import com.autodealer.crm.model.TEmployeeReporting;
import com.autodealer.crm.model.TOrganizationUnit;
import com.autodealer.crm.model.TPosition;
import com.autodealer.crm.model.TUser;
import com.autodealer.crm.model.TLoginIdentifier;
import com.autodealer.crm.result.CodeEnum;
import com.autodealer.crm.service.ManagedUserAccountService;
import com.autodealer.crm.service.AuthorizationDataScope;
import com.autodealer.crm.service.DataScopeResolver;
import com.autodealer.crm.service.CredentialService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.autodealer.crm.util.PhoneNormalizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Locale;
import java.util.Objects;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class ManagedUserAccountServiceImpl implements ManagedUserAccountService {
    private static final ObjectMapper AUDIT_JSON = new ObjectMapper().findAndRegisterModules();
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 100;
    private static final List<String> ALL_ACTIONS = List.of(
            "VIEW", "PROFILE_UPDATE", "ASSIGNMENT_UPDATE", "AUTHORIZATION_VIEW",
            "AUTHORIZATION_UPDATE", "ACCOUNT_IDENTITY_UPDATE", "SECURITY_EXPIRATION_UPDATE",
            "STATUS_UPDATE", "PASSWORD_RESET", "SESSION_VIEW",
            "SESSION_REVOKE", "HISTORY_VIEW", "TRANSFER", "DEPARTURE", "REHIRE",
            "REINVITE", "HANDOVER"
    );
    private static final Map<String, String> SORT_COLUMNS = Map.of(
            "employeeNo", "employee.employee_no",
            "name", "COALESCE(employee.name, user_account.name)",
            "loginAct", "user_account.login_act",
            "employmentStatus", "employment_status",
            "accountStatus", "user_account.account_status",
            "lockStatus", "lock_status",
            "lastLoginTime", "user_account.last_login_time"
    );
    private static final Set<String> EMPLOYMENT_STATUSES = Set.of(
            "PENDING", "ACTIVE", "HANDOVER", "LEFT", "NOT_APPLICABLE"
    );
    private static final Set<String> ACCOUNT_STATUSES = Set.of("INVITED", "ACTIVE", "DISABLED");
    private static final Set<String> LOCK_STATUSES = Set.of("UNLOCKED", "AUTO_LOCKED", "MANUAL_LOCKED");
    private final TUserMapper users;
    private final TLoginIdentifierMapper loginIdentifiers;
    private final TEmployeeMapper employees;
    private final TEmployeeAssignmentMapper assignments;
    private final TEmployeeReportingMapper reporting;
    private final TOrganizationUnitMapper organizations;
    private final TPositionMapper positions;
    private final TRoleMapper roles;
    private final CurrentUserProvider current;
    private final UserAuthorizationPolicy policy;
    private final OperationAuditRecorder audit;
    private final DataScopeResolver dataScopeResolver;
    private final TAuthorizationGraphLockMapper graphLock;
    private final CredentialService credentialService;
    private final DirectManagerPolicy directManagerPolicy;
    private final UserSecurityMutationCoordinator securityMutations;
    @Value("${security.user-management-bootstrap-gate.enabled:true}")
    private boolean bootstrapGateEnabled;

    public ManagedUserAccountServiceImpl(TUserMapper users, TLoginIdentifierMapper loginIdentifiers,
                                         TEmployeeMapper employees,
                                         TEmployeeAssignmentMapper assignments,
                                         TEmployeeReportingMapper reporting,
                                         TOrganizationUnitMapper organizations,
                                         TPositionMapper positions, TRoleMapper roles,
                                         CurrentUserProvider current, UserAuthorizationPolicy policy,
                                         OperationAuditRecorder audit,
                                         DataScopeResolver dataScopeResolver,
                                         TAuthorizationGraphLockMapper graphLock,
                                         CredentialService credentialService,
                                         DirectManagerPolicy directManagerPolicy,
                                         UserSecurityMutationCoordinator securityMutations) {
        this.users = users;
        this.loginIdentifiers = loginIdentifiers;
        this.employees = employees;
        this.assignments = assignments;
        this.reporting = reporting;
        this.organizations = organizations;
        this.positions = positions;
        this.roles = roles;
        this.current = current;
        this.policy = policy;
        this.audit = audit;
        this.dataScopeResolver = dataScopeResolver;
        this.graphLock = graphLock;
        this.credentialService = credentialService;
        this.directManagerPolicy = directManagerPolicy;
        this.securityMutations = securityMutations;
    }

    @Override
    public PageInfo<Summary> list(UserListQuery query) {
        prepareQuery(query);
        int page = query.getCurrent() == null || query.getCurrent() < 1 ? 1 : query.getCurrent();
        int pageSize = query.getPageSize() == null || query.getPageSize() < 1
                ? DEFAULT_PAGE_SIZE : query.getPageSize();
        if (pageSize > MAX_PAGE_SIZE) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "分页大小不能超过100");
        }
        PageHelper.startPage(page, pageSize);
        List<Summary> summaries = users.selectManagedUserPage(query);
        PageInfo<Summary> pageInfo = new PageInfo<>(summaries);
        populateRoleNames(summaries);
        summaries.forEach(this::populateSummaryActions);
        pageInfo.setList(summaries);
        return pageInfo;
    }

    @Override
    public FilterOptions getFilterOptions(Integer organizationUnitId) {
        UserListQuery scopeQuery = new UserListQuery();
        applyDataScope(scopeQuery);
        FilterOptions result = new FilterOptions();
        result.setOrganizations(createOrganizationOptions());
        result.setPositions(createPositionOptions());
        result.setManagers(organizationUnitId == null
                ? users.selectVisibleManagerOptions(scopeQuery)
                : directManagerPolicy.candidates(null, organizationUnitId, LocalDateTime.now()).stream()
                .map(this::managerOption).toList());
        result.setRoles(users.selectVisibleRoleOptions(scopeQuery));
        result.setAssignableRoles(policy.assignableRoleCandidates(organizationUnitId).stream().map(role -> {
            FilterOption option = new FilterOption();
            option.setId(role.getId());
            option.setLabel(role.getRoleName());
            return option;
        }).toList());
        result.setEmploymentStatuses(statusOptions(Map.of(
                "PENDING", "待入职", "ACTIVE", "在职", "HANDOVER", "待交接",
                "LEFT", "已离职", "NOT_APPLICABLE", "不适用"
        )));
        result.setAccountStatuses(statusOptions(Map.of(
                "INVITED", "待激活", "ACTIVE", "启用", "DISABLED", "禁用"
        )));
        result.setLockStatuses(statusOptions(Map.of(
                "UNLOCKED", "未锁定", "AUTO_LOCKED", "自动锁定", "MANUAL_LOCKED", "人工锁定"
        )));
        populateBootstrapOptions(result);
        return result;
    }

    private FilterOption managerOption(TEmployee employee) {
        FilterOption option = new FilterOption();
        option.setId(employee.getId());
        option.setLabel(employee.getName() + "（" + employee.getEmployeeNo() + "）");
        return option;
    }

    private List<FilterOption> createOrganizationOptions() {
        Set<Integer> allowedIds = null;
        if (!policy.isGlobalOperator() && !policy.isBootstrapRecoveryOperator()) {
            TEmployee operator = employees.selectByUserId(current.getCurrentUserId());
            TEmployeeAssignment primary = operator == null ? null
                    : assignments.selectCurrentPrimaryByEmployeeId(operator.getId(), LocalDateTime.now());
            allowedIds = primary == null ? Set.of()
                    : Set.copyOf(organizations.selectDescendantIds(primary.getOrganizationUnitId()));
        }
        Set<Integer> effectiveAllowedIds = allowedIds;
        return organizations.selectAll().stream()
                .filter(item -> Boolean.TRUE.equals(item.getEnabled()))
                .filter(item -> !Boolean.TRUE.equals(item.getMigrationPlaceholder()))
                .filter(item -> effectiveAllowedIds == null || effectiveAllowedIds.contains(item.getId()))
                .map(item -> filterOption(item.getId(), item.getName()))
                .toList();
    }

    private List<FilterOption> createPositionOptions() {
        return positions.selectAll().stream()
                .filter(item -> Boolean.TRUE.equals(item.getEnabled()))
                .filter(item -> !"UNASSIGNED_POSITION".equals(item.getCode()))
                .map(item -> filterOption(item.getId(), item.getName()))
                .toList();
    }

    private FilterOption filterOption(Object id, String label) {
        FilterOption option = new FilterOption();
        option.setId(id);
        option.setLabel(label);
        return option;
    }

    private void populateBootstrapOptions(FilterOptions result) {
        boolean required = bootstrapGateEnabled && users.countAdminUsers() == 0;
        result.setBootstrapRequired(required);
        if (!required) return;
        TUser operator = current.getCurrentUser();
        List<TOrganizationUnit> roots = organizations.selectRoots();
        boolean recovery = operator != null
                && Objects.equals(operator.getId(), 1)
                && "admin".equals(operator.getLoginAct())
                && operator.getAccountType() == AccountType.SYSTEM
                && Boolean.TRUE.equals(operator.getProtectedAccount());
        if (recovery && roots.size() == 1) {
            TOrganizationUnit root = roots.get(0);
            boolean availableRoot = root.getType() == OrganizationUnitType.COMPANY
                    && root.getParentId() == null
                    && root.getLeaderEmployeeId() == null
                    && Boolean.TRUE.equals(root.getEnabled())
                    && !Boolean.TRUE.equals(root.getMigrationPlaceholder());
            result.setBootstrapAllowed(availableRoot);
            if (availableRoot) {
                result.setBootstrapRootOrganizationId(root.getId());
                result.setBootstrapRootOrganizationVersion(root.getVersion());
            }
        }
    }

    @Override
    public Detail getDetail(Integer userId) {
        TUser user = requireViewableUser(userId);
        return detail(user);
    }

    @Override
    @Transactional
    public Detail changeStatus(Integer userId, StatusRequest request) {
        boolean invalidatesManagerEligibility = "DISABLE".equals(request.getCommand())
                || "LOCK".equals(request.getCommand());
        if (invalidatesManagerEligibility) {
            lockGraph("REPORTING_GRAPH");
            lockGraph("AVAILABLE_ADMIN_GUARD");
        }
        TUser user = requireManagedUser(userId);
        TEmployee employee = employees.selectByUserId(userId);
        if ("ENABLE".equals(request.getCommand()) && employee != null
                && employee.getEmploymentStatus() == EmployeeStatus.LEFT) {
            throw new BusinessException(CodeEnum.USER_LIFECYCLE_CONFLICT, "已离职员工必须通过返聘重新启用");
        }
        if (!Objects.equals(user.getVersion(), request.getAccountVersion())) {
            throw new BusinessException(CodeEnum.ACCOUNT_VERSION_CONFLICT, "账号版本冲突");
        }
        validateStatusCommand(user, request.getCommand());
        Map<String, Object> beforeState = accountAuditState(user);
        String reason = request.getReason().trim();
        boolean changed = switch (request.getCommand()) {
            case "LOCK" -> users.updateManualLockByExpected(userId, request.getAccountVersion(), true,
                    reason, current.getCurrentUserId(), LocalDateTime.now()) == 1;
            case "UNLOCK" -> users.updateManualLockByExpected(userId, request.getAccountVersion(), false,
                    reason, current.getCurrentUserId(), null) == 1;
            case "DISABLE" -> users.updateAccountStatusByExpected(userId, request.getAccountVersion(),
                    AccountStatus.DISABLED.name(), false, current.getCurrentUserId()) == 1;
            case "ENABLE" -> users.updateAccountStatusByExpected(userId, request.getAccountVersion(),
                    AccountStatus.ACTIVE.name(), true, current.getCurrentUserId()) == 1;
            default -> throw new BusinessException(CodeEnum.PARAM_ERROR, "未知账号状态命令");
        };
        if (!changed) throw new BusinessException(CodeEnum.ACCOUNT_VERSION_CONFLICT, "账号版本冲突");
        if ("DISABLE".equals(request.getCommand())) credentialService.revokeAll(userId);
        securityMutations.accessChanged(userId, "账号状态变化");
        AuditActionEnum auditAction = "LOCK".equals(request.getCommand()) || "UNLOCK".equals(request.getCommand())
                ? AuditActionEnum.USER_MANUAL_LOCK_CHANGE : AuditActionEnum.USER_STATUS_CHANGE;
        TUser afterUser = users.selectByPrimaryKey(userId);
        audit.record(auditAction, String.valueOf(userId), "SUCCESS", auditJson(Map.of(
                "command", request.getCommand(), "reason", reason,
                "before", beforeState, "after", accountAuditState(afterUser))));
        return detail(afterUser);
    }

    @Override
    @Transactional
    public Detail updateProfile(Integer userId, ProfileRequest request) {
        lockGraph("AVAILABLE_ADMIN_GUARD");
        TUser user = requireManagedUser(userId);
        user = users.selectByPrimaryKeyForUpdate(userId);
        if (user == null) throw new BusinessException(CodeEnum.NOT_FOUND, "账号不存在");
        TEmployee employee = employees.selectByUserIdForUpdate(userId);
        if (employee == null) throw new BusinessException(CodeEnum.NOT_FOUND, "员工档案不存在");

        String phone = PhoneNormalizer.normalizeMainlandMobile(request.getPhone());
        if (phone != null && !PhoneNormalizer.isMainlandMobile(phone)) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "手机号格式不正确");
        }
        String email = request.getEmail() == null || request.getEmail().isBlank()
                ? null : request.getEmail().trim().toLowerCase(Locale.ROOT);
        if (phone != null && employees.selectByPhoneExcludeUserId(phone, userId) != null) {
            throw new BusinessException(CodeEnum.DUPLICATE, "手机号已存在");
        }
        if (email != null && employees.selectByEmailExcludeUserId(email, userId) != null) {
            throw new BusinessException(CodeEnum.DUPLICATE, "邮箱已存在");
        }

        String oldName = employee.getName();
        String oldPhone = employee.getPhone();
        String oldEmail = employee.getEmail();
        boolean oldPhoneVerified = Boolean.TRUE.equals(employee.getPhoneVerified());
        boolean oldEmailVerified = Boolean.TRUE.equals(employee.getEmailVerified());
        boolean keepsVerifiedPhone = oldPhoneVerified && Objects.equals(phone, oldPhone) && phone != null;
        boolean keepsVerifiedEmail = oldEmailVerified && Objects.equals(email, oldEmail) && email != null;
        if ((oldPhoneVerified || oldEmailVerified) && !keepsVerifiedPhone && !keepsVerifiedEmail
                && isLastAvailableAdmin(userId)) {
            throw new BusinessException(CodeEnum.LAST_AVAILABLE_ADMIN_REQUIRED,
                    "最后一个可恢复普通管理员必须保留至少一个已验证联系方式");
        }
        employee.setName(request.getName().trim());
        employee.setPhone(phone);
        employee.setEmail(email);
        employee.setPhoneVerified(Objects.equals(phone, oldPhone) && Boolean.TRUE.equals(employee.getPhoneVerified()));
        employee.setEmailVerified(Objects.equals(email, oldEmail) && Boolean.TRUE.equals(employee.getEmailVerified()));
        employee.setProfileCompleted(true);
        employee.setEditTime(LocalDateTime.now());
        employee.setEditBy(current.getCurrentUserId());
        try {
            if (employees.updateProfileByVersion(employee, request.getProfileVersion()) != 1) {
                throw new BusinessException(CodeEnum.PROFILE_VERSION_CONFLICT, "个人资料版本冲突");
            }
            if (users.updateProfileProjection(userId, employee.getName(), phone, email,
                    current.getCurrentUserId()) != 1) {
                throw new BusinessException(CodeEnum.OPERATION_FAILED, "账号资料投影同步失败");
            }
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(CodeEnum.DUPLICATE, "手机号或邮箱已存在");
        }
        List<String> changed = new ArrayList<>();
        if (!Objects.equals(oldName, employee.getName())) changed.add("NAME");
        if (!Objects.equals(oldPhone, phone)) changed.add("PHONE");
        if (!Objects.equals(oldEmail, email)) changed.add("EMAIL");
        if (!Objects.equals(oldPhone, phone) || !Objects.equals(oldEmail, email)) credentialService.revokeAll(userId);
        if (!Objects.equals(oldName, employee.getName())) securityMutations.ownerEligibilityChanged();
        audit.record(AuditActionEnum.USER_PROFILE_UPDATE, String.valueOf(userId), "SUCCESS", auditJson(Map.of(
                "scope", "MANAGED_PROFILE", "changedFieldCodes", changed,
                "before", Map.of("name", oldName, "phoneChanged", false, "emailChanged", false,
                        "phoneVerified", oldPhoneVerified, "emailVerified", oldEmailVerified),
                "after", Map.of("name", employee.getName(), "phoneChanged", !Objects.equals(oldPhone, phone),
                        "emailChanged", !Objects.equals(oldEmail, email),
                        "phoneVerified", Boolean.TRUE.equals(employee.getPhoneVerified()),
                        "emailVerified", Boolean.TRUE.equals(employee.getEmailVerified())))));
        return detail(users.selectByPrimaryKey(userId));
    }

    @Override
    @Transactional
    public Detail changeLoginAccount(Integer userId, LoginAccountRequest request) {
        TUser user = requireManagedUser(userId);
        requireAccountVersion(user, request.getAccountVersion());
        String loginAct = request.getLoginAct().trim().toLowerCase(Locale.ROOT);
        if (user.getLoginAct() != null && user.getLoginAct().equalsIgnoreCase(loginAct)) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "新登录账号必须与当前账号不同");
        }
        if (!"LOGIN_IDENTIFIER_GUARD".equals(graphLock.lockByName("LOGIN_IDENTIFIER_GUARD"))) {
            throw new IllegalStateException("登录标识图锁缺失");
        }
        if (users.selectByLoginActExcludeId(loginAct, userId) != null) {
            throw new BusinessException(CodeEnum.DUPLICATE, "登录账号已存在");
        }
        String reason = request.getReason().trim();
        TLoginIdentifier oldIdentifier;
        TLoginIdentifier requestedIdentifier;
        if (user.getLoginAct().compareToIgnoreCase(loginAct) <= 0) {
            oldIdentifier = loginIdentifiers.selectByLoginActForUpdate(user.getLoginAct());
            requestedIdentifier = loginIdentifiers.selectByLoginActForUpdate(loginAct);
        } else {
            requestedIdentifier = loginIdentifiers.selectByLoginActForUpdate(loginAct);
            oldIdentifier = loginIdentifiers.selectByLoginActForUpdate(user.getLoginAct());
        }
        if (oldIdentifier == null || !Objects.equals(oldIdentifier.getUserId(), userId)
                || !"ACTIVE".equals(oldIdentifier.getStatus()) || !Integer.valueOf(1).equals(oldIdentifier.getActiveMarker())) {
            throw new BusinessException(CodeEnum.OPERATION_FAILED, "当前登录账号永久归属事实缺失或冲突");
        }
        if (requestedIdentifier != null && !Objects.equals(requestedIdentifier.getUserId(), userId)) {
            throw new BusinessException(CodeEnum.DUPLICATE, "登录账号已被当前或历史用户占用");
        }
        LocalDateTime changedAt = LocalDateTime.now();
        try {
            if (loginIdentifiers.retireByExpected(oldIdentifier.getId(), oldIdentifier.getVersion(), changedAt,
                    current.getCurrentUserId(), reason) != 1) {
                throw new BusinessException(CodeEnum.ACCOUNT_VERSION_CONFLICT, "登录账号归属版本冲突");
            }
            if (requestedIdentifier == null) {
                TLoginIdentifier created = new TLoginIdentifier();
                created.setUserId(userId);
                created.setLoginAct(loginAct);
                created.setChangedBy(current.getCurrentUserId());
                created.setReason(reason);
                created.setCreateTime(changedAt);
                if (loginIdentifiers.insert(created) != 1) {
                    throw new BusinessException(CodeEnum.OPERATION_FAILED, "新登录账号归属写入失败");
                }
            } else if (!"RETIRED".equals(requestedIdentifier.getStatus())
                    || loginIdentifiers.reactivateByExpected(requestedIdentifier.getId(), requestedIdentifier.getVersion(),
                    current.getCurrentUserId(), reason) != 1) {
                throw new BusinessException(CodeEnum.ACCOUNT_VERSION_CONFLICT, "历史登录账号归属版本冲突");
            }
            if (users.updateLoginActByExpected(userId, request.getAccountVersion(), loginAct,
                    current.getCurrentUserId()) != 1) {
                throw new BusinessException(CodeEnum.ACCOUNT_VERSION_CONFLICT, "账号版本冲突");
            }
            credentialService.revokeAll(userId);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(CodeEnum.DUPLICATE, "登录账号已被当前或历史用户占用");
        }
        securityMutations.authenticationChanged(userId, "登录账号变化");
        audit.record(AuditActionEnum.USER_LOGIN_ACCOUNT_CHANGE, String.valueOf(userId), "SUCCESS",
                auditJson(Map.of("reason", reason, "before", Map.of("loginAct", user.getLoginAct()),
                        "after", Map.of("loginAct", loginAct))));
        return detail(users.selectByPrimaryKey(userId));
    }

    @Override
    @Transactional
    public Detail changeSecurityExpiration(Integer userId, SecurityExpirationRequest request) {
        LocalDateTime accountExpiresAt = local(request.getAccountExpiresAt());
        LocalDateTime credentialExpiresAt = local(request.getCredentialExpiresAt());
        boolean accessWillExpire = accountExpiresAt != null || credentialExpiresAt != null;
        if (accessWillExpire) {
            lockGraph("REPORTING_GRAPH");
            lockGraph("AVAILABLE_ADMIN_GUARD");
        }
        TUser user = requireManagedUser(userId);
        requireAccountVersion(user, request.getAccountVersion());
        if (accessWillExpire && isLastAvailableAdmin(userId)) {
            throw new BusinessException(CodeEnum.ACCESS_DENIED, "不能使最后一个有效管理员的账号或凭证到期");
        }
        LocalDateTime now = LocalDateTime.now();
        int accountNoExpired = accountExpiresAt == null || accountExpiresAt.isAfter(now) ? 1 : 0;
        int credentialsNoExpired = credentialExpiresAt == null || credentialExpiresAt.isAfter(now) ? 1 : 0;
        String reason = request.getReason().trim();
        Map<String, Object> before = securityExpirationAuditState(user);
        if (users.updateSecurityExpirationByExpected(userId, request.getAccountVersion(), accountNoExpired,
                accountExpiresAt,
                credentialsNoExpired, credentialExpiresAt, current.getCurrentUserId()) != 1) {
            throw new BusinessException(CodeEnum.ACCOUNT_VERSION_CONFLICT, "账号版本冲突");
        }
        credentialService.revokeAll(userId);
        securityMutations.authenticationChanged(userId, "账号安全到期变化");
        TUser after = users.selectByPrimaryKey(userId);
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("reason", reason);
        summary.put("before", before);
        summary.put("after", securityExpirationAuditState(after));
        audit.record(AuditActionEnum.USER_SECURITY_EXPIRATION_CHANGE, String.valueOf(userId), "SUCCESS",
                auditJson(summary));
        return detail(after);
    }

    private static void requireAccountVersion(TUser user, Integer expectedVersion) {
        if (!Objects.equals(user.getVersion(), expectedVersion)) {
            throw new BusinessException(CodeEnum.ACCOUNT_VERSION_CONFLICT, "账号版本冲突");
        }
    }

    private TUser requireManagedUser(Integer userId) {
        TUser user = users.selectByPrimaryKey(userId);
        if (user == null) throw new BusinessException(CodeEnum.NOT_FOUND, "用户不存在");
        policy.requireManage(user);
        return user;
    }

    private static Map<String, Object> accountAuditState(TUser user) {
        String account = user.getAccountStatus() == null ? AccountStatus.ACTIVE.name() : user.getAccountStatus().name();
        String lock = Boolean.TRUE.equals(user.getManualLocked()) ? "MANUAL_LOCKED"
                : user.getAutoLockedUntil() != null && user.getAutoLockedUntil().isAfter(LocalDateTime.now())
                ? "AUTO_LOCKED" : "UNLOCKED";
        return Map.of("accountStatus", account, "lockStatus", lock);
    }

    private static String auditJson(Object value) {
        try { return AUDIT_JSON.writeValueAsString(value); }
        catch (JsonProcessingException exception) { throw new IllegalStateException("用户审计摘要序列化失败", exception); }
    }

    private TUser requireViewableUser(Integer userId) {
        TUser user = users.selectByPrimaryKey(userId);
        if (user == null) {
            throw new BusinessException(CodeEnum.NOT_FOUND, "用户不存在");
        }
        policy.requireView(user);
        return user;
    }

    private Detail detail(TUser user) {
        Detail detail = new Detail();
        detail.setId(user.getId());
        detail.setLoginAct(user.getLoginAct());
        detail.setName(user.getName());
        detail.setPhone(user.getPhone());
        detail.setEmail(user.getEmail());
        detail.setAccountStatus(user.getAccountStatus() == null
                ? AccountStatus.ACTIVE.name() : user.getAccountStatus().name());
        detail.setLockStatus(Boolean.TRUE.equals(user.getManualLocked()) ? "MANUAL_LOCKED"
                : user.getAutoLockedUntil() != null && user.getAutoLockedUntil().isAfter(LocalDateTime.now())
                ? "AUTO_LOCKED" : "UNLOCKED");
        detail.setLockReason(user.getManualLockReason());
        detail.setAccountExpired(!user.isAccountNonExpired());
        detail.setAccountExpiresAt(offset(user.getAccountExpiresAt()));
        detail.setCredentialExpired(!Integer.valueOf(1).equals(user.getCredentialsNoExpired())
                || user.getPasswordExpiresAt() != null && !user.getPasswordExpiresAt().isAfter(LocalDateTime.now()));
        detail.setCredentialExpiresAt(offset(user.getPasswordExpiresAt()));
        detail.setLastLoginTime(user.getLastLoginTime());
        detail.setAccountVersion(user.getVersion());
        detail.setAuthorizationVersion(user.getAuthorizationVersion());
        detail.setSessionRevision(user.getSessionRevision());
        detail.setProfileVersion(user.getProfileVersion() == null ? 0 : user.getProfileVersion());
        detail.setEmployeeVersion(0);

        TEmployee employee = employees.selectByUserId(user.getId());
        if (employee == null) {
            detail.setEmploymentStatus("NOT_APPLICABLE");
            detail.setRoleNames(roles.selectByUserId(user.getId()).stream().map(role -> role.getRoleName()).toList());
            populateDetailActions(detail, user, null);
            applySensitiveProjection(detail, user);
            return detail;
        }
        detail.setEmployeeId(employee.getId());
        detail.setEmployeeNo(employee.getEmployeeNo());
        detail.setName(employee.getName());
        detail.setPhone(employee.getPhone());
        detail.setEmail(employee.getEmail());
        detail.setEmploymentStatus(employee.getEmploymentStatus() == null ? null : employee.getEmploymentStatus().name());
        detail.setEmployeeVersion(employee.getVersion());
        detail.setProfileVersion(employee.getProfileVersion());

        LocalDateTime now = LocalDateTime.now();
        TEmployeeAssignment primary = assignments.selectCurrentPrimaryByEmployeeId(employee.getId(), now);
        if (primary != null) {
            TOrganizationUnit organization = organizations.selectByPrimaryKey(primary.getOrganizationUnitId());
            TPosition position = positions.selectByPrimaryKey(primary.getPositionId());
            detail.setOrganizationName(organization == null ? null : organization.getName());
            detail.setPositionName(position == null ? null : position.getName());
        }
        TEmployeeReporting managerRelation = reporting.selectCurrentDirectBySubordinateId(employee.getId(), now);
        if (managerRelation != null) {
            TEmployee manager = employees.selectByPrimaryKey(managerRelation.getManagerEmployeeId());
            detail.setManagerName(manager == null ? null : manager.getName());
        }
        detail.setRoleNames(roles.selectByUserId(user.getId()).stream().map(role -> role.getRoleName()).toList());
        populateDetailActions(detail, user, employee);
        applySensitiveProjection(detail, user);
        return detail;
    }

    private void prepareQuery(UserListQuery query) {
        if (query == null) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "查询条件不能为空");
        }
        query.setKeyword(trimToNull(query.getKeyword()));
        validateCode(query.getEmploymentStatus(), EMPLOYMENT_STATUSES, "任职状态");
        validateCode(query.getAccountStatus(), ACCOUNT_STATUSES, "账号状态");
        validateCode(query.getLockStatus(), LOCK_STATUSES, "锁定状态");
        String sortBy = trimToNull(query.getSortBy());
        if (sortBy == null) sortBy = "employeeNo";
        String sortColumn = SORT_COLUMNS.get(sortBy);
        if (sortColumn == null) throw new BusinessException(CodeEnum.PARAM_ERROR, "排序字段不在白名单");
        String direction = trimToNull(query.getSortDirection());
        if (direction == null) direction = "asc";
        if (!"asc".equalsIgnoreCase(direction) && !"desc".equalsIgnoreCase(direction)) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "排序方向不合法");
        }
        query.setSortBy(sortBy);
        query.setSortColumn(sortColumn);
        query.setSortOrder(direction.toUpperCase(Locale.ROOT));
        applyDataScope(query);
    }

    private void applyDataScope(UserListQuery query) {
        AuthorizationDataScope scope = dataScopeResolver.resolve(current.getCurrentUserId(), PermissionCodes.USER_LIST);
        query.setDataScopeVisibleUserIds(null);
        if (scope.global()) {
            query.setDataScopeDenied(false);
            return;
        }
        List<Integer> manageableIds = scope.visibleUserIds().stream()
                .map(users::selectByPrimaryKey)
                .filter(Objects::nonNull)
                .filter(policy::canManage)
                .map(TUser::getId)
                .toList();
        query.setDataScopeDenied(manageableIds.isEmpty());
        if (!manageableIds.isEmpty()) query.setDataScopeVisibleUserIds(new ArrayList<>(manageableIds));
    }

    private void populateRoleNames(List<Summary> summaries) {
        if (summaries.isEmpty()) return;
        List<Integer> userIds = summaries.stream().map(Summary::getId).toList();
        Map<Integer, List<String>> roleNamesByUser = new LinkedHashMap<>();
        for (RoleNameRow row : users.selectRoleNamesByUserIds(userIds, LocalDateTime.now())) {
            roleNamesByUser.computeIfAbsent(row.getUserId(), ignored -> new ArrayList<>()).add(row.getRoleName());
        }
        summaries.forEach(summary -> summary.setRoleNames(
                roleNamesByUser.getOrDefault(summary.getId(), List.of())
        ));
    }

    private void populateSummaryActions(Summary summary) {
        TUser target = new TUser();
        target.setId(summary.getId());
        target.setAccountType(summary.getAccountType() == null
                ? AccountType.HUMAN : AccountType.valueOf(summary.getAccountType()));
        target.setProtectedAccount(summary.getProtectedAccount());
        populateActions(summary.getAllowedActions(), summary.getUnavailableReasons(), target,
                summary.getEmploymentStatus(), summary.getAccountStatus());
    }

    private void populateDetailActions(Detail detail, TUser target, TEmployee employee) {
        String employmentStatus = employee == null || employee.getEmploymentStatus() == null
                ? "NOT_APPLICABLE" : employee.getEmploymentStatus().name();
        populateActions(detail.getAllowedActions(), detail.getUnavailableReasons(), target,
                employmentStatus, detail.getAccountStatus());
        if (detail.getAllowedActions().contains("STATUS_UPDATE")) {
            detail.setStatusCommands(buildStatusCommands(target, employmentStatus));
        }
    }

    private void populateActions(List<String> allowedActions, Map<String, String> unavailableReasons,
                                 TUser target, String employmentStatus, String accountStatus) {
        boolean self = Objects.equals(target.getId(), current.getCurrentUserId());
        boolean manageable = policy.canManage(target);
        if (self) {
            allowedActions.add("VIEW");
            allowedActions.add("AUTHORIZATION_VIEW");
            fillUnavailableReasons(allowedActions, unavailableReasons,
                    "本人管理详情只读；个人资料、密码和会话请前往个人中心维护");
            return;
        }
        if (!manageable) {
            fillUnavailableReasons(allowedActions, unavailableReasons,
                    "目标用户不在管理链或组织范围内，或属于受保护账号");
            return;
        }
        allow(allowedActions, "VIEW", PermissionCodes.USER_VIEW);
        allow(allowedActions, "PROFILE_UPDATE", PermissionCodes.USER_EDIT);
        allow(allowedActions, "ACCOUNT_IDENTITY_UPDATE", PermissionCodes.USER_EDIT);
        allow(allowedActions, "ASSIGNMENT_UPDATE", PermissionCodes.EMPLOYEE_ASSIGNMENT);
        allow(allowedActions, "AUTHORIZATION_VIEW", PermissionCodes.USER_VIEW);
        if (!"LEFT".equals(employmentStatus)
                && (current.hasAuthority(PermissionCodes.USER_ROLE) || current.hasAuthority(PermissionCodes.USER_PERMISSION))) {
            allowedActions.add("AUTHORIZATION_UPDATE");
        }
        allow(allowedActions, "STATUS_UPDATE", PermissionCodes.USER_STATUS);
        allow(allowedActions, "SECURITY_EXPIRATION_UPDATE", PermissionCodes.USER_STATUS);
        if (!"LEFT".equals(employmentStatus)) allow(allowedActions, "PASSWORD_RESET", PermissionCodes.USER_PASSWORD);
        allow(allowedActions, "SESSION_VIEW", PermissionCodes.USER_VIEW);
        allow(allowedActions, "SESSION_REVOKE", PermissionCodes.USER_STATUS);
        allow(allowedActions, "HISTORY_VIEW", PermissionCodes.AUDIT_OPERATION_DETAIL);
        if (current.hasAuthority(PermissionCodes.EMPLOYEE_ASSIGNMENT) && "ACTIVE".equals(employmentStatus)) {
            allowedActions.add("TRANSFER");
        }
        if (current.hasAuthority(PermissionCodes.EMPLOYEE_ASSIGNMENT)
                && ("ACTIVE".equals(employmentStatus) || "HANDOVER".equals(employmentStatus))) {
            allowedActions.add("DEPARTURE");
        }
        if (current.hasAuthority(PermissionCodes.EMPLOYEE_ASSIGNMENT) && "LEFT".equals(employmentStatus)) {
            allowedActions.add("REHIRE");
        }
        if (current.hasAuthority(PermissionCodes.USER_ADD) && "INVITED".equals(accountStatus)) {
            allowedActions.add("REINVITE");
        }
        if (current.hasAuthority(PermissionCodes.USER_STATUS) && "HANDOVER".equals(employmentStatus)) {
            allowedActions.add("HANDOVER");
        }
        fillUnavailableReasons(allowedActions, unavailableReasons, "当前权限、管理关系或目标状态不允许该操作");
    }

    private void allow(List<String> actions, String action, String permissionCode) {
        if (current.hasAuthority(permissionCode)) actions.add(action);
    }

    private void fillUnavailableReasons(List<String> allowedActions, Map<String, String> reasons, String reason) {
        ALL_ACTIONS.stream().filter(action -> !allowedActions.contains(action))
                .forEach(action -> reasons.put(action, reason));
    }

    private List<StatusCommandOption> buildStatusCommands(TUser user, String employmentStatus) {
        String status = user.getAccountStatus() == null ? "ACTIVE" : user.getAccountStatus().name();
        boolean manualLocked = Boolean.TRUE.equals(user.getManualLocked());
        boolean lastAdmin = isLastAvailableAdmin(user.getId());
        List<StatusCommandOption> commands = new ArrayList<>();
        commands.add(new StatusCommandOption("ENABLE", "启用账号", false,
                "LEFT".equals(employmentStatus) ? "已离职员工必须通过返聘重新启用"
                        : "DISABLED".equals(status) ? null : "仅禁用账号可以启用"));
        commands.add(new StatusCommandOption("DISABLE", "禁用账号", true,
                "DISABLED".equals(status) ? "账号已经禁用" : lastAdmin ? "不能禁用最后一个有效管理员" : null));
        commands.add(new StatusCommandOption("LOCK", "人工锁定", true,
                manualLocked ? "账号已经处于人工锁定" : lastAdmin ? "不能锁定最后一个有效管理员" : null));
        commands.add(new StatusCommandOption("UNLOCK", "解除人工锁定", false,
                manualLocked ? null : "账号当前没有人工锁定"));
        return commands;
    }

    private void validateStatusCommand(TUser user, String command) {
        TEmployee employee = employees.selectByUserId(user.getId());
        String employmentStatus = employee == null ? "NOT_APPLICABLE" : employee.getEmploymentStatus().name();
        StatusCommandOption option = buildStatusCommands(user, employmentStatus).stream()
                .filter(value -> value.getCommand().equals(command))
                .findFirst()
                .orElseThrow(() -> new BusinessException(CodeEnum.PARAM_ERROR, "未知账号状态命令"));
        if (option.getDisabledReason() != null) {
            throw new BusinessException(CodeEnum.ACCESS_DENIED, option.getDisabledReason());
        }
    }

    private boolean isLastAvailableAdmin(Integer userId) {
        boolean admin = roles.selectByUserId(userId).stream().anyMatch(role -> "admin".equals(role.getRole()));
        return admin && users.countAvailableAdminUsersExcluding(userId) == 0;
    }

    private static Map<String, Object> securityExpirationAuditState(TUser user) {
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("accountExpired", !user.isAccountNonExpired());
        state.put("accountExpiresAt", user.getAccountExpiresAt());
        state.put("credentialExpired", !Integer.valueOf(1).equals(user.getCredentialsNoExpired())
                || user.getPasswordExpiresAt() != null && !user.getPasswordExpiresAt().isAfter(LocalDateTime.now()));
        state.put("credentialExpiresAt", user.getPasswordExpiresAt());
        return state;
    }

    private static LocalDateTime local(OffsetDateTime value) {
        return value == null ? null : value.atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
    }

    private static OffsetDateTime offset(LocalDateTime value) {
        return value == null ? null : value.atZone(ZoneId.systemDefault()).toOffsetDateTime();
    }

    private void applySensitiveProjection(Detail detail, TUser target) {
        boolean visible = !Objects.equals(target.getId(), current.getCurrentUserId())
                && policy.canManage(target) && current.hasAuthority(PermissionCodes.USER_SENSITIVE_VIEW);
        if (!visible) {
            detail.setPhone(null);
            detail.setEmail(null);
            detail.setLockReason(null);
        }
    }

    private List<FilterOption> statusOptions(Map<String, String> values) {
        return values.entrySet().stream().sorted(Map.Entry.comparingByKey()).map(entry -> {
            FilterOption option = new FilterOption();
            option.setId(entry.getKey());
            option.setLabel(entry.getValue());
            return option;
        }).toList();
    }

    private void validateCode(String value, Set<String> allowed, String label) {
        if (value != null && !value.isBlank() && !allowed.contains(value)) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, label + "不合法");
        }
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }

    private void lockGraph(String name) {
        if (!name.equals(graphLock.lockByName(name))) {
            throw new IllegalStateException("用户账号图锁缺失: " + name);
        }
    }

}
