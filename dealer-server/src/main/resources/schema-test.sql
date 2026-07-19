-- H2 Database Schema for Testing
-- Converted from MySQL CarDealerCRM.sql
-- 与 CarDealerCRM.sql 保持等价约束：唯一约束、外键、CHECK、NOT NULL、value_code、BIGINT 对齐

CREATE TABLE IF NOT EXISTS t_activity
(
    id          INTEGER NOT NULL AUTO_INCREMENT,
    owner_id    INTEGER,
    name        VARCHAR(128),
    status      VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    channel     VARCHAR(64) NOT NULL DEFAULT 'OFFLINE_EVENT',
    target_model VARCHAR(128),
    start_time  TIMESTAMP,
    end_time    TIMESTAMP,
    cost        DECIMAL(11, 2),
    actual_cost DECIMAL(11, 2),
    description VARCHAR(255),
    result_summary VARCHAR(500),
    review_conclusion VARCHAR(500),
    reviewed_by INTEGER,
    reviewed_time TIMESTAMP,
    closed_reason VARCHAR(500),
    canceled_reason VARCHAR(500),
    create_time TIMESTAMP,
    create_by   INTEGER,
    edit_time   TIMESTAMP,
    edit_by           INTEGER,
    PRIMARY KEY (id),
    CONSTRAINT chk_activity_status CHECK (status IN ('DRAFT', 'PLANNED', 'ONGOING', 'ENDED', 'REVIEWED', 'CLOSED', 'CANCELED')),
    CONSTRAINT chk_activity_cost CHECK (cost IS NULL OR cost >= 0),
    CONSTRAINT chk_activity_actual_cost CHECK (actual_cost IS NULL OR actual_cost >= 0),
    CONSTRAINT chk_activity_time_range CHECK (start_time IS NULL OR end_time IS NULL OR end_time > start_time)
);

CREATE INDEX IF NOT EXISTS idx_activity_status_time ON t_activity(status, start_time, id);

CREATE TABLE IF NOT EXISTS t_activity_remark
(
    id           INTEGER NOT NULL AUTO_INCREMENT,
    activity_id  INTEGER,
    note_content VARCHAR(255),
    create_time  TIMESTAMP,
    create_by    INTEGER,
    edit_time    TIMESTAMP,
    edit_by      INTEGER,
    deleted      INTEGER,
    PRIMARY KEY (id),
    CONSTRAINT fk_activity_remark_activity FOREIGN KEY (activity_id) REFERENCES t_activity(id) ON DELETE RESTRICT
);

CREATE TABLE IF NOT EXISTS t_clue
(
    id                INTEGER NOT NULL AUTO_INCREMENT,
    owner_id          INTEGER,
    activity_id       INTEGER,
    activity_name_snapshot VARCHAR(128),
    full_name         VARCHAR(64),
    appellation       INTEGER,
    phone             VARCHAR(18),
    weixin            VARCHAR(128),
    qq                VARCHAR(20),
    email             VARCHAR(128),
    age               INTEGER,
    job               VARCHAR(64),
    year_income       DECIMAL(10, 2),
    address           VARCHAR(128),
    need_loan         INTEGER,
    intention_state   INTEGER,
    intention_product INTEGER,
    state             INTEGER,
    source            INTEGER,
    description       VARCHAR(255),
    next_contact_time TIMESTAMP,
    last_follow_time  TIMESTAMP,
    last_follow_summary VARCHAR(255),
    create_time       TIMESTAMP,
    create_by         INTEGER,
    edit_time         TIMESTAMP,
    edit_by     INTEGER,
    PRIMARY KEY (id),
    CONSTRAINT uk_clue_phone UNIQUE (phone)
);

CREATE TABLE IF NOT EXISTS t_clue_remark
(
    id           INTEGER NOT NULL AUTO_INCREMENT,
    clue_id      INTEGER,
    note_way     INTEGER,
    note_content VARCHAR(255),
    create_time  TIMESTAMP,
    create_by    INTEGER,
    edit_time    TIMESTAMP,
    edit_by      INTEGER,
    deleted      INTEGER,
    PRIMARY KEY (id),
    CONSTRAINT fk_clue_remark_clue FOREIGN KEY (clue_id) REFERENCES t_clue(id) ON DELETE RESTRICT
);

CREATE TABLE IF NOT EXISTS t_customer
(
    id                INTEGER NOT NULL AUTO_INCREMENT,
    clue_id           INTEGER,
    owner_id          INTEGER,
    activity_id       INTEGER,
    activity_name_snapshot VARCHAR(128),
    customer_name     VARCHAR(128),
    appellation       INTEGER,
    phone             VARCHAR(32),
    weixin            VARCHAR(128),
    qq                VARCHAR(32),
    email             VARCHAR(128),
    age               INTEGER,
    job               VARCHAR(64),
    year_income       DECIMAL(10, 2),
    address           VARCHAR(255),
    need_loan         INTEGER,
    intention_state   INTEGER,
    source            INTEGER,
    original_clue_source INTEGER,
    product           BIGINT,
    customer_status   VARCHAR(32) DEFAULT 'INTENTION',
    merged_to_customer_id INTEGER,
    merge_reason      VARCHAR(255),
    merge_time        TIMESTAMP,
    merge_by          INTEGER,
    description       VARCHAR(255),
    next_contact_time TIMESTAMP,
    last_follow_time  TIMESTAMP,
    last_follow_summary VARCHAR(255),
    create_time       TIMESTAMP,
    create_by         INTEGER,
    edit_time         TIMESTAMP,
    edit_by           INTEGER,
    PRIMARY KEY (id),
    CONSTRAINT fk_customer_clue FOREIGN KEY (clue_id) REFERENCES t_clue(id) ON DELETE RESTRICT
);

CREATE INDEX IF NOT EXISTS idx_customer_owner_status ON t_customer(owner_id, customer_status);
CREATE INDEX IF NOT EXISTS idx_customer_phone ON t_customer(phone);
CREATE INDEX IF NOT EXISTS idx_customer_weixin ON t_customer(weixin);

CREATE TABLE IF NOT EXISTS t_customer_remark
(
    id           INTEGER NOT NULL AUTO_INCREMENT,
    customer_id  INTEGER,
    note_way     INTEGER,
    note_content VARCHAR(255),
    create_by    INTEGER,
    create_time  TIMESTAMP,
    edit_time    TIMESTAMP,
    edit_by      INTEGER,
    deleted      INTEGER,
    PRIMARY KEY (id),
    CONSTRAINT fk_customer_remark_customer FOREIGN KEY (customer_id) REFERENCES t_customer(id) ON DELETE RESTRICT
);

CREATE TABLE IF NOT EXISTS t_customer_owner_history
(
    id            INTEGER NOT NULL AUTO_INCREMENT,
    customer_id   INTEGER NOT NULL,
    from_owner_id INTEGER,
    to_owner_id   INTEGER NOT NULL,
    reason        VARCHAR(255) NOT NULL,
    operator_id   INTEGER NOT NULL,
    transfer_time TIMESTAMP NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_customer_owner_history_customer FOREIGN KEY (customer_id) REFERENCES t_customer(id) ON DELETE RESTRICT
);

CREATE TABLE IF NOT EXISTS t_dic_type
(
    id                INTEGER NOT NULL AUTO_INCREMENT,
    type_code         VARCHAR(64) NOT NULL,
    type_name         VARCHAR(64),
    applicable_module VARCHAR(64),
    enabled           TINYINT NOT NULL DEFAULT 1,
    built_in          TINYINT NOT NULL DEFAULT 0,
    disable_reason    VARCHAR(255),
    disabled_by       INTEGER,
    disabled_time     TIMESTAMP,
    remark            VARCHAR(128),
    PRIMARY KEY (id),
    CONSTRAINT uk_type_code UNIQUE (type_code),
    CONSTRAINT chk_dic_type_enabled CHECK (enabled IN (0, 1)),
    CONSTRAINT chk_dic_type_built_in CHECK (built_in IN (0, 1))
);

CREATE TABLE IF NOT EXISTS t_dic_value
(
    id                INTEGER NOT NULL AUTO_INCREMENT,
    type_code         VARCHAR(64) NOT NULL,
    type_value        VARCHAR(64),
    value_code        VARCHAR(64) NOT NULL,
    `order`           INTEGER,
    applicable_module VARCHAR(64),
    enabled           TINYINT NOT NULL DEFAULT 1,
    built_in          TINYINT NOT NULL DEFAULT 0,
    disable_reason    VARCHAR(255),
    disabled_by       INTEGER,
    disabled_time     TIMESTAMP,
    remark            VARCHAR(64),
    PRIMARY KEY (id),
    CONSTRAINT uk_type_value_code UNIQUE (type_code, value_code),
    CONSTRAINT chk_dic_value_enabled CHECK (enabled IN (0, 1)),
    CONSTRAINT chk_dic_value_built_in CHECK (built_in IN (0, 1)),
    CONSTRAINT fk_dic_value_type_code FOREIGN KEY (type_code) REFERENCES t_dic_type(type_code) ON DELETE RESTRICT
);

CREATE TABLE IF NOT EXISTS t_permission
(
    id        INTEGER NOT NULL AUTO_INCREMENT,
    name      VARCHAR(64) NOT NULL,
    code      VARCHAR(64) NOT NULL,
    url       VARCHAR(255),
    type      VARCHAR(30) NOT NULL,
    parent_id INTEGER,
    order_no  INTEGER,
    icon      VARCHAR(100),
    module    VARCHAR(64) NOT NULL DEFAULT 'system',
    description VARCHAR(255),
    sensitivity_level VARCHAR(16) NOT NULL DEFAULT 'NORMAL',
    delegable TINYINT NOT NULL DEFAULT 0,
    enabled   TINYINT NOT NULL DEFAULT 1,
    version   INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_permission_code UNIQUE (code),
    CONSTRAINT chk_permission_type CHECK (type IN ('menu', 'button')),
    CONSTRAINT chk_permission_sensitivity CHECK (sensitivity_level IN ('NORMAL', 'SENSITIVE', 'PROTECTED')),
    CONSTRAINT chk_permission_delegable CHECK (delegable IN (0, 1)),
    CONSTRAINT chk_permission_version CHECK (version >= 0),
    CONSTRAINT chk_permission_parent_self CHECK (parent_id IS NULL OR parent_id <> id),
    CONSTRAINT fk_permission_parent FOREIGN KEY (parent_id) REFERENCES t_permission(id) ON DELETE RESTRICT
);

CREATE TABLE IF NOT EXISTS t_role
(
    id        INTEGER NOT NULL AUTO_INCREMENT,
    role      VARCHAR(64) NOT NULL,
    role_name VARCHAR(64) NOT NULL,
    description VARCHAR(255),
    protected_role TINYINT NOT NULL DEFAULT 0,
    authorization_level INTEGER NOT NULL DEFAULT 0,
    default_data_scope VARCHAR(32) NOT NULL DEFAULT 'SELF',
    scope_type VARCHAR(16) NOT NULL DEFAULT 'GLOBAL',
    enabled   TINYINT NOT NULL DEFAULT 1,
    version   INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_role_code UNIQUE (role),
    CONSTRAINT chk_role_protected CHECK (protected_role IN (0, 1)),
    CONSTRAINT chk_role_authorization_level CHECK (authorization_level >= 0),
    CONSTRAINT chk_role_data_scope CHECK (default_data_scope IN ('SELF', 'DIRECT_REPORTS', 'REPORTING_TREE', 'PRIMARY_ORG', 'ORG_TREE', 'CUSTOM_ORGS', 'GLOBAL')),
    CONSTRAINT chk_role_scope_type CHECK (scope_type IN ('GLOBAL', 'ORGANIZATION')),
    CONSTRAINT chk_role_enabled CHECK (enabled IN (0, 1)),
    CONSTRAINT chk_role_version CHECK (version >= 0)
);

CREATE TABLE IF NOT EXISTS t_role_permission
(
    role_id       INTEGER NOT NULL,
    permission_id INTEGER NOT NULL,
    delegable     TINYINT NOT NULL DEFAULT 0,
    data_scope_code VARCHAR(32) NOT NULL DEFAULT 'SELF',
    PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_role_permission_role FOREIGN KEY (role_id) REFERENCES t_role(id) ON DELETE RESTRICT,
    CONSTRAINT fk_role_permission_permission FOREIGN KEY (permission_id) REFERENCES t_permission(id) ON DELETE RESTRICT,
    CONSTRAINT chk_role_permission_delegable CHECK (delegable IN (0, 1)),
    CONSTRAINT chk_role_permission_data_scope CHECK (data_scope_code IN ('SELF', 'DIRECT_REPORTS', 'REPORTING_TREE', 'PRIMARY_ORG', 'ORG_TREE', 'CUSTOM_ORGS', 'GLOBAL'))
);

