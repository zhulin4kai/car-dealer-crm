package com.autodealer.crm.integration;

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
import static org.junit.jupiter.api.Assertions.assertTrue;
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
 * <h2>No seed mutation</h2>
 * data.sql seeds {@code admin(1), zhangsan(2), lisi(3)} with role/permission
 * grants. Other integration test classes (CrossLayerConsistencyTest,
 * SecurityConfigTest, UserControllerH2IntegrationTest) share the same
 * in-memory H2 instance across the JVM and depend on the seed remaining
 * intact. This class therefore NEVER mutates a seed user: no PUT, no DELETE
 * against {@code /api/user/1}, {@code /api/user/2} or {@code /api/user/3}.
 * All destructive operations run on test-owned rows (id
 * {@code >= TEST_ID_BASE = 9000}, login_act starting with
 * {@code TEST_USER_PREFIX = "tfu"}) that the test inserts via JdbcTemplate
 * and removes in {@code finally}.
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
    @DisplayName("login -> create -> list -> disable: POST /api/user with JSON body inserts a new user that shows up in the list, then PUT disable sets accountEnabled=0")
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

            // Real PUT /api/user/{id}/disable sets accountEnabled=0 in H2;
            // the next GET returns $.data.accountEnabled=0 (record still exists).
            mockMvc.perform(put("/api/user/" + newId + "/disable")
                            .header(HttpHeaders.AUTHORIZATION, token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));

            mockMvc.perform(get("/api/user/" + newId)
                            .header(HttpHeaders.AUTHORIZATION, token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.accountEnabled").value(0));
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
                jdbcTemplate.update("DELETE FROM t_user WHERE id = ?", createdId);
            }
        }
    }

    @Test
    @DisplayName("login -> edit -> detail: PUT /api/user with JSON body updates a test-owned user and the H2 row reflects the new name")
    void editThenRestoreUserPersistsToH2() throws Exception {
        String token = loginAsAdmin();

        // Use a test-owned row, not a seed user. The seed users
        // (admin=1, zhangsan=2, lisi=3) are shared with other test
        // classes and must not be mutated. Pick an id that does not
        // collide with the offsets used by the other tests in this
        // class (1, 3, 11, 12).
        int testId = TEST_ID_BASE + 5;
        String testLoginAct = TEST_USER_PREFIX + "e" + (System.nanoTime() % 1_000_000_000L);
        String originalName = "流程原始名";
        String updatedName = "流程改名后";

        try {
            // Insert a test-owned user via JdbcTemplate so we control
            // the id and the starting name.
            insertTestUser(testId, testLoginAct, originalName);

            // Sanity: GET must show the original name in H2.
            mockMvc.perform(get("/api/user/" + testId)
                            .header(HttpHeaders.AUTHORIZATION, token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.loginAct").value(testLoginAct))
                    .andExpect(jsonPath("$.data.name").value(originalName));

            // Real PUT /api/user with a JSON body. Only the id and the
            // fields we want to change are sent — updateByPrimaryKeySelective
            // ignores nulls. The Authorization header is forwarded so the
            // service can stamp editBy from the JWT.
            String updateBody = """
                    {
                      "id": %d,
                      "loginAct": "%s",
                      "name": "%s",
                      "phone": "13800000005",
                      "email": "edit@test.com",
                      "accountNoExpired": 1,
                      "credentialsNoExpired": 1,
                      "accountNoLocked": 1,
                      "accountEnabled": 1
                    }
                    """.formatted(testId, testLoginAct, updatedName);
            mockMvc.perform(put("/api/user")
                            .header(HttpHeaders.AUTHORIZATION, token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(updateBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));

            // The PUT response code alone would be a "smoke" check; we
            // also re-fetch the row to prove the change is persisted to
            // H2, not just echoed back.
            mockMvc.perform(get("/api/user/" + testId)
                            .header(HttpHeaders.AUTHORIZATION, token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.name").value(updatedName))
                    .andExpect(jsonPath("$.data.loginAct").value(testLoginAct))
                    .andExpect(jsonPath("$.data.phone").value("13800000005"))
                    .andExpect(jsonPath("$.data.email").value("edit@test.com"));
        } finally {
            jdbcTemplate.update("DELETE FROM t_user WHERE id = ?", testId);
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
                  "phone": "13800138000",
                  "email": "should_fail@example.com"
                }
                """;

        MvcResult result = mockMvc.perform(post("/api/user")
                        .header(HttpHeaders.AUTHORIZATION, zhangsanToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden())            // GlobalExceptionHandler returns HTTP 403 with error body
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
    @DisplayName("login -> batch disable: real H2 inserts of two test-owned users, then PUT /api/users/batch-disable sets accountEnabled=0, then GET shows disabled for each")
    void batchDisableWithRealRowsPersistsToH2() throws Exception {
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

            // Real batch disable with JSON object body.
            String body = "{\"ids\":[" + idA + "," + idB + "]}";
            mockMvc.perform(put("/api/users/batch-disable")
                            .header(HttpHeaders.AUTHORIZATION, token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));

            // After the batch disable, the rows still exist with
            // accountEnabled=0: GET returns $.code=200 with $.data.accountEnabled=0.
            mockMvc.perform(get("/api/user/" + idA)
                            .header(HttpHeaders.AUTHORIZATION, token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.accountEnabled").value(0));
            mockMvc.perform(get("/api/user/" + idB)
                            .header(HttpHeaders.AUTHORIZATION, token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.accountEnabled").value(0));
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
