-- Task 03：认证安全版本兼容迁移。
--
-- 执行方式：人工在目标 MariaDB/MySQL 数据库执行；不依赖自动迁移框架。
-- 幂等边界：
-- 1. 仅在 t_user.auth_version 不存在时新增，默认值 0 兼容迁移前签发的 JWT。
-- 2. 仅在约束不存在时新增非负约束，可重复执行。
-- 3. 本脚本不修改密码、账号状态、角色关系或现有 Redis 会话。
-- 4. 旧 JWT 只在 auth_version=0 且 Redis 中仍精确匹配时由应用兼容；首次安全变更后永久退出兼容。

CALL crm_require_migration_context('20260711_task03_auth_version');

DROP PROCEDURE IF EXISTS task03_add_auth_version_if_missing;
DELIMITER $$
CREATE PROCEDURE task03_add_auth_version_if_missing()
BEGIN
    CALL crm_require_migration_context('20260711_task03_auth_version');
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 't_user'
          AND COLUMN_NAME = 'auth_version'
    ) THEN
        ALTER TABLE t_user
            ADD COLUMN auth_version BIGINT NOT NULL DEFAULT 0 COMMENT '认证安全版本，安全变更时递增';
    END IF;
END$$
DELIMITER ;

CALL task03_add_auth_version_if_missing();
DROP PROCEDURE task03_add_auth_version_if_missing;
CALL crm_migration_mark_step('20260711_task03_auth_version', 'AUTH_VERSION_COLUMN_READY');

DROP PROCEDURE IF EXISTS task03_add_auth_version_check_if_missing;
DELIMITER $$
CREATE PROCEDURE task03_add_auth_version_check_if_missing()
BEGIN
    CALL crm_require_migration_context('20260711_task03_auth_version');
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.TABLE_CONSTRAINTS
        WHERE CONSTRAINT_SCHEMA = DATABASE()
          AND TABLE_NAME = 't_user'
          AND CONSTRAINT_NAME = 'chk_user_auth_version'
    ) THEN
        ALTER TABLE t_user
            ADD CONSTRAINT chk_user_auth_version CHECK (auth_version >= 0);
    END IF;
END$$
DELIMITER ;

CALL task03_add_auth_version_check_if_missing();
DROP PROCEDURE task03_add_auth_version_check_if_missing;
CALL crm_migration_mark_step('20260711_task03_auth_version', 'AUTH_VERSION_CONSTRAINT_READY');

SELECT COUNT(*) AS invalid_auth_version_count
FROM t_user
WHERE auth_version IS NULL OR auth_version < 0;
