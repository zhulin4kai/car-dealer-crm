-- Task 10：用户授权当前态、不可变历史与审计容量兼容迁移。
--
-- 只能由 Task21 用户管理迁移执行器调用并按步骤账本恢复。旧 Service 在切换前继续读写原 user_role/role_permission
-- 兼容列；个人权限和历史表在后续授权 Service 接管前不参与现有最终权限计算。
-- CUSTOM_ORGS 在本任务只保留稳定 code，其具体组织关联及执行语义留给 Task 14。

CALL crm_require_migration_context('20260711_task10_authorization_history');

SELECT COUNT(*) AS user_role_count_before FROM t_user_role;
SELECT COUNT(*) AS role_permission_count_before FROM t_role_permission;
SELECT ur.user_id, GROUP_CONCAT(DISTINCT p.code ORDER BY p.code SEPARATOR ',') AS effective_permission_codes_before
FROM t_user_role ur
INNER JOIN t_role r ON r.id = ur.role_id AND r.enabled = 1
INNER JOIN t_role_permission rp ON rp.role_id = r.id
INNER JOIN t_permission p ON p.id = rp.permission_id AND p.enabled = 1
GROUP BY ur.user_id
ORDER BY ur.user_id;

-- 以已完成步骤而非“是否 resume”决定首跑回填；回填完成前的中断必须继续，完成后不得覆盖后续事实。
SET @task10_backfill_required = NOT EXISTS (
    SELECT 1 FROM t_user_management_migration_step
    WHERE migration_key='20260711_task10_authorization_history'
      AND step_code='FIRST_RUN_COMPATIBILITY_BACKFILL_READY'
);

DROP PROCEDURE IF EXISTS task10_add_column_if_missing;
DELIMITER $$
CREATE PROCEDURE task10_add_column_if_missing(
    IN target_table VARCHAR(64), IN target_column VARCHAR(64), IN ddl_sql TEXT
)
BEGIN
    CALL crm_require_migration_context('20260711_task10_authorization_history');
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = target_table AND COLUMN_NAME = target_column
    ) THEN
        SET @task10_ddl = ddl_sql;
        PREPARE task10_stmt FROM @task10_ddl;
        EXECUTE task10_stmt;
        DEALLOCATE PREPARE task10_stmt;
    END IF;
END$$
DELIMITER ;

CALL task10_add_column_if_missing('t_role', 'description',
    'ALTER TABLE t_role ADD COLUMN description VARCHAR(255) NULL');
CALL task10_add_column_if_missing('t_role', 'protected_role',
    'ALTER TABLE t_role ADD COLUMN protected_role TINYINT(1) NOT NULL DEFAULT 0');
CALL task10_add_column_if_missing('t_role', 'authorization_level',
    'ALTER TABLE t_role ADD COLUMN authorization_level INT NOT NULL DEFAULT 0');
CALL task10_add_column_if_missing('t_role', 'default_data_scope',
    'ALTER TABLE t_role ADD COLUMN default_data_scope VARCHAR(32) NOT NULL DEFAULT ''SELF''');
CALL task10_add_column_if_missing('t_role', 'version',
    'ALTER TABLE t_role ADD COLUMN version INT NOT NULL DEFAULT 0');

CALL task10_add_column_if_missing('t_permission', 'module',
    'ALTER TABLE t_permission ADD COLUMN module VARCHAR(64) NOT NULL DEFAULT ''system''');
CALL task10_add_column_if_missing('t_permission', 'description',
    'ALTER TABLE t_permission ADD COLUMN description VARCHAR(255) NULL');
CALL task10_add_column_if_missing('t_permission', 'sensitivity_level',
    'ALTER TABLE t_permission ADD COLUMN sensitivity_level VARCHAR(16) NOT NULL DEFAULT ''NORMAL''');
CALL task10_add_column_if_missing('t_permission', 'delegable',
    'ALTER TABLE t_permission ADD COLUMN delegable TINYINT(1) NOT NULL DEFAULT 0');
