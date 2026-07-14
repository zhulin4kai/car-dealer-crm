package com.autodealer.crm.modules.identity.web;

import com.autodealer.crm.shared.security.PermissionCodes;
import com.autodealer.crm.modules.identity.application.api.dto.access.UserAuthorizationDtos.*;
import com.autodealer.crm.shared.web.Result;
import com.autodealer.crm.modules.identity.application.api.AuthorizationService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
public class UserAuthorizationController {
    private final AuthorizationService service;
    public UserAuthorizationController(AuthorizationService service) { this.service = service; }

    @PreAuthorize("#id == authentication.principal.id or hasAuthority('" + PermissionCodes.USER_VIEW + "')")
    @GetMapping("/api/users/{id}/authorization")
    public Result<Detail> get(@PathVariable Integer id) { return Result.OK(service.get(id)); }

    @PreAuthorize("hasAuthority('" + PermissionCodes.USER_ROLE + "')")
    @PutMapping("/api/users/{id}/authorization/roles")
    public Result<Detail> roles(@PathVariable Integer id, @Valid @RequestBody UpdateRolesRequest request) {
        return Result.OK(service.replaceRoles(id, request));
    }

    @PreAuthorize("hasAuthority('" + PermissionCodes.USER_PERMISSION + "')")
    @PutMapping("/api/users/{id}/authorization/permissions")
    public Result<Detail> permissions(@PathVariable Integer id, @Valid @RequestBody UpdatePermissionsRequest request) {
        return Result.OK(service.updatePermissions(id, request));
    }

    @PreAuthorize("hasAuthority('" + PermissionCodes.USER_ROLE + "')")
    @PutMapping("/api/users/authorization/batch/roles")
    public Result<BatchResult> batchRoles(@Valid @RequestBody BatchUpdateRolesRequest request) {
        return Result.OK(service.batchUpdateRoles(request));
    }

    @PreAuthorize("hasAuthority('" + PermissionCodes.USER_PERMISSION + "')")
    @PutMapping("/api/users/authorization/batch/permissions")
    public Result<BatchResult> batchPermissions(@Valid @RequestBody BatchUpdatePermissionsRequest request) {
        return Result.OK(service.batchUpdatePermissions(request));
    }
}
