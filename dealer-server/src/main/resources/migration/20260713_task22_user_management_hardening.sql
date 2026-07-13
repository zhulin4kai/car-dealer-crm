-- Task 22：用户管理终审安全加固。
-- 扩展临时凭证用途和联系方式版本绑定，并在数据库层保护永久登录标识归属和固定恢复账号身份。

CALL crm_require_migration_context('20260713_task22_user_management_hardening');

DROP PROCEDURE IF EXISTS task22_prepare_credential_and_recovery_constraints;
DELIMITER $$
CREATE PROCEDURE task22_prepare_credential_and_recovery_constraints()
BEGIN
  CALL crm_require_migration_context('20260713_task22_user_management_hardening');
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='t_account_credential'
      AND COLUMN_NAME='target_value_digest'
  ) THEN
    ALTER TABLE t_account_credential
      ADD COLUMN target_value_digest VARCHAR(64) NULL COMMENT '联系方式验证目标的HMAC摘要，不保存明文' AFTER reason;
  END IF;
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='t_account_credential'
      AND COLUMN_NAME='target_profile_version'
  ) THEN
    ALTER TABLE t_account_credential
      ADD COLUMN target_profile_version INT NULL COMMENT '联系方式验证签发时的员工资料版本' AFTER target_value_digest;
  END IF;

  -- Task22 前签发的联系方式凭证没有目标绑定，不能继续消费；保留历史行但统一撤销。
  UPDATE t_account_credential
  SET status='REVOKED',active_marker=NULL,revoked_at=COALESCE(revoked_at,NOW()),version=version+1
  WHERE purpose IN ('PHONE_VERIFY','EMAIL_VERIFY') AND status='ISSUED'
    AND (target_value_digest IS NULL OR target_profile_version IS NULL);

  IF EXISTS (
    SELECT 1 FROM information_schema.TABLE_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA=DATABASE() AND TABLE_NAME='t_account_credential'
      AND CONSTRAINT_NAME='chk_account_credential_purpose'
  ) THEN
    IF LOCATE('MariaDB', VERSION()) > 0 THEN
      SET @task22_drop_purpose_check='ALTER TABLE t_account_credential DROP CONSTRAINT chk_account_credential_purpose';
    ELSE
      SET @task22_drop_purpose_check='ALTER TABLE t_account_credential DROP CHECK chk_account_credential_purpose';
    END IF;
    PREPARE task22_stmt FROM @task22_drop_purpose_check;
    EXECUTE task22_stmt;
    DEALLOCATE PREPARE task22_stmt;
  END IF;
  ALTER TABLE t_account_credential ADD CONSTRAINT chk_account_credential_purpose
    CHECK (purpose IN ('INVITATION','SELF_RESET','ADMIN_RESET','PHONE_VERIFY','EMAIL_VERIFY','BREAK_GLASS'));

  IF EXISTS (
    SELECT 1 FROM information_schema.TABLE_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA=DATABASE() AND TABLE_NAME='t_account_credential'
      AND CONSTRAINT_NAME='chk_account_credential_contact_binding'
  ) THEN
    IF LOCATE('MariaDB', VERSION()) > 0 THEN
      SET @task22_drop_contact_check='ALTER TABLE t_account_credential DROP CONSTRAINT chk_account_credential_contact_binding';
    ELSE
      SET @task22_drop_contact_check='ALTER TABLE t_account_credential DROP CHECK chk_account_credential_contact_binding';
    END IF;
    PREPARE task22_contact_stmt FROM @task22_drop_contact_check;
    EXECUTE task22_contact_stmt;
    DEALLOCATE PREPARE task22_contact_stmt;
  END IF;
  ALTER TABLE t_account_credential ADD CONSTRAINT chk_account_credential_contact_binding
    CHECK (
      (purpose IN ('PHONE_VERIFY','EMAIL_VERIFY')
        AND (status<>'ISSUED' OR (target_value_digest IS NOT NULL AND target_profile_version IS NOT NULL)))
      OR
      (purpose NOT IN ('PHONE_VERIFY','EMAIL_VERIFY')
        AND target_value_digest IS NULL AND target_profile_version IS NULL)
    );

  IF (SELECT COUNT(*) FROM t_organization_unit
      WHERE type='COMPANY' AND parent_id IS NULL
        AND migration_placeholder=0 AND enabled=1) > 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Task22要求最多一个启用的非占位根公司';
  END IF;
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='t_organization_unit'
      AND COLUMN_NAME='active_root_marker'
  ) THEN
    IF LOCATE('MariaDB', VERSION()) > 0 THEN
      SET @task22_add_root_marker='ALTER TABLE t_organization_unit ADD COLUMN active_root_marker TINYINT GENERATED ALWAYS AS (CASE WHEN type=''COMPANY'' AND parent_id IS NULL AND migration_placeholder=0 AND enabled=1 THEN 1 ELSE NULL END) PERSISTENT';
    ELSE
      SET @task22_add_root_marker='ALTER TABLE t_organization_unit ADD COLUMN active_root_marker TINYINT GENERATED ALWAYS AS (CASE WHEN type=''COMPANY'' AND parent_id IS NULL AND migration_placeholder=0 AND enabled=1 THEN 1 ELSE NULL END) STORED';
    END IF;
    PREPARE task22_root_marker_stmt FROM @task22_add_root_marker;
    EXECUTE task22_root_marker_stmt;
    DEALLOCATE PREPARE task22_root_marker_stmt;
  END IF;
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='t_organization_unit'
      AND INDEX_NAME='uk_organization_unit_active_root'
  ) THEN
    ALTER TABLE t_organization_unit
      ADD UNIQUE INDEX uk_organization_unit_active_root(active_root_marker);
  END IF;
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.TABLE_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA=DATABASE() AND TABLE_NAME='t_organization_unit'
      AND CONSTRAINT_NAME='chk_organization_unit_hierarchy'
  ) THEN
    ALTER TABLE t_organization_unit ADD CONSTRAINT chk_organization_unit_hierarchy
      CHECK ((type='COMPANY' AND parent_id IS NULL) OR (type<>'COMPANY' AND parent_id IS NOT NULL));
  END IF;

  IF (SELECT COUNT(*) FROM t_user
      WHERE id=1 AND BINARY login_act=BINARY 'admin'
        AND account_type='SYSTEM' AND protected_account=1) <> 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Task22要求唯一固定恢复账号为 id=1/admin/SYSTEM/protected';
  END IF;
  UPDATE t_user
  SET login_pwd='$2y$12$s4SOuAYn1qhEjBjKwvawR.djU.vjb4DIVZbsdZfLi.idWdyGinyCS',
      must_change_password=1,
      manual_locked=1,
      manual_lock_reason='INITIAL_BREAK_GLASS_REQUIRED',
      account_no_locked=0,
      last_login_time=NULL,
      auth_version=auth_version+1,
      version=version+1
  WHERE id=1 AND BINARY login_act=BINARY 'admin'
    AND account_type='SYSTEM' AND protected_account=1;
  IF EXISTS (
    SELECT 1 FROM information_schema.TABLE_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA=DATABASE() AND TABLE_NAME='t_user'
      AND CONSTRAINT_NAME='chk_user_recovery_login_act'
  ) THEN
    IF LOCATE('MariaDB', VERSION()) > 0 THEN
      SET @task22_drop_recovery_check='ALTER TABLE t_user DROP CONSTRAINT chk_user_recovery_login_act';
    ELSE
      SET @task22_drop_recovery_check='ALTER TABLE t_user DROP CHECK chk_user_recovery_login_act';
    END IF;
    PREPARE task22_recovery_stmt FROM @task22_drop_recovery_check;
    EXECUTE task22_recovery_stmt;
    DEALLOCATE PREPARE task22_recovery_stmt;
  END IF;
  ALTER TABLE t_user ADD CONSTRAINT chk_user_recovery_login_act
    CHECK (
      (protected_account=1 AND login_act IS NOT NULL AND LOWER(login_act)='admin')
      OR
      (protected_account=0 AND (login_act IS NULL OR LOWER(login_act)<>'admin'))
    );