CREATE TABLE IF NOT EXISTS t_user
(
    id                     INTEGER NOT NULL AUTO_INCREMENT,
    login_act              VARCHAR(32),
    login_pwd              VARCHAR(64),
    name                   VARCHAR(64),
    phone                  VARCHAR(18),
    email                  VARCHAR(64),
    avatar_url             VARCHAR(500),
    profile_version        INTEGER NOT NULL DEFAULT 0,
    account_no_expired     INTEGER,
    credentials_no_expired INTEGER,
    account_no_locked      INTEGER,
    account_enabled        INTEGER,
    create_time            TIMESTAMP,
    create_by              INTEGER,
    edit_time              TIMESTAMP,
    edit_by                INTEGER,
    last_login_time        TIMESTAMP,
    account_type           VARCHAR(16) NOT NULL DEFAULT 'HUMAN',
    protected_account      TINYINT NOT NULL DEFAULT 0,
    version                INTEGER NOT NULL DEFAULT 0,
    authorization_version  INTEGER NOT NULL DEFAULT 0,
    auth_version           BIGINT NOT NULL DEFAULT 0,
    session_revision       BIGINT NOT NULL DEFAULT 0,
    account_status         VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    must_change_password   TINYINT NOT NULL DEFAULT 0,
    failed_login_count     INTEGER NOT NULL DEFAULT 0,
    auto_locked_until      TIMESTAMP,
    manual_locked          TINYINT NOT NULL DEFAULT 0,
    manual_lock_reason     VARCHAR(500),
    manual_locked_by       INTEGER,
    manual_locked_at       TIMESTAMP,
    account_expires_at     TIMESTAMP,
    password_expires_at    TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_user_login_act UNIQUE (login_act),
    CONSTRAINT uk_user_email UNIQUE (email),
    CONSTRAINT uk_user_phone UNIQUE (phone),
    CONSTRAINT chk_user_account_type CHECK (account_type IN ('SYSTEM', 'HUMAN')),
    CONSTRAINT chk_user_protected_account CHECK (protected_account IN (0, 1)),
    CONSTRAINT chk_user_account_protection CHECK (
        (account_type = 'SYSTEM' AND protected_account = 1)
        OR (account_type = 'HUMAN' AND protected_account = 0)
    ),
    CONSTRAINT chk_user_recovery_login_act CHECK (
        (protected_account = 1 AND login_act IS NOT NULL AND LOWER(login_act) = 'admin')
        OR (protected_account = 0 AND (login_act IS NULL OR LOWER(login_act) <> 'admin'))
    ),
    CONSTRAINT chk_user_version CHECK (version >= 0),
    CONSTRAINT chk_user_authorization_version CHECK (authorization_version >= 0),
    CONSTRAINT chk_user_auth_version CHECK (auth_version >= 0)
    ,CONSTRAINT chk_user_session_revision CHECK (session_revision >= 0)
    ,CONSTRAINT chk_user_profile_version CHECK (profile_version >= 0)
    ,CONSTRAINT chk_user_account_status CHECK (account_status IN ('INVITED','ACTIVE','DISABLED'))
    ,CONSTRAINT chk_user_must_change_password CHECK (must_change_password IN (0,1))
    ,CONSTRAINT chk_user_failed_login_count CHECK (failed_login_count >= 0)
    ,CONSTRAINT chk_user_manual_locked CHECK (manual_locked IN (0,1))
);
CREATE INDEX IF NOT EXISTS idx_user_workspace_status
    ON t_user(account_status, manual_locked, auto_locked_until, id);
CREATE INDEX IF NOT EXISTS idx_user_workspace_last_login
    ON t_user(last_login_time, id);

CREATE TABLE IF NOT EXISTS t_user_session
(
    id BIGINT NOT NULL AUTO_INCREMENT,
    session_id VARCHAR(64) NOT NULL,
    user_id INTEGER NOT NULL,
    token_digest VARCHAR(64) NOT NULL,
    issued_auth_version BIGINT NOT NULL,
    remember_me TINYINT NOT NULL DEFAULT 0,
    device_summary VARCHAR(128) NOT NULL,
    client_summary VARCHAR(128),
    network_summary VARCHAR(128),
    login_time TIMESTAMP NOT NULL,
    last_activity_time TIMESTAMP NOT NULL,
    idle_expires_at TIMESTAMP NOT NULL,
    absolute_expires_at TIMESTAMP NOT NULL,
    revoked_at TIMESTAMP,
    revoked_by INTEGER,
    revoke_reason VARCHAR(500),
    revoke_type VARCHAR(32),
    version INTEGER NOT NULL DEFAULT 0,
    create_time TIMESTAMP NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_user_session_id UNIQUE(session_id),
    CONSTRAINT uk_user_session_token_digest UNIQUE(token_digest),
    CONSTRAINT fk_user_session_user FOREIGN KEY(user_id) REFERENCES t_user(id) ON DELETE RESTRICT,
    CONSTRAINT chk_user_session_remember CHECK(remember_me IN (0,1)),
    CONSTRAINT chk_user_session_version CHECK(version >= 0),
    CONSTRAINT chk_user_session_times CHECK(login_time <= last_activity_time AND last_activity_time < idle_expires_at AND idle_expires_at <= absolute_expires_at),
    CONSTRAINT chk_user_session_revocation CHECK((revoked_at IS NULL AND revoke_reason IS NULL AND revoke_type IS NULL) OR (revoked_at IS NOT NULL AND revoke_reason IS NOT NULL AND revoke_type IS NOT NULL))
);
CREATE INDEX IF NOT EXISTS idx_user_session_user_active ON t_user_session(user_id,revoked_at,login_time,session_id);
CREATE INDEX IF NOT EXISTS idx_user_session_retention ON t_user_session(revoked_at,id);

CREATE TABLE IF NOT EXISTS t_employee
(
    id                INTEGER NOT NULL AUTO_INCREMENT,
    user_id           INTEGER,
    employee_no       VARCHAR(32) NOT NULL,
    name              VARCHAR(64) NOT NULL,
    phone             VARCHAR(18),
    email             VARCHAR(64),
    avatar_url        VARCHAR(500),
    employment_status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    profile_completed TINYINT NOT NULL DEFAULT 0,
    hire_date         DATE,
    leave_date        DATE,
    version           INTEGER NOT NULL DEFAULT 0,
    profile_version   INTEGER NOT NULL DEFAULT 0,
    phone_verified    TINYINT NOT NULL DEFAULT 0,
    email_verified    TINYINT NOT NULL DEFAULT 0,
    create_time       TIMESTAMP NOT NULL,
    create_by         INTEGER,
    edit_time         TIMESTAMP,
    edit_by           INTEGER,
    PRIMARY KEY (id),
    CONSTRAINT uk_employee_user UNIQUE (user_id),
    CONSTRAINT uk_employee_no UNIQUE (employee_no),
    CONSTRAINT uk_employee_phone UNIQUE (phone),
    CONSTRAINT uk_employee_email UNIQUE (email),
    CONSTRAINT fk_employee_user FOREIGN KEY (user_id) REFERENCES t_user(id) ON DELETE RESTRICT,
    CONSTRAINT chk_employee_status CHECK (employment_status IN ('PENDING', 'ACTIVE', 'HANDOVER', 'LEFT')),
    CONSTRAINT chk_employee_profile_completed CHECK (profile_completed IN (0, 1)),
    CONSTRAINT chk_employee_dates CHECK (leave_date IS NULL OR hire_date IS NULL OR leave_date >= hire_date),
    CONSTRAINT chk_employee_version CHECK (version >= 0)
    ,CONSTRAINT chk_employee_profile_version CHECK (profile_version >= 0)
    ,CONSTRAINT chk_employee_phone_verified CHECK (phone_verified IN (0,1))
    ,CONSTRAINT chk_employee_email_verified CHECK (email_verified IN (0,1))
);

CREATE TABLE IF NOT EXISTS t_account_credential
(
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id INTEGER NOT NULL,
    purpose VARCHAR(24) NOT NULL,
    token_digest VARCHAR(64) NOT NULL,
    status VARCHAR(16) NOT NULL,
    active_marker TINYINT,
    expires_at TIMESTAMP NOT NULL,
    consumed_at TIMESTAMP,
    revoked_at TIMESTAMP,
    issued_by INTEGER,
    reason VARCHAR(500) NOT NULL,
    target_value_digest VARCHAR(64),
    target_profile_version INTEGER,
    version INTEGER NOT NULL DEFAULT 0,
    create_time TIMESTAMP NOT NULL,
    PRIMARY KEY(id),
    CONSTRAINT uk_account_credential_digest UNIQUE(token_digest),
    CONSTRAINT uk_account_credential_active UNIQUE(user_id,purpose,active_marker),
    CONSTRAINT fk_account_credential_user FOREIGN KEY(user_id) REFERENCES t_user(id) ON DELETE RESTRICT,
    CONSTRAINT chk_account_credential_purpose CHECK (purpose IN ('INVITATION','SELF_RESET','ADMIN_RESET','PHONE_VERIFY','EMAIL_VERIFY','BREAK_GLASS')),
    CONSTRAINT chk_account_credential_status CHECK (status IN ('ISSUED','CONSUMED','REVOKED')),
    CONSTRAINT chk_account_credential_contact_binding CHECK ((purpose IN ('PHONE_VERIFY','EMAIL_VERIFY') AND (status <> 'ISSUED' OR (target_value_digest IS NOT NULL AND target_profile_version IS NOT NULL))) OR (purpose NOT IN ('PHONE_VERIFY','EMAIL_VERIFY') AND target_value_digest IS NULL AND target_profile_version IS NULL)),
    CONSTRAINT chk_account_credential_version CHECK (version >= 0)
);

CREATE TABLE IF NOT EXISTS t_credential_delivery_outbox
(
    id BIGINT NOT NULL AUTO_INCREMENT,
    message_id VARCHAR(36) NOT NULL,
    credential_id BIGINT NOT NULL,
    user_id INTEGER NOT NULL,
    purpose VARCHAR(24) NOT NULL,
    derivation_nonce VARCHAR(64),
    phone_digest VARCHAR(64),
    email_digest VARCHAR(64),
    status VARCHAR(16) NOT NULL,
    attempt_count INTEGER NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMP NOT NULL,
    claimed_at TIMESTAMP,
    delivered_at TIMESTAMP,
    failed_at TIMESTAMP,
    last_error_code VARCHAR(64),
    version INTEGER NOT NULL DEFAULT 0,
    create_time TIMESTAMP NOT NULL,
    edit_time TIMESTAMP NOT NULL,
    PRIMARY KEY(id),
    CONSTRAINT uk_credential_delivery_message UNIQUE(message_id),
    CONSTRAINT uk_credential_delivery_credential UNIQUE(credential_id),
    CONSTRAINT fk_credential_delivery_credential FOREIGN KEY(credential_id) REFERENCES t_account_credential(id) ON DELETE RESTRICT,
    CONSTRAINT fk_credential_delivery_user FOREIGN KEY(user_id) REFERENCES t_user(id) ON DELETE RESTRICT,
    CONSTRAINT chk_credential_delivery_status CHECK (status IN ('PENDING','PROCESSING','RETRY','DELIVERED','FAILED')),
    CONSTRAINT chk_credential_delivery_attempt CHECK (attempt_count >= 0 AND version >= 0),
    CONSTRAINT chk_credential_delivery_contact CHECK (phone_digest IS NOT NULL OR email_digest IS NOT NULL),
    CONSTRAINT chk_credential_delivery_nonce CHECK ((status IN ('PENDING','PROCESSING','RETRY') AND derivation_nonce IS NOT NULL) OR (status IN ('DELIVERED','FAILED') AND derivation_nonce IS NULL))
);

CREATE TABLE IF NOT EXISTS t_password_history
(
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id INTEGER NOT NULL,
    password_hash VARCHAR(64) NOT NULL,
    changed_by INTEGER,
    change_reason VARCHAR(64) NOT NULL,
    changed_at TIMESTAMP NOT NULL,
    PRIMARY KEY(id),
    CONSTRAINT fk_password_history_user FOREIGN KEY(user_id) REFERENCES t_user(id) ON DELETE RESTRICT
);

CREATE TABLE IF NOT EXISTS t_login_identifier
(
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id INTEGER NOT NULL,
    login_act VARCHAR(32) NOT NULL,
    status VARCHAR(16) NOT NULL,
    active_marker TINYINT,
    retired_at TIMESTAMP,
    changed_by INTEGER,
    reason VARCHAR(500) NOT NULL,
    version INTEGER NOT NULL DEFAULT 0,
    create_time TIMESTAMP NOT NULL,
    PRIMARY KEY(id),
    CONSTRAINT uk_login_identifier_login_act UNIQUE(login_act),
    CONSTRAINT uk_login_identifier_active_user UNIQUE(user_id,active_marker),
    CONSTRAINT fk_login_identifier_user FOREIGN KEY(user_id) REFERENCES t_user(id) ON DELETE RESTRICT,
    CONSTRAINT fk_login_identifier_changed_by FOREIGN KEY(changed_by) REFERENCES t_user(id) ON DELETE RESTRICT,
    CONSTRAINT chk_login_identifier_state CHECK (
      (status='ACTIVE' AND active_marker=1 AND retired_at IS NULL)
      OR (status='RETIRED' AND active_marker IS NULL AND retired_at IS NOT NULL)
    ),
    CONSTRAINT chk_login_identifier_version CHECK (version >= 0)
);

CREATE TABLE IF NOT EXISTS t_organization_unit
(
    id                 INTEGER NOT NULL AUTO_INCREMENT,
    code               VARCHAR(64) NOT NULL,
    name               VARCHAR(64) NOT NULL,
    type               VARCHAR(16) NOT NULL,
    parent_id          INTEGER,
    leader_employee_id INTEGER,
    order_no           INTEGER NOT NULL DEFAULT 0,
    placeholder TINYINT NOT NULL DEFAULT 0,
    enabled            TINYINT NOT NULL DEFAULT 1,
    active_root_marker TINYINT GENERATED ALWAYS AS (
      CASE WHEN `type`='COMPANY' AND `parent_id` IS NULL
        AND `placeholder`=0 AND `enabled`=1 THEN 1 ELSE NULL END
    ),
    version            INTEGER NOT NULL DEFAULT 0,
    create_time        TIMESTAMP NOT NULL,
    create_by          INTEGER,
    edit_time          TIMESTAMP,
    edit_by            INTEGER,
    PRIMARY KEY (id),
    CONSTRAINT uk_organization_unit_code UNIQUE (code),
    CONSTRAINT uk_organization_unit_active_root UNIQUE (active_root_marker),
    CONSTRAINT fk_organization_unit_parent FOREIGN KEY (parent_id) REFERENCES t_organization_unit(id) ON DELETE RESTRICT,
    CONSTRAINT fk_organization_unit_leader FOREIGN KEY (leader_employee_id) REFERENCES t_employee(id) ON DELETE RESTRICT,
    CONSTRAINT chk_organization_unit_type CHECK (type IN ('COMPANY', 'STORE', 'DEPARTMENT', 'TEAM')),
    CONSTRAINT chk_organization_unit_placeholder CHECK (placeholder IN (0, 1)),
    CONSTRAINT chk_organization_unit_enabled CHECK (enabled IN (0, 1)),
    CONSTRAINT chk_organization_unit_hierarchy CHECK (
      (type='COMPANY' AND parent_id IS NULL)
      OR (type<>'COMPANY' AND parent_id IS NOT NULL)
    ),
    CONSTRAINT chk_organization_unit_order CHECK (order_no >= 0),
    CONSTRAINT chk_organization_unit_version CHECK (version >= 0)
);
CREATE INDEX IF NOT EXISTS idx_organization_unit_parent_order
    ON t_organization_unit(parent_id, order_no, id);
CREATE INDEX IF NOT EXISTS idx_organization_unit_leader
    ON t_organization_unit(leader_employee_id);

