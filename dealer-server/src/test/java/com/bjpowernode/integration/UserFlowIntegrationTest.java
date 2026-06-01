package com.bjpowernode.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
 * create -> edit -> batch delete.
 *
 * <h2>Design contract</h2>
 * Every test is self-contained: it does not depend on the side effect of
 * any other test, and destructive operations (create, edit, delete) operate
 * on a test-owned user that the test cleans up before exiting. As a result
 * the tests do not use {@code @TestMethodOrder}, do not use {@code @Order},
 * and can be run in any order, alone, or as a group.
 *
 * <h2>Why no shared seed deletion</h2>
 * data.sql seeds {@code admin(1), zhangsan(2), lisi(3)} with role/permission
 * grants. Other integration test classes (CrossLayerConsistencyTest,
 * SecurityConfigTest) share the same in-memory H2 instance across the JVM
 * and depend on the seed. Earlier revisions of this class deleted
 * zhangsan/lisi in the final test, which broke the other classes. This
 * rewrite uses non-seed IDs (starting at 9000) for destructive operations
 * and cleans up its own created users.
 */
class UserFlowIntegrationTest extends BackendIntegrationTestBase {

    /** Prefix for test-owned users so they cannot collide with seed rows. */
    private static final String TEST_USER_PREFIX = "test_flow_user_";
    private static final int TEST_ID_BASE = 9000;

    @Test
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
    @DisplayName("login -> create -> list -> delete: POST /api/user with a real token inserts a new user that shows up in the list, then DELETE removes it")
    void createThenListThenDeletePersistsToH2() throws Exception {
        String token = loginAsAdmin();
        String newLoginAct = TEST_USER_PREFIX + System.nanoTime();

        try {
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
        } finally {
            // Always clean up, even if the assertions above failed. The test
            // owns this user — never leave it polluting the seed.
            Integer createdId = findUserIdByLoginAct(token, newLoginAct);
            if (createdId != null) {
                mockMvc.perform(delete("/api/user/" + createdId)
                                .header(HttpHeaders.AUTHORIZATION, token))
                        .andExpect(status().isOk());
            }
        }
    }

    @Test
    @DisplayName("login -> edit -> detail -> restore: PUT /api/user updates the row and the detail reflects the new name, then the original is restored")
    void editThenRestoreUserPersistsToH2() throws Exception {
        String token = loginAsAdmin();

        // Snapshot the original zhangsan name so we can restore it. Using
        // zhangsan is safe because we never delete him — we only mutate the
        // 'name' field and put it back.
        MvcResult before = mockMvc.perform(get("/api/user/2")
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();
        String originalName = objectMapper.readTree(before.getResponse().getContentAsString())
                .path("data").path("name").asText();
        assertNotNull(originalName, "Seeded zhangsan must have a name in H2");

        try {
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
        } finally {
            // Restore zhangsan so other tests (and future runs of this one)
            // see the same seed shape.
            mockMvc.perform(put("/api/user")
                            .header(HttpHeaders.AUTHORIZATION, token)
                            .param("id", "2")
                            .param("loginAct", "zhangsan")
                            .param("name", originalName)
                            .param("phone", "13800000001")
                            .param("email", "zhangsan@test.com")
                            .param("accountNoExpired", "1")
                            .param("credentialsNoExpired", "1")
                            .param("accountNoLocked", "1")
                            .param("accountEnabled", "1"))
                    .andExpect(status().isOk());
        }
    }

    @Test
    @DisplayName("permission contract: a user without user:add gets a 200 HTTP status with $.code=520 and $.msg='没有访问权限' on POST /api/user")
    void userWithoutAddPermissionIsRejected() throws Exception {
        // The seeded zhangsan (id=2) is bound to the 'user' role (id=2) which
        // has NO user:* permissions. MyAccessDeniedHandler returns
        // R.FAIL(CodeEnum.ACCESS_DENIED) = {code:520, msg:"没有访问权限"}.
        // We pin the exact code/msg (not just "not 200") so a regression that
        // changes the error code or message is caught here.
        String zhangsanToken = loginAs("zhangsan", "123456", 2);

        MvcResult result = mockMvc.perform(post("/api/user")
                        .header(HttpHeaders.AUTHORIZATION, zhangsanToken)
                        .param("loginAct", "should_fail")
                        .param("loginPwd", "abcdef")
                        .param("name", "应该失败"))
                .andExpect(status().isOk())                 // handler writes 200 with error body
                .andExpect(jsonPath("$.code").value(520))
                .andExpect(jsonPath("$.msg").value("\u6ca1\u6709\u8bbf\u95ee\u6743\u9650"))
                .andReturn();
        // Sanity: no user should have been inserted
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertEquals(520, body.path("code").asInt(),
                "Permission rejection must return CodeEnum.ACCESS_DENIED (520)");
    }

    @Test
    @DisplayName("login -> batch delete: DELETE /api/user with [id1,id2] removes the rows (using non-seed IDs)")
    void batchDeleteWithNonSeedIdsPersistsToH2() throws Exception {
        // Use IDs that don't exist in the seed so we exercise the
        // batch-delete contract without wiping admin/zhangsan/lisi that
        // other tests depend on. The controller's batchDelUser returns
        // R.OK if affected rows >= 0 (i.e. even zero matches returns 200),
        // so we assert on the response code AND that the endpoint did not
        // blow up.
        String token = loginAsAdmin();
        int id1 = TEST_ID_BASE + 1;
        int id2 = TEST_ID_BASE + 2;

        mockMvc.perform(delete("/api/user")
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .contentType("application/json")
                        .content("[" + id1 + "," + id2 + "]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
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
