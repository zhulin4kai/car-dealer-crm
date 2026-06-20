package com.autodealer.crm.service.impl;

import com.autodealer.crm.config.security.CurrentUserProvider;
import com.autodealer.crm.constant.Constants;
import com.autodealer.crm.manager.RedisManager;
import com.autodealer.crm.mapper.TPermissionMapper;
import com.autodealer.crm.mapper.TRoleMapper;
import com.autodealer.crm.mapper.TUserMapper;
import com.autodealer.crm.model.TPermission;
import com.autodealer.crm.model.TRole;
import com.autodealer.crm.model.TUser;
import com.autodealer.crm.query.BaseQuery;
import com.autodealer.crm.query.UserQuery;
import com.autodealer.crm.service.UserService;
import com.autodealer.crm.util.CacheUtils;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    @Resource
    private TUserMapper tUserMapper;

    @Resource
    private PasswordEncoder passwordEncoder;

    @Resource
    private TRoleMapper tRoleMapper;

    @Resource
    private RedisManager redisManager;

    @Resource
    private TPermissionMapper tPermissionMapper;

    @Resource
    private CurrentUserProvider currentUserProvider;

    /**
     * 登录查询
     *
     * @param username
     * @return
     * @throws UsernameNotFoundException
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        TUser tUser = tUserMapper.selectByLoginAct(username);
        if (tUser == null) {
            throw new UsernameNotFoundException("登录账号不存在");
        }

        loadLoginPermissions(tUser);

        return tUser;
    }

    @Override
    public TUser getLoginUserById(Integer id) {
        TUser tUser = tUserMapper.selectByPrimaryKey(id);
        if (tUser == null) {
            return null;
        }

        loadLoginPermissions(tUser);
        return tUser;
    }

    private void loadLoginPermissions(TUser tUser) {
        //查询一下当前用户的角色
        List<TRole> tRoleList = tRoleMapper.selectByUserId(tUser.getId());
        //字符串的角色列表
        List<String> stringRoleList = new ArrayList<>();
        tRoleList.forEach(tRole -> {
            stringRoleList.add(tRole.getRole());
        });
        tUser.setRoleList(stringRoleList); //设置用户的角色

        //查询一下该用户有哪些菜单权限
        List<TPermission> menuPermissionList = tPermissionMapper.selectMenuPermissionByUserId(tUser.getId());
        tUser.setMenuPermissionList(menuPermissionList);

        //查询一下该用户有哪些功能权限
        List<TPermission> buttonPermissionList = tPermissionMapper.selectButtonPermissionByUserId(tUser.getId());
        List<String> stringPermissionList = new ArrayList<>();
        buttonPermissionList.forEach(tPermission -> {
            stringPermissionList.add(tPermission.getCode());//权限标识符
        });
        tUser.setPermissionList(stringPermissionList);//设置用户的权限标识符
    }

    @Override
    public PageInfo<TUser> getUserByPage(Integer current) {
        // 1.设置PageHelper
        PageHelper.startPage(current, Constants.PAGE_SIZE);
        // 2.查询
        List<TUser> list = tUserMapper.selectUserByPage(BaseQuery.builder().build());
        // 3.封装分页数据到PageInfo
        PageInfo<TUser> info = new PageInfo<>(list);
        return info;
    }

    @Override
    public TUser getUserById(Integer id) {
        requireUserAccess(id);
        return tUserMapper.selectAuthUserById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public int saveUser(UserQuery userQuery) {

        TUser tUser = new TUser();

        //把UserQuery对象里面的属性数据复制到TUser对象里面去(复制要求：两个对象的属性名相同，属性类型要相同，这样才能复制)
        BeanUtils.copyProperties(userQuery, tUser);

        tUser.setLoginPwd(passwordEncoder.encode(userQuery.getLoginPwd())); //密码加密
        tUser.setCreateTime(new Date()); //创建时间

        tUser.setCreateBy(currentUserProvider.getCurrentUserId()); //创建人

        return tUserMapper.insertSelective(tUser);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public int updateUser(UserQuery userQuery) {
        requireUserAccess(userQuery.getId());
        TUser tUser = new TUser();

        //把UserQuery对象里面的属性数据复制到TUser对象里面去(复制要求：两个对象的属性名相同，属性类型要相同，这样才能复制)
        BeanUtils.copyProperties(userQuery, tUser);

        if (StringUtils.hasText(userQuery.getLoginPwd())) {
            tUser.setLoginPwd(passwordEncoder.encode(userQuery.getLoginPwd())); //密码加密
        }

        tUser.setEditTime(new Date()); //编辑时间

        tUser.setEditBy(currentUserProvider.getCurrentUserId()); //编辑人

        return tUserMapper.updateByPrimaryKeySelective(tUser);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public int delUserById(Integer id) {
        requireUserAccess(id);
        return tUserMapper.deleteByPrimaryKey(id);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public int batchDelUserIds(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        List<Integer> distinctIds = ids.stream().distinct().sorted().toList();
        distinctIds.forEach(this::requireUserAccess);
        return tUserMapper.deleteByIds(distinctIds);
    }

    @Override
    public List<TUser> getOwnerList() {
        //1、从redis查询
        //2、redis查不到，就从数据库查询，并且把数据放入redis（1小时过期）
        return CacheUtils.getCacheData(() -> {
            //生产，从缓存redis查询数据
            return (List<TUser>)redisManager.getList(Constants.REDIS_OWNER_KEY);
        },
        () -> {
            //生产，从mysql查询数据
            return (List<TUser>)tUserMapper.selectByOwner();
        },
        (t) -> {
            //消费，把数据放入缓存redis
            redisManager.setList(Constants.REDIS_OWNER_KEY, t);
        }
       );
    }

    private void requireUserAccess(Integer targetUserId) {
        Integer scopeUserId = currentUserProvider.getDataScopeUserId();
        if (scopeUserId != null && !scopeUserId.equals(targetUserId)) {
            throw new RuntimeException("用户不存在或无权访问");
        }
    }
}
