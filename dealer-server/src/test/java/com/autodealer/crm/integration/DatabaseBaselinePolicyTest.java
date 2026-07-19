package com.autodealer.crm.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseBaselinePolicyTest {

    private static final Path PROJECT_ROOT = resolveProjectRoot();
    private static final Path RESOURCE_ROOT = PROJECT_ROOT.resolve("dealer-server/src/main/resources");
    private static final Path PRODUCTION_SCHEMA = RESOURCE_ROOT.resolve("CarDealerCRM.sql");
    private static final Path H2_SCHEMA = RESOURCE_ROOT.resolve("schema-test.sql");
    private static final Path H2_DATA = RESOURCE_ROOT.resolve("data.sql");

    @Test
    @DisplayName("仓库不得保留 migration 目录、执行器或迁移账本")
    void repositoryMustNotContainMigrationInfrastructure() throws IOException {
        assertFalse(Files.exists(RESOURCE_ROOT.resolve("migration")),
                "数据库变更必须直接写入基线 SQL，不得创建 migration 目录");
        assertFalse(Files.exists(PROJECT_ROOT.resolve("scripts/database/user-management-migrate.sh")));
        assertFalse(Files.exists(PROJECT_ROOT.resolve("scripts/database/test-user-management-migrations-real.sh")));

        for (Path sqlFile : List.of(PRODUCTION_SCHEMA, H2_SCHEMA, H2_DATA)) {
            String sql = Files.readString(sqlFile).toLowerCase();
            assertFalse(sql.contains("t_user_management_migration"), sqlFile + " 不得保留迁移账本");
            assertFalse(sql.contains("crm_migration_"), sqlFile + " 不得保留迁移执行上下文");
        }
    }

    @Test
    @DisplayName("数据库规范必须明确禁止 migration 并指定唯一基线 SQL")
    void databaseRuleMustProhibitMigrations() throws IOException {
        String rule = Files.readString(PROJECT_ROOT.resolve("docs/rule/04-数据库与MyBatis规范.md"));

        assertTrue(rule.contains("禁止创建任何 migration"));
        assertTrue(rule.contains("dealer-server/src/main/resources/CarDealerCRM.sql"));
        assertTrue(rule.contains("dealer-server/src/main/resources/schema-test.sql"));
        assertTrue(rule.contains("dealer-server/src/main/resources/data.sql"));
    }

    @Test
    @DisplayName("已删除 migration 的最终结构必须完整进入生产基线")
    void productionBaselineMustContainAllFinalDatabaseObjects() throws IOException {
        String sql = Files.readString(PRODUCTION_SCHEMA);

        assertContainsAll(sql, List.of(
                "auth_version", "chk_user_auth_version",
                "t_organization_unit", "t_position", "t_employee", "t_employee_assignment", "t_employee_reporting",
                "placeholder", "uk_employee_user", "uk_employee_active_primary", "uk_employee_active_direct_manager",
                "fk_employee_assignment_org", "fk_employee_reporting_manager",
                "authorization_level", "t_user_permission", "t_authorization_history",
                "trg_authorization_history_no_update", "trg_authorization_history_no_delete", "chk_user_role_period",
                "chk_authorization_history_subject", "t_role_organization", "t_role_permission_organization",
                "t_authorization_graph_lock", "scope_type", "authorization_version", "t_user_permission_organization",
                "account_status", "manual_locked", "account_expires_at", "password_expires_at",
                "t_account_credential", "uk_account_credential_digest", "t_password_history",
                "t_login_identifier", "uk_login_identifier_login_act", "uk_login_identifier_active_user",
                "fk_login_identifier_user", "fk_login_identifier_changed_by", "chk_login_identifier_state",
                "avatar_url", "profile_version", "phone_verified", "email_verified",
                "session_revision", "t_user_session", "uk_user_session_id", "uk_user_session_token_digest",
                "idx_user_workspace_status", "idx_user_workspace_last_login",
                "idx_employee_assignment_workspace_org", "idx_employee_assignment_workspace_position",
                "idx_employee_reporting_workspace_manager", "affected_user_ids", "affected_users_snapshot",
                "t_user_lifecycle_event", "t_user_lifecycle_snapshot",
                "trg_user_lifecycle_event_no_update", "trg_user_lifecycle_event_no_delete",
                "uk_user_lifecycle_operation", "uk_user_lifecycle_snapshot_token", "chk_user_lifecycle_action",
                "target_value_digest", "target_profile_version", "chk_account_credential_purpose",
                "chk_account_credential_contact_binding", "active_root_marker", "uk_organization_unit_active_root",
                "chk_organization_unit_hierarchy", "chk_user_recovery_login_act",
                "trg_login_identifier_immutable_bu", "trg_login_identifier_immutable_bd",
                "trg_recovery_account_identity_bi", "trg_recovery_account_identity_bu", "trg_recovery_account_identity_bd",
                "chk_employee_reporting_acting_finite", "t_credential_delivery_outbox",
                "derivation_nonce", "uk_credential_delivery_message", "chk_credential_delivery_nonce",
                "idx_operation_log_user_history"
        ));
    }

    @Test
    @DisplayName("生产与H2基线必须共同保留最终业务表字段和索引")
    void h2BaselineMustContainFinalDatabaseObjects() throws IOException {
        String sql = Files.readString(H2_SCHEMA);

        assertContainsAll(sql, List.of(
                "auth_version", "chk_user_auth_version",
                "t_organization_unit", "t_position", "t_employee", "t_employee_assignment", "t_employee_reporting",
                "t_user_permission", "t_authorization_history", "t_role_organization",
                "t_role_permission_organization", "t_authorization_graph_lock", "t_user_permission_organization",
                "t_account_credential", "t_password_history", "t_login_identifier", "t_user_session",
                "idx_user_workspace_status", "idx_user_workspace_last_login",
                "idx_employee_assignment_workspace_org", "idx_employee_assignment_workspace_position",
                "idx_employee_reporting_workspace_manager", "affected_user_ids", "affected_users_snapshot",
                "t_user_lifecycle_event", "t_user_lifecycle_snapshot", "t_credential_delivery_outbox",
                "idx_operation_log_user_history"
        ));
    }

    @Test
    @DisplayName("权限和串行化锁种子必须直接进入初始化 SQL")
    void baselineDataMustContainFinalSeedData() throws IOException {
        String production = Files.readString(PRODUCTION_SCHEMA);
        String h2Data = Files.readString(H2_DATA);
        List<String> requiredSeeds = List.of(
                "organization:list", "organization:view", "organization:add", "organization:edit", "organization:status",
                "position:list", "position:add", "position:edit", "position:status",
                "employee:assignment", "employee:reporting", "user:permission", "user:sensitive:view",
                "AVAILABLE_ADMIN_GUARD", "AUTHORIZATION_MEMBERSHIP_GUARD",
                "TEST_DRIVE_SCHEDULE_GUARD", "LOGIN_IDENTIFIER_GUARD"
        );

        assertContainsAll(production, requiredSeeds);
        assertContainsAll(h2Data, requiredSeeds);
    }

    private static void assertContainsAll(String text, List<String> expectedValues) {
        for (String expected : expectedValues) {
            assertTrue(text.contains(expected), "缺少数据库基线对象或种子: " + expected);
        }
    }

    private static Path resolveProjectRoot() {
        Path workingDirectory = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        return Files.isDirectory(workingDirectory.resolve("dealer-server"))
                ? workingDirectory
                : workingDirectory.getParent();
    }
}
