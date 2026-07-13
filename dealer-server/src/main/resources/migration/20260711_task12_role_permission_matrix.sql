-- Task12：角色目录、适用组织、权限矩阵和授权历史语义约束。
-- 前置：Task03/09/10/11。禁止物理删除角色、权限及其父对象。

CALL crm_require_migration_context('20260711_task12_role_permission_matrix');

DROP PROCEDURE IF EXISTS task12_add_column_if_missing;
DELIMITER $$
CREATE PROCEDURE task12_add_column_if_missing()
BEGIN
  CALL crm_require_migration_context('20260711_task12_role_permission_matrix');
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='t_role' AND COLUMN_NAME='scope_type') THEN
    ALTER TABLE t_role ADD COLUMN scope_type VARCHAR(16) NOT NULL DEFAULT 'GLOBAL' AFTER default_data_scope;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.TABLE_CONSTRAINTS WHERE CONSTRAINT_SCHEMA=DATABASE() AND TABLE_NAME='t_role' AND CONSTRAINT_NAME='chk_role_scope_type') THEN
    ALTER TABLE t_role ADD CONSTRAINT chk_role_scope_type CHECK (scope_type IN ('GLOBAL','ORGANIZATION'));
  END IF;
END$$
DELIMITER ;
CALL task12_add_column_if_missing();
DROP PROCEDURE task12_add_column_if_missing;

DROP PROCEDURE IF EXISTS task12_create_role_organization_tables;
DELIMITER $$
CREATE PROCEDURE task12_create_role_organization_tables()
BEGIN
  CALL crm_require_migration_context('20260711_task12_role_permission_matrix');
  CREATE TABLE IF NOT EXISTS t_role_organization(
  role_id INT NOT NULL,
  organization_unit_id INT NOT NULL,
  PRIMARY KEY(role_id, organization_unit_id),
  CONSTRAINT fk_role_organization_role FOREIGN KEY(role_id) REFERENCES t_role(id) ON DELETE RESTRICT,
  CONSTRAINT fk_role_organization_unit FOREIGN KEY(organization_unit_id) REFERENCES t_organization_unit(id) ON DELETE RESTRICT
  );

  CREATE TABLE IF NOT EXISTS t_role_permission_organization(
  role_id INT NOT NULL,
  permission_id INT NOT NULL,
  organization_unit_id INT NOT NULL,
  PRIMARY KEY(role_id,permission_id,organization_unit_id),
  CONSTRAINT fk_role_permission_org_permission FOREIGN KEY(role_id,permission_id)
    REFERENCES t_role_permission(role_id,permission_id) ON DELETE RESTRICT,
  CONSTRAINT fk_role_permission_org_unit FOREIGN KEY(organization_unit_id)
    REFERENCES t_organization_unit(id) ON DELETE RESTRICT
  );
END$$
DELIMITER ;
CALL task12_create_role_organization_tables();
DROP PROCEDURE task12_create_role_organization_tables;
CALL crm_migration_mark_step('20260711_task12_role_permission_matrix', 'ROLE_ORGANIZATION_TABLES_READY');

-- 只在首次尝试中从旧角色适用组织初始化 CUSTOM_ORGS。显式 resume 和完成后重跑绝不再次扩张权限级组织集合。
DROP PROCEDURE IF EXISTS task12_backfill_custom_orgs_once;
DELIMITER $$
CREATE PROCEDURE task12_backfill_custom_orgs_once()
BEGIN
  CALL crm_require_migration_context('20260711_task12_role_permission_matrix');
  IF NOT EXISTS (
    SELECT 1 FROM t_user_management_migration_step
    WHERE migration_key='20260711_task12_role_permission_matrix'
      AND step_code='CUSTOM_ORGS_FIRST_RUN_BACKFILL_READY'
  ) THEN
    INSERT INTO t_role_permission_organization(role_id,permission_id,organization_unit_id)
    SELECT permission.role_id,permission.permission_id,applicable.organization_unit_id
    FROM t_role_permission permission
    INNER JOIN t_role_organization applicable ON applicable.role_id=permission.role_id
    WHERE permission.data_scope_code='CUSTOM_ORGS'
      AND NOT EXISTS(
        SELECT 1 FROM t_role_permission_organization existing
        WHERE existing.role_id=permission.role_id AND existing.permission_id=permission.permission_id
          AND existing.organization_unit_id=applicable.organization_unit_id
      );
  END IF;
END$$
DELIMITER ;
CALL task12_backfill_custom_orgs_once();
DROP PROCEDURE task12_backfill_custom_orgs_once;
CALL crm_migration_mark_step('20260711_task12_role_permission_matrix', 'CUSTOM_ORGS_FIRST_RUN_BACKFILL_READY');

