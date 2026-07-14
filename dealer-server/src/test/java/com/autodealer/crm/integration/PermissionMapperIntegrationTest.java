package com.autodealer.crm.integration;

import com.autodealer.crm.modules.identity.application.api.enums.DataScopeCode;
import com.autodealer.crm.modules.identity.application.api.enums.PermissionEffect;
import com.autodealer.crm.modules.identity.application.api.enums.PermissionSensitivityLevel;
import com.autodealer.crm.modules.identity.persistence.mapper.TAuthorizationHistoryMapper;
import com.autodealer.crm.modules.identity.persistence.mapper.TPermissionMapper;
import com.autodealer.crm.modules.identity.persistence.mapper.TRoleMapper;
import com.autodealer.crm.modules.identity.persistence.mapper.TUserPermissionMapper;
import com.autodealer.crm.modules.identity.persistence.model.TPermission;
import com.autodealer.crm.modules.identity.persistence.model.TRole;
import com.autodealer.crm.modules.identity.application.api.model.TUser;
import com.autodealer.crm.modules.identity.persistence.model.TUserPermission;
import com.autodealer.crm.modules.identity.application.api.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Transactional
class PermissionMapperIntegrationTest extends BackendIntegrationTestBase {

    @Autowired
    private TPermissionMapper permissionMapper;

    @Autowired
    private UserService userService;

    @Autowired
    private TUserPermissionMapper userPermissionMapper;

    @Autowired
    private TRoleMapper roleMapper;

    @Test
    @DisplayName("用户拥有多个角色时按钮权限必须去重")
    void buttonPermissionsMustBeDistinctAcrossRoles() {
        jdbcTemplate.update(
                "INSERT INTO t_user_role (user_id, role_id) " +
                "SELECT u.id, r.id FROM t_user u CROSS JOIN t_role r " +
                "WHERE u.login_act = 'zhangsan' AND r.role = 'sales_manager'");

        List<TPermission> permissions = permissionMapper.selectButtonPermissionByUserId(2);
        Set<String> uniqueCodes = new HashSet<>();
        permissions.forEach(permission -> uniqueCodes.add(permission.getCode()));

        assertEquals(uniqueCodes.size(), permissions.size());
    }

    @Test
    @DisplayName("禁用权限和禁用角色不得进入登录权限")
    void disabledPermissionAndRoleMustBeFiltered() {
        jdbcTemplate.update("UPDATE t_permission SET enabled = 0 WHERE code = 'clue:add'");
        assertFalse(permissionMapper.selectButtonPermissionByUserId(2).stream()
                .anyMatch(permission -> "clue:add".equals(permission.getCode())));

        jdbcTemplate.update(
                "INSERT INTO t_user (id, login_act, login_pwd, name, account_no_expired, " +
                "credentials_no_expired, account_no_locked, account_enabled) " +
                "VALUES (899, 'disabled_role_test', 'unused', 'disabled_role_test', 1, 1, 1, 1)");
        jdbcTemplate.update(
                "INSERT INTO t_role (role, role_name, enabled) VALUES ('disabled_test', '禁用测试角色', 0)");
        jdbcTemplate.update(
                "INSERT INTO t_user_role (user_id, role_id) SELECT 899, id FROM t_role WHERE role = 'disabled_test'");
        jdbcTemplate.update(
                "INSERT INTO t_role_permission (role_id, permission_id) " +
                "SELECT r.id, p.id FROM t_role r CROSS JOIN t_permission p " +
                "WHERE r.role = 'disabled_test' AND p.code IN ('menu:activity', 'activity:list')");
        assertTrue(permissionMapper.selectButtonPermissionByUserId(899).isEmpty());
        assertTrue(permissionMapper.selectMenuPermissionByUserId(899).isEmpty());
    }

    @Test
    @DisplayName("未授权父菜单时服务层不得返回孤立子菜单")
    void orphanMenuMustBeDroppedByService() {
        jdbcTemplate.update(
                "DELETE FROM t_role_permission WHERE role_id = " +
                "(SELECT id FROM t_role WHERE role = 'sales_consultant') AND permission_id = " +
                "(SELECT id FROM t_permission WHERE code = 'menu:clue')");

        TUser user = userService.getLoginUserById(2);
        assertFalse(containsMenuCode(user.getMenuPermissionList(), "page:clue:list"));
    }

    @Test
    @DisplayName("菜单查询按 order_no 和 id 稳定排序")
    void menuQueryMustHaveStableOrder() {
        List<TPermission> permissions = permissionMapper.selectMenuPermissionByUserId(1);
        for (int index = 1; index < permissions.size(); index++) {
            TPermission previous = permissions.get(index - 1);
            TPermission current = permissions.get(index);
            int previousOrder = previous.getOrderNo() == null ? Integer.MAX_VALUE : previous.getOrderNo();
            int currentOrder = current.getOrderNo() == null ? Integer.MAX_VALUE : current.getOrderNo();
            assertTrue(previousOrder < currentOrder
                    || previousOrder == currentOrder && previous.getId() < current.getId());
        }
    }

