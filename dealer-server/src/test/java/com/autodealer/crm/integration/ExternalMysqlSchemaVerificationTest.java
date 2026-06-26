package com.autodealer.crm.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Optional verification against a real MySQL/MariaDB schema.
 *
 * Run only when CRM_REAL_MYSQL_URL is provided, for example:
 * CRM_REAL_MYSQL_URL=jdbc:mysql://127.0.0.1:13306/car_dealer_crm \
 * CRM_REAL_MYSQL_USERNAME=root CRM_REAL_MYSQL_PASSWORD=123456 \
 * ./mvnw -Dtest=ExternalMysqlSchemaVerificationTest test
 */
@EnabledIfEnvironmentVariable(named = "CRM_REAL_MYSQL_URL", matches = ".+")
class ExternalMysqlSchemaVerificationTest {

    private static final Path PROJECT_ROOT = Paths.get(System.getProperty("user.dir")).getParent();
    private static final Path PRODUCTION_SCHEMA = PROJECT_ROOT.resolve(
            "dealer-server/src/main/resources/CarDealerCRM.sql");
    private static final Pattern CREATE_TABLE = Pattern.compile(
            "(?is)CREATE\\s+TABLE(?:\\s+IF\\s+NOT\\s+EXISTS)?\\s+`?([a-zA-Z0-9_]+)`?\\s*\\((.*?)\\)\\s*(?:ENGINE\\s*=.*?)?(?=;)");
    private static final Pattern COLUMN = Pattern.compile(
            "^\\s*`?([a-zA-Z_][a-zA-Z0-9_]*)`?\\s+[a-zA-Z]", Pattern.MULTILINE);
    private static final Set<String> NON_COLUMNS = Set.of(
            "PRIMARY", "CONSTRAINT", "UNIQUE", "FOREIGN", "KEY", "CHECK", "INDEX");

    @Test
    @DisplayName("真实 MySQL/MariaDB 表字段必须与生产 Schema 一致")
    void realMysqlColumnsMustMatchProductionSchema() throws Exception {
        Map<String, Set<String>> expected = extractColumns(Files.readString(PRODUCTION_SCHEMA));

        try (Connection connection = openConnection()) {
            String schema = currentSchema(connection);
            Map<String, Set<String>> actual = actualColumns(connection, schema);

            assertEquals(expected.keySet(), actual.keySet(), "真实数据库表集合与生产 Schema 不一致");
            for (String table : expected.keySet()) {
                assertEquals(expected.get(table), actual.get(table), table + " 字段集合与生产 Schema 不一致");
            }
        }
    }

    @Test
    @DisplayName("真实 MySQL/MariaDB 必须具备关键唯一约束、外键和索引")
    void realMysqlMustHaveCoreConstraintsAndIndexes() throws Exception {
        try (Connection connection = openConnection()) {
            String schema = currentSchema(connection);

            assertUniqueIndex(connection, schema, "t_clue", "uk_clue_phone");
            assertUniqueIndex(connection, schema, "t_tran", "uk_tran_no");
            assertUniqueIndex(connection, schema, "t_payment", "uk_payment_transaction_ref");
            assertUniqueIndex(connection, schema, "t_payment", "uk_payment_idempotency_key");
            assertUniqueIndex(connection, schema, "t_product_vehicle", "uk_product_vehicle_vin");
            assertUniqueIndex(connection, schema, "t_product_promotion_usage", "uk_product_promotion_usage_source");

            assertForeignKey(connection, schema, "fk_customer_clue", "RESTRICT");
            assertForeignKey(connection, schema, "fk_tran_customer", "RESTRICT");
            assertForeignKey(connection, schema, "fk_payment_tran", "RESTRICT");
            assertForeignKey(connection, schema, "fk_refund_request_original_payment", "RESTRICT");
            assertForeignKey(connection, schema, "fk_delivery_tran", "RESTRICT");
            assertForeignKey(connection, schema, "fk_comm_record_task", "RESTRICT");

            assertIndex(connection, schema, "t_tran", "t_tran_ibfk_1");
            assertIndex(connection, schema, "t_product_vehicle", "idx_product_vehicle_product_status");
            assertIndex(connection, schema, "t_follow_task", "idx_follow_task_owner_due");
            assertIndex(connection, schema, "t_communication_record", "idx_comm_record_owner_time");

            assertTrigger(connection, schema, "trg_permission_no_self_parent_bi");
            assertTrigger(connection, schema, "trg_permission_no_self_parent_bu");
        }
    }

