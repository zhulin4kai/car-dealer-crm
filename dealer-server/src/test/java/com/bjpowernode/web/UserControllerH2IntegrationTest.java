package com.bjpowernode.web;

import com.bjpowernode.integration.BackendIntegrationTestBase;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.sql.Timestamp;
import java.time.Instant;
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
 * for UserController. The previous UserControllerTest used
 * {@code @MockBean UserService} with {@code addFilters = false}, which
 * never actually executed the security chain or the SQL. This class
 * replaces that surface with real ones.
 *
 * <p>Setup strategy: most tests insert their own rows via JdbcTemplate
 * (the controller's POST endpoint is missing {@code @RequestBody}, so
 * JSON bodies don't reach UserQuery — see the contract-bug test below
 * for explicit detection). Cleanup runs in {@code @AfterEach} and is
 * also a real batch DELETE call, so the test exercises both the
 * controller path and the SQL.
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
        // verbatim and let the real Mapper delete the rows. We add the
        // ids in one go so the cleanup is one request.
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

    // ==================== Single-row CRUD ====================

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
    @DisplayName("admin can DELETE /api/user/{id} -> real H2 DELETE -> GET returns null data")
    void adminDeleteUser_removesFromH2() throws Exception {
        int newId = nextTestId();
        insertTestUser(newId, "待删除");

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
        insertTestUser(idA, "批量A");
        insertTestUser(idB, "批量B");

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
        // Use a fake id - the auth check fires before the SQL, so no row
        // is actually deleted. We assert the rejection contract, not the
        // SQL effect.
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
    @DisplayName("GET /api/login/free requires auth (only /api/login is permitAll) - returns 510")
    void freeLogin_isProtected() throws Exception {
        // SecurityConfig only permits /api/login (not /api/login/free).
        // Real clients always have a token by the time they call this,
        // so an unauthenticated request gets the 510 from MyAuthenticationEntryPoint.
        mockMvc.perform(get("/api/login/free"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(510));
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

    // ==================== Contract bug: UserController.addUser / editUser missing @RequestBody ====================

    @Test
    @DisplayName("POST /api/user with JSON body MUST save and return $.code=200 (currently 500 — UserController.addUser is missing @RequestBody)")
    void adminCreateUser_withJsonBody_mustReturn200() throws Exception {
        String loginAct = "test_user_" + System.nanoTime();
        // The frontend sends JSON via doPost → axios({ data }).
        // The current UserController.addUser signature is
        //     addUser(UserQuery userQuery, @RequestHeader Authorization)
        // with NO @RequestBody on userQuery, so Spring tries form
        // binding and gets no fields from the JSON. passwordEncoder
        // then fails with "rawPassword cannot be null" → $.code=500.
        // This test pins the contract: when fixed (by adding @RequestBody),
        // this assertion passes; until then it documents the real bug.
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
                .andReturn();
        String responseBody = create.getResponse().getContentAsString();
        JsonNode resp = objectMapper.readTree(responseBody);
        assertEquals(200, resp.path("code").asInt(-1),
                "POST /api/user JSON body must reach UserQuery and return 200; body was: " + responseBody);

        // Once the create succeeds, the user must be visible.
        Integer newId = findUserIdByLoginAct(adminToken, loginAct);
        assertNotNull(newId, "Created user must appear in the list");
        createdTestIds.add(newId);
    }

    // ==================== Helpers ====================

    private int nextTestId() {
        int id = BASE_TEST_ID + createdTestIds.size();
        createdTestIds.add(id);
        return id;
    }

    /**
     * Inserts a test user directly into H2, bypassing the broken
     * addUser endpoint. We use this to seed rows for tests that
     * need an existing user to operate on (update, delete, batch).
     * The addUser endpoint bug is documented separately in
     * {@link #adminCreateUser_withJsonBody_mustReturn200()}.
     */
    private void insertTestUser(int id, String name) {
        jdbcTemplate.update(
                "INSERT INTO t_user (id, login_act, login_pwd, name, account_no_expired, "
                        + "credentials_no_expired, account_no_locked, account_enabled, create_time, create_by) "
                        + "VALUES (?, ?, ?, ?, 1, 1, 1, 1, ?, 1)",
                id,
                "test_user_" + id,
                "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy", // BCrypt("password")
                name,
                Timestamp.from(Instant.now()));
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
