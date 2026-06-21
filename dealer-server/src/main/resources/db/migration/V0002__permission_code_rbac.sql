-- ============================================================================
-- V0002: 权限编码化与 RBAC 关系正规化（MariaDB/MySQL 手工执行一次）
--
-- 执行顺序:
--   1. 先执行“前置检查”，任何查询返回记录都必须先修复。
--   2. 备份 t_permission、t_role、t_role_permission、t_user_role。
--   3. 在应用停写窗口执行本文件，再部署使用新权限码的应用。
--   4. 执行末尾验收查询；结果不满足预期时从备份回滚。
-- ============================================================================

-- 前置检查：以下查询预期均返回 0 行。
SELECT code, COUNT(*) FROM t_permission
WHERE code IS NOT NULL AND TRIM(code) <> ''
GROUP BY code HAVING COUNT(*) > 1;

SELECT role, COUNT(*) FROM t_role
WHERE role IS NOT NULL AND TRIM(role) <> ''
GROUP BY role HAVING COUNT(*) > 1;

SELECT rp.role_id, rp.permission_id, COUNT(*)
FROM t_role_permission rp
GROUP BY rp.role_id, rp.permission_id HAVING COUNT(*) > 1;

SELECT ur.user_id, ur.role_id, COUNT(*)
FROM t_user_role ur
GROUP BY ur.user_id, ur.role_id HAVING COUNT(*) > 1;

SELECT p.id, p.parent_id FROM t_permission p
LEFT JOIN t_permission parent ON parent.id = p.parent_id
WHERE p.parent_id NOT IN (0, p.id) AND parent.id IS NULL;

SELECT id, parent_id FROM t_permission WHERE parent_id = id;

-- 1. 扩展权限、角色字段，先保持可空以完成回填。
ALTER TABLE t_permission
    MODIFY name VARCHAR(64) NOT NULL,
    MODIFY code VARCHAR(64) NULL,
    MODIFY url VARCHAR(255) NULL,
    MODIFY type VARCHAR(30) NOT NULL,
    ADD COLUMN enabled TINYINT(1) NOT NULL DEFAULT 1 AFTER icon;

ALTER TABLE t_role
    MODIFY role VARCHAR(64) NULL,
    MODIFY role_name VARCHAR(64) NOT NULL,
    ADD COLUMN enabled TINYINT(1) NOT NULL DEFAULT 1 AFTER role_name;

-- 2. 给所有菜单节点补稳定编码，不使用历史主键定位业务资源。
UPDATE t_permission SET code = 'menu:dashboard'
WHERE type = 'menu' AND url = '/dashboard';
UPDATE t_permission SET code = 'menu:activity'
WHERE type = 'menu' AND parent_id = 0 AND name = '市场活动';
UPDATE t_permission SET code = 'page:activity:list'
WHERE type = 'menu' AND url = '/dashboard/activity';
UPDATE t_permission SET code = 'menu:clue'
WHERE type = 'menu' AND parent_id = 0 AND name = '线索管理';
UPDATE t_permission SET code = 'page:clue:list'
WHERE type = 'menu' AND url = '/dashboard/clue';
UPDATE t_permission SET code = 'menu:customer'
WHERE type = 'menu' AND parent_id = 0 AND name = '客户管理';
UPDATE t_permission SET code = 'page:customer:list'
WHERE type = 'menu' AND url = '/dashboard/customer';
UPDATE t_permission SET code = 'menu:tran'
WHERE type = 'menu' AND parent_id = 0 AND name = '交易管理';
UPDATE t_permission SET code = 'page:tran:list'
WHERE type = 'menu' AND url = '/dashboard/tran';
UPDATE t_permission SET code = 'menu:product'
WHERE type = 'menu' AND parent_id = 0 AND name = '产品管理';
UPDATE t_permission SET code = 'page:product:list'
WHERE type = 'menu' AND url = '/dashboard/product';
UPDATE t_permission SET code = 'menu:dict'
WHERE type = 'menu' AND parent_id = 0 AND name = '字典管理';
UPDATE t_permission SET code = 'page:dict:type'
WHERE type = 'menu' AND url = '/dashboard/dict/type';
UPDATE t_permission SET code = 'page:dict:value'
WHERE type = 'menu' AND url = '/dashboard/dict/value';
UPDATE t_permission SET code = 'menu:user'
WHERE type = 'menu' AND parent_id = 0 AND name = '用户管理';
UPDATE t_permission SET code = 'page:user:list'
WHERE type = 'menu' AND url = '/dashboard/user';
UPDATE t_permission SET code = 'menu:system'
WHERE type = 'menu' AND parent_id = 0 AND name = '系统管理';
UPDATE t_permission SET code = 'page:system:list'
WHERE type = 'menu' AND url = '/dashboard/system';

