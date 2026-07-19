package com.autodealer.crm.integration;

import com.autodealer.crm.modules.identity.application.api.dto.user.ManagedUserDtos.LoginAccountRequest;
import com.autodealer.crm.shared.error.BusinessException;
import com.autodealer.crm.modules.identity.application.api.model.TUser;
import com.autodealer.crm.modules.identity.application.api.ManagedUserAccountService;
import com.autodealer.crm.modules.identity.application.api.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 登录身份与账号安全到期命令的跨层回归。 */
class ManagedUserAccountSecurityIntegrationTest extends BackendIntegrationTestBase {
    @Autowired private ManagedUserAccountService accountService;
    @Autowired private UserService userService;

    @Test
    @Transactional
    @DisplayName("登录账号改名校验当前唯一性与CAS并审计且使旧会话失效")
    void loginAccountChangeUsesUniquenessCasAuditAndSecurityInvalidation() throws Exception {
        String targetToken = loginAs("zhangsan", "123456", 2);
        String adminToken = loginAsQualifiedAdmin();
        Integer version = jdbcTemplate.queryForObject("SELECT version FROM t_user WHERE id=2", Integer.class);
        Long authVersion = jdbcTemplate.queryForObject("SELECT auth_version FROM t_user WHERE id=2", Long.class);

        mockMvc.perform(put("/api/users/2/login-account").header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountVersion\":" + version + ",\"loginAct\":\"sales.renamed\",\"reason\":\"账号规范化\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.loginAct").value("sales.renamed"))
                .andExpect(jsonPath("$.data.accountVersion").value(version + 1));

