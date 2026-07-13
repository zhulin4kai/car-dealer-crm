-- 用户管理迁移统一账本 bootstrap。
-- 仅由 scripts/database/user-management-migrate.sh 在持有数据库命名锁后执行。

CREATE TABLE IF NOT EXISTS t_user_management_migration
(
    migration_key       VARCHAR(128) NOT NULL,
    status              VARCHAR(16)  NOT NULL DEFAULT 'SUCCEEDED',
    checksum_sha256     CHAR(64)     NULL,
    started_at          DATETIME     NULL,
    completed_at        DATETIME     NULL,
    failed_at           DATETIME     NULL,
    last_completed_step VARCHAR(128) NULL,
    attempt_count       INT          NOT NULL DEFAULT 0,
    error_summary       VARCHAR(1000) NULL,
    executor_version    VARCHAR(32)  NULL,
    PRIMARY KEY (migration_key),
    CONSTRAINT chk_user_management_migration_status
        CHECK (status IN ('RUNNING', 'SUCCEEDED', 'FAILED')),
    CONSTRAINT chk_user_management_migration_attempt_count
        CHECK (attempt_count >= 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户管理显式迁移执行账本';

DROP PROCEDURE IF EXISTS crm_upgrade_user_management_migration_ledger;
DELIMITER $$
CREATE PROCEDURE crm_upgrade_user_management_migration_ledger()
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = 't_user_management_migration'
          AND column_name = 'migration_key' AND character_maximum_length < 128
    ) THEN
        ALTER TABLE t_user_management_migration
            MODIFY COLUMN migration_key VARCHAR(128) NOT NULL;
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = 't_user_management_migration'
          AND column_name = 'completed_at' AND is_nullable = 'NO'
    ) THEN
        ALTER TABLE t_user_management_migration
            MODIFY COLUMN completed_at DATETIME NULL;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = 't_user_management_migration'
          AND column_name = 'status'
    ) THEN
        ALTER TABLE t_user_management_migration
            ADD COLUMN status VARCHAR(16) NOT NULL DEFAULT 'SUCCEEDED' AFTER migration_key;
    END IF;
    UPDATE t_user_management_migration SET status='SUCCEEDED' WHERE status IS NULL;
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = 't_user_management_migration'
          AND column_name = 'checksum_sha256'
    ) THEN
        ALTER TABLE t_user_management_migration
            ADD COLUMN checksum_sha256 CHAR(64) NULL AFTER status;
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = 't_user_management_migration'
          AND column_name = 'started_at'
    ) THEN
        ALTER TABLE t_user_management_migration
            ADD COLUMN started_at DATETIME NULL AFTER checksum_sha256;
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = 't_user_management_migration'
          AND column_name = 'failed_at'
    ) THEN
        ALTER TABLE t_user_management_migration
            ADD COLUMN failed_at DATETIME NULL AFTER completed_at;
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = 't_user_management_migration'
          AND column_name = 'last_completed_step'
    ) THEN
        ALTER TABLE t_user_management_migration
            ADD COLUMN last_completed_step VARCHAR(128) NULL AFTER failed_at;
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = 't_user_management_migration'
          AND column_name = 'attempt_count'
    ) THEN
        ALTER TABLE t_user_management_migration
            ADD COLUMN attempt_count INT NOT NULL DEFAULT 0 AFTER last_completed_step;
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = 't_user_management_migration'
          AND column_name = 'error_summary'
    ) THEN
        ALTER TABLE t_user_management_migration
            ADD COLUMN error_summary VARCHAR(1000) NULL AFTER attempt_count;
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = 't_user_management_migration'
          AND column_name = 'executor_version'
    ) THEN
        ALTER TABLE t_user_management_migration
            ADD COLUMN executor_version VARCHAR(32) NULL AFTER error_summary;
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema=DATABASE() AND table_name='t_user_management_migration'
          AND column_name='status' AND (column_type<>'varchar(16)' OR is_nullable<>'NO')
    ) THEN
        ALTER TABLE t_user_management_migration MODIFY COLUMN status VARCHAR(16) NOT NULL DEFAULT 'SUCCEEDED';
    END IF;
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema=DATABASE() AND table_name='t_user_management_migration'
          AND column_name='checksum_sha256' AND column_type<>'char(64)'
    ) THEN
        ALTER TABLE t_user_management_migration MODIFY COLUMN checksum_sha256 CHAR(64) NULL;
    END IF;
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema=DATABASE() AND table_name='t_user_management_migration'
          AND column_name='last_completed_step' AND column_type<>'varchar(128)'
    ) THEN
        ALTER TABLE t_user_management_migration MODIFY COLUMN last_completed_step VARCHAR(128) NULL;
    END IF;
    UPDATE t_user_management_migration SET attempt_count=0 WHERE attempt_count IS NULL;
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema=DATABASE() AND table_name='t_user_management_migration'
          AND column_name='attempt_count' AND (data_type<>'int' OR is_nullable<>'NO')
    ) THEN
        ALTER TABLE t_user_management_migration MODIFY COLUMN attempt_count INT NOT NULL DEFAULT 0;
    END IF;
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema=DATABASE() AND table_name='t_user_management_migration'
          AND column_name='error_summary' AND column_type<>'varchar(1000)'
    ) THEN
        ALTER TABLE t_user_management_migration MODIFY COLUMN error_summary VARCHAR(1000) NULL;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_schema = DATABASE() AND table_name = 't_user_management_migration'
          AND constraint_name = 'chk_user_management_migration_status'
    ) THEN
        ALTER TABLE t_user_management_migration
            ADD CONSTRAINT chk_user_management_migration_status
            CHECK (status IN ('RUNNING', 'SUCCEEDED', 'FAILED'));
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_schema = DATABASE() AND table_name = 't_user_management_migration'
          AND constraint_name = 'chk_user_management_migration_attempt_count'
    ) THEN
        ALTER TABLE t_user_management_migration
            ADD CONSTRAINT chk_user_management_migration_attempt_count
            CHECK (attempt_count >= 0);
    END IF;
END$$
DELIMITER ;

CALL crm_upgrade_user_management_migration_ledger();
DROP PROCEDURE crm_upgrade_user_management_migration_ledger;

CREATE TABLE IF NOT EXISTS t_user_management_migration_step
(
    migration_key VARCHAR(128) NOT NULL,
    step_code     VARCHAR(128) NOT NULL,
    completed_at DATETIME     NOT NULL,
    PRIMARY KEY (migration_key, step_code),
    CONSTRAINT fk_user_management_migration_step
        FOREIGN KEY (migration_key) REFERENCES t_user_management_migration (migration_key) ON DELETE RESTRICT
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户管理迁移已完成步骤账本';