CREATE TABLE IF NOT EXISTS t_role_organization
(
    role_id INTEGER NOT NULL,
    organization_unit_id INTEGER NOT NULL,
    PRIMARY KEY (role_id, organization_unit_id),
    CONSTRAINT fk_role_organization_role FOREIGN KEY (role_id) REFERENCES t_role(id) ON DELETE RESTRICT,
    CONSTRAINT fk_role_organization_unit FOREIGN KEY (organization_unit_id) REFERENCES t_organization_unit(id) ON DELETE RESTRICT
);

CREATE TABLE IF NOT EXISTS t_role_permission_organization
(
    role_id INTEGER NOT NULL,
    permission_id INTEGER NOT NULL,
    organization_unit_id INTEGER NOT NULL,
    PRIMARY KEY (role_id, permission_id, organization_unit_id),
    CONSTRAINT fk_role_permission_org_permission FOREIGN KEY (role_id, permission_id)
        REFERENCES t_role_permission(role_id, permission_id) ON DELETE RESTRICT,
    CONSTRAINT fk_role_permission_org_unit FOREIGN KEY (organization_unit_id)
        REFERENCES t_organization_unit(id) ON DELETE RESTRICT
);

CREATE TABLE IF NOT EXISTS t_position
(
    id             INTEGER NOT NULL AUTO_INCREMENT,
    code           VARCHAR(64) NOT NULL,
    name           VARCHAR(64) NOT NULL,
    description    VARCHAR(255),
    position_level INTEGER NOT NULL DEFAULT 0,
    built_in       TINYINT NOT NULL DEFAULT 0,
    enabled        TINYINT NOT NULL DEFAULT 1,
    version        INTEGER NOT NULL DEFAULT 0,
    create_time    TIMESTAMP NOT NULL,
    create_by      INTEGER,
    edit_time      TIMESTAMP,
    edit_by        INTEGER,
    PRIMARY KEY (id),
    CONSTRAINT uk_position_code UNIQUE (code),
    CONSTRAINT chk_position_level CHECK (position_level >= 0),
    CONSTRAINT chk_position_built_in CHECK (built_in IN (0, 1)),
    CONSTRAINT chk_position_enabled CHECK (enabled IN (0, 1)),
    CONSTRAINT chk_position_version CHECK (version >= 0)
);
CREATE INDEX IF NOT EXISTS idx_position_level_code ON t_position(position_level, code, id);

CREATE TABLE IF NOT EXISTS t_employee_assignment
(
    id                    INTEGER NOT NULL AUTO_INCREMENT,
    employee_id           INTEGER NOT NULL,
    organization_unit_id  INTEGER NOT NULL,
    position_id           INTEGER NOT NULL,
    assignment_type       VARCHAR(16) NOT NULL,
    status                VARCHAR(16) NOT NULL,
    active_primary_marker TINYINT,
    effective_from        TIMESTAMP NOT NULL,
    effective_to          TIMESTAMP,
    reason                VARCHAR(500) NOT NULL,
    version               INTEGER NOT NULL DEFAULT 0,
    create_time           TIMESTAMP NOT NULL,
    create_by             INTEGER,
    edit_time             TIMESTAMP,
    edit_by               INTEGER,
    PRIMARY KEY (id),
    CONSTRAINT uk_employee_active_primary UNIQUE (employee_id, active_primary_marker),
    CONSTRAINT fk_employee_assignment_employee FOREIGN KEY (employee_id) REFERENCES t_employee(id) ON DELETE RESTRICT,
    CONSTRAINT fk_employee_assignment_org FOREIGN KEY (organization_unit_id) REFERENCES t_organization_unit(id) ON DELETE RESTRICT,
    CONSTRAINT fk_employee_assignment_position FOREIGN KEY (position_id) REFERENCES t_position(id) ON DELETE RESTRICT,
    CONSTRAINT chk_employee_assignment_type CHECK (assignment_type IN ('PRIMARY', 'SECONDARY', 'ACTING')),
    CONSTRAINT chk_employee_assignment_status CHECK (status IN ('PLANNED', 'ACTIVE', 'ENDED', 'CANCELLED')),
    CONSTRAINT chk_employee_assignment_period CHECK (effective_to IS NULL OR effective_to > effective_from),
    CONSTRAINT chk_employee_assignment_primary_marker CHECK (
        (assignment_type = 'PRIMARY' AND status = 'ACTIVE' AND active_primary_marker = 1)
        OR ((assignment_type <> 'PRIMARY' OR status <> 'ACTIVE') AND active_primary_marker IS NULL)
    ),
    CONSTRAINT chk_employee_assignment_version CHECK (version >= 0)
);
CREATE INDEX IF NOT EXISTS idx_employee_assignment_effective
    ON t_employee_assignment(employee_id, status, effective_from, effective_to, id);
CREATE INDEX IF NOT EXISTS idx_employee_assignment_org
    ON t_employee_assignment(organization_unit_id, status, id);
CREATE INDEX IF NOT EXISTS idx_employee_assignment_position
    ON t_employee_assignment(position_id, status, id);
CREATE INDEX IF NOT EXISTS idx_employee_assignment_workspace_org
    ON t_employee_assignment(organization_unit_id, assignment_type, status, active_primary_marker, employee_id);
CREATE INDEX IF NOT EXISTS idx_employee_assignment_workspace_position
    ON t_employee_assignment(position_id, assignment_type, status, active_primary_marker, employee_id);

CREATE TABLE IF NOT EXISTS t_employee_reporting
(
    id                      INTEGER NOT NULL AUTO_INCREMENT,
    subordinate_employee_id INTEGER NOT NULL,
    manager_employee_id     INTEGER NOT NULL,
    relation_type           VARCHAR(16) NOT NULL,
    status                  VARCHAR(16) NOT NULL,
    active_direct_marker    TINYINT,
    effective_from          TIMESTAMP NOT NULL,
    effective_to            TIMESTAMP,
    reason                  VARCHAR(500) NOT NULL,
    version                 INTEGER NOT NULL DEFAULT 0,
    create_time             TIMESTAMP NOT NULL,
    create_by               INTEGER,
    edit_time               TIMESTAMP,
    edit_by                 INTEGER,
    PRIMARY KEY (id),
    CONSTRAINT uk_employee_active_direct_manager UNIQUE (subordinate_employee_id, active_direct_marker),
    CONSTRAINT fk_employee_reporting_subordinate FOREIGN KEY (subordinate_employee_id) REFERENCES t_employee(id) ON DELETE RESTRICT,
    CONSTRAINT fk_employee_reporting_manager FOREIGN KEY (manager_employee_id) REFERENCES t_employee(id) ON DELETE RESTRICT,
    CONSTRAINT chk_employee_reporting_not_self CHECK (subordinate_employee_id <> manager_employee_id),
    CONSTRAINT chk_employee_reporting_type CHECK (relation_type IN ('DIRECT', 'ACTING')),
    CONSTRAINT chk_employee_reporting_status CHECK (status IN ('PLANNED', 'ACTIVE', 'ENDED', 'CANCELLED')),
    CONSTRAINT chk_employee_reporting_period CHECK (effective_to IS NULL OR effective_to > effective_from),
    CONSTRAINT chk_employee_reporting_acting_finite CHECK (relation_type <> 'ACTING' OR effective_to IS NOT NULL),
    CONSTRAINT chk_employee_reporting_direct_marker CHECK (
        (relation_type = 'DIRECT' AND status = 'ACTIVE' AND active_direct_marker = 1)
        OR ((relation_type <> 'DIRECT' OR status <> 'ACTIVE') AND active_direct_marker IS NULL)
    ),
    CONSTRAINT chk_employee_reporting_version CHECK (version >= 0)
);
CREATE INDEX IF NOT EXISTS idx_employee_reporting_manager
    ON t_employee_reporting(manager_employee_id, status, effective_from, effective_to, id);
CREATE INDEX IF NOT EXISTS idx_employee_reporting_workspace_manager
    ON t_employee_reporting(manager_employee_id, relation_type, status, active_direct_marker, subordinate_employee_id);

CREATE TABLE IF NOT EXISTS t_clue_owner_history
(
    id             INTEGER NOT NULL AUTO_INCREMENT,
    clue_id        INTEGER NOT NULL,
    from_owner_id  INTEGER,
    to_owner_id    INTEGER NOT NULL,
    assigned_by    INTEGER NOT NULL,
    reason         VARCHAR(500) NOT NULL,
    assigned_time  TIMESTAMP NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_clue_owner_history_clue FOREIGN KEY (clue_id) REFERENCES t_clue(id) ON DELETE RESTRICT,
    CONSTRAINT fk_clue_owner_history_from_owner FOREIGN KEY (from_owner_id) REFERENCES t_user(id) ON DELETE RESTRICT,
    CONSTRAINT fk_clue_owner_history_to_owner FOREIGN KEY (to_owner_id) REFERENCES t_user(id) ON DELETE RESTRICT,
    CONSTRAINT fk_clue_owner_history_assigned_by FOREIGN KEY (assigned_by) REFERENCES t_user(id) ON DELETE RESTRICT
);

CREATE TABLE IF NOT EXISTS t_user_role
(
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id INTEGER NOT NULL,
    role_id INTEGER NOT NULL,
    granted_by INTEGER,
    reason VARCHAR(500),
    effective_from TIMESTAMP,
    effective_to TIMESTAMP,
    active_marker BOOLEAN DEFAULT TRUE,
    version INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_user_role_active UNIQUE (user_id, role_id, active_marker),
    CONSTRAINT fk_user_role_user FOREIGN KEY (user_id) REFERENCES t_user(id) ON DELETE RESTRICT,
    CONSTRAINT fk_user_role_role FOREIGN KEY (role_id) REFERENCES t_role(id) ON DELETE RESTRICT,
    CONSTRAINT fk_user_role_granted_by FOREIGN KEY (granted_by) REFERENCES t_user(id) ON DELETE RESTRICT,
    CONSTRAINT chk_user_role_period CHECK (effective_to IS NULL OR effective_from IS NULL OR effective_to > effective_from)
);

CREATE TABLE IF NOT EXISTS t_user_permission
(
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id INTEGER NOT NULL,
    permission_id INTEGER NOT NULL,
    effect VARCHAR(16) NOT NULL,
    data_scope_code VARCHAR(32),
    effective_from TIMESTAMP NOT NULL,
    effective_to TIMESTAMP,
    active_marker BOOLEAN DEFAULT TRUE,
    reason VARCHAR(500) NOT NULL,
    granted_by INTEGER NOT NULL,
    version INTEGER NOT NULL,
    create_time TIMESTAMP NOT NULL,
    update_time TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_user_permission_current UNIQUE (user_id, permission_id),
    CONSTRAINT fk_user_permission_user FOREIGN KEY (user_id) REFERENCES t_user(id) ON DELETE RESTRICT,
    CONSTRAINT fk_user_permission_permission FOREIGN KEY (permission_id) REFERENCES t_permission(id) ON DELETE RESTRICT,
    CONSTRAINT fk_user_permission_granted_by FOREIGN KEY (granted_by) REFERENCES t_user(id) ON DELETE RESTRICT,
    CONSTRAINT chk_user_permission_effect CHECK (effect IN ('GRANT', 'DENY')),
    CONSTRAINT chk_user_permission_scope CHECK ((effect = 'GRANT' AND data_scope_code IS NOT NULL AND data_scope_code IN ('SELF', 'DIRECT_REPORTS', 'REPORTING_TREE', 'PRIMARY_ORG', 'ORG_TREE', 'CUSTOM_ORGS', 'GLOBAL')) OR (effect = 'DENY' AND data_scope_code IS NULL)),
    CONSTRAINT chk_user_permission_period CHECK (effective_to IS NULL OR effective_to > effective_from),
    CONSTRAINT chk_user_permission_version CHECK (version >= 0)
);
CREATE INDEX IF NOT EXISTS idx_user_permission_effective
    ON t_user_permission(user_id, active_marker, effective_from, effective_to, permission_id, version);

CREATE TABLE IF NOT EXISTS t_user_permission_organization
(
    user_permission_id BIGINT NOT NULL,
    organization_unit_id INTEGER NOT NULL,
    PRIMARY KEY (user_permission_id, organization_unit_id),
    CONSTRAINT fk_user_permission_org_permission FOREIGN KEY (user_permission_id)
        REFERENCES t_user_permission(id) ON DELETE RESTRICT,
    CONSTRAINT fk_user_permission_org_unit FOREIGN KEY (organization_unit_id)
        REFERENCES t_organization_unit(id) ON DELETE RESTRICT
);

CREATE TABLE IF NOT EXISTS t_authorization_history
(
    id BIGINT NOT NULL AUTO_INCREMENT,
    subject_type VARCHAR(32) NOT NULL,
    subject_id VARCHAR(64) NOT NULL,
    change_type VARCHAR(16) NOT NULL,
    target_user_id INTEGER,
    role_id INTEGER,
    permission_id INTEGER,
    effect VARCHAR(16),
    data_scope_code VARCHAR(32),
    effective_from TIMESTAMP,
    effective_to TIMESTAMP,
    before_value VARCHAR(2048),
    after_value VARCHAR(2048),
    reason VARCHAR(500) NOT NULL,
    operator_id INTEGER NOT NULL,
    occurred_time TIMESTAMP NOT NULL,
    request_id VARCHAR(64),
    affected_user_ids CLOB,
    affected_users_snapshot CLOB,
    PRIMARY KEY (id),
    CONSTRAINT fk_authorization_history_target_user FOREIGN KEY (target_user_id) REFERENCES t_user(id) ON DELETE RESTRICT,
    CONSTRAINT fk_authorization_history_role FOREIGN KEY (role_id) REFERENCES t_role(id) ON DELETE RESTRICT,
    CONSTRAINT fk_authorization_history_permission FOREIGN KEY (permission_id) REFERENCES t_permission(id) ON DELETE RESTRICT,
    CONSTRAINT fk_authorization_history_operator FOREIGN KEY (operator_id) REFERENCES t_user(id) ON DELETE RESTRICT,
    CONSTRAINT chk_authorization_history_subject CHECK (subject_type IN ('ROLE', 'ROLE_PERMISSION', 'USER_ROLE', 'USER_PERMISSION', 'ORGANIZATION_UNIT', 'POSITION', 'ORGANIZATION_ASSIGNMENT', 'REPORTING_RELATION')),
    CONSTRAINT chk_authorization_history_change CHECK (change_type IN ('CREATE', 'UPDATE', 'ENABLE', 'DISABLE', 'ASSIGN', 'UNASSIGN', 'GRANT', 'DENY', 'REVOKE', 'EXPIRE')),
    CONSTRAINT chk_authorization_history_effect CHECK (effect IS NULL OR effect IN ('GRANT', 'DENY')),
    CONSTRAINT chk_authorization_history_subject_ids CHECK (
      (subject_type <> 'ROLE' OR role_id IS NOT NULL) AND
      (subject_type <> 'ROLE_PERMISSION' OR (role_id IS NOT NULL AND permission_id IS NOT NULL)) AND
      (subject_type <> 'USER_ROLE' OR (target_user_id IS NOT NULL AND role_id IS NOT NULL)) AND
      (subject_type <> 'USER_PERMISSION' OR (target_user_id IS NOT NULL AND permission_id IS NOT NULL AND effect IS NOT NULL))
    ),
    CONSTRAINT chk_authorization_history_user_permission_scope CHECK (
      subject_type <> 'USER_PERMISSION' OR
      (effect = 'GRANT' AND data_scope_code IS NOT NULL) OR
      (effect = 'DENY' AND data_scope_code IS NULL)
    ),
    CONSTRAINT chk_authorization_history_period CHECK (effective_to IS NULL OR effective_from IS NULL OR effective_to > effective_from)
);
CREATE INDEX IF NOT EXISTS idx_authorization_history_subject
    ON t_authorization_history(subject_type, subject_id, occurred_time, id);