END$$
DELIMITER ;
CALL task22_prepare_credential_and_recovery_constraints();
DROP PROCEDURE task22_prepare_credential_and_recovery_constraints;
CALL crm_migration_mark_step('20260713_task22_user_management_hardening','CREDENTIAL_AND_RECOVERY_CONSTRAINTS_READY');

/* CRM_MIGRATION_RUNNER_PAYLOAD_BEGIN 20260713_task22_user_management_hardening
CALL crm_require_migration_context('20260713_task22_user_management_hardening');
DROP TRIGGER IF EXISTS trg_login_identifier_immutable_bu;
DROP TRIGGER IF EXISTS trg_login_identifier_immutable_bd;
DROP TRIGGER IF EXISTS trg_recovery_account_identity_bi;
DROP TRIGGER IF EXISTS trg_recovery_account_identity_bu;
DROP TRIGGER IF EXISTS trg_recovery_account_identity_bd;

DELIMITER $$
CREATE TRIGGER trg_login_identifier_immutable_bu
BEFORE UPDATE ON t_login_identifier
FOR EACH ROW
BEGIN
  IF NOT (NEW.user_id <=> OLD.user_id)
     OR NOT (NEW.login_act <=> OLD.login_act)
     OR NOT (NEW.create_time <=> OLD.create_time) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='登录标识永久归属字段禁止修改';
  END IF;
END$$

CREATE TRIGGER trg_login_identifier_immutable_bd
BEFORE DELETE ON t_login_identifier
FOR EACH ROW
BEGIN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='登录标识永久归属事实禁止删除';
END$$

CREATE TRIGGER trg_recovery_account_identity_bi
BEFORE INSERT ON t_user
FOR EACH ROW
BEGIN
  IF NEW.id=1 OR BINARY NEW.login_act=BINARY 'admin' OR NEW.protected_account=1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='固定恢复账号身份禁止新增或复制';
  END IF;
END$$

CREATE TRIGGER trg_recovery_account_identity_bu
BEFORE UPDATE ON t_user
FOR EACH ROW
BEGIN
  IF OLD.id=1 OR NEW.id=1
     OR BINARY OLD.login_act=BINARY 'admin' OR BINARY NEW.login_act=BINARY 'admin'
     OR OLD.protected_account=1 OR NEW.protected_account=1 THEN
    IF NOT (
      OLD.id=1 AND BINARY OLD.login_act=BINARY 'admin' AND OLD.account_type='SYSTEM' AND OLD.protected_account=1
      AND NEW.id=1 AND BINARY NEW.login_act=BINARY 'admin' AND NEW.account_type='SYSTEM' AND NEW.protected_account=1
    ) THEN
      SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='固定恢复账号身份禁止降级、转移或复制';
    END IF;
  END IF;
END$$

CREATE TRIGGER trg_recovery_account_identity_bd
BEFORE DELETE ON t_user
FOR EACH ROW
BEGIN
  IF OLD.id=1 OR BINARY OLD.login_act=BINARY 'admin' OR OLD.protected_account=1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='受保护恢复账号禁止删除';
  END IF;
END$$
DELIMITER ;

CALL crm_migration_mark_step('20260713_task22_user_management_hardening','IMMUTABILITY_TRIGGERS_READY');
CRM_MIGRATION_RUNNER_PAYLOAD_END 20260713_task22_user_management_hardening */
