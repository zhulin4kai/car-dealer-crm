package com.autodealer.crm.service.impl;

import com.autodealer.crm.audit.AuditActionEnum;
import com.autodealer.crm.audit.AuthorizationAuditRecorder;
import com.autodealer.crm.audit.OperationAuditRecorder;
import com.autodealer.crm.config.security.CurrentUserProvider;
import com.autodealer.crm.dto.credential.CredentialDtos.ManagedDeliveryResult;
import com.autodealer.crm.dto.user.ManagedUserDtos.CreateRequest;
import com.autodealer.crm.dto.user.ManagedUserDtos.CreateResult;
import com.autodealer.crm.enums.*;
import com.autodealer.crm.exception.BusinessException;
import com.autodealer.crm.mapper.*;
import com.autodealer.crm.model.*;
import com.autodealer.crm.result.CodeEnum;
import com.autodealer.crm.service.CredentialService;
import com.autodealer.crm.service.ManagedUserAccountService;
import com.autodealer.crm.service.ManagedUserInvitationService;
import com.autodealer.crm.util.PhoneNormalizer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class ManagedUserInvitationServiceImpl implements ManagedUserInvitationService {
    private final TUserMapper users;
    private final TLoginIdentifierMapper loginIdentifiers;
    private final TEmployeeMapper employees;
    private final TEmployeeAssignmentMapper assignments;
    private final TEmployeeReportingMapper reporting;
    private final TOrganizationUnitMapper organizations;
    private final TPositionMapper positions;
    private final TRoleMapper roles;
    private final TUserRoleMapper userRoles;
    private final CurrentUserProvider current;
    private final UserAuthorizationPolicy policy;
    private final PasswordEncoder encoder;
    private final CredentialService credentials;
    private final ManagedUserAccountService accountService;
    private final OperationAuditRecorder audit;
    private final AuthorizationAuditRecorder authorizationAudit;
    private final ObjectMapper objectMapper;
    private final TAuthorizationGraphLockMapper graphLock;
    private final DirectManagerPolicy directManagerPolicy;
    @Value("${security.user-management-bootstrap-gate.enabled:true}")
    private boolean bootstrapGateEnabled;

    public ManagedUserInvitationServiceImpl(TUserMapper users, TLoginIdentifierMapper loginIdentifiers,
                                            TEmployeeMapper employees,
                                            TEmployeeAssignmentMapper assignments,
                                            TEmployeeReportingMapper reporting,
                                            TOrganizationUnitMapper organizations,
                                            TPositionMapper positions, TRoleMapper roles,
                                            TUserRoleMapper userRoles, CurrentUserProvider current,
                                            UserAuthorizationPolicy policy, PasswordEncoder encoder,
                                            CredentialService credentials,
                                            ManagedUserAccountService accountService,
                                            OperationAuditRecorder audit,
                                            AuthorizationAuditRecorder authorizationAudit,
                                            ObjectMapper objectMapper,
                                            TAuthorizationGraphLockMapper graphLock,
                                            DirectManagerPolicy directManagerPolicy) {
        this.users = users;
        this.loginIdentifiers = loginIdentifiers;
        this.employees = employees;
        this.assignments = assignments;
        this.reporting = reporting;
        this.organizations = organizations;
        this.positions = positions;
        this.roles = roles;
        this.userRoles = userRoles;
        this.current = current;
        this.policy = policy;
        this.encoder = encoder;
        this.credentials = credentials;
        this.accountService = accountService;
        this.audit = audit;
        this.authorizationAudit = authorizationAudit;
        this.objectMapper = objectMapper;
        this.graphLock = graphLock;
        this.directManagerPolicy = directManagerPolicy;
    }

    @Override
    @Transactional
    public CreateResult create(CreateRequest request) {
        String loginAct = request.getLoginAct().trim().toLowerCase(Locale.ROOT);
        String employeeNo = request.getEmployeeNo().trim();
        String name = request.getName().trim();
        String phone = PhoneNormalizer.normalizeMainlandMobile(request.getPhone());
        if (phone != null && !PhoneNormalizer.isMainlandMobile(phone)) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "手机号格式不正确");
        }
        String email = request.getEmail() == null || request.getEmail().isBlank()
                ? null : request.getEmail().trim().toLowerCase(Locale.ROOT);
        if (!"LOGIN_IDENTIFIER_GUARD".equals(graphLock.lockByName("LOGIN_IDENTIFIER_GUARD"))) {
            throw new IllegalStateException("登录标识图锁缺失");
        }
        lockGraph("AUTHORIZATION_MEMBERSHIP_GUARD");
        lockGraph("ORGANIZATION_HIERARCHY");
        lockGraph("REPORTING_GRAPH");
        lockGraph("AVAILABLE_ADMIN_GUARD");
        if (users.selectByLoginAct(loginAct) != null || loginIdentifiers.selectByLoginActForUpdate(loginAct) != null
                || employees.selectByEmployeeNo(employeeNo) != null) {
            throw new BusinessException(CodeEnum.DUPLICATE, "登录账号或工号已存在");
        }
        TOrganizationUnit organization = organizations.selectByPrimaryKey(request.getOrganizationUnitId());
        TPosition position = positions.selectByPrimaryKey(request.getPositionId());
        validateOrganizationAndPosition(organization, position);

        LinkedHashSet<Integer> roleIds = new LinkedHashSet<>(request.getRoleIds());
        if (roleIds.size() != request.getRoleIds().size()) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "初始角色不能重复");
        }
        validateBootstrapRequest(request, organization, roleIds);

        Integer operator = current.getCurrentUserId();
        LocalDateTime now = LocalDateTime.now();
        TUser user = buildUser(loginAct, name, phone, email, operator);
        try {
            if (users.insert(user) != 1) {
                throw new BusinessException(CodeEnum.OPERATION_FAILED, "账号创建失败");
            }
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(CodeEnum.DUPLICATE, "账号、手机号或邮箱已存在");
        }
        TLoginIdentifier identifier = new TLoginIdentifier();
        identifier.setUserId(user.getId());
        identifier.setLoginAct(loginAct);
        identifier.setChangedBy(operator);
        identifier.setReason("账号邀请创建");
        identifier.setCreateTime(now);
        try {
            if (loginIdentifiers.insert(identifier) != 1) {
                throw new BusinessException(CodeEnum.OPERATION_FAILED, "登录账号永久归属写入失败");
            }
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(CodeEnum.DUPLICATE, "登录账号已被当前或历史用户占用");
        }

        TEmployee employee = buildEmployee(user.getId(), employeeNo, name, phone, email, operator, now);
        if (employees.insert(employee) != 1) {
            throw new BusinessException(CodeEnum.OPERATION_FAILED, "员工档案创建失败");
        }
        TEmployeeAssignment initialAssignment = buildAssignment(employee.getId(), request, operator, now);
        if (assignments.insert(initialAssignment) != 1) {
            throw new BusinessException(CodeEnum.OPERATION_FAILED, "员工任职创建失败");
        }
        if (request.isBootstrapRootLeader()
                && organizations.assignInitialRootLeader(organization.getId(), employee.getId(),
                request.getExpectedRootOrganizationVersion(), operator, now) != 1) {
            throw new BusinessException(CodeEnum.ORGANIZATION_VERSION_CONFLICT, "根公司负责人已被其他初始化请求设置，请刷新后重试");
        }
        TEmployee manager = createReportingIfPresent(employee.getId(), request.getOrganizationUnitId(),
                request.getManagerEmployeeId(), operator, now);

        List<TAuthorizationHistory> organizationHistories = new ArrayList<>();
        organizationHistories.add(initialAssignmentHistory(user.getId(), employee.getId(), initialAssignment,
                organization, position, now));
        if (manager != null) organizationHistories.add(initialReportingHistory(user.getId(), employee.getId(), manager, now));
        authorizationAudit.recordAll(organizationHistories, AuditActionEnum.EMPLOYEE_ASSIGNMENT_CHANGE,
                String.valueOf(user.getId()), "{\"command\":\"INITIAL_ASSIGNMENT\"}");

        if (!request.isBootstrapRootLeader()) policy.requireManage(user);
        List<TAuthorizationHistory> histories = assignInitialRoles(user, roleIds, operator, now,
                request.isBootstrapRootLeader());
        if (!histories.isEmpty()) {
            authorizationAudit.recordAll(histories, AuditActionEnum.USER_ROLE_CHANGE,
                    String.valueOf(user.getId()),
                    "{\"command\":\"INITIAL_ROLE_ASSIGN\",\"count\":" + histories.size() + "}");
        }
        audit.record(AuditActionEnum.USER_CREATE, String.valueOf(user.getId()), "SUCCESS",
                request.isBootstrapRootLeader()
                        ? "{\"command\":\"BOOTSTRAP_ROOT_LEADER\"}"
                        : "{\"command\":\"INVITE_CREATE\"}");
        var detail = accountService.getDetail(user.getId());
        ManagedDeliveryResult delivery = credentials.issueInvitation(user.getId(),
                request.isBootstrapRootLeader() ? "首次根公司负责人初始化" : "账号邀请创建");
        if (!delivery.accepted()) {
            throw new BusinessException(CodeEnum.CREDENTIAL_DELIVERY_FAILED, "邀请凭证未成功投递");
        }
        return new CreateResult(detail, delivery);
    }

    private void validateBootstrapRequest(CreateRequest request, TOrganizationUnit organization,
                                          Set<Integer> roleIds) {
        int availableAdminCount = users.countAdminUsers();
        if (bootstrapGateEnabled && availableAdminCount == 0 && !request.isBootstrapRootLeader()) {
            throw new BusinessException(CodeEnum.ADMIN_BOOTSTRAP_REQUIRED,
                    "必须显式执行首次根公司负责人初始化");
        }
        if (!request.isBootstrapRootLeader()) return;
        TUser operator = current.getCurrentUser();
        if (availableAdminCount != 0
                || operator == null
                || !Objects.equals(operator.getId(), 1)
                || !"admin".equals(operator.getLoginAct())
                || operator.getAccountType() != AccountType.SYSTEM
                || !Boolean.TRUE.equals(operator.getProtectedAccount())) {
            throw new BusinessException(CodeEnum.ACCESS_DENIED,
                    "只有固定受保护恢复账号可以初始化首个根公司负责人");
        }
        if (request.getManagerEmployeeId() != null) {
            throw new BusinessException(CodeEnum.INVALID_MANAGER, "根公司负责人初始化时不得设置直属管理者");
        }
        if (request.getExpectedRootOrganizationVersion() == null) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "首次初始化必须携带根公司版本");
        }
        List<TOrganizationUnit> roots = organizations.selectRoots();
        if (roots.size() != 1
                || !Objects.equals(roots.get(0).getId(), organization.getId())
                || organization.getType() != OrganizationUnitType.COMPANY
                || organization.getParentId() != null
                || organization.getLeaderEmployeeId() != null
                || !Objects.equals(organization.getVersion(), request.getExpectedRootOrganizationVersion())) {
            throw new BusinessException(CodeEnum.ORGANIZATION_HIERARCHY_INVALID,
                    "首次初始化要求唯一启用且尚未设置负责人的根公司");
        }
        List<TRole> initialRoles = roleIds.stream().map(roles::selectByPrimaryKey)
                .filter(Objects::nonNull).toList();
        boolean onlyAdminRole = roleIds.size() == 1 && initialRoles.size() == 1
                && "admin".equals(initialRoles.get(0).getRole())
                && initialRoles.get(0).getEnabled() != null && initialRoles.get(0).getEnabled() == 1
                && Boolean.TRUE.equals(initialRoles.get(0).getProtectedRole());
        if (!onlyAdminRole) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "首次根公司负责人只能授予受保护管理员角色");
        }
    }

    private void lockGraph(String name) {
        if (!name.equals(graphLock.lockByName(name))) {
            throw new IllegalStateException(name + " 图锁缺失");
        }
    }

    private void validateOrganizationAndPosition(TOrganizationUnit organization, TPosition position) {
        if (organization == null || position == null
                || !Boolean.TRUE.equals(organization.getEnabled())
                || !Boolean.TRUE.equals(position.getEnabled())
                || Boolean.TRUE.equals(organization.getMigrationPlaceholder())
                || "UNASSIGNED_POSITION".equals(position.getCode())) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "组织或岗位不可用");
        }
    }

    private TUser buildUser(String loginAct, String name, String phone, String email, Integer operator) {
        TUser user = new TUser();
        user.setLoginAct(loginAct);
        user.setLoginPwd(encoder.encode(UUID.randomUUID().toString()));
        user.setName(name);
        user.setPhone(phone);
        user.setEmail(email);
        user.setAccountNoExpired(1);
        user.setCredentialsNoExpired(1);
        user.setAccountNoLocked(1);
        user.setAccountEnabled(0);
        user.setAccountType(AccountType.HUMAN);
        user.setProtectedAccount(false);
        user.setVersion(0);
        user.setAuthorizationVersion(0);
        user.setAuthVersion(0L);
        user.setSessionRevision(0L);
        user.setAccountStatus(AccountStatus.INVITED);
        user.setMustChangePassword(true);
        user.setFailedLoginCount(0);
        user.setManualLocked(false);
        user.setCreateTime(new Date());
        user.setCreateBy(operator);
        return user;
    }

    private TEmployee buildEmployee(Integer userId, String employeeNo, String name,
                                    String phone, String email, Integer operator,
                                    LocalDateTime now) {
        TEmployee employee = new TEmployee();
        employee.setUserId(userId);
        employee.setEmployeeNo(employeeNo);
        employee.setName(name);
        employee.setPhone(phone);
        employee.setEmail(email);
        employee.setEmploymentStatus(EmployeeStatus.PENDING);
        employee.setProfileCompleted(false);
        employee.setVersion(0);
        employee.setProfileVersion(0);
        employee.setPhoneVerified(false);
        employee.setEmailVerified(false);
        employee.setCreateTime(now);
        employee.setCreateBy(operator);
        return employee;
    }

    private TEmployeeAssignment buildAssignment(Integer employeeId, CreateRequest request,
                                                Integer operator, LocalDateTime now) {
        TEmployeeAssignment assignment = new TEmployeeAssignment();
        assignment.setEmployeeId(employeeId);
        assignment.setOrganizationUnitId(request.getOrganizationUnitId());
        assignment.setPositionId(request.getPositionId());
        assignment.setAssignmentType(AssignmentType.PRIMARY);
        assignment.setStatus(AssignmentStatus.ACTIVE);
        assignment.setActivePrimaryMarker(true);
        assignment.setEffectiveFrom(now);
        assignment.setReason("账号邀请创建");
        assignment.setVersion(0);
        assignment.setCreateTime(now);
        assignment.setCreateBy(operator);
        return assignment;
    }

    private TEmployee createReportingIfPresent(Integer employeeId, Integer organizationUnitId,
                                          Integer managerEmployeeId,
                                          Integer operator, LocalDateTime now) {
        TEmployee manager = directManagerPolicy.validate(employeeId, organizationUnitId,
                managerEmployeeId, now);
        if (manager == null) return null;
        TEmployeeReporting relation = new TEmployeeReporting();
        relation.setSubordinateEmployeeId(employeeId);
        relation.setManagerEmployeeId(manager.getId());
        relation.setRelationType(ReportingType.DIRECT);
        relation.setStatus(ReportingStatus.ACTIVE);
        relation.setActiveDirectMarker(true);
        relation.setEffectiveFrom(now);
        relation.setReason("账号邀请创建");
        relation.setVersion(0);
        relation.setCreateTime(now);
        relation.setCreateBy(operator);
        if (reporting.insert(relation) != 1) {
            throw new BusinessException(CodeEnum.OPERATION_FAILED, "汇报关系创建失败");
        }
        return manager;
    }

    private TAuthorizationHistory initialAssignmentHistory(Integer userId, Integer employeeId,
                                                            TEmployeeAssignment assignment,
                                                            TOrganizationUnit organization,
                                                            TPosition position, LocalDateTime now) {
        TAuthorizationHistory history = new TAuthorizationHistory();
        history.setSubjectType(AuthorizationSubjectType.ORGANIZATION_ASSIGNMENT);
        history.setSubjectId(String.valueOf(employeeId));
        history.setTargetUserId(userId);
        history.setChangeType(AuthorizationChangeType.CREATE);
        history.setAfterValue(json(Map.of(
                "organizationUnit", Map.of("id", organization.getId(), "code", organization.getCode(), "name", organization.getName()),
                "position", Map.of("id", position.getId(), "code", position.getCode(), "name", position.getName()),
                "assignmentType", assignment.getAssignmentType().name(), "status", assignment.getStatus().name())));
        history.setReason("账号邀请创建");
        history.setEffectiveFrom(now);
        return history;
    }

    private TAuthorizationHistory initialReportingHistory(Integer userId, Integer employeeId,
                                                           TEmployee manager, LocalDateTime now) {
        TAuthorizationHistory history = new TAuthorizationHistory();
        history.setSubjectType(AuthorizationSubjectType.REPORTING_RELATION);
        history.setSubjectId(String.valueOf(employeeId));
        history.setTargetUserId(userId);
        history.setChangeType(AuthorizationChangeType.CREATE);
        history.setAfterValue(json(Map.of("manager", Map.of("id", manager.getId(),
                "code", manager.getEmployeeNo(), "name", manager.getName()),
                "relationType", ReportingType.DIRECT.name())));
        history.setReason("账号邀请创建");
        history.setEffectiveFrom(now);
        return history;
    }

    private List<TAuthorizationHistory> assignInitialRoles(TUser user, Set<Integer> roleIds,
                                                           Integer operator, LocalDateTime now,
                                                           boolean bootstrap) {
        List<TAuthorizationHistory> histories = new ArrayList<>();
        for (Integer roleId : roleIds) {
            TRole role = roles.selectByPrimaryKey(roleId);
            if (!bootstrap && !policy.canDelegateRole(role, user)) {
                throw new BusinessException(CodeEnum.ACCESS_DENIED, "角色超过委派上限");
            }
            TUserRole value = new TUserRole();
            value.setUserId(user.getId());
            value.setRoleId(roleId);
            value.setGrantedBy(operator);
            value.setReason("账号邀请创建");
            value.setEffectiveFrom(now);
            value.setActiveMarker(true);
            value.setVersion(0);
            if (userRoles.insert(value) != 1) {
                throw new BusinessException(CodeEnum.OPERATION_FAILED, "初始角色创建失败");
            }
            TAuthorizationHistory history = new TAuthorizationHistory();
            history.setSubjectType(AuthorizationSubjectType.USER_ROLE);
            history.setSubjectId(user.getId() + ":" + roleId);
            history.setTargetUserId(user.getId());
            history.setRoleId(roleId);
            history.setChangeType(AuthorizationChangeType.ASSIGN);
            history.setAfterValue(roleSnapshot(role));
            history.setReason("账号邀请创建");
            history.setEffectiveFrom(now);
            histories.add(history);
        }
        return histories;
    }

    private String roleSnapshot(TRole role) {
        return json(Map.of(
                "roleId", role.getId(),
                "roleCode", role.getRole(),
                "roleName", role.getRoleName()
        ));
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("初始用户历史快照序列化失败", exception);
        }
    }
}
