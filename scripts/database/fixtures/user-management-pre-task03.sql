-- Task21 真实迁移测试专用：Task03 之前的最小旧库快照。
CREATE TABLE t_user (
  id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  login_act VARCHAR(32) NULL UNIQUE,
  login_pwd VARCHAR(64) NULL,
  name VARCHAR(64) NOT NULL,
  phone VARCHAR(18) NULL UNIQUE,
  email VARCHAR(64) NULL UNIQUE,
  account_no_expired INT NULL,
  credentials_no_expired INT NULL,
  account_no_locked INT NULL,
  account_enabled INT NULL,
  create_time DATETIME NULL,
  create_by INT NULL,
  edit_time DATETIME NULL,
  edit_by INT NULL,
  last_login_time DATETIME NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE t_role (
  id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  role VARCHAR(32) NOT NULL UNIQUE,
  role_name VARCHAR(64) NOT NULL,
  enabled TINYINT(1) NOT NULL DEFAULT 1
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE t_permission (
  id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(64) NOT NULL,
  code VARCHAR(64) NOT NULL UNIQUE,
  url VARCHAR(255) NULL,
  type VARCHAR(16) NOT NULL,
  parent_id INT NULL,
  order_no INT NULL,
  icon VARCHAR(64) NULL,
  enabled TINYINT(1) NOT NULL DEFAULT 1
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE t_user_role (
  user_id INT NOT NULL,
  role_id INT NOT NULL,
  PRIMARY KEY(user_id,role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE t_role_permission (
  role_id INT NOT NULL,
  permission_id INT NOT NULL,
  PRIMARY KEY(role_id,permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE t_operation_log (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id INT NULL,
  user_name VARCHAR(255) NULL,
  action_code VARCHAR(32) NOT NULL,
  object_type VARCHAR(64) NULL,
  module_name VARCHAR(64) NULL,
  resource_id VARCHAR(64) NULL,
  result VARCHAR(16) NULL,
  detail VARCHAR(1000) NULL,
  ip VARCHAR(64) NULL,
  request_id VARCHAR(64) NULL,
  create_time DATETIME NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO t_user(id,login_act,login_pwd,name,phone,email,account_no_expired,credentials_no_expired,account_no_locked,account_enabled,create_time)
VALUES
  (1,'admin','legacy-admin-hash','恢复管理员','13800000001','admin@example.test',1,1,1,1,NOW()),
  (2,'disabled','legacy-disabled-hash','旧禁用用户','13800000002','disabled@example.test',1,1,1,0,NOW()),
  (3,'locked','legacy-locked-hash','旧锁定用户','13800000003','locked@example.test',1,1,0,1,NOW()),
  (4,'normal','legacy-normal-hash','旧正常用户','13800000004','normal@example.test',1,1,1,1,NOW());

INSERT INTO t_role(id,role,role_name,enabled) VALUES
  (1,'admin','管理员',1),(2,'sales_consultant','销售顾问',1);

INSERT INTO t_permission(id,name,code,url,type,parent_id,order_no,icon,enabled) VALUES
  (1,'用户管理','menu:user',NULL,'menu',NULL,1,NULL,1),
  (2,'用户列表','page:user:list','/dashboard/user','menu',1,1,NULL,1),
  (3,'用户查看','user:view',NULL,'button',2,1,NULL,1);

INSERT INTO t_user_role(user_id,role_id) VALUES (1,1),(2,2),(3,2),(4,2);
INSERT INTO t_role_permission(role_id,permission_id) VALUES (1,1),(1,2),(1,3),(2,3);
