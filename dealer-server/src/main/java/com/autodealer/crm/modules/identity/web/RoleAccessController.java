package com.autodealer.crm.modules.identity.web;

import com.autodealer.crm.shared.security.PermissionCodes;
import com.autodealer.crm.modules.identity.application.api.dto.access.RoleDtos.*;
import com.autodealer.crm.shared.web.Result;
import com.autodealer.crm.modules.identity.application.api.RoleAccessService;
import com.github.pagehelper.PageInfo;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
public class RoleAccessController {
  private final RoleAccessService service;
  public RoleAccessController(RoleAccessService service){this.service=service;}
  @PreAuthorize("hasAuthority('"+PermissionCodes.ROLE_LIST+"')") @GetMapping("/api/roles")
  public Result<PageInfo<RoleResponse>> page(@RequestParam(defaultValue="1")int page,@RequestParam(defaultValue="10")int size,@RequestParam(required=false)String keyword,@RequestParam(required=false)Boolean enabled){return Result.OK(service.page(page,size,keyword,enabled));}
  @PreAuthorize("hasAuthority('"+PermissionCodes.ROLE_VIEW+"')") @GetMapping("/api/roles/{id}") public Result<RoleResponse> detail(@PathVariable Integer id){return Result.OK(service.detail(id));}
  @PreAuthorize("hasAuthority('"+PermissionCodes.ROLE_ADD+"')") @PostMapping("/api/roles") public Result<RoleResponse> create(@Valid @RequestBody CreateRoleRequest q){return Result.OK(service.create(q));}
  @PreAuthorize("hasAuthority('"+PermissionCodes.ROLE_EDIT+"')") @PutMapping("/api/roles/{id}") public Result<RoleResponse> update(@PathVariable Integer id,@Valid @RequestBody UpdateRoleRequest q){return Result.OK(service.update(id,q));}
  @PreAuthorize("hasAuthority('"+PermissionCodes.ROLE_COPY+"')") @PostMapping("/api/roles/{id}/copy") public Result<RoleResponse> copy(@PathVariable Integer id,@Valid @RequestBody CopyRoleRequest q){return Result.OK(service.copy(id,q));}
  @PreAuthorize("hasAuthority('"+PermissionCodes.ROLE_STATUS+"')") @PutMapping("/api/roles/{id}/enable") public Result<RoleResponse> enable(@PathVariable Integer id,@Valid @RequestBody ChangeRoleStatusRequest q){return Result.OK(service.status(id,q,true));}
  @PreAuthorize("hasAuthority('"+PermissionCodes.ROLE_STATUS+"')") @PutMapping("/api/roles/{id}/disable") public Result<RoleResponse> disable(@PathVariable Integer id,@Valid @RequestBody ChangeRoleStatusRequest q){return Result.OK(service.status(id,q,false));}
  @PreAuthorize("hasAuthority('"+PermissionCodes.ROLE_VIEW+"')") @GetMapping("/api/roles/organization-options") public Result<List<OrganizationOption>> orgs(){return Result.OK(service.organizationOptions());}
  @PreAuthorize("hasAuthority('"+PermissionCodes.PERMISSION_LIST+"')") @GetMapping("/api/permissions/tree") public Result<List<PermissionItem>> permissions(){return Result.OK(service.permissionTree());}
  @PreAuthorize("hasAuthority('"+PermissionCodes.ROLE_VIEW+"') and hasAuthority('"+PermissionCodes.PERMISSION_LIST+"')") @GetMapping("/api/roles/{id}/permissions") public Result<MatrixResponse> matrix(@PathVariable Integer id){return Result.OK(service.matrix(id));}
  @PreAuthorize("hasAuthority('"+PermissionCodes.ROLE_PERMISSION_MANAGE+"')") @PostMapping("/api/roles/{id}/permissions/preview") public Result<PreviewResponse> preview(@PathVariable Integer id,@Valid @RequestBody MatrixRequest q){return Result.OK(service.preview(id,q));}
  @PreAuthorize("hasAuthority('"+PermissionCodes.ROLE_PERMISSION_MANAGE+"')") @PutMapping("/api/roles/{id}/permissions") public Result<UpdateMatrixResponse> matrixUpdate(@PathVariable Integer id,@Valid @RequestBody UpdateMatrixRequest q){return Result.OK(service.updateMatrix(id,q));}
}