DROP PROCEDURE IF EXISTS task12_prepare_graph_locks;
DELIMITER $$
CREATE PROCEDURE task12_prepare_graph_locks()
BEGIN
  CALL crm_require_migration_context('20260711_task12_role_permission_matrix');
  CREATE TABLE IF NOT EXISTS t_authorization_graph_lock(lock_name VARCHAR(64) NOT NULL PRIMARY KEY);
  INSERT INTO t_authorization_graph_lock(lock_name) VALUES('ORGANIZATION_HIERARCHY') ON DUPLICATE KEY UPDATE lock_name=VALUES(lock_name);
  INSERT INTO t_authorization_graph_lock(lock_name) VALUES('REPORTING_GRAPH') ON DUPLICATE KEY UPDATE lock_name=VALUES(lock_name);
END$$
DELIMITER ;
CALL task12_prepare_graph_locks();
DROP PROCEDURE task12_prepare_graph_locks;

DROP PROCEDURE IF EXISTS task12_restrict_parent_fks;
DELIMITER $$
CREATE PROCEDURE task12_restrict_parent_fks()
BEGIN
  CALL crm_require_migration_context('20260711_task12_role_permission_matrix');
  IF NOT EXISTS(SELECT 1 FROM information_schema.REFERENTIAL_CONSTRAINTS WHERE CONSTRAINT_SCHEMA=DATABASE() AND CONSTRAINT_NAME='fk_role_permission_role') THEN
    ALTER TABLE t_role_permission ADD CONSTRAINT fk_role_permission_role FOREIGN KEY(role_id) REFERENCES t_role(id) ON DELETE RESTRICT;
  ELSEIF EXISTS(SELECT 1 FROM information_schema.REFERENTIAL_CONSTRAINTS WHERE CONSTRAINT_SCHEMA=DATABASE() AND CONSTRAINT_NAME='fk_role_permission_role' AND DELETE_RULE <> 'RESTRICT') THEN
    ALTER TABLE t_role_permission DROP FOREIGN KEY fk_role_permission_role,
      ADD CONSTRAINT fk_role_permission_role FOREIGN KEY(role_id) REFERENCES t_role(id) ON DELETE RESTRICT;
  END IF;
  IF NOT EXISTS(SELECT 1 FROM information_schema.REFERENTIAL_CONSTRAINTS WHERE CONSTRAINT_SCHEMA=DATABASE() AND CONSTRAINT_NAME='fk_role_permission_permission') THEN
    ALTER TABLE t_role_permission ADD CONSTRAINT fk_role_permission_permission FOREIGN KEY(permission_id) REFERENCES t_permission(id) ON DELETE RESTRICT;
  ELSEIF EXISTS(SELECT 1 FROM information_schema.REFERENTIAL_CONSTRAINTS WHERE CONSTRAINT_SCHEMA=DATABASE() AND CONSTRAINT_NAME='fk_role_permission_permission' AND DELETE_RULE <> 'RESTRICT') THEN
    ALTER TABLE t_role_permission DROP FOREIGN KEY fk_role_permission_permission,
      ADD CONSTRAINT fk_role_permission_permission FOREIGN KEY(permission_id) REFERENCES t_permission(id) ON DELETE RESTRICT;
  END IF;
  IF NOT EXISTS(SELECT 1 FROM information_schema.REFERENTIAL_CONSTRAINTS WHERE CONSTRAINT_SCHEMA=DATABASE() AND CONSTRAINT_NAME='fk_user_role_user') THEN
    ALTER TABLE t_user_role ADD CONSTRAINT fk_user_role_user FOREIGN KEY(user_id) REFERENCES t_user(id) ON DELETE RESTRICT;
  ELSEIF EXISTS(SELECT 1 FROM information_schema.REFERENTIAL_CONSTRAINTS WHERE CONSTRAINT_SCHEMA=DATABASE() AND CONSTRAINT_NAME='fk_user_role_user' AND DELETE_RULE <> 'RESTRICT') THEN
    ALTER TABLE t_user_role DROP FOREIGN KEY fk_user_role_user,
      ADD CONSTRAINT fk_user_role_user FOREIGN KEY(user_id) REFERENCES t_user(id) ON DELETE RESTRICT;
  END IF;
  IF NOT EXISTS(SELECT 1 FROM information_schema.REFERENTIAL_CONSTRAINTS WHERE CONSTRAINT_SCHEMA=DATABASE() AND CONSTRAINT_NAME='fk_user_role_role') THEN
    ALTER TABLE t_user_role ADD CONSTRAINT fk_user_role_role FOREIGN KEY(role_id) REFERENCES t_role(id) ON DELETE RESTRICT;
  ELSEIF EXISTS(SELECT 1 FROM information_schema.REFERENTIAL_CONSTRAINTS WHERE CONSTRAINT_SCHEMA=DATABASE() AND CONSTRAINT_NAME='fk_user_role_role' AND DELETE_RULE <> 'RESTRICT') THEN
    ALTER TABLE t_user_role DROP FOREIGN KEY fk_user_role_role,
      ADD CONSTRAINT fk_user_role_role FOREIGN KEY(role_id) REFERENCES t_role(id) ON DELETE RESTRICT;
  END IF;
