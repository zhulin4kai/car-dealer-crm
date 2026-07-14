package com.autodealer.crm.modules.identity.application.api;

import com.autodealer.crm.modules.identity.application.api.dto.access.UserAuthorizationDtos.*;

public interface AuthorizationService {
    Detail get(Integer userId);
    Detail replaceRoles(Integer userId, UpdateRolesRequest request);
    Detail updatePermissions(Integer userId, UpdatePermissionsRequest request);
    BatchResult batchUpdateRoles(BatchUpdateRolesRequest request);
    BatchResult batchUpdatePermissions(BatchUpdatePermissionsRequest request);
}
