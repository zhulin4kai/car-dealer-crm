package com.bjpowernode.web;

import com.bjpowernode.model.TUser;
import com.bjpowernode.query.UserQuery;
import com.bjpowernode.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.pagehelper.PageInfo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper;

    // ==================== GET /api/login/info ====================

    @Test
    @WithMockUser
    void loginInfo_shouldReturnCurrentUser() throws Exception {
        mockMvc.perform(get("/api/login/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    // ==================== GET /api/login/free ====================

    @Test
    @WithMockUser
    void freeLogin_shouldReturnOk() throws Exception {
        mockMvc.perform(get("/api/login/free"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    // ==================== GET /api/users ====================

    @Test
    @WithMockUser(authorities = {"user:list"})
    void userPage_shouldReturnPageInfo() throws Exception {
        TUser user = new TUser();
        user.setId(1);
        user.setLoginAct("admin");
        user.setName("管理员");
        PageInfo<TUser> pageInfo = new PageInfo<>(Collections.singletonList(user));

        when(userService.getUserByPage(1)).thenReturn(pageInfo);

        mockMvc.perform(get("/api/users")
                        .param("current", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.list[0].loginAct").value("admin"));
    }

    @Test
    @WithMockUser(authorities = {"user:list"})
    void userPage_withoutCurrentParam_shouldDefaultToOne() throws Exception {
        PageInfo<TUser> pageInfo = new PageInfo<>(Collections.emptyList());
        when(userService.getUserByPage(1)).thenReturn(pageInfo);

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    // ==================== GET /api/user/{id} ====================

    @Test
    @WithMockUser(authorities = {"user:view"})
    void userDetail_shouldReturnUser() throws Exception {
        TUser user = new TUser();
        user.setId(1);
        user.setLoginAct("admin");
        user.setName("管理员");

        when(userService.getUserById(1)).thenReturn(user);

        mockMvc.perform(get("/api/user/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.name").value("管理员"));
    }

    @Test
    @WithMockUser(authorities = {"user:view"})
    void userDetail_nonExistentId_shouldReturnNull() throws Exception {
        when(userService.getUserById(999)).thenReturn(null);

        mockMvc.perform(get("/api/user/999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    // ==================== POST /api/user ====================

    @Test
    @WithMockUser(authorities = {"user:add"})
    void addUser_success_shouldReturnOk() throws Exception {
        when(userService.saveUser(any(UserQuery.class))).thenReturn(1);

        mockMvc.perform(post("/api/user")
                        .header("Authorization", "Bearer test-token")
                        .param("loginAct", "newuser")
                        .param("name", "新用户")
                        .param("phone", "13800138000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @WithMockUser(authorities = {"user:add"})
    void addUser_failure_shouldReturnFail() throws Exception {
        when(userService.saveUser(any(UserQuery.class))).thenReturn(0);

        mockMvc.perform(post("/api/user")
                        .header("Authorization", "Bearer test-token")
                        .param("loginAct", "newuser")
                        .param("name", "新用户"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    // ==================== PUT /api/user ====================

    @Test
    @WithMockUser(authorities = {"user:edit"})
    void editUser_success_shouldReturnOk() throws Exception {
        when(userService.updateUser(any(UserQuery.class))).thenReturn(1);

        mockMvc.perform(put("/api/user")
                        .header("Authorization", "Bearer test-token")
                        .param("id", "1")
                        .param("name", "修改后姓名"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @WithMockUser(authorities = {"user:edit"})
    void editUser_failure_shouldReturnFail() throws Exception {
        when(userService.updateUser(any(UserQuery.class))).thenReturn(0);

        mockMvc.perform(put("/api/user")
                        .header("Authorization", "Bearer test-token")
                        .param("id", "1")
                        .param("name", "修改后姓名"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    // ==================== DELETE /api/user/{id} ====================

    @Test
    @WithMockUser(authorities = {"user:delete"})
    void delUser_success_shouldReturnOk() throws Exception {
        when(userService.delUserById(1)).thenReturn(1);

        mockMvc.perform(delete("/api/user/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @WithMockUser(authorities = {"user:delete"})
    void delUser_failure_shouldReturnFail() throws Exception {
        when(userService.delUserById(1)).thenReturn(0);

        mockMvc.perform(delete("/api/user/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    // ==================== DELETE /api/user (batch) ====================

    @Test
    @WithMockUser(authorities = {"user:delete"})
    void batchDelUser_success_shouldReturnOk() throws Exception {
        when(userService.batchDelUserIds(anyList())).thenReturn(2);

        mockMvc.perform(delete("/api/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Arrays.asList(1, 2))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @WithMockUser(authorities = {"user:delete"})
    void batchDelUser_failure_shouldReturnFail() throws Exception {
        when(userService.batchDelUserIds(anyList())).thenReturn(-1);

        mockMvc.perform(delete("/api/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Arrays.asList(1, 2))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    // ==================== GET /api/owner ====================

    @Test
    @WithMockUser
    void owner_shouldReturnOwnerList() throws Exception {
        TUser user = new TUser();
        user.setId(1);
        user.setName("管理员");

        when(userService.getOwnerList()).thenReturn(Collections.singletonList(user));

        mockMvc.perform(get("/api/owner"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].name").value("管理员"));
    }

    @Test
    @WithMockUser
    void owner_emptyList_shouldReturnEmpty() throws Exception {
        when(userService.getOwnerList()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/owner"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isEmpty());
    }
}