END$$
DELIMITER ;
CALL task12_restrict_parent_fks();
DROP PROCEDURE task12_restrict_parent_fks;
CALL crm_migration_mark_step('20260711_task12_role_permission_matrix', 'PARENT_FOREIGN_KEYS_READY');

DROP PROCEDURE IF EXISTS task12_add_history_checks;
DELIMITER $$
CREATE PROCEDURE task12_add_history_checks()
BEGIN
CALL crm_require_migration_context('20260711_task12_role_permission_matrix');
IF NOT EXISTS(SELECT 1 FROM information_schema.TABLE_CONSTRAINTS WHERE CONSTRAINT_SCHEMA=DATABASE() AND TABLE_NAME='t_authorization_history' AND CONSTRAINT_NAME='chk_authorization_history_subject_ids') THEN
ALTER TABLE t_authorization_history ADD CONSTRAINT chk_authorization_history_subject_ids CHECK (
    (subject_type <> 'ROLE' OR role_id IS NOT NULL) AND
    (subject_type <> 'ROLE_PERMISSION' OR (role_id IS NOT NULL AND permission_id IS NOT NULL)) AND
    (subject_type <> 'USER_ROLE' OR (target_user_id IS NOT NULL AND role_id IS NOT NULL)) AND
    (subject_type <> 'USER_PERMISSION' OR (target_user_id IS NOT NULL AND permission_id IS NOT NULL AND effect IS NOT NULL))
  );
END IF;
IF NOT EXISTS(SELECT 1 FROM information_schema.TABLE_CONSTRAINTS WHERE CONSTRAINT_SCHEMA=DATABASE() AND TABLE_NAME='t_authorization_history' AND CONSTRAINT_NAME='chk_authorization_history_user_permission_scope') THEN
ALTER TABLE t_authorization_history ADD CONSTRAINT chk_authorization_history_user_permission_scope CHECK (
    subject_type <> 'USER_PERMISSION' OR
    (effect='GRANT' AND data_scope_code IS NOT NULL) OR
    (effect='DENY' AND data_scope_code IS NULL)
  );
END IF;
END$$
DELIMITER ;
CALL task12_add_history_checks();
DROP PROCEDURE task12_add_history_checks;
CALL crm_migration_mark_step('20260711_task12_role_permission_matrix', 'AUTHORIZATION_HISTORY_CHECKS_READY');

-- 角色与权限目录种子使用稳定 code 幂等补齐；只给恢复管理员补新权限。
DROP PROCEDURE IF EXISTS task12_seed_role_permission_catalog;
DELIMITER $$
CREATE PROCEDURE task12_seed_role_permission_catalog()
BEGIN
  CALL crm_require_migration_context('20260711_task12_role_permission_matrix');
  INSERT INTO t_permission(name,code,url,type,parent_id,order_no,icon,module,description,sensitivity_level,delegable,enabled,version)
SELECT '权限管理','menu:access',NULL,'menu',NULL,3,'Shield','access','权限管理','NORMAL',1,1,0
WHERE NOT EXISTS(SELECT 1 FROM t_permission WHERE code='menu:access');
INSERT INTO t_permission(name,code,url,type,parent_id,order_no,icon,module,description,sensitivity_level,delegable,enabled,version)
SELECT '角色管理','page:role:list','/dashboard/role','menu',parent.id,1,'Users','access','角色管理','NORMAL',1,1,0
FROM t_permission parent WHERE parent.code='menu:access' AND NOT EXISTS(SELECT 1 FROM t_permission WHERE code='page:role:list');
INSERT INTO t_permission(name,code,url,type,parent_id,order_no,icon,module,description,sensitivity_level,delegable,enabled,version)
SELECT '权限目录','page:permission:list','/dashboard/permission','menu',parent.id,2,'KeyRound','access','权限目录','NORMAL',1,1,0
FROM t_permission parent WHERE parent.code='menu:access' AND NOT EXISTS(SELECT 1 FROM t_permission WHERE code='page:permission:list');

