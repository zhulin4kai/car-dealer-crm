package com.autodealer.crm.integration;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class Task19MigrationContractTest {
    @Test
    void migrationIsIdempotentAndDelegatesCompletionToTheRunner() throws Exception {
        String sql = Files.readString(Path.of("src/main/resources/migration/20260712_task19_user_history.sql"))
                .toLowerCase();
        assertTrue(sql.contains("information_schema.columns"));
        assertTrue(sql.contains("column_name='affected_user_ids'"));
        assertTrue(sql.contains("column_name='affected_users_snapshot'"));
        assertTrue(sql.contains("authorization_membership_guard"));
        int secondAlter = sql.indexOf("add column affected_users_snapshot");
        assertTrue(secondAlter >= 0);
        assertTrue(sql.contains("on duplicate key update"));
        assertTrue(sql.contains("crm_require_migration_context('20260712_task19_user_history')"));
        assertTrue(sql.contains("crm_migration_mark_step('20260712_task19_user_history'"));
        assertFalse(sql.contains("insert into t_user_management_migration"));
    }
}
