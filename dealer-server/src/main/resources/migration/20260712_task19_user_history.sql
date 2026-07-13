-- Task 19: 为角色矩阵事件保存发生时受影响用户集合与稳定身份快照。
CALL crm_require_migration_context('20260712_task19_user_history');
DELIMITER $$
DROP PROCEDURE IF EXISTS migrate_task19_user_history$$
CREATE PROCEDURE migrate_task19_user_history()
BEGIN
  CALL crm_require_migration_context('20260712_task19_user_history');
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema=DATABASE() AND table_name='t_authorization_history'
      AND column_name='affected_user_ids'
  ) THEN
    ALTER TABLE t_authorization_history ADD COLUMN affected_user_ids MEDIUMTEXT NULL AFTER request_id;
  END IF;
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema=DATABASE() AND table_name='t_authorization_history'
      AND column_name='affected_users_snapshot'
  ) THEN
    ALTER TABLE t_authorization_history ADD COLUMN affected_users_snapshot MEDIUMTEXT NULL AFTER affected_user_ids;
  END IF;
  INSERT INTO t_authorization_graph_lock(lock_name)
  VALUES('AUTHORIZATION_MEMBERSHIP_GUARD')
  ON DUPLICATE KEY UPDATE lock_name=VALUES(lock_name);
END$$
CALL migrate_task19_user_history()$$
DROP PROCEDURE migrate_task19_user_history$$
DELIMITER ;
CALL crm_migration_mark_step('20260712_task19_user_history', 'HISTORY_IMPACT_SNAPSHOT_READY');
