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
    edit_by     INTEGER,
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
    CONSTRAINT fk_activity_remark_activity FOREIGN KEY (activity_id) REFERENCES t_activity(id) ON DELETE CASCADE
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
    edit_by           INTEGER,
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
    CONSTRAINT fk_clue_remark_clue FOREIGN KEY (clue_id) REFERENCES t_clue(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS t_customer
(
    id                INTEGER NOT NULL AUTO_INCREMENT,
    clue_id           INTEGER,
    product           BIGINT,
    description       VARCHAR(255),
    next_contact_time TIMESTAMP,
    create_time       TIMESTAMP,
    create_by         INTEGER,
    edit_time         TIMESTAMP,
    edit_by           INTEGER,
    PRIMARY KEY (id),
    CONSTRAINT fk_customer_clue FOREIGN KEY (clue_id) REFERENCES t_clue(id) ON DELETE RESTRICT
);

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
    CONSTRAINT fk_customer_remark_customer FOREIGN KEY (customer_id) REFERENCES t_customer(id) ON DELETE CASCADE
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

CREATE TABLE IF NOT EXISTS t_system_info
(
    id           INTEGER NOT NULL AUTO_INCREMENT,
    system_code  VARCHAR(45),
    name         VARCHAR(100) NOT NULL,
    site         VARCHAR(100) NOT NULL,
    logo         VARCHAR(100),
    title        VARCHAR(45) NOT NULL,
    description  VARCHAR(45) NOT NULL,
    keywords     VARCHAR(100) NOT NULL,
    shortcuticon VARCHAR(100) NOT NULL,
    tel          VARCHAR(100),
    weixin       VARCHAR(25),
    email        VARCHAR(45),
    address      VARCHAR(100),
    version      VARCHAR(145),
    closeMsg     VARCHAR(500),
    isopen       VARCHAR(8) DEFAULT 'y',
    create_time  TIMESTAMP,
    create_by    INTEGER,
    edit_time    TIMESTAMP,
    edit_by      INTEGER,
    PRIMARY KEY (id)
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
    CONSTRAINT fk_tran_customer FOREIGN KEY (customer_id) REFERENCES t_customer(id) ON DELETE RESTRICT
);

CREATE TABLE IF NOT EXISTS t_tran_history
(
    id            INTEGER NOT NULL AUTO_INCREMENT,
    tran_id       INTEGER,
    stage         VARCHAR(32),
    money         DECIMAL(10, 2),
    expected_date TIMESTAMP,
    create_time   TIMESTAMP,
    create_by     INTEGER,
    PRIMARY KEY (id),
    CONSTRAINT fk_tran_history_tran FOREIGN KEY (tran_id) REFERENCES t_tran(id) ON DELETE RESTRICT
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
    status        VARCHAR(50) NOT NULL DEFAULT 'off_sale',
    create_time   TIMESTAMP,
    update_time   TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_sku UNIQUE (sku),
    CONSTRAINT chk_product_price_nonneg CHECK (price >= 0),
    CONSTRAINT chk_product_stock_nonneg CHECK (stock >= 0)
);

CREATE TABLE IF NOT EXISTS t_tran_product
(
    id          INTEGER NOT NULL AUTO_INCREMENT,
    tran_id     INTEGER NOT NULL,
    product_id  BIGINT NOT NULL,
    quantity    INTEGER NOT NULL,
    price       DECIMAL(10, 2) NOT NULL,
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
    CONSTRAINT fk_tran_invoice_tran FOREIGN KEY (tran_id) REFERENCES t_tran(id) ON DELETE RESTRICT
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
    remark          VARCHAR(255),
    create_time     TIMESTAMP,
    create_by       INTEGER,
    edit_time       TIMESTAMP,
    edit_by         INTEGER,
    PRIMARY KEY (id),
    CONSTRAINT uk_payment_no UNIQUE (payment_no),
    CONSTRAINT fk_payment_tran FOREIGN KEY (tran_id) REFERENCES t_tran(id) ON DELETE RESTRICT
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

CREATE TABLE IF NOT EXISTS t_product_stock_record
(
    id          BIGINT NOT NULL AUTO_INCREMENT,
    product_id  BIGINT NOT NULL,
    quantity    INTEGER DEFAULT 0,
    type        VARCHAR(50),
    remark      TEXT,
    create_time TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_stock_record_product FOREIGN KEY (product_id) REFERENCES t_product(id) ON DELETE RESTRICT
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
    CONSTRAINT fk_tran_remark_tran FOREIGN KEY (tran_id) REFERENCES t_tran(id) ON DELETE CASCADE
);
