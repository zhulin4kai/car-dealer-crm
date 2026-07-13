-- Task 16：个人资料并发版本和联系方式验证事实。
-- 存量手机和邮箱只能保留“未知/未验证”，迁移不伪造已验证事实。
CALL crm_require_migration_context('20260712_task16_profile');

DROP PROCEDURE IF EXISTS task16_apply;
DELIMITER $$
CREATE PROCEDURE task16_apply()
BEGIN
  CALL crm_require_migration_context('20260712_task16_profile');
  IF (SELECT COALESCE(CHARACTER_MAXIMUM_LENGTH,0) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='t_employee' AND COLUMN_NAME='avatar_url') < 500 THEN
    ALTER TABLE t_employee MODIFY COLUMN avatar_url varchar(500) NULL;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='t_employee' AND COLUMN_NAME='profile_version') THEN ALTER TABLE t_employee ADD profile_version int NOT NULL DEFAULT 0; END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='t_employee' AND COLUMN_NAME='phone_verified') THEN ALTER TABLE t_employee ADD phone_verified tinyint(1) NOT NULL DEFAULT 0; END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='t_employee' AND COLUMN_NAME='email_verified') THEN ALTER TABLE t_employee ADD email_verified tinyint(1) NOT NULL DEFAULT 0; END IF;
  IF (SELECT IS_NULLABLE FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='t_user' AND COLUMN_NAME='name')='NO' THEN ALTER TABLE t_user MODIFY COLUMN name varchar(64) NULL; END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='t_user' AND COLUMN_NAME='avatar_url') THEN ALTER TABLE t_user ADD avatar_url varchar(500) NULL COMMENT '系统账号个人头像'; END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='t_user' AND COLUMN_NAME='profile_version') THEN ALTER TABLE t_user ADD profile_version int NOT NULL DEFAULT 0 COMMENT '系统账号个人资料并发版本'; END IF;
END$$
DELIMITER ;
CALL task16_apply();
DROP PROCEDURE task16_apply;

DROP PROCEDURE IF EXISTS task16_constraint;
DELIMITER $$
CREATE PROCEDURE task16_constraint(IN p_table_name VARCHAR(64), IN p_constraint_name VARCHAR(64), IN p_definition_sql TEXT)
BEGIN
  CALL crm_require_migration_context('20260712_task16_profile');
  IF NOT EXISTS (SELECT 1 FROM information_schema.TABLE_CONSTRAINTS WHERE CONSTRAINT_SCHEMA=DATABASE() AND TABLE_NAME=p_table_name AND CONSTRAINT_NAME=p_constraint_name) THEN
    SET @ddl=CONCAT('ALTER TABLE ',p_table_name,' ADD CONSTRAINT ',p_definition_sql); PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
  END IF;
END$$
DELIMITER ;
CALL task16_constraint('t_employee','chk_employee_profile_version','chk_employee_profile_version CHECK (profile_version >= 0)');
CALL task16_constraint('t_employee','chk_employee_phone_verified','chk_employee_phone_verified CHECK (phone_verified IN (0,1))');
CALL task16_constraint('t_employee','chk_employee_email_verified','chk_employee_email_verified CHECK (email_verified IN (0,1))');
CALL task16_constraint('t_user','chk_user_profile_version','chk_user_profile_version CHECK (profile_version >= 0)');
DROP PROCEDURE task16_constraint;
CALL crm_migration_mark_step('20260712_task16_profile','PROFILE_SCHEMA_READY');
