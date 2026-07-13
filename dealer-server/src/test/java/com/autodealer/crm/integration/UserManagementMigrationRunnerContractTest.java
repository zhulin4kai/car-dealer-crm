package com.autodealer.crm.integration;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class UserManagementMigrationRunnerContractTest {
    private static final Path ROOT = Path.of("..").toAbsolutePath().normalize();

    @Test
    void manifestIsOrderedAndChecksummed() throws Exception {
        String manifest = Files.readString(Path.of("src/main/resources/migration/manifest.tsv"));
        for (String task : new String[]{"task03", "task09", "task10", "task11", "task12", "task13", "task15", "task16", "task17", "task18", "task19", "task20", "task22", "task23", "task24"}) {
            assertTrue(manifest.contains(task), task);
        }
        assertTrue(manifest.contains("FIRST_RUN_BACKFILL_THEN_OBJECT_RESUME"));
        assertFalse(manifest.contains("TABLE_NAME='t_organization'"));
        assertFalse(manifest.contains("code='organization:manage'"));
        assertTrue(manifest.contains("t_organization_unit"));
        assertTrue(manifest.contains("trg_authorization_history_no_update"));
        assertTrue(manifest.contains("trg_user_lifecycle_event_no_update"));
        assertTrue(manifest.contains("idx_employee_reporting_workspace_manager"));
        assertTrue(manifest.contains("CHECK_CLAUSE NOT LIKE '%>=%'"));
        assertTrue(manifest.contains("uk_login_identifier_login_act"));
        assertTrue(manifest.contains("uk_login_identifier_active_user"));
        assertTrue(manifest.contains("t_login_identifier"));
        assertTrue(manifest.contains("COLUMN_NAME='account_expires_at'"));
        assertTrue(manifest.contains("lock_name='LOGIN_IDENTIFIER_GUARD'"));
        assertTrue(manifest.contains("trg_login_identifier_immutable_bu"));
        assertTrue(manifest.contains("trg_recovery_account_identity_bu"));
        String previous = null;
        for (String line : manifest.lines().filter(value -> !value.startsWith("#") && !value.isBlank()).toList()) {
            String[] fields = line.split("\\t", -1);
            assertEquals(9, fields.length, line);
            if ("ACTIVE".equals(fields[5])) {
                assertEquals(previous == null ? "-" : previous, fields[3], "dependency chain: " + fields[1]);
                byte[] bytes = Files.readAllBytes(Path.of("src/main/resources/migration").resolve(fields[2]));
                assertEquals(fields[4], HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)), fields[1]);
                previous = fields[1];
            }
        }
    }

    @Test
    void everyBusinessMigrationRechecksContextInsideMutationProceduresAndHasNoTopLevelMutation() throws Exception {
        Map<String, String> migrations = new LinkedHashMap<>();
        migrations.put("20260711_task09_organization_foundation", "20260711_task09_organization_foundation.sql");
        migrations.put("20260711_task10_authorization_history", "20260711_task10_authorization_history.sql");
        migrations.put("20260711_task11_organization_management", "20260711_task11_organization_management.sql");
        migrations.put("20260711_task12_role_permission_matrix", "20260711_task12_role_permission_matrix.sql");
        migrations.put("20260711_task13_user_authorization", "20260711_task13_user_authorization.sql");
        migrations.put("20260712_task15_credential_lifecycle", "20260712_task15_credential_lifecycle.sql");
        migrations.put("20260712_task16_profile", "20260712_task16_profile.sql");
        migrations.put("20260712_task17_user_session", "20260712_task17_user_session.sql");
        migrations.put("20260712_task18_user_management_workspace", "20260712_task18_user_management_workspace.sql");
        migrations.put("20260712_task19_user_history", "20260712_task19_user_history.sql");
        migrations.put("20260712_task20_user_lifecycle", "20260712_task20_user_lifecycle.sql");
        migrations.put("20260713_task22_user_management_hardening", "20260713_task22_user_management_hardening.sql");
        migrations.put("20260713_task23_acting_reporting_and_audit_width", "20260713_task23_acting_reporting_and_audit_width.sql");
        migrations.put("20260713_task24_credential_delivery_outbox", "20260713_task24_credential_delivery_outbox.sql");

        for (Map.Entry<String, String> migration : migrations.entrySet()) {
            assertForceCannotBypassMigration(migration.getKey(), migration.getValue());
        }
    }

    @Test
    void runnerFailsClosedAndIsNotWiredIntoApplicationStartup() throws Exception {
        String runner = Files.readString(ROOT.resolve("scripts/database/user-management-migrate.sh"));
        int migrationSource = runner.indexOf("printf 'SOURCE %s\\n' \"${script_path}\"");
        int runnerContextGuard = runner.indexOf("printf \"CALL crm_require_migration_context('%s');\\n\" \"${key}\"", migrationSource);
        int guardedPayload = runner.indexOf("emit_guarded_runner_payloads \"${script_path}\" \"${key}\"", migrationSource);
        assertAll(
                () -> assertTrue(runner.contains("GET_LOCK")),
                () -> assertTrue(runner.contains("checksum_sha256")),
                () -> assertTrue(runner.contains("status IN ('RUNNING','FAILED')")),
                () -> assertTrue(runner.contains("attempt_count=attempt_count+1")),
                () -> assertTrue(runner.contains("t_user_management_migration_step")),
                () -> assertTrue(runner.contains("IS_USED_LOCK('car_dealer_crm:user_management_migration')")),
                () -> assertTrue(runner.contains("--init-command=")),
                () -> assertTrue(runner.contains("emit_guarded_runner_payloads")),
                () -> assertTrue(runner.contains("CRM_MIGRATION_RUNNER_PAYLOAD_BEGIN")),
                () -> assertTrue(migrationSource >= 0),
                () -> assertTrue(migrationSource < runnerContextGuard),
                () -> assertTrue(runnerContextGuard < guardedPayload),
                () -> assertTrue(runner.indexOf("迁移对象定义核验失败") < runner.indexOf("SET status='SUCCEEDED'")),
                () -> assertTrue(runner.contains("迁移成功状态账本更新影响行数异常")),
                () -> assertTrue(runner.contains("迁移失败状态账本更新影响行数异常")),
                () -> assertTrue(runner.contains("apply APPLY")),
                () -> assertTrue(runner.contains("resume <migration_key> RESUME"))
        );
        String searchable = Files.readString(ROOT.resolve("compose.yaml"))
                + Files.readString(Path.of("src/main/resources/application.yml"));
        assertFalse(searchable.contains("user-management-migrate.sh"));
    }

    @Test
    void dangerousBackfillsDoNotInventSecurityFacts() throws Exception {
        String task16 = Files.readString(Path.of("src/main/resources/migration/20260712_task16_profile.sql")).toLowerCase();
        assertFalse(task16.contains("set phone_verified=1"));
        assertFalse(task16.contains("set email_verified=1"));
        String task10 = Files.readString(Path.of("src/main/resources/migration/20260711_task10_authorization_history.sql")).toLowerCase();
        assertFalse(task10.contains("granted_by = coalesce(ur.granted_by, 1)"));
        assertFalse(task10.contains("effective_from = coalesce"));
        assertTrue(task10.contains("t_user_management_migration_step"));
        assertTrue(task10.contains("start transaction"));
        assertTrue(task10.contains("effective_to > effective_from"));
        assertFalse(task10.contains("effective_to >= effective_from"));

        String task03 = Files.readString(Path.of("src/main/resources/migration/20260711_task03_auth_version.sql")).toLowerCase();
        assertTrue(task03.split("crm_require_migration_context", -1).length >= 4,
                "入口和每个业务DDL过程都必须二次校验context");

        String task12 = Files.readString(Path.of("src/main/resources/migration/20260711_task12_role_permission_matrix.sql")).toLowerCase();
        assertTrue(task12.contains("role_organization_tables_ready"));
        assertTrue(task12.contains("permission_metadata_first_run_backfill_ready"));

        String task15 = Files.readString(Path.of("src/main/resources/migration/20260712_task15_credential_lifecycle.sql")).toLowerCase();
        assertTrue(task15.contains("account_enabled"));
        assertTrue(task15.contains("account_no_locked"));
        assertTrue(task15.contains("account_expires_at"));
        assertTrue(task15.contains("password_expires_at"));
        assertTrue(task15.contains("legacy_account_state_backfill_ready"));
        assertTrue(task15.contains("login_identifier_backfill_ready"));
        assertTrue(task15.contains("login_identifier_guard_ready"));
        assertTrue(task15.contains("login_identifier_guard"));
        assertTrue(task15.contains("create table if not exists t_login_identifier"));
        assertTrue(task15.contains("insert into t_login_identifier"));
        assertTrue(task15.contains("禁止覆盖迁移"));
        assertFalse(task15.contains("insert ignore into t_login_identifier"));
        String fullSchema = Files.readString(Path.of("src/main/resources/CarDealerCRM.sql")).toLowerCase();
        assertTrue(fullSchema.contains("insert into `t_login_identifier`"));
        assertTrue(fullSchema.contains("'login_identifier_guard'"));
        String testData = Files.readString(Path.of("src/main/resources/data.sql")).toLowerCase();
        assertTrue(testData.contains("merge into t_login_identifier"));
        assertTrue(testData.contains("'login_identifier_guard'"));
        assertTrue(Files.exists(ROOT.resolve("scripts/database/fixtures/user-management-pre-task03.sql")));
    }

    private static void assertForceCannotBypassMigration(String migrationKey, String fileName) throws Exception {
        String sql = Files.readString(Path.of("src/main/resources/migration").resolve(fileName));
        String normalized = sql.toLowerCase();
        String expectedGuard = "call crm_require_migration_context('" + migrationKey.toLowerCase() + "')";
        assertTrue(normalized.contains(expectedGuard), fileName + " 缺少入口 context 校验");

        Pattern procedurePattern = Pattern.compile(
                "(?is)create\\s+procedure\\s+([a-z0-9_]+).*?\\bbegin\\b(.*?)\\bend\\s*(?:\\$\\$|//)");
        Pattern mutationPattern = Pattern.compile(
                "(?is)\\b(?:alter\\s+table|create\\s+(?:temporary\\s+)?table|drop\\s+(?:temporary\\s+)?table|rename\\s+table|create\\s+(?:unique\\s+)?index|drop\\s+index|create\\s+trigger|drop\\s+trigger|insert\\s+into|replace\\s+into|update\\s+[`a-z0-9_]+|delete\\s+from|truncate\\s+table|execute\\s+[a-z0-9_]+)\\b");
        Matcher procedures = procedurePattern.matcher(normalized);
        StringBuilder topLevel = new StringBuilder();
        int previousEnd = 0;
        int mutatingProcedureCount = 0;
        while (procedures.find()) {
            topLevel.append(normalized, previousEnd, procedures.start());
            String procedureName = procedures.group(1);
            String body = procedures.group(2);
            Matcher mutation = mutationPattern.matcher(body);
            if (mutation.find()) {
                mutatingProcedureCount++;
                int guardIndex = body.indexOf(expectedGuard);
                assertTrue(guardIndex >= 0,
                        fileName + " 的业务过程 " + procedureName + " 缺少内部 context 二次校验");
                assertTrue(guardIndex < mutation.start(),
                        fileName + " 的业务过程 " + procedureName + " 必须先校验 context 再执行变更");
            }
            previousEnd = procedures.end();
        }
        topLevel.append(normalized.substring(previousEnd));

        assertTrue(mutatingProcedureCount > 0, fileName + " 必须把业务 DDL/回填封装到过程内");
        Matcher topLevelMutation = mutationPattern.matcher(stripCommentsAndStrings(topLevel.toString()));
        assertFalse(topLevelMutation.find(),
                fileName + " 存在可被 mysql --force 继续执行的过程外业务变更：" +
                        (topLevelMutation.hitEnd() ? "unknown" : topLevelMutation.group()));
    }

    private static String stripCommentsAndStrings(String sql) {
        return sql
                .replaceAll("(?m)--.*$", " ")
                .replaceAll("(?s)/\\*.*?\\*/", " ")
                .replaceAll("'(?:''|[^'])*'", "''");
    }

}
