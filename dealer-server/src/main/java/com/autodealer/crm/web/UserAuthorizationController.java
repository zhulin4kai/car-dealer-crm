package com.autodealer.crm.web;

import com.autodealer.crm.constant.PermissionCodes;
import com.autodealer.crm.dto.access.UserAuthorizationDtos.*;
import com.autodealer.crm.result.R;
import com.autodealer.crm.service.AuthorizationService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
public class UserAuthorizationController {
    private final AuthorizationService service;
    public UserAuthorizationController(AuthorizationService service) { this.service = service; }

    @PreAuthorize("#id == authentication.principal.id or hasAuthority('" + PermissionCodes.USER_VIEW + "')")
    @GetMapping("/api/users/{id}/authorization")
    public R<Detail> get(@PathVariable Integer id) { return R.OK(service.get(id)); }

    @PreAuthorize("hasAuthority('" + PermissionCodes.USER_ROLE + "')")
    @PutMapping("/api/users/{id}/authorization/roles")
    public R<Detail> roles(@PathVariable Integer id, @Valid @RequestBody UpdateRolesRequest request) {
        return R.OK(service.replaceRoles(id, request));
    }

    @PreAuthorize("hasAuthority('" + PermissionCodes.USER_PERMISSION + "')")
    @PutMapping("/api/users/{id}/authorization/permissions")
    public R<Detail> permissions(@PathVariable Integer id, @Valid @RequestBody UpdatePermissionsRequest request) {
        return R.OK(service.updatePermissions(id, request));
    }

    @PreAuthorize("hasAuthority('" + PermissionCodes.USER_ROLE + "')")
    @PutMapping("/api/users/authorization/batch/roles")
    public R<BatchResult> batchRoles(@Valid @RequestBody BatchUpdateRolesRequest request) {
        return R.OK(service.batchUpdateRoles(request));
    }

    @PreAuthorize("hasAuthority('" + PermissionCodes.USER_PERMISSION + "')")
    @PutMapping("/api/users/authorization/batch/permissions")
    public R<BatchResult> batchPermissions(@Valid @RequestBody BatchUpdatePermissionsRequest request) {
        return R.OK(service.batchUpdatePermissions(request));
    }
}
