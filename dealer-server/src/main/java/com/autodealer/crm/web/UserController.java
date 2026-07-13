package com.autodealer.crm.web;

import com.autodealer.crm.constant.PermissionCodes;
import com.autodealer.crm.config.security.UserManagementAccessGate;

import com.autodealer.crm.dto.AssignUserRolesRequest;
import com.autodealer.crm.dto.BatchDisableUsersRequest;
import com.autodealer.crm.dto.ChangePasswordRequest;
import com.autodealer.crm.dto.CreateUserRequest;
import com.autodealer.crm.dto.HandoverUserResponsibilitiesRequest;
import com.autodealer.crm.dto.HandoverUserResponsibilitiesResponse;
import com.autodealer.crm.dto.UpdateUserRequest;
import com.autodealer.crm.dto.UserDetailResponse;
import com.autodealer.crm.dto.UserListQuery;
import com.autodealer.crm.model.TUser;
import com.autodealer.crm.exception.BusinessException;
import com.autodealer.crm.result.CodeEnum;
import com.autodealer.crm.result.R;
import com.autodealer.crm.service.UserService;
import com.autodealer.crm.service.ProfileService;
import com.autodealer.crm.dto.profile.ProfileDtos.LoginInfo;
import com.autodealer.crm.dto.profile.ProfileDtos.Profile;
import com.github.pagehelper.PageInfo;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;