CREATE INDEX IF NOT EXISTS idx_authorization_history_target
    ON t_authorization_history(target_user_id, occurred_time, id);

CREATE TABLE IF NOT EXISTS t_authorization_graph_lock
(
    lock_name VARCHAR(64) NOT NULL,
    PRIMARY KEY(lock_name)
);
MERGE INTO t_authorization_graph_lock(lock_name) KEY(lock_name)
VALUES ('LOGIN_IDENTIFIER_GUARD');

CREATE TABLE IF NOT EXISTS t_user_lifecycle_event
(
    id BIGINT NOT NULL AUTO_INCREMENT,
    operation_id VARCHAR(64) NOT NULL,
    request_id VARCHAR(64) NOT NULL,
    action VARCHAR(32) NOT NULL,
    user_id INTEGER NOT NULL,
    employee_id INTEGER NOT NULL,
    before_value CLOB NOT NULL,
    after_value CLOB NOT NULL,
    reason VARCHAR(500) NOT NULL,
    operator_id INTEGER NOT NULL,
    occurred_time TIMESTAMP NOT NULL,
    PRIMARY KEY(id),
    CONSTRAINT uk_user_lifecycle_operation UNIQUE(operation_id),
    CONSTRAINT fk_user_lifecycle_user FOREIGN KEY(user_id) REFERENCES t_user(id) ON DELETE RESTRICT,
    CONSTRAINT fk_user_lifecycle_employee FOREIGN KEY(employee_id) REFERENCES t_employee(id) ON DELETE RESTRICT,
    CONSTRAINT fk_user_lifecycle_operator FOREIGN KEY(operator_id) REFERENCES t_user(id) ON DELETE RESTRICT,
    CONSTRAINT chk_user_lifecycle_action CHECK(action IN ('TRANSFER','DEPARTURE_START','HANDOVER_CONFIRM','DEPARTURE_COMPLETE','REHIRE'))
);
CREATE INDEX IF NOT EXISTS idx_user_lifecycle_target_time ON t_user_lifecycle_event(user_id,occurred_time,id);

CREATE TABLE IF NOT EXISTS t_user_lifecycle_snapshot
(
    id BIGINT NOT NULL AUTO_INCREMENT,
    token_digest VARCHAR(64) NOT NULL,
    user_id INTEGER NOT NULL,
    employee_id INTEGER NOT NULL,
    employee_version INTEGER NOT NULL,
    reason_digest VARCHAR(64) NOT NULL,
    fact_digest VARCHAR(64) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    consumed_at TIMESTAMP NULL,
    version INTEGER NOT NULL DEFAULT 0,
    create_time TIMESTAMP NOT NULL,
    PRIMARY KEY(id),
    CONSTRAINT uk_user_lifecycle_snapshot_token UNIQUE(token_digest),
    CONSTRAINT fk_user_lifecycle_snapshot_user FOREIGN KEY(user_id) REFERENCES t_user(id) ON DELETE RESTRICT,
    CONSTRAINT fk_user_lifecycle_snapshot_employee FOREIGN KEY(employee_id) REFERENCES t_employee(id) ON DELETE RESTRICT
);
CREATE INDEX IF NOT EXISTS idx_user_lifecycle_snapshot_expiry ON t_user_lifecycle_snapshot(expires_at,consumed_at,id);

CREATE TABLE IF NOT EXISTS t_tran
(
    id                INTEGER NOT NULL AUTO_INCREMENT,
    tran_no           VARCHAR(255),
    customer_id       INTEGER,
    money             DECIMAL(10, 2),
    expected_date     TIMESTAMP,
    stage             VARCHAR(32),
    description       VARCHAR(255),
    next_contact_time TIMESTAMP,
    create_time       TIMESTAMP,
    create_by         INTEGER,
    edit_time         TIMESTAMP,
    edit_by           INTEGER,
    version           INTEGER NOT NULL DEFAULT 0,
    promotion_id      BIGINT,
    original_amount   DECIMAL(10, 2),
    discount_amount   DECIMAL(10, 2) NOT NULL DEFAULT 0,
    promotion_snapshot TEXT,
    PRIMARY KEY (id),
    CONSTRAINT uk_tran_no UNIQUE (tran_no),
    CONSTRAINT fk_tran_customer FOREIGN KEY (customer_id) REFERENCES t_customer(id) ON DELETE RESTRICT
);

CREATE TABLE IF NOT EXISTS t_tran_history
(
    id            INTEGER NOT NULL AUTO_INCREMENT,
    tran_id       INTEGER,
    stage         VARCHAR(32),
    reason        VARCHAR(500),
    money         DECIMAL(10, 2),
    expected_date TIMESTAMP,
    create_time   TIMESTAMP,
    create_by     INTEGER,
    PRIMARY KEY (id),
    CONSTRAINT fk_tran_history_tran FOREIGN KEY (tran_id) REFERENCES t_tran(id) ON DELETE RESTRICT
);

CREATE TABLE IF NOT EXISTS t_quote
(
    id                 BIGINT NOT NULL AUTO_INCREMENT,
    quote_no           VARCHAR(64) NOT NULL,
    customer_id        INTEGER NOT NULL,
    opportunity_id     BIGINT,
    current_version_id BIGINT,
    status             VARCHAR(50) NOT NULL,
    remark             VARCHAR(500),
    create_time        TIMESTAMP,
    create_by          INTEGER,
    update_time        TIMESTAMP,
    update_by          INTEGER,
    PRIMARY KEY (id),
    CONSTRAINT uk_quote_no UNIQUE (quote_no),
    CONSTRAINT fk_quote_customer FOREIGN KEY (customer_id) REFERENCES t_customer(id) ON DELETE RESTRICT,
    CONSTRAINT chk_quote_status CHECK (status IN (
        'DRAFT', 'PENDING_SUBMIT', 'PENDING_APPROVAL', 'REJECTED',
        'PENDING_CUSTOMER_CONFIRMATION', 'ACCEPTED', 'REFUSED',
        'EXPIRED', 'VOIDED', 'CONVERTED_TO_ORDER'
    ))
);

CREATE TABLE IF NOT EXISTS t_quote_version
(
    id           BIGINT NOT NULL AUTO_INCREMENT,
    quote_id     BIGINT NOT NULL,
    version_no   INTEGER NOT NULL,
    valid_until  TIMESTAMP NOT NULL,
    total_amount DECIMAL(10, 2) NOT NULL,
    remark       VARCHAR(500),
    create_time  TIMESTAMP,
    create_by    INTEGER,
    PRIMARY KEY (id),
    CONSTRAINT uk_quote_version_no UNIQUE (quote_id, version_no),
    CONSTRAINT fk_quote_version_quote FOREIGN KEY (quote_id) REFERENCES t_quote(id) ON DELETE RESTRICT
);

ALTER TABLE t_quote
    ADD CONSTRAINT fk_quote_current_version
        FOREIGN KEY (current_version_id) REFERENCES t_quote_version(id) ON DELETE RESTRICT;

CREATE TABLE IF NOT EXISTS t_quote_status_history
(
    id          BIGINT NOT NULL AUTO_INCREMENT,
    quote_id    BIGINT NOT NULL,
    from_status VARCHAR(50),
    to_status   VARCHAR(50) NOT NULL,
    reason      VARCHAR(500) NOT NULL,
    confirmed_by_name VARCHAR(100),
    confirmed_at TIMESTAMP,
    confirmation_method VARCHAR(50),
    confirmation_evidence VARCHAR(500),
    proxy_confirm_reason VARCHAR(500),
    create_time TIMESTAMP,
    create_by   INTEGER,
    PRIMARY KEY (id),
    CONSTRAINT fk_quote_status_history_quote FOREIGN KEY (quote_id) REFERENCES t_quote(id) ON DELETE RESTRICT
);

CREATE TABLE IF NOT EXISTS t_product
(
    id            BIGINT NOT NULL AUTO_INCREMENT,
    sku           VARCHAR(255) NOT NULL,
    name          VARCHAR(255) NOT NULL,
    category_id   BIGINT,
    specification VARCHAR(255),
    price         DECIMAL(10, 2) NOT NULL DEFAULT 0,
    stock         INTEGER NOT NULL DEFAULT 0,
    min_stock     INTEGER,
    status        VARCHAR(50) NOT NULL DEFAULT 'OFF_SALE',
    create_time   TIMESTAMP,
    update_time   TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_sku UNIQUE (sku),
    CONSTRAINT chk_product_price_nonneg CHECK (price >= 0),
    CONSTRAINT chk_product_stock_nonneg CHECK (stock >= 0),
    CONSTRAINT chk_product_status_code CHECK (status IN ('ON_SALE', 'OFF_SALE'))
);

CREATE TABLE IF NOT EXISTS t_opportunity
(
    id                  BIGINT NOT NULL AUTO_INCREMENT,
    opportunity_no      VARCHAR(64) NOT NULL,
    customer_id         INTEGER NOT NULL,
    clue_id             INTEGER,
    owner_id            INTEGER NOT NULL,
    product_id          BIGINT,
    source_type         VARCHAR(64),
    stage               VARCHAR(50) NOT NULL,
    requirement         VARCHAR(1000) NOT NULL,
    expected_amount     DECIMAL(10, 2),
    expected_close_date DATE,
    next_action_time    DATE,
    last_follow_time    TIMESTAMP,
    last_follow_summary VARCHAR(255),
    lost_reason         VARCHAR(500),
    lost_competitor     VARCHAR(255),
    result_remark       VARCHAR(500),
    order_tran_id       INTEGER,
    version             INTEGER NOT NULL DEFAULT 0,
    create_time         TIMESTAMP,
    create_by           INTEGER,
    update_time         TIMESTAMP,
    update_by           INTEGER,
    PRIMARY KEY (id),
    CONSTRAINT uk_opportunity_no UNIQUE (opportunity_no),
    CONSTRAINT fk_opportunity_customer FOREIGN KEY (customer_id) REFERENCES t_customer(id) ON DELETE RESTRICT,
    CONSTRAINT fk_opportunity_clue FOREIGN KEY (clue_id) REFERENCES t_clue(id) ON DELETE RESTRICT,
    CONSTRAINT fk_opportunity_owner FOREIGN KEY (owner_id) REFERENCES t_user(id) ON DELETE RESTRICT,
    CONSTRAINT fk_opportunity_product FOREIGN KEY (product_id) REFERENCES t_product(id) ON DELETE RESTRICT,
    CONSTRAINT fk_opportunity_order_tran FOREIGN KEY (order_tran_id) REFERENCES t_tran(id) ON DELETE RESTRICT,
    CONSTRAINT chk_opportunity_stage CHECK (stage IN (
        'INITIAL_CONTACT', 'NEEDS_ANALYSIS', 'VEHICLE_MATCHING', 'TEST_DRIVE_INVITED',
        'QUOTING', 'NEGOTIATION', 'PENDING_APPROVAL', 'WON', 'LOST', 'SHELVED', 'CLOSED'
    )),
    CONSTRAINT chk_opportunity_expected_amount CHECK (expected_amount IS NULL OR expected_amount >= 0)
);

CREATE INDEX IF NOT EXISTS idx_opportunity_customer_stage ON t_opportunity(customer_id, stage);
CREATE INDEX IF NOT EXISTS idx_opportunity_owner_stage ON t_opportunity(owner_id, stage);
CREATE INDEX IF NOT EXISTS idx_opportunity_product ON t_opportunity(product_id);
CREATE INDEX IF NOT EXISTS idx_opportunity_order_tran ON t_opportunity(order_tran_id);

CREATE TABLE IF NOT EXISTS t_opportunity_stage_history
(
    id             BIGINT NOT NULL AUTO_INCREMENT,
    opportunity_id BIGINT NOT NULL,
    from_stage     VARCHAR(50),
    to_stage       VARCHAR(50) NOT NULL,
    reason         VARCHAR(500) NOT NULL,
    operate_by     INTEGER NOT NULL,
    operate_time   TIMESTAMP NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_opportunity_history_opportunity FOREIGN KEY (opportunity_id) REFERENCES t_opportunity(id) ON DELETE RESTRICT,
    CONSTRAINT fk_opportunity_history_operator FOREIGN KEY (operate_by) REFERENCES t_user(id) ON DELETE RESTRICT,
    CONSTRAINT chk_opportunity_history_to_stage CHECK (to_stage IN (
        'INITIAL_CONTACT', 'NEEDS_ANALYSIS', 'VEHICLE_MATCHING', 'TEST_DRIVE_INVITED',
        'QUOTING', 'NEGOTIATION', 'PENDING_APPROVAL', 'WON', 'LOST', 'SHELVED', 'CLOSED'
    )),
    CONSTRAINT chk_opportunity_history_from_stage CHECK (from_stage IS NULL OR from_stage IN (
        'INITIAL_CONTACT', 'NEEDS_ANALYSIS', 'VEHICLE_MATCHING', 'TEST_DRIVE_INVITED',
        'QUOTING', 'NEGOTIATION', 'PENDING_APPROVAL', 'WON', 'LOST', 'SHELVED', 'CLOSED'
    ))
);

