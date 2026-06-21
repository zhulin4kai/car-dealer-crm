package com.autodealer.crm.web;

import com.autodealer.crm.constant.PermissionCodes;

import com.autodealer.crm.dto.AssignUserRolesRequest;
import com.autodealer.crm.dto.BatchDisableUsersRequest;
import com.autodealer.crm.dto.ChangePasswordRequest;
import com.autodealer.crm.dto.CreateUserRequest;
import com.autodealer.crm.dto.UpdateUserRequest;
import com.autodealer.crm.dto.UserDetailResponse;
import com.autodealer.crm.dto.UserListQuery;
import com.autodealer.crm.model.TUser;
import com.autodealer.crm.result.R;
import com.autodealer.crm.service.UserService;
import com.github.pagehelper.PageInfo;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping(value = "/api/login/info")
    public R<TUser> loginInfo(Authentication authentication) {
        TUser tUser = (TUser) authentication.getPrincipal();
        TUser loginUser = userService.getLoginUserById(tUser.getId());
        return R.OK(loginUser);
    }

    @GetMapping(value = "/api/login/free")
    public R<Void> freeLogin() {
        return R.OK();
    }

    @PreAuthorize(value = "hasAuthority('" + PermissionCodes.USER_LIST + "')")
    @GetMapping(value = "/api/users")
    public R<PageInfo<UserDetailResponse>> userPage(UserListQuery query) {
        if (query.getCurrent() == null) {
            query.setCurrent(1);
        }
        PageInfo<UserDetailResponse> pageInfo = userService.getUserByPage(query);
        return R.OK(pageInfo);
    }

    @PreAuthorize(value = "hasAuthority('" + PermissionCodes.USER_VIEW + "')")
    @GetMapping(value = "/api/user/{id}")
    public R<UserDetailResponse> userDetail(@PathVariable(value = "id") Integer id) {
        UserDetailResponse response = userService.getUserById(id);
        return R.OK(response);
    }

    @PreAuthorize(value = "hasAuthority('" + PermissionCodes.USER_ADD + "')")
    @PostMapping(value = "/api/user")
    public R<UserDetailResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        UserDetailResponse response = userService.createUser(request);
        return R.OK(response);
    }

    @PreAuthorize(value = "hasAuthority('" + PermissionCodes.USER_EDIT + "')")
    @PutMapping(value = "/api/user")
    public R<UserDetailResponse> updateUser(@Valid @RequestBody UpdateUserRequest request) {
        UserDetailResponse response = userService.updateUser(request);
        return R.OK(response);
    }

    @PreAuthorize(value = "hasAuthority('" + PermissionCodes.USER_STATUS + "')")
    @PutMapping(value = "/api/user/{id}/disable")
    public R<Void> disableUser(@PathVariable(value = "id") Integer id) {
        userService.disableUser(id);
        return R.OK();
    }

    @PreAuthorize(value = "hasAuthority('" + PermissionCodes.USER_STATUS + "')")
    @PutMapping(value = "/api/user/{id}/enable")
    public R<Void> enableUser(@PathVariable(value = "id") Integer id) {
        userService.enableUser(id);
        return R.OK();
    }

    @PreAuthorize(value = "hasAuthority('" + PermissionCodes.USER_STATUS + "')")
    @PutMapping(value = "/api/user/{id}/lock")
    public R<Void> lockUser(@PathVariable(value = "id") Integer id) {
        userService.lockUser(id);
        return R.OK();
    }

    @PreAuthorize(value = "hasAuthority('" + PermissionCodes.USER_STATUS + "')")
    @PutMapping(value = "/api/user/{id}/unlock")
    public R<Void> unlockUser(@PathVariable(value = "id") Integer id) {
        userService.unlockUser(id);
        return R.OK();
    }

    @PreAuthorize(value = "hasAuthority('" + PermissionCodes.USER_STATUS + "')")
    @PutMapping(value = "/api/users/batch-disable")
    public R<Void> batchDisableUsers(@Valid @RequestBody BatchDisableUsersRequest request) {
        userService.batchDisableUsers(request.getIds());
        return R.OK();
    }

    @PreAuthorize(value = "hasAuthority('" + PermissionCodes.USER_ROLE + "')")
    @PutMapping(value = "/api/user/{id}/roles")
    public R<Void> assignRoles(@PathVariable(value = "id") Integer id,
                               @Valid @RequestBody AssignUserRolesRequest request) {
        request.setUserId(id);
        userService.assignRoles(request);
        return R.OK();
    }

    @PreAuthorize(value = "hasAuthority('" + PermissionCodes.USER_PASSWORD + "')")
    @PutMapping(value = "/api/user/{id}/password")
    public R<Void> changePassword(@PathVariable(value = "id") Integer id,
                                  @Valid @RequestBody ChangePasswordRequest request) {
        request.setUserId(id);
        userService.changePassword(request);
        return R.OK();
    }

    @GetMapping(value = "/api/owner")
    public R<List<TUser>> owner() {
        List<TUser> ownerList = userService.getOwnerList();
        return R.OK(ownerList);
    }
}
