package com.autodealer.crm.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class UserHistoryIntegrationTest extends BackendIntegrationTestBase {
    private static final int HISTORY_ADMIN_ID = 9890;
    private static final int HISTORY_ADMIN_ORG_ID = 9890;
    private static final int HISTORY_ADMIN_POSITION_ID = 9890;
    private static final int HISTORY_NO_AUDIT_ID = 9891;
    private static final int HISTORY_SCOPED_AUDITOR_ID = 9892;
    private static final int HISTORY_OUTSIDE_ORG_ID = 9891;
    private static final int HISTORY_OUTSIDE_POSITION_ID = 9891;

    @BeforeEach
    void cleanHistory() {
        cleanupHistoryAdministrator();
        cleanupQualifiedHistoryViewer(HISTORY_NO_AUDIT_ID);
        cleanupQualifiedHistoryViewer(HISTORY_SCOPED_AUDITOR_ID);
        jdbcTemplate.update("DELETE FROM t_position WHERE id=?", HISTORY_OUTSIDE_POSITION_ID);
        jdbcTemplate.update("DELETE FROM t_organization_unit WHERE id=?", HISTORY_OUTSIDE_ORG_ID);
        jdbcTemplate.update("DELETE FROM t_authorization_history");
        jdbcTemplate.update("DELETE FROM t_user_lifecycle_event WHERE user_id=2");
        jdbcTemplate.update("DELETE FROM t_operation_log WHERE resource_id='2'");
        jdbcTemplate.update("INSERT INTO t_organization_unit(id,code,name,type,parent_id,order_no,migration_placeholder,enabled,version,create_time,create_by) VALUES(?, 'HISTORY_ADMIN_ORG', '历史审计组织', 'DEPARTMENT', 1, 1, 0, 1, 0, CURRENT_TIMESTAMP, 1)", HISTORY_ADMIN_ORG_ID);
        jdbcTemplate.update("INSERT INTO t_position(id,code,name,position_level,built_in,enabled,version,create_time,create_by) VALUES(?, 'HISTORY_ADMIN_POSITION', '历史审计岗位', 100, 0, 1, 0, CURRENT_TIMESTAMP, 1)", HISTORY_ADMIN_POSITION_ID);
        jdbcTemplate.update("INSERT INTO t_user(id,login_act,login_pwd,name,account_no_expired,credentials_no_expired,account_no_locked,account_enabled,account_status,account_type,protected_account,must_change_password,manual_locked,version,authorization_version,auth_version,session_revision,create_time,create_by) VALUES(?, 'history_security_admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '历史安全管理员',1,1,1,1,'ACTIVE','HUMAN',0,0,0,0,0,0,0,CURRENT_TIMESTAMP,1)", HISTORY_ADMIN_ID);
        jdbcTemplate.update("INSERT INTO t_employee(id,user_id,employee_no,name,phone,employment_status,profile_completed,version,profile_version,phone_verified,email_verified,create_time,create_by) VALUES(?,?, 'EMP-HISTORY-ADMIN', '历史安全管理员','13900009010','ACTIVE',1,0,0,1,0,CURRENT_TIMESTAMP,1)", HISTORY_ADMIN_ID, HISTORY_ADMIN_ID);
        jdbcTemplate.update("INSERT INTO t_employee_assignment(employee_id,organization_unit_id,position_id,assignment_type,status,active_primary_marker,effective_from,reason,version,create_time,create_by) VALUES(?,?,?,'PRIMARY','ACTIVE',1,CURRENT_TIMESTAMP,'历史审计测试',0,CURRENT_TIMESTAMP,1)", HISTORY_ADMIN_ID, HISTORY_ADMIN_ORG_ID, HISTORY_ADMIN_POSITION_ID);
        jdbcTemplate.update("INSERT INTO t_user_role(user_id,role_id,granted_by,reason,effective_from,active_marker,version) SELECT ?,id,1,'历史审计测试',CURRENT_TIMESTAMP,1,0 FROM t_role WHERE role='admin'", HISTORY_ADMIN_ID);
        jdbcTemplate.update("INSERT INTO t_organization_unit(id,code,name,type,parent_id,order_no,migration_placeholder,enabled,version,create_time,create_by) VALUES(?, 'HISTORY_OUTSIDE_ORG', '历史范围外组织', 'DEPARTMENT', 1, 1, 0, 1, 0, CURRENT_TIMESTAMP, 1)", HISTORY_OUTSIDE_ORG_ID);
        jdbcTemplate.update("INSERT INTO t_position(id,code,name,position_level,built_in,enabled,version,create_time,create_by) VALUES(?, 'HISTORY_OUTSIDE_POSITION', '历史范围外岗位', 10, 0, 1, 0, CURRENT_TIMESTAMP, 1)", HISTORY_OUTSIDE_POSITION_ID);
        insertQualifiedHistoryViewer(HISTORY_NO_AUDIT_ID, "history_no_audit", "无审计权限员工", "EMP-HISTORY-NO-AUDIT", "13900009011");
        insertQualifiedHistoryViewer(HISTORY_SCOPED_AUDITOR_ID, "history_scoped_auditor", "范围外审计员工", "EMP-HISTORY-SCOPED", "13900009012");
        jdbcTemplate.update("""
                INSERT INTO t_user_permission(user_id,permission_id,effect,data_scope_code,effective_from,active_marker,reason,granted_by,version,create_time)
                SELECT ?,id,'GRANT','SELF',CURRENT_TIMESTAMP,1,'历史范围隔离测试',1,0,CURRENT_TIMESTAMP
                FROM t_permission WHERE code='audit:operation:detail'
                """, HISTORY_SCOPED_AUDITOR_ID);
    }

    @AfterEach void restoreAdminName() {
        jdbcTemplate.update("UPDATE t_user SET name='管理员' WHERE id=1");
        jdbcTemplate.update("UPDATE t_user SET name='张三' WHERE id=2");
        cleanupQualifiedHistoryViewer(HISTORY_NO_AUDIT_ID);
        cleanupQualifiedHistoryViewer(HISTORY_SCOPED_AUDITOR_ID);
        jdbcTemplate.update("DELETE FROM t_position WHERE id=?", HISTORY_OUTSIDE_POSITION_ID);
        jdbcTemplate.update("DELETE FROM t_organization_unit WHERE id=?", HISTORY_OUTSIDE_ORG_ID);
        cleanupHistoryAdministrator();
    }

    @Test
    void mergesStableFactsPaginatesAndSanitizesEveryDisplaySurface() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        String unsafe = "理由 192.168.1.10 owner@example.com 13800138000 eyJabcdefghijk.abcdefghijk.abcdefghijk "
                + "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";
        jdbcTemplate.update("""
                INSERT INTO t_operation_log(user_id,user_name,action_code,module_name,object_type,resource_id,result,detail,ip,request_id,create_time)
                VALUES(1,'事件时管理员','USER_PROFILE_UPDATE','用户管理','USER','2','SUCCESS',?,'10.0.0.8','history-request',?)
                """, "{\"summary\":{\"reason\":\"" + unsafe + "\",\"before\":{\"name\":\"旧名 172.16.0.1\",\"passwordHash\":\"never\",\"credentialDigest\":\"0123456789abcdef0123456789abcdef\",\"nested\":{\"signature\":\"signed-secret\"}},\"after\":{\"name\":\"新名\",\"email\":\"full@example.com\",\"recoveryKey\":\"never-key\"}}}", now.minusSeconds(1));
        jdbcTemplate.update("""
                INSERT INTO t_authorization_history(subject_type,subject_id,change_type,target_user_id,role_id,before_value,after_value,reason,operator_id,occurred_time,request_id)
                VALUES('USER_ROLE','2:2','ASSIGN',2,2,NULL,'{"roleId":2,"roleCode":"sales_consultant","roleName":"销售顾问"}',?,1,?,'history-request')
                """, unsafe, now);
        jdbcTemplate.update("UPDATE t_user SET name='改名后的管理员' WHERE id=1");

        String token = historyAdministratorToken();
        MvcResult response = mockMvc.perform(get("/api/users/2/history")
                        .header(HttpHeaders.AUTHORIZATION, token).param("page", "1").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.size").value(1))
                .andExpect(jsonPath("$.data.pages").value(2))
                .andExpect(jsonPath("$.data.list[0].sourceKey").value("AUTHORIZATION_HISTORY"))
                .andExpect(jsonPath("$.data.allowedActions[0]").value("VIEW"))
                .andReturn();
        MvcResult secondPage = mockMvc.perform(get("/api/users/2/history")
                        .header(HttpHeaders.AUTHORIZATION, token).param("page", "2").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.list[0].sourceKey").value("OPERATION_LOG"))
                .andReturn();
        String body = response.getResponse().getContentAsString(StandardCharsets.UTF_8)
                + secondPage.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertTrue(body.contains("事件时管理员"), body);
        assertFalse(body.contains("改名后的管理员"));
        for (String secret : new String[]{"192.168.1.10", "172.16.0.1", "10.0.0.8", "owner@example.com",
                "full@example.com", "13800138000", "eyJabcdefghijk", "0123456789abcdef", "passwordHash",
                "credentialDigest", "signature", "signed-secret", "recoveryKey", "never-key", "never", "detail"}) {
            assertFalse(body.contains(secret), secret);
        }
    }

    @Test
    void enforcesAuditPermissionManageScopeAndStrictQueryValidation() throws Exception {
        String admin = historyAdministratorToken();
        mockMvc.perform(get("/api/users/1/history").header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/users/999999/history").header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/users/2/history").header(HttpHeaders.AUTHORIZATION, admin).param("page", "0"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/users/2/history").header(HttpHeaders.AUTHORIZATION, admin).param("size", "101"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/users/2/history").header(HttpHeaders.AUTHORIZATION, admin).param("actionCode", "UNKNOWN"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/users/2/history").header(HttpHeaders.AUTHORIZATION, admin).param("actionCode", "POSITION_DENIED"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/users/2/history").header(HttpHeaders.AUTHORIZATION, admin)
                        .param("startTime", "2026-07-12T10:00:00+08:00").param("endTime", "2026-07-11T10:00:00+08:00"))
                .andExpect(status().isBadRequest());

        String noAuditPermission = loginAs("history_no_audit", "123456", HISTORY_NO_AUDIT_ID);
        mockMvc.perform(get("/api/users/3/history").header(HttpHeaders.AUTHORIZATION, noAuditPermission))
                .andExpect(status().isForbidden());

        String outsideManagementScope = loginAs("history_scoped_auditor", "123456", HISTORY_SCOPED_AUDITOR_ID);
        mockMvc.perform(get("/api/users/2/history").header(HttpHeaders.AUTHORIZATION, outsideManagementScope))
                .andExpect(status().isForbidden());
    }

    @Test
    void actionAndTimeFiltersUseCorrectTotalBeforeStablePaging() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        jdbcTemplate.update("""
                INSERT INTO t_operation_log(user_id,user_name,action_code,module_name,object_type,resource_id,result,detail,request_id,create_time)
                VALUES(1,'管理员','USER_STATUS_CHANGE','用户管理','USER','2','SUCCESS','{"summary":{"reason":"状态调整"}}','filter-1',?)
                """, now.minusMinutes(2));
        jdbcTemplate.update("""
                INSERT INTO t_operation_log(user_id,user_name,action_code,module_name,object_type,resource_id,result,detail,request_id,create_time)
                VALUES(1,'管理员','USER_PROFILE_UPDATE','用户管理','USER','2','SUCCESS','{"summary":{"reason":"资料调整"}}','filter-2',?)
                """, now.minusMinutes(1));
        String token = historyAdministratorToken();
        MvcResult result = mockMvc.perform(get("/api/users/2/history").header(HttpHeaders.AUTHORIZATION, token)
                        .param("actionCode", "USER_STATUS_CHANGE").param("page", "1").param("size", "10"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].actionCode").value("USER_STATUS_CHANGE")).andReturn();
        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
        assertTrue(data.path("actionOptions").size() >= 2);
    }

    @Test
    void lifecycleEventsAppearOnceWithEventTimeOperatorAndSafeBeforeAfterProjection() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        jdbcTemplate.update("""
                INSERT INTO t_operation_log(user_id,user_name,action_code,module_name,object_type,resource_id,result,detail,request_id,create_time)
                VALUES(1,'事件时生命周期管理员','USER_TRANSFER','用户管理','USER','2','SUCCESS','{"summary":{"operationId":"lifecycle-op"}}','lifecycle-request',?)
                """, now);
        jdbcTemplate.update("""
                INSERT INTO t_user_lifecycle_event(operation_id,request_id,action,user_id,employee_id,before_value,after_value,reason,operator_id,occurred_time)
                VALUES('lifecycle-op','lifecycle-request','TRANSFER',2,1,
                  '{"organizationCode":"OLD","passwordHash":"never","ip":"10.0.0.1"}',
                  '{"organizationCode":"NEW","managerName":"新经理","email":"full@example.com"}',
                  '正常调岗',1,?)
                """, now);

        MvcResult result = mockMvc.perform(get("/api/users/2/history").header(HttpHeaders.AUTHORIZATION, historyAdministratorToken())
                        .param("actionCode", "USER_TRANSFER").param("page", "1").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].sourceKey").value("USER_LIFECYCLE_EVENT"))
                .andExpect(jsonPath("$.data.list[0].actionCode").value("USER_TRANSFER"))
                .andExpect(jsonPath("$.data.list[0].operator.name").value("事件时生命周期管理员"))
                .andExpect(jsonPath("$.data.list[0].reason").value("正常调岗"))
                .andReturn();
        String body=result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertTrue(body.contains("OLD"));assertTrue(body.contains("NEW"));assertTrue(body.contains("新经理"));
        for(String secret:new String[]{"passwordHash","never","10.0.0.1","full@example.com"})assertFalse(body.contains(secret),secret);
    }

    @Test
    void includesRegisteredSecurityEventsButExcludesDigestScopedAndRecoveryOnlyEvents() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        String[] included = {"USER_CREATE", "USER_UPDATE", "USER_STATUS_CHANGE", "USER_MANUAL_LOCK_CHANGE",
                "USER_LOGIN_ACCOUNT_CHANGE", "USER_SECURITY_EXPIRATION_CHANGE", "USER_PROFILE_CHANGE",
                "USER_PROFILE_UPDATE", "USER_PASSWORD_CHANGE", "USER_PASSWORD_RESET_ISSUE", "USER_ACTIVATION",
                "USER_SESSION_REVOKE", "USER_SESSION_SECURITY_REVOKE", "USER_SESSION_CREATE",
                "USER_INVITATION_ISSUE", "USER_CREDENTIAL_ISSUE", "USER_CREDENTIAL_CONSUME",
                "USER_CREDENTIAL_DELIVERY_SUCCESS", "USER_CREDENTIAL_DELIVERY_FAILURE",
                "USER_DEGRADED_ADMIN_RECOVERY", "USER_LOGIN_AUTO_LOCK", "USER_LOGIN_AUTO_LOCK_BYPASSED",
                "USER_CONTACT_VERIFICATION_ISSUE", "USER_CONTACT_VERIFICATION_COMPLETE", "USER_HANDOVER"};
        for (int index = 0; index < included.length; index++) {
            jdbcTemplate.update("""
                    INSERT INTO t_operation_log(user_id,user_name,action_code,module_name,object_type,resource_id,result,detail,request_id,create_time)
                    VALUES(1,'安全审计员',?,'账号安全','USER','2','SUCCESS','{"summary":{"reason":"安全事件"}}',?,?)
                    """, included[index], "security-history-" + index, now.plusSeconds(index));
        }
        String[] excluded = {"USER_CREDENTIAL_RATE_LIMIT", "USER_CREDENTIAL_ATTEMPT_REJECTED",
                "USER_RECOVERY_KEY_REJECTED", "USER_MANAGEMENT_GATE_REJECTED",
                "USER_RECOVERY_BREAK_GLASS_ISSUE", "USER_RECOVERY_BREAK_GLASS_COMPLETE"};
        for (String action : excluded) {
            jdbcTemplate.update("""
                    INSERT INTO t_operation_log(user_id,user_name,action_code,module_name,object_type,resource_id,result,detail,request_id,create_time)
                    VALUES(NULL,'系统',?,'账号安全','USER','2','REJECTED','{"summary":{"reason":"不应进入普通历史"}}',?,?)
                    """, action, "excluded-history-" + action, now);
        }

        MvcResult response = mockMvc.perform(get("/api/users/2/history")
                        .header(HttpHeaders.AUTHORIZATION, historyAdministratorToken())
                        .param("page", "1").param("size", "100"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.total").value(included.length)).andReturn();
        JsonNode list = objectMapper.readTree(response.getResponse().getContentAsString()).path("data").path("list");
        java.util.Map<String, JsonNode> byCode = new java.util.HashMap<>();
        list.forEach(item -> byCode.put(item.path("actionCode").asText(), item));
        for (String action : included) assertTrue(byCode.containsKey(action), action);
        assertEquals("SECURITY", byCode.get("USER_SESSION_CREATE").path("categoryCode").asText());
        assertEquals("SECURITY", byCode.get("USER_CONTACT_VERIFICATION_COMPLETE").path("categoryCode").asText());
        assertEquals("ORGANIZATION", byCode.get("USER_HANDOVER").path("categoryCode").asText());
        for (String action : excluded) assertFalse(byCode.containsKey(action), action);
    }

    @Test
    void sanitizesOperatorTargetReasonAndBatchDisplaySurfaces() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        jdbcTemplate.update("UPDATE t_user SET name='目标用户 target@example.com 13800138000' WHERE id=2");
        jdbcTemplate.update("""
                INSERT INTO t_operation_log(user_id,user_name,action_code,module_name,object_type,resource_id,result,detail,request_id,create_time)
                VALUES(1,'operator@example.com 13900139000','USER_STATUS_CHANGE','用户管理','USER','2','SUCCESS',?, 'history-sensitive-surfaces',?)
                """, "{\"summary\":{\"reason\":\"reason@example.com 13700137000\",\"batchId\":\"batch-secret@example.com\",\"totalCount\":1,\"successCount\":1,\"failureCount\":0,\"targetResultCode\":\"0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef\",\"targetResultName\":\"target@example.com\"}}", now);

        String body = mockMvc.perform(get("/api/users/2/history")
                        .header(HttpHeaders.AUTHORIZATION, historyAdministratorToken())
                        .param("actionCode", "USER_STATUS_CHANGE").param("page", "1").param("size", "10"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.total").value(1))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        for (String secret : new String[]{"operator@example.com", "13900139000", "target@example.com",
                "13800138000", "reason@example.com", "13700137000", "batch-secret@example.com",
                "0123456789abcdef"}) {
            assertFalse(body.contains(secret), secret);
        }
    }

    private String historyAdministratorToken() throws Exception {
        return loginAs("history_security_admin", "123456", HISTORY_ADMIN_ID);
    }

    private void cleanupHistoryAdministrator() {
        jdbcTemplate.update("DELETE FROM t_user_session WHERE user_id=?", HISTORY_ADMIN_ID);
        jdbcTemplate.update("DELETE FROM t_operation_log WHERE user_id=? OR resource_id=?", HISTORY_ADMIN_ID, String.valueOf(HISTORY_ADMIN_ID));
        jdbcTemplate.update("DELETE FROM t_user_role WHERE user_id=?", HISTORY_ADMIN_ID);
        jdbcTemplate.update("DELETE FROM t_employee_assignment WHERE employee_id=?", HISTORY_ADMIN_ID);
        jdbcTemplate.update("DELETE FROM t_employee WHERE id=?", HISTORY_ADMIN_ID);
        jdbcTemplate.update("DELETE FROM t_login_identifier WHERE user_id=?", HISTORY_ADMIN_ID);
        jdbcTemplate.update("DELETE FROM t_user WHERE id=?", HISTORY_ADMIN_ID);
        jdbcTemplate.update("DELETE FROM t_position WHERE id=?", HISTORY_ADMIN_POSITION_ID);
        jdbcTemplate.update("DELETE FROM t_organization_unit WHERE id=?", HISTORY_ADMIN_ORG_ID);
    }

    private void insertQualifiedHistoryViewer(int id, String loginAct, String name, String employeeNo, String phone) {
        jdbcTemplate.update("INSERT INTO t_user(id,login_act,login_pwd,name,account_no_expired,credentials_no_expired,account_no_locked,account_enabled,account_status,account_type,protected_account,must_change_password,manual_locked,version,authorization_version,auth_version,session_revision,create_time,create_by) VALUES(?,?, '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi',?,1,1,1,1,'ACTIVE','HUMAN',0,0,0,0,0,0,0,CURRENT_TIMESTAMP,1)", id, loginAct, name);
        jdbcTemplate.update("INSERT INTO t_employee(id,user_id,employee_no,name,phone,employment_status,profile_completed,version,profile_version,phone_verified,email_verified,create_time,create_by) VALUES(?,?,?,?,?,'ACTIVE',1,0,0,1,0,CURRENT_TIMESTAMP,1)", id, id, employeeNo, name, phone);
        jdbcTemplate.update("INSERT INTO t_employee_assignment(employee_id,organization_unit_id,position_id,assignment_type,status,active_primary_marker,effective_from,reason,version,create_time,create_by) VALUES(?,?,?,'PRIMARY','ACTIVE',1,CURRENT_TIMESTAMP,'历史权限边界测试',0,CURRENT_TIMESTAMP,1)", id, HISTORY_OUTSIDE_ORG_ID, HISTORY_OUTSIDE_POSITION_ID);
    }

    private void cleanupQualifiedHistoryViewer(int id) {
        jdbcTemplate.update("DELETE FROM t_user_session WHERE user_id=?", id);
        jdbcTemplate.update("DELETE FROM t_operation_log WHERE user_id=? OR resource_id=?", id, String.valueOf(id));
        jdbcTemplate.update("DELETE FROM t_user_permission WHERE user_id=?", id);
        jdbcTemplate.update("DELETE FROM t_user_role WHERE user_id=?", id);
        jdbcTemplate.update("DELETE FROM t_employee_assignment WHERE employee_id=?", id);
        jdbcTemplate.update("DELETE FROM t_employee WHERE id=?", id);
        jdbcTemplate.update("DELETE FROM t_user WHERE id=?", id);
    }
}
