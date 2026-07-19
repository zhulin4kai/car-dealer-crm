package com.autodealer.crm.modules.identity.application.internal;

import com.autodealer.crm.modules.identity.application.api.security.CurrentUserProvider;
import com.autodealer.crm.modules.identity.application.api.enums.OrganizationUnitType;
import com.autodealer.crm.shared.error.BusinessException;
import com.autodealer.crm.modules.identity.persistence.mapper.TEmployeeAssignmentMapper;
import com.autodealer.crm.modules.identity.persistence.mapper.TEmployeeMapper;
import com.autodealer.crm.modules.identity.persistence.mapper.TEmployeeReportingMapper;
import com.autodealer.crm.modules.identity.persistence.mapper.TOrganizationUnitMapper;
import com.autodealer.crm.modules.identity.persistence.model.TEmployee;
import com.autodealer.crm.modules.identity.persistence.model.TEmployeeAssignment;
import com.autodealer.crm.modules.identity.persistence.model.TEmployeeReporting;
import com.autodealer.crm.modules.identity.persistence.model.TOrganizationUnit;
import com.autodealer.crm.shared.error.CodeEnum;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** 邀请、组织调整和生命周期命令共用的直属管理者唯一事实边界。 */
@Component
public class DirectManagerPolicy {
    private final CurrentUserProvider current;
    private final TEmployeeMapper employees;
    private final TEmployeeAssignmentMapper assignments;
    private final TEmployeeReportingMapper reporting;
    private final TOrganizationUnitMapper organizations;
    private final UserAuthorizationPolicy authorizationPolicy;

    public DirectManagerPolicy(CurrentUserProvider current, TEmployeeMapper employees,
                               TEmployeeAssignmentMapper assignments,
                               TEmployeeReportingMapper reporting,
                               TOrganizationUnitMapper organizations,
                               UserAuthorizationPolicy authorizationPolicy) {
        this.current = current;
        this.employees = employees;
        this.assignments = assignments;
        this.reporting = reporting;
        this.organizations = organizations;
        this.authorizationPolicy = authorizationPolicy;
    }

    public TEmployee validate(Integer employeeId, Integer targetOrganizationId,
                              Integer managerEmployeeId, LocalDateTime at) {
        TOrganizationUnit targetOrganization = requireTargetOrganization(targetOrganizationId);
        Set<Integer> operatorScope = isBootstrapRootLeaderValidation(
                employeeId, managerEmployeeId, targetOrganization) ? null : operatorOrganizationScope(at);
        if (operatorScope != null && !operatorScope.contains(targetOrganizationId)) {
            throw new BusinessException(CodeEnum.ACCESS_DENIED, "目标任职组织超出操作者范围");
        }
        if (managerEmployeeId == null) {
            if (targetOrganization.getType() == OrganizationUnitType.COMPANY
                    && targetOrganization.getParentId() == null
                    && Objects.equals(targetOrganization.getLeaderEmployeeId(), employeeId)) {
                return null;
            }
            throw new BusinessException(CodeEnum.INVALID_MANAGER, "只有根公司负责人允许没有直属管理者");
        }
        TEmployee candidate = eligibleCandidates(employeeId, targetOrganizationId, at, operatorScope).stream()
                .filter(value -> value.getId().equals(managerEmployeeId))
                .findFirst()
                .orElseThrow(() -> new BusinessException(CodeEnum.INVALID_MANAGER,
                        "直属管理者必须位于目标主要组织或其上级组织，并处于可用状态"));
        if (wouldCreateCycle(employeeId, candidate.getId(), at, null)) {
            throw new BusinessException(CodeEnum.REPORTING_CYCLE, "直属管理者关系会形成当前或未来汇报环");
        }
        return candidate;
    }

    public List<TEmployee> candidates(Integer employeeId, Integer targetOrganizationId, LocalDateTime at) {
        requireTargetOrganization(targetOrganizationId);
        Set<Integer> operatorScope = operatorOrganizationScope(at);
        if (operatorScope != null && !operatorScope.contains(targetOrganizationId)) return List.of();
        return eligibleCandidates(employeeId, targetOrganizationId, at, operatorScope).stream()
                .filter(candidate -> !wouldCreateCycle(employeeId, candidate.getId(), at, null))
                .toList();
    }

    public List<TEmployee> actingCandidates(Integer employeeId, Integer targetOrganizationId, LocalDateTime at) {
        requireTargetOrganization(targetOrganizationId);
        Set<Integer> operatorScope = operatorOrganizationScope(at);
        if (operatorScope != null && !operatorScope.contains(targetOrganizationId)) return List.of();
        return eligibleCandidates(employeeId, targetOrganizationId, at, operatorScope);
    }