-- 历史字典权限编码统一为冒号分隔。
UPDATE t_permission SET code = REPLACE(code, 'dict/type:', 'dict:type:')
WHERE code LIKE 'dict/type:%';
UPDATE t_permission SET code = REPLACE(code, 'dict/value:', 'dict:value:')
WHERE code LIKE 'dict/value:%';

-- 若历史库没有仪表盘菜单，补一个独立根菜单。
INSERT INTO t_permission (name, code, url, type, parent_id, order_no, icon, enabled)
SELECT '仪表盘', 'menu:dashboard', '/dashboard', 'menu', NULL, 0, 'Gauge', 1
WHERE NOT EXISTS (SELECT 1 FROM t_permission WHERE code = 'menu:dashboard');

-- 产品子页面。
INSERT INTO t_permission (name, code, url, type, parent_id, order_no, icon, enabled)
SELECT '商品分类', 'page:product:category', '/dashboard/product/category', 'menu', p.id, 2, 'ListTree', 1
FROM t_permission p WHERE p.code = 'menu:product'
  AND NOT EXISTS (SELECT 1 FROM t_permission WHERE code = 'page:product:category');
INSERT INTO t_permission (name, code, url, type, parent_id, order_no, icon, enabled)
SELECT '促销管理', 'page:product:promotion', '/dashboard/product/promotion', 'menu', p.id, 3, 'BadgePercent', 1
FROM t_permission p WHERE p.code = 'menu:product'
  AND NOT EXISTS (SELECT 1 FROM t_permission WHERE code = 'page:product:promotion');
INSERT INTO t_permission (name, code, url, type, parent_id, order_no, icon, enabled)
SELECT '库存预警', 'page:product:stock', '/dashboard/product/stock', 'menu', p.id, 4, 'Warehouse', 1
FROM t_permission p WHERE p.code = 'menu:product'
  AND NOT EXISTS (SELECT 1 FROM t_permission WHERE code = 'page:product:stock');

-- 3. 新增细粒度权限。每条都按 code 幂等判断。
INSERT INTO t_permission (name, code, type, parent_id, enabled)
SELECT '交易管理-结算', 'tran:settle', 'button', p.id, 1 FROM t_permission p
WHERE p.code = 'page:tran:list' AND NOT EXISTS (SELECT 1 FROM t_permission WHERE code = 'tran:settle');
INSERT INTO t_permission (name, code, type, parent_id, enabled)
SELECT '交易管理-重新提交', 'tran:resubmit', 'button', p.id, 1 FROM t_permission p
WHERE p.code = 'page:tran:list' AND NOT EXISTS (SELECT 1 FROM t_permission WHERE code = 'tran:resubmit');
INSERT INTO t_permission (name, code, type, parent_id, enabled)
SELECT '交易管理-收款', 'tran:payment', 'button', p.id, 1 FROM t_permission p
WHERE p.code = 'page:tran:list' AND NOT EXISTS (SELECT 1 FROM t_permission WHERE code = 'tran:payment');
INSERT INTO t_permission (name, code, type, parent_id, enabled)
SELECT '交易管理-退款', 'tran:refund', 'button', p.id, 1 FROM t_permission p
WHERE p.code = 'page:tran:list' AND NOT EXISTS (SELECT 1 FROM t_permission WHERE code = 'tran:refund');

INSERT INTO t_permission (name, code, type, parent_id, enabled)
SELECT '库存管理-查看', 'product:stock:view', 'button', p.id, 1 FROM t_permission p
WHERE p.code = 'page:product:stock' AND NOT EXISTS (SELECT 1 FROM t_permission WHERE code = 'product:stock:view');
INSERT INTO t_permission (name, code, type, parent_id, enabled)
SELECT '库存管理-调整', 'product:stock:adjust', 'button', p.id, 1 FROM t_permission p
WHERE p.code = 'page:product:stock' AND NOT EXISTS (SELECT 1 FROM t_permission WHERE code = 'product:stock:adjust');

