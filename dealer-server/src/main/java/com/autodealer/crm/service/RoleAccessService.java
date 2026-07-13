package com.autodealer.crm.service;

import com.autodealer.crm.dto.access.RoleDtos.*;
import com.github.pagehelper.PageInfo;
import java.util.List;

public interface RoleAccessService {
    PageInfo<RoleResponse> page(int page, int size, String keyword, Boolean enabled);
    RoleResponse detail(Integer id);
    RoleResponse create(CreateRoleRequest request);
    RoleResponse update(Integer id, UpdateRoleRequest request);
    RoleResponse copy(Integer id, CopyRoleRequest request);
    RoleResponse status(Integer id, ChangeRoleStatusRequest request, boolean enabled);
    List<OrganizationOption> organizationOptions();
    List<PermissionItem> permissionTree();
    MatrixResponse matrix(Integer roleId);
    PreviewResponse preview(Integer roleId, MatrixRequest request);
    UpdateMatrixResponse updateMatrix(Integer roleId, UpdateMatrixRequest request);
}
