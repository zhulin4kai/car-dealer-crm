package com.autodealer.crm.modules.identity.web;

import com.autodealer.crm.modules.audit.application.api.OperationAuditRecorder;
import com.autodealer.crm.modules.identity.application.api.dto.organization.AssignmentInput;
import com.autodealer.crm.modules.identity.application.api.dto.organization.ReportingInput;
import com.autodealer.crm.modules.identity.application.api.dto.organization.UpdateEmployeeOrganizationRequest;
import com.autodealer.crm.modules.identity.application.api.dto.organization.UpdateOrganizationUnitRequest;
import com.autodealer.crm.modules.identity.application.api.enums.AssignmentType;
import com.autodealer.crm.modules.identity.application.api.enums.ReportingType;
import com.autodealer.crm.modules.identity.application.api.enums.OrganizationUnitType;
import com.autodealer.crm.shared.error.BusinessException;
import com.autodealer.crm.integration.BackendIntegrationTestBase;
import com.autodealer.crm.modules.identity.application.api.model.TUser;
import com.autodealer.crm.shared.error.CodeEnum;
import com.autodealer.crm.modules.identity.application.api.OrganizationService;
import com.autodealer.crm.modules.identity.application.api.UserService;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
class OrganizationControllerH2IntegrationTest extends BackendIntegrationTestBase {
    private static final int QUALIFIED_ADMIN_USER_ID=9100;
    private static final int QUALIFIED_ADMIN_EMPLOYEE_ID=9100;
    private static final int QUALIFIED_ADMIN_POSITION_ID=9100;
    private static final String QUALIFIED_ADMIN_LOGIN="organization_test_admin";
    private String adminToken;

    @MockBean
    private OperationAuditRecorder operationAuditRecorder;

    @Autowired private OrganizationService organizationService;
    @Autowired private UserService userService;
    @Autowired private PlatformTransactionManager transactionManager;

    @BeforeEach
    void login() throws Exception {
        ensureQualifiedHumanAdministrator();
        adminToken = loginAs(QUALIFIED_ADMIN_LOGIN,"123456",QUALIFIED_ADMIN_USER_ID);
    }

