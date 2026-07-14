package com.autodealer.crm.modules.identity.application.internal;
import com.autodealer.crm.modules.identity.application.api.dto.UserDetailResponse;
import com.autodealer.crm.modules.identity.application.api.model.TUser;
import java.util.Collections;
public final class UserConverter {
    private UserConverter() {}
    public static UserDetailResponse toDetailResponse(TUser tUser) {
        if (tUser == null) return null;
        UserDetailResponse r = new UserDetailResponse();
        r.setId(tUser.getId());
        r.setLoginAct(tUser.getLoginAct());
        r.setName(tUser.getName());
        r.setPhone(tUser.getPhone());
        r.setEmail(tUser.getEmail());
        r.setAccountNoExpired(tUser.getAccountNoExpired());
        r.setCredentialsNoExpired(tUser.getCredentialsNoExpired());
        r.setAccountNoLocked(tUser.getAccountNoLocked());
        r.setAccountEnabled(tUser.getAccountEnabled());
        r.setCreateTime(tUser.getCreateTime());
        r.setCreateBy(tUser.getCreateBy());
        r.setEditTime(tUser.getEditTime());
        r.setEditBy(tUser.getEditBy());
        r.setLastLoginTime(tUser.getLastLoginTime());
        r.setRoleList(tUser.getRoleList() != null ? tUser.getRoleList() : Collections.emptyList());
        r.setPermissionList(tUser.getPermissionList() != null ? tUser.getPermissionList() : Collections.emptyList());
        if (tUser.getCreateByDO() != null) {
            UserDetailResponse.UserRef ref = new UserDetailResponse.UserRef();
            ref.setId(tUser.getCreateByDO().getId());
            ref.setName(tUser.getCreateByDO().getName());
            r.setCreateByDO(ref);
        }
        if (tUser.getEditByDO() != null) {
            UserDetailResponse.UserRef ref = new UserDetailResponse.UserRef();
            ref.setId(tUser.getEditByDO().getId());
            ref.setName(tUser.getEditByDO().getName());
            r.setEditByDO(ref);
        }
        return r;
    }
}
