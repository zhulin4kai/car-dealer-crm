-- Task 15：账号邀请、凭证生命周期和独立锁定事实。
-- 只允许由 Task21 迁移执行器调用；每个 DDL 都按对象定义恢复，可从中断点显式 resume。
CALL crm_require_migration_context('20260712_task15_credential_lifecycle');

DROP PROCEDURE IF EXISTS task15_add_column;
DELIMITER $$
CREATE PROCEDURE task15_add_column(IN p_column_name VARCHAR(64), IN p_definition_sql TEXT)
BEGIN
  CALL crm_require_migration_context('20260712_task15_credential_lifecycle');
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='t_user' AND COLUMN_NAME=p_column_name) THEN
    SET @ddl=CONCAT('ALTER TABLE t_user ADD COLUMN ', p_definition_sql);
    PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
  END IF;
END$$
DELIMITER ;
CALL task15_add_column('account_status', 'account_status varchar(16) NOT NULL DEFAULT ''ACTIVE''');
CALL task15_add_column('must_change_password', 'must_change_password tinyint(1) NOT NULL DEFAULT 0');
CALL task15_add_column('failed_login_count', 'failed_login_count int NOT NULL DEFAULT 0');
CALL task15_add_column('auto_locked_until', 'auto_locked_until datetime NULL');
CALL task15_add_column('manual_locked', 'manual_locked tinyint(1) NOT NULL DEFAULT 0');
CALL task15_add_column('manual_lock_reason', 'manual_lock_reason varchar(500) NULL');
CALL task15_add_column('manual_locked_by', 'manual_locked_by int NULL');
CALL task15_add_column('manual_locked_at', 'manual_locked_at datetime NULL');
CALL task15_add_column('account_expires_at', 'account_expires_at datetime NULL');
CALL task15_add_column('password_expires_at', 'password_expires_at datetime NULL');
DROP PROCEDURE task15_add_column;

-- 旧认证字段仍参与登录判断；首次升级必须把旧禁用/锁定事实保真映射到新工作台字段。
-- 已完成本步骤后 resume 不得再次覆盖后续人工状态调整。
SET @task15_legacy_state_backfill_required = NOT EXISTS (
  SELECT 1 FROM t_user_management_migration_step
  WHERE migration_key='20260712_task15_credential_lifecycle'
    AND step_code='LEGACY_ACCOUNT_STATE_BACKFILL_READY'
);
DROP PROCEDURE IF EXISTS task15_backfill_legacy_account_state;
DELIMITER $$
CREATE PROCEDURE task15_backfill_legacy_account_state()
BEGIN
  CALL crm_require_migration_context('20260712_task15_credential_lifecycle');
  START TRANSACTION;
  UPDATE t_user
SET account_status = CASE WHEN COALESCE(account_enabled, 0) = 1 THEN 'ACTIVE' ELSE 'DISABLED' END,
    manual_locked = CASE WHEN COALESCE(account_no_locked, 0) = 1 THEN 0 ELSE 1 END,
    manual_lock_reason = CASE
      WHEN COALESCE(account_no_locked, 0) = 1 THEN NULL
      ELSE COALESCE(manual_lock_reason, '旧账号锁定状态迁移：原操作人和时间未记录')
    END,
    manual_locked_by = CASE WHEN COALESCE(account_no_locked, 0) = 1 THEN NULL ELSE manual_locked_by END,
    manual_locked_at = CASE WHEN COALESCE(account_no_locked, 0) = 1 THEN NULL ELSE manual_locked_at END
  WHERE @task15_legacy_state_backfill_required;
  COMMIT;
END$$
DELIMITER ;
CALL task15_backfill_legacy_account_state();
DROP PROCEDURE task15_backfill_legacy_account_state;
CALL crm_migration_mark_step('20260712_task15_credential_lifecycle','LEGACY_ACCOUNT_STATE_BACKFILL_READY');

DROP PROCEDURE IF EXISTS task15_add_constraint;
DELIMITER $$
CREATE PROCEDURE task15_add_constraint(IN p_table_name VARCHAR(64), IN p_constraint_name VARCHAR(64), IN p_definition_sql TEXT)
BEGIN
  CALL crm_require_migration_context('20260712_task15_credential_lifecycle');
  IF NOT EXISTS (SELECT 1 FROM information_schema.TABLE_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA=DATABASE() AND TABLE_NAME=p_table_name AND CONSTRAINT_NAME=p_constraint_name) THEN
    SET @ddl=CONCAT('ALTER TABLE ', p_table_name, ' ADD CONSTRAINT ', p_definition_sql);
    PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
  END IF;
END$$
DELIMITER ;
CALL task15_add_constraint('t_user','chk_user_account_status','chk_user_account_status CHECK (account_status IN (''INVITED'',''ACTIVE'',''DISABLED''))');
CALL task15_add_constraint('t_user','chk_user_must_change_password','chk_user_must_change_password CHECK (must_change_password IN (0,1))');
CALL task15_add_constraint('t_user','chk_user_failed_login_count','chk_user_failed_login_count CHECK (failed_login_count >= 0)');
CALL task15_add_constraint('t_user','chk_user_manual_locked','chk_user_manual_locked CHECK (manual_locked IN (0,1))');
CALL crm_migration_mark_step('20260712_task15_credential_lifecycle','USER_CREDENTIAL_COLUMNS_READY');

