package com.autodealer.crm.web;

import com.autodealer.crm.integration.BackendIntegrationTestBase;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;
import java.util.List;

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
 * Real H2 + real Service + real Mapper + real Security integration tests
 * for UserController. The legacy UserControllerTest was removed because it
 * used {@code @MockBean UserService} with {@code addFilters = false}, which
 * never actually executed the security chain or the SQL.
 *
 * <p>Setup strategy: tests own the rows they insert. {@code @AfterEach}
 * issues a real batch DELETE to the controller, so the test exercises both
 * the controller path and the SQL.
 *
 * <p>JSON body contract: UserController.addUser and editUser have
 * {@code @RequestBody}, so the frontend's axios JSON bodies are accepted.
 * The tests cover both POST and PUT with JSON bodies end-to-end.
 */
class UserControllerH2IntegrationTest extends BackendIntegrationTestBase {

    private static final int BASE_TEST_ID = 9001;

    private String adminToken;
    private String zhangsanToken;
    private final List<Integer> createdTestIds = new ArrayList<>();

    @BeforeEach
    void setup() throws Exception {
        adminToken = super.loginAsAdmin();
        zhangsanToken = loginAs("zhangsan", "123456", 2);
    }

    @AfterEach
    void cleanupTestUsers() throws Exception {
        if (createdTestIds.isEmpty()) {
            return;
        }
        // The batch delete endpoint accepts a raw JSON array; we send it
        // verbatim and let the real Mapper delete the rows.
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < createdTestIds.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(createdTestIds.get(i));
        }
        sb.append("]");
        mockMvc.perform(delete("/api/user")
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sb.toString()))
                .andExpect(status().isOk());
        createdTestIds.clear();
    }

    // ==================== Auth ====================

    @Test
    @DisplayName("admin can list users and the result includes the seeded admin row")
    void adminListUsers_seesSeedData() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/users")
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .param("current", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.list").isArray())
                .andReturn();

        JsonNode list = objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("list");
        // Admin row (id=1, loginAct=admin) must be present in H2.
        boolean foundAdmin = false;
        for (JsonNode node : list) {
            if ("admin".equals(node.path("loginAct").asText())) {
                foundAdmin = true;
                assertEquals(1, node.path("id").asInt());
                break;
            }
        }
        assertTrue(foundAdmin, "Seeded admin user must appear in the list");
    }

    @Test
    @DisplayName("zhangsan (no user:* permission) is rejected from /api/users with 520")
    void zhangsanListUsers_isPermissionDenied() throws Exception {
        mockMvc.perform(get("/api/users")
                        .header(HttpHeaders.AUTHORIZATION, zhangsanToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(520))
                .andExpect(jsonPath("$.msg").value("\u6ca1\u6709\u8bbf\u95ee\u6743\u9650"));
    }

    @Test
    @DisplayName("unauthenticated request to /api/users is rejected with 510 (unauthorized)")
    void unauthenticated_isUnauthorized() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(510));
    }

    // ==================== Single-row CRUD via JSON body ====================

    @Test
    @DisplayName("admin can GET /api/user/{id} for the seeded admin and see the real H2 row")
    void adminGetUserById_returnsRealRow() throws Exception {
        mockMvc.perform(get("/api/user/1")
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.loginAct").value("admin"));
    }

    @Test
    @DisplayName("GET /api/user/{id} for a non-existent id returns 200 with $.data null (real service)")
    void adminGetUserById_nonExistent_returnsNullData() throws Exception {
        mockMvc.perform(get("/api/user/9999")
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    @DisplayName("admin can POST /api/user with a JSON body: real H2 INSERT, then GET shows the new row")
    void adminCreateUser_withJsonBody_persistsToH2() throws Exception {
        String loginAct = "test_user_" + System.nanoTime();
        // JSON body is what the frontend sends via doPost. With @RequestBody
        // on addUser, the JSON reaches UserQuery, passwordEncoder succeeds,
        // and the row is inserted into H2. The next GET /api/user/{id} and
        // the list endpoint both see the new row, proving the round-trip
        // through the real service and mapper.
        String body = """
                {
                  "loginAct": "%s",
                  "loginPwd": "test-password",
                  "name": "测试用户",
                  "phone": "13800138000",
                  "email": "test@example.com",
                  "accountNoExpired": 1,
                  "accountNoLocked": 1,
                  "credentialsNoExpired": 1,
                  "accountEnabled": 1
                }
                """.formatted(loginAct);

        MvcResult create = mockMvc.perform(post("/api/user")
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();
        assertEquals(200, objectMapper.readTree(create.getResponse().getContentAsString())
                        .path("code").asInt(-1),
                "POST /api/user JSON body must return 200; body was: "
                        + create.getResponse().getContentAsString());

        Integer newId = findUserIdByLoginAct(adminToken, loginAct);
        assertNotNull(newId, "POST /api/user JSON body must create a row in H2 (loginAct=" + loginAct + ")");
        createdTestIds.add(newId);

        // The created user must be visible to a direct GET.
        mockMvc.perform(get("/api/user/" + newId)
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.loginAct").value(loginAct))
                .andExpect(jsonPath("$.data.name").value("测试用户"));
    }

    @Test
    @DisplayName("admin can PUT /api/user with a JSON body: real H2 UPDATE, then GET reflects the new name")
    void adminUpdateUser_withJsonBody_persistsToH2() throws Exception {
        // First create a row to update. The create goes through the real
        // JSON POST path; the update goes through the real JSON PUT path.
        String loginAct = "test_user_update_" + System.nanoTime();
        String createBody = """
                {
                  "loginAct": "%s",
                  "loginPwd": "test-password",
                  "name": "原始名",
                  "phone": "13800138000",
                  "email": "test@example.com",
                  "accountNoExpired": 1,
                  "accountNoLocked": 1,
                  "credentialsNoExpired": 1,
                  "accountEnabled": 1
                }
                """.formatted(loginAct);
        mockMvc.perform(post("/api/user")
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        Integer newId = findUserIdByLoginAct(adminToken, loginAct);
        assertNotNull(newId, "Pre-condition: POST /api/user JSON body must create a row");
        createdTestIds.add(newId);

        // Now update via JSON PUT.
        String updateBody = """
                {
                  "id": %d,
                  "loginAct": "%s",
                  "name": "更新后姓名",
                  "phone": "13800138000",
                  "email": "test@example.com",
                  "accountNoExpired": 1,
                  "accountNoLocked": 1,
                  "credentialsNoExpired": 1,
                  "accountEnabled": 1
                }
                """.formatted(newId, loginAct);

        mockMvc.perform(put("/api/user")
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        mockMvc.perform(get("/api/user/" + newId)
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.name").value("更新后姓名"))
                .andExpect(jsonPath("$.data.loginAct").value(loginAct));
    }

    @Test
    @DisplayName("admin can DELETE /api/user/{id} -> real H2 DELETE -> GET returns null data")
    void adminDeleteUser_removesFromH2() throws Exception {
        int newId = nextTestId();
        insertTestUser(newId, "test_user_" + newId, "待删除");

        mockMvc.perform(delete("/api/user/" + newId)
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        mockMvc.perform(get("/api/user/" + newId)
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    // ==================== Batch ====================

    @Test
    @DisplayName("admin can batch DELETE /api/user with a raw JSON array; the real SQL deletes both rows")
    void adminBatchDelete_persistsToH2() throws Exception {
        int idA = nextTestId();
        int idB = nextTestId();
        insertTestUser(idA, "test_user_" + idA, "批量A");
        insertTestUser(idB, "test_user_" + idB, "批量B");

        String body = "[" + idA + "," + idB + "]";
        mockMvc.perform(delete("/api/user")
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        mockMvc.perform(get("/api/user/" + idA)
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty());

        mockMvc.perform(get("/api/user/" + idB)
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    @DisplayName("zhangsan (no user:delete permission) is rejected from batch DELETE with 520")
    void zhangsanBatchDelete_isPermissionDenied() throws Exception {
        // The auth check fires before the SQL, so no row is actually deleted.
        // We assert the rejection contract, not the SQL effect.
        mockMvc.perform(delete("/api/user")
                        .header(HttpHeaders.AUTHORIZATION, zhangsanToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[9999, 9998]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(520))
                .andExpect(jsonPath("$.msg").value("\u6ca1\u6709\u8bbf\u95ee\u6743\u9650"));
    }

    // ==================== Login info / free / owner ====================

    @Test
    @DisplayName("GET /api/login/info returns the currently authenticated principal (real JWT, real DB)")
    void loginInfo_returnsCurrentPrincipal() throws Exception {
        mockMvc.perform(get("/api/login/info")
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.loginAct").value("admin"));
    }

    @Test
    @DisplayName("GET /api/login/free is publicly accessible")
    void freeLogin_isPublic() throws Exception {
        mockMvc.perform(get("/api/login/free"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("GET /api/owner returns the owner list (real SQL, real data)")
    void owner_returnsRealList() throws Exception {
        mockMvc.perform(get("/api/owner")
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }

    // ==================== Helpers ====================

    private int nextTestId() {
        int id = BASE_TEST_ID + createdTestIds.size();
        createdTestIds.add(id);
        return id;
    }

    /**
     * Inserts a test-owned user directly into H2 with a BCrypt-hashed
     * password. Used by tests that need a row to operate on but do not
     * want to assert on the create path.
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
