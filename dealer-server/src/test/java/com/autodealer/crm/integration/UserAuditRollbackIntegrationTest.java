package com.autodealer.crm.integration;

import com.autodealer.crm.audit.AuditActionEnum;
import com.autodealer.crm.audit.OperationAuditRecorder;
import com.autodealer.crm.config.security.CurrentUserProvider;
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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class UserAuditRollbackIntegrationTest {

    private static final int LOCK_USER_ID = 9901;
    private static final int UNLOCK_USER_ID = 9902;

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
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.update("DELETE FROM t_user WHERE id IN (?, ?)", LOCK_USER_ID, UNLOCK_USER_ID);
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

    private int accountNoLocked(int userId) {
        return jdbcTemplate.queryForObject(
                "SELECT account_no_locked FROM t_user WHERE id = ?", Integer.class, userId);
    }
}
