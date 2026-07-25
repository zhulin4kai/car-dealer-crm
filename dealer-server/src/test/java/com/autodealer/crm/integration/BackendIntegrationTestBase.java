package com.autodealer.crm.integration;

import com.autodealer.crm.bootstrap.DealerCRMApplication;
import com.autodealer.crm.bootstrap.security.MyAuthenticationSuccessHandler;
import com.autodealer.crm.bootstrap.security.SecurityConfig;
import com.autodealer.crm.bootstrap.security.TokenVerifyFilter;
import com.autodealer.crm.shared.infrastructure.constants.Constants;
import com.autodealer.crm.shared.infrastructure.cache.RedisKeys;
import com.autodealer.crm.shared.infrastructure.cache.RedisManager;
import com.autodealer.crm.modules.identity.application.api.model.TUser;
import com.autodealer.crm.shared.security.JWTUtils;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Base class for backend integration tests using real H2 database.
 *
 * Provides:
 * - Real Spring Boot context with H2 datasource (test profile)
 * - Real Mapper, Service, Controller, Security, JWT layer
 * - Faked RedisManager so token can be persisted in-memory during the test
 *
 * Login response contract (per MyAuthenticationSuccessHandler):
 *   { "code": 200, "msg": "操作成功", "data": "<jwt-token-string>" }
 */
