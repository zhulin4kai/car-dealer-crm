package com.autodealer.crm.modules.identity.web;

import com.autodealer.crm.shared.security.PermissionCodes;
import com.autodealer.crm.modules.identity.application.api.security.UserManagementAccessGate;

import com.autodealer.crm.modules.identity.application.api.dto.AssignUserRolesRequest;
import com.autodealer.crm.modules.identity.application.api.dto.BatchDisableUsersRequest;
import com.autodealer.crm.modules.identity.application.api.dto.ChangePasswordRequest;
import com.autodealer.crm.modules.identity.application.api.dto.CreateUserRequest;
import com.autodealer.crm.modules.identity.application.api.dto.HandoverUserResponsibilitiesRequest;
import com.autodealer.crm.modules.identity.application.api.dto.HandoverUserResponsibilitiesResponse;
import com.autodealer.crm.modules.identity.application.api.dto.UpdateUserRequest;
import com.autodealer.crm.modules.identity.application.api.dto.UserDetailResponse;
import com.autodealer.crm.modules.identity.application.api.dto.UserListQuery;
import com.autodealer.crm.modules.identity.application.api.model.TUser;
import com.autodealer.crm.shared.error.BusinessException;
import com.autodealer.crm.shared.error.CodeEnum;
import com.autodealer.crm.shared.web.Result;
import com.autodealer.crm.modules.identity.application.api.UserService;
import com.autodealer.crm.modules.identity.application.api.ProfileService;
import com.autodealer.crm.modules.identity.application.api.dto.profile.ProfileDtos.LoginInfo;
import com.autodealer.crm.modules.identity.application.api.dto.profile.ProfileDtos.Profile;
import com.github.pagehelper.PageInfo;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;

import java.util.List;
import com.autodealer.crm.modules.identity.application.api.dto.OwnerCandidate;
import com.autodealer.crm.modules.identity.application.api.dto.user.ManagedUserDtos.CreateRequest;
import com.autodealer.crm.modules.identity.application.api.dto.user.ManagedUserDtos.CreateResult;
import com.autodealer.crm.modules.identity.application.api.ManagedUserInvitationService;
import com.autodealer.crm.modules.identity.application.api.ManagedUserAccountService;
import com.autodealer.crm.modules.identity.application.api.dto.user.ManagedUserDtos.StatusRequest;
import com.autodealer.crm.modules.identity.application.api.dto.user.ManagedUserDtos.ProfileRequest;
import com.autodealer.crm.modules.identity.application.api.dto.user.ManagedUserDtos.LoginAccountRequest;
import com.autodealer.crm.modules.identity.application.api.dto.user.ManagedUserDtos.SecurityExpirationRequest;
import com.autodealer.crm.modules.identity.application.api.dto.user.ManagedUserDtos.Detail;
import com.autodealer.crm.modules.identity.application.api.dto.user.ManagedUserDtos.FilterOptions;
import com.autodealer.crm.modules.identity.application.api.dto.user.ManagedUserDtos.Summary;

@RestController
public class UserController {

    private final UserService userService;
    private final ProfileService profileService;
    private final ManagedUserInvitationService invitationService;
    private final ManagedUserAccountService accountService;
    private final UserManagementAccessGate userManagementAccessGate;

    public UserController(UserService userService, ProfileService profileService,
                          ManagedUserInvitationService invitationService,
                          ManagedUserAccountService accountService,
                          UserManagementAccessGate userManagementAccessGate) {
        this.userService = userService;
        this.profileService = profileService;
        this.invitationService = invitationService;
        this.accountService = accountService;
        this.userManagementAccessGate = userManagementAccessGate;
    }

