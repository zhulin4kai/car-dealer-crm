-- Task 11：组织管理权限与授权历史类型增量迁移。
-- 前置：已执行 Task09、Task10。脚本只新增组织权限种子和管理员映射，不改现有业务角色权限。

CALL crm_require_migration_context('20260711_task11_organization_management');

DROP PROCEDURE IF EXISTS task11_seed_organization_permissions;
DELIMITER $$
CREATE PROCEDURE task11_seed_organization_permissions()
BEGIN
    CALL crm_require_migration_context('20260711_task11_organization_management');

INSERT INTO t_permission
(name, code, url, type, parent_id, order_no, icon, module, description,
 sensitivity_level, delegable, enabled, version)
SELECT '组织架构', 'menu:organization', NULL, 'menu', NULL, 2, 'Network',
       'organization', '组织架构', 'NORMAL', 1, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM t_permission WHERE code = 'menu:organization');

INSERT INTO t_permission
(name, code, url, type, parent_id, order_no, icon, module, description,
 sensitivity_level, delegable, enabled, version)
SELECT '组织架构', 'page:organization:list', '/dashboard/organization', 'menu', parent.id, 1, 'Network',
       'organization', '组织架构', 'NORMAL', 1, 1, 0
FROM t_permission parent
WHERE parent.code = 'menu:organization'
  AND NOT EXISTS (SELECT 1 FROM t_permission WHERE code = 'page:organization:list');

DROP TEMPORARY TABLE IF EXISTS task11_permission_seed;
CREATE TEMPORARY TABLE task11_permission_seed
(
    code VARCHAR(64) PRIMARY KEY,
    name VARCHAR(64) NOT NULL,
    module VARCHAR(64) NOT NULL,
    sensitivity_level VARCHAR(16) NOT NULL,
    delegable TINYINT(1) NOT NULL
);
INSERT INTO task11_permission_seed VALUES
('organization:list', '组织架构-列表', 'organization', 'NORMAL', 1),
('organization:view', '组织架构-查看', 'organization', 'NORMAL', 1),
('organization:add', '组织架构-新增', 'organization', 'SENSITIVE', 0),
('organization:edit', '组织架构-编辑', 'organization', 'PROTECTED', 0),
('organization:status', '组织架构-状态', 'organization', 'PROTECTED', 0),
('position:list', '岗位-列表', 'position', 'NORMAL', 1),
('position:add', '岗位-新增', 'position', 'SENSITIVE', 0),
('position:edit', '岗位-编辑', 'position', 'SENSITIVE', 0),
('position:status', '岗位-状态', 'position', 'PROTECTED', 0),
('employee:assignment', '员工-任职调整', 'employee', 'PROTECTED', 0),
('employee:reporting', '员工-汇报关系', 'employee', 'PROTECTED', 0);

INSERT INTO t_permission
(name, code, url, type, parent_id, order_no, icon, module, description,
 sensitivity_level, delegable, enabled, version)
SELECT seed.name, seed.code, NULL, 'button', parent.id, NULL, NULL,
       seed.module, seed.name, seed.sensitivity_level, seed.delegable, 1, 0
FROM task11_permission_seed seed
CROSS JOIN t_permission parent
WHERE parent.code = 'page:organization:list'
  AND NOT EXISTS (SELECT 1 FROM t_permission existing WHERE existing.code = seed.code);

-- 只给受保护管理员补新权限，不扩大任何业务角色的管理能力。
INSERT INTO t_role_permission (role_id, permission_id, delegable, data_scope_code)
SELECT role_record.id, permission_record.id, permission_record.delegable, 'GLOBAL'
FROM t_role role_record
CROSS JOIN t_permission permission_record
WHERE role_record.role = 'admin'
  AND permission_record.code IN (
      'menu:organization', 'page:organization:list',
      'organization:list', 'organization:view', 'organization:add', 'organization:edit', 'organization:status',
      'position:list', 'position:add', 'position:edit', 'position:status',
      'employee:assignment', 'employee:reporting')
  AND NOT EXISTS (
      SELECT 1 FROM t_role_permission existing
      WHERE existing.role_id = role_record.id AND existing.permission_id = permission_record.id
  );

DROP TEMPORARY TABLE task11_permission_seed;
END$$
DELIMITER ;
CALL task11_seed_organization_permissions();
DROP PROCEDURE task11_seed_organization_permissions;
CALL crm_migration_mark_step('20260711_task11_organization_management', 'ORGANIZATION_PERMISSION_SEEDS_READY');

-- Task11 新增组织目录和岗位历史类型；已有同名约束不删除重建，避免中断时留下保护缺口。
DROP PROCEDURE IF EXISTS task11_refresh_history_subject_constraint;
DELIMITER $$
CREATE PROCEDURE task11_refresh_history_subject_constraint()
BEGIN
    CALL crm_require_migration_context('20260711_task11_organization_management');
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.TABLE_CONSTRAINTS
        WHERE CONSTRAINT_SCHEMA = DATABASE()
          AND TABLE_NAME = 't_authorization_history'
          AND CONSTRAINT_NAME = 'chk_authorization_history_subject'
    ) THEN
        ALTER TABLE t_authorization_history
            ADD CONSTRAINT chk_authorization_history_subject CHECK (
                subject_type IN ('ROLE', 'ROLE_PERMISSION', 'USER_ROLE', 'USER_PERMISSION',
                                 'ORGANIZATION_UNIT', 'POSITION',
                                 'ORGANIZATION_ASSIGNMENT', 'REPORTING_RELATION')
            );
    END IF;
END$$
DELIMITER ;
CALL task11_refresh_history_subject_constraint();
DROP PROCEDURE task11_refresh_history_subject_constraint;
CALL crm_migration_mark_step('20260711_task11_organization_management', 'HISTORY_SUBJECT_CONSTRAINT_READY');

SELECT p.code, p.sensitivity_level, p.delegable
FROM t_permission p
WHERE p.code IN ('organization:list', 'organization:view', 'organization:add', 'organization:edit',
                 'organization:status', 'position:list', 'position:add', 'position:edit',
                 'position:status', 'employee:assignment', 'employee:reporting')
ORDER BY p.code;
