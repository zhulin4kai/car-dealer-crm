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
 * issues a JDBC DELETE to clean up test rows, since the DELETE endpoint
 * has been replaced by PUT disable (which only sets accountEnabled=0).
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
    void cleanupTestUsers() {
        if (createdTestIds.isEmpty()) {
            return;
        }
        // Use JDBC DELETE for cleanup since the DELETE /api/user endpoint
        // has been replaced by PUT disable (which only sets accountEnabled=0).
        for (int id : createdTestIds) {
            jdbcTemplate.update("DELETE FROM t_user WHERE id = ?", id);
        }
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
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(520))
                .andExpect(jsonPath("$.msg").value("\u6ca1\u6709\u8bbf\u95ee\u6743\u9650"));
    }

    @Test
    @DisplayName("unauthenticated request to /api/users is rejected with 510 (unauthorized)")
    void unauthenticated_isUnauthorized() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isUnauthorized())
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
        // JSON body is what the frontend sends via the user module API. With @RequestBody
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
    @DisplayName("admin can PUT /api/user/{id}/disable -> real H2 UPDATE -> GET returns accountEnabled=0")
    void adminDisableUser_setsAccountEnabledZero() throws Exception {
        int newId = nextTestId();
        insertTestUser(newId, "test_user_" + newId, "待禁用");

        mockMvc.perform(put("/api/user/" + newId + "/disable")
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        mockMvc.perform(get("/api/user/" + newId)
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.accountEnabled").value(0));
    }

    // ==================== Batch ====================

    @Test
    @DisplayName("admin can PUT /api/users/batch-disable with JSON body; the real SQL disables both users (accountEnabled=0)")
    void adminBatchDisable_persistsToH2() throws Exception {
        int idA = nextTestId();
        int idB = nextTestId();
        insertTestUser(idA, "test_user_" + idA, "批量A");
        insertTestUser(idB, "test_user_" + idB, "批量B");

        String body = "{\"ids\":[" + idA + "," + idB + "]}";
        mockMvc.perform(put("/api/users/batch-disable")
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        mockMvc.perform(get("/api/user/" + idA)
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accountEnabled").value(0));

        mockMvc.perform(get("/api/user/" + idB)
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accountEnabled").value(0));
    }

    @Test
    @DisplayName("zhangsan (no user:delete permission) is rejected from PUT /api/users/batch-disable with 520")
    void zhangsanBatchDisable_isPermissionDenied() throws Exception {
        // The auth check fires before the SQL, so no row is actually disabled.
        // We assert the rejection contract, not the SQL effect.
        mockMvc.perform(put("/api/users/batch-disable")
                        .header(HttpHeaders.AUTHORIZATION, zhangsanToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ids\":[9999, 9998]}"))
                .andExpect(status().isForbidden())
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
    @DisplayName("GET /api/owner returns only enabled unlocked sales owners")
    void owner_returnsOnlyEnabledUnlockedSalesUsers() throws Exception {
        int financeId = nextTestId();
        insertTestUser(financeId, "owner_finance_" + financeId, "测试财务");
        assignTestRole(financeId, "finance_specialist");
        int inventoryId = nextTestId();
        insertTestUser(inventoryId, "owner_inventory_" + inventoryId, "测试库存");
        assignTestRole(inventoryId, "inventory_specialist");
        int disabledSalesId = nextTestId();
        insertTestUser(disabledSalesId, "owner_disabled_" + disabledSalesId, "禁用销售", 1, 0);
        assignTestRole(disabledSalesId, "sales_consultant");
        int lockedSalesId = nextTestId();
        insertTestUser(lockedSalesId, "owner_locked_" + lockedSalesId, "锁定销售", 0, 1);
        assignTestRole(lockedSalesId, "sales_consultant");

        mockMvc.perform(get("/api/owner")
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[?(@.name == '张三')]").exists())
                .andExpect(jsonPath("$.data[?(@.name == '李四')]").exists())
                .andExpect(jsonPath("$.data[?(@.name == '管理员')]").doesNotExist())
                .andExpect(jsonPath("$.data[?(@.name == '测试财务')]").doesNotExist())
                .andExpect(jsonPath("$.data[?(@.name == '测试库存')]").doesNotExist())
                .andExpect(jsonPath("$.data[?(@.name == '禁用销售')]").doesNotExist())
                .andExpect(jsonPath("$.data[?(@.name == '锁定销售')]").doesNotExist());
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
        insertTestUser(id, loginAct, name, 1, 1);
    }

    private void insertTestUser(int id, String loginAct, String name,
                                int accountNoLocked, int accountEnabled) {
        jdbcTemplate.update(
                "INSERT INTO t_user (id, login_act, login_pwd, name, account_no_expired, "
                        + "credentials_no_expired, account_no_locked, account_enabled, create_time, create_by) "
                        + "VALUES (?, ?, ?, ?, 1, 1, ?, ?, CURRENT_TIMESTAMP, 1)",
                id,
                loginAct,
                "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy", // BCrypt("password")
                name,
                accountNoLocked,
                accountEnabled);
    }

    private void assignTestRole(int userId, String roleCode) {
        jdbcTemplate.update(
                "INSERT INTO t_user_role (user_id, role_id) "
                        + "SELECT ?, id FROM t_role WHERE role = ?",
                userId, roleCode);
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
