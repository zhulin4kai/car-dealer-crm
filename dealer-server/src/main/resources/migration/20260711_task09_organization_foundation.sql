-- Task 09：组织、岗位、员工、任职和汇报关系兼容迁移。
--
-- 执行方式：只能由 Task21 用户管理迁移执行器在持有命名锁并写入 RUNNING 账本后执行。
-- 幂等边界：
-- 1. t_user 新字段通过 information_schema 判断后新增，可重复执行。
-- 2. 新表使用 CREATE TABLE IF NOT EXISTS，种子和回填使用 NOT EXISTS，可重复执行。
-- 3. 本脚本只新增和回填，不删除、重命名、收紧原 t_user 字段，不修改用户 ID、密码和角色关系。
-- 4. 若人工修改过本任务新表结构，CREATE TABLE IF NOT EXISTS 不负责修复该漂移，必须先人工核对。
-- 5. UNASSIGNED_ORG 的 migration_placeholder=1；任何管理范围、负责人资格和业务数据范围查询都必须排除它。

CALL crm_require_migration_context('20260711_task09_organization_foundation');

SELECT COUNT(*) AS user_count_before FROM t_user;
SELECT COUNT(*) AS user_role_count_before FROM t_user_role;

DROP PROCEDURE IF EXISTS task09_add_column_if_missing;
DELIMITER $$
CREATE PROCEDURE task09_add_column_if_missing(
    IN target_table VARCHAR(64),
    IN target_column VARCHAR(64),
    IN ddl_sql TEXT
)
BEGIN
    CALL crm_require_migration_context('20260711_task09_organization_foundation');
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = target_table
          AND COLUMN_NAME = target_column
    ) THEN
        SET @task09_ddl = ddl_sql;
        PREPARE task09_stmt FROM @task09_ddl;
        EXECUTE task09_stmt;
        DEALLOCATE PREPARE task09_stmt;
    END IF;
END$$
DELIMITER ;

CALL task09_add_column_if_missing(
    't_user', 'account_type',
    'ALTER TABLE t_user ADD COLUMN account_type VARCHAR(16) NOT NULL DEFAULT ''HUMAN'' COMMENT ''账号类型：SYSTEM-系统恢复账号，HUMAN-人员账号'''
);
CALL task09_add_column_if_missing(
    't_user', 'protected_account',
    'ALTER TABLE t_user ADD COLUMN protected_account TINYINT(1) NOT NULL DEFAULT 0 COMMENT ''是否为受保护恢复账号：0否，1是'''
);
CALL task09_add_column_if_missing(
    't_user', 'version',
    'ALTER TABLE t_user ADD COLUMN version INT NOT NULL DEFAULT 0 COMMENT ''并发更新版本'''
);
DROP PROCEDURE task09_add_column_if_missing;

-- 恢复账号身份是固定迁移前提；不满足时立即中止，避免把恢复账号误建为普通员工。
DROP PROCEDURE IF EXISTS task09_validate_recovery_account;
DELIMITER $$
CREATE PROCEDURE task09_validate_recovery_account()
BEGIN
    CALL crm_require_migration_context('20260711_task09_organization_foundation');
    IF (SELECT COUNT(*) FROM t_user WHERE id = 1 AND login_act = 'admin') <> 1 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Task09迁移中止：未找到固定恢复账号 id=1/login_act=admin';
    END IF;
END$$
DELIMITER ;
CALL task09_validate_recovery_account();
DROP PROCEDURE task09_validate_recovery_account;

-- 只标记已知内置恢复账号；不根据角色名把其他管理员推断为系统账号。
DROP PROCEDURE IF EXISTS task09_mark_recovery_account;
DELIMITER $$
CREATE PROCEDURE task09_mark_recovery_account()
BEGIN
    CALL crm_require_migration_context('20260711_task09_organization_foundation');
    UPDATE t_user
    SET account_type = 'SYSTEM', protected_account = 1
    WHERE id = 1 AND login_act = 'admin';
