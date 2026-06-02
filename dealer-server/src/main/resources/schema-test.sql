-- H2 Database Schema for Testing
-- Converted from MySQL CarManager.sql

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
    PRIMARY KEY (id)
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
    PRIMARY KEY (id)
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
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS t_customer
(
    id                INTEGER NOT NULL AUTO_INCREMENT,
    clue_id           INTEGER,
    product           INTEGER,
    description       VARCHAR(255),
    next_contact_time TIMESTAMP,
    create_time       TIMESTAMP,
    create_by         INTEGER,
    edit_time         TIMESTAMP,
    edit_by           INTEGER,
    PRIMARY KEY (id)
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
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS t_dic_type
(
    id        INTEGER NOT NULL AUTO_INCREMENT,
    type_code VARCHAR(64) NOT NULL,
    type_name VARCHAR(64),
    remark    VARCHAR(128),
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS t_dic_value
(
    id         INTEGER NOT NULL AUTO_INCREMENT,
    type_code  VARCHAR(64),
    type_value VARCHAR(64),
    `order`    INTEGER,
    remark     VARCHAR(64),
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS t_permission
(
    id        INTEGER NOT NULL AUTO_INCREMENT,
    name      VARCHAR(30),
    code      VARCHAR(30),
    url       VARCHAR(30),
    type      VARCHAR(30),
    parent_id INTEGER,
    order_no  INTEGER,
    icon      VARCHAR(100),
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS t_role
(
    id        INTEGER NOT NULL AUTO_INCREMENT,
    role      VARCHAR(30),
    role_name VARCHAR(30),
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS t_role_permission
(
    id            INTEGER NOT NULL AUTO_INCREMENT,
    role_id       INTEGER,
    permission_id INTEGER,
    PRIMARY KEY (id)
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
    PRIMARY KEY (id)
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
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS t_tran_product
(
    id          INTEGER NOT NULL AUTO_INCREMENT,
    tran_id     INTEGER NOT NULL,
    product_id  INTEGER NOT NULL,
    quantity    INTEGER NOT NULL,
    price       DECIMAL(10, 2) NOT NULL,
    create_time TIMESTAMP,
    create_by   INTEGER,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS t_tran_production
(
    id              INTEGER NOT NULL AUTO_INCREMENT,
    tran_product_id INTEGER NOT NULL,
    status          VARCHAR(20) NOT NULL,
    description     VARCHAR(255),
    create_time     TIMESTAMP,
    create_by       INTEGER,
    update_time     TIMESTAMP,
    update_by       INTEGER,
    PRIMARY KEY (id)
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
    update_time  TIMESTAMP,
    update_by    INTEGER,
    PRIMARY KEY (id),
    CONSTRAINT uk_invoice_no UNIQUE (invoice_no)
);

CREATE TABLE IF NOT EXISTS t_product
(
    id            BIGINT NOT NULL AUTO_INCREMENT,
    sku           VARCHAR(255),
    name          VARCHAR(255) NOT NULL,
    category      VARCHAR(255),
    specification VARCHAR(255),
    price         DECIMAL(10, 2),
    stock         INTEGER,
    min_stock     INTEGER,
    status        VARCHAR(50),
    create_time   TIMESTAMP,
    update_time   TIMESTAMP,
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

CREATE TABLE IF NOT EXISTS t_product_promotion
(
    id          BIGINT NOT NULL AUTO_INCREMENT,
    name        VARCHAR(255) NOT NULL,
    type        VARCHAR(50),
    discount    DECIMAL(10, 2),
    start_time  TIMESTAMP,
    end_time    TIMESTAMP,
    status      VARCHAR(50),
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS t_product_stock_record
(
    id          BIGINT NOT NULL AUTO_INCREMENT,
    product_id  BIGINT NOT NULL,
    quantity    INTEGER,
    type        VARCHAR(50),
    remark      TEXT,
    create_time TIMESTAMP,
    PRIMARY KEY (id)
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
    id      INTEGER NOT NULL AUTO_INCREMENT,
    user_id INTEGER,
    role_id INTEGER,
    PRIMARY KEY (id)
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
    PRIMARY KEY (id)
);
