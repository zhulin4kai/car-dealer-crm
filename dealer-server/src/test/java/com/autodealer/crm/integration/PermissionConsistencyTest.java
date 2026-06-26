package com.autodealer.crm.integration;

import com.autodealer.crm.constant.PermissionCodes;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class PermissionConsistencyTest extends BackendIntegrationTestBase {

    private static final Path PROJECT_ROOT = Paths.get(System.getProperty("user.dir")).getParent();
    private static final Path FRONTEND_PERMISSION_CODES = PROJECT_ROOT.resolve(
            "dealer-web/src/shared/constants/permissions.ts");
    private static final Path PRODUCTION_SQL = PROJECT_ROOT.resolve(
            "dealer-server/src/main/resources/CarDealerCRM.sql");
    private static final Path TEST_DATA_SQL = PROJECT_ROOT.resolve(
            "dealer-server/src/main/resources/data.sql");
    private static final Pattern TS_PERMISSION_VALUE = Pattern.compile("'([a-z-]+(?::[a-z-]+)+)'");
    private static final Pattern FORBIDDEN_PERMISSION_ID = Pattern.compile("\\b270[1-5]\\b");
    private static final Pattern NUMERIC_ROLE_PERMISSION_SEED = Pattern.compile(
            "(?is)insert\\s+into\\s+`?t_role_permission`?\\s*\\([^)]*\\)\\s*values");

    @Test
    @DisplayName("后端权限常量、数据库按钮权限和前端权限常量必须完全一致")
    void permissionCodesMustMatchAcrossLayers() throws Exception {
        Set<String> backendCodes = Arrays.stream(PermissionCodes.class.getDeclaredFields())
                .filter(field -> Modifier.isPublic(field.getModifiers()))
                .filter(field -> Modifier.isStatic(field.getModifiers()))
                .filter(field -> field.getType().equals(String.class))
                .map(this::readConstant)
                .collect(Collectors.toSet());

        Set<String> databaseCodes = new HashSet<>(jdbcTemplate.queryForList(
                "SELECT code FROM t_permission WHERE type = 'button'", String.class));
        Set<String> frontendCodes = collectFrontendCodes();

        assertEquals(databaseCodes, backendCodes, "数据库按钮权限与后端 PermissionCodes 存在漂移");
        assertEquals(backendCodes, frontendCodes, "前端权限常量与后端 PermissionCodes 存在漂移");
    }

    @Test
    @DisplayName("生产和测试种子不得包含固定权限主键或数字角色权限清单")
    void seedSqlMustNotDependOnNumericPermissionIds() throws IOException {
        for (Path sqlFile : Set.of(PRODUCTION_SQL, TEST_DATA_SQL)) {
            String sql = Files.readString(sqlFile);
            assertFalse(FORBIDDEN_PERMISSION_ID.matcher(sql).find(),
                    sqlFile.getFileName() + " 仍包含 2701-2705 权限主键");
            assertFalse(NUMERIC_ROLE_PERMISSION_SEED.matcher(sql).find(),
                    sqlFile.getFileName() + " 仍使用 VALUES 写死角色权限关联");
        }
    }

    private String readConstant(Field field) {
        try {
            return (String) field.get(null);
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("无法读取权限常量 " + field.getName(), exception);
        }
    }

    private Set<String> collectFrontendCodes() throws IOException {
        Matcher matcher = TS_PERMISSION_VALUE.matcher(Files.readString(FRONTEND_PERMISSION_CODES));
        Set<String> result = new HashSet<>();
        while (matcher.find()) {
            result.add(matcher.group(1));
        }
        return result;
    }
}
