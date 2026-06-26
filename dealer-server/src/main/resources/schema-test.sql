-- H2 Database Schema for Testing
-- Converted from MySQL CarDealerCRM.sql
-- 与 CarDealerCRM.sql 保持等价约束：唯一约束、外键、CHECK、NOT NULL、value_code、BIGINT 对齐

CREATE TABLE IF NOT EXISTS t_activity
(
    id          INTEGER NOT NULL AUTO_INCREMENT,
    owner_id    INTEGER,
    name        VARCHAR(128),
    start_time  TIMESTAMP,
    end_time    TIMESTAMP,
    cost        DECIMAL(11, 2),
    description VARCHAR(255),
    create_time TIMESTAMP,
    create_by   INTEGER,
    edit_time   TIMESTAMP,
    edit_by           INTEGER,
    PRIMARY KEY (id)
);

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
    id        INTEGER NOT NULL AUTO_INCREMENT,
    type_code VARCHAR(64) NOT NULL,
    type_name VARCHAR(64),
    remark    VARCHAR(128),
    PRIMARY KEY (id),
    CONSTRAINT uk_type_code UNIQUE (type_code)
);

CREATE TABLE IF NOT EXISTS t_dic_value
(
    id         INTEGER NOT NULL AUTO_INCREMENT,
    type_code  VARCHAR(64),
    type_value VARCHAR(64),
    value_code VARCHAR(64) NOT NULL,
    `order`    INTEGER,
    remark     VARCHAR(64),
    PRIMARY KEY (id),
    CONSTRAINT uk_type_value_code UNIQUE (type_code, value_code)
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
    promotion_name        VARCHAR(255),
    promotion_amount      DECIMAL(10, 2),
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
    module_name VARCHAR(64),
    resource_id VARCHAR(64),
    detail      VARCHAR(512),
    ip          VARCHAR(64),
    create_time TIMESTAMP,
    PRIMARY KEY (id)
);

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
    id           BIGINT NOT NULL AUTO_INCREMENT,
    product_id   BIGINT NOT NULL,
    name         VARCHAR(255) NOT NULL,
    type        VARCHAR(50),
    discount    DECIMAL(10, 2),
    start_time  TIMESTAMP,
    end_time    TIMESTAMP,
    status      VARCHAR(50),
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_product_promotion_product FOREIGN KEY (product_id) REFERENCES t_product(id) ON DELETE RESTRICT
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