DROP PROCEDURE IF EXISTS task15_create_credential_tables;
DELIMITER $$
CREATE PROCEDURE task15_create_credential_tables()
BEGIN
  CALL crm_require_migration_context('20260712_task15_credential_lifecycle');
  CREATE TABLE IF NOT EXISTS t_account_credential (
  id bigint NOT NULL AUTO_INCREMENT PRIMARY KEY, user_id int NOT NULL, purpose varchar(24) NOT NULL,
  token_digest varchar(64) NOT NULL, status varchar(16) NOT NULL, active_marker tinyint(1) NULL,
  expires_at datetime NOT NULL, consumed_at datetime NULL, revoked_at datetime NULL, issued_by int NULL,
  reason varchar(500) NOT NULL, version int NOT NULL DEFAULT 0, create_time datetime NOT NULL,
  UNIQUE KEY uk_account_credential_digest(token_digest), UNIQUE KEY uk_account_credential_active(user_id,purpose,active_marker),
  CONSTRAINT fk_account_credential_user FOREIGN KEY(user_id) REFERENCES t_user(id) ON DELETE RESTRICT,
  CONSTRAINT chk_account_credential_purpose CHECK (purpose IN ('INVITATION','SELF_RESET','ADMIN_RESET')),
  CONSTRAINT chk_account_credential_status CHECK (status IN ('ISSUED','CONSUMED','REVOKED')),
  CONSTRAINT chk_account_credential_version CHECK (version >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
  CREATE TABLE IF NOT EXISTS t_login_identifier (
  id bigint NOT NULL AUTO_INCREMENT PRIMARY KEY, user_id int NOT NULL, login_act varchar(32) NOT NULL,
  status varchar(16) NOT NULL, active_marker tinyint(1) NULL, retired_at datetime NULL,
  changed_by int NULL, reason varchar(500) NOT NULL, version int NOT NULL DEFAULT 0, create_time datetime NOT NULL,
  UNIQUE KEY uk_login_identifier_login_act(login_act), UNIQUE KEY uk_login_identifier_active_user(user_id,active_marker),
  CONSTRAINT fk_login_identifier_user FOREIGN KEY(user_id) REFERENCES t_user(id) ON DELETE RESTRICT,
  CONSTRAINT fk_login_identifier_changed_by FOREIGN KEY(changed_by) REFERENCES t_user(id) ON DELETE RESTRICT,
  CONSTRAINT chk_login_identifier_state CHECK (
    (status='ACTIVE' AND active_marker=1 AND retired_at IS NULL)
    OR (status='RETIRED' AND active_marker IS NULL AND retired_at IS NOT NULL)
  ),
  CONSTRAINT chk_login_identifier_version CHECK (version >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
END$$
DELIMITER ;
CALL task15_create_credential_tables();
DROP PROCEDURE task15_create_credential_tables;
CALL task15_add_constraint('t_account_credential','chk_account_credential_purpose','chk_account_credential_purpose CHECK (purpose IN (''INVITATION'',''SELF_RESET'',''ADMIN_RESET''))');
CALL task15_add_constraint('t_account_credential','chk_account_credential_status','chk_account_credential_status CHECK (status IN (''ISSUED'',''CONSUMED'',''REVOKED''))');
CALL task15_add_constraint('t_account_credential','chk_account_credential_version','chk_account_credential_version CHECK (version >= 0)');
CALL task15_add_constraint('t_login_identifier','uk_login_identifier_login_act','uk_login_identifier_login_act UNIQUE (login_act)');
CALL task15_add_constraint('t_login_identifier','uk_login_identifier_active_user','uk_login_identifier_active_user UNIQUE (user_id,active_marker)');
CALL task15_add_constraint('t_login_identifier','fk_login_identifier_user','fk_login_identifier_user FOREIGN KEY (user_id) REFERENCES t_user(id) ON DELETE RESTRICT');
CALL task15_add_constraint('t_login_identifier','fk_login_identifier_changed_by','fk_login_identifier_changed_by FOREIGN KEY (changed_by) REFERENCES t_user(id) ON DELETE RESTRICT');
CALL task15_add_constraint('t_login_identifier','chk_login_identifier_state','chk_login_identifier_state CHECK ((status=''ACTIVE'' AND active_marker=1 AND retired_at IS NULL) OR (status=''RETIRED'' AND active_marker IS NULL AND retired_at IS NOT NULL))');
CALL task15_add_constraint('t_login_identifier','chk_login_identifier_version','chk_login_identifier_version CHECK (version >= 0)');

DROP PROCEDURE IF EXISTS task15_prepare_login_identifier_guard;
DELIMITER $$
CREATE PROCEDURE task15_prepare_login_identifier_guard()
BEGIN
  CALL crm_require_migration_context('20260712_task15_credential_lifecycle');
  INSERT INTO t_authorization_graph_lock(lock_name)
  VALUES('LOGIN_IDENTIFIER_GUARD')
  ON DUPLICATE KEY UPDATE lock_name=VALUES(lock_name);
END$$
DELIMITER ;
CALL task15_prepare_login_identifier_guard();
DROP PROCEDURE task15_prepare_login_identifier_guard;
CALL crm_migration_mark_step('20260712_task15_credential_lifecycle','LOGIN_IDENTIFIER_GUARD_READY');

-- 首次升级把所有现有 t_user.login_act 固化为不可转让的 ACTIVE 归属事实。
-- resume 仅补齐缺失行；任何既有归属冲突都必须 fail-close，不能用 IGNORE/覆盖掩盖历史污染。
SET @task15_login_identifier_backfill_required = NOT EXISTS (
  SELECT 1 FROM t_user_management_migration_step
  WHERE migration_key='20260712_task15_credential_lifecycle'
    AND step_code='LOGIN_IDENTIFIER_BACKFILL_READY'
);
DROP PROCEDURE IF EXISTS task15_backfill_login_identifiers;
DELIMITER $$
CREATE PROCEDURE task15_backfill_login_identifiers()
BEGIN
  DECLARE v_conflict_count BIGINT DEFAULT 0;
  CALL crm_require_migration_context('20260712_task15_credential_lifecycle');
  START TRANSACTION;
  IF @task15_login_identifier_backfill_required THEN
    SELECT COUNT(*) INTO v_conflict_count FROM t_user WHERE login_act IS NULL OR TRIM(login_act)='';
    IF v_conflict_count <> 0 THEN
      SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='现有用户存在空登录账号，不能建立永久归属事实';
    END IF;
    SELECT COUNT(*) INTO v_conflict_count
    FROM t_user u INNER JOIN t_login_identifier li ON li.login_act=u.login_act
    WHERE li.user_id<>u.id OR li.status<>'ACTIVE' OR li.active_marker<>1 OR li.retired_at IS NOT NULL;
    IF v_conflict_count <> 0 THEN
      SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='登录账号已存在不一致归属，禁止覆盖迁移';
    END IF;
    SELECT COUNT(*) INTO v_conflict_count
    FROM t_user u INNER JOIN t_login_identifier li ON li.user_id=u.id AND li.active_marker=1
    WHERE li.login_act<>u.login_act OR li.status<>'ACTIVE' OR li.retired_at IS NOT NULL;
    IF v_conflict_count <> 0 THEN
      SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='用户已存在不一致当前账号，禁止覆盖迁移';
    END IF;
    INSERT INTO t_login_identifier
      (user_id,login_act,status,active_marker,retired_at,changed_by,reason,version,create_time)
    SELECT u.id,u.login_act,'ACTIVE',1,NULL,NULL,'旧账号登录标识迁移',0,COALESCE(u.create_time,NOW())
    FROM t_user u LEFT JOIN t_login_identifier li ON li.login_act=u.login_act
    WHERE li.id IS NULL;
    SELECT COUNT(*) INTO v_conflict_count
    FROM t_user u LEFT JOIN t_login_identifier li
      ON li.user_id=u.id AND li.login_act=u.login_act AND li.status='ACTIVE'
      AND li.active_marker=1 AND li.retired_at IS NULL
    WHERE li.id IS NULL;
    IF v_conflict_count <> 0 THEN
      SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='登录账号永久归属回填不完整';
    END IF;
  END IF;
  COMMIT;
END$$
DELIMITER ;
CALL task15_backfill_login_identifiers();
DROP PROCEDURE task15_backfill_login_identifiers;
CALL crm_migration_mark_step('20260712_task15_credential_lifecycle','LOGIN_IDENTIFIER_BACKFILL_READY');

DROP PROCEDURE IF EXISTS task15_create_password_history;
DELIMITER $$
CREATE PROCEDURE task15_create_password_history()
BEGIN
  CALL crm_require_migration_context('20260712_task15_credential_lifecycle');
  CREATE TABLE IF NOT EXISTS t_password_history (
  id bigint NOT NULL AUTO_INCREMENT PRIMARY KEY, user_id int NOT NULL, password_hash varchar(64) NOT NULL,
  changed_by int NULL, change_reason varchar(64) NOT NULL, changed_at datetime NOT NULL,
  KEY idx_password_history_user_time(user_id,changed_at,id),
  CONSTRAINT fk_password_history_user FOREIGN KEY(user_id) REFERENCES t_user(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
END$$
DELIMITER ;
CALL task15_create_password_history();
DROP PROCEDURE task15_create_password_history;
DROP PROCEDURE task15_add_constraint;
CALL crm_migration_mark_step('20260712_task15_credential_lifecycle','CREDENTIAL_TABLES_READY');
