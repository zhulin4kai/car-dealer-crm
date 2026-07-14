package com.autodealer.crm.integration;

import com.autodealer.crm.bootstrap.DealerCRMApplication;
import com.autodealer.crm.modules.identity.application.api.AuthorizationAuditRecorder;
import com.autodealer.crm.modules.identity.application.api.security.CurrentUserProvider;
import com.autodealer.crm.modules.identity.application.api.dto.access.UserAuthorizationDtos.UpdateRolesRequest;
import com.autodealer.crm.modules.identity.application.api.enums.DataScopeCode;
import com.autodealer.crm.shared.error.BusinessException;
import com.autodealer.crm.modules.identity.persistence.mapper.TUserMapper;
import com.autodealer.crm.shared.error.CodeEnum;
import com.autodealer.crm.modules.identity.application.api.AuthorizationService;
import com.autodealer.crm.modules.identity.application.api.UserSessionService;
import com.autodealer.crm.modules.identity.application.internal.UserAuthorizationPolicy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = DealerCRMApplication.class)
@ActiveProfiles("test")
class AvailableAdminAuthorizationConcurrencyTest {
    private static final int FIRST_USER_ID = 9931;
    private static final int SECOND_USER_ID = 9932;
    private static final int ORGANIZATION_ID = 9930;
    private static final int POSITION_ID = 9930;

    @Autowired JdbcTemplate jdbc;
    @Autowired TUserMapper userMapper;
    @Autowired AuthorizationService authorizationService;
    @MockBean CurrentUserProvider currentUserProvider;
    @MockBean UserAuthorizationPolicy policy;
    @MockBean AuthorizationAuditRecorder auditRecorder;
    @MockBean UserSessionService userSessionService;

    @BeforeEach
    void seed() {
        cleanup();
        when(currentUserProvider.getCurrentUserId()).thenReturn(1);
        when(policy.canManage(any())).thenReturn(true);
        when(policy.canRevokeRole(any(),any())).thenReturn(true);
        when(policy.roleCandidates(any())).thenReturn(List.of());
        when(policy.delegableScopes(any(), any())).thenReturn(List.<DataScopeCode>of());

        jdbc.update("INSERT INTO t_organization_unit(id,code,name,type,parent_id,order_no,migration_placeholder,enabled,version,create_time,create_by) VALUES(?, 'ADMIN_GUARD_ORG', '管理员保护组织', 'DEPARTMENT', 1, 1, 0, 1, 0, CURRENT_TIMESTAMP, 1)", ORGANIZATION_ID);
        jdbc.update("INSERT INTO t_position(id,code,name,position_level,built_in,enabled,version,create_time,create_by) VALUES(?, 'ADMIN_GUARD_POSITION', '管理员保护岗位', 100, 0, 1, 0, CURRENT_TIMESTAMP, 1)", POSITION_ID);
        insertAdmin(FIRST_USER_ID);
        insertAdmin(SECOND_USER_ID);
        assertEquals(2, userMapper.countAdminUsers());
    }

    @AfterEach
    void clean() {
        cleanup();
    }

    @Test
    void twoConcurrentAdminRoleRemovalsAllowOnlyOneCommit() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<CodeEnum> first = executor.submit(() -> removeAdminRole(FIRST_USER_ID, ready, start));
            Future<CodeEnum> second = executor.submit(() -> removeAdminRole(SECOND_USER_ID, ready, start));
            assertTrue(ready.await(3, TimeUnit.SECONDS));
            start.countDown();

