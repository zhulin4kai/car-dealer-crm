package com.bjpowernode.web;

import com.bjpowernode.model.TUser;
import com.bjpowernode.query.UserQuery;
import com.bjpowernode.result.R;
import com.bjpowernode.service.UserService;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.parameters.P;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@RestController
public class UserController {

    @Resource
    private UserService userService;

    /**
     * 获取登录人信息
     *
     * @param authentication
     * @return
     */
    @GetMapping(value = "/api/login/info")
    public R loginInfo(Authentication authentication) {
        TUser tUser = (TUser)authentication.getPrincipal();
        return R.OK(tUser);
    }

    /**
     * 免登录
     *
     * @return
     */
    @GetMapping(value = "/api/login/free")
    public R freeLogin() {
        return R.OK();
    }

    /**
     * 用户列表分页查询
     *
     * @param current
     * @return
     */
    @PreAuthorize(value = "hasAuthority('user:list')")
    @GetMapping(value = "/api/users")
    public R userPage(@RequestParam(value = "current", required = false) Integer current) {
        //required = false 表示参数可以传，也可以不传；
        //required = true 表示参数必须要传，不传会报错；
        if (current == null) {
            current = 1;
        }
        PageInfo<TUser> pageInfo = userService.getUserByPage(current);
        return R.OK(pageInfo);
    }

    @PreAuthorize(value = "hasAuthority('user:view')")
    @GetMapping(value = "/api/user/{id}")
    public R userDetail(@PathVariable(value = "id") Integer id) {
        TUser tUser = userService.getUserById(id);
        return R.OK(tUser);
    }

    /**
     * 新增用户
     *
     * @param userQuery
     * @return
     */
    @PreAuthorize(value = "hasAuthority('user:add')")
    @PostMapping(value = "/api/user")
    public R addUser(@RequestBody UserQuery userQuery, @RequestHeader(value = "Authorization") String token) {
        userQuery.setToken(token);
        int save = userService.saveUser(userQuery);
        return save >= 1 ? R.OK() : R.FAIL();
    }

    /**
     * 编辑用户
     *
     * @param userQuery
     * @return
     */
    @PreAuthorize(value = "hasAuthority('user:edit')")
    @PutMapping(value = "/api/user")
    public R editUser(@RequestBody UserQuery userQuery, @RequestHeader(value = "Authorization") String token) {
        userQuery.setToken(token);
        int update = userService.updateUser(userQuery);
        return update >= 1 ? R.OK() : R.FAIL();
    }

    @PreAuthorize(value = "hasAuthority('user:delete')")
    @DeleteMapping(value = "/api/user/{id}")
    public R delUser(@PathVariable(value = "id") Integer id) {
        int del = userService.delUserById(id);
        return del >= 1 ? R.OK() : R.FAIL();
    }

    @PreAuthorize(value = "hasAuthority('user:delete')")
    @DeleteMapping(value = "/api/user")
    public R batchDelUser(@RequestBody List<Integer> ids) {
        int batchDel = userService.batchDelUserIds(ids);
        return batchDel >= 0 ? R.OK() : R.FAIL();
    }

    @GetMapping(value = "/api/owner")
    public R owner() {
        List<TUser> ownerList = userService.getOwnerList();
        return R.OK(ownerList);
    }
}
