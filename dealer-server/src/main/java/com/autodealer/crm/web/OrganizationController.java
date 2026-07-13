package com.autodealer.crm.web;

import com.autodealer.crm.constant.PermissionCodes;
import com.autodealer.crm.dto.organization.*;
import com.autodealer.crm.enums.OrganizationUnitType;
import com.autodealer.crm.result.R;
import com.autodealer.crm.service.OrganizationService;
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
    public R<List<OrganizationUnitResponse>> organizationTree() {
        return R.OK(organizationService.getOrganizationTree());
    }

    @PreAuthorize("hasAuthority('" + PermissionCodes.ORGANIZATION_ADD + "')")
    @PostMapping("/api/organization-units")
    public R<OrganizationUnitResponse> createOrganization(@Valid @RequestBody CreateOrganizationUnitRequest request) {
        return R.OK(organizationService.createOrganizationUnit(request));
    }

    @PreAuthorize("hasAuthority('" + PermissionCodes.ORGANIZATION_EDIT + "')")
    @PutMapping("/api/organization-units/{id}")
    public R<OrganizationUnitResponse> updateOrganization(
            @PathVariable Integer id, @Valid @RequestBody UpdateOrganizationUnitRequest request) {
        return R.OK(organizationService.updateOrganizationUnit(id, request));
    }

    @PreAuthorize("hasAuthority('" + PermissionCodes.ORGANIZATION_STATUS + "')")
    @PutMapping("/api/organization-units/{id}/enable")
    public R<OrganizationUnitResponse> enableOrganization(
            @PathVariable Integer id, @Valid @RequestBody ChangeEntityStatusRequest request) {
        return R.OK(organizationService.changeOrganizationUnitStatus(id, request, true));
    }

    @PreAuthorize("hasAuthority('" + PermissionCodes.ORGANIZATION_STATUS + "')")
    @PutMapping("/api/organization-units/{id}/disable")
    public R<OrganizationUnitResponse> disableOrganization(
            @PathVariable Integer id, @Valid @RequestBody ChangeEntityStatusRequest request) {
        return R.OK(organizationService.changeOrganizationUnitStatus(id, request, false));
    }

    @PreAuthorize("hasAuthority('" + PermissionCodes.ORGANIZATION_VIEW + "')")
    @GetMapping("/api/organization-units/parent-candidates")
    public R<List<OrganizationUnitResponse>> parentCandidates(
            @RequestParam OrganizationUnitType type,
            @RequestParam(required = false) Integer excludeId) {
        return R.OK(organizationService.getParentCandidates(type, excludeId));
    }

    @PreAuthorize("hasAuthority('" + PermissionCodes.ORGANIZATION_VIEW + "')")
    @GetMapping("/api/organization-units/leader-candidates")
    public R<List<ManagerCandidateResponse>> leaderCandidates(
            @RequestParam(required = false) Integer organizationUnitId,
            @RequestParam(required = false) Integer parentId) {
        return R.OK(organizationService.getLeaderCandidates(organizationUnitId, parentId));
    }

    @PreAuthorize("hasAuthority('" + PermissionCodes.POSITION_LIST + "')")
    @GetMapping("/api/positions")
    public R<List<PositionResponse>> positions() {
        return R.OK(organizationService.getPositions());
    }

    @PreAuthorize("hasAuthority('" + PermissionCodes.POSITION_ADD + "')")
    @PostMapping("/api/positions")
    public R<PositionResponse> createPosition(@Valid @RequestBody CreatePositionRequest request) {
        return R.OK(organizationService.createPosition(request));
    }

    @PreAuthorize("hasAuthority('" + PermissionCodes.POSITION_EDIT + "')")
    @PutMapping("/api/positions/{id}")
    public R<PositionResponse> updatePosition(
            @PathVariable Integer id, @Valid @RequestBody UpdatePositionRequest request) {
        return R.OK(organizationService.updatePosition(id, request));
    }

    @PreAuthorize("hasAuthority('" + PermissionCodes.POSITION_STATUS + "')")
    @PutMapping("/api/positions/{id}/enable")
    public R<PositionResponse> enablePosition(
            @PathVariable Integer id, @Valid @RequestBody ChangeEntityStatusRequest request) {
        return R.OK(organizationService.changePositionStatus(id, request, true));
    }

    @PreAuthorize("hasAuthority('" + PermissionCodes.POSITION_STATUS + "')")
    @PutMapping("/api/positions/{id}/disable")
    public R<PositionResponse> disablePosition(
            @PathVariable Integer id, @Valid @RequestBody ChangeEntityStatusRequest request) {
        return R.OK(organizationService.changePositionStatus(id, request, false));
    }

    @PreAuthorize("hasAuthority('" + PermissionCodes.ORGANIZATION_VIEW + "')")
    @GetMapping("/api/organization-units/{id}/employees")
    public R<List<EmployeeSummaryResponse>> organizationEmployees(@PathVariable Integer id) {
        return R.OK(organizationService.getOrganizationEmployees(id));
    }

    @PreAuthorize("hasAuthority('" + PermissionCodes.ORGANIZATION_VIEW + "')")
    @GetMapping("/api/employees/{id}/organization-membership")
    public R<EmployeeOrganizationMembershipResponse> employeeMembership(@PathVariable Integer id) {
        return R.OK(organizationService.getEmployeeOrganizationMembership(id));
    }

    @PreAuthorize("hasAuthority('" + PermissionCodes.EMPLOYEE_ASSIGNMENT + "') or "
            + "hasAuthority('" + PermissionCodes.EMPLOYEE_REPORTING + "')")
    @PutMapping("/api/employees/{id}/organization-membership")
    public R<EmployeeOrganizationMembershipResponse> updateEmployeeMembership(
            @PathVariable Integer id, @Valid @RequestBody UpdateEmployeeOrganizationRequest request) {
        return R.OK(organizationService.updateEmployeeOrganizationMembership(id, request));
    }

    @PreAuthorize("hasAuthority('" + PermissionCodes.EMPLOYEE_REPORTING + "')")
    @GetMapping("/api/employees/{id}/manager-candidates")
    public R<List<ManagerCandidateResponse>> managerCandidates(
            @PathVariable Integer id,
            @RequestParam(required = false) Integer organizationUnitId) {
        return R.OK(organizationService.getManagerCandidates(id, organizationUnitId));
    }

    @PreAuthorize("hasAuthority('" + PermissionCodes.ORGANIZATION_VIEW + "')")
    @GetMapping("/api/employees/{id}/acting-reporting-relations")
    public R<ActingReportingCollectionResponse> actingReportings(@PathVariable Integer id) {
        return R.OK(organizationService.getActingReportings(id));
    }

    @PreAuthorize("hasAuthority('" + PermissionCodes.EMPLOYEE_REPORTING + "')")
    @PutMapping("/api/employees/{id}/acting-reporting-relations")
    public R<ActingReportingCollectionResponse> replaceActingReportings(
            @PathVariable Integer id, @Valid @RequestBody ReplaceActingReportingsRequest request) {
        return R.OK(organizationService.replaceActingReportings(id, request));
    }

    @PreAuthorize("hasAuthority('" + PermissionCodes.EMPLOYEE_REPORTING + "')")
    @GetMapping("/api/employees/{id}/acting-reporting-relations/manager-candidates")
    public R<List<ManagerCandidateResponse>> actingManagerCandidates(@PathVariable Integer id) {
        return R.OK(organizationService.getActingManagerCandidates(id));
    }

    @PreAuthorize("hasAuthority('" + PermissionCodes.ORGANIZATION_VIEW + "')")
    @GetMapping("/api/employees/{id}/organization-history")
    public R<List<OrganizationChangeHistoryResponse>> organizationHistory(@PathVariable Integer id) {
        return R.OK(organizationService.getOrganizationHistory(id));
    }
}