        assertEquals("sales.renamed", jdbcTemplate.queryForObject("SELECT login_act FROM t_user WHERE id=2", String.class));
        assertEquals("RETIRED", jdbcTemplate.queryForObject("SELECT status FROM t_login_identifier WHERE login_act='zhangsan'", String.class));
        assertEquals("ACTIVE", jdbcTemplate.queryForObject("SELECT status FROM t_login_identifier WHERE login_act='sales.renamed'", String.class));
        assertTrue(jdbcTemplate.queryForObject("SELECT auth_version FROM t_user WHERE id=2", Long.class) > authVersion);
        assertEquals(1, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM t_operation_log WHERE resource_id='2' AND action_code='USER_LOGIN_ACCOUNT_CHANGE'", Integer.class));
        mockMvc.perform(get("/api/login/info").header(HttpHeaders.AUTHORIZATION, targetToken))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/login").contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("loginAct", "zhangsan").param("loginPwd", "123456"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(put("/api/users/2/login-account").header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountVersion\":" + (version + 1) + ",\"loginAct\":\"LiSi\",\"reason\":\"重复账号\"}"))
                .andExpect(status().isConflict());
        mockMvc.perform(put("/api/users/3/login-account").header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountVersion\":0,\"loginAct\":\"zhangsan\",\"reason\":\"尝试复用他人历史账号\"}"))
                .andExpect(status().isConflict());
        mockMvc.perform(put("/api/users/2/login-account").header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountVersion\":" + (version + 1) + ",\"loginAct\":\"zhangsan\",\"reason\":\"本人恢复历史账号\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accountVersion").value(version + 2));
        assertEquals("ACTIVE", jdbcTemplate.queryForObject("SELECT status FROM t_login_identifier WHERE login_act='zhangsan'", String.class));
        assertEquals("RETIRED", jdbcTemplate.queryForObject("SELECT status FROM t_login_identifier WHERE login_act='sales.renamed'", String.class));
        mockMvc.perform(put("/api/users/2/login-account").header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountVersion\":" + version + ",\"loginAct\":\"another.login\",\"reason\":\"过期页面\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(627));
    }

    @Test
    @Transactional
    @DisplayName("账号到期状态和凭证到期时间独立设置清除并使用CAS与审计")
    void securityExpirationCommandSetsAndClearsExistingSecurityFacts() throws Exception {
        String targetToken = loginAs("zhangsan", "123456", 2);
        String adminToken = loginAsQualifiedAdmin();
        Integer version = jdbcTemplate.queryForObject("SELECT version FROM t_user WHERE id=2", Integer.class);
        String past = LocalDateTime.now().minusMinutes(5).withNano(0)
                .atZone(ZoneId.systemDefault()).toOffsetDateTime().toString();

        mockMvc.perform(put("/api/users/2/security-expiration").header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountVersion\":" + version + ",\"accountExpiresAt\":\"" + past + "\",\"credentialExpiresAt\":\"" + past + "\",\"reason\":\"安全处置\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accountExpired").value(true))
                .andExpect(jsonPath("$.data.credentialExpired").value(true))
                .andExpect(jsonPath("$.data.accountVersion").value(version + 1));

        assertEquals(0, jdbcTemplate.queryForObject("SELECT account_no_expired FROM t_user WHERE id=2", Integer.class));
        assertEquals(0, jdbcTemplate.queryForObject("SELECT credentials_no_expired FROM t_user WHERE id=2", Integer.class));
        mockMvc.perform(get("/api/login/info").header(HttpHeaders.AUTHORIZATION, targetToken))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/login").contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("loginAct", "zhangsan").param("loginPwd", "123456"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(put("/api/users/2/security-expiration").header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountVersion\":" + (version + 1) + ",\"accountExpiresAt\":null,\"credentialExpiresAt\":null,\"reason\":\"解除安全处置\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accountExpired").value(false))
                .andExpect(jsonPath("$.data.credentialExpired").value(false))
                .andExpect(jsonPath("$.data.credentialExpiresAt").isEmpty());
        assertEquals(1, jdbcTemplate.queryForObject("SELECT account_no_expired FROM t_user WHERE id=2", Integer.class));
        assertEquals(1, jdbcTemplate.queryForObject("SELECT credentials_no_expired FROM t_user WHERE id=2", Integer.class));
        assertEquals(2, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM t_operation_log WHERE resource_id='2' AND action_code='USER_SECURITY_EXPIRATION_CHANGE'", Integer.class));
    }

    @Test
    @Transactional
    @DisplayName("本人不能使用身份或安全到期命令、数据库拒绝第二恢复账号且未知字段被拒绝")
    void identityAndSecurityCommandsRejectSelfProtectedAndUnknownFields() throws Exception {
        String adminToken = loginAsAdmin();
        Integer version = jdbcTemplate.queryForObject("SELECT version FROM t_user WHERE id=1", Integer.class);
        assertThrows(DataIntegrityViolationException.class,()->jdbcTemplate.update("INSERT INTO t_user(id,login_act,login_pwd,name,account_no_expired,credentials_no_expired,account_no_locked,account_enabled,account_type,protected_account,version,authorization_version,auth_version,session_revision,account_status,must_change_password,failed_login_count,manual_locked,create_time,create_by) VALUES(9919,'protected_recovery_9919','x','另一个恢复账号',1,1,1,1,'SYSTEM',1,0,0,0,0,'ACTIVE',0,0,0,CURRENT_TIMESTAMP,1)"));
        mockMvc.perform(put("/api/users/1/login-account").header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountVersion\":" + version + ",\"loginAct\":\"self.change\",\"reason\":\"不应允许\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/users/1/security-expiration").header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountVersion\":" + version + ",\"accountExpiresAt\":\"2026-01-01T00:00:00+08:00\",\"credentialExpiresAt\":null,\"reason\":\"不应允许\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/users/2/security-expiration").header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountVersion\":0,\"accountExpired\":true,\"reason\":\"严格白名单\",\"operatorId\":1}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Transactional
    @DisplayName("账号或凭证到期不能移除最后一个可用普通管理员入口")
    void securityExpirationProtectsLastAvailableOrdinaryAdministrator() throws Exception {
        int operatorId = 9917;
        insertQualifiedSecurityOperator(operatorId, "security_expiration_operator", "user:status");
        String adminToken = loginAs("security_expiration_operator", "123456", operatorId);
        jdbcTemplate.update("UPDATE t_user SET account_enabled=0,account_status='DISABLED' WHERE id=?", QUALIFIED_ADMIN_USER_ID);
        int userId = 9918;
        jdbcTemplate.update("INSERT INTO t_organization_unit(code,name,type,parent_id,placeholder,enabled,version,create_time,create_by) VALUES('SEC_ADMIN_ORG','安全管理门店','STORE',1,0,1,0,CURRENT_TIMESTAMP,1)");
        int organizationId = jdbcTemplate.queryForObject("SELECT id FROM t_organization_unit WHERE code='SEC_ADMIN_ORG'", Integer.class);
        jdbcTemplate.update("INSERT INTO t_position(code,name,position_level,built_in,enabled,version,create_time,create_by) VALUES('SEC_ADMIN_POS','安全管理员',99,0,1,0,CURRENT_TIMESTAMP,1)");
        int positionId = jdbcTemplate.queryForObject("SELECT id FROM t_position WHERE code='SEC_ADMIN_POS'", Integer.class);
        jdbcTemplate.update("INSERT INTO t_user(id,login_act,login_pwd,name,account_no_expired,credentials_no_expired,account_no_locked,account_enabled,account_type,protected_account,version,authorization_version,auth_version,session_revision,account_status,must_change_password,failed_login_count,manual_locked,create_time,create_by) VALUES(?,?,?,'普通管理员',1,1,1,1,'HUMAN',0,0,0,0,0,'ACTIVE',0,0,0,CURRENT_TIMESTAMP,1)", userId, "ordinary_admin_9918", "x");
        jdbcTemplate.update("INSERT INTO t_employee(user_id,employee_no,name,phone,employment_status,profile_completed,version,profile_version,phone_verified,email_verified,create_time,create_by) VALUES(?,?,'普通管理员','13900009918','ACTIVE',1,0,0,1,0,CURRENT_TIMESTAMP,1)", userId, "SEC-ADMIN-9918");
        int employeeId = jdbcTemplate.queryForObject("SELECT id FROM t_employee WHERE user_id=?", Integer.class, userId);
        jdbcTemplate.update("INSERT INTO t_employee_assignment(employee_id,organization_unit_id,position_id,assignment_type,status,active_primary_marker,effective_from,reason,version,create_time,create_by) VALUES(?,?,?,'PRIMARY','ACTIVE',1,CURRENT_TIMESTAMP,'安全管理员入口',0,CURRENT_TIMESTAMP,1)", employeeId, organizationId, positionId);
        int adminRoleId = jdbcTemplate.queryForObject("SELECT id FROM t_role WHERE role='admin'", Integer.class);
        jdbcTemplate.update("INSERT INTO t_user_role(user_id,role_id,granted_by,reason,effective_from,active_marker,version) VALUES(?,?,1,'最后普通管理员测试',CURRENT_TIMESTAMP,1,0)", userId, adminRoleId);

        mockMvc.perform(put("/api/users/" + userId + "/security-expiration")
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountVersion\":0,\"accountExpiresAt\":\"2026-01-01T00:00:00+08:00\",\"credentialExpiresAt\":null,\"reason\":\"不应允许\"}"))
                .andExpect(status().isForbidden());
        assertEquals(1, jdbcTemplate.queryForObject("SELECT account_no_expired FROM t_user WHERE id=?", Integer.class, userId));
        assertEquals(1, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM t_user WHERE id=? AND account_enabled=1", Integer.class, userId));
    }

    @Test
    @Transactional
    @DisplayName("受管资料命令不能替换最后一个可恢复管理员的唯一已验证联系方式")
    void managedProfileCannotReplaceLastRecoverableAdminContact() throws Exception {
        int operatorId=9916;
        insertQualifiedSecurityOperator(operatorId,"managed_profile_sec","user:permission","user:edit");
        String operatorToken=loginAs("managed_profile_sec","123456",operatorId);
        Integer profileVersion=jdbcTemplate.queryForObject("SELECT profile_version FROM t_employee WHERE user_id=?",Integer.class,QUALIFIED_ADMIN_USER_ID);
        String originalPhone=jdbcTemplate.queryForObject("SELECT phone FROM t_employee WHERE user_id=?",String.class,QUALIFIED_ADMIN_USER_ID);

        mockMvc.perform(put("/api/users/"+QUALIFIED_ADMIN_USER_ID+"/profile")
                        .header(HttpHeaders.AUTHORIZATION,operatorToken).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"profileVersion\":"+profileVersion+",\"name\":\"合格测试管理员\",\"phone\":\"13900009001\",\"email\":null}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value(605));

        assertEquals(originalPhone,jdbcTemplate.queryForObject("SELECT phone FROM t_employee WHERE user_id=?",String.class,QUALIFIED_ADMIN_USER_ID));
    }

    @Test
    @DisplayName("并发抢占同一新登录标识时全局标识锁保证仅一人成功")
    void concurrentLoginIdentifierClaimHasOneWinnerAndStableDuplicate() throws Exception {
        int firstUserId = 9920;
        int secondUserId = 9921;
        insertBareAccount(firstUserId, "identity_race_old_1");
        insertBareAccount(secondUserId, "identity_race_old_2");
        ensureQualifiedHumanAdmin();
        TUser operator = userService.getLoginUserById(QUALIFIED_ADMIN_USER_ID);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<String> first = executor.submit(() -> raceRename(operator, firstUserId, ready, start));
            Future<String> second = executor.submit(() -> raceRename(operator, secondUserId, ready, start));
            ready.await();
            start.countDown();
            List<String> results = List.of(first.get(), second.get());
            assertEquals(1, results.stream().filter("SUCCESS"::equals).count(), results.toString());
            assertEquals(1, results.stream().filter("DUPLICATE"::equals).count(), results.toString());
            assertEquals(1, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM t_login_identifier WHERE login_act='identity.race.winner' AND status='ACTIVE'", Integer.class));
        } finally {
            executor.shutdownNow();
            jdbcTemplate.update("DELETE FROM t_operation_log WHERE resource_id IN (?,?)", String.valueOf(firstUserId), String.valueOf(secondUserId));
            jdbcTemplate.update("DELETE FROM t_login_identifier WHERE user_id IN (?,?)", firstUserId, secondUserId);
            jdbcTemplate.update("DELETE FROM t_user WHERE id IN (?,?)", firstUserId, secondUserId);
        }
    }

    private void insertBareAccount(int userId, String loginAct) {
        jdbcTemplate.update("INSERT INTO t_user(id,login_act,login_pwd,name,account_no_expired,credentials_no_expired,account_no_locked,account_enabled,account_type,protected_account,version,authorization_version,auth_version,session_revision,account_status,must_change_password,failed_login_count,manual_locked,create_time,create_by) VALUES(?,?,?,'登录标识并发用户',1,1,1,1,'HUMAN',0,0,0,0,0,'ACTIVE',0,0,0,CURRENT_TIMESTAMP,1)", userId, loginAct, "x");
        jdbcTemplate.update("INSERT INTO t_login_identifier(user_id,login_act,status,active_marker,changed_by,reason,version,create_time) VALUES(?,?,'ACTIVE',1,1,'并发测试初始化',0,CURRENT_TIMESTAMP)", userId, loginAct);
    }

    private String raceRename(TUser operator, int userId, CountDownLatch ready, CountDownLatch start) {
        try {
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(operator, null, operator.getAuthorities()));
            ready.countDown();
            start.await();
            LoginAccountRequest request = new LoginAccountRequest();
            request.setAccountVersion(0);
            request.setLoginAct("identity.race.winner");
            request.setReason("并发标识抢占测试");
            accountService.changeLoginAccount(userId, request);
            return "SUCCESS";
        } catch (BusinessException exception) {
            return exception.getCodeEnum().name();
        } catch (Exception exception) {
            return exception.getClass().getSimpleName();
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