END$$
DELIMITER ;
CALL task09_mark_recovery_account();
DROP PROCEDURE task09_mark_recovery_account;

DROP PROCEDURE IF EXISTS task09_add_constraint_if_missing;
DELIMITER $$
CREATE PROCEDURE task09_add_constraint_if_missing(
    IN target_table VARCHAR(64),
    IN target_constraint VARCHAR(64),
    IN ddl_sql TEXT
)
BEGIN
    CALL crm_require_migration_context('20260711_task09_organization_foundation');
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.TABLE_CONSTRAINTS
        WHERE CONSTRAINT_SCHEMA = DATABASE()
          AND TABLE_NAME = target_table
          AND CONSTRAINT_NAME = target_constraint
    ) THEN
        SET @task09_ddl = ddl_sql;
        PREPARE task09_stmt FROM @task09_ddl;
        EXECUTE task09_stmt;
        DEALLOCATE PREPARE task09_stmt;
    END IF;
END$$
DELIMITER ;

-- 只约束本任务新增兼容字段，不收紧原 t_user 字段。
CALL task09_add_constraint_if_missing(
    't_user', 'chk_user_account_type',
    'ALTER TABLE t_user ADD CONSTRAINT chk_user_account_type CHECK (account_type IN (''SYSTEM'', ''HUMAN''))'
);
CALL task09_add_constraint_if_missing(
    't_user', 'chk_user_protected_account',
    'ALTER TABLE t_user ADD CONSTRAINT chk_user_protected_account CHECK (protected_account IN (0, 1))'
);
CALL task09_add_constraint_if_missing(
    't_user', 'chk_user_account_protection',
    'ALTER TABLE t_user ADD CONSTRAINT chk_user_account_protection CHECK ((account_type = ''SYSTEM'' AND protected_account = 1) OR (account_type = ''HUMAN'' AND protected_account = 0))'
);
CALL task09_add_constraint_if_missing(
    't_user', 'chk_user_version',
    'ALTER TABLE t_user ADD CONSTRAINT chk_user_version CHECK (version >= 0)'
);
DROP PROCEDURE task09_add_constraint_if_missing;
CALL crm_migration_mark_step('20260711_task09_organization_foundation', 'USER_COMPATIBILITY_FIELDS_READY');

DROP PROCEDURE IF EXISTS task09_create_foundation_tables;
DELIMITER $$
CREATE PROCEDURE task09_create_foundation_tables()
BEGIN
    CALL crm_require_migration_context('20260711_task09_organization_foundation');
    CREATE TABLE IF NOT EXISTS t_employee