    @Test
    void adminCanCreateCatalogAssignEmployeeAndWriteHistoryAgainstUserId() throws Exception {
        int suffix = Math.abs((int) (System.nanoTime() % 1_000_000));
        int organizationId = createOrganization("STORE_" + suffix);
        int positionId = createPosition("POSITION_" + suffix);
        String effectiveFrom = OffsetDateTime.now().minusSeconds(2).toString();

        mockMvc.perform(put("/api/employees/1/organization-membership")
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "expectedVersion": 0,
                                  "primaryAssignment": {
                                    "organizationUnitId": %d,
                                    "positionId": %d,
                                    "assignmentType": "PRIMARY",
                                    "effectiveFrom": "%s"
                                  },
                                  "additionalAssignments": [],
                                  "reporting": {
                                    "managerEmployeeId": 9100,
                                    "relationType": "DIRECT",
                                    "effectiveFrom": "%s"
                                  },
                                  "reason": "集成测试调整任职"
                                }
                                """.formatted(organizationId, positionId, effectiveFrom,effectiveFrom)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.employee.id").value(1))
                .andExpect(jsonPath("$.data.primaryAssignment.organizationUnitId").value(organizationId));

        Integer targetUserId = jdbcTemplate.queryForObject("""
                SELECT target_user_id FROM t_authorization_history
                WHERE subject_type = 'ORGANIZATION_ASSIGNMENT' AND subject_id = '1'
                ORDER BY id DESC LIMIT 1
                """, Integer.class);
        assertEquals(2, targetUserId);
        java.sql.Timestamp newStart = jdbcTemplate.queryForObject("""
                SELECT effective_from FROM t_employee_assignment
                WHERE employee_id=1 AND active_primary_marker=1
                """, java.sql.Timestamp.class);
        java.sql.Timestamp oldEnd = jdbcTemplate.queryForObject("""
                SELECT effective_to FROM t_employee_assignment
                WHERE employee_id=1 AND active_primary_marker IS NULL AND status='ENDED'
                ORDER BY id DESC LIMIT 1
                """, java.sql.Timestamp.class);
        assertEquals(newStart, oldEnd, "旧事实关闭时刻必须与新事实服务端生效时刻完全一致");
        assertTrue(newStart.toInstant().isAfter(OffsetDateTime.parse(effectiveFrom).toInstant()),
                "客户端回溯 effectiveFrom 不得写入新事实");
    }

    @Test
    void userWithAssignmentPermissionStillCannotUpdateSelf() throws Exception {
        jdbcTemplate.update("""
                INSERT INTO t_role_permission (role_id, permission_id, delegable, data_scope_code)
                SELECT r.id, p.id, p.delegable, r.default_data_scope
                FROM t_role r CROSS JOIN t_permission p
                WHERE r.role = 'sales_consultant' AND p.code = 'employee:assignment'
                  AND NOT EXISTS (
                    SELECT 1 FROM t_role_permission rp
                    WHERE rp.role_id = r.id AND rp.permission_id = p.id)
                """);
        String token = loginAs("zhangsan", "123456", 2);

        mockMvc.perform(put("/api/employees/1/organization-membership")
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "expectedVersion": 0,
                                  "primaryAssignment": {
                                    "organizationUnitId": 2,
                                    "positionId": 1,
                                    "assignmentType": "PRIMARY",
                                    "effectiveFrom": "2026-01-01T00:00:00+08:00"
                                  },
                                  "additionalAssignments": [],
                                  "reporting": null,
                                  "reason": "不得生效的本人调整"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(593));
    }

    @Test
    void hierarchyDisableAndCasConflictsReturnStableCodes() throws Exception {
        int suffix = Math.abs((int) (System.nanoTime() % 1_000_000));
        int storeId = createOrganization("STORE_RULE_" + suffix);
        int teamId = createOrganization("TEAM_RULE_" + suffix, "TEAM", storeId);

        mockMvc.perform(put("/api/organization-units/" + storeId)
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"expectedVersion":0,"name":"循环门店","type":"STORE",
                                 "parentId":%d,"leaderEmployeeId":null,"orderNo":10}
                                """.formatted(storeId)))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value(591));

        mockMvc.perform(post("/api/organization-units")
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"ILLEGAL_%d","name":"非法团队","type":"TEAM",
                                 "parentId":1,"leaderEmployeeId":null,"orderNo":1}
                                """.formatted(suffix)))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value(599));

        mockMvc.perform(put("/api/organization-units/" + storeId + "/disable")
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expectedVersion\":0,\"reason\":\"停用测试\"}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value(595));

        mockMvc.perform(put("/api/organization-units/" + teamId)
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"expectedVersion":0,"name":"团队新名","type":"TEAM",
                                 "parentId":%d,"leaderEmployeeId":null,"orderNo":2}
                                """.formatted(storeId)))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/organization-units/" + teamId)
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"expectedVersion":0,"name":"过期写入","type":"TEAM",
                                 "parentId":%d,"leaderEmployeeId":null,"orderNo":3}
                                """.formatted(storeId)))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value(590));
    }

    @Test
    void primarySecondaryAndActingPeriodsAreEnforced() throws Exception {
        int suffix = Math.abs((int) (System.nanoTime() % 1_000_000));
        int primaryOrg = createOrganization("PRIMARY_" + suffix);
        int secondaryOrg = createOrganization("SECONDARY_" + suffix);
        int positionId = createPosition("POSITION_PERIOD_" + suffix);
        String from = OffsetDateTime.now().minusSeconds(2).toString();
        String to = OffsetDateTime.now().plusDays(1).toString();

        mockMvc.perform(put("/api/employees/1/organization-membership")
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(membershipBody(0, primaryOrg, positionId, from,
                                "[{\"organizationUnitId\":%d,\"positionId\":%d,\"assignmentType\":\"SECONDARY\",\"effectiveFrom\":\"%s\"}]"
                                        .formatted(secondaryOrg, positionId, from), "null")))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value(598));

        mockMvc.perform(put("/api/employees/1/organization-membership")
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(membershipBody(0, primaryOrg, positionId, from,
                                "[{\"organizationUnitId\":%d,\"positionId\":%d,\"assignmentType\":\"SECONDARY\",\"effectiveFrom\":\"%s\",\"effectiveTo\":\"%s\"}]"
                                        .formatted(secondaryOrg, positionId, from, to), reportingJson(QUALIFIED_ADMIN_EMPLOYEE_ID,"DIRECT",from,null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.primaryAssignment.organizationUnitId").value(primaryOrg))
                .andExpect(jsonPath("$.data.additionalAssignments[0].organizationUnitId").value(secondaryOrg));

        mockMvc.perform(put("/api/organization-units/" + primaryOrg + "/disable")
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expectedVersion\":0,\"reason\":\"仍有员工\"}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value(596));
    }

    @Test
    void multiLevelReportingCycleAndActingEndTimeAreRejected() throws Exception {
        int suffix = Math.abs((int) (System.nanoTime() % 1_000_000));
        int orgId = createOrganization("REPORTING_" + suffix);
        int positionId = createPosition("REPORTING_POS_" + suffix);
        insertThirdHumanEmployee(orgId, positionId, suffix);
        String from = OffsetDateTime.now().minusSeconds(2).toString();

        updateMembership(2, 0, orgId, positionId, from, reportingJson(QUALIFIED_ADMIN_EMPLOYEE_ID,"DIRECT",from,null));
        updateMembership(3, 0, orgId, positionId, from, reportingJson(QUALIFIED_ADMIN_EMPLOYEE_ID,"DIRECT",from,null));
        updateMembership(1, 0, orgId, positionId, from,
                reportingJson(2, "DIRECT", from, null));
        updateMembership(2, 1, orgId, positionId, from,
                reportingJson(3, "DIRECT", from, null));

        mockMvc.perform(put("/api/employees/3/organization-membership")
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(membershipBody(1, orgId, positionId, from, "[]",
                                reportingJson(1, "DIRECT", from, null))))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value(592));

        mockMvc.perform(put("/api/employees/3/organization-membership")
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(membershipBody(1, orgId, positionId, from, "[]",
                                reportingJson(QUALIFIED_ADMIN_EMPLOYEE_ID, "ACTING", from, null))))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value(598));
    }

    @Test
    void assignmentOnlyAuthorityCanChangeAssignmentWithoutTouchingReporting() throws Exception {
        int suffix = Math.abs((int) (System.nanoTime() % 1_000_000));
        int oldOrg = createOrganization("OLD_SCOPE_" + suffix);
        int newOrg = createOrganization("NEW_SCOPE_" + suffix,"DEPARTMENT",oldOrg);
        int positionId = createPosition("SCOPE_POS_" + suffix);
        String from = OffsetDateTime.now().minusSeconds(2).toString();
        updateMembership(2, 0, oldOrg, positionId, from, reportingJson(QUALIFIED_ADMIN_EMPLOYEE_ID,"DIRECT",from,null));
        updateMembership(1, 0, oldOrg, positionId, from, reportingJson(2, "DIRECT", from, null));
        grantRolePermission("sales_manager", "employee:assignment");
        String token = loginAs("lisi", "123456", 3);

        String existingReporting = reportingJson(2, "DIRECT", from, null);
        mockMvc.perform(put("/api/employees/1/organization-membership")
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(membershipBody(1, newOrg, positionId, from, "[]", existingReporting)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.primaryAssignment.organizationUnitId").value(newOrg))
                .andExpect(jsonPath("$.data.reporting.managerEmployeeId").value(2));
    }

    @Test
    void reportingOnlyAuthorityCanChangeReportingWithoutTouchingAssignment() throws Exception {
        int suffix = Math.abs((int) (System.nanoTime() % 1_000_000));
        int orgId = createOrganization("REPORT_ONLY_" + suffix);
        int positionId = createPosition("REPORT_ONLY_POS_" + suffix);
        String from = OffsetDateTime.now().minusSeconds(2).toString();
        updateMembership(2, 0, orgId, positionId, from, reportingJson(QUALIFIED_ADMIN_EMPLOYEE_ID,"DIRECT",from,null));
        updateMembership(1, 0, orgId, positionId, from, reportingJson(QUALIFIED_ADMIN_EMPLOYEE_ID,"DIRECT",from,null));
        grantRolePermission("sales_manager", "employee:reporting");
        insertActingManagerRelation(1,2);
        String token = loginAs("lisi", "123456", 3);

        mockMvc.perform(put("/api/employees/1/organization-membership")
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(membershipBody(1, orgId, positionId, from, "[]",
                                reportingJson(2, "DIRECT", from, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.primaryAssignment.organizationUnitId").value(orgId))
                .andExpect(jsonPath("$.data.reporting.managerEmployeeId").value(2));

        assertEquals(1, jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM t_employee_assignment
                WHERE employee_id = 1 AND status = 'ACTIVE' AND active_primary_marker = 1
                """, Integer.class));
    }

    @Test
    void ordinaryManagerCanManageDirectAndIndirectReportsButNotCrossOrganization() throws Exception {
        int suffix = Math.abs((int) (System.nanoTime() % 1_000_000));
        int storeA = createOrganization("MANAGER_A_" + suffix);
        int departmentA = createOrganization("MANAGER_DEPT_" + suffix, "DEPARTMENT", storeA);
        int teamA = createOrganization("MANAGER_TEAM_" + suffix, "TEAM", departmentA);
        int storeB = createOrganization("MANAGER_B_" + suffix);
        int positionId = createPosition("MANAGER_POS_" + suffix);
        String from = OffsetDateTime.now().minusSeconds(2).toString();
        updateMembership(2, 0, storeA, positionId, from, reportingJson(QUALIFIED_ADMIN_EMPLOYEE_ID,"DIRECT",from,null));
        updateMembership(1, 0, storeA, positionId, from, reportingJson(2, "DIRECT", from, null));
        insertThirdHumanEmployee(storeA, positionId, suffix);
        updateMembership(3, 0, storeA, positionId, from, reportingJson(1, "DIRECT", from, null));
        grantRolePermission("sales_manager", "organization:list");
        grantRolePermission("sales_manager", "organization:view");
        grantRolePermission("sales_manager", "employee:assignment");
        String managerToken = loginAs("lisi", "123456", 3);

        mockMvc.perform(put("/api/employees/1/organization-membership")
                        .header(HttpHeaders.AUTHORIZATION, managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(membershipBody(1, departmentA, positionId, from, "[]",
                                reportingJson(2, "DIRECT", from, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.primaryAssignment.organizationUnitId").value(departmentA));

        mockMvc.perform(put("/api/employees/3/organization-membership")
                        .header(HttpHeaders.AUTHORIZATION, managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(membershipBody(1, teamA, positionId, from, "[]",
                                reportingJson(1, "DIRECT", from, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.primaryAssignment.organizationUnitId").value(teamA));

        mockMvc.perform(put("/api/employees/1/organization-membership")
                        .header(HttpHeaders.AUTHORIZATION, managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(membershipBody(2, storeB, positionId, from, "[]",
                                reportingJson(2, "DIRECT", from, null))))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value(520));

        MvcResult treeResult = mockMvc.perform(get("/api/organization-units/tree")
                        .header(HttpHeaders.AUTHORIZATION, managerToken))
                .andExpect(status().isOk()).andReturn();
        String treeJson = treeResult.getResponse().getContentAsString();
        assertFalse(treeJson.contains("MANAGER_B_" + suffix));

        mockMvc.perform(get("/api/organization-units/" + storeB + "/employees")
                        .header(HttpHeaders.AUTHORIZATION, managerToken))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value(520));
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void auditFailureRollsBackOrganizationAndHistory() throws Exception {
        int suffix = Math.abs((int) (System.nanoTime() % 1_000_000));
        String code = "ROLLBACK_" + suffix;
        doThrow(new IllegalStateException("审计故障")).when(operationAuditRecorder)
                .record(any(), anyString(), anyString(), anyString());

        mockMvc.perform(post("/api/organization-units")
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"%s","name":"回滚组织","type":"STORE",
                                 "parentId":1,"leaderEmployeeId":null,"orderNo":1}
                                """.formatted(code)))
                .andExpect(status().isInternalServerError());

        assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_organization_unit WHERE code = ?", Integer.class, code));
        assertEquals(0, jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM t_authorization_history
                WHERE subject_type = 'ORGANIZATION_UNIT' AND after_value LIKE ?
                """, Integer.class, "%" + code + "%"));
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void reportingGraphLockSerializesTransactionsAndSecondCycleIsRejected() throws Exception {
        int suffix = Math.abs((int) (System.nanoTime() % 1_000_000));
        int orgId = createOrganization("CONCURRENT_REPORT_" + suffix);
        int positionId = createPosition("CONCURRENT_POS_" + suffix);
        int firstEmployeeId = jdbcTemplate.queryForObject("SELECT COALESCE(MAX(id), 0) + 1 FROM t_employee", Integer.class);
        int secondEmployeeId = firstEmployeeId + 1;
        int firstUserId = jdbcTemplate.queryForObject("SELECT COALESCE(MAX(id), 0) + 1 FROM t_user", Integer.class);
        int secondUserId = firstUserId + 1;
        TUser admin = (TUser) userService.loadUserByUsername(QUALIFIED_ADMIN_LOGIN);
        CountDownLatch firstWritten = new CountDownLatch(1);
        CountDownLatch allowCommit = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            insertConcurrentEmployee(firstUserId, firstEmployeeId, orgId, positionId, suffix, "A");
            insertConcurrentEmployee(secondUserId, secondEmployeeId, orgId, positionId, suffix, "B");
            Future<?> first = executor.submit(() -> withAdmin(admin, () -> new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                organizationService.updateEmployeeOrganizationMembership(firstEmployeeId,
                        reportingOnlyRequest(0, orgId, positionId, secondEmployeeId));
                firstWritten.countDown();
                await(allowCommit);
            })));
            assertTrue(firstWritten.await(5, TimeUnit.SECONDS));
            Future<CodeEnum> second = executor.submit(() -> withAdminResult(admin, () -> {
                try {
                    new TransactionTemplate(transactionManager).executeWithoutResult(status ->
                            organizationService.updateEmployeeOrganizationMembership(secondEmployeeId,
                                    reportingOnlyRequest(0, orgId, positionId, firstEmployeeId)));
                    return null;
                } catch (BusinessException exception) {
                    return exception.getCodeEnum();
                }
            }));
            Thread.sleep(150);
            assertFalse(second.isDone(), "第二事务应阻塞在 REPORTING_GRAPH 行锁");
            allowCommit.countDown();
            first.get(5, TimeUnit.SECONDS);
            assertEquals(CodeEnum.REPORTING_CYCLE, second.get(5, TimeUnit.SECONDS));
        } finally {
            allowCommit.countDown(); executor.shutdownNow();
            cleanupConcurrentEmployees(firstEmployeeId, secondEmployeeId, firstUserId, secondUserId);
        }
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void organizationGraphLockSerializesTransactionsAndSecondVersionIsRejected() throws Exception {
        int suffix = Math.abs((int) (System.nanoTime() % 1_000_000));
        int organizationId = createOrganization("CONCURRENT_ORG_" + suffix);
        TUser admin = (TUser) userService.loadUserByUsername(QUALIFIED_ADMIN_LOGIN);
        CountDownLatch firstWritten = new CountDownLatch(1); CountDownLatch allowCommit = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> first = executor.submit(() -> withAdmin(admin, () -> new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                organizationService.updateOrganizationUnit(organizationId, organizationUpdate("事务一", 0));
                firstWritten.countDown(); await(allowCommit);
            })));
            assertTrue(firstWritten.await(5, TimeUnit.SECONDS));
            Future<CodeEnum> second = executor.submit(() -> withAdminResult(admin, () -> {
                try { new TransactionTemplate(transactionManager).executeWithoutResult(status ->
                        organizationService.updateOrganizationUnit(organizationId, organizationUpdate("事务二", 0))); return null; }
                catch (BusinessException exception) { return exception.getCodeEnum(); }
            }));
            Thread.sleep(150); assertFalse(second.isDone(), "第二事务应阻塞在 ORGANIZATION_HIERARCHY 行锁");
            allowCommit.countDown(); first.get(5, TimeUnit.SECONDS);
            assertEquals(CodeEnum.ORGANIZATION_VERSION_CONFLICT, second.get(5, TimeUnit.SECONDS));
        } finally { allowCommit.countDown(); executor.shutdownNow(); }
    }

    private UpdateOrganizationUnitRequest organizationUpdate(String name, int version) {
        UpdateOrganizationUnitRequest request=new UpdateOrganizationUnitRequest();request.setExpectedVersion(version);request.setName(name);
        request.setType(OrganizationUnitType.STORE);request.setParentId(1);request.setOrderNo(10);return request;
    }

    private UpdateEmployeeOrganizationRequest reportingOnlyRequest(int version, int orgId, int positionId, int managerId) {
        UpdateEmployeeOrganizationRequest request = new UpdateEmployeeOrganizationRequest(); request.setExpectedVersion(version);
        AssignmentInput primary = new AssignmentInput(); primary.setOrganizationUnitId(orgId); primary.setPositionId(positionId);
        primary.setAssignmentType(AssignmentType.PRIMARY); primary.setEffectiveFrom(OffsetDateTime.now()); request.setPrimaryAssignment(primary);
        request.setAdditionalAssignments(List.of()); ReportingInput reporting = new ReportingInput(); reporting.setManagerEmployeeId(managerId);
        reporting.setRelationType(ReportingType.DIRECT); reporting.setEffectiveFrom(OffsetDateTime.now()); request.setReporting(reporting); request.setReason("并发图锁测试"); return request;
    }

    private void insertConcurrentEmployee(int userId, int employeeId, int organizationId,
                                          int positionId, int suffix, String marker) {
        jdbcTemplate.update("""
                INSERT INTO t_user (id, login_act, login_pwd, name, account_no_expired,
                  credentials_no_expired, account_no_locked, account_enabled, account_type,
                  protected_account, version, auth_version)
                VALUES (?, ?, '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi',
                  ?, 1, 1, 1, 1, 'HUMAN', 0, 0, 0)
                """, userId, "concurrent_" + suffix + "_" + marker, "并发员工" + marker);
        jdbcTemplate.update("""
                INSERT INTO t_employee (id, user_id, employee_no, name, employment_status,
                  profile_completed, version, create_time, create_by)
                VALUES (?, ?, ?, ?, 'ACTIVE', 1, 0, CURRENT_TIMESTAMP, 1)
                """, employeeId, userId, "EMP-CONCURRENT-" + suffix + "-" + marker, "并发员工" + marker);
        jdbcTemplate.update("""
                INSERT INTO t_employee_assignment
                  (employee_id, organization_unit_id, position_id, assignment_type, status,
                   active_primary_marker, effective_from, reason, version, create_time, create_by)
                VALUES (?, ?, ?, 'PRIMARY', 'ACTIVE', 1, CURRENT_TIMESTAMP,
                        '并发图锁测试初始任职', 0, CURRENT_TIMESTAMP, 1)
                """, employeeId, organizationId, positionId);
    }

    private void cleanupConcurrentEmployees(int firstEmployeeId, int secondEmployeeId,
                                            int firstUserId, int secondUserId) {
        jdbcTemplate.update("DELETE FROM t_authorization_history WHERE target_user_id IN (?, ?)", firstUserId, secondUserId);
        jdbcTemplate.update("DELETE FROM t_employee_reporting WHERE subordinate_employee_id IN (?, ?) OR manager_employee_id IN (?, ?)",
                firstEmployeeId, secondEmployeeId, firstEmployeeId, secondEmployeeId);
        jdbcTemplate.update("DELETE FROM t_employee_assignment WHERE employee_id IN (?, ?)", firstEmployeeId, secondEmployeeId);
        jdbcTemplate.update("DELETE FROM t_employee WHERE id IN (?, ?)", firstEmployeeId, secondEmployeeId);
        jdbcTemplate.update("DELETE FROM t_user WHERE id IN (?, ?)", firstUserId, secondUserId);
    }

    private void withAdmin(TUser admin, Runnable action) { withAdminResult(admin, () -> { action.run(); return null; }); }
    private <T> T withAdminResult(TUser admin, Callable<T> action) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(admin, null, admin.getAuthorities()));
        try { return action.call(); } catch (RuntimeException exception) { throw exception; } catch (Exception exception) { throw new IllegalStateException(exception); }
        finally { SecurityContextHolder.clearContext(); }
    }
    private void await(CountDownLatch latch) { try { if(!latch.await(5,TimeUnit.SECONDS)) throw new IllegalStateException("等待超时"); } catch(InterruptedException exception){Thread.currentThread().interrupt();throw new IllegalStateException(exception);} }

    private int createOrganization(String code) throws Exception {
        return createOrganization(code, "STORE", 1);
    }

    private int createOrganization(String code, String type, int parentId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/organization-units")
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"%s","name":"测试组织","type":"%s",
                                 "parentId":%d,"leaderEmployeeId":null,"orderNo":10}
                                """.formatted(code, type, parentId)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200)).andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("data").path("id").asInt();
    }

    private int createPosition(String code) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/positions")
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"%s","name":"测试岗位","description":"集成测试","positionLevel":10}
                                """.formatted(code)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200)).andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return body.path("data").path("id").asInt();
    }

    private String membershipBody(int version, int orgId, int positionId, String from,
                                  String additional, String reporting) {
        return """
                {"expectedVersion":%d,
                 "primaryAssignment":{"organizationUnitId":%d,"positionId":%d,
                   "assignmentType":"PRIMARY","effectiveFrom":"%s"},
                 "additionalAssignments":%s,"reporting":%s,"reason":"组织任职测试"}
                """.formatted(version, orgId, positionId, from, additional, reporting);
    }

    private String reportingJson(int managerId, String type, String from, String to) {
        String effectiveTo = to == null ? "" : ",\"effectiveTo\":\"" + to + "\"";
        return "{\"managerEmployeeId\":" + managerId + ",\"relationType\":\"" + type
                + "\",\"effectiveFrom\":\"" + from + "\"" + effectiveTo + "}";
    }

    private void updateMembership(int employeeId, int version, int orgId, int positionId,
                                  String from, String reporting) throws Exception {
        mockMvc.perform(put("/api/employees/" + employeeId + "/organization-membership")
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(membershipBody(version, orgId, positionId, from, "[]", reporting)))
                .andExpect(status().isOk());
    }

    private void insertThirdHumanEmployee(int orgId, int positionId, int suffix) {
        jdbcTemplate.update("""
                INSERT INTO t_user (id, login_act, login_pwd, name, account_no_expired,
                  credentials_no_expired, account_no_locked, account_enabled, account_type,
                  protected_account, version, auth_version)
                VALUES (4, ?, '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi',
                  '王五', 1, 1, 1, 1, 'HUMAN', 0, 0, 0)
                """, "wangwu_" + suffix);
        jdbcTemplate.update("""
                INSERT INTO t_employee (id, user_id, employee_no, name, employment_status,
                  profile_completed, hire_date, version, create_time, create_by)
                VALUES (3, 4, ?, '王五', 'ACTIVE', 1, CURRENT_DATE, 0, CURRENT_TIMESTAMP, 1)
                """, "EMP-" + suffix);
        jdbcTemplate.update("""
                INSERT INTO t_employee_assignment (employee_id, organization_unit_id, position_id,
                  assignment_type, status, active_primary_marker, effective_from, reason, version,
                  create_time, create_by)
                VALUES (3, ?, ?, 'PRIMARY', 'ACTIVE', 1, CURRENT_TIMESTAMP - INTERVAL '1' SECOND,
                  '测试主要任职', 0, CURRENT_TIMESTAMP, 1)
                """, orgId, positionId);
    }

    private void ensureQualifiedHumanAdministrator() {
        jdbcTemplate.update("""
                MERGE INTO t_position(id,code,name,description,position_level,built_in,enabled,version,create_time,create_by) KEY(id)
                VALUES (?, 'ORGANIZATION_TEST_ADMIN', '组织测试管理员', 'H2 组织测试真实岗位', 100, 0, 1, 0, CURRENT_TIMESTAMP, 1)
                """,QUALIFIED_ADMIN_POSITION_ID);
        jdbcTemplate.update("""
                MERGE INTO t_user (id, login_act, login_pwd, name, account_no_expired,
                  credentials_no_expired, account_no_locked, account_enabled, account_type,
                  protected_account, version, authorization_version, auth_version, account_status,
                  must_change_password, failed_login_count, manual_locked)
                KEY(id)
                VALUES (?, ?, '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi',
                  '组织测试管理员', 1, 1, 1, 1, 'HUMAN', 0, 0, 0, 0, 'ACTIVE', 0, 0, 0)
                """, QUALIFIED_ADMIN_USER_ID, QUALIFIED_ADMIN_LOGIN);
        jdbcTemplate.update("""
                MERGE INTO t_login_identifier(user_id,login_act,status,active_marker,reason,version,create_time,changed_by)
                KEY(login_act) VALUES (?,?,'ACTIVE',1,'组织 H2 测试管理员',0,CURRENT_TIMESTAMP,1)
                """,QUALIFIED_ADMIN_USER_ID,QUALIFIED_ADMIN_LOGIN);
        jdbcTemplate.update("""
                MERGE INTO t_employee(id,user_id,employee_no,name,phone,employment_status,profile_completed,
                  hire_date,version,phone_verified,email_verified,create_time,create_by) KEY(id)
                VALUES (?,?,'EMP-ORG-ADMIN','组织测试管理员','13900009100','ACTIVE',1,CURRENT_DATE,0,1,0,CURRENT_TIMESTAMP,1)
                """,QUALIFIED_ADMIN_EMPLOYEE_ID,QUALIFIED_ADMIN_USER_ID);
        jdbcTemplate.update("""
                INSERT INTO t_employee_assignment(employee_id,organization_unit_id,position_id,assignment_type,
                  status,active_primary_marker,effective_from,reason,version,create_time,create_by)
                SELECT ?,1,?,'PRIMARY','ACTIVE',1,CURRENT_TIMESTAMP,'组织 H2 测试管理员任职',0,CURRENT_TIMESTAMP,1
                WHERE NOT EXISTS(SELECT 1 FROM t_employee_assignment WHERE employee_id=? AND active_primary_marker=1)
                """,QUALIFIED_ADMIN_EMPLOYEE_ID,QUALIFIED_ADMIN_POSITION_ID,QUALIFIED_ADMIN_EMPLOYEE_ID);
        jdbcTemplate.update("""
                INSERT INTO t_user_role (user_id, role_id, granted_by, reason, effective_from)
                SELECT ?, id, 1, '组织 H2 测试管理员角色', CURRENT_TIMESTAMP FROM t_role WHERE role = 'admin'
                AND NOT EXISTS(SELECT 1 FROM t_user_role membership WHERE membership.user_id=?
                  AND membership.role_id=t_role.id AND membership.active_marker=1)
                """, QUALIFIED_ADMIN_USER_ID,QUALIFIED_ADMIN_USER_ID);
    }

    private void insertActingManagerRelation(int subordinateEmployeeId,int managerEmployeeId){
        jdbcTemplate.update("""
                INSERT INTO t_employee_reporting(subordinate_employee_id,manager_employee_id,relation_type,status,
                  effective_from,effective_to,reason,version,create_time,create_by)
                VALUES (?,?,'ACTING','ACTIVE',CURRENT_TIMESTAMP - INTERVAL '1' SECOND,
                  CURRENT_TIMESTAMP + INTERVAL '1' DAY,'仅用于验证 reporting-only 管理边界',0,CURRENT_TIMESTAMP,1)
                """,subordinateEmployeeId,managerEmployeeId);
    }

    private void grantRolePermission(String role, String permission) {
        jdbcTemplate.update("""
                INSERT INTO t_role_permission (role_id, permission_id, delegable, data_scope_code)
                SELECT r.id, p.id, p.delegable, r.default_data_scope
                FROM t_role r CROSS JOIN t_permission p
                WHERE r.role = ? AND p.code = ? AND NOT EXISTS (
                  SELECT 1 FROM t_role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = p.id)
                """, role, permission);
    }
}
