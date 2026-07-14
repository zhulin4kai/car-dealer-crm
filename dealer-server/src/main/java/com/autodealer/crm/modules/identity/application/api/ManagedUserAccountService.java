package com.autodealer.crm.modules.identity.application.api;

import com.autodealer.crm.modules.identity.application.api.dto.user.ManagedUserDtos.Detail;
import com.autodealer.crm.modules.identity.application.api.dto.user.ManagedUserDtos.ProfileRequest;
import com.autodealer.crm.modules.identity.application.api.dto.user.ManagedUserDtos.LoginAccountRequest;
import com.autodealer.crm.modules.identity.application.api.dto.user.ManagedUserDtos.SecurityExpirationRequest;
import com.autodealer.crm.modules.identity.application.api.dto.user.ManagedUserDtos.StatusRequest;
import com.autodealer.crm.modules.identity.application.api.dto.user.ManagedUserDtos.FilterOptions;
import com.autodealer.crm.modules.identity.application.api.dto.user.ManagedUserDtos.Summary;
import com.autodealer.crm.modules.identity.application.api.dto.UserListQuery;
import com.github.pagehelper.PageInfo;
public interface ManagedUserAccountService {
    PageInfo<Summary> list(UserListQuery query);
    FilterOptions getFilterOptions(Integer organizationUnitId);
    Detail getDetail(Integer userId);
    Detail changeStatus(Integer userId,StatusRequest request);
    Detail updateProfile(Integer userId,ProfileRequest request);
    Detail changeLoginAccount(Integer userId, LoginAccountRequest request);
    Detail changeSecurityExpiration(Integer userId, SecurityExpirationRequest request);
}