CREATE INDEX IF NOT EXISTS idx_opportunity_history_opportunity ON t_opportunity_stage_history(opportunity_id, operate_time);

ALTER TABLE t_quote
    ADD CONSTRAINT fk_quote_opportunity FOREIGN KEY (opportunity_id) REFERENCES t_opportunity(id) ON DELETE RESTRICT;

CREATE TABLE IF NOT EXISTS t_quote_version_item
(
    id                    BIGINT NOT NULL AUTO_INCREMENT,
    quote_version_id      BIGINT NOT NULL,
    product_id            BIGINT NOT NULL,
    product_sku           VARCHAR(100),
    product_name          VARCHAR(255),
    product_specification VARCHAR(255),
    guide_price           DECIMAL(10, 2),
    unit_price            DECIMAL(10, 2) NOT NULL,
    quantity              INTEGER NOT NULL,
    line_amount           DECIMAL(10, 2) NOT NULL,
    promotion_id          BIGINT,
    promotion_code        VARCHAR(64),
    promotion_name        VARCHAR(255),
    promotion_rule_summary VARCHAR(500),
    promotion_amount      DECIMAL(10, 2),
    promotion_snapshot    TEXT,
    create_time           TIMESTAMP,
    create_by             INTEGER,
    PRIMARY KEY (id),
    CONSTRAINT fk_quote_item_version FOREIGN KEY (quote_version_id) REFERENCES t_quote_version(id) ON DELETE RESTRICT,
    CONSTRAINT fk_quote_item_product FOREIGN KEY (product_id) REFERENCES t_product(id) ON DELETE RESTRICT
);

CREATE TABLE IF NOT EXISTS t_tran_product
(
    id          INTEGER NOT NULL AUTO_INCREMENT,
    tran_id     INTEGER NOT NULL,
    product_id  BIGINT NOT NULL,
    quantity    INTEGER NOT NULL,
    price       DECIMAL(10, 2) NOT NULL,
    product_sku VARCHAR(100),
    product_name VARCHAR(255),
    product_specification VARCHAR(255),
    guide_price DECIMAL(10, 2),
    create_time TIMESTAMP,
    create_by   INTEGER,
    PRIMARY KEY (id),
    CONSTRAINT fk_tran_product_tran FOREIGN KEY (tran_id) REFERENCES t_tran(id) ON DELETE RESTRICT,
    CONSTRAINT fk_tran_product_product FOREIGN KEY (product_id) REFERENCES t_product(id) ON DELETE RESTRICT
);

CREATE TABLE IF NOT EXISTS t_tran_invoice
(
    id           INTEGER NOT NULL AUTO_INCREMENT,
    tran_id      INTEGER NOT NULL,
    invoice_no   VARCHAR(32) NOT NULL,
    type         VARCHAR(20) NOT NULL,
    title        VARCHAR(128) NOT NULL,
    tax_number   VARCHAR(32) NOT NULL,
    bank_name    VARCHAR(128),
    bank_account VARCHAR(32),
    address      VARCHAR(255),
    phone        VARCHAR(20),
    original_invoice_id INTEGER,
    amount       DECIMAL(10, 2) NOT NULL,
    status       VARCHAR(20) NOT NULL,
    remark       VARCHAR(255),
    issue_time   TIMESTAMP,
    create_time  TIMESTAMP,
    create_by    INTEGER,
    edit_time    TIMESTAMP,
    edit_by      INTEGER,
    PRIMARY KEY (id),
    CONSTRAINT uk_invoice_no UNIQUE (invoice_no),
    CONSTRAINT fk_tran_invoice_tran FOREIGN KEY (tran_id) REFERENCES t_tran(id) ON DELETE RESTRICT,
    CONSTRAINT fk_tran_invoice_original FOREIGN KEY (original_invoice_id) REFERENCES t_tran_invoice(id) ON DELETE RESTRICT,
    CONSTRAINT chk_tran_invoice_status CHECK (status IN ('PENDING', 'ISSUING', 'ISSUED', 'FAILED', 'VOIDED', 'PARTIAL_RED_REVERSED', 'RED_REVERSED', 'NOT_REQUIRED'))
);

CREATE TABLE IF NOT EXISTS t_tran_approve
(
    id              INTEGER NOT NULL AUTO_INCREMENT,
    tran_id         INTEGER NOT NULL,
    approve_result  TINYINT NOT NULL,
    approve_comment VARCHAR(500),
    approve_time    TIMESTAMP,
    approve_by      INTEGER,
    create_time     TIMESTAMP,
    create_by       INTEGER,
    PRIMARY KEY (id),
    CONSTRAINT uk_tran_id UNIQUE (tran_id),
    CONSTRAINT fk_tran_approve_tran FOREIGN KEY (tran_id) REFERENCES t_tran(id) ON DELETE RESTRICT
);

CREATE TABLE IF NOT EXISTS t_payment
(
    id              INTEGER NOT NULL AUTO_INCREMENT,
    tran_id         INTEGER NOT NULL,
    payment_no      VARCHAR(64) NOT NULL,
    amount          DECIMAL(10,2) NOT NULL,
    payment_method  VARCHAR(32) NOT NULL,
    payment_type    VARCHAR(32) NOT NULL,
    payment_status  VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    payment_time    TIMESTAMP,
    transaction_ref VARCHAR(128),
    idempotency_key VARCHAR(160),
    remark          VARCHAR(255),
    create_time     TIMESTAMP,
    create_by       INTEGER,
    edit_time       TIMESTAMP,
    edit_by         INTEGER,
    PRIMARY KEY (id),
    CONSTRAINT uk_payment_no UNIQUE (payment_no),
    CONSTRAINT uk_payment_transaction_ref UNIQUE (transaction_ref),
    CONSTRAINT uk_payment_idempotency_key UNIQUE (idempotency_key),
    CONSTRAINT fk_payment_tran FOREIGN KEY (tran_id) REFERENCES t_tran(id) ON DELETE RESTRICT,
    CONSTRAINT chk_payment_method CHECK (payment_method IN ('CASH', 'BANK_TRANSFER', 'WECHAT', 'ALIPAY', 'CHECK', 'OTHER')),
    CONSTRAINT chk_payment_type CHECK (payment_type IN ('DEPOSIT', 'INSTALLMENT', 'FULL', 'BALANCE', 'REFUND')),
    CONSTRAINT chk_payment_status CHECK (payment_status IN ('PENDING', 'COMPLETED', 'FAILED', 'REVERSED', 'VOIDED'))
);

CREATE TABLE IF NOT EXISTS t_refund_request
(
    id                  INTEGER NOT NULL AUTO_INCREMENT,
    tran_id             INTEGER NOT NULL,
    original_payment_id INTEGER NOT NULL,
    refund_payment_id   INTEGER,
    amount              DECIMAL(10,2) NOT NULL,
    refund_type         VARCHAR(32) NOT NULL,
    reason              VARCHAR(500) NOT NULL,
    status              VARCHAR(32) NOT NULL,
    requested_by        INTEGER,
    requested_time      TIMESTAMP,
    approved_by         INTEGER,
    approved_time       TIMESTAMP,
    approve_comment     VARCHAR(500),
    executed_by         INTEGER,
    execution_started_time TIMESTAMP,
    executed_time       TIMESTAMP,
    execution_ref       VARCHAR(128),
    execution_remark    VARCHAR(500),
    failure_reason      VARCHAR(500),
    create_time         TIMESTAMP,
    create_by           INTEGER,
    edit_time           TIMESTAMP,
    edit_by             INTEGER,
    PRIMARY KEY (id),
    CONSTRAINT fk_refund_request_tran FOREIGN KEY (tran_id) REFERENCES t_tran(id) ON DELETE RESTRICT,
    CONSTRAINT fk_refund_request_original_payment FOREIGN KEY (original_payment_id) REFERENCES t_payment(id) ON DELETE RESTRICT,
    CONSTRAINT fk_refund_request_refund_payment FOREIGN KEY (refund_payment_id) REFERENCES t_payment(id) ON DELETE RESTRICT,
    CONSTRAINT chk_refund_request_type CHECK (refund_type IN ('ORDER_CANCEL', 'OVERPAY', 'PRICE_ADJUSTMENT', 'CUSTOMER_BREACH', 'INTERNAL_CORRECTION')),
    CONSTRAINT chk_refund_request_status CHECK (status IN ('PENDING_APPROVAL', 'PENDING_EXECUTION', 'EXECUTING', 'COMPLETED', 'REJECTED', 'FAILED', 'CANCELLED'))
);

CREATE TABLE IF NOT EXISTS t_operation_log
(
    id          INTEGER NOT NULL AUTO_INCREMENT,
    user_id     INTEGER,
    user_name   VARCHAR(64),
    action_code VARCHAR(64) NOT NULL,
    object_type VARCHAR(64),
    module_name VARCHAR(64),
    resource_id VARCHAR(64),
    result      VARCHAR(32),
    detail      VARCHAR(2048),
    ip          VARCHAR(64),
    request_id  VARCHAR(64),
    create_time TIMESTAMP,
    PRIMARY KEY (id)
);
CREATE INDEX IF NOT EXISTS idx_operation_log_time ON t_operation_log(create_time, id);
CREATE INDEX IF NOT EXISTS idx_operation_log_query ON t_operation_log(module_name, action_code, user_id, result);
CREATE INDEX IF NOT EXISTS idx_operation_log_user_history ON t_operation_log(resource_id, action_code, create_time, id);

CREATE TABLE IF NOT EXISTS t_login_log
(
    id             INTEGER NOT NULL AUTO_INCREMENT,
    login_act      VARCHAR(64) NOT NULL,
    user_id        INTEGER,
    user_name      VARCHAR(64),
    result         VARCHAR(32) NOT NULL,
    reason_code    VARCHAR(64) NOT NULL,
    reason_message VARCHAR(255),
    ip             VARCHAR(64),
    browser        VARCHAR(128),
    os             VARCHAR(128),
    request_id     VARCHAR(64),
    create_time    TIMESTAMP NOT NULL,
    PRIMARY KEY (id)
);
CREATE INDEX IF NOT EXISTS idx_login_log_time ON t_login_log(create_time, id);
CREATE INDEX IF NOT EXISTS idx_login_log_query ON t_login_log(login_act, user_id, result, reason_code);

CREATE TABLE IF NOT EXISTS t_product_category
(
    id          BIGINT NOT NULL AUTO_INCREMENT,
    name        VARCHAR(255) NOT NULL,
    code        VARCHAR(100),
    description TEXT,
    sort        INTEGER DEFAULT 0,
    status      VARCHAR(50),
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_category_code UNIQUE (code)
);

ALTER TABLE t_product
    ADD CONSTRAINT fk_product_category FOREIGN KEY (category_id) REFERENCES t_product_category(id) ON DELETE RESTRICT;
ALTER TABLE t_customer
    ADD CONSTRAINT fk_customer_product FOREIGN KEY (product) REFERENCES t_product(id) ON DELETE RESTRICT;
ALTER TABLE t_customer
    ADD CONSTRAINT fk_customer_owner FOREIGN KEY (owner_id) REFERENCES t_user(id) ON DELETE RESTRICT;
ALTER TABLE t_customer
    ADD CONSTRAINT fk_customer_merged_to FOREIGN KEY (merged_to_customer_id) REFERENCES t_customer(id) ON DELETE RESTRICT;
ALTER TABLE t_customer_owner_history
    ADD CONSTRAINT fk_customer_owner_history_from_user FOREIGN KEY (from_owner_id) REFERENCES t_user(id) ON DELETE RESTRICT;
ALTER TABLE t_customer_owner_history
    ADD CONSTRAINT fk_customer_owner_history_to_user FOREIGN KEY (to_owner_id) REFERENCES t_user(id) ON DELETE RESTRICT;
ALTER TABLE t_customer_owner_history
    ADD CONSTRAINT fk_customer_owner_history_operator FOREIGN KEY (operator_id) REFERENCES t_user(id) ON DELETE RESTRICT;

CREATE TABLE IF NOT EXISTS t_product_promotion
(
    id                 BIGINT NOT NULL AUTO_INCREMENT,
    product_id         BIGINT NOT NULL,
    code               VARCHAR(64) NOT NULL,
    name               VARCHAR(255) NOT NULL,
    type               VARCHAR(50) NOT NULL,
    discount           DECIMAL(10, 2) NOT NULL DEFAULT 0,
    rule_summary       VARCHAR(500) NOT NULL,
    applicable_store   VARCHAR(64) NOT NULL DEFAULT 'ALL',
    customer_type      VARCHAR(64) NOT NULL DEFAULT 'ALL',
    applicable_channel VARCHAR(64) NOT NULL DEFAULT 'ALL',
    inventory_scope    VARCHAR(64) NOT NULL DEFAULT 'ALL',
    stackable          BOOLEAN NOT NULL DEFAULT FALSE,
    priority           INTEGER NOT NULL DEFAULT 0,
    budget_limit       DECIMAL(10, 2),
    used_budget        DECIMAL(10, 2) NOT NULL DEFAULT 0,
    usage_limit        INTEGER,
    used_count         INTEGER NOT NULL DEFAULT 0,
    start_time         TIMESTAMP NOT NULL,
    end_time           TIMESTAMP NOT NULL,
    status             VARCHAR(50) NOT NULL,
    pause_reason       VARCHAR(500),
    end_reason         VARCHAR(500),
    void_reason        VARCHAR(500),
    create_time        TIMESTAMP,
    update_time        TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_product_promotion_code UNIQUE (code),
    CONSTRAINT fk_product_promotion_product FOREIGN KEY (product_id) REFERENCES t_product(id) ON DELETE RESTRICT,
    CONSTRAINT chk_product_promotion_status CHECK (status IN ('DRAFT', 'PENDING_EFFECTIVE', 'ACTIVE', 'PAUSED', 'ENDED', 'VOIDED', 'EXHAUSTED')),
    CONSTRAINT chk_product_promotion_type CHECK (type IN ('AMOUNT', 'PERCENTAGE', 'EXCHANGE_SUBSIDY', 'FINANCE_SUBSIDY', 'GIFT', 'MAINTENANCE', 'INSURANCE_SUBSIDY', 'LIMITED_TIME', 'INVENTORY_CLEARANCE')),
    CONSTRAINT chk_product_promotion_time CHECK (end_time > start_time),
    CONSTRAINT chk_product_promotion_discount CHECK (
        (type = 'PERCENTAGE' AND discount > 0 AND discount < 1)
        OR (type IN ('AMOUNT', 'EXCHANGE_SUBSIDY', 'FINANCE_SUBSIDY', 'INSURANCE_SUBSIDY', 'LIMITED_TIME', 'INVENTORY_CLEARANCE') AND discount > 0)
        OR (type IN ('GIFT', 'MAINTENANCE') AND discount >= 0)
    ),
    CONSTRAINT chk_product_promotion_budget CHECK (budget_limit IS NULL OR (budget_limit > 0 AND used_budget <= budget_limit)),
    CONSTRAINT chk_product_promotion_usage CHECK (usage_limit IS NULL OR (usage_limit > 0 AND used_count <= usage_limit)),
    CONSTRAINT chk_product_promotion_used_non_negative CHECK (used_budget >= 0 AND used_count >= 0)
);

