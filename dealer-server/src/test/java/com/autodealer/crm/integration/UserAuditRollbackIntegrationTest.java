package com.autodealer.crm.integration;

import com.autodealer.crm.audit.AuditActionEnum;
import com.autodealer.crm.audit.OperationAuditRecorder;
import com.autodealer.crm.config.security.CurrentUserProvider;
import com.autodealer.crm.constant.RedisKeys;
import com.autodealer.crm.dto.HandoverUserResponsibilitiesRequest;
import com.autodealer.crm.manager.RedisManager;
import com.autodealer.crm.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class UserAuditRollbackIntegrationTest {

    private static final int LOCK_USER_ID = 9901;
    private static final int UNLOCK_USER_ID = 9902;
    private static final int HANDOVER_SOURCE_USER_ID = 9903;
    private static final int HANDOVER_TARGET_USER_ID = 9904;
    private static final int HANDOVER_ACTIVITY_ID = 9905;
    private static final int HANDOVER_CLUE_ID = 9906;
    private static final int HANDOVER_CUSTOMER_ID = 9907;

    @Autowired
    private UserService userService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private OperationAuditRecorder auditRecorder;

    @MockBean
    private CurrentUserProvider currentUserProvider;

    @MockBean
    private RedisManager redisManager;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM t_user WHERE id IN (?, ?)", LOCK_USER_ID, UNLOCK_USER_ID);
        deleteHandoverFixture();
        jdbcTemplate.update("""
                INSERT INTO t_user
                    (id, login_act, login_pwd, name, account_no_expired,
                     credentials_no_expired, account_no_locked, account_enabled)
                VALUES (?, ?, ?, ?, 1, 1, ?, 1)
                """, LOCK_USER_ID, "rollback_lock_user", "test", "锁定回滚用户", 1);
        jdbcTemplate.update("""
                INSERT INTO t_user
                    (id, login_act, login_pwd, name, account_no_expired,
                     credentials_no_expired, account_no_locked, account_enabled)
                VALUES (?, ?, ?, ?, 1, 1, ?, 1)
                """, UNLOCK_USER_ID, "rollback_unlock_user", "test", "解锁回滚用户", 0);

        when(currentUserProvider.getCurrentUserId()).thenReturn(9000);
        when(currentUserProvider.getDataScopeUserId()).thenReturn(null);
        when(redisManager.delete(RedisKeys.userLogin(LOCK_USER_ID))).thenReturn(true);
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.update("DELETE FROM t_user WHERE id IN (?, ?)", LOCK_USER_ID, UNLOCK_USER_ID);
        deleteHandoverFixture();
    }

    @Test
    void lockUser_auditFailure_shouldRollbackDatabaseChange() {
        doThrow(new IllegalStateException("审计写入失败"))
                .when(auditRecorder).record(AuditActionEnum.USER_STATUS_CHANGE, String.valueOf(LOCK_USER_ID));

        assertThrows(IllegalStateException.class, () -> userService.lockUser(LOCK_USER_ID));

        assertEquals(1, accountNoLocked(LOCK_USER_ID));
    }

    @Test
    void unlockUser_auditFailure_shouldRollbackDatabaseChange() {
        doThrow(new IllegalStateException("审计写入失败"))
                .when(auditRecorder).record(AuditActionEnum.USER_STATUS_CHANGE, String.valueOf(UNLOCK_USER_ID));

        assertThrows(IllegalStateException.class, () -> userService.unlockUser(UNLOCK_USER_ID));

        assertEquals(0, accountNoLocked(UNLOCK_USER_ID));
    }

    @Test
    void handoverResponsibilities_auditFailure_shouldRollbackResponsibilityChangesAndHistory() {
        insertHandoverFixture();
        when(currentUserProvider.getCurrentUserId()).thenReturn(1);
        doThrow(new IllegalStateException("审计写入失败"))
                .when(auditRecorder).record(eq(AuditActionEnum.USER_HANDOVER),
                        eq(String.valueOf(HANDOVER_SOURCE_USER_ID)), eq("SUCCESS"), anyString());
        HandoverUserResponsibilitiesRequest request = new HandoverUserResponsibilitiesRequest();
        request.setTargetUserId(HANDOVER_TARGET_USER_ID);
        request.setReason("离职交接");

        assertThrows(IllegalStateException.class,
                () -> userService.handoverResponsibilities(HANDOVER_SOURCE_USER_ID, request));

        assertEquals(HANDOVER_SOURCE_USER_ID, activityOwnerId());
        assertEquals(HANDOVER_SOURCE_USER_ID, clueOwnerId());
        assertEquals(HANDOVER_SOURCE_USER_ID, customerOwnerId());
        assertEquals(0, clueOwnerHistoryCount());
        assertEquals(0, customerOwnerHistoryCount());
    }

    private int accountNoLocked(int userId) {
        return jdbcTemplate.queryForObject(
                "SELECT account_no_locked FROM t_user WHERE id = ?", Integer.class, userId);
    }

    private void insertHandoverFixture() {
        jdbcTemplate.update("""
                INSERT INTO t_user
                    (id, login_act, login_pwd, name, account_no_expired,
                     credentials_no_expired, account_no_locked, account_enabled)
                VALUES (?, ?, ?, ?, 1, 1, 1, 1)
                """, HANDOVER_SOURCE_USER_ID, "handover_source_user", "test", "交接原负责人");
        jdbcTemplate.update("""
                INSERT INTO t_user
                    (id, login_act, login_pwd, name, account_no_expired,
                     credentials_no_expired, account_no_locked, account_enabled)
                VALUES (?, ?, ?, ?, 1, 1, 1, 1)
                """, HANDOVER_TARGET_USER_ID, "handover_target_user", "test", "交接目标负责人");
        jdbcTemplate.update("""
                INSERT INTO t_user_role (user_id, role_id)
                SELECT ?, id FROM t_role WHERE role = 'sales_consultant'
                """, HANDOVER_TARGET_USER_ID);
        jdbcTemplate.update("""
                INSERT INTO t_activity (id, owner_id, name)
                VALUES (?, ?, ?)
                """, HANDOVER_ACTIVITY_ID, HANDOVER_SOURCE_USER_ID, "交接活动");
        jdbcTemplate.update("""
                INSERT INTO t_clue (id, owner_id, activity_id, full_name, phone)
                VALUES (?, ?, ?, ?, ?)
                """, HANDOVER_CLUE_ID, HANDOVER_SOURCE_USER_ID, HANDOVER_ACTIVITY_ID,
                "交接线索", "13999999906");
        jdbcTemplate.update("""
                INSERT INTO t_customer (id, clue_id, owner_id, activity_id, customer_name, phone, customer_status)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, HANDOVER_CUSTOMER_ID, HANDOVER_CLUE_ID, HANDOVER_SOURCE_USER_ID,
                HANDOVER_ACTIVITY_ID, "交接客户", "13999999907", "INTENTION");
    }

    private void deleteHandoverFixture() {
        jdbcTemplate.update("DELETE FROM t_customer_owner_history WHERE customer_id = ?", HANDOVER_CUSTOMER_ID);
        jdbcTemplate.update("DELETE FROM t_clue_owner_history WHERE clue_id = ?", HANDOVER_CLUE_ID);
        jdbcTemplate.update("DELETE FROM t_customer WHERE id = ?", HANDOVER_CUSTOMER_ID);
        jdbcTemplate.update("DELETE FROM t_clue WHERE id = ?", HANDOVER_CLUE_ID);
        jdbcTemplate.update("DELETE FROM t_activity WHERE id = ?", HANDOVER_ACTIVITY_ID);
        jdbcTemplate.update("DELETE FROM t_user_role WHERE user_id IN (?, ?)",
                HANDOVER_SOURCE_USER_ID, HANDOVER_TARGET_USER_ID);
        jdbcTemplate.update("DELETE FROM t_user WHERE id IN (?, ?)",
                HANDOVER_SOURCE_USER_ID, HANDOVER_TARGET_USER_ID);
    }

    private int activityOwnerId() {
        return jdbcTemplate.queryForObject(
                "SELECT owner_id FROM t_activity WHERE id = ?", Integer.class, HANDOVER_ACTIVITY_ID);
    }

    private int clueOwnerId() {
        return jdbcTemplate.queryForObject(
                "SELECT owner_id FROM t_clue WHERE id = ?", Integer.class, HANDOVER_CLUE_ID);
    }

    private int customerOwnerId() {
        return jdbcTemplate.queryForObject(
                "SELECT owner_id FROM t_customer WHERE id = ?", Integer.class, HANDOVER_CUSTOMER_ID);
    }

    private int clueOwnerHistoryCount() {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_clue_owner_history WHERE clue_id = ?", Integer.class, HANDOVER_CLUE_ID);
    }

    private int customerOwnerHistoryCount() {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_customer_owner_history WHERE customer_id = ?",
                Integer.class, HANDOVER_CUSTOMER_ID);
    }
}
