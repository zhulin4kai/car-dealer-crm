-- Task 19 补充：为已部署数据库补齐用户历史操作日志查询索引。
-- t_operation_log 可能是大表，生产执行前必须停止写入流量并预留索引构建窗口。

CALL crm_require_migration_context('20260713_task19_operation_log_history_index');

DROP PROCEDURE IF EXISTS task19_prepare_operation_log_history_index;
DELIMITER $$
CREATE PROCEDURE task19_prepare_operation_log_history_index()
BEGIN
  CALL crm_require_migration_context('20260713_task19_operation_log_history_index');

  IF EXISTS (
    SELECT 1 FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='t_operation_log'
      AND INDEX_NAME='idx_operation_log_user_history'
  ) AND (
    COALESCE((
      SELECT GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX)
      FROM information_schema.STATISTICS
      WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='t_operation_log'
        AND INDEX_NAME='idx_operation_log_user_history'
    ), '') <> 'resource_id,action_code,create_time,id'
    OR COALESCE((
      SELECT MIN(NON_UNIQUE)
      FROM information_schema.STATISTICS
      WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='t_operation_log'
        AND INDEX_NAME='idx_operation_log_user_history'
    ), -1) <> 1
    OR COALESCE((
      SELECT MIN(INDEX_TYPE)
      FROM information_schema.STATISTICS
      WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='t_operation_log'
        AND INDEX_NAME='idx_operation_log_user_history'
    ), '') <> 'BTREE'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT='idx_operation_log_user_history 已存在但定义不一致，请人工核对后恢复迁移';
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='t_operation_log'
      AND INDEX_NAME='idx_operation_log_user_history'
  ) THEN
    ALTER TABLE t_operation_log
      ADD INDEX idx_operation_log_user_history(resource_id,action_code,create_time,id);
  END IF;
END$$
DELIMITER ;

CALL task19_prepare_operation_log_history_index();
DROP PROCEDURE task19_prepare_operation_log_history_index;
CALL crm_migration_mark_step(
  '20260713_task19_operation_log_history_index',
  'OPERATION_LOG_USER_HISTORY_INDEX_READY'
);