import java.util.List;
import com.autodealer.crm.dto.OwnerCandidate;
import com.autodealer.crm.dto.user.ManagedUserDtos.CreateRequest;
import com.autodealer.crm.dto.user.ManagedUserDtos.CreateResult;
import com.autodealer.crm.service.ManagedUserInvitationService;
import com.autodealer.crm.service.ManagedUserAccountService;
import com.autodealer.crm.dto.user.ManagedUserDtos.StatusRequest;
import com.autodealer.crm.dto.user.ManagedUserDtos.ProfileRequest;
import com.autodealer.crm.dto.user.ManagedUserDtos.LoginAccountRequest;
import com.autodealer.crm.dto.user.ManagedUserDtos.SecurityExpirationRequest;
import com.autodealer.crm.dto.user.ManagedUserDtos.Detail;
import com.autodealer.crm.dto.user.ManagedUserDtos.FilterOptions;
import com.autodealer.crm.dto.user.ManagedUserDtos.Summary;

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
    public R<LoginInfo> loginInfo(Authentication authentication) {
        TUser tUser = (TUser) authentication.getPrincipal();
        TUser loginUser = userService.getLoginUserById(tUser.getId());
        Profile profile;try{profile=profileService.getOwn();}catch(BusinessException exception){profile=new Profile();profile.setName(loginUser.getName());profile.setPhone(loginUser.getPhone());profile.setEmail(loginUser.getEmail());}LoginInfo response=new LoginInfo();response.setId(loginUser.getId());response.setLoginAct(loginUser.getLoginAct());response.setName(profile.getName());response.setPhone(profile.getPhone());response.setEmail(profile.getEmail());response.setAvatarUrl(profile.getAvatarUrl());response.setMustChangePassword(loginUser.getMustChangePassword());response.setProtectedRecoveryAccount(userManagementAccessGate.isFixedRecoveryAccount(loginUser));response.setUserManagementGateState(userManagementAccessGate.state().name());response.setRoleList(loginUser.getRoleList());response.setPermissionList(loginUser.getPermissionList());response.setMenuPermissionList(loginUser.getMenuPermissionList());return R.OK(response);
    }

    @GetMapping(value = "/api/login/free")
    public R<Void> freeLogin() {
        return R.OK();
    }

    @PreAuthorize(value = "hasAuthority('" + PermissionCodes.USER_LIST + "')")
    @GetMapping(value = "/api/users")
    public R<PageInfo<Summary>> userPage(@RequestParam(value = "page", required = false) Integer page,
                                         @RequestParam(value = "size", required = false) Integer size,
                                         UserListQuery query) {
        query.setCurrent(page == null ? 1 : page);
        query.setPageSize(size == null ? 10 : size);
        PageInfo<Summary> pageInfo = accountService.list(query);
        return R.OK(pageInfo);
    }

    @PreAuthorize(value = "hasAuthority('" + PermissionCodes.USER_LIST + "')")
    @GetMapping(value = "/api/users/filter-options")
    public R<FilterOptions> userFilterOptions(@RequestParam(required = false) Integer organizationUnitId) {
        return R.OK(accountService.getFilterOptions(organizationUnitId));
    }

    @GetMapping(value = "/api/user/{id}")
    @Deprecated
    public R<Detail> userDetail(@PathVariable(value = "id") Integer id) {
        return R.OK(accountService.getDetail(id));
    }

    @GetMapping(value = "/api/users/{id}")
    public R<Detail> managedUserDetail(@PathVariable Integer id){return R.OK(accountService.getDetail(id));}

    @PreAuthorize(value = "hasAuthority('" + PermissionCodes.USER_ADD + "')")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PostMapping(value = "/api/users")
    public R<CreateResult> createManagedUser(@Valid @RequestBody CreateRequest request) {
        return R.OK(invitationService.create(request));
    }

    @PreAuthorize(value = "hasAuthority('" + PermissionCodes.USER_ADD + "')")
    @PostMapping(value = "/api/user")
    public R<UserDetailResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        throw new BusinessException(CodeEnum.ACCESS_DENIED,
                "旧用户创建入口已停用，请使用邀请激活流程");
    }

    @PreAuthorize(value = "hasAuthority('" + PermissionCodes.USER_EDIT + "')")
    @PutMapping(value = "/api/user")
    public R<UserDetailResponse> updateUser(@Valid @RequestBody UpdateUserRequest request) {
        throw new BusinessException(CodeEnum.ACCESS_DENIED,
                "旧用户万能更新入口已停用，请使用受管资料命令");
    }

    @PreAuthorize(value = "hasAuthority('" + PermissionCodes.USER_STATUS + "')")
    @PostMapping(value = "/api/users/{id}/status")
    public R<Detail> changeManagedStatus(@PathVariable Integer id,@Valid @RequestBody StatusRequest request){return R.OK(accountService.changeStatus(id,request));}

    @PreAuthorize(value = "hasAuthority('" + PermissionCodes.USER_EDIT + "')")
    @PutMapping(value = "/api/users/{id}/profile")
    public R<Detail> updateManagedProfile(@PathVariable Integer id,@Valid @RequestBody ProfileRequest request){return R.OK(accountService.updateProfile(id,request));}

    @PreAuthorize(value = "hasAuthority('" + PermissionCodes.USER_EDIT + "')")
    @PutMapping(value = "/api/users/{id}/login-account")
    public R<Detail> changeManagedLoginAccount(@PathVariable Integer id,@Valid @RequestBody LoginAccountRequest request){return R.OK(accountService.changeLoginAccount(id,request));}

    @PreAuthorize(value = "hasAuthority('" + PermissionCodes.USER_STATUS + "')")
    @PutMapping(value = "/api/users/{id}/security-expiration")
    public R<Detail> changeManagedSecurityExpiration(@PathVariable Integer id,@Valid @RequestBody SecurityExpirationRequest request){return R.OK(accountService.changeSecurityExpiration(id,request));}

    @PreAuthorize(value = "hasAuthority('" + PermissionCodes.USER_STATUS + "')")
    @PutMapping(value = "/api/user/{id}/disable")
    public R<Void> disableUser(@PathVariable(value = "id") Integer id) {
        throw legacyStatusEntryClosed();
    }

    @PreAuthorize(value = "hasAuthority('" + PermissionCodes.USER_STATUS + "')")
    @PutMapping(value = "/api/user/{id}/enable")
    public R<Void> enableUser(@PathVariable(value = "id") Integer id) {
        throw legacyStatusEntryClosed();
    }

    @PreAuthorize(value = "hasAuthority('" + PermissionCodes.USER_STATUS + "')")
    @PutMapping(value = "/api/user/{id}/lock")
    public R<Void> lockUser(@PathVariable(value = "id") Integer id) {
        throw legacyStatusEntryClosed();
    }

    @PreAuthorize(value = "hasAuthority('" + PermissionCodes.USER_STATUS + "')")
    @PutMapping(value = "/api/user/{id}/unlock")
    public R<Void> unlockUser(@PathVariable(value = "id") Integer id) {
        throw legacyStatusEntryClosed();
    }

    @PreAuthorize(value = "hasAuthority('" + PermissionCodes.USER_STATUS + "')")
    @PutMapping(value = "/api/users/batch-disable")
    public R<Void> batchDisableUsers(@Valid @RequestBody BatchDisableUsersRequest request) {
        throw legacyStatusEntryClosed();
    }

    @PreAuthorize(value = "hasAuthority('" + PermissionCodes.USER_ROLE + "')")
    @PutMapping(value = "/api/user/{id}/roles")
    public R<Void> assignRoles(@PathVariable(value = "id") Integer id,
                               @RequestBody(required = false) AssignUserRolesRequest request) {
        throw legacyStatusEntryClosed();
    }

    @PreAuthorize(value = "hasAuthority('" + PermissionCodes.USER_PASSWORD + "')")
    @PutMapping(value = "/api/user/{id}/password")
    public R<Void> changePassword(@PathVariable(value = "id") Integer id,
                                  @Valid @RequestBody ChangePasswordRequest request) {
        throw new BusinessException(CodeEnum.ACCESS_DENIED,
                "旧密码重置入口已停用，请使用本人改密或管理员重置凭证流程");
    }

    @PreAuthorize(value = "hasAuthority('" + PermissionCodes.USER_STATUS + "')")
    @PutMapping(value = "/api/user/{id}/handover")
    public R<HandoverUserResponsibilitiesResponse> handoverResponsibilities(
            @PathVariable(value = "id") Integer id,
            @Valid @RequestBody HandoverUserResponsibilitiesRequest request) {
        throw new BusinessException(CodeEnum.ACCESS_DENIED,
                "旧责任交接入口已停用，请使用离职交接闭环命令");
    }

    @GetMapping(value = "/api/owner")
    public R<List<OwnerCandidate>> owner(@RequestParam String permissionCode,
                                         @RequestParam String qualificationContext) {
        List<OwnerCandidate> ownerList = userService.getOwnerCandidates(permissionCode, qualificationContext);
        return R.OK(ownerList);
    }

    private BusinessException legacyStatusEntryClosed() {
        return new BusinessException(CodeEnum.ACCESS_DENIED,
                "旧账号状态入口已停用，请使用带版本、原因和管理范围校验的状态命令");
    }
}
