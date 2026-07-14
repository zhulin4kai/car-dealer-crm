package com.autodealer.crm.modules.identity.application.api;

import com.autodealer.crm.modules.identity.application.api.dto.organization.ActingReportingCollectionResponse;
import com.autodealer.crm.modules.identity.application.api.dto.organization.ChangeEntityStatusRequest;
import com.autodealer.crm.modules.identity.application.api.dto.organization.CreateOrganizationUnitRequest;
import com.autodealer.crm.modules.identity.application.api.dto.organization.CreatePositionRequest;
import com.autodealer.crm.modules.identity.application.api.dto.organization.EmployeeOrganizationMembershipResponse;
import com.autodealer.crm.modules.identity.application.api.dto.organization.EmployeeSummaryResponse;
import com.autodealer.crm.modules.identity.application.api.dto.organization.ManagerCandidateResponse;
import com.autodealer.crm.modules.identity.application.api.dto.organization.OrganizationChangeHistoryResponse;
import com.autodealer.crm.modules.identity.application.api.dto.organization.OrganizationUnitResponse;
import com.autodealer.crm.modules.identity.application.api.dto.organization.PositionResponse;
import com.autodealer.crm.modules.identity.application.api.dto.organization.ReplaceActingReportingsRequest;
import com.autodealer.crm.modules.identity.application.api.dto.organization.UpdateEmployeeOrganizationRequest;
import com.autodealer.crm.modules.identity.application.api.dto.organization.UpdateOrganizationUnitRequest;
import com.autodealer.crm.modules.identity.application.api.dto.organization.UpdatePositionRequest;
import com.autodealer.crm.modules.identity.application.api.dto.organization.*;
import com.autodealer.crm.modules.identity.application.api.enums.OrganizationUnitType;

import java.util.List;

public interface OrganizationService {
    List<OrganizationUnitResponse> getOrganizationTree();
    OrganizationUnitResponse createOrganizationUnit(CreateOrganizationUnitRequest request);
    OrganizationUnitResponse updateOrganizationUnit(Integer id, UpdateOrganizationUnitRequest request);
    OrganizationUnitResponse changeOrganizationUnitStatus(Integer id, ChangeEntityStatusRequest request, boolean enabled);
    List<OrganizationUnitResponse> getParentCandidates(OrganizationUnitType type, Integer excludeId);
    List<ManagerCandidateResponse> getLeaderCandidates(Integer organizationUnitId, Integer parentId);
    List<PositionResponse> getPositions();
    PositionResponse createPosition(CreatePositionRequest request);
    PositionResponse updatePosition(Integer id, UpdatePositionRequest request);
    PositionResponse changePositionStatus(Integer id, ChangeEntityStatusRequest request, boolean enabled);
    List<EmployeeSummaryResponse> getOrganizationEmployees(Integer organizationUnitId);
    EmployeeOrganizationMembershipResponse getEmployeeOrganizationMembership(Integer employeeId);
    EmployeeOrganizationMembershipResponse updateEmployeeOrganizationMembership(
            Integer employeeId, UpdateEmployeeOrganizationRequest request);
    List<ManagerCandidateResponse> getManagerCandidates(Integer employeeId, Integer targetOrganizationId);
    default List<ManagerCandidateResponse> getManagerCandidates(Integer employeeId) {
        return getManagerCandidates(employeeId, null);
    }
    ActingReportingCollectionResponse getActingReportings(Integer employeeId);
    ActingReportingCollectionResponse replaceActingReportings(
            Integer employeeId, ReplaceActingReportingsRequest request);
    List<ManagerCandidateResponse> getActingManagerCandidates(Integer employeeId);
    List<OrganizationChangeHistoryResponse> getOrganizationHistory(Integer employeeId);
}