DROP TEMPORARY TABLE IF EXISTS task12_permission_seed;
CREATE TEMPORARY TABLE task12_permission_seed(code VARCHAR(64) PRIMARY KEY,name VARCHAR(64),sensitivity VARCHAR(16),delegable TINYINT);
INSERT INTO task12_permission_seed VALUES
('role:list','角色-列表','NORMAL',1),('role:view','角色-查看','NORMAL',1),
('role:add','角色-新增','NORMAL',1),('role:edit','角色-编辑','NORMAL',1),
('role:copy','角色-复制','NORMAL',1),('role:status','角色-状态','PROTECTED',0),
('role:permission:manage','角色-权限矩阵','PROTECTED',0);
  INSERT INTO t_permission(name,code,url,type,parent_id,module,description,sensitivity_level,delegable,enabled,version)
SELECT seed.name,seed.code,NULL,'button',parent.id,'access',seed.name,seed.sensitivity,seed.delegable,1,0
FROM task12_permission_seed seed CROSS JOIN t_permission parent
WHERE parent.code='page:role:list' AND NOT EXISTS(SELECT 1 FROM t_permission existing WHERE existing.code=seed.code);
DROP TEMPORARY TABLE task12_permission_seed;
INSERT INTO t_permission(name,code,url,type,parent_id,module,description,sensitivity_level,delegable,enabled,version)
SELECT '权限目录-列表','permission:list',NULL,'button',parent.id,'access','权限目录-列表','NORMAL',1,1,0
FROM t_permission parent WHERE parent.code='page:permission:list' AND NOT EXISTS(SELECT 1 FROM t_permission WHERE code='permission:list');
INSERT INTO t_permission(name,code,url,type,parent_id,module,description,sensitivity_level,delegable,enabled,version)
SELECT '用户管理-个人权限','user:permission',NULL,'button',parent.id,'user','用户管理-个人权限','PROTECTED',0,1,0
FROM t_permission parent WHERE parent.code='page:user:list' AND NOT EXISTS(SELECT 1 FROM t_permission WHERE code='user:permission');
END$$
DELIMITER ;
CALL task12_seed_role_permission_catalog();
DROP PROCEDURE task12_seed_role_permission_catalog;

DROP PROCEDURE IF EXISTS task12_normalize_permission_metadata_once;
DELIMITER $$
CREATE PROCEDURE task12_normalize_permission_metadata_once()
BEGIN
  CALL crm_require_migration_context('20260711_task12_role_permission_matrix');
  IF NOT EXISTS (
    SELECT 1 FROM t_user_management_migration_step
    WHERE migration_key='20260711_task12_role_permission_matrix'
      AND step_code='PERMISSION_METADATA_FIRST_RUN_BACKFILL_READY'
  ) THEN
    UPDATE t_permission
    SET module=CASE WHEN code='user:permission' THEN 'user' ELSE 'access' END,
        sensitivity_level=CASE WHEN code IN ('role:status','role:permission:manage','user:permission') THEN 'PROTECTED' ELSE 'NORMAL' END,
        delegable=CASE WHEN code IN ('role:status','role:permission:manage','user:permission') THEN 0 ELSE 1 END
    WHERE code IN ('role:list','role:view','role:add','role:edit','role:copy','role:status',
                   'role:permission:manage','permission:list','user:permission');
  END IF;
END$$
DELIMITER ;
CALL task12_normalize_permission_metadata_once();
DROP PROCEDURE task12_normalize_permission_metadata_once;
CALL crm_migration_mark_step('20260711_task12_role_permission_matrix', 'PERMISSION_METADATA_FIRST_RUN_BACKFILL_READY');

DROP PROCEDURE IF EXISTS task12_seed_recovery_role_permissions;
DELIMITER $$
CREATE PROCEDURE task12_seed_recovery_role_permissions()
BEGIN
  CALL crm_require_migration_context('20260711_task12_role_permission_matrix');
  INSERT INTO t_role_permission(role_id,permission_id,delegable,data_scope_code)
SELECT role_record.id,permission_record.id,permission_record.delegable,'GLOBAL'
FROM t_role role_record CROSS JOIN t_permission permission_record
WHERE role_record.role='admin' AND permission_record.code IN
('menu:access','page:role:list','page:permission:list','role:list','role:view','role:add','role:edit','role:copy','role:status','role:permission:manage','permission:list','user:permission')
AND NOT EXISTS(SELECT 1 FROM t_role_permission existing WHERE existing.role_id=role_record.id AND existing.permission_id=permission_record.id);
END$$
DELIMITER ;
CALL task12_seed_recovery_role_permissions();
DROP PROCEDURE task12_seed_recovery_role_permissions;

CALL crm_migration_mark_step('20260711_task12_role_permission_matrix', 'ROLE_PERMISSION_CATALOG_READY');

-- 完成后核验未知/停用权限关系为0、角色适用组织孤儿为0、四个父外键 DELETE_RULE 均为 RESTRICT。
