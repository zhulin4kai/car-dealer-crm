-- Task 24：将原始凭证交付移出业务事务，建立可幂等重试且不保存明文的 Outbox。

CALL crm_require_migration_context('20260713_task24_credential_delivery_outbox');

DROP PROCEDURE IF EXISTS task24_prepare_credential_delivery_outbox;
DELIMITER $$
CREATE PROCEDURE task24_prepare_credential_delivery_outbox()
BEGIN
  CALL crm_require_migration_context('20260713_task24_credential_delivery_outbox');

  CREATE TABLE IF NOT EXISTS t_credential_delivery_outbox (
    id BIGINT NOT NULL AUTO_INCREMENT,
    message_id VARCHAR(36) NOT NULL,
    credential_id BIGINT NOT NULL,
    user_id INT NOT NULL,
    purpose VARCHAR(24) NOT NULL,
    derivation_nonce VARCHAR(64) NULL COMMENT '使用独立部署密钥派生原始凭证的随机 nonce，终态清除',
    phone_digest VARCHAR(64) NULL,
    email_digest VARCHAR(64) NULL,
    status VARCHAR(16) NOT NULL,
    attempt_count INT NOT NULL DEFAULT 0,
    next_attempt_at DATETIME NOT NULL,
    claimed_at DATETIME NULL,
    delivered_at DATETIME NULL,
    failed_at DATETIME NULL,
    last_error_code VARCHAR(64) NULL,
    version INT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL,
    edit_time DATETIME NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_credential_delivery_message (message_id),
    UNIQUE KEY uk_credential_delivery_credential (credential_id),
    KEY idx_credential_delivery_due (status,next_attempt_at,id),
    CONSTRAINT fk_credential_delivery_credential FOREIGN KEY (credential_id) REFERENCES t_account_credential(id) ON DELETE RESTRICT,
    CONSTRAINT fk_credential_delivery_user FOREIGN KEY (user_id) REFERENCES t_user(id) ON DELETE RESTRICT,
    CONSTRAINT chk_credential_delivery_status CHECK (status IN ('PENDING','PROCESSING','RETRY','DELIVERED','FAILED')),
    CONSTRAINT chk_credential_delivery_attempt CHECK (attempt_count >= 0 AND version >= 0),
    CONSTRAINT chk_credential_delivery_contact CHECK (phone_digest IS NOT NULL OR email_digest IS NOT NULL),
    CONSTRAINT chk_credential_delivery_nonce CHECK ((status IN ('PENDING','PROCESSING','RETRY') AND derivation_nonce IS NOT NULL) OR (status IN ('DELIVERED','FAILED') AND derivation_nonce IS NULL))
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='提交后一次性凭证投递 Outbox';
END$$
DELIMITER ;

CALL task24_prepare_credential_delivery_outbox();
DROP PROCEDURE task24_prepare_credential_delivery_outbox;
CALL crm_migration_mark_step('20260713_task24_credential_delivery_outbox','CREDENTIAL_DELIVERY_OUTBOX_READY');
