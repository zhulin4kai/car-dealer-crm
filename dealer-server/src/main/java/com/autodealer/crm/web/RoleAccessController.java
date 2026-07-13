package com.autodealer.crm.web;

import com.autodealer.crm.constant.PermissionCodes;
import com.autodealer.crm.dto.access.RoleDtos.*;
import com.autodealer.crm.result.R;
import com.autodealer.crm.service.RoleAccessService;
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
  public R<PageInfo<RoleResponse>> page(@RequestParam(defaultValue="1")int page,@RequestParam(defaultValue="10")int size,@RequestParam(required=false)String keyword,@RequestParam(required=false)Boolean enabled){return R.OK(service.page(page,size,keyword,enabled));}
  @PreAuthorize("hasAuthority('"+PermissionCodes.ROLE_VIEW+"')") @GetMapping("/api/roles/{id}") public R<RoleResponse> detail(@PathVariable Integer id){return R.OK(service.detail(id));}
  @PreAuthorize("hasAuthority('"+PermissionCodes.ROLE_ADD+"')") @PostMapping("/api/roles") public R<RoleResponse> create(@Valid @RequestBody CreateRoleRequest q){return R.OK(service.create(q));}
  @PreAuthorize("hasAuthority('"+PermissionCodes.ROLE_EDIT+"')") @PutMapping("/api/roles/{id}") public R<RoleResponse> update(@PathVariable Integer id,@Valid @RequestBody UpdateRoleRequest q){return R.OK(service.update(id,q));}
  @PreAuthorize("hasAuthority('"+PermissionCodes.ROLE_COPY+"')") @PostMapping("/api/roles/{id}/copy") public R<RoleResponse> copy(@PathVariable Integer id,@Valid @RequestBody CopyRoleRequest q){return R.OK(service.copy(id,q));}
  @PreAuthorize("hasAuthority('"+PermissionCodes.ROLE_STATUS+"')") @PutMapping("/api/roles/{id}/enable") public R<RoleResponse> enable(@PathVariable Integer id,@Valid @RequestBody ChangeRoleStatusRequest q){return R.OK(service.status(id,q,true));}
  @PreAuthorize("hasAuthority('"+PermissionCodes.ROLE_STATUS+"')") @PutMapping("/api/roles/{id}/disable") public R<RoleResponse> disable(@PathVariable Integer id,@Valid @RequestBody ChangeRoleStatusRequest q){return R.OK(service.status(id,q,false));}
  @PreAuthorize("hasAuthority('"+PermissionCodes.ROLE_VIEW+"')") @GetMapping("/api/roles/organization-options") public R<List<OrganizationOption>> orgs(){return R.OK(service.organizationOptions());}
  @PreAuthorize("hasAuthority('"+PermissionCodes.PERMISSION_LIST+"')") @GetMapping("/api/permissions/tree") public R<List<PermissionItem>> permissions(){return R.OK(service.permissionTree());}
  @PreAuthorize("hasAuthority('"+PermissionCodes.ROLE_VIEW+"') and hasAuthority('"+PermissionCodes.PERMISSION_LIST+"')") @GetMapping("/api/roles/{id}/permissions") public R<MatrixResponse> matrix(@PathVariable Integer id){return R.OK(service.matrix(id));}
  @PreAuthorize("hasAuthority('"+PermissionCodes.ROLE_PERMISSION_MANAGE+"')") @PostMapping("/api/roles/{id}/permissions/preview") public R<PreviewResponse> preview(@PathVariable Integer id,@Valid @RequestBody MatrixRequest q){return R.OK(service.preview(id,q));}
  @PreAuthorize("hasAuthority('"+PermissionCodes.ROLE_PERMISSION_MANAGE+"')") @PutMapping("/api/roles/{id}/permissions") public R<UpdateMatrixResponse> matrixUpdate(@PathVariable Integer id,@Valid @RequestBody UpdateMatrixRequest q){return R.OK(service.updateMatrix(id,q));}
}