    @GetMapping(value = "/api/login/info")
    public Result<LoginInfo> loginInfo(Authentication authentication) {
        TUser tUser = (TUser) authentication.getPrincipal();
        TUser loginUser = userService.getLoginUserById(tUser.getId());
        Profile profile;try{profile=profileService.getOwn();}catch(BusinessException exception){profile=new Profile();profile.setName(loginUser.getName());profile.setPhone(loginUser.getPhone());profile.setEmail(loginUser.getEmail());}LoginInfo response=new LoginInfo();response.setId(loginUser.getId());response.setLoginAct(loginUser.getLoginAct());response.setName(profile.getName());response.setPhone(profile.getPhone());response.setEmail(profile.getEmail());response.setAvatarUrl(profile.getAvatarUrl());response.setMustChangePassword(loginUser.getMustChangePassword());response.setProtectedRecoveryAccount(userManagementAccessGate.isFixedRecoveryAccount(loginUser));response.setUserManagementGateState(userManagementAccessGate.state().name());response.setRoleList(loginUser.getRoleList());response.setPermissionList(loginUser.getPermissionList());response.setMenuPermissionList(loginUser.getMenuPermissionList());return Result.OK(response);
    }

    @GetMapping(value = "/api/login/free")
    public Result<Void> freeLogin() {
        return Result.OK();
    }

    @PreAuthorize(value = "hasAuthority('" + PermissionCodes.USER_LIST + "')")
    @GetMapping(value = "/api/users")
    public Result<PageInfo<Summary>> userPage(@RequestParam(value = "page", required = false) Integer page,
                                         @RequestParam(value = "size", required = false) Integer size,
                                         UserListQuery query) {
        query.setCurrent(page == null ? 1 : page);
        query.setPageSize(size == null ? 10 : size);
        PageInfo<Summary> pageInfo = accountService.list(query);
        return Result.OK(pageInfo);
    }

    @PreAuthorize(value = "hasAuthority('" + PermissionCodes.USER_LIST + "')")
    @GetMapping(value = "/api/users/filter-options")
    public Result<FilterOptions> userFilterOptions(@RequestParam(required = false) Integer organizationUnitId) {
        return Result.OK(accountService.getFilterOptions(organizationUnitId));
    }

    @GetMapping(value = "/api/user/{id}")
    @Deprecated
    public Result<Detail> userDetail(@PathVariable(value = "id") Integer id) {
        return Result.OK(accountService.getDetail(id));
    }

    @GetMapping(value = "/api/users/{id}")
    public Result<Detail> managedUserDetail(@PathVariable Integer id){return Result.OK(accountService.getDetail(id));}

    @PreAuthorize(value = "hasAuthority('" + PermissionCodes.USER_ADD + "')")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PostMapping(value = "/api/users")
    public Result<CreateResult> createManagedUser(@Valid @RequestBody CreateRequest request) {
        return Result.OK(invitationService.create(request));
    }