    public TEmployee validateActing(Integer employeeId, Integer targetOrganizationId,
                                    Integer managerEmployeeId, LocalDateTime effectiveFrom,
                                    LocalDateTime effectiveTo) {
        requireTargetOrganization(targetOrganizationId);
        Set<Integer> operatorScope = operatorOrganizationScope(effectiveFrom);
        if (operatorScope != null && !operatorScope.contains(targetOrganizationId)) {
            throw new BusinessException(CodeEnum.ACCESS_DENIED, "目标任职组织超出操作者范围");
        }
        TEmployee candidate = eligibleCandidates(employeeId, targetOrganizationId, effectiveFrom, operatorScope).stream()
                .filter(value -> Objects.equals(value.getId(), managerEmployeeId))
                .findFirst().orElseThrow(() -> new BusinessException(CodeEnum.INVALID_MANAGER,
                        "代理管理者必须位于目标主要组织或其上级组织，并处于可用状态"));
        if (wouldCreateCycle(employeeId, candidate.getId(), effectiveFrom, effectiveTo)) {
            throw new BusinessException(CodeEnum.REPORTING_CYCLE, "代理管理关系会在有效期内形成汇报环");
        }
        return candidate;
    }

    private List<TEmployee> eligibleCandidates(Integer employeeId, Integer targetOrganizationId,
                                                LocalDateTime at, Set<Integer> operatorScope) {
        List<Integer> candidateOrganizations = ancestorOrganizationIds(targetOrganizationId);
        if (operatorScope != null) {
            candidateOrganizations = candidateOrganizations.stream().filter(operatorScope::contains).toList();
        }
        if (candidateOrganizations.isEmpty()) return List.of();
        return employees.selectEligibleManagerCandidates(employeeId, at, candidateOrganizations);
    }

    private TOrganizationUnit requireTargetOrganization(Integer organizationId) {
        TOrganizationUnit organization = organizations.selectByPrimaryKey(organizationId);
        if (organization == null || !Boolean.TRUE.equals(organization.getEnabled())
                || Boolean.TRUE.equals(organization.getPlaceholder())) {
            throw new BusinessException(CodeEnum.ORGANIZATION_HIERARCHY_INVALID, "目标任职组织不可用");
        }
        return organization;
    }

    private Set<Integer> operatorOrganizationScope(LocalDateTime at) {
        if (authorizationPolicy.isGlobalOperator()) return null;
        TEmployee operator = employees.selectByUserId(current.getCurrentUserId());
        TEmployeeAssignment primary = operator == null ? null
                : assignments.selectCurrentPrimaryByEmployeeId(operator.getId(), at);
        if (primary == null) return Set.of();
        return new HashSet<>(organizations.selectDescendantIds(primary.getOrganizationUnitId()));
    }

    private boolean isBootstrapRootLeaderValidation(Integer employeeId, Integer managerEmployeeId,
                                                     TOrganizationUnit targetOrganization) {
        return managerEmployeeId == null
                && targetOrganization.getType() == OrganizationUnitType.COMPANY
                && targetOrganization.getParentId() == null
                && Objects.equals(targetOrganization.getLeaderEmployeeId(), employeeId)
                && authorizationPolicy.isBootstrapRecoveryOperator();
    }

    private List<Integer> ancestorOrganizationIds(Integer organizationId) {
        List<Integer> result = new ArrayList<>();
        Set<Integer> visited = new HashSet<>();
        Integer currentId = organizationId;
        while (currentId != null && visited.add(currentId)) {
            TOrganizationUnit value = organizations.selectByPrimaryKey(currentId);
            if (value == null || !Boolean.TRUE.equals(value.getEnabled())
                    || Boolean.TRUE.equals(value.getPlaceholder())) break;
            result.add(value.getId());
            currentId = value.getParentId();
        }
        return result;
    }

    private boolean wouldCreateCycle(Integer employeeId, Integer candidateManagerId,
                                     LocalDateTime effectiveFrom, LocalDateTime effectiveTo) {
        if (Objects.equals(employeeId, candidateManagerId)) return true;
        Deque<GraphState> queue = new ArrayDeque<>();
        Set<GraphState> visited = new HashSet<>();
        queue.add(new GraphState(candidateManagerId, effectiveFrom, effectiveTo));
        while (!queue.isEmpty()) {
            GraphState state = queue.removeFirst();
            if (!visited.add(state)) continue;
            if (Objects.equals(state.employeeId(), employeeId)) return true;
            for (TEmployeeReporting relation : reporting.selectOverlappingManagers(
                    state.employeeId(), state.effectiveFrom(), state.effectiveTo())) {
                LocalDateTime overlapFrom = relation.getEffectiveFrom().isAfter(state.effectiveFrom())
                        ? relation.getEffectiveFrom() : state.effectiveFrom();
                LocalDateTime overlapTo = earliest(state.effectiveTo(), relation.getEffectiveTo());
                if (overlapTo == null || overlapTo.isAfter(overlapFrom)) {
                    queue.addLast(new GraphState(relation.getManagerEmployeeId(), overlapFrom, overlapTo));
                }
            }
        }
        return false;
    }

    private LocalDateTime earliest(LocalDateTime left, LocalDateTime right) {
        if (left == null) return right;
        if (right == null) return left;
        return left.isBefore(right) ? left : right;
    }

    private record GraphState(Integer employeeId, LocalDateTime effectiveFrom, LocalDateTime effectiveTo) {}
}
