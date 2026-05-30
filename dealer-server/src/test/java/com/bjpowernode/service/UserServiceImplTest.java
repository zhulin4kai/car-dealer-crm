package com.bjpowernode.service;

import com.bjpowernode.manager.RedisManager;
import com.bjpowernode.mapper.TPermissionMapper;
import com.bjpowernode.mapper.TRoleMapper;
import com.bjpowernode.mapper.TUserMapper;
import com.bjpowernode.model.TPermission;
import com.bjpowernode.model.TRole;
import com.bjpowernode.model.TUser;
import com.bjpowernode.query.UserQuery;
import com.bjpowernode.service.impl.UserServiceImpl;
import com.bjpowernode.util.JWTUtils;
import com.github.pagehelper.PageInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @InjectMocks
    private UserServiceImpl userService;

    @Mock
    private TUserMapper tUserMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TRoleMapper tRoleMapper;

    @Mock
    private RedisManager redisManager;

    @Mock
    private TPermissionMapper tPermissionMapper;

    @Test
    void testGetUserByPage() {
        TUser user = new TUser();
        user.setId(1);
        user.setLoginAct("admin");
        user.setName("Admin");
        List<TUser> list = Collections.singletonList(user);

        when(tUserMapper.selectUserByPage(any())).thenReturn(list);

        PageInfo<TUser> result = userService.getUserByPage(1);

        assertNotNull(result);
        assertEquals(1, result.getList().size());
        assertEquals("admin", result.getList().get(0).getLoginAct());
        verify(tUserMapper).selectUserByPage(any());
    }

    @Test
    void testGetUserByPageEmpty() {
        when(tUserMapper.selectUserByPage(any())).thenReturn(Collections.emptyList());

        PageInfo<TUser> result = userService.getUserByPage(1);

        assertNotNull(result);
        assertTrue(result.getList().isEmpty());
    }

    @Test
    void testLoadUserByUsernameFound() {
        TUser user = new TUser();
        user.setId(1);
        user.setLoginAct("admin");
        user.setLoginPwd("encodedPwd");
        user.setAccountNoExpired(1);
        user.setCredentialsNoExpired(1);
        user.setAccountNoLocked(1);
        user.setAccountEnabled(1);

        TRole role = new TRole();
        role.setRole("ROLE_ADMIN");

        TPermission menuPerm = new TPermission();
        menuPerm.setName("Dashboard");
        menuPerm.setCode("dashboard");
        menuPerm.setType("menu");

        TPermission buttonPerm = new TPermission();
        buttonPerm.setName("Add User");
        buttonPerm.setCode("user:add");
        buttonPerm.setType("button");

        when(tUserMapper.selectByLoginAct("admin")).thenReturn(user);
        when(tRoleMapper.selectByUserId(1)).thenReturn(Collections.singletonList(role));
        when(tPermissionMapper.selectMenuPermissionByUserId(1)).thenReturn(Collections.singletonList(menuPerm));
        when(tPermissionMapper.selectButtonPermissionByUserId(1)).thenReturn(Collections.singletonList(buttonPerm));

        UserDetails result = userService.loadUserByUsername("admin");

        assertNotNull(result);
        assertEquals("admin", result.getUsername());
        assertTrue(result.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
        assertTrue(result.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("user:add")));
    }

    @Test
    void testLoadUserByUsernameNotFound() {
        when(tUserMapper.selectByLoginAct("nonexistent")).thenReturn(null);

        assertThrows(UsernameNotFoundException.class,
                () -> userService.loadUserByUsername("nonexistent"));
        verify(tUserMapper).selectByLoginAct("nonexistent");
    }

    @Test
    void testGetUserById() {
        TUser user = new TUser();
        user.setId(1);
        user.setName("Admin");

        when(tUserMapper.selectDetailById(1)).thenReturn(user);

        TUser result = userService.getUserById(1);

        assertNotNull(result);
        assertEquals(1, result.getId());
        assertEquals("Admin", result.getName());
    }

    @Test
    void testGetUserByIdNotFound() {
        when(tUserMapper.selectDetailById(999)).thenReturn(null);

        TUser result = userService.getUserById(999);

        assertNull(result);
    }

    @Test
    void testSaveUser() {
        try (MockedStatic<JWTUtils> jwtUtils = mockStatic(JWTUtils.class)) {
            UserQuery query = new UserQuery();
            query.setLoginAct("newuser");
            query.setLoginPwd("rawPassword");
            query.setName("New User");
            query.setToken("test-token");

            TUser loginUser = new TUser();
            loginUser.setId(10);
            jwtUtils.when(() -> JWTUtils.parseUserFromJWT("test-token")).thenReturn(loginUser);

            when(passwordEncoder.encode("rawPassword")).thenReturn("encodedPassword");
            when(tUserMapper.insertSelective(any(TUser.class))).thenReturn(1);

            int result = userService.saveUser(query);

            assertEquals(1, result);
            verify(passwordEncoder).encode("rawPassword");
            verify(tUserMapper).insertSelective(argThat(user ->
                    "newuser".equals(user.getLoginAct())
                            && "encodedPassword".equals(user.getLoginPwd())
                            && user.getCreateTime() != null
                            && user.getCreateBy() != null
            ));
        }
    }

    @Test
    void testUpdateUser() {
        try (MockedStatic<JWTUtils> jwtUtils = mockStatic(JWTUtils.class)) {
            UserQuery query = new UserQuery();
            query.setId(1);
            query.setName("Updated User");
            query.setLoginPwd("newPassword");
            query.setToken("test-token");

            TUser loginUser = new TUser();
            loginUser.setId(10);
            jwtUtils.when(() -> JWTUtils.parseUserFromJWT("test-token")).thenReturn(loginUser);

            when(passwordEncoder.encode("newPassword")).thenReturn("encodedNewPwd");
            when(tUserMapper.updateByPrimaryKeySelective(any(TUser.class))).thenReturn(1);

            int result = userService.updateUser(query);

            assertEquals(1, result);
            verify(passwordEncoder).encode("newPassword");
            verify(tUserMapper).updateByPrimaryKeySelective(argThat(user ->
                    "Updated User".equals(user.getName())
                            && "encodedNewPwd".equals(user.getLoginPwd())
                            && user.getEditTime() != null
                            && user.getEditBy() != null
            ));
        }
    }

    @Test
    void testUpdateUserWithoutPassword() {
        try (MockedStatic<JWTUtils> jwtUtils = mockStatic(JWTUtils.class)) {
            UserQuery query = new UserQuery();
            query.setId(1);
            query.setName("Updated User");
            query.setToken("test-token");

            TUser loginUser = new TUser();
            loginUser.setId(10);
            jwtUtils.when(() -> JWTUtils.parseUserFromJWT("test-token")).thenReturn(loginUser);

            when(tUserMapper.updateByPrimaryKeySelective(any(TUser.class))).thenReturn(1);

            int result = userService.updateUser(query);

            assertEquals(1, result);
            verify(passwordEncoder, never()).encode(anyString());
            verify(tUserMapper).updateByPrimaryKeySelective(argThat(user ->
                    "Updated User".equals(user.getName())
                            && user.getLoginPwd() == null
            ));
        }
    }

    @Test
    void testDeleteUser() {
        when(tUserMapper.deleteByPrimaryKey(1)).thenReturn(1);

        int result = userService.delUserById(1);

        assertEquals(1, result);
        verify(tUserMapper).deleteByPrimaryKey(1);
    }

    @Test
    void testBatchDeleteUsers() {
        List<Integer> ids = Arrays.asList(1, 2, 3);
        when(tUserMapper.deleteByIds(ids)).thenReturn(3);

        int result = userService.batchDelUserIds(ids);

        assertEquals(3, result);
        verify(tUserMapper).deleteByIds(ids);
    }

    @Test
    void testGetOwnerListFromCache() {
        TUser owner = new TUser();
        owner.setId(1);
        owner.setName("Owner");
        List<TUser> cachedList = Collections.singletonList(owner);

        when(redisManager.getValue(anyString())).thenReturn(cachedList);

        List<TUser> result = userService.getOwnerList();

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(redisManager).getValue(anyString());
    }

    @Test
    void testGetOwnerListFromDatabase() {
        TUser owner = new TUser();
        owner.setId(1);
        owner.setName("Owner");
        List<TUser> dbList = Collections.singletonList(owner);

        when(redisManager.getValue(anyString())).thenReturn(null);
        when(tUserMapper.selectByOwner()).thenReturn(dbList);

        List<TUser> result = userService.getOwnerList();

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(tUserMapper).selectByOwner();
        verify(redisManager).setValue(anyString(), anyList());
    }
}
