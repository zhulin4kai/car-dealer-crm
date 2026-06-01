package com.bjpowernode.integration;

import com.bjpowernode.model.TUser;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end User flow against real H2 seed data: login -> list -> detail ->
 * create -> edit -> single delete -> batch delete. Validates that the
 * Controller, Service, Mapper, SQL, JWT and H2 seed data are all wired
 * together correctly. This is the example called out in
 * docs/codex_fix_tests_2026-05-31.md as the "first complete business
 * chain to fix".
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class UserFlowIntegrationTest extends BackendIntegrationTestBase {
    // Order numbering uses gaps of 10 to leave room for future tests to be
    // inserted between existing ones without renumbering. The 40 -> 60 gap
    // (skipping 50) groups the read/create/edit tests together and reserves
    // 50 for an as-yet-unwritten read-after-create validation test.
    // The 60 -> 70 jump is deliberate: Order 60 (permission) must run BEFORE
    // Order 70 (batch delete) because the permission test needs zhangsan
    // (id=2) to still exist.

    @Test
    @Order(10)
    @DisplayName("login -> list: admin can log in and the H2 seed users (admin, zhangsan, lisi) appear in /api/users")
    void loginAndListSeededUsers() throws Exception {
        String token = loginAsAdmin();

        MvcResult result = mockMvc.perform(get("/api/users")
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .param("current", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.list").isArray())
                .andExpect(jsonPath("$.data.pageNum").value(1))
                .andExpect(jsonPath("$.data.pageSize").value(10))
                .andReturn();

        String body = result.getResponse().getContentAsString();
        JsonNode list = objectMapper.readTree(body).path("data").path("list");
        assertTrue(list.isArray() && list.size() >= 3,
                "H2 seed should provide at least 3 users (admin, zhangsan, lisi), got ("
                        + list.size() + "): " + body);

        boolean hasAdmin = false, hasZhangsan = false, hasLisi = false;
        for (JsonNode node : list) {
            String act = node.path("loginAct").asText();
            if ("admin".equals(act)) hasAdmin = true;
            if ("zhangsan".equals(act)) hasZhangsan = true;
            if ("lisi".equals(act)) hasLisi = true;
        }
        assertTrue(hasAdmin, "Seeded user 'admin' must be visible in the list");
        assertTrue(hasZhangsan, "Seeded user 'zhangsan' must be visible in the list");
        assertTrue(hasLisi, "Seeded user 'lisi' must be visible in the list");
    }

    @Test
    @Order(20)
    @DisplayName("login -> detail: /api/user/{id} returns the seeded admin user with all H2 fields populated")
    void loginAndFetchUserDetail() throws Exception {
        String token = loginAsAdmin();

        mockMvc.perform(get("/api/user/1")
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.loginAct").value("admin"))
                .andExpect(jsonPath("$.data.name").value("\u7ba1\u7406\u5458"))
                .andExpect(jsonPath("$.data.accountEnabled").value(1));
    }

    @Test
    @Order(30)
    @DisplayName("login -> create -> list: POST /api/user with a real token inserts a new user that shows up in the list")
    void createUserPersistsToH2() throws Exception {
        String token = loginAsAdmin();
        String newLoginAct = "test_flow_user";

        MvcResult create = mockMvc.perform(post("/api/user")
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .param("loginAct", newLoginAct)
                        .param("loginPwd", "abcdef")
                        .param("name", "流程测试用户")
                        .param("phone", "13900099999")
                        .param("email", "flow@test.com")
                        .param("accountNoExpired", "1")
                        .param("credentialsNoExpired", "1")
                        .param("accountNoLocked", "1")
                        .param("accountEnabled", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();

        // The newly created user must be visible in the next list call.
        mockMvc.perform(get("/api/users")
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .param("current", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.list[?(@.loginAct == '" + newLoginAct + "')]").exists());

        // Cleanup: remove the test user so the test is idempotent.
        Integer createdId = findUserIdByLoginAct(token, newLoginAct);
        assertNotNull(createdId, "Newly created user must be findable in the list");
        mockMvc.perform(delete("/api/user/" + createdId)
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @Order(40)
    @DisplayName("login -> edit -> detail: PUT /api/user updates the H2 row and the detail reflects the new name")
    void editUserPersistsToH2() throws Exception {
        String token = loginAsAdmin();

        // Modify the seeded zhangsan (id=2) using a real update.
        mockMvc.perform(put("/api/user")
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .param("id", "2")
                        .param("loginAct", "zhangsan")
                        .param("name", "改名后的张三")
                        .param("phone", "13800000001")
                        .param("email", "zhangsan@test.com")
                        .param("accountNoExpired", "1")
                        .param("credentialsNoExpired", "1")
                        .param("accountNoLocked", "1")
                        .param("accountEnabled", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        mockMvc.perform(get("/api/user/2")
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.name").value("改名后的张三"));
    }

    @Test
    @Order(70)
    @DisplayName("login -> batch delete: DELETE /api/user with [id1,id2] removes the seeded users in H2")
    void batchDeleteUserPersistsToH2() throws Exception {
        // Order 70 is intentionally last: this test is the one and only place
        // in the change set that actually DELETEs the seeded users via the
        // real DELETE /api/user endpoint. The other batch-delete tests in
        // CrossLayerConsistencyTest and SecurityConfigTest use [999, 1000]
        // to avoid wiping seed data. The @DirtiesContext(AFTER_CLASS) on
        // this class ensures the next test class gets a fresh Spring context
        // (and H2 DB) so it sees the original seed again.
        String token = loginAsAdmin();

        // Seed contains zhangsan(2) and lisi(3); delete them via the documented plain-array contract.
        mockMvc.perform(delete("/api/user")
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[2,3]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // Both users must no longer be visible.
        mockMvc.perform(get("/api/user/2")
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty());

        mockMvc.perform(get("/api/user/3")
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    @Order(60)
    @DisplayName("permission failure: a user without user:add cannot create a new user")
    void userWithoutAddPermissionIsRejected() throws Exception {
        // The seeded zhangsan (id=2) is bound to the 'user' role (id=2) which
        // intentionally has NO user:* permissions (only clue:* and customer:*).
        // zhangsan MUST be rejected when attempting POST /api/user (which
        // requires user:add). Order 60 deliberately runs before the
        // @Order(70) batch-delete so zhangsan still exists for this test.
        String zhangsanToken = loginAs("zhangsan", "123456", 2);

        mockMvc.perform(post("/api/user")
                        .header(HttpHeaders.AUTHORIZATION, zhangsanToken)
                        .param("loginAct", "should_fail")
                        .param("loginPwd", "abcdef")
                        .param("name", "应该失败"))
                .andExpect(jsonPath("$.code").value(org.hamcrest.Matchers.not(200)));
    }

    private Integer findUserIdByLoginAct(String token, String loginAct) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/users")
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .param("current", "1"))
                .andReturn();
        JsonNode list = objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("list");
        for (JsonNode node : list) {
            if (loginAct.equals(node.path("loginAct").asText())) {
                return node.path("id").asInt();
            }
        }
        return null;
    }
}