CALL task10_add_column_if_missing('t_permission', 'version',
    'ALTER TABLE t_permission ADD COLUMN version INT NOT NULL DEFAULT 0');

CALL task10_add_column_if_missing('t_role_permission', 'delegable',
    'ALTER TABLE t_role_permission ADD COLUMN delegable TINYINT(1) NOT NULL DEFAULT 0');
CALL task10_add_column_if_missing('t_role_permission', 'data_scope_code',
    'ALTER TABLE t_role_permission ADD COLUMN data_scope_code VARCHAR(32) NOT NULL DEFAULT ''SELF''');

CALL task10_add_column_if_missing('t_user_role', 'granted_by',
    'ALTER TABLE t_user_role ADD COLUMN granted_by INT NULL');
CALL task10_add_column_if_missing('t_user_role', 'reason',
    'ALTER TABLE t_user_role ADD COLUMN reason VARCHAR(500) NULL');
CALL task10_add_column_if_missing('t_user_role', 'effective_from',
    'ALTER TABLE t_user_role ADD COLUMN effective_from DATETIME NULL');
CALL task10_add_column_if_missing('t_user_role', 'effective_to',
    'ALTER TABLE t_user_role ADD COLUMN effective_to DATETIME NULL');
DROP PROCEDURE task10_add_column_if_missing;

DROP PROCEDURE IF EXISTS task10_add_constraint_if_missing;
DELIMITER $$
CREATE PROCEDURE task10_add_constraint_if_missing(
    IN target_table VARCHAR(64), IN target_constraint VARCHAR(64), IN ddl_sql TEXT
)
BEGIN
    CALL crm_require_migration_context('20260711_task10_authorization_history');
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.TABLE_CONSTRAINTS
        WHERE CONSTRAINT_SCHEMA = DATABASE() AND TABLE_NAME = target_table AND CONSTRAINT_NAME = target_constraint
    ) THEN
        SET @task10_ddl = ddl_sql;
        PREPARE task10_stmt FROM @task10_ddl;
        EXECUTE task10_stmt;
        DEALLOCATE PREPARE task10_stmt;
    END IF;
END$$
DELIMITER ;

CALL task10_add_constraint_if_missing('t_role', 'chk_role_protected',
    'ALTER TABLE t_role ADD CONSTRAINT chk_role_protected CHECK (protected_role IN (0, 1))');
CALL task10_add_constraint_if_missing('t_role', 'chk_role_authorization_level',
    'ALTER TABLE t_role ADD CONSTRAINT chk_role_authorization_level CHECK (authorization_level >= 0)');
CALL task10_add_constraint_if_missing('t_role', 'chk_role_data_scope',
    'ALTER TABLE t_role ADD CONSTRAINT chk_role_data_scope CHECK (default_data_scope IN (''SELF'', ''DIRECT_REPORTS'', ''REPORTING_TREE'', ''PRIMARY_ORG'', ''ORG_TREE'', ''CUSTOM_ORGS'', ''GLOBAL''))');
CALL task10_add_constraint_if_missing('t_role', 'chk_role_version',
    'ALTER TABLE t_role ADD CONSTRAINT chk_role_version CHECK (version >= 0)');
CALL task10_add_constraint_if_missing('t_permission', 'chk_permission_sensitivity',
    'ALTER TABLE t_permission ADD CONSTRAINT chk_permission_sensitivity CHECK (sensitivity_level IN (''NORMAL'', ''SENSITIVE'', ''PROTECTED''))');
CALL task10_add_constraint_if_missing('t_permission', 'chk_permission_delegable',
    'ALTER TABLE t_permission ADD CONSTRAINT chk_permission_delegable CHECK (delegable IN (0, 1))');
CALL task10_add_constraint_if_missing('t_permission', 'chk_permission_version',
    'ALTER TABLE t_permission ADD CONSTRAINT chk_permission_version CHECK (version >= 0)');