    @Test
    @DisplayName("个人 GRANT/DENY 当前态按有效期过滤且不改变旧权限查询结果")
    void personalPermissionCurrentStateMustUseCasAndEffectivePeriodWithoutChangingLegacyResult() {
        List<String> legacyBefore = permissionMapper.selectButtonPermissionByUserId(2).stream()
                .map(TPermission::getCode).toList();
        Integer permissionId = jdbcTemplate.queryForObject(
                "SELECT id FROM t_permission WHERE code = 'user:role'", Integer.class);
        LocalDateTime now = LocalDateTime.now();

        TUserPermission current = new TUserPermission();
        current.setUserId(2);
        current.setPermissionId(permissionId);
        current.setEffect(PermissionEffect.GRANT);
        current.setDataScopeCode(DataScopeCode.SELF);
        current.setEffectiveFrom(now.minusMinutes(1));
        current.setEffectiveTo(now.plusMinutes(1));
        current.setReason("定向测试授权");
        current.setGrantedBy(1);
        current.setVersion(0);
        current.setCreateTime(now);
        assertEquals(1, userPermissionMapper.insert(current));
        assertEquals(PermissionEffect.GRANT,
                userPermissionMapper.selectCurrentEffective(2, permissionId, now).getEffect());
        assertEquals(null, userPermissionMapper.selectCurrentEffective(2, permissionId, now.plusMinutes(2)));

        current.setEffect(PermissionEffect.DENY);
        current.setDataScopeCode(null);
        current.setEffectiveFrom(now.minusSeconds(1));
        current.setEffectiveTo(null);
        current.setReason("定向测试拒绝");
        current.setUpdateTime(now);
        assertEquals(1, userPermissionMapper.updateCurrentByVersion(current, 0));
        assertEquals(0, userPermissionMapper.updateCurrentByVersion(current, 0));
        TUserPermission denied = userPermissionMapper.selectCurrentEffective(2, permissionId, now);
        assertEquals(PermissionEffect.DENY, denied.getEffect());
        assertEquals(1, denied.getVersion());
        assertEquals(null, denied.getDataScopeCode());

        List<String> legacyAfter = permissionMapper.selectButtonPermissionByUserId(2).stream()
                .map(TPermission::getCode).toList();
        assertEquals(legacyBefore, legacyAfter);
    }

    @Test
    @DisplayName("个人授权数据库约束必须区分 GRANT 数据范围和 DENY 空范围")
    void personalPermissionScopeConstraintsMustRejectInvalidCombinations() {
        Integer permissionId = jdbcTemplate.queryForObject(
                "SELECT id FROM t_permission WHERE code = 'user:status'", Integer.class);
        assertThrowsRuntime(() -> jdbcTemplate.update("""
                INSERT INTO t_user_permission
                    (user_id, permission_id, effect, data_scope_code, effective_from,
                     reason, granted_by, version, create_time)
                VALUES (2, ?, 'DENY', 'SELF', CURRENT_TIMESTAMP, '非法拒绝范围', 1, 0, CURRENT_TIMESTAMP)
                """, permissionId));
        assertThrowsRuntime(() -> jdbcTemplate.update("""
                INSERT INTO t_user_permission
                    (user_id, permission_id, effect, data_scope_code, effective_from,
                     reason, granted_by, version, create_time)
                VALUES (2, ?, 'GRANT', NULL, CURRENT_TIMESTAMP, '非法授权范围', 1, 0, CURRENT_TIMESTAMP)
                """, permissionId));
    }

    @Test
    @DisplayName("内置角色权限元数据与稳定枚举一致且目录更新必须 CAS")
    void seededAuthorizationMetadataAndCatalogCasMustBeStable() {
        TRole admin = roleMapper.selectByUserId(1).stream()
                .filter(role -> "admin".equals(role.getRole()))
                .findFirst().orElseThrow();
        assertTrue(admin.getProtectedRole());
        assertEquals(100, admin.getAuthorizationLevel());
        assertEquals(DataScopeCode.GLOBAL, admin.getDefaultDataScope());
        admin.setDescription("不得通过普通 CAS 修改受保护角色");
        assertEquals(0, roleMapper.updateMutableByIdAndVersion(admin, admin.getVersion()));

        Integer permissionId = jdbcTemplate.queryForObject(
                "SELECT id FROM t_permission WHERE code = 'user:role'", Integer.class);
        TPermission permission = permissionMapper.selectByPrimaryKey(permissionId);
        assertEquals(PermissionSensitivityLevel.PROTECTED, permission.getSensitivityLevel());
        assertFalse(permission.getDelegable());
        String stableCode = permission.getCode();
        permission.setCode("must:not:change");
        permission.setDescription("CAS 更新说明");
        assertEquals(1, permissionMapper.updateMutableByIdAndVersion(permission, 0));
        assertEquals(0, permissionMapper.updateMutableByIdAndVersion(permission, 0));
        TPermission updated = permissionMapper.selectByPrimaryKey(permissionId);
        assertEquals(stableCode, updated.getCode());
        assertEquals(1, updated.getVersion());
    }

    @Test
    @DisplayName("不可变授权历史 Mapper 不得暴露更新或删除方法")
    void authorizationHistoryMapperMustExposeOnlyInsertAndQuery() {
        Set<String> methodNames = Arrays.stream(TAuthorizationHistoryMapper.class.getMethods())
                .map(method -> method.getName()).collect(java.util.stream.Collectors.toSet());
        assertTrue(methodNames.contains("insert"));
        assertTrue(methodNames.stream().anyMatch(name -> name.startsWith("select")));
        assertFalse(methodNames.stream().anyMatch(name -> name.startsWith("update") || name.startsWith("delete")));
    }

    private void assertThrowsRuntime(Runnable action) {
        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, action::run);
    }

    private boolean containsMenuCode(List<TPermission> permissions, String code) {
        if (permissions == null) {
            return false;
        }
        for (TPermission permission : permissions) {
            if (code.equals(permission.getCode()) || containsMenuCode(permission.getSubPermissionList(), code)) {
                return true;
            }
        }
        return false;
    }
}