    @Test
    @DisplayName("真实 MySQL/MariaDB 必须拒绝权限父级自引用")
    void realMysqlMustRejectPermissionSelfParent() throws Exception {
        try (Connection connection = openConnection()) {
            assertThrows(SQLException.class, () -> executeUpdate(connection, """
                    INSERT INTO t_permission (id, name, code, type, parent_id)
                    VALUES (991001, '自引用权限', 'test:mysql:self-parent-insert', 'menu', 991001)
                    """));

            executeUpdate(connection, """
                    INSERT INTO t_permission (id, name, code, type)
                    VALUES (991002, '自引用更新权限', 'test:mysql:self-parent-update', 'menu')
                    """);
            try {
                assertThrows(SQLException.class, () -> executeUpdate(connection,
                        "UPDATE t_permission SET parent_id = 991002 WHERE id = 991002"));
            } finally {
                executeUpdate(connection, "DELETE FROM t_permission WHERE id = 991002");
            }
        }
    }

    private Connection openConnection() throws SQLException {
        String url = System.getenv("CRM_REAL_MYSQL_URL");
        String username = System.getenv().getOrDefault("CRM_REAL_MYSQL_USERNAME", "root");
        String password = System.getenv().getOrDefault("CRM_REAL_MYSQL_PASSWORD", "");
        return DriverManager.getConnection(url, username, password);
    }

    private String currentSchema(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT DATABASE()");
             ResultSet resultSet = statement.executeQuery()) {
            assertTrue(resultSet.next(), "无法读取当前数据库名");
            return resultSet.getString(1);
        }
    }

    private Map<String, Set<String>> actualColumns(Connection connection, String schema) throws SQLException {
        Map<String, Set<String>> result = new LinkedHashMap<>();
        String sql = """
                SELECT table_name, column_name
                FROM information_schema.columns
                WHERE table_schema = ?
                ORDER BY table_name, ordinal_position
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, schema);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    result.computeIfAbsent(resultSet.getString("table_name").toLowerCase(),
                            ignored -> new LinkedHashSet<>()).add(resultSet.getString("column_name").toLowerCase());
                }
            }
        }
        return result;
    }

    private Map<String, Set<String>> extractColumns(String sql) throws IOException {
        Map<String, Set<String>> result = new LinkedHashMap<>();
        Matcher tableMatcher = CREATE_TABLE.matcher(sql);
        while (tableMatcher.find()) {
            Set<String> columns = new LinkedHashSet<>();
            Matcher columnMatcher = COLUMN.matcher(tableMatcher.group(2));
            while (columnMatcher.find()) {
                String column = columnMatcher.group(1);
                if (!NON_COLUMNS.contains(column.toUpperCase())) {
                    columns.add(column.toLowerCase());
                }
            }
            result.put(tableMatcher.group(1).toLowerCase(), columns);
        }
        return result;
    }

    private void assertUniqueIndex(Connection connection, String schema, String table, String index) throws SQLException {
        String sql = """
                SELECT COUNT(*)
                FROM information_schema.statistics
                WHERE table_schema = ? AND table_name = ? AND index_name = ? AND non_unique = 0
                """;
        assertCountAtLeastOne(connection, sql, schema, table, index, table + "." + index + " 唯一索引缺失");
    }

    private void assertIndex(Connection connection, String schema, String table, String index) throws SQLException {
        String sql = """
                SELECT COUNT(*)
                FROM information_schema.statistics
                WHERE table_schema = ? AND table_name = ? AND index_name = ?
                """;
        assertCountAtLeastOne(connection, sql, schema, table, index, table + "." + index + " 索引缺失");
    }

    private void assertForeignKey(Connection connection, String schema, String constraint, String deleteRule)
            throws SQLException {
        String sql = """
                SELECT COUNT(*)
                FROM information_schema.referential_constraints
                WHERE constraint_schema = ? AND constraint_name = ? AND delete_rule = ?
                """;
        assertCountAtLeastOne(connection, sql, schema, constraint, deleteRule, constraint + " 外键删除规则缺失");
    }

    private void assertTrigger(Connection connection, String schema, String trigger) throws SQLException {
        String sql = """
                SELECT COUNT(*)
                FROM information_schema.triggers
                WHERE trigger_schema = ? AND trigger_name = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, schema);
            statement.setString(2, trigger);
            try (ResultSet resultSet = statement.executeQuery()) {
                assertTrue(resultSet.next(), trigger + " 触发器缺失");
                assertTrue(resultSet.getInt(1) > 0, trigger + " 触发器缺失");
            }
        }
    }

    private void executeUpdate(Connection connection, String sql) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.executeUpdate();
        }
    }

    private void assertCountAtLeastOne(Connection connection, String sql, String arg1, String arg2, String arg3,
                                       String message) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, arg1);
            statement.setString(2, arg2);
            statement.setString(3, arg3);
            try (ResultSet resultSet = statement.executeQuery()) {
                assertTrue(resultSet.next(), message);
                assertTrue(resultSet.getInt(1) > 0, message);
            }
        }
    }
}
