package com.autodealer.crm.modules.identity.web;

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
import com.autodealer.crm.shared.security.PermissionCodes;
import com.autodealer.crm.modules.identity.application.api.dto.organization.*;
import com.autodealer.crm.modules.identity.application.api.enums.OrganizationUnitType;
import com.autodealer.crm.shared.web.Result;
import com.autodealer.crm.modules.identity.application.api.OrganizationService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class OrganizationController {
    private final OrganizationService organizationService;

    public OrganizationController(OrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    @PreAuthorize("hasAuthority('" + PermissionCodes.ORGANIZATION_LIST + "')")
    @GetMapping("/api/organization-units/tree")
    public Result<List<OrganizationUnitResponse>> organizationTree() {
        return Result.OK(organizationService.getOrganizationTree());
    }

    @PreAuthorize("hasAuthority('" + PermissionCodes.ORGANIZATION_ADD + "')")
    @PostMapping("/api/organization-units")
    public Result<OrganizationUnitResponse> createOrganization(@Valid @RequestBody CreateOrganizationUnitRequest request) {
        return Result.OK(organizationService.createOrganizationUnit(request));
    }

    @PreAuthorize("hasAuthority('" + PermissionCodes.ORGANIZATION_EDIT + "')")
    @PutMapping("/api/organization-units/{id}")
    public Result<OrganizationUnitResponse> updateOrganization(
            @PathVariable Integer id, @Valid @RequestBody UpdateOrganizationUnitRequest request) {
        return Result.OK(organizationService.updateOrganizationUnit(id, request));
    }

    @PreAuthorize("hasAuthority('" + PermissionCodes.ORGANIZATION_STATUS + "')")
    @PutMapping("/api/organization-units/{id}/enable")
    public Result<OrganizationUnitResponse> enableOrganization(
            @PathVariable Integer id, @Valid @RequestBody ChangeEntityStatusRequest request) {
        return Result.OK(organizationService.changeOrganizationUnitStatus(id, request, true));
    }

    @PreAuthorize("hasAuthority('" + PermissionCodes.ORGANIZATION_STATUS + "')")
    @PutMapping("/api/organization-units/{id}/disable")
    public Result<OrganizationUnitResponse> disableOrganization(
            @PathVariable Integer id, @Valid @RequestBody ChangeEntityStatusRequest request) {
        return Result.OK(organizationService.changeOrganizationUnitStatus(id, request, false));
    }

    @PreAuthorize("hasAuthority('" + PermissionCodes.ORGANIZATION_VIEW + "')")
    @GetMapping("/api/organization-units/parent-candidates")
    public Result<List<OrganizationUnitResponse>> parentCandidates(
            @RequestParam OrganizationUnitType type,
            @RequestParam(required = false) Integer excludeId) {
        return Result.OK(organizationService.getParentCandidates(type, excludeId));
    }

    @PreAuthorize("hasAuthority('" + PermissionCodes.ORGANIZATION_VIEW + "')")
    @GetMapping("/api/organization-units/leader-candidates")
    public Result<List<ManagerCandidateResponse>> leaderCandidates(
            @RequestParam(required = false) Integer organizationUnitId,
            @RequestParam(required = false) Integer parentId) {
        return Result.OK(organizationService.getLeaderCandidates(organizationUnitId, parentId));
    }

    @PreAuthorize("hasAuthority('" + PermissionCodes.POSITION_LIST + "')")
    @GetMapping("/api/positions")
    public Result<List<PositionResponse>> positions() {
        return Result.OK(organizationService.getPositions());
    }

    @PreAuthorize("hasAuthority('" + PermissionCodes.POSITION_ADD + "')")
    @PostMapping("/api/positions")
    public Result<PositionResponse> createPosition(@Valid @RequestBody CreatePositionRequest request) {
        return Result.OK(organizationService.createPosition(request));
    }

    @PreAuthorize("hasAuthority('" + PermissionCodes.POSITION_EDIT + "')")
    @PutMapping("/api/positions/{id}")
    public Result<PositionResponse> updatePosition(
            @PathVariable Integer id, @Valid @RequestBody UpdatePositionRequest request) {
        return Result.OK(organizationService.updatePosition(id, request));
    }

    @PreAuthorize("hasAuthority('" + PermissionCodes.POSITION_STATUS + "')")
    @PutMapping("/api/positions/{id}/enable")
    public Result<PositionResponse> enablePosition(
            @PathVariable Integer id, @Valid @RequestBody ChangeEntityStatusRequest request) {
        return Result.OK(organizationService.changePositionStatus(id, request, true));
    }

    @PreAuthorize("hasAuthority('" + PermissionCodes.POSITION_STATUS + "')")
    @PutMapping("/api/positions/{id}/disable")
    public Result<PositionResponse> disablePosition(
            @PathVariable Integer id, @Valid @RequestBody ChangeEntityStatusRequest request) {
        return Result.OK(organizationService.changePositionStatus(id, request, false));
    }

    @PreAuthorize("hasAuthority('" + PermissionCodes.ORGANIZATION_VIEW + "')")
    @GetMapping("/api/organization-units/{id}/employees")
    public Result<List<EmployeeSummaryResponse>> organizationEmployees(@PathVariable Integer id) {
        return Result.OK(organizationService.getOrganizationEmployees(id));
    }

    @PreAuthorize("hasAuthority('" + PermissionCodes.ORGANIZATION_VIEW + "')")
    @GetMapping("/api/employees/{id}/organization-membership")
    public Result<EmployeeOrganizationMembershipResponse> employeeMembership(@PathVariable Integer id) {
        return Result.OK(organizationService.getEmployeeOrganizationMembership(id));
    }

    @PreAuthorize("hasAuthority('" + PermissionCodes.EMPLOYEE_ASSIGNMENT + "') or "
            + "hasAuthority('" + PermissionCodes.EMPLOYEE_REPORTING + "')")
    @PutMapping("/api/employees/{id}/organization-membership")
    public Result<EmployeeOrganizationMembershipResponse> updateEmployeeMembership(
            @PathVariable Integer id, @Valid @RequestBody UpdateEmployeeOrganizationRequest request) {
        return Result.OK(organizationService.updateEmployeeOrganizationMembership(id, request));
    }

    @PreAuthorize("hasAuthority('" + PermissionCodes.EMPLOYEE_REPORTING + "')")
    @GetMapping("/api/employees/{id}/manager-candidates")
    public Result<List<ManagerCandidateResponse>> managerCandidates(
            @PathVariable Integer id,
            @RequestParam(required = false) Integer organizationUnitId) {
        return Result.OK(organizationService.getManagerCandidates(id, organizationUnitId));
    }

    @PreAuthorize("hasAuthority('" + PermissionCodes.ORGANIZATION_VIEW + "')")
    @GetMapping("/api/employees/{id}/acting-reporting-relations")
    public Result<ActingReportingCollectionResponse> actingReportings(@PathVariable Integer id) {
        return Result.OK(organizationService.getActingReportings(id));
    }

    @PreAuthorize("hasAuthority('" + PermissionCodes.EMPLOYEE_REPORTING + "')")
    @PutMapping("/api/employees/{id}/acting-reporting-relations")
    public Result<ActingReportingCollectionResponse> replaceActingReportings(
            @PathVariable Integer id, @Valid @RequestBody ReplaceActingReportingsRequest request) {
        return Result.OK(organizationService.replaceActingReportings(id, request));
    }

    @PreAuthorize("hasAuthority('" + PermissionCodes.EMPLOYEE_REPORTING + "')")
    @GetMapping("/api/employees/{id}/acting-reporting-relations/manager-candidates")
    public Result<List<ManagerCandidateResponse>> actingManagerCandidates(@PathVariable Integer id) {
        return Result.OK(organizationService.getActingManagerCandidates(id));
    }

    @PreAuthorize("hasAuthority('" + PermissionCodes.ORGANIZATION_VIEW + "')")
    @GetMapping("/api/employees/{id}/organization-history")
    public Result<List<OrganizationChangeHistoryResponse>> organizationHistory(@PathVariable Integer id) {
        return Result.OK(organizationService.getOrganizationHistory(id));
    }
}