CREATE TABLE IF NOT EXISTS t_product_promotion_usage
(
    id              BIGINT NOT NULL AUTO_INCREMENT,
    promotion_id    BIGINT NOT NULL,
    source_type     VARCHAR(50) NOT NULL,
    source_id       BIGINT NOT NULL,
    discount_amount DECIMAL(10, 2) NOT NULL DEFAULT 0,
    create_time     TIMESTAMP NOT NULL,
    create_by       INTEGER,
    PRIMARY KEY (id),
    CONSTRAINT uk_product_promotion_usage_source UNIQUE (promotion_id, source_type, source_id),
    CONSTRAINT fk_product_promotion_usage_promotion FOREIGN KEY (promotion_id) REFERENCES t_product_promotion(id) ON DELETE RESTRICT,
    CONSTRAINT chk_product_promotion_usage_amount CHECK (discount_amount >= 0)
);

CREATE TABLE IF NOT EXISTS t_product_vehicle
(
    id            BIGINT NOT NULL AUTO_INCREMENT,
    product_id    BIGINT NOT NULL,
    vin           VARCHAR(64) NOT NULL,
    color         VARCHAR(64) NOT NULL,
    configuration VARCHAR(255),
    location      VARCHAR(128) NOT NULL,
    status        VARCHAR(50) NOT NULL,
    hold_type     VARCHAR(50),
    source_type   VARCHAR(50),
    source_id     BIGINT,
    hold_until    TIMESTAMP,
    create_time   TIMESTAMP,
    create_by     INTEGER,
    update_time   TIMESTAMP,
    update_by     INTEGER,
    PRIMARY KEY (id),
    CONSTRAINT uk_product_vehicle_vin UNIQUE (vin),
    CONSTRAINT fk_product_vehicle_product FOREIGN KEY (product_id) REFERENCES t_product(id) ON DELETE RESTRICT,
    CONSTRAINT chk_product_vehicle_status CHECK (status IN (
        'PENDING_INBOUND', 'AVAILABLE', 'TEST_DRIVE_RESERVED', 'SALES_LOCKED',
        'ORDER_RESERVED', 'PENDING_DELIVERY', 'OUTBOUND', 'DELIVERED',
        'INVENTORY_EXCEPTION', 'UNAVAILABLE'
    ))
);

CREATE TABLE IF NOT EXISTS t_test_drive
(
    id                      BIGINT NOT NULL AUTO_INCREMENT,
    test_drive_no           VARCHAR(64) NOT NULL,
    customer_id             INTEGER NOT NULL,
    opportunity_id          BIGINT,
    vehicle_id              BIGINT NOT NULL,
    owner_id                INTEGER NOT NULL,
    planned_start_time      TIMESTAMP NOT NULL,
    planned_end_time        TIMESTAMP NOT NULL,
    actual_arrive_time      TIMESTAMP,
    actual_start_time       TIMESTAMP,
    actual_end_time         TIMESTAMP,
    safety_confirmed_at     TIMESTAMP,
    safety_confirmed_by     INTEGER,
    check_in_by             INTEGER,
    customer_confirm_method VARCHAR(50),
    status                  VARCHAR(50) NOT NULL,
    contact_name            VARCHAR(100) NOT NULL,
    contact_phone           VARCHAR(50) NOT NULL,
    result                  VARCHAR(100),
    customer_feedback       VARCHAR(1000),
    next_action             VARCHAR(500),
    cancel_type             VARCHAR(50),
    cancel_reason           VARCHAR(500),
    remark                  VARCHAR(500),
    reschedule_count        INTEGER NOT NULL DEFAULT 0,
    version                 INTEGER NOT NULL DEFAULT 0,
    create_time             TIMESTAMP,
    create_by               INTEGER,
    update_time             TIMESTAMP,
    update_by               INTEGER,
    PRIMARY KEY (id),
    CONSTRAINT uk_test_drive_no UNIQUE (test_drive_no),
    CONSTRAINT fk_test_drive_customer FOREIGN KEY (customer_id) REFERENCES t_customer(id) ON DELETE RESTRICT,
    CONSTRAINT fk_test_drive_opportunity FOREIGN KEY (opportunity_id) REFERENCES t_opportunity(id) ON DELETE RESTRICT,
    CONSTRAINT fk_test_drive_vehicle FOREIGN KEY (vehicle_id) REFERENCES t_product_vehicle(id) ON DELETE RESTRICT,
    CONSTRAINT fk_test_drive_owner FOREIGN KEY (owner_id) REFERENCES t_user(id) ON DELETE RESTRICT,
    CONSTRAINT chk_test_drive_status CHECK (status IN (
        'PENDING_CONFIRM', 'SCHEDULED', 'RESCHEDULED', 'CHECKED_IN',
        'COMPLETED', 'CANCELED', 'NO_SHOW', 'EXCEPTION_CLOSED'
    )),
    CONSTRAINT chk_test_drive_time_range CHECK (planned_start_time < planned_end_time),
    CONSTRAINT chk_test_drive_reschedule_count CHECK (reschedule_count >= 0)
);

CREATE INDEX IF NOT EXISTS idx_test_drive_customer_status ON t_test_drive(customer_id, status);
CREATE INDEX IF NOT EXISTS idx_test_drive_opportunity ON t_test_drive(opportunity_id);
CREATE INDEX IF NOT EXISTS idx_test_drive_vehicle_time ON t_test_drive(vehicle_id, planned_start_time, planned_end_time);
CREATE INDEX IF NOT EXISTS idx_test_drive_owner_time ON t_test_drive(owner_id, planned_start_time, planned_end_time);

CREATE TABLE IF NOT EXISTS t_test_drive_vehicle_hold
(
    id             BIGINT NOT NULL AUTO_INCREMENT,
    test_drive_id  BIGINT NOT NULL,
    vehicle_id     BIGINT NOT NULL,
    start_time     TIMESTAMP NOT NULL,
    end_time       TIMESTAMP NOT NULL,
    status         VARCHAR(30) NOT NULL,
    release_reason VARCHAR(500),
    release_time   TIMESTAMP,
    create_time    TIMESTAMP,
    create_by      INTEGER,
    update_time    TIMESTAMP,
    update_by      INTEGER,
    PRIMARY KEY (id),
    CONSTRAINT fk_test_drive_hold_drive FOREIGN KEY (test_drive_id) REFERENCES t_test_drive(id) ON DELETE RESTRICT,
    CONSTRAINT fk_test_drive_hold_vehicle FOREIGN KEY (vehicle_id) REFERENCES t_product_vehicle(id) ON DELETE RESTRICT,
    CONSTRAINT chk_test_drive_hold_status CHECK (status IN ('ACTIVE', 'RELEASED')),
    CONSTRAINT chk_test_drive_hold_time_range CHECK (start_time < end_time)
);

CREATE INDEX IF NOT EXISTS idx_test_drive_hold_vehicle_time ON t_test_drive_vehicle_hold(vehicle_id, status, start_time, end_time);
CREATE INDEX IF NOT EXISTS idx_test_drive_hold_drive ON t_test_drive_vehicle_hold(test_drive_id, status);

CREATE TABLE IF NOT EXISTS t_test_drive_status_history
(
    id             BIGINT NOT NULL AUTO_INCREMENT,
    test_drive_id  BIGINT NOT NULL,
    from_status    VARCHAR(50),
    to_status      VARCHAR(50) NOT NULL,
    action_type    VARCHAR(50) NOT NULL,
    reason         VARCHAR(500),
    old_start_time TIMESTAMP,
    old_end_time   TIMESTAMP,
    new_start_time TIMESTAMP,
    new_end_time   TIMESTAMP,
    operate_by     INTEGER NOT NULL,
    operate_time   TIMESTAMP NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_test_drive_history_drive FOREIGN KEY (test_drive_id) REFERENCES t_test_drive(id) ON DELETE RESTRICT,
    CONSTRAINT fk_test_drive_history_operator FOREIGN KEY (operate_by) REFERENCES t_user(id) ON DELETE RESTRICT,
    CONSTRAINT chk_test_drive_history_to_status CHECK (to_status IN (
        'PENDING_CONFIRM', 'SCHEDULED', 'RESCHEDULED', 'CHECKED_IN',
        'COMPLETED', 'CANCELED', 'NO_SHOW', 'EXCEPTION_CLOSED'
    )),
    CONSTRAINT chk_test_drive_history_from_status CHECK (from_status IS NULL OR from_status IN (
        'PENDING_CONFIRM', 'SCHEDULED', 'RESCHEDULED', 'CHECKED_IN',
        'COMPLETED', 'CANCELED', 'NO_SHOW', 'EXCEPTION_CLOSED'
    ))
);

CREATE INDEX IF NOT EXISTS idx_test_drive_history_drive ON t_test_drive_status_history(test_drive_id, operate_time);

CREATE TABLE IF NOT EXISTS t_follow_task
(
    id                      BIGINT NOT NULL AUTO_INCREMENT,
    title                   VARCHAR(128) NOT NULL,
    task_type               VARCHAR(64) NOT NULL,
    related_object_type     VARCHAR(32) NOT NULL,
    related_object_id       BIGINT NOT NULL,
    owner_id                INTEGER NOT NULL,
    priority                VARCHAR(32) NOT NULL DEFAULT 'NORMAL',
    due_time                TIMESTAMP NOT NULL,
    remind_time             TIMESTAMP,
    status                  VARCHAR(32) NOT NULL,
    result                  VARCHAR(500),
    postpone_reason         VARCHAR(500),
    original_due_time       TIMESTAMP,
    postpone_count          INTEGER NOT NULL DEFAULT 0,
    cancel_reason           VARCHAR(500),
    communication_record_id BIGINT,
    completed_time          TIMESTAMP,
    completed_by            INTEGER,
    version                 INTEGER NOT NULL DEFAULT 0,
    create_time             TIMESTAMP,
    create_by               INTEGER,
    update_time             TIMESTAMP,
    update_by               INTEGER,
    PRIMARY KEY (id),
    CONSTRAINT chk_follow_task_object_type CHECK (related_object_type IN ('CLUE', 'CUSTOMER', 'OPPORTUNITY', 'TEST_DRIVE', 'ORDER')),
    CONSTRAINT chk_follow_task_status CHECK (status IN ('PENDING', 'IN_PROGRESS', 'POSTPONED', 'OVERDUE', 'COMPLETED', 'CANCELLED', 'CLOSED')),
    CONSTRAINT chk_follow_task_priority CHECK (priority IN ('LOW', 'NORMAL', 'HIGH', 'URGENT')),
    CONSTRAINT chk_follow_task_postpone_count CHECK (postpone_count >= 0)
);

CREATE INDEX IF NOT EXISTS idx_follow_task_owner_due ON t_follow_task(owner_id, status, due_time, id);
CREATE INDEX IF NOT EXISTS idx_follow_task_object ON t_follow_task(related_object_type, related_object_id, due_time, id);

CREATE TABLE IF NOT EXISTS t_communication_record
(
    id                   BIGINT NOT NULL AUTO_INCREMENT,
    follow_task_id       BIGINT,
    parent_record_id     BIGINT,
    related_object_type  VARCHAR(32) NOT NULL,
    related_object_id    BIGINT NOT NULL,
    owner_id             INTEGER NOT NULL,
    communication_method VARCHAR(32) NOT NULL,
    communication_time   TIMESTAMP NOT NULL,
    summary              VARCHAR(500) NOT NULL,
    customer_feedback    VARCHAR(500),
    next_action          VARCHAR(500),
    next_follow_time     TIMESTAMP,
    status               VARCHAR(32) NOT NULL,
    correction_reason    VARCHAR(500),
    void_reason          VARCHAR(500),
    version              INTEGER NOT NULL DEFAULT 0,
    create_time          TIMESTAMP,
    create_by            INTEGER,
    update_time          TIMESTAMP,
    update_by            INTEGER,
    PRIMARY KEY (id),
    CONSTRAINT fk_comm_record_task FOREIGN KEY (follow_task_id) REFERENCES t_follow_task(id) ON DELETE RESTRICT,
    CONSTRAINT fk_comm_record_parent FOREIGN KEY (parent_record_id) REFERENCES t_communication_record(id) ON DELETE RESTRICT,
    CONSTRAINT chk_comm_record_object_type CHECK (related_object_type IN ('CLUE', 'CUSTOMER', 'OPPORTUNITY', 'TEST_DRIVE', 'ORDER')),
    CONSTRAINT chk_comm_record_status CHECK (status IN ('ACTIVE', 'CORRECTED', 'VOIDED')),
    CONSTRAINT chk_comm_record_method CHECK (communication_method IN ('PHONE', 'STORE_VISIT', 'WECHAT', 'SMS', 'EMAIL', 'OTHER'))
);

CREATE INDEX IF NOT EXISTS idx_comm_record_owner_time ON t_communication_record(owner_id, communication_time, id);
CREATE INDEX IF NOT EXISTS idx_comm_record_object ON t_communication_record(related_object_type, related_object_id, communication_time, id);
CREATE INDEX IF NOT EXISTS idx_comm_record_task ON t_communication_record(follow_task_id, status);

CREATE TABLE IF NOT EXISTS t_product_stock_record
(
    id                BIGINT NOT NULL AUTO_INCREMENT,
    product_id        BIGINT NOT NULL,
    vehicle_id        BIGINT,
    quantity          INTEGER DEFAULT 0,
    type              VARCHAR(50),
    source_type       VARCHAR(50),
    source_id         BIGINT,
    before_status     VARCHAR(50),
    after_status      VARCHAR(50),
    related_record_id BIGINT,
    remark            TEXT,
    create_time       TIMESTAMP,
    create_by         INTEGER,
    PRIMARY KEY (id),
    CONSTRAINT fk_stock_record_product FOREIGN KEY (product_id) REFERENCES t_product(id) ON DELETE RESTRICT,
    CONSTRAINT fk_stock_record_vehicle FOREIGN KEY (vehicle_id) REFERENCES t_product_vehicle(id) ON DELETE RESTRICT,
    CONSTRAINT fk_stock_record_related FOREIGN KEY (related_record_id) REFERENCES t_product_stock_record(id) ON DELETE RESTRICT
);

