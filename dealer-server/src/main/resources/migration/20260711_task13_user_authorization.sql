-- Task13：用户角色不可变事实、个人三态授权与认证版本并发控制。
-- 前置：Task03、09、10、11、12；只由 Task21 执行器按统一账本调度。

CALL crm_require_migration_context('20260711_task13_user_authorization');

DROP PROCEDURE IF EXISTS task13_add_authorization_version;
DELIMITER $$
CREATE PROCEDURE task13_add_authorization_version()
BEGIN
  CALL crm_require_migration_context('20260711_task13_user_authorization');
  IF NOT EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='t_user' AND COLUMN_NAME='authorization_version') THEN
    ALTER TABLE t_user ADD COLUMN authorization_version INT NOT NULL DEFAULT 0 COMMENT '授权配置并发版本，仅授权事实变化时递增' AFTER version;
  END IF;
  IF NOT EXISTS(SELECT 1 FROM information_schema.TABLE_CONSTRAINTS WHERE CONSTRAINT_SCHEMA=DATABASE() AND TABLE_NAME='t_user' AND CONSTRAINT_NAME='chk_user_authorization_version') THEN
    ALTER TABLE t_user ADD CONSTRAINT chk_user_authorization_version CHECK (authorization_version >= 0);
  END IF;
END$$
DELIMITER ;
CALL task13_add_authorization_version();
DROP PROCEDURE task13_add_authorization_version;
CALL crm_migration_mark_step('20260711_task13_user_authorization', 'AUTHORIZATION_VERSION_READY');

DROP PROCEDURE IF EXISTS task13_upgrade_user_role_fact;
DELIMITER $$
CREATE PROCEDURE task13_upgrade_user_role_fact()
BEGIN
  CALL crm_require_migration_context('20260711_task13_user_authorization');
  IF NOT EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='t_user_role' AND COLUMN_NAME='id') THEN
    ALTER TABLE t_user_role DROP PRIMARY KEY,
      ADD COLUMN id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY FIRST,
      ADD COLUMN active_marker TINYINT(1) NULL DEFAULT 1 AFTER effective_to,
      ADD COLUMN version INT NOT NULL DEFAULT 0 AFTER active_marker,
      ADD UNIQUE KEY uk_user_role_active(user_id,role_id,active_marker);
  END IF;
END$$
DELIMITER ;
CALL task13_upgrade_user_role_fact();
DROP PROCEDURE task13_upgrade_user_role_fact;

-- 当前态行 active_marker=1；关闭后 marker=NULL，原始授予人、原因和生效时间原样保留。
DROP PROCEDURE IF EXISTS task13_backfill_user_role_fact;
DELIMITER $$
CREATE PROCEDURE task13_backfill_user_role_fact()
BEGIN
  CALL crm_require_migration_context('20260711_task13_user_authorization');
  UPDATE t_user_role
SET active_marker=1,version=COALESCE(version,0)
WHERE NOT EXISTS (
    SELECT 1 FROM t_user_management_migration_step
    WHERE migration_key='20260711_task13_user_authorization'
      AND step_code='USER_ROLE_FACT_READY'
  )
  AND active_marker IS NULL AND effective_to IS NULL;
END$$
DELIMITER ;
CALL task13_backfill_user_role_fact();
DROP PROCEDURE task13_backfill_user_role_fact;
CALL crm_migration_mark_step('20260711_task13_user_authorization', 'USER_ROLE_FACT_READY');

DROP PROCEDURE IF EXISTS task13_create_user_permission_organization;
DELIMITER $$
CREATE PROCEDURE task13_create_user_permission_organization()
BEGIN
  CALL crm_require_migration_context('20260711_task13_user_authorization');
  CREATE TABLE IF NOT EXISTS t_user_permission_organization(
  user_permission_id BIGINT NOT NULL,
  organization_unit_id INT NOT NULL,
  PRIMARY KEY(user_permission_id,organization_unit_id),
  CONSTRAINT fk_user_permission_org_permission FOREIGN KEY(user_permission_id)
    REFERENCES t_user_permission(id) ON DELETE RESTRICT,
  CONSTRAINT fk_user_permission_org_unit FOREIGN KEY(organization_unit_id)
    REFERENCES t_organization_unit(id) ON DELETE RESTRICT
  );
END$$
DELIMITER ;
CALL task13_create_user_permission_organization();
DROP PROCEDURE task13_create_user_permission_organization;
CALL crm_migration_mark_step('20260711_task13_user_authorization', 'USER_PERMISSION_ORGANIZATION_READY');