-- 分类和促销权限从旧 product:* 权限派生，旧权限继续用于车型商品。
INSERT INTO t_permission (name, code, type, parent_id, enabled)
SELECT CONCAT('商品分类-', suffix.label), CONCAT('product:category:', suffix.action), 'button', page.id, 1
FROM t_permission page
CROSS JOIN (
    SELECT 'list' action, '列表' label UNION ALL SELECT 'view', '查看'
    UNION ALL SELECT 'add', '录入' UNION ALL SELECT 'edit', '编辑'
    UNION ALL SELECT 'delete', '删除'
) suffix
WHERE page.code = 'page:product:category'
  AND NOT EXISTS (
      SELECT 1 FROM t_permission existing
      WHERE existing.code = CONCAT('product:category:', suffix.action)
  );
INSERT INTO t_permission (name, code, type, parent_id, enabled)
SELECT CONCAT('促销管理-', suffix.label), CONCAT('product:promotion:', suffix.action), 'button', page.id, 1
FROM t_permission page
CROSS JOIN (
    SELECT 'list' action, '列表' label UNION ALL SELECT 'view', '查看'
    UNION ALL SELECT 'add', '录入' UNION ALL SELECT 'edit', '编辑'
    UNION ALL SELECT 'delete', '删除'
) suffix
WHERE page.code = 'page:product:promotion'
  AND NOT EXISTS (
      SELECT 1 FROM t_permission existing
      WHERE existing.code = CONCAT('product:promotion:', suffix.action)
  );

INSERT INTO t_permission (name, code, type, parent_id, enabled)
SELECT '字典缓存-刷新', 'dict:cache:refresh', 'button', p.id, 1 FROM t_permission p
WHERE p.code = 'page:dict:type' AND NOT EXISTS (SELECT 1 FROM t_permission WHERE code = 'dict:cache:refresh');
INSERT INTO t_permission (name, code, type, parent_id, enabled)
SELECT '系统监控-查看', 'monitor:view', 'button', p.id, 1 FROM t_permission p
WHERE p.code = 'menu:system' AND NOT EXISTS (SELECT 1 FROM t_permission WHERE code = 'monitor:view');

INSERT INTO t_permission (name, code, type, parent_id, enabled)
SELECT '用户管理-状态', 'user:status', 'button', p.id, 1 FROM t_permission p
WHERE p.code = 'page:user:list' AND NOT EXISTS (SELECT 1 FROM t_permission WHERE code = 'user:status');
INSERT INTO t_permission (name, code, type, parent_id, enabled)
SELECT '用户管理-角色分配', 'user:role', 'button', p.id, 1 FROM t_permission p
WHERE p.code = 'page:user:list' AND NOT EXISTS (SELECT 1 FROM t_permission WHERE code = 'user:role');
INSERT INTO t_permission (name, code, type, parent_id, enabled)
SELECT '用户管理-密码重置', 'user:password', 'button', p.id, 1 FROM t_permission p
WHERE p.code = 'page:user:list' AND NOT EXISTS (SELECT 1 FROM t_permission WHERE code = 'user:password');

-- 4. 规范角色编码，并补库存角色。
UPDATE t_role SET role = 'sales_consultant', role_name = '销售顾问' WHERE role IN ('saler', 'user');
UPDATE t_role SET role = 'sales_manager', role_name = '销售经理' WHERE role = 'manager';
UPDATE t_role SET role = 'marketing_specialist', role_name = '市场专员' WHERE role = 'marketing';
UPDATE t_role SET role = 'finance_specialist', role_name = '财务专员' WHERE role = 'accountant';
INSERT INTO t_role (role, role_name, enabled)
SELECT 'inventory_specialist', '库存专员', 1
WHERE NOT EXISTS (SELECT 1 FROM t_role WHERE role = 'inventory_specialist');

-- 5. 根节点改用 NULL，添加业务约束。
UPDATE t_permission SET parent_id = NULL WHERE parent_id = 0;
ALTER TABLE t_permission MODIFY code VARCHAR(64) NOT NULL;
ALTER TABLE t_permission
    ADD CONSTRAINT uk_permission_code UNIQUE (code),
    ADD CONSTRAINT chk_permission_type CHECK (type IN ('menu', 'button')),
    ADD INDEX idx_permission_parent (parent_id),
    ADD CONSTRAINT fk_permission_parent FOREIGN KEY (parent_id) REFERENCES t_permission(id) ON DELETE RESTRICT;
ALTER TABLE t_role MODIFY role VARCHAR(64) NOT NULL;
ALTER TABLE t_role ADD CONSTRAINT uk_role_code UNIQUE (role);