CREATE TABLE IF NOT EXISTS t_delivery
(
    id                    BIGINT NOT NULL AUTO_INCREMENT,
    tran_id               INTEGER NOT NULL,
    customer_id           INTEGER NOT NULL,
    vehicle_id            BIGINT NOT NULL,
    status                VARCHAR(50) NOT NULL,
    planned_delivery_time TIMESTAMP NOT NULL,
    actual_delivery_time  TIMESTAMP,
    responsible_user_id   INTEGER,
    signer_name           VARCHAR(100),
    signed_at             TIMESTAMP,
    sign_method           VARCHAR(50),
    sign_evidence         VARCHAR(500),
    exception_type        VARCHAR(50),
    exception_reason      VARCHAR(500),
    create_time           TIMESTAMP,
    create_by             INTEGER,
    update_time           TIMESTAMP,
    update_by             INTEGER,
    PRIMARY KEY (id),
    CONSTRAINT uk_delivery_tran UNIQUE (tran_id),
    CONSTRAINT fk_delivery_tran FOREIGN KEY (tran_id) REFERENCES t_tran(id) ON DELETE RESTRICT,
    CONSTRAINT fk_delivery_customer FOREIGN KEY (customer_id) REFERENCES t_customer(id) ON DELETE RESTRICT,
    CONSTRAINT fk_delivery_vehicle FOREIGN KEY (vehicle_id) REFERENCES t_product_vehicle(id) ON DELETE RESTRICT,
    CONSTRAINT fk_delivery_responsible_user FOREIGN KEY (responsible_user_id) REFERENCES t_user(id) ON DELETE RESTRICT,
    CONSTRAINT chk_delivery_status CHECK (status IN (
        'PENDING_PREPARE', 'PREPARING', 'WAITING_CUSTOMER', 'WAITING_DELIVERY',
        'DELIVERING', 'SIGNED', 'COMPLETED', 'EXCEPTION', 'CANCELLED'
    ))
);

CREATE TABLE IF NOT EXISTS t_delivery_check_item
(
    id                  BIGINT NOT NULL AUTO_INCREMENT,
    delivery_id         BIGINT NOT NULL,
    item_code           VARCHAR(64) NOT NULL,
    item_name           VARCHAR(100) NOT NULL,
    status              VARCHAR(30) NOT NULL,
    responsible_user_id INTEGER,
    completed_time      TIMESTAMP,
    remark              VARCHAR(500),
    create_time         TIMESTAMP,
    create_by           INTEGER,
    update_time         TIMESTAMP,
    update_by           INTEGER,
    PRIMARY KEY (id),
    CONSTRAINT uk_delivery_check_item_code UNIQUE (delivery_id, item_code),
    CONSTRAINT fk_delivery_check_delivery FOREIGN KEY (delivery_id) REFERENCES t_delivery(id) ON DELETE RESTRICT,
    CONSTRAINT fk_delivery_check_responsible_user FOREIGN KEY (responsible_user_id) REFERENCES t_user(id) ON DELETE RESTRICT,
    CONSTRAINT chk_delivery_check_status CHECK (status IN ('PENDING', 'COMPLETED', 'BLOCKED'))
);

CREATE TABLE IF NOT EXISTS t_tran_remark
(
    id           INTEGER NOT NULL AUTO_INCREMENT,
    tran_id      INTEGER,
    note_way     INTEGER,
    note_content VARCHAR(255),
    create_time  TIMESTAMP,
    create_by    INTEGER,
    edit_time    TIMESTAMP,
    edit_by      INTEGER,
    deleted      INTEGER,
    PRIMARY KEY (id),
    CONSTRAINT fk_tran_remark_tran FOREIGN KEY (tran_id) REFERENCES t_tran(id) ON DELETE RESTRICT
);

CREATE TABLE IF NOT EXISTS t_ai_conversation
(
    id                  BIGINT NOT NULL AUTO_INCREMENT,
    conversation_no     VARCHAR(64) NOT NULL,
    user_id             INTEGER NOT NULL,
    title               VARCHAR(128) NOT NULL,
    status              VARCHAR(32) NOT NULL,
    entry_point         VARCHAR(32) NOT NULL,
    context_object_type VARCHAR(64),
    context_object_id   VARCHAR(64),
    summary_text        VARCHAR(8000),
    last_run_no         VARCHAR(64),
    last_message_time   TIMESTAMP,
    create_time         TIMESTAMP NOT NULL,
    create_by           INTEGER NOT NULL,
    edit_time           TIMESTAMP,
    edit_by             INTEGER,
    PRIMARY KEY (id),
    CONSTRAINT uk_ai_conversation_no UNIQUE (conversation_no),
    CONSTRAINT fk_ai_conversation_user FOREIGN KEY (user_id) REFERENCES t_user(id) ON DELETE RESTRICT,
    CONSTRAINT chk_ai_conversation_status CHECK (status IN ('ACTIVE', 'ARCHIVED')),
    CONSTRAINT chk_ai_conversation_entry_point CHECK (entry_point IN ('PAGE', 'SIDE_PANEL'))
);

CREATE INDEX IF NOT EXISTS idx_ai_conversation_user_time ON t_ai_conversation(user_id, last_message_time, create_time, id);
CREATE INDEX IF NOT EXISTS idx_ai_conversation_context ON t_ai_conversation(user_id, context_object_type, context_object_id, status);

CREATE TABLE IF NOT EXISTS t_ai_run
(
    id                  BIGINT NOT NULL AUTO_INCREMENT,
    run_no              VARCHAR(64) NOT NULL,
    conversation_id     BIGINT NOT NULL,
    parent_run_id       BIGINT,
    turn_no             INTEGER NOT NULL,
    user_id             INTEGER NOT NULL,
    user_name           VARCHAR(64),
    entry_point         VARCHAR(32) NOT NULL,
    context_object_type VARCHAR(64),
    context_object_id   VARCHAR(64),
    prompt_summary      VARCHAR(4000) NOT NULL,
    status              VARCHAR(32) NOT NULL,
    error_code          VARCHAR(64),
    error_message       VARCHAR(255),
    started_time        TIMESTAMP,
    completed_time      TIMESTAMP,
    expires_time        TIMESTAMP,
    context_active      BOOLEAN NOT NULL DEFAULT TRUE,
    invalidation_reason VARCHAR(255),
    create_time         TIMESTAMP NOT NULL,
    create_by           INTEGER NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_ai_run_no UNIQUE (run_no),
    CONSTRAINT uk_ai_run_conversation_turn UNIQUE (conversation_id, turn_no),
    CONSTRAINT fk_ai_run_conversation FOREIGN KEY (conversation_id) REFERENCES t_ai_conversation(id) ON DELETE RESTRICT,
    CONSTRAINT fk_ai_run_parent FOREIGN KEY (parent_run_id) REFERENCES t_ai_run(id) ON DELETE RESTRICT,
    CONSTRAINT fk_ai_run_user FOREIGN KEY (user_id) REFERENCES t_user(id) ON DELETE RESTRICT,
    CONSTRAINT chk_ai_run_status CHECK (status IN ('CREATED', 'RUNNING', 'WAITING_FOR_APPROVAL', 'COMPLETED', 'FAILED', 'CANCELLED', 'EXPIRED')),
    CONSTRAINT chk_ai_run_entry_point CHECK (entry_point IN ('PAGE', 'SIDE_PANEL'))
);

CREATE INDEX IF NOT EXISTS idx_ai_run_user_time ON t_ai_run(user_id, create_time, id);
CREATE INDEX IF NOT EXISTS idx_ai_run_status ON t_ai_run(status, create_time);
CREATE INDEX IF NOT EXISTS idx_ai_run_context ON t_ai_run(context_object_type, context_object_id);
CREATE INDEX IF NOT EXISTS idx_ai_run_conversation_turn ON t_ai_run(conversation_id, turn_no, id);

CREATE TABLE IF NOT EXISTS t_ai_run_event
(
    id            BIGINT NOT NULL AUTO_INCREMENT,
    run_id        BIGINT NOT NULL,
    event_id      VARCHAR(64) NOT NULL,
    sequence_no   INTEGER NOT NULL,
    event_type    VARCHAR(64) NOT NULL,
    payload_json  TEXT NOT NULL,
    occurred_time TIMESTAMP NOT NULL,
    create_time   TIMESTAMP NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_ai_run_event_sequence UNIQUE (run_id, sequence_no),
    CONSTRAINT uk_ai_run_event_id UNIQUE (run_id, event_id),
    CONSTRAINT fk_ai_run_event_run FOREIGN KEY (run_id) REFERENCES t_ai_run(id) ON DELETE RESTRICT
);

CREATE TABLE IF NOT EXISTS t_ai_assistant_policy
(
    id                     BIGINT NOT NULL,
    enabled_tools          BOOLEAN NOT NULL DEFAULT TRUE,
    allowed_tool_names     TEXT NOT NULL,
    proposals_enabled      BOOLEAN NOT NULL DEFAULT TRUE,
    max_tool_calls_per_run INTEGER NOT NULL,
    safety_mode            VARCHAR(32) NOT NULL,
    network_mode           VARCHAR(32) NOT NULL,
    context_message_limit  INTEGER NOT NULL,
    summary_max_chars      INTEGER NOT NULL,
    max_run_seconds        INTEGER NOT NULL,
    version                INTEGER NOT NULL,
    create_time            TIMESTAMP NOT NULL,
    create_by              INTEGER NOT NULL,
    edit_time              TIMESTAMP,
    edit_by                INTEGER,
    PRIMARY KEY (id),
    CONSTRAINT chk_ai_policy_singleton CHECK (id = 1),
    CONSTRAINT chk_ai_policy_safety_mode CHECK (safety_mode IN ('STRICT', 'STANDARD')),
    CONSTRAINT chk_ai_policy_network_mode CHECK (network_mode IN ('DISABLED', 'PROVIDER_ONLY')),
    CONSTRAINT chk_ai_policy_context_limit CHECK (context_message_limit BETWEEN 1 AND 8)
);

MERGE INTO t_ai_assistant_policy
(id, enabled_tools, allowed_tool_names, proposals_enabled, max_tool_calls_per_run,
 safety_mode, network_mode, context_message_limit, summary_max_chars, max_run_seconds,
 version, create_time, create_by, edit_time, edit_by)
KEY(id) VALUES
(1, TRUE, '["create_communication_record_proposal","create_follow_task_proposal","get_business_overview","get_customer_profile","get_delivery_detail","get_inventory_alerts","get_opportunity_detail","get_quote_detail","get_test_drive_detail","get_transaction_detail","list_my_followups","list_pending_transaction_approvals","resolve_vehicle_product","search_customers"]',
 TRUE, 8, 'STRICT', 'PROVIDER_ONLY', 8, 2000, 120, 1, CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP, 1);

CREATE TABLE IF NOT EXISTS t_ai_provider_config
(
    id                       BIGINT NOT NULL AUTO_INCREMENT,
    config_no                VARCHAR(64) NOT NULL,
    provider_name            VARCHAR(64) NOT NULL,
    provider_format          VARCHAR(32) NOT NULL,
    base_url                 VARCHAR(255) NOT NULL,
    model_name               VARCHAR(128) NOT NULL,
    model_display_name       VARCHAR(128) NOT NULL,
    encrypted_api_key        VARCHAR(1000) NOT NULL,
    api_key_nonce            VARCHAR(128) NOT NULL,
    masked_api_key           VARCHAR(64) NOT NULL,
    enabled                  BOOLEAN NOT NULL DEFAULT FALSE,
    test_status              VARCHAR(32) NOT NULL,
    last_test_time           TIMESTAMP,
    last_test_error_code     VARCHAR(64),
    last_test_message        VARCHAR(255),
    timeout_seconds          INTEGER NOT NULL,
    max_output_tokens        INTEGER NOT NULL,
    temperature              DECIMAL(4,2) NOT NULL,
    create_time              TIMESTAMP NOT NULL,
    create_by                INTEGER NOT NULL,
    edit_time                TIMESTAMP,
    edit_by                  INTEGER,
    PRIMARY KEY (id),
    CONSTRAINT uk_ai_provider_config_no UNIQUE (config_no),
    CONSTRAINT chk_ai_provider_format CHECK (provider_format IN ('OPENAI_COMPATIBLE', 'ANTHROPIC')),
    CONSTRAINT chk_ai_provider_test_status CHECK (test_status IN ('UNTESTED', 'SUCCESS', 'FAILED'))
);

CREATE INDEX IF NOT EXISTS idx_ai_provider_enabled ON t_ai_provider_config(enabled, edit_time, id);

CREATE TABLE IF NOT EXISTS t_ai_workflow
(
    id                  BIGINT NOT NULL AUTO_INCREMENT,
    workflow_no         VARCHAR(64) NOT NULL,
    run_id              BIGINT NOT NULL,
    user_id             INTEGER NOT NULL,
    workflow_type       VARCHAR(64) NOT NULL,
    title               VARCHAR(128) NOT NULL,
    status              VARCHAR(32) NOT NULL,
    current_step_no     INTEGER,
    context_object_type VARCHAR(64),
    context_object_id   VARCHAR(64),
    pause_reason        VARCHAR(500),
    error_code          VARCHAR(64),
    error_message       VARCHAR(255),
    started_time        TIMESTAMP,
    paused_time         TIMESTAMP,
    resumed_time        TIMESTAMP,
    completed_time      TIMESTAMP,
    expires_time        TIMESTAMP,
    create_time         TIMESTAMP NOT NULL,
    create_by           INTEGER NOT NULL,
    edit_time           TIMESTAMP,
    edit_by             INTEGER,
    PRIMARY KEY (id),
    CONSTRAINT uk_ai_workflow_no UNIQUE (workflow_no),
    CONSTRAINT fk_ai_workflow_run FOREIGN KEY (run_id) REFERENCES t_ai_run(id) ON DELETE RESTRICT,
    CONSTRAINT fk_ai_workflow_user FOREIGN KEY (user_id) REFERENCES t_user(id) ON DELETE RESTRICT,
    CONSTRAINT chk_ai_workflow_type CHECK (workflow_type IN ('CUSTOMER_FOLLOW_UP', 'TRANSACTION_GAP_REVIEW', 'INVENTORY_RISK_REVIEW')),
    CONSTRAINT chk_ai_workflow_status CHECK (status IN ('CREATED', 'RUNNING', 'PAUSED', 'WAITING_USER_CONFIRMATION', 'COMPLETED', 'FAILED', 'CANCELLED', 'EXPIRED'))
);

