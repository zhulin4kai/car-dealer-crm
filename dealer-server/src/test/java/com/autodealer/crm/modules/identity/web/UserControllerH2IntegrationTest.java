package com.autodealer.crm.modules.identity.web;

import com.autodealer.crm.modules.identity.application.api.UserService;
import com.autodealer.crm.modules.identity.application.api.query.UserQuery;
import com.autodealer.crm.integration.BackendIntegrationTestBase;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

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
        adminToken = super.loginAsQualifiedAdmin();
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
            jdbcTemplate.update("DELETE FROM t_user_role WHERE user_id = ?", id);
            jdbcTemplate.update("DELETE FROM t_user WHERE id = ?", id);
        }
        createdTestIds.clear();
    }

    // ==================== Auth ====================

    @Test
    @DisplayName("合格 HUMAN 管理员可以列出可管理的普通用户")
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
        boolean foundOrdinaryUser = false;
        for (JsonNode node : list) {
            if ("zhangsan".equals(node.path("loginAct").asText())) {
                foundOrdinaryUser = true;
                assertEquals(2, node.path("id").asInt());
                break;
            }
        }
        assertTrue(foundOrdinaryUser, "普通受管用户必须出现在列表中");
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
    @DisplayName("合格 HUMAN 管理员可以读取下属的真实 H2 记录")
    void adminGetUserById_returnsRealRow() throws Exception {
        mockMvc.perform(get("/api/user/2")
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(2))
                .andExpect(jsonPath("$.data.loginAct").value("zhangsan"));
    }

    @Test
    @DisplayName("普通用户无需 user:view 权限也可以读取自己的受管详情")
    void ordinaryUserCanReadOwnManagedDetail() throws Exception {
        mockMvc.perform(get("/api/users/2")
                        .header(HttpHeaders.AUTHORIZATION, zhangsanToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(2))
                .andExpect(jsonPath("$.data.loginAct").value("zhangsan"));
    }

    @Test
    @DisplayName("GET /api/user/{id} for a non-existent id returns the managed-resource 404 contract")
    void adminGetUserById_nonExistent_returnsNotFound() throws Exception {
        mockMvc.perform(get("/api/user/9999")
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    @DisplayName("旧 POST /api/user 在专用邀请命令完成前必须 fail-close")
    void legacyCreateUser_isForbiddenWithoutWritingH2() throws Exception {
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

        mockMvc.perform(post("/api/user")
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
        assertEquals(0,jdbcTemplate.queryForObject("SELECT COUNT(*) FROM t_user WHERE login_act=?",Integer.class,loginAct));
    }

    @Test
    @DisplayName("旧 PUT /api/user 在专用资料命令完成前必须 fail-close")
    void legacyUpdateUser_isForbiddenWithoutWritingH2() throws Exception {
        int newId=nextTestId();String loginAct="test_user_update_"+newId;insertTestUser(newId,loginAct,"原始名");
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
                .andExpect(status().isForbidden());
        assertEquals("原始名",jdbcTemplate.queryForObject("SELECT name FROM t_user WHERE id=?",String.class,newId));
    }

    @Test
    @Transactional
    @DisplayName("管理员可通过专用命令修改下属白名单资料")
    void managedProfileUpdate_changesEmployeeAndProjectionTogether() throws Exception {
        Integer profileVersion = jdbcTemplate.queryForObject(
                "SELECT profile_version FROM t_employee WHERE user_id=2", Integer.class);
        String body = """
                {"profileVersion":%d,"name":"张三更新","phone":"13900000001","email":"zhangsan.updated@example.com"}
                """.formatted(profileVersion);

        mockMvc.perform(put("/api/users/2/profile")
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("张三更新"))
                .andExpect(jsonPath("$.data.profileVersion").value(profileVersion + 1));

        assertEquals("张三更新", jdbcTemplate.queryForObject(
                "SELECT name FROM t_employee WHERE user_id=2", String.class));
        assertEquals("张三更新", jdbcTemplate.queryForObject(
                "SELECT name FROM t_user WHERE id=2", String.class));
    }

    @Test
    @Transactional
    @DisplayName("受管资料命令拒绝角色和组织等越权字段")
    void managedProfileUpdate_rejectsAuthorizationFields() throws Exception {
        Integer profileVersion = jdbcTemplate.queryForObject(
                "SELECT profile_version FROM t_employee WHERE user_id=2", Integer.class);
        String originalName = jdbcTemplate.queryForObject(
                "SELECT name FROM t_employee WHERE user_id=2", String.class);
        String body = """
                {"profileVersion":%d,"name":"越权更新","phone":null,"email":null,"roleIds":[1],"organizationUnitId":1}
                """.formatted(profileVersion);

        mockMvc.perform(put("/api/users/2/profile")
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
        assertEquals(originalName, jdbcTemplate.queryForObject(
                "SELECT name FROM t_employee WHERE user_id=2", String.class));
    }

    @Test
    @DisplayName("旧禁用入口必须 fail-close")
    void legacyDisableUser_isForbiddenWithoutWritingH2() throws Exception {
        int newId = nextTestId();
        insertTestUser(newId, "test_user_" + newId, "待禁用");

        mockMvc.perform(put("/api/user/" + newId + "/disable")
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isForbidden());
        assertEquals(1,jdbcTemplate.queryForObject("SELECT account_enabled FROM t_user WHERE id=?",Integer.class,newId));
    }

    // ==================== Batch ====================

    @Test
    @DisplayName("旧批量禁用入口必须 fail-close")
    void legacyBatchDisable_isForbiddenWithoutWritingH2() throws Exception {
        int idA = nextTestId();
        int idB = nextTestId();
        insertTestUser(idA, "test_user_" + idA, "批量A");
        insertTestUser(idB, "test_user_" + idB, "批量B");

        String body = "{\"ids\":[" + idA + "," + idB + "]}";
        mockMvc.perform(put("/api/users/batch-disable")
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
        assertEquals(2,jdbcTemplate.queryForObject("SELECT COUNT(*) FROM t_user WHERE id IN (?,?) AND account_enabled=1",Integer.class,idA,idB));
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
                .andExpect(jsonPath("$.data.loginAct").value(QUALIFIED_ADMIN_LOGIN_ACT))
                .andExpect(jsonPath("$.data.protectedRecoveryAccount").value(false))
                .andExpect(jsonPath("$.data.userManagementGateState").value("UNINITIALIZED"));
    }

    @Test
    @Transactional
    @DisplayName("SYSTEM 管理员无需员工档案也能维护普通个人资料")
    void systemAdminProfile_usesIndependentUserProfileVersion() throws Exception {
        String recoveryToken=super.loginAsAdmin();
        mockMvc.perform(get("/api/profile").header(HttpHeaders.AUTHORIZATION,recoveryToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.profileVersion").value(0));
        String body="{\"profileVersion\":0,\"name\":\"安全管理员\",\"phone\":\"13700000000\",\"email\":\"admin@test.com\",\"avatarUrl\":\"https://example.com/avatar.png\"}";
        mockMvc.perform(put("/api/profile").header(HttpHeaders.AUTHORIZATION,recoveryToken)
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.name").value("安全管理员"))
                .andExpect(jsonPath("$.data.profileVersion").value(1));
        assertEquals(1,jdbcTemplate.queryForObject("SELECT profile_version FROM t_user WHERE id=1",Integer.class));
    }

    @Test
    @Transactional
    @DisplayName("本人资料拒绝授权字段且不发生部分写入")
    void ownProfile_rejectsAuthorizationFields() throws Exception {
        String recoveryToken=super.loginAsAdmin();
        String original=jdbcTemplate.queryForObject("SELECT name FROM t_user WHERE id=1",String.class);
        String body="{\"profileVersion\":0,\"name\":\"越权\",\"phone\":null,\"email\":null,\"avatarUrl\":null,\"roleIds\":[1]}";
        mockMvc.perform(put("/api/profile").header(HttpHeaders.AUTHORIZATION,recoveryToken)
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
        assertEquals(original,jdbcTemplate.queryForObject("SELECT name FROM t_user WHERE id=1",String.class));
    }

    @Test
    @Transactional
    @DisplayName("最后一个可恢复普通管理员不能替换唯一已验证联系方式")
    void lastRecoverableAdminCannotReplaceOnlyVerifiedContact() throws Exception {
        Integer profileVersion=jdbcTemplate.queryForObject("SELECT profile_version FROM t_employee WHERE user_id=?",Integer.class,QUALIFIED_ADMIN_USER_ID);
        String originalPhone=jdbcTemplate.queryForObject("SELECT phone FROM t_employee WHERE user_id=?",String.class,QUALIFIED_ADMIN_USER_ID);
        String body="{\"profileVersion\":"+profileVersion+",\"name\":\"合格测试管理员\",\"phone\":\"13900009001\",\"email\":null,\"avatarUrl\":null}";

        mockMvc.perform(put("/api/profile").header(HttpHeaders.AUTHORIZATION,adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value(605));

        assertEquals(originalPhone,jdbcTemplate.queryForObject("SELECT phone FROM t_employee WHERE user_id=?",String.class,QUALIFIED_ADMIN_USER_ID));
        assertEquals(1,jdbcTemplate.queryForObject("SELECT phone_verified FROM t_employee WHERE user_id=?",Integer.class,QUALIFIED_ADMIN_USER_ID));
    }

    @Test
    @DisplayName("GET /api/login/free requires and verifies the current token")
    void freeLogin_requiresVerifiedToken() throws Exception {
        mockMvc.perform(get("/api/login/free").header(HttpHeaders.AUTHORIZATION,adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        mockMvc.perform(get("/api/login/free").header(HttpHeaders.AUTHORIZATION,"Bearer expired-or-invalid"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Transactional
    @DisplayName("GET /api/owner respects recovery-account isolation and the caller's effective data scope")
    void owner_returnsOnlyEnabledUnlockedSalesUsers() throws Exception {
        String recoveryToken=super.loginAsAdmin();
        String orgCode="OWNER_VALID_ORG_"+System.nanoTime();
        jdbcTemplate.update("INSERT INTO t_organization_unit(code,name,type,parent_id,order_no,migration_placeholder,enabled,version,create_time,create_by) VALUES(?,?,'STORE',1,1,0,1,0,CURRENT_TIMESTAMP,1)",orgCode,orgCode);
        int organizationId=jdbcTemplate.queryForObject("SELECT id FROM t_organization_unit WHERE code=?",Integer.class,orgCode);
        String positionCode="OWNER_VALID_POSITION_"+System.nanoTime();
        jdbcTemplate.update("INSERT INTO t_position(code,name,position_level,built_in,enabled,version,create_time,create_by) VALUES(?,?,10,0,1,0,CURRENT_TIMESTAMP,1)",positionCode,positionCode);
        int positionId=jdbcTemplate.queryForObject("SELECT id FROM t_position WHERE code=?",Integer.class,positionCode);
        jdbcTemplate.update("UPDATE t_employee_assignment SET organization_unit_id=?,position_id=? WHERE employee_id IN (1,2) AND active_primary_marker=1",organizationId,positionId);
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
                        .param("permissionCode", "clue:add")
                        .param("qualificationContext", "CLUE_OWNER")
                        .header(HttpHeaders.AUTHORIZATION, recoveryToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(520));

        mockMvc.perform(get("/api/owner")
                        .param("permissionCode", "clue:add")
                        .param("qualificationContext", "CLUE_OWNER")
                        .header(HttpHeaders.AUTHORIZATION, zhangsanToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[?(@.name == '张三')]").exists())
                .andExpect(jsonPath("$.data[?(@.name == '李四')]").doesNotExist())
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
