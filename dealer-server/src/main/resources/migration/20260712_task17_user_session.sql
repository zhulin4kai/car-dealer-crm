-- Task 17：独立多设备会话、最小 JWT 与会话撤销事实。
CALL crm_require_migration_context('20260712_task17_user_session');

DROP PROCEDURE IF EXISTS task17_prepare_user;
DELIMITER $$
CREATE PROCEDURE task17_prepare_user()
BEGIN
  CALL crm_require_migration_context('20260712_task17_user_session');
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='t_user' AND COLUMN_NAME='session_revision') THEN
    ALTER TABLE t_user ADD session_revision bigint NOT NULL DEFAULT 0 COMMENT '会话列表命令独立并发版本';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.TABLE_CONSTRAINTS WHERE CONSTRAINT_SCHEMA=DATABASE() AND TABLE_NAME='t_user' AND CONSTRAINT_NAME='chk_user_session_revision') THEN
    ALTER TABLE t_user ADD CONSTRAINT chk_user_session_revision CHECK (session_revision >= 0);
  END IF;
END$$
DELIMITER ;
CALL task17_prepare_user();
DROP PROCEDURE task17_prepare_user;
CALL crm_migration_mark_step('20260712_task17_user_session','USER_SESSION_REVISION_READY');

DROP PROCEDURE IF EXISTS task17_create_session_table;
DELIMITER $$
CREATE PROCEDURE task17_create_session_table()
BEGIN
  CALL crm_require_migration_context('20260712_task17_user_session');
  CREATE TABLE IF NOT EXISTS t_user_session (
  id bigint NOT NULL AUTO_INCREMENT PRIMARY KEY, session_id varchar(64) NOT NULL, user_id int NOT NULL,
  token_digest varchar(64) NOT NULL, issued_auth_version bigint NOT NULL, remember_me tinyint(1) NOT NULL DEFAULT 0,
  device_summary varchar(128) NOT NULL, client_summary varchar(128) NULL, network_summary varchar(128) NULL,
  login_time datetime NOT NULL, last_activity_time datetime NOT NULL, idle_expires_at datetime NOT NULL,
  absolute_expires_at datetime NOT NULL, revoked_at datetime NULL, revoked_by int NULL,
  revoke_reason varchar(500) NULL, revoke_type varchar(32) NULL, version int NOT NULL DEFAULT 0, create_time datetime NOT NULL,
  UNIQUE KEY uk_user_session_id(session_id), UNIQUE KEY uk_user_session_token_digest(token_digest),
  KEY idx_user_session_user_active(user_id,revoked_at,login_time,session_id), KEY idx_user_session_retention(revoked_at,id),
  CONSTRAINT fk_user_session_user FOREIGN KEY(user_id) REFERENCES t_user(id) ON DELETE RESTRICT,
  CONSTRAINT chk_user_session_remember CHECK(remember_me IN (0,1)), CONSTRAINT chk_user_session_version CHECK(version >= 0),
  CONSTRAINT chk_user_session_times CHECK(login_time <= last_activity_time AND last_activity_time < idle_expires_at AND idle_expires_at <= absolute_expires_at),
  CONSTRAINT chk_user_session_revocation CHECK((revoked_at IS NULL AND revoke_reason IS NULL AND revoke_type IS NULL) OR (revoked_at IS NOT NULL AND revoke_reason IS NOT NULL AND revoke_type IS NOT NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
END$$
DELIMITER ;
CALL task17_create_session_table();
DROP PROCEDURE task17_create_session_table;
CALL crm_migration_mark_step('20260712_task17_user_session','USER_SESSION_TABLE_READY');
