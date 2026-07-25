package com.autodealer.crm.modules.identity.application.internal;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.autodealer.crm.modules.audit.application.api.AuditActionEnum;
import com.autodealer.crm.modules.identity.application.api.AuthorizationAuditRecorder;
import com.autodealer.crm.modules.identity.application.api.OrganizationService;
import com.autodealer.crm.modules.identity.application.api.dto.organization.ActingReportingCollectionResponse;
import com.autodealer.crm.modules.identity.application.api.dto.organization.ActingReportingInput;
import com.autodealer.crm.modules.identity.application.api.dto.organization.ActingReportingRelationResponse;
import com.autodealer.crm.modules.identity.application.api.dto.organization.AssignmentInput;
import com.autodealer.crm.modules.identity.application.api.dto.organization.ChangeEntityStatusRequest;
import com.autodealer.crm.modules.identity.application.api.dto.organization.CreateOrganizationUnitRequest;
import com.autodealer.crm.modules.identity.application.api.dto.organization.CreatePositionRequest;
import com.autodealer.crm.modules.identity.application.api.dto.organization.EmployeeAssignmentResponse;
import com.autodealer.crm.modules.identity.application.api.dto.organization.EmployeeOrganizationMembershipResponse;
import com.autodealer.crm.modules.identity.application.api.dto.organization.EmployeeReportingResponse;
import com.autodealer.crm.modules.identity.application.api.dto.organization.EmployeeSummaryResponse;
import com.autodealer.crm.modules.identity.application.api.dto.organization.ManagerCandidateResponse;
import com.autodealer.crm.modules.identity.application.api.dto.organization.OrganizationChangeHistoryResponse;
import com.autodealer.crm.modules.identity.application.api.dto.organization.OrganizationUnitResponse;
import com.autodealer.crm.modules.identity.application.api.dto.organization.PositionResponse;
import com.autodealer.crm.modules.identity.application.api.dto.organization.ReplaceActingReportingsRequest;
import com.autodealer.crm.modules.identity.application.api.dto.organization.ReportingInput;
import com.autodealer.crm.modules.identity.application.api.dto.organization.UpdateEmployeeOrganizationRequest;
import com.autodealer.crm.modules.identity.application.api.dto.organization.UpdateOrganizationUnitRequest;
import com.autodealer.crm.modules.identity.application.api.dto.organization.UpdatePositionRequest;
import com.autodealer.crm.modules.identity.application.api.enums.AccountType;
import com.autodealer.crm.modules.identity.application.api.enums.AssignmentStatus;
import com.autodealer.crm.modules.identity.application.api.enums.AssignmentType;
import com.autodealer.crm.modules.identity.application.api.enums.AuthorizationChangeType;
import com.autodealer.crm.modules.identity.application.api.enums.AuthorizationSubjectType;
import com.autodealer.crm.modules.identity.application.api.enums.EmployeeStatus;
import com.autodealer.crm.modules.identity.application.api.enums.OrganizationUnitType;
import com.autodealer.crm.modules.identity.application.api.enums.ReportingStatus;
import com.autodealer.crm.modules.identity.application.api.enums.ReportingType;
import com.autodealer.crm.modules.identity.application.api.model.TUser;
import com.autodealer.crm.modules.identity.application.api.security.CurrentUserProvider;
import com.autodealer.crm.modules.identity.persistence.mapper.TAuthorizationGraphLockMapper;
import com.autodealer.crm.modules.identity.persistence.mapper.TAuthorizationHistoryMapper;
import com.autodealer.crm.modules.identity.persistence.mapper.TEmployeeAssignmentMapper;
import com.autodealer.crm.modules.identity.persistence.mapper.TEmployeeMapper;
import com.autodealer.crm.modules.identity.persistence.mapper.TEmployeeReportingMapper;
import com.autodealer.crm.modules.identity.persistence.mapper.TOrganizationUnitMapper;
import com.autodealer.crm.modules.identity.persistence.mapper.TPositionMapper;
import com.autodealer.crm.modules.identity.persistence.mapper.TUserMapper;
import com.autodealer.crm.modules.identity.persistence.model.TAuthorizationHistory;
import com.autodealer.crm.modules.identity.persistence.model.TEmployee;
import com.autodealer.crm.modules.identity.persistence.model.TEmployeeAssignment;
import com.autodealer.crm.modules.identity.persistence.model.TEmployeeReporting;
import com.autodealer.crm.modules.identity.persistence.model.TOrganizationUnit;
import com.autodealer.crm.modules.identity.persistence.model.TPosition;
import com.autodealer.crm.shared.error.BusinessException;
import com.autodealer.crm.shared.error.CodeEnum;
import com.autodealer.crm.shared.security.PermissionCodes;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Service
public class OrganizationServiceImpl implements OrganizationService {
    private static final ZoneId BUSINESS_ZONE = ZoneId.systemDefault();

    private final TOrganizationUnitMapper organizationUnitMapper;
    private final TPositionMapper positionMapper;
    private final TEmployeeMapper employeeMapper;
    private final TEmployeeAssignmentMapper assignmentMapper;
    private final TEmployeeReportingMapper reportingMapper;
    private final TAuthorizationHistoryMapper historyMapper;
    private final TAuthorizationGraphLockMapper graphLockMapper;
    private final TUserMapper userMapper;
    private final AuthorizationAuditRecorder authorizationAuditRecorder;
    private final CurrentUserProvider currentUserProvider;
    private final ObjectMapper objectMapper;
    private final DirectManagerPolicy directManagerPolicy;
    private final UserAuthorizationPolicy authorizationPolicy;
    private final UserSecurityMutationCoordinator securityMutations;

    public OrganizationServiceImpl(TOrganizationUnitMapper organizationUnitMapper,
                                   TPositionMapper positionMapper,
                                   TEmployeeMapper employeeMapper,
                                   TEmployeeAssignmentMapper assignmentMapper,
                                   TEmployeeReportingMapper reportingMapper,
                                   TAuthorizationHistoryMapper historyMapper,
                                   TAuthorizationGraphLockMapper graphLockMapper,
                                   TUserMapper userMapper,
                                   AuthorizationAuditRecorder authorizationAuditRecorder,
                                   CurrentUserProvider currentUserProvider,
                                   ObjectMapper objectMapper,
                                   DirectManagerPolicy directManagerPolicy,
                                   UserAuthorizationPolicy authorizationPolicy,
                                   UserSecurityMutationCoordinator securityMutations) {
        this.organizationUnitMapper = organizationUnitMapper;
        this.positionMapper = positionMapper;
        this.employeeMapper = employeeMapper;
        this.assignmentMapper = assignmentMapper;
        this.reportingMapper = reportingMapper;
        this.historyMapper = historyMapper;
        this.graphLockMapper = graphLockMapper;
        this.userMapper = userMapper;
        this.authorizationAuditRecorder = authorizationAuditRecorder;
        this.currentUserProvider = currentUserProvider;
        this.objectMapper = objectMapper;
        this.directManagerPolicy = directManagerPolicy;
        this.authorizationPolicy = authorizationPolicy;
        this.securityMutations = securityMutations;
    }

