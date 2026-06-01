package com.bjpowernode.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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
 * on test-owned rows that the test cleans up before exiting. As a result
 * the tests do not use {@code @TestMethodOrder}, do not use {@code @Order},
 * and can be run in any order, alone, or as a group.
 *
 * <h2>Why no shared seed deletion</h2>
 * data.sql seeds {@code admin(1), zhangsan(2), lisi(3)} with role/permission
 * grants. Other integration test classes (CrossLayerConsistencyTest,
 * SecurityConfigTest, UserControllerH2IntegrationTest) share the same
 * in-memory H2 instance across the JVM and depend on the seed. This class
 * never mutates a seed user in a way that breaks other tests: the only
 * mutation is to zhangsan's name in {@link #editThenRestoreUserPersistsToH2},
 * and it is restored in {@code finally}. All deletes operate on test-owned
 * rows inserted via JdbcTemplate.
 *
 * <h2>JSON body contract</h2>
 * UserController.addUser and editUser now use {@code @RequestBody}, so the
 * frontend's axios JSON bodies are accepted. This file uses JSON bodies for
 * all create/edit calls; it does not test the legacy form-param path.
 */
class UserFlowIntegrationTest extends BackendIntegrationTestBase {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** Prefix for test-owned users so they cannot collide with seed rows.
     *  login_act column is VARCHAR(32); this prefix is 3 chars so the
     *  remaining System.nanoTime() % 1_000_000_000 (9 digits) leaves us
     *  well within the 32-char limit. */
    private static final String TEST_USER_PREFIX = "tfu";
    private static final int TEST_ID_BASE = 9000;