CALL task10_add_constraint_if_missing('t_role_permission', 'chk_role_permission_delegable',
    'ALTER TABLE t_role_permission ADD CONSTRAINT chk_role_permission_delegable CHECK (delegable IN (0, 1))');
CALL task10_add_constraint_if_missing('t_role_permission', 'chk_role_permission_data_scope',
    'ALTER TABLE t_role_permission ADD CONSTRAINT chk_role_permission_data_scope CHECK (data_scope_code IN (''SELF'', ''DIRECT_REPORTS'', ''REPORTING_TREE'', ''PRIMARY_ORG'', ''ORG_TREE'', ''CUSTOM_ORGS'', ''GLOBAL''))');
CALL task10_add_constraint_if_missing('t_user_role', 'chk_user_role_period',
    'ALTER TABLE t_user_role ADD CONSTRAINT chk_user_role_period CHECK (effective_to IS NULL OR effective_from IS NULL OR effective_to > effective_from)');
CALL task10_add_constraint_if_missing('t_user_role', 'fk_user_role_granted_by',
    'ALTER TABLE t_user_role ADD CONSTRAINT fk_user_role_granted_by FOREIGN KEY (granted_by) REFERENCES t_user(id) ON DELETE RESTRICT');
DROP PROCEDURE task10_add_constraint_if_missing;
CALL crm_migration_mark_step('20260711_task10_authorization_history', 'AUTHORIZATION_COLUMNS_CONSTRAINTS_READY');

DROP PROCEDURE IF EXISTS task10_backfill_compatibility;
DELIMITER $$
CREATE PROCEDURE task10_backfill_compatibility()
BEGIN
  CALL crm_require_migration_context('20260711_task10_authorization_history');
  START TRANSACTION;
  UPDATE t_role
SET description = role_name,
    protected_role = CASE WHEN role = 'admin' THEN 1 ELSE 0 END,
    authorization_level = CASE role
        WHEN 'admin' THEN 100 WHEN 'sales_manager' THEN 60 WHEN 'finance_specialist' THEN 50
        WHEN 'marketing_specialist' THEN 40 WHEN 'inventory_specialist' THEN 40
        WHEN 'sales_consultant' THEN 30 ELSE 0 END,
    default_data_scope = CASE role
        WHEN 'admin' THEN 'GLOBAL' WHEN 'sales_manager' THEN 'REPORTING_TREE'
        WHEN 'marketing_specialist' THEN 'PRIMARY_ORG' WHEN 'finance_specialist' THEN 'PRIMARY_ORG'
        WHEN 'inventory_specialist' THEN 'PRIMARY_ORG' ELSE 'SELF' END
WHERE @task10_backfill_required;

UPDATE t_permission
SET module = CASE
        WHEN code LIKE 'menu:%' THEN SUBSTRING_INDEX(code, ':', -1)
        WHEN code LIKE 'page:%' THEN SUBSTRING_INDEX(SUBSTRING_INDEX(code, ':', 2), ':', -1)
        ELSE SUBSTRING_INDEX(code, ':', 1) END,
    description = name,
    sensitivity_level = CASE
        WHEN code IN ('user:role', 'user:status', 'user:password', 'user:delete',
                      'ai:provider-config:manage', 'ai:provider-config:rotate-key') THEN 'PROTECTED'
        WHEN code LIKE '%:delete' OR code LIKE '%:export' OR code LIKE '%:import'
          OR code LIKE '%:approve' OR code LIKE '%:payment%' OR code LIKE '%:refund%'
          OR code LIKE '%:invoice%' OR code LIKE '%:sensitive%' OR code LIKE '%:adjust'
          THEN 'SENSITIVE'
        ELSE 'NORMAL' END
WHERE @task10_backfill_required;
UPDATE t_permission
SET delegable = CASE WHEN sensitivity_level = 'NORMAL' THEN 1 ELSE 0 END
WHERE @task10_backfill_required;

UPDATE t_role_permission rp
INNER JOIN t_permission p ON p.id = rp.permission_id
INNER JOIN t_role r ON r.id = rp.role_id
SET rp.delegable = p.delegable,
    rp.data_scope_code = r.default_data_scope
