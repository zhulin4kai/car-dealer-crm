package com.autodealer.crm.integration;

import com.autodealer.crm.modules.identity.application.api.dto.user.ManagedUserDtos.StatusRequest;
import com.autodealer.crm.shared.error.BusinessException;
import com.autodealer.crm.modules.identity.application.api.model.TUser;
import com.autodealer.crm.modules.identity.application.api.ManagedUserAccountService;
import com.autodealer.crm.modules.identity.application.api.UserService;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Task 18 用户工作台真实数据库与安全链路回归。 */
class UserFlowIntegrationTest extends BackendIntegrationTestBase {

    @Autowired private ManagedUserAccountService managedUserAccountService;
    @Autowired private UserService userService;

    @Test
    @DisplayName("列表、筛选项和详情使用工作台聚合契约且不返回秘密字段")
    void workspaceReadContractsAreAggregatedAndSecretFree() throws Exception {
        String token = loginAsQualifiedAdmin();
        mockMvc.perform(get("/api/users").header(HttpHeaders.AUTHORIZATION, token)
                        .param("keyword", "EMP-000002").param("sortBy", "employeeNo")
                        .param("sortDirection", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.list[0].loginAct").value("zhangsan"))
                .andExpect(jsonPath("$.data.list[0].organizationName").value("测试公司"))
                .andExpect(jsonPath("$.data.list[0].roleNames").isArray())
                .andExpect(jsonPath("$.data.list[0].allowedActions").isArray())
                .andExpect(jsonPath("$.data.list[0].loginPwd").doesNotExist())
                .andExpect(jsonPath("$.data.list[0].phone").doesNotExist())
                .andExpect(jsonPath("$.data.list[0].email").doesNotExist());

        mockMvc.perform(get("/api/users/filter-options").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.organizations").isArray())
                .andExpect(jsonPath("$.data.positions").isArray())
                .andExpect(jsonPath("$.data.managers").isArray())
                .andExpect(jsonPath("$.data.roles").isArray())
                .andExpect(jsonPath("$.data.roles[?(@.label == '系统管理员')]").exists())
                .andExpect(jsonPath("$.data.assignableRoles").isArray())
                .andExpect(jsonPath("$.data.assignableRoles[?(@.label == '系统管理员')]").exists())
                .andExpect(jsonPath("$.data.lockStatuses").isArray());

        mockMvc.perform(get("/api/users/2").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.authorizationVersion").isNumber())
                .andExpect(jsonPath("$.data.sessionRevision").isNumber())
                .andExpect(jsonPath("$.data.statusCommands[0].command").exists())
                .andExpect(jsonPath("$.data.statusCommands[0].label").exists())
                .andExpect(jsonPath("$.data.statusCommands[0].destructive").isBoolean())
                .andExpect(jsonPath("$.data.loginPwd").doesNotExist());

        mockMvc.perform(get("/api/users/1").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("排序白名单和状态枚举拒绝非法查询")
    void listRejectsUnknownSortAndStatus() throws Exception {
        String token = loginAsQualifiedAdmin();
        mockMvc.perform(get("/api/users").header(HttpHeaders.AUTHORIZATION, token)
                        .param("sortBy", "login_pwd desc --"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/users").header(HttpHeaders.AUTHORIZATION, token)
                        .param("employmentStatus", "UNKNOWN"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Transactional
    @DisplayName("经理的列表和筛选在SQL层只保留组织范围内汇报树，详情区分本人和越界目标")
    void managerWorkspaceScopeRestrictsListFiltersDetailsAndStablePages() throws Exception {
        int[] primaryWorkplace = insertRealWorkplace("MANAGER_PRIMARY");
        int[] crossWorkplace = insertRealWorkplace("MANAGER_CROSS");
        jdbcTemplate.update("UPDATE t_employee_assignment SET organization_unit_id=?,position_id=? WHERE employee_id IN (1,2) AND active_primary_marker=1", primaryWorkplace[0], primaryWorkplace[1]);
        grantPersonalScope(3, "user:list", "REPORTING_TREE");
        grantPersonalScope(3, "user:view", "REPORTING_TREE");
        jdbcTemplate.update("INSERT INTO t_employee_reporting(subordinate_employee_id,manager_employee_id,relation_type,status,active_direct_marker,effective_from,reason,version,create_time,create_by) VALUES(1,2,'DIRECT','ACTIVE',1,CURRENT_TIMESTAMP,'Task18范围测试',0,CURRENT_TIMESTAMP,1)");

        int indirectUserId = 9711;
        int indirectEmployeeId = insertWorkspaceEmployee(indirectUserId, "scope_indirect", "SCOPE-002", primaryWorkplace[0], primaryWorkplace[1]);
        jdbcTemplate.update("INSERT INTO t_employee_reporting(subordinate_employee_id,manager_employee_id,relation_type,status,active_direct_marker,effective_from,reason,version,create_time,create_by) VALUES(?,1,'DIRECT','ACTIVE',1,CURRENT_TIMESTAMP,'Task18范围测试',0,CURRENT_TIMESTAMP,1)", indirectEmployeeId);
        int peerUserId = 9712;
        insertWorkspaceEmployee(peerUserId, "scope_peer", "SCOPE-003", primaryWorkplace[0], primaryWorkplace[1]);
        int crossUserId = 9713;
        int crossEmployeeId = insertWorkspaceEmployee(crossUserId, "scope_cross", "SCOPE-004", crossWorkplace[0], crossWorkplace[1]);
        jdbcTemplate.update("INSERT INTO t_employee_reporting(subordinate_employee_id,manager_employee_id,relation_type,status,active_direct_marker,effective_from,reason,version,create_time,create_by) VALUES(?,2,'DIRECT','ACTIVE',1,CURRENT_TIMESTAMP,'Task18范围测试',0,CURRENT_TIMESTAMP,1)", crossEmployeeId);

        String managerToken = loginAs("lisi", "123456", 3);
        MvcResult firstPage = mockMvc.perform(get("/api/users").header(HttpHeaders.AUTHORIZATION, managerToken)
                        .param("organizationUnitId", String.valueOf(primaryWorkplace[0]))
                        .param("employmentStatus", "ACTIVE")
                        .param("sortBy", "employeeNo").param("sortDirection", "asc")
                        .param("page", "1").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.list.length()").value(1))
                .andReturn();
        MvcResult secondPage = mockMvc.perform(get("/api/users").header(HttpHeaders.AUTHORIZATION, managerToken)
                        .param("organizationUnitId", String.valueOf(primaryWorkplace[0]))
                        .param("employmentStatus", "ACTIVE")
                        .param("sortBy", "employeeNo").param("sortDirection", "asc")
                        .param("page", "2").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(2))
                .andReturn();
        int firstId = objectMapper.readTree(firstPage.getResponse().getContentAsString()).path("data").path("list").get(0).path("id").asInt();
        int secondId = objectMapper.readTree(secondPage.getResponse().getContentAsString()).path("data").path("list").get(0).path("id").asInt();
        assertTrue(firstId == 2 || firstId == indirectUserId);
        assertTrue(secondId == 2 || secondId == indirectUserId);
        assertTrue(firstId != secondId, "稳定分页不得跨页重复");

        mockMvc.perform(get("/api/users/filter-options").header(HttpHeaders.AUTHORIZATION, managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.organizations.length()").value(1))
                .andExpect(jsonPath("$.data.organizations[0].id").value(primaryWorkplace[0]))
                .andExpect(jsonPath("$.data.organizations[?(@.id == " + crossWorkplace[0] + ")]").doesNotExist());

        mockMvc.perform(get("/api/users/2").header(HttpHeaders.AUTHORIZATION, managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.allowedActions[?(@ == 'VIEW')]").exists())
                .andExpect(jsonPath("$.data.allowedActions[?(@ == 'STATUS_UPDATE')]").doesNotExist());
        mockMvc.perform(get("/api/users/3").header(HttpHeaders.AUTHORIZATION, managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.allowedActions.length()").value(2))
                .andExpect(jsonPath("$.data.allowedActions[?(@ == 'VIEW')]").exists())
                .andExpect(jsonPath("$.data.allowedActions[?(@ == 'AUTHORIZATION_VIEW')]").exists());
        mockMvc.perform(get("/api/users/" + peerUserId).header(HttpHeaders.AUTHORIZATION, managerToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/users/" + crossUserId).header(HttpHeaders.AUTHORIZATION, managerToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/users/1").header(HttpHeaders.AUTHORIZATION, managerToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/users/999999").header(HttpHeaders.AUTHORIZATION, managerToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    @DisplayName("邀请创建原子写入账号、员工、任职、初始角色、历史和摘要，不返回凭证明文")
    void invitationCreatePersistsCompleteWorkspaceFactsAndHistory() throws Exception {
        String token = loginAsQualifiedAdmin();
        int roleId = jdbcTemplate.queryForObject("SELECT id FROM t_role WHERE role='sales_consultant'", Integer.class);
        int[] workplace = insertRealWorkplace("CREATE");
        String suffix = String.valueOf(System.nanoTime());
        String loginAct = "invite" + suffix.substring(Math.max(0, suffix.length() - 10));
        String employeeNo = "EMP-T18-" + suffix.substring(Math.max(0, suffix.length() - 8));
        String body = """
                {"loginAct":"%s","name":"邀请员工","phone":"13900008888","email":"invite.t18@example.com",
                 "employeeNo":"%s","organizationUnitId":%d,"positionId":%d,"managerEmployeeId":%d,"roleIds":[%d]}
                """.formatted(loginAct, employeeNo, workplace[0], workplace[1], QUALIFIED_ADMIN_EMPLOYEE_ID,roleId);

        MvcResult result = mockMvc.perform(post("/api/users").header(HttpHeaders.AUTHORIZATION, token)
                        .header("X-Request-Id", "task18-create-history")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.data.user.accountStatus").value("INVITED"))
                .andExpect(jsonPath("$.data.user.employmentStatus").value("PENDING"))
                .andExpect(jsonPath("$.data.user.authorizationVersion").value(0))
                .andExpect(jsonPath("$.data.user.sessionRevision").value(0))
                .andExpect(jsonPath("$.data.credentialDelivery.deliveryStatus").value("QUEUED"))
                .andExpect(jsonPath("$.data.credentialDelivery.credential").doesNotExist())
                .andExpect(jsonPath("$.data.user.loginPwd").doesNotExist())
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        int userId = response.path("data").path("user").path("id").asInt();
        assertEquals(1, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM t_employee WHERE user_id=?", Integer.class, userId));
        assertEquals(1, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM t_login_identifier WHERE user_id=? AND login_act=? AND status='ACTIVE' AND active_marker=1", Integer.class, userId, loginAct));
        assertEquals(1, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM t_employee_assignment a JOIN t_employee e ON e.id=a.employee_id WHERE e.user_id=? AND a.active_primary_marker=1", Integer.class, userId));
        assertEquals(1, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM t_user_role WHERE user_id=? AND role_id=? AND active_marker=1", Integer.class, userId, roleId));
        String snapshot = jdbcTemplate.queryForObject("SELECT after_value FROM t_authorization_history WHERE target_user_id=? AND role_id=? AND change_type='ASSIGN'", String.class, userId, roleId);
        assertTrue(snapshot.contains("sales_consultant"));
        assertTrue(snapshot.contains("销售顾问"));
        List<String> requestIds = jdbcTemplate.queryForList("SELECT DISTINCT request_id FROM t_authorization_history WHERE target_user_id=?", String.class, userId);
        assertEquals(1, requestIds.size());
        assertTrue(requestIds.get(0).startsWith("task18-create-history-"));
        String trustedRequestId = requestIds.get(0);
        assertEquals(3, jdbcTemplate.queryForObject("SELECT COUNT(DISTINCT subject_type) FROM t_authorization_history WHERE target_user_id=? AND subject_type IN ('ORGANIZATION_ASSIGNMENT','REPORTING_RELATION','USER_ROLE')", Integer.class, userId));
        List<String> operationActions = jdbcTemplate.queryForList("SELECT action_code FROM t_operation_log WHERE resource_id=? AND request_id=?", String.class, String.valueOf(userId), trustedRequestId);
        assertTrue(operationActions.contains("USER_CREATE"));
        assertTrue(operationActions.contains("EMPLOYEE_ASSIGNMENT_CHANGE"));
        assertTrue(operationActions.contains("USER_ROLE_CHANGE"));
        assertTrue(operationActions.contains("USER_INVITATION_ISSUE"));
        MvcResult historyResult = mockMvc.perform(get("/api/users/" + userId + "/history")
                        .header(HttpHeaders.AUTHORIZATION, token).param("size", "20"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.total").value(5)).andReturn();
        JsonNode history = objectMapper.readTree(historyResult.getResponse().getContentAsString()).path("data").path("list");
        assertTrue(containsAction(history, "USER_CREATE"));
        assertTrue(containsAction(history, "USER_INVITATION_ISSUE"));
        assertTrue(containsAction(history, "ORGANIZATION_ASSIGNMENT_CREATED"));
        assertTrue(containsAction(history, "REPORTING_RELATION_CREATED"));
        assertTrue(containsAction(history, "USER_ROLE_ASSIGNED"));
        String passwordHash = jdbcTemplate.queryForObject("SELECT login_pwd FROM t_user WHERE id=?", String.class, userId);
        assertFalse(result.getResponse().getContentAsString().contains(passwordHash));

        String placeholderLogin = "placeholder" + suffix.substring(Math.max(0, suffix.length() - 8));
        String placeholderBody = "{\"loginAct\":\"" + placeholderLogin + "\",\"name\":\"占位拒绝\",\"employeeNo\":\"PLACEHOLDER-18\",\"organizationUnitId\":2,\"positionId\":1,\"roleIds\":[" + roleId + "]}";
        mockMvc.perform(post("/api/users").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON).content(placeholderBody))
                .andExpect(status().isBadRequest());
        assertEquals(0, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM t_user WHERE login_act=?", Integer.class, placeholderLogin));
    }

    private boolean containsAction(JsonNode history, String actionCode) {
        for (JsonNode item : history) if (actionCode.equals(item.path("actionCode").asText())) return true;
        return false;
    }

    @Test
    @Transactional
    @DisplayName("状态命令使用账号版本并保持自动锁定与人工锁定事实独立")
    void statusCommandUsesCasAndKeepsManualAndAutoLockIndependent() throws Exception {
        String token = loginAsQualifiedAdmin();
        jdbcTemplate.update("UPDATE t_user SET auto_locked_until=DATEADD('HOUR',1,CURRENT_TIMESTAMP),version=0,manual_locked=0 WHERE id=2");
        String lock = "{\"accountVersion\":0,\"command\":\"LOCK\",\"reason\":\"人工复核\"}";
        mockMvc.perform(post("/api/users/2/status").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON).content(lock))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.lockStatus").value("MANUAL_LOCKED"))
                .andExpect(jsonPath("$.data.accountVersion").value(1));

        String unlock = "{\"accountVersion\":1,\"command\":\"UNLOCK\",\"reason\":\"人工复核完成\"}";
        mockMvc.perform(post("/api/users/2/status").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON).content(unlock))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.lockStatus").value("AUTO_LOCKED"))
                .andExpect(jsonPath("$.data.accountVersion").value(2));
        assertEquals(0, jdbcTemplate.queryForObject("SELECT manual_locked FROM t_user WHERE id=2", Integer.class));
        assertTrue(jdbcTemplate.queryForObject("SELECT auto_locked_until > CURRENT_TIMESTAMP FROM t_user WHERE id=2", Boolean.class));

        mockMvc.perform(post("/api/users/2/status").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON).content(unlock))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(627));
    }

    @Test
    @DisplayName("严格请求白名单拒绝 password、userId 和授权状态混入且不写入")
    void managedCommandsRejectUnknownFieldsWithoutPartialWrite() throws Exception {
        String token = loginAsQualifiedAdmin();
        int roleId = jdbcTemplate.queryForObject("SELECT id FROM t_role WHERE role='sales_consultant'", Integer.class);
        String loginAct = "strict" + System.nanoTime();
        String createBody = "{\"loginAct\":\"" + loginAct + "\",\"name\":\"严格请求\",\"employeeNo\":\"STRICT-18\",\"organizationUnitId\":1,\"positionId\":1,\"roleIds\":[" + roleId + "],\"password\":\"should-not-be-accepted\"}";
        mockMvc.perform(post("/api/users").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON).content(createBody))
                .andExpect(status().isBadRequest());
        assertEquals(0, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM t_user WHERE login_act=?", Integer.class, loginAct));

        int manualLockBefore = jdbcTemplate.queryForObject("SELECT manual_locked FROM t_user WHERE id=2", Integer.class);
        mockMvc.perform(post("/api/users/2/status").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountVersion\":0,\"command\":\"LOCK\",\"reason\":\"x\",\"userId\":3}"))
                .andExpect(status().isBadRequest());
        assertEquals(manualLockBefore, jdbcTemplate.queryForObject("SELECT manual_locked FROM t_user WHERE id=2", Integer.class));

        int adminRoleId = jdbcTemplate.queryForObject("SELECT id FROM t_role WHERE role='admin'", Integer.class);
        int[] workplace = insertRealWorkplace("ADMIN_MEMBER");
        String adminLogin = "ordinaryadmin" + System.nanoTime();
        String adminBody = "{\"loginAct\":\"" + adminLogin + "\",\"name\":\"普通安全管理员\",\"phone\":\"13900007777\",\"employeeNo\":\"ADMIN-" + System.nanoTime() + "\",\"organizationUnitId\":" + workplace[0] + ",\"positionId\":" + workplace[1] + ",\"managerEmployeeId\":"+QUALIFIED_ADMIN_EMPLOYEE_ID+",\"roleIds\":[" + adminRoleId + "]}";
        try {
            mockMvc.perform(post("/api/users").header(HttpHeaders.AUTHORIZATION, token)
                            .contentType(MediaType.APPLICATION_JSON).content(adminBody))
                    .andExpect(status().isAccepted());
            assertEquals(1, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM t_user WHERE login_act=?", Integer.class, adminLogin));
        } finally {
            cleanupInviteAttempt(adminLogin, workplace[0], workplace[1]);
        }
    }

    @Test
    @DisplayName("旧万能写入口保持 fail-close")
    void legacyWriteEntryPointsStayClosed() throws Exception {
        String token = loginAsQualifiedAdmin();
        mockMvc.perform(post("/api/user").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"loginAct\":\"legacy\",\"loginPwd\":\"secret\",\"name\":\"旧入口\",\"phone\":\"13900009999\",\"email\":\"legacy@example.com\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/users/batch-disable").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"ids\":[2,3]}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("两个有效管理员并发降级由共享数据库锁保证最多一个成功")
    void concurrentAdminDisableLeavesOneAvailableAdministrator() throws Exception {
        int operatorId=9810;
        int firstId = 9811;
        int secondId = 9812;
        int adminRoleId = jdbcTemplate.queryForObject("SELECT id FROM t_role WHERE role='admin'", Integer.class);
        int[] workplace = insertRealWorkplace("ADMIN_CONCURRENCY");
        int operatorRoleId=insertSecurityOperator(operatorId,"concurrent_security_operator",workplace);
        TUser operator=userService.getLoginUserById(operatorId);
        insertAvailableAdmin(firstId, "concurrent_admin_1", adminRoleId, workplace);
        insertAvailableAdmin(secondId, "concurrent_admin_2", adminRoleId, workplace);
        jdbcTemplate.update("UPDATE t_user SET account_enabled=0,account_status='DISABLED' WHERE id=?",QUALIFIED_ADMIN_USER_ID);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<String> first = executor.submit(() -> runDisable(operator, firstId, ready, start));
            Future<String> second = executor.submit(() -> runDisable(operator, secondId, ready, start));
            ready.await();
            start.countDown();
            List<String> results = List.of(first.get(), second.get());
            assertEquals(1, results.stream().filter("SUCCESS"::equals).count(), results.toString());
            assertEquals(1, results.stream().filter("ACCESS_DENIED"::equals).count(), results.toString());
            assertEquals(1, jdbcTemplate.queryForObject("""
                    SELECT COUNT(DISTINCT u.id) FROM t_user u
                    JOIN t_user_role ur ON ur.user_id=u.id AND ur.active_marker=1
                    JOIN t_role r ON r.id=ur.role_id AND r.role='admin' AND r.enabled=1
                    WHERE u.account_enabled=1 AND u.account_no_locked=1
                      AND u.account_type='HUMAN' AND u.protected_account=0 AND u.account_status='ACTIVE'
                    """, Integer.class));
        } finally {
            executor.shutdownNow();
            jdbcTemplate.update("UPDATE t_user SET account_enabled=1,account_status='ACTIVE' WHERE id=?",QUALIFIED_ADMIN_USER_ID);
            jdbcTemplate.update("DELETE FROM t_user_role WHERE user_id IN (?,?,?)", operatorId,firstId, secondId);
            jdbcTemplate.update("DELETE FROM t_role_permission WHERE role_id=?",operatorRoleId);
            jdbcTemplate.update("DELETE FROM t_employee_assignment WHERE employee_id IN (?,?,?)",operatorId, firstId, secondId);
            jdbcTemplate.update("DELETE FROM t_employee WHERE id IN (?,?,?)",operatorId, firstId, secondId);
            jdbcTemplate.update("DELETE FROM t_user WHERE id IN (?,?,?)",operatorId, firstId, secondId);
            jdbcTemplate.update("DELETE FROM t_role WHERE id=?",operatorRoleId);
            jdbcTemplate.update("DELETE FROM t_organization_unit WHERE id=?", workplace[0]);
            jdbcTemplate.update("DELETE FROM t_position WHERE id=?", workplace[1]);
        }
    }

    private String runDisable(TUser operator, int targetId, CountDownLatch ready, CountDownLatch start) {
        try {
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(operator, null, operator.getAuthorities()));
            ready.countDown();
            start.await();
            StatusRequest request = new StatusRequest();
            request.setAccountVersion(0);
            request.setCommand("DISABLE");
            request.setReason("并发最后管理员保护测试");
            managedUserAccountService.changeStatus(targetId, request);
            return "SUCCESS";
        } catch (BusinessException exception) {
            return exception.getCodeEnum().name();
        } catch (Exception exception) {
            return exception.getClass().getSimpleName();
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private void insertAvailableAdmin(int id, String loginAct, int adminRoleId, int[] workplace) {
        jdbcTemplate.update("""
                INSERT INTO t_user(id,login_act,login_pwd,name,account_no_expired,credentials_no_expired,
                  account_no_locked,account_enabled,account_type,protected_account,version,authorization_version,
                  auth_version,session_revision,account_status,must_change_password,failed_login_count,manual_locked,
                  create_time,create_by)
                VALUES(?,?,?,'并发管理员',1,1,1,1,'HUMAN',0,0,0,0,0,'ACTIVE',0,0,0,CURRENT_TIMESTAMP,1)
                """, id, loginAct, "$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi");
        jdbcTemplate.update("""
                INSERT INTO t_employee(id,user_id,employee_no,name,phone,employment_status,profile_completed,version,
                  profile_version,phone_verified,email_verified,create_time,create_by)
                VALUES(?,?,?,'并发管理员',?,'ACTIVE',1,0,0,1,0,CURRENT_TIMESTAMP,1)
                """, id, id, "EMP-" + id,"139"+String.format("%08d",id));
        jdbcTemplate.update("""
                INSERT INTO t_employee_assignment(employee_id,organization_unit_id,position_id,assignment_type,status,
                  active_primary_marker,effective_from,reason,version,create_time,create_by)
                VALUES(?,?,?,'PRIMARY','ACTIVE',1,CURRENT_TIMESTAMP,'并发管理员测试',0,CURRENT_TIMESTAMP,1)
                """, id, workplace[0], workplace[1]);
        jdbcTemplate.update("INSERT INTO t_user_role(user_id,role_id,granted_by,reason,effective_from,active_marker,version) VALUES(?,?,1,'并发测试',CURRENT_TIMESTAMP,1,0)", id, adminRoleId);
    }

    private int insertSecurityOperator(int id,String loginAct,int[] workplace){
        String roleCode="test_security_operator_"+System.nanoTime();
        jdbcTemplate.update("""
                INSERT INTO t_role(role,role_name,description,protected_role,authorization_level,default_data_scope,
                  scope_type,enabled,version) VALUES(?,?,'最后管理员并发测试操作者',1,101,'GLOBAL','GLOBAL',1,0)
                """,roleCode,"并发安全操作者");
        int roleId=jdbcTemplate.queryForObject("SELECT id FROM t_role WHERE role=?",Integer.class,roleCode);
        jdbcTemplate.update("""
                INSERT INTO t_role_permission(role_id,permission_id,delegable,data_scope_code)
                SELECT ?,id,0,'GLOBAL' FROM t_permission WHERE code='user:permission'
                """,roleId);
        jdbcTemplate.update("""
                INSERT INTO t_user(id,login_act,login_pwd,name,account_no_expired,credentials_no_expired,
                  account_no_locked,account_enabled,account_type,protected_account,version,authorization_version,
                  auth_version,session_revision,account_status,must_change_password,failed_login_count,manual_locked,
                  create_time,create_by)
                VALUES(?,?,?,'并发安全操作者',1,1,1,1,'HUMAN',0,0,0,0,0,'ACTIVE',0,0,0,CURRENT_TIMESTAMP,1)
                """,id,loginAct,"$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi");
        jdbcTemplate.update("""
                INSERT INTO t_employee(id,user_id,employee_no,name,phone,employment_status,profile_completed,version,
                  profile_version,phone_verified,email_verified,create_time,create_by)
                VALUES(?,?,?,'并发安全操作者',?,'ACTIVE',1,0,0,1,0,CURRENT_TIMESTAMP,1)
                """,id,id,"EMP-"+id,"139"+String.format("%08d",id));
        jdbcTemplate.update("""
                INSERT INTO t_employee_assignment(employee_id,organization_unit_id,position_id,assignment_type,status,
                  active_primary_marker,effective_from,reason,version,create_time,create_by)
                VALUES(?,?,?,'PRIMARY','ACTIVE',1,CURRENT_TIMESTAMP,'并发安全操作者任职',0,CURRENT_TIMESTAMP,1)
                """,id,workplace[0],workplace[1]);
        jdbcTemplate.update("INSERT INTO t_user_role(user_id,role_id,granted_by,reason,effective_from,active_marker,version) VALUES(?,?,1,'并发安全操作者',CURRENT_TIMESTAMP,1,0)",id,roleId);
        return roleId;
    }

    private int[] insertRealWorkplace(String prefix) {
        String suffix = prefix + System.nanoTime();
        jdbcTemplate.update("INSERT INTO t_organization_unit(code,name,type,parent_id,order_no,placeholder,enabled,version,create_time,create_by) VALUES(?,?, 'STORE',1,10,0,1,0,CURRENT_TIMESTAMP,1)", "T18_ORG_" + suffix, "Task18真实组织" + prefix);
        int organizationId = jdbcTemplate.queryForObject("SELECT id FROM t_organization_unit WHERE code=?", Integer.class, "T18_ORG_" + suffix);
        jdbcTemplate.update("INSERT INTO t_position(code,name,position_level,built_in,enabled,version,create_time,create_by) VALUES(?,?,10,0,1,0,CURRENT_TIMESTAMP,1)", "T18_POS_" + suffix, "Task18真实岗位" + prefix);
        int positionId = jdbcTemplate.queryForObject("SELECT id FROM t_position WHERE code=?", Integer.class, "T18_POS_" + suffix);
        return new int[]{organizationId, positionId};
    }

    private void grantPersonalScope(int userId, String permissionCode, String scope) {
        jdbcTemplate.update("""
                INSERT INTO t_user_permission(user_id,permission_id,effect,data_scope_code,effective_from,reason,granted_by,version,create_time)
                SELECT ?,id,'GRANT',?,CURRENT_TIMESTAMP,'Task18范围测试',1,0,CURRENT_TIMESTAMP
                FROM t_permission WHERE code=?
                """, userId, scope, permissionCode);
    }

    private int insertWorkspaceEmployee(int userId, String loginAct, String employeeNo,
                                        int organizationId, int positionId) {
        jdbcTemplate.update("""
                INSERT INTO t_user(id,login_act,login_pwd,name,account_no_expired,credentials_no_expired,
                  account_no_locked,account_enabled,account_type,protected_account,version,authorization_version,
                  auth_version,session_revision,account_status,must_change_password,failed_login_count,manual_locked,
                  create_time,create_by)
                VALUES(?,?,?,'范围测试员工',1,1,1,1,'HUMAN',0,0,0,0,0,'ACTIVE',0,0,0,CURRENT_TIMESTAMP,1)
                """, userId, loginAct, "$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi");
        jdbcTemplate.update("""
                INSERT INTO t_employee(user_id,employee_no,name,employment_status,profile_completed,version,profile_version,
                  phone_verified,email_verified,create_time,create_by)
                VALUES(?,?,'范围测试员工','ACTIVE',1,0,0,0,0,CURRENT_TIMESTAMP,1)
                """, userId, employeeNo);
        int employeeId = jdbcTemplate.queryForObject("SELECT id FROM t_employee WHERE user_id=?", Integer.class, userId);
        jdbcTemplate.update("""
                INSERT INTO t_employee_assignment(employee_id,organization_unit_id,position_id,assignment_type,status,
                  active_primary_marker,effective_from,reason,version,create_time,create_by)
                VALUES(?,?,?,'PRIMARY','ACTIVE',1,CURRENT_TIMESTAMP,'Task18范围测试',0,CURRENT_TIMESTAMP,1)
                """, employeeId, organizationId, positionId);
        return employeeId;
    }

    private void cleanupInviteAttempt(String loginAct, int organizationId, int positionId) {
        List<Integer> userIds = jdbcTemplate.queryForList("SELECT id FROM t_user WHERE login_act=?", Integer.class, loginAct);
        for (Integer userId : userIds) {
            List<Integer> employeeIds = jdbcTemplate.queryForList("SELECT id FROM t_employee WHERE user_id=?", Integer.class, userId);
            for (Integer employeeId : employeeIds) {
                jdbcTemplate.update("DELETE FROM t_employee_reporting WHERE subordinate_employee_id=?", employeeId);
                jdbcTemplate.update("DELETE FROM t_employee_assignment WHERE employee_id=?", employeeId);
            }
            jdbcTemplate.update("DELETE FROM t_credential_delivery_outbox WHERE user_id=?",userId);
            jdbcTemplate.update("DELETE FROM t_account_credential WHERE user_id=?", userId);
            jdbcTemplate.update("DELETE FROM t_authorization_history WHERE target_user_id=?", userId);
            jdbcTemplate.update("DELETE FROM t_user_role WHERE user_id=?", userId);
            jdbcTemplate.update("DELETE FROM t_employee WHERE user_id=?", userId);
            jdbcTemplate.update("DELETE FROM t_login_identifier WHERE user_id=?", userId);
            jdbcTemplate.update("DELETE FROM t_user WHERE id=?", userId);
        }
        jdbcTemplate.update("DELETE FROM t_position WHERE id=?", positionId);
        jdbcTemplate.update("DELETE FROM t_organization_unit WHERE id=?", organizationId);
    }
}