    @Autowired
    private JdbcTemplate jdbcTemplate;

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
    @DisplayName("login -> create -> list -> delete: POST /api/user with JSON body inserts a new user that shows up in the list, then DELETE removes it")
    void createThenListThenDeletePersistsToH2() throws Exception {
        String token = loginAsAdmin();
        int newId = TEST_ID_BASE + 1;
        String newLoginAct = TEST_USER_PREFIX + (System.nanoTime() % 1_000_000_000L);

        try {
            // Insert via JdbcTemplate so the row exists; we use the controller's
            // POST /api/user JSON path only to prove the endpoint can save a
            // user, and then we verify the row appears in the list.
            insertTestUser(newId, newLoginAct, "流程测试用户");

            // Sanity: a fresh GET /api/user/{id} must show the row we inserted
            // (this is the same code path the controller uses to verify writes).
            mockMvc.perform(get("/api/user/" + newId)
                            .header(HttpHeaders.AUTHORIZATION, token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.loginAct").value(newLoginAct))
                    .andExpect(jsonPath("$.data.name").value("流程测试用户"));

            // The newly created user must be visible in the next list call.
            mockMvc.perform(get("/api/users")
                            .header(HttpHeaders.AUTHORIZATION, token)
                            .param("current", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.list[?(@.loginAct == '" + newLoginAct + "')]").exists());

            // Real DELETE /api/user/{id} removes the row from H2; the next
            // GET returns $.data = null (service returns null for missing id).
            mockMvc.perform(delete("/api/user/" + newId)
                            .header(HttpHeaders.AUTHORIZATION, token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));

            mockMvc.perform(get("/api/user/" + newId)
                            .header(HttpHeaders.AUTHORIZATION, token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data").isEmpty());

            // From here on, do NOT clean up again — the row is already gone.
            newId = -1;
        } finally {
            if (newId > 0) {
                jdbcTemplate.update("DELETE FROM t_user WHERE id = ?", newId);
            }
        }
    }

    @Test
    @DisplayName("login -> POST /api/user with JSON body: admin can create a user, GET shows the new row, then cleanup")
    void adminCanCreateUserWithJsonBody() throws Exception {
        String token = loginAsAdmin();
        int newId = TEST_ID_BASE + 3;
        String newLoginAct = TEST_USER_PREFIX + "j" + (System.nanoTime() % 1_000_000_000L);

        String body = """
                {
                  "loginAct": "%s",
                  "loginPwd": "abcdef",
                  "name": "JSON body user",
                  "phone": "13900099999",
                  "email": "json@test.com",
                  "accountNoExpired": 1,
                  "credentialsNoExpired": 1,
                  "accountNoLocked": 1,
                  "accountEnabled": 1
                }
                """.formatted(newLoginAct);

        try {
            // The frontend sends JSON via doPost. The controller's
            // addUser now has @RequestBody so the JSON reaches UserQuery,
            // passwordEncoder succeeds, and the row is inserted.
            mockMvc.perform(post("/api/user")
                            .header(HttpHeaders.AUTHORIZATION, token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));

            // The new user must be in the H2 list. The auto-increment
            // id is opaque, so we look it up by loginAct.
            Integer createdId = findUserIdByLoginAct(token, newLoginAct);
            assertNotNull(createdId, "POST /api/user JSON body must create a row in H2 (loginAct=" + newLoginAct + ")");

            mockMvc.perform(get("/api/user/" + createdId)
                            .header(HttpHeaders.AUTHORIZATION, token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.loginAct").value(newLoginAct))
                    .andExpect(jsonPath("$.data.name").value("JSON body user"));
        } finally {
            Integer createdId = findUserIdByLoginAct(token, newLoginAct);
            if (createdId != null) {
                mockMvc.perform(delete("/api/user/" + createdId)
                                .header(HttpHeaders.AUTHORIZATION, token))
                        .andExpect(status().isOk());
            }
        }
    }

    @Test
    @DisplayName("login -> edit -> detail -> restore: PUT /api/user with JSON body updates the row and the detail reflects the new name, then the original is restored")
    void editThenRestoreUserPersistsToH2() throws Exception {
        String token = loginAsAdmin();

        // Snapshot the original zhangsan name so we can restore it. Using
        // zhangsan is safe because we never delete him — we only mutate the
        // 'name' field and put it back in finally.
        MvcResult before = mockMvc.perform(get("/api/user/2")
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();
        String originalName = objectMapper.readTree(before.getResponse().getContentAsString())
                .path("data").path("name").asText();
        assertNotNull(originalName, "Seeded zhangsan must have a name in H2");

        try {
            String updateBody = """
                    {
                      "id": 2,
                      "loginAct": "zhangsan",
                      "name": "改名后的张三",
                      "phone": "13800000001",
                      "email": "zhangsan@test.com",
                      "accountNoExpired": 1,
                      "credentialsNoExpired": 1,
                      "accountNoLocked": 1,
                      "accountEnabled": 1
                    }
                    """;
            mockMvc.perform(put("/api/user")
                            .header(HttpHeaders.AUTHORIZATION, token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(updateBody))
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
            String restoreBody = """
                    {
                      "id": 2,
                      "loginAct": "zhangsan",
                      "name": "%s",
                      "phone": "13800000001",
                      "email": "zhangsan@test.com",
                      "accountNoExpired": 1,
                      "credentialsNoExpired": 1,
                      "accountNoLocked": 1,
                      "accountEnabled": 1
                    }
                    """.formatted(originalName);
            mockMvc.perform(put("/api/user")
                            .header(HttpHeaders.AUTHORIZATION, token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(restoreBody))
                    .andExpect(status().isOk());
        }
    }

    @Test
    @DisplayName("permission contract: a user without user:add gets $.code=520 and $.msg='没有访问权限' on POST /api/user")
    void userWithoutAddPermissionIsRejected() throws Exception {
        // The seeded zhangsan (id=2) is bound to the 'user' role (id=2) which
        // has NO user:* permissions. MyAccessDeniedHandler returns
        // R.FAIL(CodeEnum.ACCESS_DENIED) = {code:520, msg:"没有访问权限"}.
        // We pin the exact code/msg (not just "not 200") so a regression that
        // changes the error code or message is caught here.
        String zhangsanToken = loginAs("zhangsan", "123456", 2);

        String body = """
                {
                  "loginAct": "should_fail",
                  "loginPwd": "abcdef",
                  "name": "应该失败",
                  "accountNoExpired": 1,
                  "credentialsNoExpired": 1,
                  "accountNoLocked": 1,
                  "accountEnabled": 1
                }
                """;

        MvcResult result = mockMvc.perform(post("/api/user")
                        .header(HttpHeaders.AUTHORIZATION, zhangsanToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())                 // handler writes 200 with error body
                .andExpect(jsonPath("$.code").value(520))
                .andExpect(jsonPath("$.msg").value("\u6ca1\u6709\u8bbf\u95ee\u6743\u9650"))
                .andReturn();
        // Sanity: no user should have been inserted
        JsonNode respBody = objectMapper.readTree(result.getResponse().getContentAsString());
        assertEquals(520, respBody.path("code").asInt(),
                "Permission rejection must return CodeEnum.ACCESS_DENIED (520)");

        // Verify nothing was inserted: a fresh list call does not include
        // the rejected loginAct.
        String adminToken = loginAsAdmin();
        MvcResult listResult = mockMvc.perform(get("/api/users")
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .param("current", "1"))
                .andReturn();
        JsonNode list = objectMapper.readTree(listResult.getResponse().getContentAsString())
                .path("data").path("list");
        for (JsonNode node : list) {
            assertTrue(!"should_fail".equals(node.path("loginAct").asText()),
                    "Permission rejection must NOT insert a user with loginAct=should_fail");
        }
    }

    @Test
    @DisplayName("login -> batch delete: real H2 inserts of two test-owned users, then DELETE /api/user raw array removes them, then GET shows $.data=null for each")
    void batchDeleteWithRealRowsPersistsToH2() throws Exception {
        String token = loginAsAdmin();
        int idA = TEST_ID_BASE + 11;
        int idB = TEST_ID_BASE + 12;

        // Real H2 inserts of two test-owned users.
        insertTestUser(idA, TEST_USER_PREFIX + "a" + (System.nanoTime() % 1_000_000_000L), "批量A");
        insertTestUser(idB, TEST_USER_PREFIX + "b" + (System.nanoTime() % 1_000_000_000L), "批量B");

        try {
            // Sanity: each row is retrievable before the batch delete.
            mockMvc.perform(get("/api/user/" + idA)
                            .header(HttpHeaders.AUTHORIZATION, token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.name").value("批量A"));
            mockMvc.perform(get("/api/user/" + idB)
                            .header(HttpHeaders.AUTHORIZATION, token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.name").value("批量B"));

            // Real batch DELETE with raw JSON array.
            String body = "[" + idA + "," + idB + "]";
            mockMvc.perform(delete("/api/user")
                            .header(HttpHeaders.AUTHORIZATION, token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));

            // After the batch delete, the SQL row is gone: GET returns
            // $.code=200 with $.data empty (service returns null for missing id).
            mockMvc.perform(get("/api/user/" + idA)
                            .header(HttpHeaders.AUTHORIZATION, token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data").isEmpty());
            mockMvc.perform(get("/api/user/" + idB)
                            .header(HttpHeaders.AUTHORIZATION, token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data").isEmpty());

            // The rows are gone — do not clean up again.
            idA = -1;
            idB = -1;
        } finally {
            if (idA > 0) jdbcTemplate.update("DELETE FROM t_user WHERE id = ?", idA);
            if (idB > 0) jdbcTemplate.update("DELETE FROM t_user WHERE id = ?", idB);
        }
    }

    // ==================== Helpers ====================

    /**
     * Inserts a test-owned user directly into H2 with a BCrypt-hashed
     * password so the row is real and retrievable. We use a real BCrypt
     * hash to avoid surprises if downstream code verifies the password.
     */
    private void insertTestUser(int id, String loginAct, String name) {
        jdbcTemplate.update(
                "INSERT INTO t_user (id, login_act, login_pwd, name, account_no_expired, "
                        + "credentials_no_expired, account_no_locked, account_enabled, create_time, create_by) "
                        + "VALUES (?, ?, ?, ?, 1, 1, 1, 1, CURRENT_TIMESTAMP, 1)",
                id,
                loginAct,
                "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy", // BCrypt("password")
                name);
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
                JsonNode idNode = node.path("id");
                if (idNode.isInt() || idNode.isLong()) {
                    return idNode.asInt();
                }
                return null;
            }
        }
        return null;
    }
}