-- 6. 关联表去重并转换为复合主键。
DELETE older FROM t_role_permission older
JOIN t_role_permission newer
  ON newer.role_id = older.role_id
 AND newer.permission_id = older.permission_id
 AND newer.id < older.id;
ALTER TABLE t_role_permission
    DROP PRIMARY KEY,
    DROP COLUMN id,
    ADD PRIMARY KEY (role_id, permission_id);

DELETE older FROM t_user_role older
JOIN t_user_role newer
  ON newer.user_id = older.user_id
 AND newer.role_id = older.role_id
 AND newer.id < older.id;
ALTER TABLE t_user_role
    DROP PRIMARY KEY,
    DROP COLUMN id,
    ADD PRIMARY KEY (user_id, role_id);

-- 7. 按稳定 code 重建默认角色权限。
DELETE FROM t_role_permission;

INSERT INTO t_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM t_role r CROSS JOIN t_permission p
WHERE r.role = 'admin' AND r.enabled = 1 AND p.enabled = 1;

INSERT INTO t_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM t_role r CROSS JOIN t_permission p
WHERE r.role = 'sales_consultant' AND p.code IN (
    'menu:activity', 'page:activity:list', 'activity:list', 'activity:view',
    'menu:clue', 'page:clue:list', 'clue:list', 'clue:view', 'clue:add', 'clue:edit',
    'menu:customer', 'page:customer:list', 'customer:list', 'customer:view', 'customer:transfer',
    'menu:tran', 'page:tran:list', 'tran:list', 'tran:view', 'tran:create', 'tran:edit',
    'tran:settle', 'tran:resubmit'
);

INSERT INTO t_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM t_role r CROSS JOIN t_permission p
WHERE r.role = 'sales_manager' AND p.code IN (
    'menu:dashboard', 'statistic:view',
    'menu:activity', 'page:activity:list', 'activity:list', 'activity:view', 'activity:add', 'activity:edit', 'activity:delete',
    'menu:clue', 'page:clue:list', 'clue:list', 'clue:view', 'clue:add', 'clue:edit', 'clue:delete', 'clue:import',
    'menu:customer', 'page:customer:list', 'customer:list', 'customer:view', 'customer:transfer', 'customer:export',
    'menu:tran', 'page:tran:list', 'tran:list', 'tran:view', 'tran:create', 'tran:edit',
    'tran:delete', 'tran:settle', 'tran:resubmit', 'tran:approve'
);

INSERT INTO t_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM t_role r CROSS JOIN t_permission p
WHERE r.role = 'marketing_specialist' AND p.code IN (
    'menu:dashboard', 'statistic:view',
    'menu:activity', 'page:activity:list', 'activity:list', 'activity:view', 'activity:add', 'activity:edit', 'activity:delete',
    'menu:clue', 'page:clue:list', 'clue:list', 'clue:view', 'clue:add', 'clue:edit', 'clue:import'
);

INSERT INTO t_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM t_role r CROSS JOIN t_permission p
WHERE r.role = 'finance_specialist' AND p.code IN (
    'menu:dashboard', 'statistic:view', 'menu:tran', 'page:tran:list',
    'tran:list', 'tran:view', 'tran:invoice', 'tran:payment', 'tran:refund'
);

INSERT INTO t_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM t_role r CROSS JOIN t_permission p
WHERE r.role = 'inventory_specialist' AND (
    p.code IN ('menu:product', 'page:product:list',
               'page:product:category', 'page:product:promotion', 'page:product:stock')
    OR p.code LIKE 'product:%'
);

-- 8. 验收：空编码、重复编码、孤儿、重复授权均应为 0。
SELECT COUNT(*) AS empty_permission_codes
FROM t_permission WHERE code IS NULL OR TRIM(code) = '';
SELECT code, COUNT(*) FROM t_permission GROUP BY code HAVING COUNT(*) > 1;
SELECT role, COUNT(*) FROM t_role GROUP BY role HAVING COUNT(*) > 1;
SELECT COUNT(*) AS orphan_role_permissions
FROM t_role_permission rp
LEFT JOIN t_role r ON r.id = rp.role_id
LEFT JOIN t_permission p ON p.id = rp.permission_id
WHERE r.id IS NULL OR p.id IS NULL;
SELECT COUNT(*) AS self_referencing_permissions
FROM t_permission WHERE parent_id = id;
SELECT r.role, COUNT(*) AS permission_count
FROM t_role r LEFT JOIN t_role_permission rp ON rp.role_id = r.id
GROUP BY r.id, r.role ORDER BY r.role;