(
    id                INT NOT NULL AUTO_INCREMENT,
    user_id           INT NULL,
    employee_no       VARCHAR(32) NOT NULL,
    name              VARCHAR(64) NOT NULL,
    phone             VARCHAR(18) NULL,
    email             VARCHAR(64) NULL,
    avatar_url        VARCHAR(255) NULL,
    employment_status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    profile_completed TINYINT(1) NOT NULL DEFAULT 0,
    hire_date         DATE NULL,
    leave_date        DATE NULL,
    version           INT NOT NULL DEFAULT 0,
    create_time       DATETIME NOT NULL,
    create_by         INT NULL,
    edit_time         DATETIME NULL,
    edit_by           INT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_employee_user (user_id),
    UNIQUE KEY uk_employee_no (employee_no),
    UNIQUE KEY uk_employee_phone (phone),
    UNIQUE KEY uk_employee_email (email),
    CONSTRAINT fk_employee_user FOREIGN KEY (user_id) REFERENCES t_user (id) ON DELETE RESTRICT,
    CONSTRAINT chk_employee_status CHECK (employment_status IN ('PENDING', 'ACTIVE', 'HANDOVER', 'LEFT')),
    CONSTRAINT chk_employee_profile_completed CHECK (profile_completed IN (0, 1)),
    CONSTRAINT chk_employee_dates CHECK (leave_date IS NULL OR hire_date IS NULL OR leave_date >= hire_date),
    CONSTRAINT chk_employee_version CHECK (version >= 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '员工档案表';

    CREATE TABLE IF NOT EXISTS t_organization_unit
(
    id                    INT NOT NULL AUTO_INCREMENT,
    code                  VARCHAR(64) NOT NULL,
    name                  VARCHAR(64) NOT NULL,
    type                  VARCHAR(16) NOT NULL,
    parent_id             INT NULL,
    leader_employee_id    INT NULL,
    order_no              INT NOT NULL DEFAULT 0,
    migration_placeholder TINYINT(1) NOT NULL DEFAULT 0,
    enabled               TINYINT(1) NOT NULL DEFAULT 1,
    version               INT NOT NULL DEFAULT 0,
    create_time           DATETIME NOT NULL,
    create_by             INT NULL,
    edit_time             DATETIME NULL,
    edit_by               INT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_organization_unit_code (code),
    KEY idx_organization_unit_parent_order (parent_id, order_no, id),
    KEY idx_organization_unit_leader (leader_employee_id),
    CONSTRAINT fk_organization_unit_parent FOREIGN KEY (parent_id) REFERENCES t_organization_unit (id) ON DELETE RESTRICT,
    CONSTRAINT fk_organization_unit_leader FOREIGN KEY (leader_employee_id) REFERENCES t_employee (id) ON DELETE RESTRICT,
    CONSTRAINT chk_organization_unit_type CHECK (type IN ('COMPANY', 'STORE', 'DEPARTMENT', 'TEAM')),
    CONSTRAINT chk_organization_unit_migration_placeholder CHECK (migration_placeholder IN (0, 1)),
    CONSTRAINT chk_organization_unit_enabled CHECK (enabled IN (0, 1)),
    CONSTRAINT chk_organization_unit_order CHECK (order_no >= 0),
    CONSTRAINT chk_organization_unit_version CHECK (version >= 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '组织单元表';

    CREATE TABLE IF NOT EXISTS t_position
(
    id             INT NOT NULL AUTO_INCREMENT,
    code           VARCHAR(64) NOT NULL,
    name           VARCHAR(64) NOT NULL,
    description    VARCHAR(255) NULL,
    position_level INT NOT NULL DEFAULT 0,
    built_in       TINYINT(1) NOT NULL DEFAULT 0,
    enabled        TINYINT(1) NOT NULL DEFAULT 1,
    version        INT NOT NULL DEFAULT 0,
    create_time    DATETIME NOT NULL,
    create_by      INT NULL,
    edit_time      DATETIME NULL,
    edit_by        INT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_position_code (code),
    KEY idx_position_level_code (position_level, code, id),
    CONSTRAINT chk_position_level CHECK (position_level >= 0),
    CONSTRAINT chk_position_built_in CHECK (built_in IN (0, 1)),
    CONSTRAINT chk_position_enabled CHECK (enabled IN (0, 1)),
    CONSTRAINT chk_position_version CHECK (version >= 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '岗位目录表';

    CREATE TABLE IF NOT EXISTS t_employee_assignment
(
    id                    INT NOT NULL AUTO_INCREMENT,
    employee_id           INT NOT NULL,
    organization_unit_id  INT NOT NULL,
    position_id           INT NOT NULL,
    assignment_type       VARCHAR(16) NOT NULL,
    status                VARCHAR(16) NOT NULL,
    active_primary_marker TINYINT(1) NULL,
    effective_from        DATETIME NOT NULL,
    effective_to          DATETIME NULL,
    reason                VARCHAR(500) NOT NULL,
    version               INT NOT NULL DEFAULT 0,
    create_time           DATETIME NOT NULL,
    create_by             INT NULL,
    edit_time             DATETIME NULL,
    edit_by               INT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_employee_active_primary (employee_id, active_primary_marker),
    KEY idx_employee_assignment_effective (employee_id, status, effective_from, effective_to, id),
    KEY idx_employee_assignment_org (organization_unit_id, status, id),
    KEY idx_employee_assignment_position (position_id, status, id),
    CONSTRAINT fk_employee_assignment_employee FOREIGN KEY (employee_id) REFERENCES t_employee (id) ON DELETE RESTRICT,
    CONSTRAINT fk_employee_assignment_org FOREIGN KEY (organization_unit_id) REFERENCES t_organization_unit (id) ON DELETE RESTRICT,
    CONSTRAINT fk_employee_assignment_position FOREIGN KEY (position_id) REFERENCES t_position (id) ON DELETE RESTRICT,
    CONSTRAINT chk_employee_assignment_type CHECK (assignment_type IN ('PRIMARY', 'SECONDARY', 'ACTING')),
    CONSTRAINT chk_employee_assignment_status CHECK (status IN ('PLANNED', 'ACTIVE', 'ENDED', 'CANCELLED')),
    CONSTRAINT chk_employee_assignment_period CHECK (effective_to IS NULL OR effective_to > effective_from),
    CONSTRAINT chk_employee_assignment_primary_marker CHECK (
        (assignment_type = 'PRIMARY' AND status = 'ACTIVE' AND active_primary_marker = 1)
        OR ((assignment_type <> 'PRIMARY' OR status <> 'ACTIVE') AND active_primary_marker IS NULL)
    ),
    CONSTRAINT chk_employee_assignment_version CHECK (version >= 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '员工任职事实表';

    CREATE TABLE IF NOT EXISTS t_employee_reporting
(
    id                      INT NOT NULL AUTO_INCREMENT,
    subordinate_employee_id INT NOT NULL,
    manager_employee_id     INT NOT NULL,
    relation_type           VARCHAR(16) NOT NULL,
    status                  VARCHAR(16) NOT NULL,
    active_direct_marker    TINYINT(1) NULL,
    effective_from          DATETIME NOT NULL,
    effective_to            DATETIME NULL,
    reason                  VARCHAR(500) NOT NULL,
    version                 INT NOT NULL DEFAULT 0,
    create_time             DATETIME NOT NULL,
    create_by               INT NULL,
    edit_time               DATETIME NULL,
    edit_by                 INT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_employee_active_direct_manager (subordinate_employee_id, active_direct_marker),
    KEY idx_employee_reporting_manager (manager_employee_id, status, effective_from, effective_to, id),
    CONSTRAINT fk_employee_reporting_subordinate FOREIGN KEY (subordinate_employee_id) REFERENCES t_employee (id) ON DELETE RESTRICT,
    CONSTRAINT fk_employee_reporting_manager FOREIGN KEY (manager_employee_id) REFERENCES t_employee (id) ON DELETE RESTRICT,
    CONSTRAINT chk_employee_reporting_not_self CHECK (subordinate_employee_id <> manager_employee_id),
    CONSTRAINT chk_employee_reporting_type CHECK (relation_type IN ('DIRECT', 'ACTING')),
    CONSTRAINT chk_employee_reporting_status CHECK (status IN ('PLANNED', 'ACTIVE', 'ENDED', 'CANCELLED')),
    CONSTRAINT chk_employee_reporting_period CHECK (effective_to IS NULL OR effective_to > effective_from),
    CONSTRAINT chk_employee_reporting_direct_marker CHECK (
        (relation_type = 'DIRECT' AND status = 'ACTIVE' AND active_direct_marker = 1)
        OR ((relation_type <> 'DIRECT' OR status <> 'ACTIVE') AND active_direct_marker IS NULL)
    ),
    CONSTRAINT chk_employee_reporting_version CHECK (version >= 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '员工汇报关系事实表';
END$$
DELIMITER ;
CALL task09_create_foundation_tables();
DROP PROCEDURE task09_create_foundation_tables;
CALL crm_migration_mark_step('20260711_task09_organization_foundation', 'ORGANIZATION_TABLES_READY');

DROP PROCEDURE IF EXISTS task09_seed_placeholder_objects;
DELIMITER $$
CREATE PROCEDURE task09_seed_placeholder_objects()
BEGIN
    CALL crm_require_migration_context('20260711_task09_organization_foundation');
    INSERT INTO t_organization_unit
(code, name, type, parent_id, leader_employee_id, order_no, migration_placeholder,
 enabled, version, create_time, create_by)
SELECT 'DEFAULT_COMPANY', '默认公司', 'COMPANY', NULL, NULL, 0, 0, 1, 0, NOW(), 1
WHERE NOT EXISTS (SELECT 1 FROM t_organization_unit WHERE code = 'DEFAULT_COMPANY');

INSERT INTO t_organization_unit
(code, name, type, parent_id, leader_employee_id, order_no, migration_placeholder,
 enabled, version, create_time, create_by)
SELECT 'UNASSIGNED_ORG', '待分配组织', 'TEAM', company.id, NULL, 999, 1, 1, 0, NOW(), 1
FROM t_organization_unit company
WHERE company.code = 'DEFAULT_COMPANY'
  AND NOT EXISTS (SELECT 1 FROM t_organization_unit WHERE code = 'UNASSIGNED_ORG');

INSERT INTO t_position
(code, name, description, position_level, built_in, enabled, version, create_time, create_by)
SELECT 'UNASSIGNED_POSITION', '待分配岗位', '兼容迁移占位岗位，完成员工资料补录后应替换。',
       0, 1, 1, 0, NOW(), 1
WHERE NOT EXISTS (SELECT 1 FROM t_position WHERE code = 'UNASSIGNED_POSITION');
END$$
DELIMITER ;
CALL task09_seed_placeholder_objects();
DROP PROCEDURE task09_seed_placeholder_objects;

-- 稳定 code 若已被其他数据占用必须中止，不能静默把迁移员工绑定到普通业务对象。
DROP PROCEDURE IF EXISTS task09_validate_seed_objects;
DELIMITER $$
CREATE PROCEDURE task09_validate_seed_objects()
BEGIN
    CALL crm_require_migration_context('20260711_task09_organization_foundation');
    IF (SELECT COUNT(*) FROM t_organization_unit
        WHERE code = 'DEFAULT_COMPANY' AND type = 'COMPANY'
          AND parent_id IS NULL AND migration_placeholder = 0) <> 1 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Task09迁移中止：DEFAULT_COMPANY与预期根公司不一致';
    END IF;
    IF (SELECT COUNT(*)
        FROM t_organization_unit unassigned
        INNER JOIN t_organization_unit company ON company.id = unassigned.parent_id
        WHERE unassigned.code = 'UNASSIGNED_ORG' AND unassigned.type = 'TEAM'
          AND unassigned.migration_placeholder = 1 AND company.code = 'DEFAULT_COMPANY') <> 1 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Task09迁移中止：UNASSIGNED_ORG与预期占位组织不一致';
    END IF;
    IF (SELECT COUNT(*) FROM t_position
        WHERE code = 'UNASSIGNED_POSITION' AND built_in = 1) <> 1 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Task09迁移中止：UNASSIGNED_POSITION与预期占位岗位不一致';
    END IF;
END$$
DELIMITER ;
CALL task09_validate_seed_objects();
DROP PROCEDURE task09_validate_seed_objects;
CALL crm_migration_mark_step('20260711_task09_organization_foundation', 'PLACEHOLDER_SEEDS_READY');

DROP PROCEDURE IF EXISTS task09_backfill_employee_foundation;
DELIMITER $$
CREATE PROCEDURE task09_backfill_employee_foundation()
BEGIN
    CALL crm_require_migration_context('20260711_task09_organization_foundation');
    INSERT INTO t_employee
(user_id, employee_no, name, phone, email, employment_status, profile_completed,
 hire_date, version, create_time, create_by)
SELECT u.id, CONCAT('EMP-', LPAD(u.id, 6, '0')), u.name, u.phone, u.email,
       CASE WHEN COALESCE(u.account_enabled, 0) = 1 THEN 'ACTIVE' ELSE 'PENDING' END,
       0, NULL, 0, NOW(), 1
FROM t_user u
WHERE u.account_type = 'HUMAN'
  AND NOT EXISTS (SELECT 1 FROM t_employee e WHERE e.user_id = u.id);

-- 该主任职只满足迁移兼容和唯一约束，不表达真实门店、团队、管理范围或负责人资格。
INSERT INTO t_employee_assignment
(employee_id, organization_unit_id, position_id, assignment_type, status,
 active_primary_marker, effective_from, reason, version, create_time, create_by)
SELECT e.id, organization_unit.id, position.id, 'PRIMARY', 'ACTIVE', 1,
       COALESCE(u.create_time, NOW()), '兼容迁移生成的待补录主要任职', 0, NOW(), 1
FROM t_employee e
INNER JOIN t_user u ON u.id = e.user_id
INNER JOIN t_organization_unit organization_unit ON organization_unit.code = 'UNASSIGNED_ORG'
INNER JOIN t_position position ON position.code = 'UNASSIGNED_POSITION'
WHERE e.employment_status = 'ACTIVE'
  AND COALESCE(u.account_enabled, 0) = 1
  AND NOT EXISTS (
    SELECT 1
    FROM t_employee_assignment assignment
    WHERE assignment.employee_id = e.id
      AND assignment.assignment_type = 'PRIMARY'
      AND assignment.status = 'ACTIVE'
      AND assignment.active_primary_marker = 1
);
END$$
DELIMITER ;
CALL task09_backfill_employee_foundation();
DROP PROCEDURE task09_backfill_employee_foundation;
CALL crm_migration_mark_step('20260711_task09_organization_foundation', 'COMPATIBILITY_BACKFILL_READY');

-- 迁移核验：以下查询均应返回 0；placeholder_assignment_count 是待业务补录数量，不应进入任何管理范围计算。
SELECT COUNT(*) AS human_user_without_employee
FROM t_user u
LEFT JOIN t_employee e ON e.user_id = u.id
WHERE u.account_type = 'HUMAN' AND e.id IS NULL;

SELECT COUNT(*) AS protected_system_account_with_employee
FROM t_user u
INNER JOIN t_employee e ON e.user_id = u.id
WHERE u.protected_account = 1 OR u.account_type = 'SYSTEM';

SELECT COUNT(*) AS employee_without_primary_assignment
FROM t_employee e
LEFT JOIN t_employee_assignment assignment
  ON assignment.employee_id = e.id
 AND assignment.assignment_type = 'PRIMARY'
 AND assignment.status = 'ACTIVE'
 AND assignment.active_primary_marker = 1
WHERE assignment.id IS NULL;

SELECT COUNT(*) AS orphan_assignment_count
FROM t_employee_assignment assignment
LEFT JOIN t_employee employee ON employee.id = assignment.employee_id
LEFT JOIN t_organization_unit organization_unit ON organization_unit.id = assignment.organization_unit_id
LEFT JOIN t_position position ON position.id = assignment.position_id
WHERE employee.id IS NULL OR organization_unit.id IS NULL OR position.id IS NULL;

SELECT COUNT(*) AS invalid_assignment_period_count
FROM t_employee_assignment
WHERE effective_to IS NOT NULL AND effective_to <= effective_from;

SELECT COUNT(*) AS placeholder_assignment_count
FROM t_employee_assignment assignment
INNER JOIN t_organization_unit organization_unit ON organization_unit.id = assignment.organization_unit_id
WHERE organization_unit.migration_placeholder = 1
  AND assignment.status = 'ACTIVE';

SELECT COUNT(*) AS user_count_after FROM t_user;
SELECT COUNT(*) AS user_role_count_after FROM t_user_role;