@SpringBootTest(classes = DealerCRMApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class BackendIntegrationTestBase {

    private static final String ADMIN_LOGIN_ACT = "admin";
    private static final String ADMIN_LOGIN_PWD = "123456";
    protected static final int QUALIFIED_ADMIN_USER_ID = 9000;
    protected static final int QUALIFIED_ADMIN_EMPLOYEE_ID = 9000;
    protected static final int QUALIFIED_ADMIN_POSITION_ID = 9000;
    protected static final String QUALIFIED_ADMIN_LOGIN_ACT = "qualified_test_admin";

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @MockitoBean
    protected RedisManager redisManager;

    private final Map<String, String> tokenStore = new HashMap<>();

    @BeforeEach
    void resetTokenStore() {
        tokenStore.clear();
        // When MyAuthenticationSuccessHandler writes the JWT to Redis,
        // the TokenVerifyFilter later reads it back. Mirror that flow in-memory.
        lenient().doAnswer(invocation -> {
            String key = invocation.getArgument(0);
            Object value = invocation.getArgument(1);
            tokenStore.put(key, String.valueOf(value));
            return Boolean.TRUE;
        }).when(redisManager).set(anyString(), anyString(), anyLong());
        lenient().when(redisManager.addToSet(anyString(),anyString(),anyLong())).thenReturn(true);
        lenient().when(redisManager.removeFromSet(anyString(),anyString())).thenReturn(true);
        lenient().when(redisManager.expire(anyString(),anyLong())).thenReturn(true);

        lenient().when(redisManager.get(anyString())).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            return tokenStore.get(key);
        });
        lenient().when(redisManager.keyPresence(anyString())).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            return tokenStore.containsKey(key)
                    ? RedisManager.KeyPresence.PRESENT : RedisManager.KeyPresence.ABSENT;
        });

        lenient().doAnswer(invocation -> {
            String key = invocation.getArgument(0);
            tokenStore.remove(key);
            return Boolean.TRUE;
        }).when(redisManager).delete(anyString());
    }

    /**
     * Performs a real login against the SecurityConfig + MyAuthenticationSuccessHandler
     * + H2 user table, and returns the JWT string persisted in the fake Redis store.
     */
    protected String loginAsAdmin() throws Exception {
        return loginAs(ADMIN_LOGIN_ACT, ADMIN_LOGIN_PWD, 1);
    }

    /** 用户治理测试使用具备真实员工、任职和受保护全局权限事实的 HUMAN 管理员。 */
    protected String loginAsQualifiedAdmin() throws Exception {
        ensureQualifiedHumanAdmin();
        return loginAs(QUALIFIED_ADMIN_LOGIN_ACT, ADMIN_LOGIN_PWD, QUALIFIED_ADMIN_USER_ID);
    }

    protected void ensureQualifiedHumanAdmin() {
        jdbcTemplate.update("""
                MERGE INTO t_position(id,code,name,description,position_level,built_in,enabled,version,create_time,create_by) KEY(id)
                VALUES (?, 'QUALIFIED_TEST_ADMIN', '合格测试管理员', '用户治理 H2 测试岗位', 100, 0, 1, 0, CURRENT_TIMESTAMP, 1)
                """,QUALIFIED_ADMIN_POSITION_ID);
        jdbcTemplate.update("""
                MERGE INTO t_user(id,login_act,login_pwd,name,account_no_expired,credentials_no_expired,
                  account_no_locked,account_enabled,account_type,protected_account,version,authorization_version,
                  auth_version,session_revision,account_status,must_change_password,failed_login_count,manual_locked)
                KEY(id) VALUES(?,?,'$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi',
                  '合格测试管理员',1,1,1,1,'HUMAN',0,0,0,0,0,'ACTIVE',0,0,0)
                """,QUALIFIED_ADMIN_USER_ID,QUALIFIED_ADMIN_LOGIN_ACT);
        jdbcTemplate.update("""
                MERGE INTO t_login_identifier(user_id,login_act,status,active_marker,reason,version,create_time,changed_by)
                KEY(login_act) VALUES(?,?,'ACTIVE',1,'用户治理 H2 测试管理员',0,CURRENT_TIMESTAMP,1)
                """,QUALIFIED_ADMIN_USER_ID,QUALIFIED_ADMIN_LOGIN_ACT);
        jdbcTemplate.update("""
                MERGE INTO t_employee(id,user_id,employee_no,name,phone,employment_status,profile_completed,
                  hire_date,version,phone_verified,email_verified,create_time,create_by) KEY(id)
                VALUES(?,?,'EMP-QUALIFIED-ADMIN','合格测试管理员','13900009000','ACTIVE',1,CURRENT_DATE,0,1,0,CURRENT_TIMESTAMP,1)
                """,QUALIFIED_ADMIN_EMPLOYEE_ID,QUALIFIED_ADMIN_USER_ID);
        jdbcTemplate.update("""
                INSERT INTO t_employee_assignment(employee_id,organization_unit_id,position_id,assignment_type,
                  status,active_primary_marker,effective_from,reason,version,create_time,create_by)
                SELECT ?,1,?,'PRIMARY','ACTIVE',1,CURRENT_TIMESTAMP,'用户治理 H2 测试管理员任职',0,CURRENT_TIMESTAMP,1
                WHERE NOT EXISTS(SELECT 1 FROM t_employee_assignment WHERE employee_id=? AND active_primary_marker=1)
                """,QUALIFIED_ADMIN_EMPLOYEE_ID,QUALIFIED_ADMIN_POSITION_ID,QUALIFIED_ADMIN_EMPLOYEE_ID);
        jdbcTemplate.update("""
                INSERT INTO t_user_role(user_id,role_id,granted_by,reason,effective_from)
                SELECT ?,role_record.id,1,'用户治理 H2 测试管理员角色',CURRENT_TIMESTAMP
                FROM t_role role_record WHERE role_record.role='admin'
                  AND NOT EXISTS(SELECT 1 FROM t_user_role membership WHERE membership.user_id=?
                    AND membership.role_id=role_record.id AND membership.active_marker=1)
                """,QUALIFIED_ADMIN_USER_ID,QUALIFIED_ADMIN_USER_ID);
    }

    /**
     * 为“最后一个普通管理员”保护测试创建不占用 admin 名额的 HUMAN 安全操作者。
     * 该操作者仍具有真实员工、根组织任职、严格更高授权级别和一项明确的全局治理权限。
     */
    protected int insertQualifiedSecurityOperator(int userId, String loginAct, String... permissionCodes) {
        ensureQualifiedHumanAdmin();
        String roleCode = "test_security_operator_" + userId + "_" + System.nanoTime();
        jdbcTemplate.update("""
                INSERT INTO t_role(role,role_name,description,protected_role,authorization_level,default_data_scope,
                  scope_type,enabled,version) VALUES(?,?,'最后普通管理员保护测试操作者',1,101,'GLOBAL','GLOBAL',1,0)
                """, roleCode, "测试安全操作者" + userId);
        int roleId = jdbcTemplate.queryForObject("SELECT id FROM t_role WHERE role=?", Integer.class, roleCode);
        for (String permissionCode : permissionCodes) {
            jdbcTemplate.update("""
                    INSERT INTO t_role_permission(role_id,permission_id,delegable,data_scope_code)
                    SELECT ?,id,0,'GLOBAL' FROM t_permission WHERE code=?
                    """, roleId, permissionCode);
        }
        jdbcTemplate.update("""
                INSERT INTO t_user(id,login_act,login_pwd,name,account_no_expired,credentials_no_expired,
                  account_no_locked,account_enabled,account_type,protected_account,version,authorization_version,
                  auth_version,session_revision,account_status,must_change_password,failed_login_count,manual_locked)
                VALUES(?,?,'$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi',?,1,1,1,1,
                  'HUMAN',0,0,0,0,0,'ACTIVE',0,0,0)
                """, userId, loginAct, "测试安全操作者" + userId);
        jdbcTemplate.update("""
                INSERT INTO t_login_identifier(user_id,login_act,status,active_marker,reason,version,create_time,changed_by)
                VALUES(?,?,'ACTIVE',1,'最后普通管理员保护测试',0,CURRENT_TIMESTAMP,1)
                """, userId, loginAct);
        jdbcTemplate.update("""
                INSERT INTO t_employee(id,user_id,employee_no,name,phone,employment_status,profile_completed,
                  hire_date,version,phone_verified,email_verified,create_time,create_by)
                VALUES(?,?,?,?,?,'ACTIVE',1,CURRENT_DATE,0,1,0,CURRENT_TIMESTAMP,1)
                """, userId, userId, "EMP-SECURITY-" + userId, "测试安全操作者" + userId,
                "139" + String.format("%08d", userId));
        jdbcTemplate.update("""
                INSERT INTO t_employee_assignment(employee_id,organization_unit_id,position_id,assignment_type,
                  status,active_primary_marker,effective_from,reason,version,create_time,create_by)
                VALUES(?,1,?,'PRIMARY','ACTIVE',1,CURRENT_TIMESTAMP,'最后普通管理员保护测试任职',0,CURRENT_TIMESTAMP,1)
                """, userId, QUALIFIED_ADMIN_POSITION_ID);
        jdbcTemplate.update("""
                INSERT INTO t_user_role(user_id,role_id,granted_by,reason,effective_from,active_marker,version)
                VALUES(?,?,1,'最后普通管理员保护测试角色',CURRENT_TIMESTAMP,1,0)
                """, userId, roleId);
        for (String permissionCode : permissionCodes) {
            jdbcTemplate.update("""
                    INSERT INTO t_user_permission(user_id,permission_id,effect,data_scope_code,effective_from,
                      active_marker,reason,granted_by,version,create_time)
                    SELECT ?,id,'GRANT','GLOBAL',CURRENT_TIMESTAMP,1,'最后普通管理员保护测试权限',1,0,CURRENT_TIMESTAMP
                    FROM t_permission WHERE code=?
                    """, userId, permissionCode);
        }
        return roleId;
    }

    protected String loginAs(String loginAct, String loginPwd, int expectedUserId) throws Exception {
        MvcResult result = mockMvc.perform(post(Constants.LOGIN_URI)
                        .param("loginAct", loginAct)
                        .param("loginPwd", loginPwd)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        String token = body.path("data").asText();
        if (token == null || token.isEmpty()) {
            throw new IllegalStateException("Login response did not include a JWT in $.data: "
                    + result.getResponse().getContentAsString());
        }
        // Make sure the token is in our fake Redis so TokenVerifyFilter can find it.
        tokenStore.put(RedisKeys.userLogin(expectedUserId), token);
        return "Bearer " + token;
    }

    /**
     * Builds a valid bearer token for an arbitrary user without going through /api/login.
     * Useful for permission tests where we need a logged-in user that does NOT have a
     * certain authority.
     */
    protected String buildDirectToken(TUser user) {
        String token = JWTUtils.createJWT(user.getId(), user.getLoginAct(), Constants.DEFAULT_EXPIRE_TIME);
        tokenStore.put(RedisKeys.userLogin(user.getId()), token);
        return "Bearer " + token;
    }
}
