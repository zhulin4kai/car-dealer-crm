-- Task 20：调岗、离职、责任交接和返聘不可变事件历史。
-- 仅允许由统一用户管理迁移执行器在持有命名锁和 RUNNING 账本上下文时执行。

CALL crm_require_migration_context('20260712_task20_user_lifecycle');

DROP PROCEDURE IF EXISTS task20_prepare_user_lifecycle;
DELIMITER $$
CREATE PROCEDURE task20_prepare_user_lifecycle()
BEGIN
  CALL crm_require_migration_context('20260712_task20_user_lifecycle');

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema=DATABASE() AND table_name='t_user_permission' AND column_name='active_marker'
  ) THEN
    ALTER TABLE t_user_permission
      ADD COLUMN active_marker TINYINT(1) NULL DEFAULT 1 AFTER effective_to;
  END IF;

  INSERT INTO t_authorization_graph_lock(lock_name)
  VALUES('TEST_DRIVE_SCHEDULE_GUARD')
  ON DUPLICATE KEY UPDATE lock_name=VALUES(lock_name);

  CREATE TABLE IF NOT EXISTS t_user_lifecycle_event
  (
    id BIGINT NOT NULL AUTO_INCREMENT,
    operation_id VARCHAR(64) NOT NULL,
    request_id VARCHAR(64) NOT NULL,
    action VARCHAR(32) NOT NULL,
    user_id INT NOT NULL,
    employee_id INT NOT NULL,
    before_value MEDIUMTEXT NOT NULL,
    after_value MEDIUMTEXT NOT NULL,
    reason VARCHAR(500) NOT NULL,
    operator_id INT NOT NULL,
    occurred_time DATETIME NOT NULL,
    PRIMARY KEY(id),
    UNIQUE KEY uk_user_lifecycle_operation(operation_id),
    KEY idx_user_lifecycle_target_time(user_id,occurred_time,id),
    CONSTRAINT fk_user_lifecycle_user FOREIGN KEY(user_id) REFERENCES t_user(id) ON DELETE RESTRICT,
    CONSTRAINT fk_user_lifecycle_employee FOREIGN KEY(employee_id) REFERENCES t_employee(id) ON DELETE RESTRICT,
    CONSTRAINT fk_user_lifecycle_operator FOREIGN KEY(operator_id) REFERENCES t_user(id) ON DELETE RESTRICT,
    CONSTRAINT chk_user_lifecycle_action CHECK(action IN ('TRANSFER','DEPARTURE_START','HANDOVER_CONFIRM','DEPARTURE_COMPLETE','REHIRE'))
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='人员生命周期不可变事件历史';

  CREATE TABLE IF NOT EXISTS t_user_lifecycle_snapshot
  (
    id BIGINT NOT NULL AUTO_INCREMENT,
    token_digest VARCHAR(64) NOT NULL,
    user_id INT NOT NULL,
    employee_id INT NOT NULL,
    employee_version INT NOT NULL,
    reason_digest VARCHAR(64) NOT NULL,
    fact_digest VARCHAR(64) NOT NULL,
    expires_at DATETIME NOT NULL,
    consumed_at DATETIME NULL,
    version INT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL,
    PRIMARY KEY(id),
    UNIQUE KEY uk_user_lifecycle_snapshot_token(token_digest),
    KEY idx_user_lifecycle_snapshot_expiry(expires_at,consumed_at,id),
    CONSTRAINT fk_user_lifecycle_snapshot_user FOREIGN KEY(user_id) REFERENCES t_user(id) ON DELETE RESTRICT,
    CONSTRAINT fk_user_lifecycle_snapshot_employee FOREIGN KEY(employee_id) REFERENCES t_employee(id) ON DELETE RESTRICT
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='一次性离职预检快照';
END$$
DELIMITER ;
CALL task20_prepare_user_lifecycle();
DROP PROCEDURE task20_prepare_user_lifecycle;
CALL crm_migration_mark_step('20260712_task20_user_lifecycle', 'LIFECYCLE_TABLES_AND_LOCK_READY');

-- 事件历史是审计事实；数据库层拒绝任何覆盖或删除。
/* CRM_MIGRATION_RUNNER_PAYLOAD_BEGIN 20260712_task20_user_lifecycle
CALL crm_require_migration_context('20260712_task20_user_lifecycle');
DROP TRIGGER IF EXISTS trg_user_lifecycle_event_no_update;
DROP TRIGGER IF EXISTS trg_user_lifecycle_event_no_delete;
DELIMITER $$
CREATE TRIGGER trg_user_lifecycle_event_no_update
BEFORE UPDATE ON t_user_lifecycle_event
FOR EACH ROW
BEGIN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'user lifecycle event is immutable';
END$$
CREATE TRIGGER trg_user_lifecycle_event_no_delete
BEFORE DELETE ON t_user_lifecycle_event
FOR EACH ROW
BEGIN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'user lifecycle event is immutable';
END$$
DELIMITER ;
CALL crm_migration_mark_step('20260712_task20_user_lifecycle', 'LIFECYCLE_IMMUTABILITY_READY');
CRM_MIGRATION_RUNNER_PAYLOAD_END 20260712_task20_user_lifecycle */