CREATE INDEX IF NOT EXISTS idx_ai_workflow_run ON t_ai_workflow(run_id, create_time, id);
CREATE INDEX IF NOT EXISTS idx_ai_workflow_user_status ON t_ai_workflow(user_id, status, create_time);

CREATE TABLE IF NOT EXISTS t_ai_message
(
    id              BIGINT NOT NULL AUTO_INCREMENT,
    message_no      VARCHAR(64) NOT NULL,
    conversation_id BIGINT NOT NULL,
    run_id          BIGINT NOT NULL,
    role            VARCHAR(32) NOT NULL,
    sequence_no     INTEGER NOT NULL,
    visible_to_user BOOLEAN NOT NULL DEFAULT TRUE,
    status          VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    revision_no     INTEGER NOT NULL DEFAULT 1,
    supersedes_message_id BIGINT,
    included_in_context BOOLEAN NOT NULL DEFAULT TRUE,
    version         INTEGER NOT NULL DEFAULT 1,
    content_summary VARCHAR(16000) NOT NULL,
    create_time     TIMESTAMP NOT NULL,
    create_by       INTEGER NOT NULL,
    edit_time       TIMESTAMP,
    edit_by         INTEGER,
    withdrawn_time  TIMESTAMP,
    withdrawn_by    INTEGER,
    PRIMARY KEY (id),
    CONSTRAINT uk_ai_message_run_seq UNIQUE (run_id, sequence_no),
    CONSTRAINT uk_ai_message_no UNIQUE (message_no),
    CONSTRAINT fk_ai_message_conversation FOREIGN KEY (conversation_id) REFERENCES t_ai_conversation(id) ON DELETE RESTRICT,
    CONSTRAINT fk_ai_message_run FOREIGN KEY (run_id) REFERENCES t_ai_run(id) ON DELETE RESTRICT,
    CONSTRAINT fk_ai_message_supersedes FOREIGN KEY (supersedes_message_id) REFERENCES t_ai_message(id) ON DELETE RESTRICT,
    CONSTRAINT chk_ai_message_role CHECK (role IN ('USER', 'ASSISTANT', 'SYSTEM', 'TOOL')),
    CONSTRAINT chk_ai_message_status CHECK (status IN ('ACTIVE', 'SUPERSEDED', 'WITHDRAWN'))
);

CREATE INDEX IF NOT EXISTS idx_ai_message_conversation_time ON t_ai_message(conversation_id, visible_to_user, create_time, id);

CREATE TABLE IF NOT EXISTS t_ai_tool_call
(
    id              BIGINT NOT NULL AUTO_INCREMENT,
    run_id          BIGINT NOT NULL,
    tool_name       VARCHAR(128) NOT NULL,
    permission_code VARCHAR(128) NOT NULL,
    risk_level      VARCHAR(32) NOT NULL,
    input_summary   VARCHAR(1000) NOT NULL,
    output_summary  VARCHAR(1000),
    object_refs     VARCHAR(1000),
    display_payload_json TEXT,
    result_status   VARCHAR(32) NOT NULL,
    error_code      VARCHAR(64),
    duration_ms     INTEGER,
    started_time    TIMESTAMP NOT NULL,
    completed_time  TIMESTAMP,
    create_by       INTEGER NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_ai_tool_run FOREIGN KEY (run_id) REFERENCES t_ai_run(id) ON DELETE RESTRICT,
    CONSTRAINT chk_ai_tool_risk CHECK (risk_level IN ('READONLY', 'LOW', 'MEDIUM', 'HIGH')),
    CONSTRAINT chk_ai_tool_result CHECK (result_status IN ('STARTED', 'SUCCESS', 'FAILED'))
);

CREATE INDEX IF NOT EXISTS idx_ai_tool_run ON t_ai_tool_call(run_id, started_time, id);
CREATE INDEX IF NOT EXISTS idx_ai_tool_name ON t_ai_tool_call(tool_name, started_time);

CREATE TABLE IF NOT EXISTS t_ai_action_proposal
(
    id                  BIGINT NOT NULL AUTO_INCREMENT,
    run_id              BIGINT NOT NULL,
    proposal_type       VARCHAR(128) NOT NULL,
    status              VARCHAR(32) NOT NULL,
    risk_level          VARCHAR(32) NOT NULL,
    permission_code     VARCHAR(128) NOT NULL,
    related_object_type VARCHAR(64) NOT NULL,
    related_object_id   VARCHAR(64) NOT NULL,
    normalized_params   TEXT NOT NULL,
    params_hash         VARCHAR(128) NOT NULL,
    params_summary      VARCHAR(1000) NOT NULL,
    impact_summary      VARCHAR(1000) NOT NULL,
    expires_time        TIMESTAMP NOT NULL,
    confirmed_time      TIMESTAMP,
    executed_time       TIMESTAMP,
    result_summary      VARCHAR(1000),
    error_code          VARCHAR(64),
    create_time         TIMESTAMP NOT NULL,
    create_by           INTEGER NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_ai_proposal_run FOREIGN KEY (run_id) REFERENCES t_ai_run(id) ON DELETE RESTRICT,
    CONSTRAINT chk_ai_proposal_type CHECK (proposal_type IN ('create_communication_record_proposal', 'create_follow_task_proposal')),
    CONSTRAINT chk_ai_proposal_status CHECK (status IN ('PENDING_CONFIRMATION', 'CONFIRMED', 'REJECTED', 'EXPIRED', 'EXECUTED', 'FAILED')),
    CONSTRAINT chk_ai_proposal_risk CHECK (risk_level IN ('LOW'))
);

CREATE INDEX IF NOT EXISTS idx_ai_proposal_run ON t_ai_action_proposal(run_id, create_time, id);
CREATE INDEX IF NOT EXISTS idx_ai_proposal_status ON t_ai_action_proposal(status, expires_time);

CREATE TABLE IF NOT EXISTS t_ai_workflow_step
(
    id             BIGINT NOT NULL AUTO_INCREMENT,
    workflow_id    BIGINT NOT NULL,
    step_no        INTEGER NOT NULL,
    step_type      VARCHAR(64) NOT NULL,
    title          VARCHAR(128) NOT NULL,
    status         VARCHAR(32) NOT NULL,
    tool_name      VARCHAR(128),
    proposal_id    BIGINT,
    input_summary  VARCHAR(1000),
    output_summary VARCHAR(1000),
    error_code     VARCHAR(64),
    started_time   TIMESTAMP,
    completed_time TIMESTAMP,
    create_time    TIMESTAMP NOT NULL,
    create_by      INTEGER NOT NULL,
    edit_time      TIMESTAMP,
    edit_by        INTEGER,
    PRIMARY KEY (id),
    CONSTRAINT uk_ai_workflow_step_no UNIQUE (workflow_id, step_no),
    CONSTRAINT fk_ai_workflow_step_workflow FOREIGN KEY (workflow_id) REFERENCES t_ai_workflow(id) ON DELETE RESTRICT,
    CONSTRAINT fk_ai_workflow_step_proposal FOREIGN KEY (proposal_id) REFERENCES t_ai_action_proposal(id) ON DELETE RESTRICT,
    CONSTRAINT chk_ai_workflow_step_status CHECK (status IN ('PENDING', 'RUNNING', 'WAITING_USER_CONFIRMATION', 'COMPLETED', 'FAILED', 'CANCELLED', 'EXPIRED'))
);

CREATE TABLE IF NOT EXISTS t_ai_approval
(
    id                 BIGINT NOT NULL AUTO_INCREMENT,
    run_id             BIGINT NOT NULL,
    proposal_id        BIGINT NOT NULL,
    decision           VARCHAR(32) NOT NULL,
    permission_summary VARCHAR(500) NOT NULL,
    reason             VARCHAR(500),
    result_status      VARCHAR(32) NOT NULL,
    approved_time      TIMESTAMP NOT NULL,
    approved_by        INTEGER NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_ai_approval_run FOREIGN KEY (run_id) REFERENCES t_ai_run(id) ON DELETE RESTRICT,
    CONSTRAINT fk_ai_approval_proposal FOREIGN KEY (proposal_id) REFERENCES t_ai_action_proposal(id) ON DELETE RESTRICT,
    CONSTRAINT fk_ai_approval_user FOREIGN KEY (approved_by) REFERENCES t_user(id) ON DELETE RESTRICT,
    CONSTRAINT chk_ai_approval_decision CHECK (decision IN ('CONFIRMED', 'REJECTED', 'EXPIRED')),
    CONSTRAINT chk_ai_approval_result CHECK (result_status IN ('SUCCESS', 'FAILED', 'IGNORED'))
);

CREATE INDEX IF NOT EXISTS idx_ai_approval_run ON t_ai_approval(run_id, approved_time);

CREATE TABLE IF NOT EXISTS t_ai_execution_event
(
    id            BIGINT NOT NULL AUTO_INCREMENT,
    run_id        BIGINT NOT NULL,
    proposal_id   BIGINT,
    event_type    VARCHAR(64) NOT NULL,
    result_status VARCHAR(32) NOT NULL,
    object_type   VARCHAR(64),
    object_id     VARCHAR(64),
    summary       VARCHAR(1000) NOT NULL,
    error_code    VARCHAR(64),
    occurred_time TIMESTAMP NOT NULL,
    create_by     INTEGER NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_ai_execution_run FOREIGN KEY (run_id) REFERENCES t_ai_run(id) ON DELETE RESTRICT,
    CONSTRAINT fk_ai_execution_proposal FOREIGN KEY (proposal_id) REFERENCES t_ai_action_proposal(id) ON DELETE RESTRICT,
    CONSTRAINT chk_ai_execution_result CHECK (result_status IN ('SUCCESS', 'FAILED', 'SKIPPED'))
);

CREATE INDEX IF NOT EXISTS idx_ai_execution_run ON t_ai_execution_event(run_id, occurred_time, id);
CREATE INDEX IF NOT EXISTS idx_ai_execution_object ON t_ai_execution_event(object_type, object_id);

CREATE TABLE IF NOT EXISTS t_ai_proactive_subscription
(
    id                       BIGINT NOT NULL AUTO_INCREMENT,
    subscription_no          VARCHAR(64) NOT NULL,
    user_id                  INTEGER NOT NULL,
    subscription_type        VARCHAR(64) NOT NULL,
    status                   VARCHAR(32) NOT NULL,
    frequency                VARCHAR(32) NOT NULL,
    quiet_start_time         VARCHAR(5),
    quiet_end_time           VARCHAR(5),
    daily_limit              INTEGER NOT NULL DEFAULT 5,
    max_results              INTEGER NOT NULL DEFAULT 10,
    duplicate_window_minutes INTEGER NOT NULL DEFAULT 60,
    config_summary           VARCHAR(1000),
    last_triggered_time      TIMESTAMP,
    next_trigger_time        TIMESTAMP,
    create_time              TIMESTAMP NOT NULL,
    create_by                INTEGER NOT NULL,
    edit_time                TIMESTAMP,
    edit_by                  INTEGER,
    PRIMARY KEY (id),
    CONSTRAINT uk_ai_proactive_subscription_no UNIQUE (subscription_no),
    CONSTRAINT fk_ai_proactive_subscription_user FOREIGN KEY (user_id) REFERENCES t_user(id) ON DELETE RESTRICT,
    CONSTRAINT chk_ai_proactive_subscription_type CHECK (subscription_type IN ('FOLLOW_UP_REMINDER', 'TRANSACTION_EXCEPTION', 'INVENTORY_ALERT', 'DAILY_SUMMARY', 'PERIODIC_SALES_ANALYSIS')),
    CONSTRAINT chk_ai_proactive_subscription_status CHECK (status IN ('ACTIVE', 'PAUSED', 'CANCELLED')),
    CONSTRAINT chk_ai_proactive_frequency CHECK (frequency IN ('REALTIME_LIMITED', 'DAILY', 'WEEKLY', 'MONTHLY'))
);

CREATE INDEX IF NOT EXISTS idx_ai_proactive_subscription_user ON t_ai_proactive_subscription(user_id, status, next_trigger_time);

CREATE TABLE IF NOT EXISTS t_ai_proactive_event
(
    id              BIGINT NOT NULL AUTO_INCREMENT,
    event_no        VARCHAR(64) NOT NULL,
    subscription_id BIGINT NOT NULL,
    user_id         INTEGER NOT NULL,
    event_type      VARCHAR(64) NOT NULL,
    status          VARCHAR(32) NOT NULL,
    title           VARCHAR(128) NOT NULL,
    summary         VARCHAR(1000) NOT NULL,
    detail_summary  VARCHAR(2000),
    object_type     VARCHAR(64),
    object_id       VARCHAR(64),
    severity        VARCHAR(32) NOT NULL,
    generated_time  TIMESTAMP NOT NULL,
    delivered_time  TIMESTAMP,
    error_code      VARCHAR(64),
    create_time     TIMESTAMP NOT NULL,
    create_by       INTEGER NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_ai_proactive_event_no UNIQUE (event_no),
    CONSTRAINT fk_ai_proactive_event_subscription FOREIGN KEY (subscription_id) REFERENCES t_ai_proactive_subscription(id) ON DELETE RESTRICT,
    CONSTRAINT fk_ai_proactive_event_user FOREIGN KEY (user_id) REFERENCES t_user(id) ON DELETE RESTRICT,
    CONSTRAINT chk_ai_proactive_event_status CHECK (status IN ('CREATED', 'GENERATING', 'READY', 'NO_DATA', 'FAILED', 'SKIPPED'))
);

CREATE INDEX IF NOT EXISTS idx_ai_proactive_event_user ON t_ai_proactive_event(user_id, generated_time, id);
CREATE INDEX IF NOT EXISTS idx_ai_proactive_event_subscription ON t_ai_proactive_event(subscription_id, generated_time, id);
CREATE INDEX IF NOT EXISTS idx_ai_proactive_event_object ON t_ai_proactive_event(object_type, object_id, generated_time);