WHERE @task10_backfill_required;

UPDATE t_user_role ur
SET ur.reason = COALESCE(ur.reason, 'Task10迁移：旧角色的授权人与实际生效时间未记录')
WHERE @task10_backfill_required;
  COMMIT;
END$$
DELIMITER ;
CALL task10_backfill_compatibility();
DROP PROCEDURE task10_backfill_compatibility;
CALL crm_migration_mark_step('20260711_task10_authorization_history', 'FIRST_RUN_COMPATIBILITY_BACKFILL_READY');

DROP PROCEDURE IF EXISTS task10_create_authorization_tables;
DELIMITER $$
CREATE PROCEDURE task10_create_authorization_tables()
BEGIN
    CALL crm_require_migration_context('20260711_task10_authorization_history');
    CREATE TABLE IF NOT EXISTS t_user_permission
(
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id INT NOT NULL,
    permission_id INT NOT NULL,
    effect VARCHAR(16) NOT NULL,
    data_scope_code VARCHAR(32) NULL,
    effective_from DATETIME NOT NULL,
    effective_to DATETIME NULL,
    reason VARCHAR(500) NOT NULL,
    granted_by INT NOT NULL,
    version INT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL,
    update_time DATETIME NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_permission_current (user_id, permission_id),
    KEY idx_user_permission_effective (user_id, effective_from, effective_to, permission_id, version),
    CONSTRAINT fk_user_permission_user FOREIGN KEY (user_id) REFERENCES t_user(id) ON DELETE RESTRICT,
    CONSTRAINT fk_user_permission_permission FOREIGN KEY (permission_id) REFERENCES t_permission(id) ON DELETE RESTRICT,
    CONSTRAINT fk_user_permission_granted_by FOREIGN KEY (granted_by) REFERENCES t_user(id) ON DELETE RESTRICT,
    CONSTRAINT chk_user_permission_effect CHECK (effect IN ('GRANT', 'DENY')),
    CONSTRAINT chk_user_permission_scope CHECK ((effect = 'GRANT' AND data_scope_code IS NOT NULL AND data_scope_code IN ('SELF', 'DIRECT_REPORTS', 'REPORTING_TREE', 'PRIMARY_ORG', 'ORG_TREE', 'CUSTOM_ORGS', 'GLOBAL')) OR (effect = 'DENY' AND data_scope_code IS NULL)),
    CONSTRAINT chk_user_permission_period CHECK (effective_to IS NULL OR effective_to > effective_from),
    CONSTRAINT chk_user_permission_version CHECK (version >= 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户个人授权当前态表（version CAS）';

    CREATE TABLE IF NOT EXISTS t_authorization_history
(
    id BIGINT NOT NULL AUTO_INCREMENT,
    subject_type VARCHAR(32) NOT NULL,
    subject_id VARCHAR(64) NOT NULL,
    change_type VARCHAR(16) NOT NULL,
    target_user_id INT NULL,
    role_id INT NULL,
    permission_id INT NULL,
    effect VARCHAR(16) NULL,
    data_scope_code VARCHAR(32) NULL,
    effective_from DATETIME NULL,
    effective_to DATETIME NULL,
    before_value VARCHAR(2048) NULL,
    after_value VARCHAR(2048) NULL,
    reason VARCHAR(500) NOT NULL,
    operator_id INT NOT NULL,
    occurred_time DATETIME NOT NULL,
    request_id VARCHAR(64) NULL,
    PRIMARY KEY (id),
    KEY idx_authorization_history_subject (subject_type, subject_id, occurred_time, id),
    KEY idx_authorization_history_target (target_user_id, occurred_time, id),
    CONSTRAINT fk_authorization_history_target_user FOREIGN KEY (target_user_id) REFERENCES t_user(id) ON DELETE RESTRICT,
    CONSTRAINT fk_authorization_history_role FOREIGN KEY (role_id) REFERENCES t_role(id) ON DELETE RESTRICT,
    CONSTRAINT fk_authorization_history_permission FOREIGN KEY (permission_id) REFERENCES t_permission(id) ON DELETE RESTRICT,
    CONSTRAINT fk_authorization_history_operator FOREIGN KEY (operator_id) REFERENCES t_user(id) ON DELETE RESTRICT,
    CONSTRAINT chk_authorization_history_subject CHECK (subject_type IN ('ROLE', 'ROLE_PERMISSION', 'USER_ROLE', 'USER_PERMISSION', 'ORGANIZATION_UNIT', 'POSITION', 'ORGANIZATION_ASSIGNMENT', 'REPORTING_RELATION')),
    CONSTRAINT chk_authorization_history_change CHECK (change_type IN ('CREATE', 'UPDATE', 'ENABLE', 'DISABLE', 'ASSIGN', 'UNASSIGN', 'GRANT', 'DENY', 'REVOKE', 'EXPIRE')),
    CONSTRAINT chk_authorization_history_effect CHECK (effect IS NULL OR effect IN ('GRANT', 'DENY')),
    CONSTRAINT chk_authorization_history_period CHECK (effective_to IS NULL OR effective_from IS NULL OR effective_to > effective_from)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '授权变化不可变历史表';
END$$
DELIMITER ;
CALL task10_create_authorization_tables();
DROP PROCEDURE task10_create_authorization_tables;
CALL crm_migration_mark_step('20260711_task10_authorization_history', 'AUTHORIZATION_TABLES_READY');

/* CRM_MIGRATION_RUNNER_PAYLOAD_BEGIN 20260711_task10_authorization_history
CALL crm_require_migration_context('20260711_task10_authorization_history');
DROP TRIGGER IF EXISTS trg_authorization_history_no_update;
DROP TRIGGER IF EXISTS trg_authorization_history_no_delete;
DELIMITER $$
CREATE TRIGGER trg_authorization_history_no_update BEFORE UPDATE ON t_authorization_history
FOR EACH ROW BEGIN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'authorization history is immutable'; END$$
CREATE TRIGGER trg_authorization_history_no_delete BEFORE DELETE ON t_authorization_history
FOR EACH ROW BEGIN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'authorization history is immutable'; END$$
DELIMITER ;
CALL crm_migration_mark_step('20260711_task10_authorization_history', 'IMMUTABILITY_TRIGGERS_READY');
CRM_MIGRATION_RUNNER_PAYLOAD_END 20260711_task10_authorization_history */

DROP PROCEDURE IF EXISTS task10_expand_operation_audit;
DELIMITER $$
CREATE PROCEDURE task10_expand_operation_audit()
BEGIN
    CALL crm_require_migration_context('20260711_task10_authorization_history');
    ALTER TABLE t_operation_log MODIFY COLUMN detail VARCHAR(2048) NULL COMMENT '结构化审计摘要JSON';
END$$
DELIMITER ;
CALL task10_expand_operation_audit();
DROP PROCEDURE task10_expand_operation_audit;
CALL crm_migration_mark_step('20260711_task10_authorization_history', 'AUDIT_CAPACITY_READY');

SELECT COUNT(*) AS user_role_count_after FROM t_user_role;
SELECT COUNT(*) AS role_permission_count_after FROM t_role_permission;
SELECT ur.user_id, GROUP_CONCAT(DISTINCT p.code ORDER BY p.code SEPARATOR ',') AS effective_permission_codes_after
FROM t_user_role ur
INNER JOIN t_role r ON r.id = ur.role_id AND r.enabled = 1
INNER JOIN t_role_permission rp ON rp.role_id = r.id
INNER JOIN t_permission p ON p.id = rp.permission_id AND p.enabled = 1
GROUP BY ur.user_id
ORDER BY ur.user_id;

-- 收紧时点：Task 13/14 切换新授权 Service 并完成 null/默认值核验后，方可将
-- t_user_role 的 granted_by/reason/effective_from 收紧为 NOT NULL；本迁移不提前收紧。