    @PreAuthorize(value = "hasAuthority('" + PermissionCodes.USER_ADD + "')")
    @PostMapping(value = "/api/user")
    public Result<UserDetailResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        throw new BusinessException(CodeEnum.ACCESS_DENIED,
                "旧用户创建入口已停用，请使用邀请激活流程");
    }

    @PreAuthorize(value = "hasAuthority('" + PermissionCodes.USER_EDIT + "')")
    @PutMapping(value = "/api/user")
    public Result<UserDetailResponse> updateUser(@Valid @RequestBody UpdateUserRequest request) {
        throw new BusinessException(CodeEnum.ACCESS_DENIED,
                "旧用户万能更新入口已停用，请使用受管资料命令");
    }

    @PreAuthorize(value = "hasAuthority('" + PermissionCodes.USER_STATUS + "')")
    @PostMapping(value = "/api/users/{id}/status")
    public Result<Detail> changeManagedStatus(@PathVariable Integer id,@Valid @RequestBody StatusRequest request){return Result.OK(accountService.changeStatus(id,request));}

    @PreAuthorize(value = "hasAuthority('" + PermissionCodes.USER_EDIT + "')")
    @PutMapping(value = "/api/users/{id}/profile")
    public Result<Detail> updateManagedProfile(@PathVariable Integer id,@Valid @RequestBody ProfileRequest request){return Result.OK(accountService.updateProfile(id,request));}

    @PreAuthorize(value = "hasAuthority('" + PermissionCodes.USER_EDIT + "')")
    @PutMapping(value = "/api/users/{id}/login-account")
    public Result<Detail> changeManagedLoginAccount(@PathVariable Integer id,@Valid @RequestBody LoginAccountRequest request){return Result.OK(accountService.changeLoginAccount(id,request));}

    @PreAuthorize(value = "hasAuthority('" + PermissionCodes.USER_STATUS + "')")
    @PutMapping(value = "/api/users/{id}/security-expiration")
    public Result<Detail> changeManagedSecurityExpiration(@PathVariable Integer id,@Valid @RequestBody SecurityExpirationRequest request){return Result.OK(accountService.changeSecurityExpiration(id,request));}

    @PreAuthorize(value = "hasAuthority('" + PermissionCodes.USER_STATUS + "')")
    @PutMapping(value = "/api/user/{id}/disable")
    public Result<Void> disableUser(@PathVariable(value = "id") Integer id) {
        throw legacyStatusEntryClosed();
    }

    @PreAuthorize(value = "hasAuthority('" + PermissionCodes.USER_STATUS + "')")
    @PutMapping(value = "/api/user/{id}/enable")
    public Result<Void> enableUser(@PathVariable(value = "id") Integer id) {
        throw legacyStatusEntryClosed();
    }

    @PreAuthorize(value = "hasAuthority('" + PermissionCodes.USER_STATUS + "')")
    @PutMapping(value = "/api/user/{id}/lock")
    public Result<Void> lockUser(@PathVariable(value = "id") Integer id) {
        throw legacyStatusEntryClosed();
    }

    @PreAuthorize(value = "hasAuthority('" + PermissionCodes.USER_STATUS + "')")
    @PutMapping(value = "/api/user/{id}/unlock")
    public Result<Void> unlockUser(@PathVariable(value = "id") Integer id) {
        throw legacyStatusEntryClosed();
    }

    @PreAuthorize(value = "hasAuthority('" + PermissionCodes.USER_STATUS + "')")
    @PutMapping(value = "/api/users/batch-disable")
    public Result<Void> batchDisableUsers(@Valid @RequestBody BatchDisableUsersRequest request) {
        throw legacyStatusEntryClosed();
    }

    @PreAuthorize(value = "hasAuthority('" + PermissionCodes.USER_ROLE + "')")
    @PutMapping(value = "/api/user/{id}/roles")
    public Result<Void> assignRoles(@PathVariable(value = "id") Integer id,
                               @RequestBody(required = false) AssignUserRolesRequest request) {
        throw legacyStatusEntryClosed();
    }

    @PreAuthorize(value = "hasAuthority('" + PermissionCodes.USER_PASSWORD + "')")
    @PutMapping(value = "/api/user/{id}/password")
    public Result<Void> changePassword(@PathVariable(value = "id") Integer id,
                                  @Valid @RequestBody ChangePasswordRequest request) {
        throw new BusinessException(CodeEnum.ACCESS_DENIED,
                "旧密码重置入口已停用，请使用本人改密或管理员重置凭证流程");
    }

    @PreAuthorize(value = "hasAuthority('" + PermissionCodes.USER_STATUS + "')")
    @PutMapping(value = "/api/user/{id}/handover")
    public Result<HandoverUserResponsibilitiesResponse> handoverResponsibilities(
            @PathVariable(value = "id") Integer id,
            @Valid @RequestBody HandoverUserResponsibilitiesRequest request) {
        throw new BusinessException(CodeEnum.ACCESS_DENIED,
                "旧责任交接入口已停用，请使用离职交接闭环命令");
    }

    @GetMapping(value = "/api/owner")
    public Result<List<OwnerCandidate>> owner(@RequestParam String permissionCode,
                                         @RequestParam String qualificationContext) {
        List<OwnerCandidate> ownerList = userService.getOwnerCandidates(permissionCode, qualificationContext);
        return Result.OK(ownerList);
    }

    private BusinessException legacyStatusEntryClosed() {
        return new BusinessException(CodeEnum.ACCESS_DENIED,
                "旧账号状态入口已停用，请使用带版本、原因和管理范围校验的状态命令");
    }
}
