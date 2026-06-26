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
    enabled   TINYINT NOT NULL DEFAULT 1,
    PRIMARY KEY (id),
    CONSTRAINT uk_permission_code UNIQUE (code),
    CONSTRAINT chk_permission_type CHECK (type IN ('menu', 'button')),
    CONSTRAINT chk_permission_parent_self CHECK (parent_id IS NULL OR parent_id <> id),
    CONSTRAINT fk_permission_parent FOREIGN KEY (parent_id) REFERENCES t_permission(id) ON DELETE RESTRICT
);

CREATE TABLE IF NOT EXISTS t_role
(
    id        INTEGER NOT NULL AUTO_INCREMENT,
    role      VARCHAR(64) NOT NULL,
    role_name VARCHAR(64) NOT NULL,
    enabled   TINYINT NOT NULL DEFAULT 1,
    PRIMARY KEY (id),
    CONSTRAINT uk_role_code UNIQUE (role)
);

CREATE TABLE IF NOT EXISTS t_role_permission
(
    role_id       INTEGER NOT NULL,
    permission_id INTEGER NOT NULL,
    PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_role_permission_role FOREIGN KEY (role_id) REFERENCES t_role(id) ON DELETE CASCADE,
    CONSTRAINT fk_role_permission_permission FOREIGN KEY (permission_id) REFERENCES t_permission(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS t_user
(
    id                     INTEGER NOT NULL AUTO_INCREMENT,
    login_act              VARCHAR(32),
    login_pwd              VARCHAR(64),
    name                   VARCHAR(32),
    phone                  VARCHAR(18),
    email                  VARCHAR(64),
    account_no_expired     INTEGER,
    credentials_no_expired INTEGER,
    account_no_locked      INTEGER,
    account_enabled        INTEGER,
    create_time            TIMESTAMP,
    create_by              INTEGER,
    edit_time              TIMESTAMP,
    edit_by                INTEGER,
    last_login_time        TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_user_login_act UNIQUE (login_act),
    CONSTRAINT uk_user_email UNIQUE (email),
    CONSTRAINT uk_user_phone UNIQUE (phone)
);

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
    user_id INTEGER NOT NULL,
    role_id INTEGER NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_role_user FOREIGN KEY (user_id) REFERENCES t_user(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_role_role FOREIGN KEY (role_id) REFERENCES t_role(id) ON DELETE CASCADE
);

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
    action_code VARCHAR(32) NOT NULL,
    object_type VARCHAR(64),
    module_name VARCHAR(64),
    resource_id VARCHAR(64),
    result      VARCHAR(32),
    detail      VARCHAR(512),
    ip          VARCHAR(64),
    request_id  VARCHAR(64),
    create_time TIMESTAMP,
    PRIMARY KEY (id)
);
CREATE INDEX IF NOT EXISTS idx_operation_log_time ON t_operation_log(create_time, id);
CREATE INDEX IF NOT EXISTS idx_operation_log_query ON t_operation_log(module_name, action_code, user_id, result);

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
