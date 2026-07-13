package com.autodealer.crm.integration;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class Task18MigrationContractTest {
    private static final Path MIGRATION = Path.of("src/main/resources/migration/20260712_task18_user_management_workspace.sql");

    @Test
    void migrationIsRestartSafeAndDelegatesCompletionToTheRunner() throws Exception {
        String sql = Files.readString(MIGRATION).toLowerCase();
        for (String index : new String[]{
                "idx_user_workspace_status", "idx_user_workspace_last_login",
                "idx_employee_assignment_workspace_org", "idx_employee_assignment_workspace_position",
                "idx_employee_reporting_workspace_manager"}) {
            assertTrue(sql.contains("information_schema.statistics") && sql.contains("index_name='" + index + "'"));
        }
        assertTrue(sql.contains("available_admin_guard"));
        assertTrue(sql.contains("on duplicate key update lock_name=values(lock_name)"));
        assertTrue(sql.contains("not exists (select 1 from t_permission"));
        assertTrue(sql.contains("crm_require_migration_context('20260712_task18_user_management_workspace')"));
        assertTrue(sql.contains("crm_migration_mark_step('20260712_task18_user_management_workspace'"));
        assertTrue(!sql.contains("insert into t_user_management_migration"));
    }
}
