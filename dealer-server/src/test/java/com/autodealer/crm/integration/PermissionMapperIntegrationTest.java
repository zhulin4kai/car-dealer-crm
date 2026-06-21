package com.autodealer.crm.integration;

import com.autodealer.crm.mapper.TPermissionMapper;
import com.autodealer.crm.model.TPermission;
import com.autodealer.crm.model.TUser;
import com.autodealer.crm.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

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
