-- Task 18: 用户管理工作台筛选索引、敏感资料权限与最后管理员共享锁。
-- 允许首次执行、完成后重跑及 DDL 中断后重跑；完成标记只在全部步骤成功后写入。

CALL crm_require_migration_context('20260712_task18_user_management_workspace');

DELIMITER $$
DROP PROCEDURE IF EXISTS migrate_task18_user_management_workspace$$
CREATE PROCEDURE migrate_task18_user_management_workspace()
BEGIN
  CALL crm_require_migration_context('20260712_task18_user_management_workspace');
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.statistics
    WHERE table_schema=DATABASE() AND table_name='t_user' AND index_name='idx_user_workspace_status'
  ) THEN
    ALTER TABLE t_user ADD INDEX idx_user_workspace_status(account_status,manual_locked,auto_locked_until,id);
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.statistics
    WHERE table_schema=DATABASE() AND table_name='t_user' AND index_name='idx_user_workspace_last_login'
  ) THEN
    ALTER TABLE t_user ADD INDEX idx_user_workspace_last_login(last_login_time,id);
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.statistics
    WHERE table_schema=DATABASE() AND table_name='t_employee_assignment'
      AND index_name='idx_employee_assignment_workspace_org'
  ) THEN
    ALTER TABLE t_employee_assignment
      ADD INDEX idx_employee_assignment_workspace_org
        (organization_unit_id,assignment_type,status,active_primary_marker,employee_id);
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.statistics
    WHERE table_schema=DATABASE() AND table_name='t_employee_assignment'
      AND index_name='idx_employee_assignment_workspace_position'
  ) THEN
    ALTER TABLE t_employee_assignment
      ADD INDEX idx_employee_assignment_workspace_position
        (position_id,assignment_type,status,active_primary_marker,employee_id);
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.statistics
    WHERE table_schema=DATABASE() AND table_name='t_employee_reporting'
      AND index_name='idx_employee_reporting_workspace_manager'
  ) THEN
    ALTER TABLE t_employee_reporting
      ADD INDEX idx_employee_reporting_workspace_manager
        (manager_employee_id,relation_type,status,active_direct_marker,subordinate_employee_id);
  END IF;

  INSERT INTO t_authorization_graph_lock(lock_name)
  VALUES('AVAILABLE_ADMIN_GUARD')
  ON DUPLICATE KEY UPDATE lock_name=VALUES(lock_name);

  INSERT INTO t_permission
    (name,code,url,type,parent_id,module,description,sensitivity_level,delegable,enabled)
  SELECT '用户管理-敏感资料查看','user:sensitive:view',NULL,'button',page.id,
         'user','查看受管用户联系方式和账号安全原因','SENSITIVE',0,1
  FROM t_permission page
  WHERE page.code='page:user:list'
    AND NOT EXISTS (SELECT 1 FROM t_permission existing WHERE existing.code='user:sensitive:view');

  INSERT INTO t_role_permission(role_id,permission_id,data_scope_code,delegable)
  SELECT role.id,permission.id,'GLOBAL',0
  FROM t_role role
  CROSS JOIN t_permission permission
  WHERE role.role='admin' AND permission.code='user:sensitive:view'
    AND NOT EXISTS (
      SELECT 1 FROM t_role_permission existing
      WHERE existing.role_id=role.id AND existing.permission_id=permission.id
    );

END$$

CALL migrate_task18_user_management_workspace()$$
DROP PROCEDURE migrate_task18_user_management_workspace$$
DELIMITER ;
CALL crm_migration_mark_step('20260712_task18_user_management_workspace', 'WORKSPACE_INDEXES_AND_PERMISSION_READY');
