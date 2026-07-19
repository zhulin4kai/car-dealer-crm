package com.autodealer.crm.modules.identity.application.internal;

import com.autodealer.crm.modules.identity.application.api.*;

import com.autodealer.crm.modules.identity.application.api.security.CurrentUserProvider;
import com.autodealer.crm.modules.identity.application.api.enums.EmployeeStatus;
import com.autodealer.crm.modules.identity.application.api.enums.OrganizationUnitType;
import com.autodealer.crm.shared.error.BusinessException;
import com.autodealer.crm.modules.identity.persistence.mapper.TEmployeeAssignmentMapper;
import com.autodealer.crm.modules.identity.persistence.mapper.TEmployeeMapper;
import com.autodealer.crm.modules.identity.persistence.mapper.TEmployeeReportingMapper;
import com.autodealer.crm.modules.identity.persistence.mapper.TOrganizationUnitMapper;
import com.autodealer.crm.modules.identity.persistence.model.TEmployee;
import com.autodealer.crm.modules.identity.persistence.model.TOrganizationUnit;
import com.autodealer.crm.modules.identity.persistence.model.TEmployeeReporting;
import com.autodealer.crm.shared.error.CodeEnum;
import com.autodealer.crm.modules.identity.application.internal.DirectManagerPolicy;
import com.autodealer.crm.modules.identity.application.internal.UserAuthorizationPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DirectManagerPolicyTest {
    @Mock CurrentUserProvider current;
    @Mock TEmployeeMapper employees;
    @Mock TEmployeeAssignmentMapper assignments;
    @Mock TEmployeeReportingMapper reporting;
    @Mock TOrganizationUnitMapper organizations;
    @Mock UserAuthorizationPolicy authorizationPolicy;
    DirectManagerPolicy policy;
    LocalDateTime at;

    @BeforeEach
    void setUp() {
        policy = new DirectManagerPolicy(current, employees, assignments, reporting, organizations,
                authorizationPolicy);
        lenient().when(authorizationPolicy.isGlobalOperator()).thenReturn(true);
        at = LocalDateTime.of(2026, 7, 13, 12, 0);
    }

    @Test
    void ordinaryEmployeeCannotOmitDirectManager() {
        when(organizations.selectByPrimaryKey(30)).thenReturn(organization(30, 20,
                OrganizationUnitType.TEAM, null));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> policy.validate(100, 30, null, at));

        assertEquals(CodeEnum.INVALID_MANAGER, exception.getCodeEnum());
    }

    @Test
    void onlySelectedRootCompanyLeaderMayOmitDirectManager() {
        when(organizations.selectByPrimaryKey(10)).thenReturn(organization(10, null,
                OrganizationUnitType.COMPANY, 100));

        assertNull(policy.validate(100, 10, null, at));
    }

    @Test
    void bootstrapRecoveryMayOnlyValidateSelectedRootLeaderWithoutManager() {
        when(authorizationPolicy.isBootstrapRecoveryOperator()).thenReturn(true);
        when(organizations.selectByPrimaryKey(10)).thenReturn(organization(10, null,
                OrganizationUnitType.COMPANY, 100));

        assertNull(policy.validate(100, 10, null, at));
        verify(employees, never()).selectByUserId(any());
    }

    @Test
    void bootstrapRecoveryDoesNotGainGlobalCandidateScope() {
        when(authorizationPolicy.isGlobalOperator()).thenReturn(false);
        when(current.getCurrentUserId()).thenReturn(1);
        when(organizations.selectByPrimaryKey(30)).thenReturn(organization(30, 10,
                OrganizationUnitType.TEAM, 200));

        assertEquals(List.of(), policy.candidates(100, 30, at));
        verify(employees, never()).selectEligibleManagerCandidates(any(), any(), any());
    }

    @Test
    void roleNameAdminWithoutQualifiedFactsDoesNotGainGlobalManagerScope() {
        when(authorizationPolicy.isGlobalOperator()).thenReturn(false);
        when(current.getCurrentUserId()).thenReturn(20);
        when(organizations.selectByPrimaryKey(30)).thenReturn(organization(30, 10,
                OrganizationUnitType.TEAM, 200));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> policy.validate(100, 30, 200, at));

        assertEquals(CodeEnum.ACCESS_DENIED, exception.getCodeEnum());
        verify(current, never()).isAdmin();
    }

    @Test
    void candidateQueryIsAnchoredToTargetOrganizationAncestors() {
        TOrganizationUnit root = organization(10, null, OrganizationUnitType.COMPANY, 1);
        TOrganizationUnit department = organization(20, 10, OrganizationUnitType.DEPARTMENT, 2);
        TOrganizationUnit team = organization(30, 20, OrganizationUnitType.TEAM, 3);
        when(organizations.selectByPrimaryKey(30)).thenReturn(team);
        when(organizations.selectByPrimaryKey(20)).thenReturn(department);
        when(organizations.selectByPrimaryKey(10)).thenReturn(root);
        TEmployee manager = employee(200);
        when(employees.selectEligibleManagerCandidates(eq(100), eq(at), any())).thenReturn(List.of(manager));
        when(reporting.selectOverlappingManagers(200, at, null)).thenReturn(List.of());

        assertEquals(200, policy.validate(100, 30, 200, at).getId());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Integer>> organizationsCaptor = ArgumentCaptor.forClass(List.class);
        verify(employees).selectEligibleManagerCandidates(eq(100), eq(at), organizationsCaptor.capture());
        assertEquals(List.of(30, 20, 10), organizationsCaptor.getValue());
    }

    @Test
    void rejectsCycleThatOnlyAppearsThroughFutureDirectFacts() {
        TOrganizationUnit root = organization(10, null, OrganizationUnitType.COMPANY, 1);
        TOrganizationUnit team = organization(30, 10, OrganizationUnitType.TEAM, 3);
        when(organizations.selectByPrimaryKey(30)).thenReturn(team);
        when(organizations.selectByPrimaryKey(10)).thenReturn(root);
        when(employees.selectEligibleManagerCandidates(eq(100), eq(at), any()))
                .thenReturn(List.of(employee(200)));
        when(reporting.selectOverlappingManagers(200, at, null))
                .thenReturn(List.of(reporting(200, 300)));
        when(reporting.selectOverlappingManagers(300, at, null))
                .thenReturn(List.of(reporting(300, 100)));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> policy.validate(100, 30, 200, at));

        assertEquals(CodeEnum.REPORTING_CYCLE, exception.getCodeEnum());
    }

    @Test
    void actingCycleUsesTheIntersectionAcrossDirectAndActingIntervals() {
        LocalDateTime end = at.plusDays(10);
        when(organizations.selectByPrimaryKey(30)).thenReturn(organization(30, 10, OrganizationUnitType.TEAM, 3));
        when(organizations.selectByPrimaryKey(10)).thenReturn(organization(10, null, OrganizationUnitType.COMPANY, 1));
        when(employees.selectEligibleManagerCandidates(eq(100), eq(at), any())).thenReturn(List.of(employee(200)));
        when(reporting.selectOverlappingManagers(200, at, end))
                .thenReturn(List.of(reporting(200, 300, at.minusDays(1), at.plusDays(8))));
        when(reporting.selectOverlappingManagers(300, at, at.plusDays(8)))
                .thenReturn(List.of(reporting(300, 100, at.plusDays(2), at.plusDays(5))));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> policy.validateActing(100, 30, 200, at, end));

        assertEquals(CodeEnum.REPORTING_CYCLE, exception.getCodeEnum());
    }

    @Test
    void nonOverlappingIntervalsDoNotCreateAFalseCycle() {
        LocalDateTime end = at.plusDays(10);
        when(organizations.selectByPrimaryKey(30)).thenReturn(organization(30, 10, OrganizationUnitType.TEAM, 3));
        when(organizations.selectByPrimaryKey(10)).thenReturn(organization(10, null, OrganizationUnitType.COMPANY, 1));
        when(employees.selectEligibleManagerCandidates(eq(100), eq(at), any())).thenReturn(List.of(employee(200)));
        when(reporting.selectOverlappingManagers(200, at, end))
                .thenReturn(List.of(reporting(200, 300, at, at.plusDays(3))));
        when(reporting.selectOverlappingManagers(300, at, at.plusDays(3))).thenReturn(List.of());

        assertDoesNotThrow(() -> policy.validateActing(100, 30, 200, at, end));
        verify(reporting).selectOverlappingManagers(300, at, at.plusDays(3));
    }

    private static TOrganizationUnit organization(Integer id, Integer parentId,
                                                   OrganizationUnitType type, Integer leaderId) {
        TOrganizationUnit value = new TOrganizationUnit();
        value.setId(id);
        value.setParentId(parentId);
        value.setType(type);
        value.setLeaderEmployeeId(leaderId);
        value.setEnabled(true);
        value.setPlaceholder(false);
        return value;
    }

    private static TEmployee employee(Integer id) {
        TEmployee value = new TEmployee();
        value.setId(id);
        value.setEmploymentStatus(EmployeeStatus.ACTIVE);
        return value;
    }

    private static TEmployeeReporting reporting(Integer subordinateId, Integer managerId) {
        return reporting(subordinateId, managerId, LocalDateTime.of(2026, 1, 1, 0, 0), null);
    }

    private static TEmployeeReporting reporting(Integer subordinateId, Integer managerId,
                                                 LocalDateTime effectiveFrom, LocalDateTime effectiveTo) {
        TEmployeeReporting value = new TEmployeeReporting();
        value.setSubordinateEmployeeId(subordinateId);
        value.setManagerEmployeeId(managerId);
        value.setEffectiveFrom(effectiveFrom);
        value.setEffectiveTo(effectiveTo);
        return value;
    }
}