    @Override
    public List<OrganizationUnitResponse> getOrganizationTree() {
        LocalDateTime now = LocalDateTime.now();
        List<TOrganizationUnit> units = visibleOrganizationUnits(now);
        Map<Integer, OrganizationUnitResponse> responses = new LinkedHashMap<>();
        for (TOrganizationUnit unit : units) {
            OrganizationUnitResponse response = toOrganizationResponse(unit, now);
            responses.put(unit.getId(), response);
        }
        List<OrganizationUnitResponse> roots = new ArrayList<>();
        for (OrganizationUnitResponse response : responses.values()) {
            OrganizationUnitResponse parent = responses.get(response.getParentId());
            if (parent == null) roots.add(response);
            else parent.getChildren().add(response);
        }
        sortTree(roots);
        return roots;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrganizationUnitResponse createOrganizationUnit(CreateOrganizationUnitRequest request) {
        lockOrganizationHierarchy();
        if (organizationUnitMapper.selectByCode(request.getCode()) != null) {
            throw new BusinessException(CodeEnum.DUPLICATE, "组织编码已存在");
        }
        LocalDateTime now = LocalDateTime.now();
        if (request.getParentId() == null) requireGlobalOrganizationScope();
        else requireOrganizationReadScope(request.getParentId(), now);
        if (request.getType() == OrganizationUnitType.COMPANY && request.getParentId() == null) {
            ensureNoEnabledRoot(null);
            if (request.getLeaderEmployeeId() != null) {
                throw new BusinessException(CodeEnum.ORGANIZATION_HIERARCHY_INVALID,
                        "根公司负责人只能通过首次初始化或独立负责人交接命令设置");
            }
        }
        validateParent(request.getType(), request.getParentId(), null);
        validateLeader(request.getLeaderEmployeeId(), null, request.getParentId());
        TOrganizationUnit unit = new TOrganizationUnit();
        unit.setCode(request.getCode());
        unit.setName(request.getName());
        unit.setType(request.getType());
        unit.setParentId(request.getParentId());
        unit.setLeaderEmployeeId(request.getLeaderEmployeeId());
        unit.setOrderNo(request.getOrderNo());
        unit.setPlaceholder(false);
        unit.setEnabled(true);
        unit.setVersion(0);
        unit.setCreateTime(now);
        unit.setCreateBy(currentUserProvider.getCurrentUserId());
        try {
            if (organizationUnitMapper.insert(unit) != 1) throw operationFailed("创建组织失败");
        } catch (DuplicateKeyException e) {
            throw new BusinessException(CodeEnum.DUPLICATE, "组织编码已存在", e);
        }
        recordCatalogHistory(unit.getId(), AuthorizationSubjectType.ORGANIZATION_UNIT,
                AuthorizationChangeType.CREATE, null, organizationSnapshot(unit), "创建组织",
                AuditActionEnum.ORGANIZATION_CATALOG_CHANGE);
        return toOrganizationResponse(unit, now);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrganizationUnitResponse updateOrganizationUnit(Integer id, UpdateOrganizationUnitRequest request) {
        lockOrganizationHierarchy();
        TOrganizationUnit existing = requireOrganization(id);
        LocalDateTime now = LocalDateTime.now();
        requireOrganizationReadScope(id, now);
        if (request.getParentId() == null) requireGlobalOrganizationScope();
        else requireOrganizationReadScope(request.getParentId(), now);
        requireVersion(existing.getVersion(), request.getExpectedVersion(), CodeEnum.ORGANIZATION_VERSION_CONFLICT);
        boolean existingRoot = isRootCompany(existing);
        boolean requestedRoot = request.getType() == OrganizationUnitType.COMPANY && request.getParentId() == null;
        if (existingRoot != requestedRoot) {
            throw new BusinessException(CodeEnum.ORGANIZATION_HIERARCHY_INVALID,
                    "根公司不得移动、改类型，普通组织也不得转换为根公司");
        }
        if (existingRoot && !Objects.equals(existing.getLeaderEmployeeId(), request.getLeaderEmployeeId())) {
            throw new BusinessException(CodeEnum.ORGANIZATION_HIERARCHY_INVALID,
                    "根公司负责人不能通过通用组织编辑修改");
        }
        if (requestedRoot) ensureNoEnabledRoot(id);
        validateParent(request.getType(), request.getParentId(), id);
        validateChildrenCompatible(id, request.getType());
        validateLeader(request.getLeaderEmployeeId(), id, request.getParentId());
        TOrganizationUnit update = copyOrganization(existing);
        update.setName(request.getName());
        update.setType(request.getType());
        update.setParentId(request.getParentId());
        update.setLeaderEmployeeId(request.getLeaderEmployeeId());
        update.setOrderNo(request.getOrderNo());
        update.setEditTime(LocalDateTime.now());
        update.setEditBy(currentUserProvider.getCurrentUserId());
        if (organizationUnitMapper.updateByIdAndVersion(update, request.getExpectedVersion()) != 1) {
            throw new BusinessException(CodeEnum.ORGANIZATION_VERSION_CONFLICT);
        }
        update.setVersion(request.getExpectedVersion() + 1);
        recordCatalogHistory(id, AuthorizationSubjectType.ORGANIZATION_UNIT,
                AuthorizationChangeType.UPDATE, organizationSnapshot(existing), organizationSnapshot(update),
                "编辑组织", AuditActionEnum.ORGANIZATION_CATALOG_CHANGE);
        securityMutations.ownerEligibilityChanged();
        return toOrganizationResponse(update, LocalDateTime.now());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrganizationUnitResponse changeOrganizationUnitStatus(
            Integer id, ChangeEntityStatusRequest request, boolean enabled) {
        lockOrganizationHierarchy();
        TOrganizationUnit existing = requireOrganization(id);
        requireOrganizationReadScope(id, LocalDateTime.now());
        requireVersion(existing.getVersion(), request.getExpectedVersion(), CodeEnum.ORGANIZATION_VERSION_CONFLICT);
        if (Objects.equals(existing.getEnabled(), enabled)) return toOrganizationResponse(existing, LocalDateTime.now());
        if (!enabled && isRootCompany(existing)) {
            throw new BusinessException(CodeEnum.ORGANIZATION_HIERARCHY_INVALID, "根公司不得停用");
        }
        if (enabled && isRootCompany(existing)) ensureNoEnabledRoot(id);
        if (!enabled) validateOrganizationCanDisable(id);
        else if (existing.getParentId() != null && !Boolean.TRUE.equals(requireOrganization(existing.getParentId()).getEnabled())) {
            throw new BusinessException(CodeEnum.ORGANIZATION_HIERARCHY_INVALID, "上级组织未启用");
        }
        TOrganizationUnit update = copyOrganization(existing);
        update.setEnabled(enabled);
        update.setEditTime(LocalDateTime.now());
        update.setEditBy(currentUserProvider.getCurrentUserId());
        if (organizationUnitMapper.updateByIdAndVersion(update, request.getExpectedVersion()) != 1) {
            throw new BusinessException(CodeEnum.ORGANIZATION_VERSION_CONFLICT);
        }
        update.setVersion(request.getExpectedVersion() + 1);
        recordCatalogHistory(id, AuthorizationSubjectType.ORGANIZATION_UNIT,
                enabled ? AuthorizationChangeType.ENABLE : AuthorizationChangeType.DISABLE,
                organizationSnapshot(existing), organizationSnapshot(update), request.getReason(),
                AuditActionEnum.ORGANIZATION_CATALOG_CHANGE);
        securityMutations.ownerEligibilityChanged();
        return toOrganizationResponse(update, LocalDateTime.now());
    }

    @Override
    public List<OrganizationUnitResponse> getParentCandidates(OrganizationUnitType type, Integer excludeId) {
        if (type == OrganizationUnitType.COMPANY) return List.of();
        LocalDateTime now = LocalDateTime.now();
        return visibleOrganizationUnits(now).stream()
                .filter(unit -> Boolean.TRUE.equals(unit.getEnabled()))
                .filter(unit -> supportsChild(unit.getType(), type))
                .filter(unit -> excludeId == null || !unit.getId().equals(excludeId))
                .filter(unit -> excludeId == null || !isDescendant(unit.getId(), excludeId))
                .map(unit -> toOrganizationResponse(unit, now))
                .toList();
    }

    @Override
    public List<ManagerCandidateResponse> getLeaderCandidates(Integer organizationUnitId, Integer parentId) {
        LocalDateTime now = LocalDateTime.now();
        Integer anchorId = organizationUnitId != null ? organizationUnitId : parentId;
        if (anchorId != null) requireOrganizationReadScope(anchorId, now);
        List<Integer> organizationIds = candidateOrganizationIds(anchorId, now);
        if (organizationIds != null && organizationIds.isEmpty()) return List.of();
        return employeeMapper.selectEligibleManagerCandidates(-1, now, organizationIds).stream()
                .map(this::toManagerCandidate).toList();
    }

    @Override
    public List<PositionResponse> getPositions() {
        return positionMapper.selectManageable().stream().map(this::toPositionResponse).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PositionResponse createPosition(CreatePositionRequest request) {
        requireGlobalOrganizationScope();
        if (positionMapper.selectByCode(request.getCode()) != null) {
            throw new BusinessException(CodeEnum.DUPLICATE, "岗位编码已存在");
        }
        LocalDateTime now = LocalDateTime.now();
        TPosition position = new TPosition();
        position.setCode(request.getCode());
        position.setName(request.getName());
        position.setDescription(request.getDescription());
        position.setPositionLevel(request.getPositionLevel());
        position.setBuiltIn(false);
        position.setEnabled(true);
        position.setVersion(0);
        position.setCreateTime(now);
        position.setCreateBy(currentUserProvider.getCurrentUserId());
        try {
            if (positionMapper.insert(position) != 1) throw operationFailed("创建岗位失败");
        } catch (DuplicateKeyException e) {
            throw new BusinessException(CodeEnum.DUPLICATE, "岗位编码已存在", e);
        }
        recordCatalogHistory(position.getId(), AuthorizationSubjectType.POSITION,
                AuthorizationChangeType.CREATE, null, positionSnapshot(position), "创建岗位",
                AuditActionEnum.POSITION_CHANGE);
        return toPositionResponse(position);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PositionResponse updatePosition(Integer id, UpdatePositionRequest request) {
        requireGlobalOrganizationScope();
        TPosition existing = requirePosition(id);
        if (Boolean.TRUE.equals(existing.getBuiltIn())) throw new BusinessException(CodeEnum.ACCESS_DENIED, "内置岗位不能编辑");
        requireVersion(existing.getVersion(), request.getExpectedVersion(), CodeEnum.ORGANIZATION_VERSION_CONFLICT);
        TPosition update = copyPosition(existing);
        update.setName(request.getName());
        update.setDescription(request.getDescription());
        update.setPositionLevel(request.getPositionLevel());
        update.setEditTime(LocalDateTime.now());
        update.setEditBy(currentUserProvider.getCurrentUserId());
        if (positionMapper.updateByIdAndVersion(update, request.getExpectedVersion()) != 1) {
            throw new BusinessException(CodeEnum.ORGANIZATION_VERSION_CONFLICT);
        }
        update.setVersion(request.getExpectedVersion() + 1);
        recordCatalogHistory(id, AuthorizationSubjectType.POSITION, AuthorizationChangeType.UPDATE,
                positionSnapshot(existing), positionSnapshot(update), "编辑岗位", AuditActionEnum.POSITION_CHANGE);
        securityMutations.ownerEligibilityChanged();
        return toPositionResponse(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PositionResponse changePositionStatus(Integer id, ChangeEntityStatusRequest request, boolean enabled) {
        requireGlobalOrganizationScope();
        TPosition existing = requirePosition(id);
        if (Boolean.TRUE.equals(existing.getBuiltIn())) throw new BusinessException(CodeEnum.ACCESS_DENIED, "内置岗位不能调整状态");
        requireVersion(existing.getVersion(), request.getExpectedVersion(), CodeEnum.ORGANIZATION_VERSION_CONFLICT);
        if (Objects.equals(existing.getEnabled(), enabled)) return toPositionResponse(existing);
        if (!enabled && positionMapper.countEffectiveAssignments(id, LocalDateTime.now()) > 0) {
            throw new BusinessException(CodeEnum.POSITION_IN_USE);
        }
        TPosition update = copyPosition(existing);
        update.setEnabled(enabled);
        update.setEditTime(LocalDateTime.now());
        update.setEditBy(currentUserProvider.getCurrentUserId());
        if (positionMapper.updateByIdAndVersion(update, request.getExpectedVersion()) != 1) {
            throw new BusinessException(CodeEnum.ORGANIZATION_VERSION_CONFLICT);
        }
        update.setVersion(request.getExpectedVersion() + 1);
        recordCatalogHistory(id, AuthorizationSubjectType.POSITION,
                enabled ? AuthorizationChangeType.ENABLE : AuthorizationChangeType.DISABLE,
                positionSnapshot(existing), positionSnapshot(update), request.getReason(), AuditActionEnum.POSITION_CHANGE);
        securityMutations.ownerEligibilityChanged();
        return toPositionResponse(update);
    }

    @Override
    public List<EmployeeSummaryResponse> getOrganizationEmployees(Integer organizationUnitId) {
        requireOrganization(organizationUnitId);
        LocalDateTime now = LocalDateTime.now();
        requireOrganizationReadScope(organizationUnitId, now);
        return employeeMapper.selectEffectiveByOrganizationUnitId(organizationUnitId, now).stream()
                .map(employee -> toEmployeeSummary(employee, now)).toList();
    }

    @Override
    public EmployeeOrganizationMembershipResponse getEmployeeOrganizationMembership(Integer employeeId) {
        TEmployee employee = requireEmployee(employeeId);
        requireCanViewEmployee(employee, LocalDateTime.now());
        return buildMembership(employee, LocalDateTime.now());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EmployeeOrganizationMembershipResponse updateEmployeeOrganizationMembership(
            Integer employeeId, UpdateEmployeeOrganizationRequest request) {
        lockOrganizationHierarchy();
        if (!"REPORTING_GRAPH".equals(graphLockMapper.lockByName("REPORTING_GRAPH"))) {
            throw new IllegalStateException("汇报关系图锁缺失");
        }
        TEmployee employee = requireEmployee(employeeId);
        requireCanManageEmployee(employee, LocalDateTime.now());
        if (employee.getEmploymentStatus() == EmployeeStatus.LEFT) {
            throw new BusinessException(CodeEnum.ASSIGNMENT_CONFLICT, "已离职员工不能调整任职");
        }
        LocalDateTime now = LocalDateTime.now();
        EmployeeOrganizationMembershipResponse before = buildMembership(employee, now);
        requireVersion(employee.getVersion(), request.getExpectedVersion(), CodeEnum.ASSIGNMENT_CONFLICT);
        boolean assignmentChanged = assignmentsChanged(before, request);
        boolean reportingChanged = reportingChanged(before.getReporting(), request.getReporting());
        if (!assignmentChanged && !reportingChanged) return before;
        if (assignmentChanged && !currentUserProvider.hasAuthority(PermissionCodes.EMPLOYEE_ASSIGNMENT)) {
            throw new BusinessException(CodeEnum.ACCESS_DENIED, "没有员工任职调整权限");
        }
        if (reportingChanged && !currentUserProvider.hasAuthority(PermissionCodes.EMPLOYEE_REPORTING)) {
            throw new BusinessException(CodeEnum.ACCESS_DENIED, "没有汇报关系调整权限");
        }
        validateMembershipRequest(employeeId, request, now, assignmentChanged, reportingChanged);
        Integer operatorId = currentUserProvider.getCurrentUserId();
        if (employeeMapper.incrementVersionByExpected(employeeId, request.getExpectedVersion(), now, operatorId) != 1) {
            throw new BusinessException(CodeEnum.ASSIGNMENT_CONFLICT);
        }
        if (assignmentChanged) {
            closeAssignments(employeeId, now, operatorId);
            insertAssignment(employeeId, request.getPrimaryAssignment(), request.getReason(), now, operatorId, true);
            for (AssignmentInput input : request.getAdditionalAssignments()) {
                insertAssignment(employeeId, input, request.getReason(), now, operatorId, false);
            }
        }
        if (reportingChanged) {
            closeReporting(employeeId, now, operatorId, ReportingType.DIRECT);
            if (request.getReporting() != null) {
                insertReporting(employeeId, request.getReporting(), request.getReason(), now, operatorId);
            }
        }
        employee.setVersion(request.getExpectedVersion() + 1);
        EmployeeOrganizationMembershipResponse after = buildMembership(employee, now);
        List<TAuthorizationHistory> histories = new ArrayList<>();
        if (assignmentChanged) {
            histories.add(newHistory(AuthorizationSubjectType.ORGANIZATION_ASSIGNMENT,
                    String.valueOf(employeeId), AuthorizationChangeType.UPDATE, employee.getUserId(),
                    assignmentSnapshot(before), assignmentSnapshot(after), request.getReason(), now));
        }
        if (reportingChanged) {
            histories.add(newHistory(AuthorizationSubjectType.REPORTING_RELATION,
                    String.valueOf(employeeId), AuthorizationChangeType.UPDATE, employee.getUserId(),
                    reportingSnapshot(before.getReporting()), reportingSnapshot(after.getReporting()), request.getReason(), now));
        }
        authorizationAuditRecorder.recordAll(histories,
                assignmentChanged ? AuditActionEnum.EMPLOYEE_ASSIGNMENT_CHANGE : AuditActionEnum.REPORTING_RELATION_CHANGE,
                String.valueOf(employeeId), json(Map.of("employeeId", employeeId, "version", employee.getVersion())));
        if ((assignmentChanged || reportingChanged) && employee.getUserId() != null) {
            if (userMapper.incrementAuthVersion(employee.getUserId()) != 1) {
                throw new BusinessException(CodeEnum.SYSTEM_ERROR, "任职变更后的认证安全版本更新失败");
            }
            scheduleAssignmentSecurityCleanup(employee.getUserId());
        }
        return after;
    }

    private void scheduleAssignmentSecurityCleanup(Integer userId) {
        securityMutations.accessChanged(userId, "组织任职或汇报关系变化");
    }

    @Override
    public List<ManagerCandidateResponse> getManagerCandidates(Integer employeeId, Integer targetOrganizationId) {
        TEmployee target = requireEmployee(employeeId);
        LocalDateTime now = LocalDateTime.now();
        requireCanManageEmployee(target, now);
        TEmployeeAssignment primary = assignmentMapper.selectCurrentPrimaryByEmployeeId(employeeId, now);
        Integer anchor = targetOrganizationId != null ? targetOrganizationId
                : primary == null ? null : primary.getOrganizationUnitId();
        if (anchor == null) return List.of();
        return directManagerPolicy.candidates(employeeId, anchor, now).stream()
                .map(this::toManagerCandidate).toList();
    }

    @Override
    public ActingReportingCollectionResponse getActingReportings(Integer employeeId) {
        TEmployee employee = requireEmployee(employeeId);
        LocalDateTime now = LocalDateTime.now();
        requireCanViewEmployee(employee, now);
        return buildActingCollection(employee, now);
    }

    @Override
    public List<ManagerCandidateResponse> getActingManagerCandidates(Integer employeeId) {
        TEmployee employee = requireEmployee(employeeId);
        LocalDateTime now = LocalDateTime.now();
        requireCanManageEmployee(employee, now);
        TEmployeeAssignment primary = assignmentMapper.selectCurrentPrimaryByEmployeeId(employeeId, now);
        if (primary == null) return List.of();
        return directManagerPolicy.actingCandidates(employeeId, primary.getOrganizationUnitId(), now).stream()
                .map(this::toManagerCandidate).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ActingReportingCollectionResponse replaceActingReportings(
            Integer employeeId, ReplaceActingReportingsRequest request) {
        lockOrganizationHierarchy();
        lockReportingGraph();
        LocalDateTime now = LocalDateTime.now().withNano(0);
        TEmployee employee = requireEmployee(employeeId);
        requireCanManageEmployee(employee, now);
        if (!currentUserProvider.hasAuthority(PermissionCodes.EMPLOYEE_REPORTING)) {
            throw new BusinessException(CodeEnum.ACCESS_DENIED, "没有代理汇报关系调整权限");
        }
        if (employee.getEmploymentStatus() == EmployeeStatus.LEFT) {
            throw new BusinessException(CodeEnum.ASSIGNMENT_CONFLICT, "已离职员工不能维护代理管理关系");
        }
        requireVersion(employee.getVersion(), request.getExpectedEmployeeVersion(), CodeEnum.ASSIGNMENT_CONFLICT);
        TEmployeeAssignment primary = assignmentMapper.selectCurrentPrimaryByEmployeeId(employeeId, now);
        if (primary == null) throw new BusinessException(CodeEnum.ASSIGNMENT_CONFLICT, "目标员工缺少当前主要任职");

        LinkedHashMap<Integer, LocalDateTime> requested = new LinkedHashMap<>();
        for (ActingReportingInput input : request.getRelations()) {
            LocalDateTime effectiveTo = toLocal(input.getEffectiveTo()).withNano(0);
            if (!effectiveTo.isAfter(now)) {
                throw new BusinessException(CodeEnum.ASSIGNMENT_CONFLICT, "代理管理失效时间必须晚于当前时间");
            }
            if (effectiveTo.isAfter(now.plusYears(1))) {
                throw new BusinessException(CodeEnum.ASSIGNMENT_CONFLICT, "代理管理期限不能超过一年");
            }
            if (requested.putIfAbsent(input.getManagerEmployeeId(), effectiveTo) != null) {
                throw new BusinessException(CodeEnum.ASSIGNMENT_CONFLICT, "同一代理管理者不能重复");
            }
            directManagerPolicy.validateActing(employeeId, primary.getOrganizationUnitId(),
                    input.getManagerEmployeeId(), now, effectiveTo);
        }

        reportingMapper.expireElapsedActingMarkers(employeeId, now, currentUserProvider.getCurrentUserId());
        List<TEmployeeReporting> existing = reportingMapper
                .selectCurrentAndFutureActingBySubordinateId(employeeId, now);
        Set<Integer> matchedManagers = new HashSet<>();
        List<TEmployeeReporting> closing = new ArrayList<>();
        for (TEmployeeReporting relation : existing) {
            LocalDateTime requestedEnd = requested.get(relation.getManagerEmployeeId());
            if (requestedEnd != null && requestedEnd.equals(relation.getEffectiveTo())
                    && matchedManagers.add(relation.getManagerEmployeeId())) continue;
            closing.add(relation);
        }
        List<Map.Entry<Integer, LocalDateTime>> inserting = requested.entrySet().stream()
                .filter(entry -> !matchedManagers.contains(entry.getKey())).toList();
        if (closing.isEmpty() && inserting.isEmpty()) return buildActingCollection(employee, now);

        List<Map<String, Object>> before = actingSnapshot(existing);
        Integer operatorId = currentUserProvider.getCurrentUserId();
        if (employeeMapper.incrementVersionByExpected(employeeId, request.getExpectedEmployeeVersion(), now, operatorId) != 1) {
            throw new BusinessException(CodeEnum.ASSIGNMENT_CONFLICT, "员工版本冲突");
        }
        for (TEmployeeReporting relation : closing) {
            int rows = !relation.getEffectiveFrom().isBefore(now)
                    ? reportingMapper.cancelFutureByIdAndVersion(relation.getId(), relation.getVersion(), now, now, operatorId)
                    : reportingMapper.endByIdAndVersion(relation.getId(), relation.getVersion(), now, now, operatorId);
            if (rows != 1) throw new BusinessException(CodeEnum.ASSIGNMENT_CONFLICT, "代理管理关系版本冲突");
        }
        for (Map.Entry<Integer, LocalDateTime> entry : inserting) {
            insertActingReporting(employeeId, entry.getKey(), entry.getValue(), request.getReason(), now, operatorId);
        }
        employee.setVersion(request.getExpectedEmployeeVersion() + 1);
        List<TEmployeeReporting> afterRelations = reportingMapper
                .selectCurrentAndFutureActingBySubordinateId(employeeId, now);
        TAuthorizationHistory history = newHistory(AuthorizationSubjectType.REPORTING_RELATION,
                String.valueOf(employeeId), AuthorizationChangeType.UPDATE, employee.getUserId(),
                json(before), json(actingSnapshot(afterRelations)), request.getReason(), now);
        authorizationAuditRecorder.record(history, AuditActionEnum.REPORTING_RELATION_CHANGE,
                String.valueOf(employeeId), json(Map.of("command", "REPLACE_ACTING_REPORTINGS",
                        "employeeId", employeeId, "version", employee.getVersion(),
                        "relationCount", afterRelations.size())));
        if (employee.getUserId() != null) {
            if (userMapper.incrementAuthVersion(employee.getUserId()) != 1) {
                throw new BusinessException(CodeEnum.SYSTEM_ERROR, "代理管理变化后的认证安全版本更新失败");
            }
            scheduleAssignmentSecurityCleanup(employee.getUserId());
        }
        return buildActingCollection(employee, now);
    }

    @Override
    public List<OrganizationChangeHistoryResponse> getOrganizationHistory(Integer employeeId) {
        TEmployee employee = requireEmployee(employeeId);
        requireCanViewEmployee(employee, LocalDateTime.now());
        return historyMapper.selectOrganizationHistoryByEmployeeId(String.valueOf(employeeId)).stream()
                .map(this::toHistoryResponse).toList();
    }

    private void validateMembershipRequest(Integer employeeId, UpdateEmployeeOrganizationRequest request,
                                           LocalDateTime now, boolean assignmentChanged,
                                           boolean reportingChanged) {
        AssignmentInput primary = request.getPrimaryAssignment();
        if (assignmentChanged && primary.getAssignmentType() != AssignmentType.PRIMARY) {
            throw new BusinessException(CodeEnum.ASSIGNMENT_CONFLICT, "主要任职类型必须为 PRIMARY");
        }
        if (assignmentChanged) {
            validateAssignmentInput(primary, now, false);
            Set<String> keys = new HashSet<>();
            for (AssignmentInput input : request.getAdditionalAssignments()) {
                if (input.getAssignmentType() == AssignmentType.PRIMARY) {
                    throw new BusinessException(CodeEnum.ASSIGNMENT_CONFLICT, "补充任职不能使用 PRIMARY");
                }
                validateAssignmentInput(input, now, true);
                String key = input.getOrganizationUnitId() + ":" + input.getPositionId() + ":" + input.getAssignmentType();
                if (!keys.add(key)) throw new BusinessException(CodeEnum.ASSIGNMENT_CONFLICT, "补充任职重复");
            }
        }
        ReportingInput reporting = request.getReporting();
        if (assignmentChanged || reportingChanged) {
            Integer managerEmployeeId = reporting == null ? null : reporting.getManagerEmployeeId();
            if (Objects.equals(managerEmployeeId, employeeId)) {
                throw new BusinessException(CodeEnum.SELF_MANAGEMENT_FORBIDDEN);
            }
            directManagerPolicy.validate(employeeId, primary.getOrganizationUnitId(), managerEmployeeId, now);
        }
        if (reportingChanged && reporting != null) {
            if (reporting.getRelationType() != ReportingType.DIRECT) {
                throw new BusinessException(CodeEnum.ASSIGNMENT_CONFLICT,
                        "直属管理与代理管理必须使用独立命令，当前入口只接受 DIRECT");
            }
            if (reporting.getEffectiveTo() != null) {
                throw new BusinessException(CodeEnum.ASSIGNMENT_CONFLICT, "直属管理关系不得预设结束时间");
            }
            validatePeriod(reporting.getEffectiveFrom(), reporting.getEffectiveTo(), false, now);
        }
    }

    private void validateAssignmentInput(AssignmentInput input, LocalDateTime now, boolean endRequired) {
        TOrganizationUnit organization = requireOrganization(input.getOrganizationUnitId());
        TPosition position = requirePosition(input.getPositionId());
        if (!Boolean.TRUE.equals(organization.getEnabled()) || Boolean.TRUE.equals(organization.getPlaceholder())) {
            throw new BusinessException(CodeEnum.ORGANIZATION_HIERARCHY_INVALID, "任职组织不可用");
        }
        if (!Boolean.TRUE.equals(position.getEnabled()) || Boolean.TRUE.equals(position.getBuiltIn())) {
            throw new BusinessException(CodeEnum.ASSIGNMENT_CONFLICT, "任职岗位不可用");
        }
        requireAssignmentOrganizationScope(input.getOrganizationUnitId(), now);
        validatePeriod(input.getEffectiveFrom(), input.getEffectiveTo(), endRequired, now);
    }

    private void requireAssignmentOrganizationScope(Integer organizationUnitId, LocalDateTime at) {
        if (hasGlobalOrganizationScope()) return;
        TEmployee operator = employeeMapper.selectByUserId(currentUserProvider.getCurrentUserId());
        TEmployeeAssignment operatorPrimary = operator == null ? null
                : assignmentMapper.selectCurrentPrimaryByEmployeeId(operator.getId(), at);
        if (operatorPrimary == null
                || !isDescendant(organizationUnitId, operatorPrimary.getOrganizationUnitId())) {
            throw new BusinessException(CodeEnum.ACCESS_DENIED, "目标任职组织超出操作者组织范围");
        }
    }

    private void validatePeriod(OffsetDateTime from, OffsetDateTime to, boolean endRequired, LocalDateTime now) {
        LocalDateTime localFrom = toLocal(from);
        LocalDateTime localTo = to == null ? null : toLocal(to);
        if (localFrom.isAfter(now.plusSeconds(1))) {
            throw new BusinessException(CodeEnum.ASSIGNMENT_CONFLICT, "本任务暂不接受未来生效的任职或管理关系");
        }
        if (endRequired && localTo == null) {
            throw new BusinessException(CodeEnum.ASSIGNMENT_CONFLICT, "代理关系必须设置失效时间");
        }
        if (localTo != null && !localTo.isAfter(now)) {
            throw new BusinessException(CodeEnum.ASSIGNMENT_CONFLICT, "失效时间必须晚于当前时间");
        }
    }

    private void closeAssignments(Integer employeeId, LocalDateTime now, Integer operatorId) {
        assignmentMapper.expireElapsedMarkers(employeeId, now, operatorId);
        for (TEmployeeAssignment existing : assignmentMapper.selectReplaceableByEmployeeId(employeeId, now)) {
            int rows = !existing.getEffectiveFrom().isBefore(now)
                    ? assignmentMapper.cancelFutureByIdAndVersion(existing.getId(), existing.getVersion(), now, now, operatorId)
                    : assignmentMapper.endByIdAndVersion(existing.getId(), existing.getVersion(), now, now, operatorId);
            if (rows != 1) throw new BusinessException(CodeEnum.ASSIGNMENT_CONFLICT);
        }
    }

    private void closeReporting(Integer employeeId, LocalDateTime now, Integer operatorId, ReportingType replacingType) {
        reportingMapper.expireElapsedMarkers(employeeId, now, operatorId);
        for (TEmployeeReporting existing : reportingMapper.selectReplaceableBySubordinateId(employeeId, now)) {
            if (replacingType != null && existing.getRelationType() != replacingType) continue;
            int rows = !existing.getEffectiveFrom().isBefore(now)
                    ? reportingMapper.cancelFutureByIdAndVersion(existing.getId(), existing.getVersion(), now, now, operatorId)
                    : reportingMapper.endByIdAndVersion(existing.getId(), existing.getVersion(), now, now, operatorId);
            if (rows != 1) throw new BusinessException(CodeEnum.ASSIGNMENT_CONFLICT);
        }
    }

    private void insertAssignment(Integer employeeId, AssignmentInput input, String reason,
                                  LocalDateTime now, Integer operatorId, boolean primary) {
        TEmployeeAssignment assignment = new TEmployeeAssignment();
        assignment.setEmployeeId(employeeId);
        assignment.setOrganizationUnitId(input.getOrganizationUnitId());
        assignment.setPositionId(input.getPositionId());
        assignment.setAssignmentType(input.getAssignmentType());
        assignment.setStatus(AssignmentStatus.ACTIVE);
        assignment.setActivePrimaryMarker(primary ? Boolean.TRUE : null);
        assignment.setEffectiveFrom(now);
        assignment.setEffectiveTo(input.getEffectiveTo() == null ? null : toLocal(input.getEffectiveTo()));
        assignment.setReason(reason);
        assignment.setVersion(0);
        assignment.setCreateTime(now);
        assignment.setCreateBy(operatorId);
        try {
            if (assignmentMapper.insert(assignment) != 1) throw operationFailed("任职写入失败");
        } catch (DuplicateKeyException e) {
            throw new BusinessException(CodeEnum.ASSIGNMENT_CONFLICT, "主要任职已被其他操作更新", e);
        }
    }

    private void insertReporting(Integer employeeId, ReportingInput input, String reason,
                                 LocalDateTime now, Integer operatorId) {
        TEmployeeReporting reporting = new TEmployeeReporting();
        reporting.setSubordinateEmployeeId(employeeId);
        reporting.setManagerEmployeeId(input.getManagerEmployeeId());
        reporting.setRelationType(input.getRelationType());
        reporting.setStatus(ReportingStatus.ACTIVE);
        reporting.setActiveDirectMarker(input.getRelationType() == ReportingType.DIRECT ? Boolean.TRUE : null);
        reporting.setEffectiveFrom(now);
        reporting.setEffectiveTo(input.getEffectiveTo() == null ? null : toLocal(input.getEffectiveTo()));
        reporting.setReason(reason);
        reporting.setVersion(0);
        reporting.setCreateTime(now);
        reporting.setCreateBy(operatorId);
        try {
            if (reportingMapper.insert(reporting) != 1) throw operationFailed("汇报关系写入失败");
        } catch (DuplicateKeyException e) {
            throw new BusinessException(CodeEnum.ASSIGNMENT_CONFLICT, "直属管理关系已被其他操作更新", e);
        }
    }

    private void insertActingReporting(Integer employeeId, Integer managerEmployeeId,
                                       LocalDateTime effectiveTo, String reason,
                                       LocalDateTime now, Integer operatorId) {
        TEmployeeReporting relation = new TEmployeeReporting();
        relation.setSubordinateEmployeeId(employeeId);
        relation.setManagerEmployeeId(managerEmployeeId);
        relation.setRelationType(ReportingType.ACTING);
        relation.setStatus(ReportingStatus.ACTIVE);
        relation.setActiveDirectMarker(null);
        relation.setEffectiveFrom(now);
        relation.setEffectiveTo(effectiveTo);
        relation.setReason(reason.trim());
        relation.setVersion(0);
        relation.setCreateTime(now);
        relation.setCreateBy(operatorId);
        if (reportingMapper.insert(relation) != 1) {
            throw new BusinessException(CodeEnum.OPERATION_FAILED, "代理管理关系写入失败");
        }
    }

    private ActingReportingCollectionResponse buildActingCollection(TEmployee employee, LocalDateTime now) {
        ActingReportingCollectionResponse response = new ActingReportingCollectionResponse();
        response.setEmployeeId(employee.getId());
        response.setEmployeeVersion(employee.getVersion());
        for (TEmployeeReporting relation : reportingMapper
                .selectCurrentAndFutureActingBySubordinateId(employee.getId(), now)) {
            ActingReportingRelationResponse item = new ActingReportingRelationResponse();
            item.setId(relation.getId());
            item.setVersion(relation.getVersion());
            item.setManagerEmployeeId(relation.getManagerEmployeeId());
            item.setStatus(relation.getStatus());
            item.setEffectiveFrom(toOffset(relation.getEffectiveFrom()));
            item.setEffectiveTo(toOffset(relation.getEffectiveTo()));
            TEmployee manager = employeeMapper.selectByPrimaryKey(relation.getManagerEmployeeId());
            if (manager != null) {
                item.setManagerEmployeeNo(manager.getEmployeeNo());
                item.setManagerEmployeeName(manager.getName());
            }
            response.getRelations().add(item);
        }
        EmployeeSummaryResponse summary = toEmployeeSummary(employee, now);
        if (summary.getAllowedActions().contains("reporting")) response.getAllowedActions().add("UPDATE");
        else response.getUnavailableReasons().put("UPDATE",
                summary.getUnavailableReasons().getOrDefault("reporting", "当前用户不能维护代理管理关系"));
        return response;
    }

    private List<Map<String, Object>> actingSnapshot(List<TEmployeeReporting> relations) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (TEmployeeReporting relation : relations) {
            TEmployee manager = employeeMapper.selectByPrimaryKey(relation.getManagerEmployeeId());
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("relationId", relation.getId());
            item.put("managerEmployeeId", relation.getManagerEmployeeId());
            item.put("managerEmployeeNo", manager == null ? null : manager.getEmployeeNo());
            item.put("managerName", manager == null ? null : manager.getName());
            item.put("relationType", ReportingType.ACTING.name());
            item.put("effectiveFrom", timeText(relation.getEffectiveFrom()));
            item.put("effectiveTo", timeText(relation.getEffectiveTo()));
            result.add(item);
        }
        return result;
    }

    private EmployeeOrganizationMembershipResponse buildMembership(TEmployee employee, LocalDateTime effectiveAt) {
        EmployeeOrganizationMembershipResponse response = new EmployeeOrganizationMembershipResponse();
        EmployeeSummaryResponse summary = toEmployeeSummary(employee, effectiveAt);
        response.setEmployee(summary);
        List<TEmployeeAssignment> assignments = assignmentMapper.selectEffectiveByEmployeeId(employee.getId(), effectiveAt);
        for (TEmployeeAssignment assignment : assignments) {
            EmployeeAssignmentResponse item = toAssignmentResponse(assignment);
            if (assignment.getAssignmentType() == AssignmentType.PRIMARY) response.setPrimaryAssignment(item);
            else response.getAdditionalAssignments().add(item);
        }
        TEmployeeReporting direct = reportingMapper.selectCurrentDirectBySubordinateId(employee.getId(), effectiveAt);
        if (direct != null) response.setReporting(toReportingResponse(direct));
        response.setVersion(employee.getVersion());
        response.setAllowedActions(new ArrayList<>(summary.getAllowedActions()));
        response.setUnavailableReasons(new LinkedHashMap<>(summary.getUnavailableReasons()));
        return response;
    }

    private EmployeeSummaryResponse toEmployeeSummary(TEmployee employee, LocalDateTime effectiveAt) {
        EmployeeSummaryResponse response = new EmployeeSummaryResponse();
        response.setId(employee.getId());
        response.setUserId(employee.getUserId());
        response.setEmployeeNo(employee.getEmployeeNo());
        response.setName(employee.getName());
        response.setEmploymentStatus(employee.getEmploymentStatus());
        response.setVersion(employee.getVersion());
        TEmployeeAssignment primary = assignmentMapper.selectCurrentPrimaryByEmployeeId(employee.getId(), effectiveAt);
        if (primary != null) {
            TOrganizationUnit org = organizationUnitMapper.selectByPrimaryKey(primary.getOrganizationUnitId());
            TPosition position = positionMapper.selectByPrimaryKey(primary.getPositionId());
            if (org != null && !Boolean.TRUE.equals(org.getPlaceholder())
                    && position != null && !Boolean.TRUE.equals(position.getBuiltIn())) {
                response.setOrganizationUnitId(org.getId());
                response.setOrganizationUnitName(org.getName());
                response.setPositionId(position.getId());
                response.setPositionName(position.getName());
            }
        }
        TEmployeeReporting direct = reportingMapper.selectCurrentDirectBySubordinateId(employee.getId(), effectiveAt);
        if (direct != null) {
            TEmployee manager = employeeMapper.selectByPrimaryKey(direct.getManagerEmployeeId());
            if (manager != null) {
                response.setManagerEmployeeId(manager.getId());
                response.setManagerEmployeeName(manager.getName());
            }
        }
        applyAllowedActions(response, employee);
        return response;
    }

    private void applyAllowedActions(EmployeeSummaryResponse response, TEmployee employee) {
        TEmployee operator = employeeMapper.selectByUserId(currentUserProvider.getCurrentUserId());
        boolean self = operator != null && operator.getId().equals(employee.getId());
        boolean active = employee.getEmploymentStatus() != EmployeeStatus.LEFT;
        boolean manageable = canManageEmployee(employee, LocalDateTime.now());
        if (currentUserProvider.hasAuthority(PermissionCodes.ORGANIZATION_VIEW) && manageable) {
            response.getAllowedActions().add("history");
        }
        if (!self && active && manageable && currentUserProvider.hasAuthority(PermissionCodes.EMPLOYEE_ASSIGNMENT)) {
            response.getAllowedActions().add("assignment");
        } else response.getUnavailableReasons().put("assignment", self ? "不能调整本人任职" : "无管理范围、任职调整权限或员工已离职");
        if (!self && active && manageable && currentUserProvider.hasAuthority(PermissionCodes.EMPLOYEE_REPORTING)) {
            response.getAllowedActions().add("reporting");
        } else response.getUnavailableReasons().put("reporting", self ? "不能调整本人管理关系" : "无管理范围、汇报关系权限或员工已离职");
        if (!response.getAllowedActions().contains("history")) response.getUnavailableReasons().put("history", "无组织历史查看权限");
    }

    private void validateParent(OrganizationUnitType childType, Integer parentId, Integer editingId) {
        if (childType == OrganizationUnitType.COMPANY) {
            if (parentId != null) throw new BusinessException(CodeEnum.ORGANIZATION_HIERARCHY_INVALID, "公司必须是根组织");
            return;
        }
        if (parentId == null) throw new BusinessException(CodeEnum.ORGANIZATION_HIERARCHY_INVALID, "非公司组织必须设置上级");
        if (editingId != null && editingId.equals(parentId)) throw new BusinessException(CodeEnum.ORGANIZATION_PARENT_CYCLE);
        TOrganizationUnit parent = requireOrganization(parentId);
        if (!Boolean.TRUE.equals(parent.getEnabled()) || !supportsChild(parent.getType(), childType)) {
            throw new BusinessException(CodeEnum.ORGANIZATION_HIERARCHY_INVALID);
        }
        if (editingId != null && isDescendant(parentId, editingId)) {
            throw new BusinessException(CodeEnum.ORGANIZATION_PARENT_CYCLE);
        }
    }

    private void lockOrganizationHierarchy() {
        if (!"ORGANIZATION_HIERARCHY".equals(graphLockMapper.lockByName("ORGANIZATION_HIERARCHY"))) {
            throw new IllegalStateException("组织层级图锁缺失");
        }
    }

    private void lockReportingGraph() {
        if (!"REPORTING_GRAPH".equals(graphLockMapper.lockByName("REPORTING_GRAPH"))) {
            throw new IllegalStateException("汇报关系图锁缺失");
        }
    }

    private boolean isRootCompany(TOrganizationUnit unit) {
        return unit != null && unit.getType() == OrganizationUnitType.COMPANY
                && unit.getParentId() == null
                && !Boolean.TRUE.equals(unit.getPlaceholder());
    }

    private void ensureNoEnabledRoot(Integer excludeId) {
        boolean duplicate = organizationUnitMapper.selectRoots().stream()
                .filter(this::isRootCompany)
                .filter(unit -> Boolean.TRUE.equals(unit.getEnabled()))
                .anyMatch(unit -> !Objects.equals(unit.getId(), excludeId));
        if (duplicate) {
            throw new BusinessException(CodeEnum.ORGANIZATION_HIERARCHY_INVALID,
                    "系统只能存在一个启用的根公司");
        }
    }

    private boolean supportsChild(OrganizationUnitType parent, OrganizationUnitType child) {
        return switch (parent) {
            case COMPANY -> child == OrganizationUnitType.STORE || child == OrganizationUnitType.DEPARTMENT;
            case STORE -> child == OrganizationUnitType.DEPARTMENT || child == OrganizationUnitType.TEAM;
            case DEPARTMENT -> child == OrganizationUnitType.TEAM;
            case TEAM -> false;
        };
    }

    private void validateChildrenCompatible(Integer id, OrganizationUnitType parentType) {
        for (TOrganizationUnit child : organizationUnitMapper.selectByParentId(id)) {
            if (!supportsChild(parentType, child.getType())) {
                throw new BusinessException(CodeEnum.ORGANIZATION_HIERARCHY_INVALID, "当前组织类型不能承载已有下级");
            }
        }
    }

    private boolean isDescendant(Integer possibleDescendantId, Integer ancestorId) {
        Integer currentId = possibleDescendantId;
        Set<Integer> visited = new HashSet<>();
        while (currentId != null && visited.add(currentId)) {
            if (currentId.equals(ancestorId)) return true;
            TOrganizationUnit unit = organizationUnitMapper.selectByPrimaryKey(currentId);
            currentId = unit == null ? null : unit.getParentId();
        }
        return false;
    }

    private void validateLeader(Integer leaderEmployeeId, Integer targetOrganizationId, Integer parentOrganizationId) {
        if (leaderEmployeeId == null) return;
        LocalDateTime now = LocalDateTime.now();
        Integer anchorOrganizationId = targetOrganizationId != null ? targetOrganizationId : parentOrganizationId;
        List<Integer> organizationIds = candidateOrganizationIds(anchorOrganizationId, now);
        TEmployee candidate = employeeMapper.selectEligibleManagerCandidates(-1, now, organizationIds).stream()
                .filter(employee -> employee.getId().equals(leaderEmployeeId))
                .findFirst().orElseThrow(() -> new BusinessException(CodeEnum.INVALID_MANAGER, "组织负责人候选不可用"));
        if (!isCandidateWithinOperatorScope(candidate, now)) {
            throw new BusinessException(CodeEnum.ACCESS_DENIED, "负责人候选超出操作者组织范围");
        }
        if (anchorOrganizationId != null) {
            TEmployeeAssignment primary = assignmentMapper.selectCurrentPrimaryByEmployeeId(candidate.getId(), now);
            if (primary == null || !isDescendant(primary.getOrganizationUnitId(), anchorOrganizationId)) {
                throw new BusinessException(CodeEnum.INVALID_MANAGER, "负责人不在目标组织范围内");
            }
        }
        // 负责人只是组织属性，本方法绝不生成直属汇报关系。
    }

    private void validateOrganizationCanDisable(Integer id) {
        for (TOrganizationUnit unit : organizationUnitMapper.selectAll()) {
            if (Boolean.TRUE.equals(unit.getEnabled()) && !unit.getId().equals(id) && isDescendant(unit.getId(), id)) {
                throw new BusinessException(CodeEnum.ORGANIZATION_HAS_ACTIVE_CHILDREN);
            }
        }
        if (organizationUnitMapper.countEffectiveEmployees(id, LocalDateTime.now()) > 0) {
            throw new BusinessException(CodeEnum.ORGANIZATION_HAS_ACTIVE_EMPLOYEES);
        }
    }

    private void requireCanViewEmployee(TEmployee target, LocalDateTime at) {
        if (!canManageEmployee(target, at)) throw new BusinessException(CodeEnum.ACCESS_DENIED, "目标员工超出可查看管理范围");
    }

    private void requireCanManageEmployee(TEmployee target, LocalDateTime at) {
        TUser targetAccount = target.getUserId() == null ? null : userMapper.selectByPrimaryKey(target.getUserId());
        if (targetAccount != null && (targetAccount.getAccountType() == AccountType.SYSTEM
                || Boolean.TRUE.equals(targetAccount.getProtectedAccount()))) {
            throw new BusinessException(CodeEnum.ACCESS_DENIED, "受保护账号不能通过组织任职入口调整");
        }
        TEmployee operator = employeeMapper.selectByUserId(currentUserProvider.getCurrentUserId());
        if (operator != null && operator.getId().equals(target.getId())) {
            throw new BusinessException(CodeEnum.SELF_MANAGEMENT_FORBIDDEN);
        }
        if (!canManageEmployee(target, at)) {
            throw new BusinessException(CodeEnum.ACCESS_DENIED, "目标员工超出管理关系或组织范围");
        }
    }

    private boolean canManageEmployee(TEmployee target, LocalDateTime at) {
        if (hasGlobalOrganizationScope()) return true;
        TEmployee operator = employeeMapper.selectByUserId(currentUserProvider.getCurrentUserId());
        return operator != null
                && !operator.getId().equals(target.getId())
                && isManagerOf(operator.getId(), target.getId(), at)
                && isWithinOrganizationScope(operator.getId(), target.getId(), at);
    }

    private boolean isManagerOf(Integer managerId, Integer subordinateId, LocalDateTime at) {
        Deque<Integer> queue = new ArrayDeque<>();
        Set<Integer> visited = new HashSet<>();
        queue.add(subordinateId);
        while (!queue.isEmpty()) {
            Integer current = queue.removeFirst();
            if (!visited.add(current)) continue;
            for (TEmployeeReporting reporting : reportingMapper.selectEffectiveManagers(current, at)) {
                if (managerId.equals(reporting.getManagerEmployeeId())) return true;
                queue.addLast(reporting.getManagerEmployeeId());
            }
        }
        return false;
    }

    private boolean isWithinOrganizationScope(Integer managerId, Integer targetId, LocalDateTime at) {
        TEmployeeAssignment targetPrimary = assignmentMapper.selectCurrentPrimaryByEmployeeId(targetId, at);
        TEmployeeAssignment managerPrimary = assignmentMapper.selectCurrentPrimaryByEmployeeId(managerId, at);
        return targetPrimary != null && managerPrimary != null
                && isDescendant(targetPrimary.getOrganizationUnitId(), managerPrimary.getOrganizationUnitId());
    }

    private boolean isCandidateWithinOperatorScope(TEmployee candidate, LocalDateTime at) {
        if (hasGlobalOrganizationScope()) return true;
        TEmployee operator = employeeMapper.selectByUserId(currentUserProvider.getCurrentUserId());
        if (operator == null) return false;
        TEmployeeAssignment candidatePrimary = assignmentMapper.selectCurrentPrimaryByEmployeeId(candidate.getId(), at);
        TEmployeeAssignment operatorPrimary = assignmentMapper.selectCurrentPrimaryByEmployeeId(operator.getId(), at);
        return candidatePrimary != null && operatorPrimary != null
                && isDescendant(candidatePrimary.getOrganizationUnitId(), operatorPrimary.getOrganizationUnitId());
    }

    /** null 表示全局；非 null 列表直接下推给 SQL，避免先加载全量候选及其敏感字段。 */
    private List<Integer> candidateOrganizationIds(Integer anchorOrganizationId, LocalDateTime at) {
        List<TOrganizationUnit> visible = visibleOrganizationUnits(at);
        if (anchorOrganizationId != null) {
            return visible.stream().filter(unit -> isDescendant(unit.getId(), anchorOrganizationId))
                    .map(TOrganizationUnit::getId).toList();
        }
        if (hasGlobalOrganizationScope()) return null;
        return visible.stream().map(TOrganizationUnit::getId).toList();
    }

    private void requireOrganizationReadScope(Integer organizationUnitId, LocalDateTime at) {
        if (hasGlobalOrganizationScope()) return;
        TEmployee operator = employeeMapper.selectByUserId(currentUserProvider.getCurrentUserId());
        TEmployeeAssignment operatorPrimary = operator == null ? null
                : assignmentMapper.selectCurrentPrimaryByEmployeeId(operator.getId(), at);
        if (operatorPrimary == null
                || !isDescendant(organizationUnitId, operatorPrimary.getOrganizationUnitId())) {
            throw new BusinessException(CodeEnum.ACCESS_DENIED, "组织超出可查看范围");
        }
    }

    private List<TOrganizationUnit> visibleOrganizationUnits(LocalDateTime at) {
        List<TOrganizationUnit> allUnits = organizationUnitMapper.selectAll().stream()
                .filter(unit -> !Boolean.TRUE.equals(unit.getPlaceholder()))
                .toList();
        if (hasGlobalOrganizationScope()) return allUnits;
        TEmployee operator = employeeMapper.selectByUserId(currentUserProvider.getCurrentUserId());
        if (operator == null) return List.of();
        TEmployeeAssignment operatorPrimary = assignmentMapper.selectCurrentPrimaryByEmployeeId(operator.getId(), at);
        if (operatorPrimary == null) return List.of();
        return allUnits.stream()
                .filter(unit -> isDescendant(unit.getId(), operatorPrimary.getOrganizationUnitId()))
                .toList();
    }

    private boolean hasGlobalOrganizationScope() {
        return authorizationPolicy.isGlobalOperator();
    }

    private void requireGlobalOrganizationScope() {
        if (!hasGlobalOrganizationScope()) {
            throw new BusinessException(CodeEnum.ACCESS_DENIED, "岗位目录和根组织仅允许全局组织管理员维护");
        }
    }

    private OrganizationUnitResponse toOrganizationResponse(TOrganizationUnit unit, LocalDateTime now) {
        OrganizationUnitResponse response = new OrganizationUnitResponse();
        response.setId(unit.getId()); response.setCode(unit.getCode()); response.setName(unit.getName());
        response.setType(unit.getType()); response.setParentId(unit.getParentId());
        response.setLeaderEmployeeId(unit.getLeaderEmployeeId()); response.setOrderNo(unit.getOrderNo());
        response.setEnabled(unit.getEnabled()); response.setVersion(unit.getVersion());
        response.setEmployeeCount(organizationUnitMapper.countEffectiveEmployees(unit.getId(), now));
        if (unit.getLeaderEmployeeId() != null) {
            TEmployee leader = employeeMapper.selectByPrimaryKey(unit.getLeaderEmployeeId());
            if (leader != null) response.setLeaderEmployeeName(leader.getName());
        }
        return response;
    }

    private PositionResponse toPositionResponse(TPosition position) {
        PositionResponse response = new PositionResponse();
        response.setId(position.getId()); response.setCode(position.getCode()); response.setName(position.getName());
        response.setDescription(position.getDescription()); response.setPositionLevel(position.getPositionLevel());
        response.setBuiltIn(position.getBuiltIn()); response.setEnabled(position.getEnabled());
        response.setVersion(position.getVersion());
        return response;
    }

    private EmployeeAssignmentResponse toAssignmentResponse(TEmployeeAssignment assignment) {
        EmployeeAssignmentResponse response = new EmployeeAssignmentResponse();
        response.setId(assignment.getId()); response.setOrganizationUnitId(assignment.getOrganizationUnitId());
        response.setPositionId(assignment.getPositionId()); response.setAssignmentType(assignment.getAssignmentType());
        response.setEffectiveFrom(toOffset(assignment.getEffectiveFrom()));
        response.setEffectiveTo(toOffset(assignment.getEffectiveTo()));
        TOrganizationUnit org = organizationUnitMapper.selectByPrimaryKey(assignment.getOrganizationUnitId());
        TPosition position = positionMapper.selectByPrimaryKey(assignment.getPositionId());
        if (org != null) response.setOrganizationUnitName(org.getName());
        if (position != null) response.setPositionName(position.getName());
        return response;
    }

    private EmployeeReportingResponse toReportingResponse(TEmployeeReporting reporting) {
        EmployeeReportingResponse response = new EmployeeReportingResponse();
        response.setManagerEmployeeId(reporting.getManagerEmployeeId());
        TEmployee manager = employeeMapper.selectByPrimaryKey(reporting.getManagerEmployeeId());
        if (manager != null) response.setManagerEmployeeName(manager.getName());
        response.setRelationType(reporting.getRelationType());
        response.setEffectiveFrom(toOffset(reporting.getEffectiveFrom()));
        response.setEffectiveTo(toOffset(reporting.getEffectiveTo()));
        return response;
    }

    private ManagerCandidateResponse toManagerCandidate(TEmployee employee) {
        ManagerCandidateResponse response = new ManagerCandidateResponse();
        response.setEmployeeId(employee.getId()); response.setEmployeeNo(employee.getEmployeeNo()); response.setName(employee.getName());
        TEmployeeAssignment primary = assignmentMapper.selectCurrentPrimaryByEmployeeId(employee.getId(), LocalDateTime.now());
        if (primary != null) {
            TOrganizationUnit org = organizationUnitMapper.selectByPrimaryKey(primary.getOrganizationUnitId());
            TPosition position = positionMapper.selectByPrimaryKey(primary.getPositionId());
            if (org != null) response.setOrganizationUnitName(org.getName());
            if (position != null) response.setPositionName(position.getName());
        }
        return response;
    }

    private OrganizationChangeHistoryResponse toHistoryResponse(TAuthorizationHistory history) {
        OrganizationChangeHistoryResponse response = new OrganizationChangeHistoryResponse();
        response.setId(history.getId()); response.setChangeType(history.getChangeType().name());
        response.setBeforeSummary(history.getBeforeValue()); response.setAfterSummary(history.getAfterValue());
        response.setReason(history.getReason()); response.setCreateTime(toOffset(history.getOccurredTime()));
        TUser operator = userMapper.selectByPrimaryKey(history.getOperatorId());
        if (operator != null) response.setOperatorName(operator.getName());
        return response;
    }

    private void recordCatalogHistory(Integer id, AuthorizationSubjectType subjectType,
                                      AuthorizationChangeType changeType, String before, String after,
                                      String reason, AuditActionEnum action) {
        LocalDateTime now = LocalDateTime.now();
        TAuthorizationHistory history = newHistory(subjectType, String.valueOf(id), changeType,
                null, before, after, reason, now);
        authorizationAuditRecorder.record(history, action, String.valueOf(id),
                json(Map.of("id", id, "changeType", changeType.name())));
    }

    private TAuthorizationHistory newHistory(AuthorizationSubjectType subjectType, String subjectId,
                                             AuthorizationChangeType changeType, Integer targetUserId,
                                             String before, String after, String reason, LocalDateTime now) {
        TAuthorizationHistory history = new TAuthorizationHistory();
        history.setSubjectType(subjectType); history.setSubjectId(subjectId); history.setChangeType(changeType);
        history.setTargetUserId(targetUserId); history.setBeforeValue(before); history.setAfterValue(after);
        history.setReason(reason); history.setOperatorId(currentUserProvider.getCurrentUserId()); history.setOccurredTime(now);
        return history;
    }

    private boolean assignmentsChanged(EmployeeOrganizationMembershipResponse before,
                                       UpdateEmployeeOrganizationRequest request) {
        if (!Objects.equals(assignmentSignature(before.getPrimaryAssignment()),
                assignmentSignature(request.getPrimaryAssignment()))) {
            return true;
        }
        List<String> beforeAdditional = before.getAdditionalAssignments().stream()
                .map(this::assignmentSignature).sorted().toList();
        List<String> requestedAdditional = request.getAdditionalAssignments().stream()
                .map(this::assignmentSignature).sorted().toList();
        return !beforeAdditional.equals(requestedAdditional);
    }

    private boolean reportingChanged(EmployeeReportingResponse before, ReportingInput requested) {
        return !Objects.equals(reportingSignature(before), reportingSignature(requested));
    }

    private String assignmentSignature(EmployeeAssignmentResponse value) {
        if (value == null) return null;
        return String.join("|", String.valueOf(value.getOrganizationUnitId()),
                String.valueOf(value.getPositionId()), String.valueOf(value.getAssignmentType()),
                timeSignature(value.getEffectiveTo()));
    }

    private String assignmentSignature(AssignmentInput value) {
        if (value == null) return null;
        return String.join("|", String.valueOf(value.getOrganizationUnitId()),
                String.valueOf(value.getPositionId()), String.valueOf(value.getAssignmentType()),
                timeSignature(value.getEffectiveTo()));
    }

    private String reportingSignature(EmployeeReportingResponse value) {
        if (value == null) return null;
        return String.join("|", String.valueOf(value.getManagerEmployeeId()),
                String.valueOf(value.getRelationType()), timeSignature(value.getEffectiveTo()));
    }

    private String reportingSignature(ReportingInput value) {
        if (value == null) return null;
        return String.join("|", String.valueOf(value.getManagerEmployeeId()),
                String.valueOf(value.getRelationType()), timeSignature(value.getEffectiveTo()));
    }

    private String timeSignature(OffsetDateTime value) {
        return value == null ? "" : String.valueOf(value.toInstant().toEpochMilli());
    }

    private String assignmentSnapshot(EmployeeOrganizationMembershipResponse membership) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        EmployeeSummaryResponse employee = membership.getEmployee();
        if (employee != null) snapshot.put("employee", Map.of("id", employee.getId(),
                "code", employee.getEmployeeNo(), "name", employee.getName()));
        snapshot.put("primaryAssignment", assignmentFact(membership.getPrimaryAssignment()));
        snapshot.put("additionalAssignments", membership.getAdditionalAssignments().stream().map(this::assignmentFact).toList());
        return json(snapshot);
    }

    private Map<String, Object> assignmentFact(EmployeeAssignmentResponse assignment) {
        if (assignment == null) return Map.of();
        TOrganizationUnit organization = organizationUnitMapper.selectByPrimaryKey(assignment.getOrganizationUnitId());
        TPosition position = positionMapper.selectByPrimaryKey(assignment.getPositionId());
        Map<String, Object> fact = new LinkedHashMap<>();
        fact.put("organizationUnit", stableRef(assignment.getOrganizationUnitId(), organization == null ? null : organization.getCode(), assignment.getOrganizationUnitName()));
        fact.put("position", stableRef(assignment.getPositionId(), position == null ? null : position.getCode(), assignment.getPositionName()));
        fact.put("assignmentType", assignment.getAssignmentType() == null ? null : assignment.getAssignmentType().name());
        fact.put("effectiveFrom", assignment.getEffectiveFrom()); fact.put("effectiveTo", assignment.getEffectiveTo());
        return fact;
    }

    private String reportingSnapshot(EmployeeReportingResponse reporting) {
        if (reporting == null) return null;
        TEmployee manager = employeeMapper.selectByPrimaryKey(reporting.getManagerEmployeeId());
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("manager", stableRef(reporting.getManagerEmployeeId(), manager == null ? null : manager.getEmployeeNo(), reporting.getManagerEmployeeName()));
        snapshot.put("relationType", reporting.getRelationType() == null ? null : reporting.getRelationType().name());
        snapshot.put("effectiveFrom", timeText(reporting.getEffectiveFrom()));
        snapshot.put("effectiveTo", timeText(reporting.getEffectiveTo()));
        return json(snapshot);
    }

    private String timeText(Object value) {
        return value == null ? null : value.toString();
    }

    private Map<String, Object> stableRef(Integer id, String code, String name) {
        Map<String, Object> ref = new LinkedHashMap<>(); ref.put("id", id); ref.put("code", code); ref.put("name", name); return ref;
    }

    private String organizationSnapshot(TOrganizationUnit value) {
        return json(Map.of("code", value.getCode(), "name", value.getName(), "type", value.getType(),
                "parentId", nullable(value.getParentId()), "leaderEmployeeId", nullable(value.getLeaderEmployeeId()),
                "orderNo", value.getOrderNo(), "enabled", value.getEnabled(), "version", value.getVersion()));
    }

    private String positionSnapshot(TPosition value) {
        return json(Map.of("code", value.getCode(), "name", value.getName(),
                "positionLevel", value.getPositionLevel(), "enabled", value.getEnabled(), "version", value.getVersion()));
    }

    private Object nullable(Object value) { return value == null ? "" : value; }

    private String json(Object value) {
        if (value == null) return null;
        try { return objectMapper.writeValueAsString(value); }
        catch (JacksonException e) { throw new IllegalStateException("组织审计摘要序列化失败", e); }
    }

    private TOrganizationUnit requireOrganization(Integer id) {
        TOrganizationUnit value = id == null ? null : organizationUnitMapper.selectByPrimaryKey(id);
        if (value == null || Boolean.TRUE.equals(value.getPlaceholder())) {
            throw new BusinessException(CodeEnum.NOT_FOUND, "组织不存在");
        }
        return value;
    }

    private TPosition requirePosition(Integer id) {
        TPosition value = id == null ? null : positionMapper.selectByPrimaryKey(id);
        if (value == null) throw new BusinessException(CodeEnum.NOT_FOUND, "岗位不存在");
        return value;
    }

    private TEmployee requireEmployee(Integer id) {
        TEmployee value = id == null ? null : employeeMapper.selectByPrimaryKey(id);
        if (value == null) throw new BusinessException(CodeEnum.NOT_FOUND, "员工不存在");
        return value;
    }

    private void requireVersion(Integer actual, Integer expected, CodeEnum code) {
        if (!Objects.equals(actual, expected)) throw new BusinessException(code);
    }

    private BusinessException operationFailed(String message) {
        return new BusinessException(CodeEnum.OPERATION_FAILED, message);
    }

    private TOrganizationUnit copyOrganization(TOrganizationUnit source) {
        TOrganizationUnit copy = new TOrganizationUnit();
        copy.setId(source.getId()); copy.setCode(source.getCode()); copy.setName(source.getName());
        copy.setType(source.getType()); copy.setParentId(source.getParentId()); copy.setLeaderEmployeeId(source.getLeaderEmployeeId());
        copy.setOrderNo(source.getOrderNo()); copy.setPlaceholder(source.getPlaceholder());
        copy.setEnabled(source.getEnabled()); copy.setVersion(source.getVersion());
        return copy;
    }

    private TPosition copyPosition(TPosition source) {
        TPosition copy = new TPosition();
        copy.setId(source.getId()); copy.setCode(source.getCode()); copy.setName(source.getName());
        copy.setDescription(source.getDescription()); copy.setPositionLevel(source.getPositionLevel());
        copy.setBuiltIn(source.getBuiltIn()); copy.setEnabled(source.getEnabled()); copy.setVersion(source.getVersion());
        return copy;
    }

    private void sortTree(List<OrganizationUnitResponse> values) {
        values.sort(Comparator.comparing(OrganizationUnitResponse::getOrderNo).thenComparing(OrganizationUnitResponse::getId));
        values.forEach(value -> sortTree(value.getChildren()));
    }

    private LocalDateTime toLocal(OffsetDateTime value) {
        return value.atZoneSameInstant(BUSINESS_ZONE).toLocalDateTime();
    }

    private OffsetDateTime toOffset(LocalDateTime value) {
        return value == null ? null : value.atZone(BUSINESS_ZONE).toOffsetDateTime();
    }
}