            List<CodeEnum> outcomes = List.of(
                    first.get(5, TimeUnit.SECONDS),
                    second.get(5, TimeUnit.SECONDS)
            );
            assertEquals(1, outcomes.stream().filter(code -> code == CodeEnum.OK).count());
            assertEquals(1, outcomes.stream().filter(code -> code == CodeEnum.LAST_AVAILABLE_ADMIN_REQUIRED).count());
            assertEquals(1, userMapper.countAdminUsers());
            assertEquals(1, jdbc.queryForObject(
                    "SELECT COUNT(*) FROM t_user_role relation INNER JOIN t_role role ON role.id=relation.role_id WHERE relation.user_id IN (?,?) AND role.role='admin' AND relation.active_marker=1",
                    Integer.class, FIRST_USER_ID, SECOND_USER_ID));
        } finally {
            start.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void firstPasswordChangeIsRequiredBeforeAdminCountsAsReady() {
        jdbc.update("UPDATE t_user SET must_change_password=1 WHERE id=?",FIRST_USER_ID);
        assertEquals(1,userMapper.countAdminUsers());
        assertEquals(1,userMapper.countPendingAdminUsers());
        jdbc.update("UPDATE t_user SET must_change_password=1 WHERE id=?",SECOND_USER_ID);
        assertEquals(0,userMapper.countAdminUsers());
        assertEquals(2,userMapper.countPendingAdminUsers());
    }

    private CodeEnum removeAdminRole(int userId, CountDownLatch ready, CountDownLatch start) throws Exception {
        ready.countDown();
        if (!start.await(3, TimeUnit.SECONDS)) throw new IllegalStateException("并发请求未能同时启动");
        UpdateRolesRequest request = new UpdateRolesRequest();
        request.setAuthorizationVersion(0);
        request.setRoleIds(List.of());
        request.setReason("并发移除管理员保护验证");
        try {
            authorizationService.replaceRoles(userId, request);
            return CodeEnum.OK;
        } catch (BusinessException exception) {
            return exception.getCodeEnum();
        }
    }

    private void insertAdmin(int userId) {
        jdbc.update("INSERT INTO t_user(id,login_act,login_pwd,name,account_no_expired,credentials_no_expired,account_no_locked,account_enabled,account_status,account_type,protected_account,manual_locked,version,authorization_version,auth_version,session_revision,create_time,create_by) VALUES(?,?, 'x', ?,1,1,1,1,'ACTIVE','HUMAN',0,0,0,0,0,0,CURRENT_TIMESTAMP,1)", userId, "ordinary_admin_" + userId, "普通管理员" + userId);
        jdbc.update("INSERT INTO t_employee(id,user_id,employee_no,name,phone,employment_status,profile_completed,version,profile_version,phone_verified,email_verified,create_time,create_by) VALUES(?,?,?,?,?,'ACTIVE',1,0,0,1,0,CURRENT_TIMESTAMP,1)", userId, userId, "EMP-ADMIN-" + userId, "普通管理员" + userId, "139" + String.format("%08d", userId));
        jdbc.update("INSERT INTO t_employee_assignment(employee_id,organization_unit_id,position_id,assignment_type,status,active_primary_marker,effective_from,reason,version,create_time,create_by) VALUES(?,?,?,'PRIMARY','ACTIVE',1,CURRENT_TIMESTAMP,'管理员保护测试',0,CURRENT_TIMESTAMP,1)", userId, ORGANIZATION_ID, POSITION_ID);
        jdbc.update("INSERT INTO t_user_role(user_id,role_id,granted_by,reason,effective_from,active_marker,version) SELECT ?,id,1,'管理员保护测试',CURRENT_TIMESTAMP,1,0 FROM t_role WHERE role='admin'", userId);
    }

    private void cleanup() {
        jdbc.update("DELETE FROM t_authorization_history WHERE target_user_id IN (?,?)", FIRST_USER_ID, SECOND_USER_ID);
        jdbc.update("DELETE FROM t_user_role WHERE user_id IN (?,?)", FIRST_USER_ID, SECOND_USER_ID);
        jdbc.update("DELETE FROM t_employee_assignment WHERE employee_id IN (?,?)", FIRST_USER_ID, SECOND_USER_ID);
        jdbc.update("DELETE FROM t_employee WHERE id IN (?,?)", FIRST_USER_ID, SECOND_USER_ID);
        jdbc.update("DELETE FROM t_user WHERE id IN (?,?)", FIRST_USER_ID, SECOND_USER_ID);
        jdbc.update("DELETE FROM t_position WHERE id=?", POSITION_ID);
        jdbc.update("DELETE FROM t_organization_unit WHERE id=?", ORGANIZATION_ID);
    }
}
