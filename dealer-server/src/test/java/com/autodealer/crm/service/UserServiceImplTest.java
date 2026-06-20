package com.autodealer.crm.service;

import com.autodealer.crm.config.security.CurrentUserProvider;
import com.autodealer.crm.manager.RedisManager;
import com.autodealer.crm.mapper.TPermissionMapper;
import com.autodealer.crm.mapper.TRoleMapper;
import com.autodealer.crm.mapper.TUserMapper;
import com.autodealer.crm.model.TPermission;
import com.autodealer.crm.model.TRole;
import com.autodealer.crm.model.TUser;
import com.autodealer.crm.query.UserQuery;
import com.autodealer.crm.service.impl.UserServiceImpl;
import com.github.pagehelper.PageInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
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

    @Mock
    private CurrentUserProvider currentUserProvider;

    @BeforeEach
    void setUp() {
        lenient().when(currentUserProvider.getDataScopeUserId()).thenReturn(null);
    }

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

        when(tUserMapper.selectAuthUserById(1)).thenReturn(user);

        TUser result = userService.getUserById(1);

        assertNotNull(result);
        assertEquals(1, result.getId());
        assertEquals("Admin", result.getName());
    }

    @Test
    void testGetUserByIdNotFound() {
        when(tUserMapper.selectAuthUserById(999)).thenReturn(null);

        TUser result = userService.getUserById(999);

        assertNull(result);
    }

    @Test
    void testSaveUser() {
            UserQuery query = new UserQuery();
            query.setLoginAct("newuser");
            query.setLoginPwd("rawPassword");
            query.setName("New User");

            when(currentUserProvider.getCurrentUserId()).thenReturn(10);
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

    @Test
    void testUpdateUser() {
            UserQuery query = new UserQuery();
            query.setId(1);
            query.setName("Updated User");
            query.setLoginPwd("newPassword");

            when(currentUserProvider.getCurrentUserId()).thenReturn(10);
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

    @Test
    void testUpdateUserWithoutPassword() {
            UserQuery query = new UserQuery();
            query.setId(1);
            query.setName("Updated User");

            when(currentUserProvider.getCurrentUserId()).thenReturn(10);
            when(tUserMapper.updateByPrimaryKeySelective(any(TUser.class))).thenReturn(1);

            int result = userService.updateUser(query);

            assertEquals(1, result);
            verify(passwordEncoder, never()).encode(anyString());
            verify(tUserMapper).updateByPrimaryKeySelective(argThat(user ->
                    "Updated User".equals(user.getName())
                            && user.getLoginPwd() == null
            ));
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

        when(redisManager.getList(anyString())).thenReturn(cachedList);

        List<TUser> result = userService.getOwnerList();

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(redisManager).getList(anyString());
    }

    @Test
    void testGetOwnerListFromDatabase() {
        TUser owner = new TUser();
        owner.setId(1);
        owner.setName("Owner");
        List<TUser> dbList = Collections.singletonList(owner);

        when(redisManager.getList(anyString())).thenReturn(null);
        when(tUserMapper.selectByOwner()).thenReturn(dbList);

        List<TUser> result = userService.getOwnerList();

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(tUserMapper).selectByOwner();
        verify(redisManager).setList(anyString(), anyList());
    }
}
