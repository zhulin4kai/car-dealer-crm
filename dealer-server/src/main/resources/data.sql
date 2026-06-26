-- Test Data for H2 Database
-- Password for all users: 123456 (BCrypt hash)

-- ==================== Users ====================
MERGE INTO t_user (id, login_act, login_pwd, name, phone, email, account_no_expired, credentials_no_expired, account_no_locked, account_enabled, create_time, create_by, edit_time, edit_by, last_login_time) KEY(id)
VALUES (1, 'admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '管理员', '13700000000', 'admin@test.com', 1, 1, 1, 1, CURRENT_TIMESTAMP, NULL, NULL, NULL, CURRENT_TIMESTAMP);

MERGE INTO t_user (id, login_act, login_pwd, name, phone, email, account_no_expired, credentials_no_expired, account_no_locked, account_enabled, create_time, create_by, edit_time, edit_by, last_login_time) KEY(id)
VALUES (2, 'zhangsan', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '张三', '13800000001', 'zhangsan@test.com', 1, 1, 1, 1, CURRENT_TIMESTAMP, 1, NULL, NULL, NULL);

MERGE INTO t_user (id, login_act, login_pwd, name, phone, email, account_no_expired, credentials_no_expired, account_no_locked, account_enabled, create_time, create_by, edit_time, edit_by, last_login_time) KEY(id)
VALUES (3, 'lisi', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '李四', '13800000002', 'lisi@test.com', 1, 1, 1, 1, CURRENT_TIMESTAMP, 1, NULL, NULL, NULL);

-- ==================== Roles ====================
INSERT INTO `t_role` (`role`, `role_name`, `enabled`) VALUES
('admin', '系统管理员', 1),
('sales_consultant', '销售顾问', 1),
('sales_manager', '销售经理', 1),
('marketing_specialist', '市场专员', 1),
('finance_specialist', '财务专员', 1),
('inventory_specialist', '库存专员', 1);

-- ==================== User Roles ====================
INSERT INTO t_user_role (user_id, role_id)
SELECT u.id, r.id FROM t_user u CROSS JOIN t_role r WHERE u.login_act = 'admin' AND r.role = 'admin';
INSERT INTO t_user_role (user_id, role_id)
SELECT u.id, r.id FROM t_user u CROSS JOIN t_role r WHERE u.login_act = 'zhangsan' AND r.role = 'sales_consultant';
INSERT INTO t_user_role (user_id, role_id)
SELECT u.id, r.id FROM t_user u CROSS JOIN t_role r WHERE u.login_act = 'lisi' AND r.role = 'sales_manager';

-- ==================== Permissions ====================
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`) VALUES
('仪表盘', 'menu:dashboard', '/dashboard', 'menu', NULL, 0, 'Gauge', 1),
('市场活动', 'menu:activity', NULL, 'menu', NULL, 1, 'OfficeBuilding', 1),
('线索管理', 'menu:clue', NULL, 'menu', NULL, 2, 'Magnet', 1),
('客户管理', 'menu:customer', NULL, 'menu', NULL, 3, 'User', 1),
('商机管理', 'menu:opportunity', NULL, 'menu', NULL, 4, 'Target', 1),
('试驾管理', 'menu:test-drive', NULL, 'menu', NULL, 5, 'Car', 1),
('交易管理', 'menu:tran', NULL, 'menu', NULL, 6, 'Wallet', 1),
('报价订单', 'menu:quote', NULL, 'menu', NULL, 7, 'FileText', 1),
('交付管理', 'menu:delivery', NULL, 'menu', NULL, 8, 'Truck', 1),
('产品管理', 'menu:product', NULL, 'menu', NULL, 9, 'Memo', 1),
('字典管理', 'menu:dict', NULL, 'menu', NULL, 10, 'Grid', 1),
('用户管理', 'menu:user', NULL, 'menu', NULL, 11, 'Stamp', 1);

INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '市场活动', 'page:activity:list', '/dashboard/activity', 'menu', id, 1, 'CreditCard', 1 FROM `t_permission` WHERE code = 'menu:activity';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '线索管理', 'page:clue:list', '/dashboard/clue', 'menu', id, 1, 'Paperclip', 1 FROM `t_permission` WHERE code = 'menu:clue';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '客户管理', 'page:customer:list', '/dashboard/customer', 'menu', id, 1, 'UserFilled', 1 FROM `t_permission` WHERE code = 'menu:customer';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '商机管理', 'page:opportunity:list', '/dashboard/opportunity', 'menu', id, 1, 'Target', 1 FROM `t_permission` WHERE code = 'menu:opportunity';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '试驾管理', 'page:test-drive:list', '/dashboard/test-drive', 'menu', id, 1, 'Car', 1 FROM `t_permission` WHERE code = 'menu:test-drive';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '交易管理', 'page:tran:list', '/dashboard/tran', 'menu', id, 1, 'Coin', 1 FROM `t_permission` WHERE code = 'menu:tran';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '报价订单', 'page:quote:list', '/dashboard/quote', 'menu', id, 1, 'FileText', 1 FROM `t_permission` WHERE code = 'menu:quote';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '交付管理', 'page:delivery:list', '/dashboard/delivery', 'menu', id, 1, 'Truck', 1 FROM `t_permission` WHERE code = 'menu:delivery';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '车型商品', 'page:product:list', '/dashboard/product', 'menu', id, 1, 'SetUp', 1 FROM `t_permission` WHERE code = 'menu:product';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '商品分类', 'page:product:category', '/dashboard/product/category', 'menu', id, 2, 'ListTree', 1 FROM `t_permission` WHERE code = 'menu:product';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '促销管理', 'page:product:promotion', '/dashboard/product/promotion', 'menu', id, 3, 'BadgePercent', 1 FROM `t_permission` WHERE code = 'menu:product';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '库存预警', 'page:product:stock', '/dashboard/product/stock', 'menu', id, 4, 'Warehouse', 1 FROM `t_permission` WHERE code = 'menu:product';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '字典类型', 'page:dict:type', '/dashboard/dict/type', 'menu', id, 1, 'Postcard', 1 FROM `t_permission` WHERE code = 'menu:dict';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '字典数据', 'page:dict:value', '/dashboard/dict/value', 'menu', id, 2, 'DataAnalysis', 1 FROM `t_permission` WHERE code = 'menu:dict';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '用户管理', 'page:user:list', '/dashboard/user', 'menu', id, 1, 'UserCog', 1 FROM `t_permission` WHERE code = 'menu:user';

INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '市场活动-列表', 'activity:list', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'page:activity:list';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '市场活动-录入', 'activity:add', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'page:activity:list';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '市场活动-编辑', 'activity:edit', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'page:activity:list';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '市场活动-查看', 'activity:view', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'page:activity:list';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '市场活动-删除', 'activity:delete', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'page:activity:list';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '市场活动-复盘', 'activity:review', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'page:activity:list';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '市场活动-关闭', 'activity:close', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'page:activity:list';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '市场活动-导出', 'activity:export', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'page:activity:list';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '线索管理-列表', 'clue:list', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'page:clue:list';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '线索管理-录入', 'clue:add', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'page:clue:list';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '线索管理-编辑', 'clue:edit', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'page:clue:list';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '线索管理-查看', 'clue:view', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'page:clue:list';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '线索管理-删除', 'clue:delete', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'page:clue:list';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '线索管理-导入', 'clue:import', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'page:clue:list';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '线索管理-转派', 'clue:transfer', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'page:clue:list';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '线索管理-关闭', 'clue:close', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'page:clue:list';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '线索管理-恢复', 'clue:restore', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'page:clue:list';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '客户管理-列表', 'customer:list', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'page:customer:list';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '客户管理-查看', 'customer:view', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'page:customer:list';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '客户管理-导出', 'customer:export', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'page:customer:list';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '客户管理-转客户', 'customer:transfer', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'page:customer:list';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '客户管理-合并', 'customer:merge', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'page:customer:list';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '客户管理-删除', 'customer:delete', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'page:customer:list';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '客户管理-敏感字段查看', 'customer:sensitive:view', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'page:customer:list';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '商机管理-列表', 'opportunity:list', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'page:opportunity:list';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '商机管理-查看', 'opportunity:view', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'page:opportunity:list';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '商机管理-创建', 'opportunity:create', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'page:opportunity:list';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '商机管理-编辑', 'opportunity:edit', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'page:opportunity:list';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '商机管理-推进', 'opportunity:advance', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'page:opportunity:list';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '商机管理-赢单', 'opportunity:win', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'page:opportunity:list';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '商机管理-输单', 'opportunity:lose', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'page:opportunity:list';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '商机管理-搁置', 'opportunity:shelve', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'page:opportunity:list';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '商机管理-恢复', 'opportunity:restore', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'page:opportunity:list';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '试驾管理-列表', 'test-drive:list', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'page:test-drive:list';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '试驾管理-查看', 'test-drive:view', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'page:test-drive:list';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '试驾管理-预约', 'test-drive:create', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'page:test-drive:list';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '试驾管理-改期', 'test-drive:reschedule', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'page:test-drive:list';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '试驾管理-取消', 'test-drive:cancel', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'page:test-drive:list';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '试驾管理-签到', 'test-drive:check-in', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'page:test-drive:list';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '试驾管理-完成', 'test-drive:complete', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'page:test-drive:list';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '交易管理-列表', 'tran:list', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'page:tran:list';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '交易管理-查看', 'tran:view', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'page:tran:list';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '交易管理-创建', 'tran:create', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'page:tran:list';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '交易管理-编辑', 'tran:edit', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'page:tran:list';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '交易管理-删除', 'tran:delete', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'page:tran:list';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '交易管理-取消', 'tran:cancel', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'page:tran:list';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '交易管理-关闭', 'tran:close', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'page:tran:list';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '交易管理-结算', 'tran:settle', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'page:tran:list';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '交易管理-重新提交', 'tran:resubmit', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'page:tran:list';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '交易管理-审批', 'tran:approve', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'page:tran:list';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '交易管理-发票', 'tran:invoice', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'page:tran:list';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '交易管理-发票敏感信息查看', 'tran:invoice:sensitive', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'page:tran:list';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '交易管理-收款', 'tran:payment', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'page:tran:list';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '交易管理-收款确认', 'tran:payment:confirm', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'page:tran:list';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '交易管理-退款', 'tran:refund', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'page:tran:list';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '交易管理-退款审批', 'tran:refund:approve', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'page:tran:list';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '交易管理-退款执行', 'tran:refund:execute', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'page:tran:list';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '报价订单-列表', 'quote:list', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'page:quote:list';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '报价订单-查看', 'quote:view', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'page:quote:list';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '报价订单-创建', 'quote:create', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'page:quote:list';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '报价订单-编辑', 'quote:edit', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'page:quote:list';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '报价订单-审批', 'quote:approve', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'page:quote:list';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '报价订单-客户确认', 'quote:confirm', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'page:quote:list';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '报价订单-成单', 'quote:order', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'page:quote:list';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '报价订单-取消', 'quote:cancel', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'page:quote:list';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '交付管理-列表', 'delivery:list', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'page:delivery:list';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '交付管理-查看', 'delivery:view', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'page:delivery:list';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '交付管理-创建', 'delivery:create', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'page:delivery:list';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '交付管理-准备项', 'delivery:check', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'page:delivery:list';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '交付管理-签收', 'delivery:sign', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'page:delivery:list';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '交付管理-异常', 'delivery:exception', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'page:delivery:list';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '交付管理-取消', 'delivery:cancel', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'page:delivery:list';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '车型商品-列表', 'product:list', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'page:product:list';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '车型商品-查看', 'product:view', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'page:product:list';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '车型商品-录入', 'product:add', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'page:product:list';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '车型商品-编辑', 'product:edit', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'page:product:list';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '车型商品-删除', 'product:delete', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'page:product:list';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '商品分类-列表', 'product:category:list', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'page:product:category';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '商品分类-查看', 'product:category:view', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'page:product:category';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '商品分类-录入', 'product:category:add', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'page:product:category';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '商品分类-编辑', 'product:category:edit', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'page:product:category';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '商品分类-删除', 'product:category:delete', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'page:product:category';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '促销管理-列表', 'product:promotion:list', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'page:product:promotion';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '促销管理-查看', 'product:promotion:view', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'page:product:promotion';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '促销管理-录入', 'product:promotion:add', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'page:product:promotion';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '促销管理-编辑', 'product:promotion:edit', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'page:product:promotion';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '促销管理-启停作废', 'product:promotion:status', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'page:product:promotion';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '促销管理-删除', 'product:promotion:delete', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'page:product:promotion';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '库存管理-查看', 'product:stock:view', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'page:product:stock';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '库存管理-调整', 'product:stock:adjust', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'page:product:stock';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '字典类型-列表', 'dict:type:list', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'page:dict:type';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '字典类型-查看', 'dict:type:view', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'page:dict:type';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '字典类型-录入', 'dict:type:add', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'page:dict:type';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '字典类型-编辑', 'dict:type:edit', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'page:dict:type';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '字典类型-删除', 'dict:type:delete', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'page:dict:type';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '字典数据-列表', 'dict:value:list', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'page:dict:value';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '字典数据-查看', 'dict:value:view', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'page:dict:value';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '字典数据-录入', 'dict:value:add', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'page:dict:value';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '字典数据-编辑', 'dict:value:edit', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'page:dict:value';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '字典数据-删除', 'dict:value:delete', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'page:dict:value';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '字典缓存-刷新', 'dict:cache:refresh', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'page:dict:type';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '用户管理-列表', 'user:list', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'page:user:list';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '用户管理-查看', 'user:view', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'page:user:list';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '用户管理-录入', 'user:add', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'page:user:list';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '用户管理-编辑', 'user:edit', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'page:user:list';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '用户管理-删除', 'user:delete', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'page:user:list';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '用户管理-状态', 'user:status', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'page:user:list';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '用户管理-角色分配', 'user:role', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'page:user:list';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '用户管理-密码重置', 'user:password', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'page:user:list';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '统计报表-查看', 'statistic:view', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'menu:dashboard';

INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
VALUES ('跟进任务', 'menu:follow', NULL, 'menu', NULL, 6, 'CalendarCheck', 1);
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '我的跟进', 'page:follow:list', '/dashboard/follow', 'menu', id, 1, 'CalendarCheck', 1 FROM `t_permission` WHERE code = 'menu:follow';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '跟进任务-列表', 'follow-task:list', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'page:follow:list';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '跟进任务-查看', 'follow-task:view', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'page:follow:list';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '跟进任务-创建', 'follow-task:create', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'page:follow:list';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '跟进任务-处理', 'follow-task:update', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'page:follow:list';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '跟进任务-取消', 'follow-task:cancel', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'page:follow:list';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '跟进任务-完成', 'follow-task:complete', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'page:follow:list';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '沟通记录-列表', 'communication-record:list', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'page:follow:list';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '沟通记录-新增', 'communication-record:create', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'page:follow:list';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '沟通记录-更正', 'communication-record:correct', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'page:follow:list';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '沟通记录-作废', 'communication-record:void', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'page:follow:list';

-- ==================== Role Permissions ====================
-- 管理员显式拥有当前全部已启用权限；后续新增权限时必须同步补充管理员映射。
INSERT INTO `t_role_permission` (`role_id`, `permission_id`)
SELECT r.id, p.id FROM `t_role` r CROSS JOIN `t_permission` p
WHERE r.role = 'admin' AND r.enabled = 1 AND p.enabled = 1;

INSERT INTO `t_role_permission` (`role_id`, `permission_id`)
SELECT r.id, p.id FROM `t_role` r CROSS JOIN `t_permission` p
WHERE r.role = 'sales_consultant' AND p.code IN ('menu:activity', 'page:activity:list', 'activity:list', 'activity:view', 'menu:clue', 'page:clue:list', 'clue:list', 'clue:view', 'clue:add', 'clue:edit', 'menu:customer', 'page:customer:list', 'customer:list', 'customer:view', 'customer:transfer', 'menu:opportunity', 'page:opportunity:list', 'opportunity:list', 'opportunity:view', 'opportunity:create', 'opportunity:edit', 'opportunity:advance', 'opportunity:lose', 'opportunity:shelve', 'menu:test-drive', 'page:test-drive:list', 'test-drive:list', 'test-drive:view', 'test-drive:create', 'test-drive:reschedule', 'test-drive:cancel', 'test-drive:check-in', 'test-drive:complete', 'menu:tran', 'page:tran:list', 'tran:list', 'tran:view', 'tran:create', 'tran:edit', 'tran:settle', 'tran:resubmit', 'menu:quote', 'page:quote:list', 'quote:list', 'quote:view', 'quote:create', 'quote:edit', 'quote:confirm', 'menu:delivery', 'page:delivery:list', 'delivery:list', 'delivery:view', 'delivery:create', 'delivery:check', 'delivery:sign', 'delivery:exception');

INSERT INTO `t_role_permission` (`role_id`, `permission_id`)
SELECT r.id, p.id FROM `t_role` r CROSS JOIN `t_permission` p
WHERE r.role = 'sales_manager' AND p.code IN ('menu:dashboard', 'menu:activity', 'page:activity:list', 'activity:list', 'activity:view', 'activity:add', 'activity:edit', 'activity:delete', 'activity:review', 'activity:close', 'activity:export', 'menu:clue', 'page:clue:list', 'clue:list', 'clue:view', 'clue:add', 'clue:edit', 'clue:delete', 'clue:import', 'clue:transfer', 'clue:close', 'clue:restore', 'menu:customer', 'page:customer:list', 'customer:list', 'customer:view', 'customer:transfer', 'customer:export', 'customer:merge', 'customer:delete', 'customer:sensitive:view', 'menu:opportunity', 'page:opportunity:list', 'opportunity:list', 'opportunity:view', 'opportunity:create', 'opportunity:edit', 'opportunity:advance', 'opportunity:win', 'opportunity:lose', 'opportunity:shelve', 'opportunity:restore', 'menu:test-drive', 'page:test-drive:list', 'test-drive:list', 'test-drive:view', 'test-drive:create', 'test-drive:reschedule', 'test-drive:cancel', 'test-drive:check-in', 'test-drive:complete', 'menu:tran', 'page:tran:list', 'tran:list', 'tran:view', 'tran:create', 'tran:edit', 'tran:delete', 'tran:cancel', 'tran:close', 'tran:settle', 'tran:resubmit', 'tran:approve', 'menu:quote', 'page:quote:list', 'quote:list', 'quote:view', 'quote:create', 'quote:edit', 'quote:approve', 'quote:confirm', 'quote:order', 'quote:cancel', 'menu:delivery', 'page:delivery:list', 'delivery:list', 'delivery:view', 'delivery:create', 'delivery:check', 'delivery:sign', 'delivery:exception', 'delivery:cancel', 'statistic:view');

INSERT INTO `t_role_permission` (`role_id`, `permission_id`)
SELECT r.id, p.id FROM `t_role` r CROSS JOIN `t_permission` p
WHERE r.role = 'marketing_specialist' AND p.code IN ('menu:dashboard', 'menu:activity', 'page:activity:list', 'activity:list', 'activity:view', 'activity:add', 'activity:edit', 'activity:delete', 'activity:review', 'activity:close', 'activity:export', 'menu:clue', 'page:clue:list', 'clue:list', 'clue:view', 'clue:add', 'clue:edit', 'clue:import', 'statistic:view');

INSERT INTO `t_role_permission` (`role_id`, `permission_id`)
SELECT r.id, p.id FROM `t_role` r CROSS JOIN `t_permission` p
WHERE r.role = 'finance_specialist' AND p.code IN ('menu:dashboard', 'menu:tran', 'page:tran:list', 'tran:list', 'tran:view', 'tran:invoice', 'tran:invoice:sensitive', 'tran:payment', 'tran:payment:confirm', 'tran:refund', 'tran:refund:approve', 'tran:refund:execute', 'menu:delivery', 'page:delivery:list', 'delivery:list', 'delivery:view', 'delivery:check', 'statistic:view');

INSERT INTO `t_role_permission` (`role_id`, `permission_id`)
SELECT r.id, p.id FROM `t_role` r CROSS JOIN `t_permission` p
WHERE r.role = 'inventory_specialist' AND p.code IN ('menu:product', 'page:product:list', 'page:product:category', 'page:product:promotion', 'page:product:stock', 'product:list', 'product:view', 'product:add', 'product:edit', 'product:delete', 'product:category:list', 'product:category:view', 'product:category:add', 'product:category:edit', 'product:category:delete', 'product:promotion:list', 'product:promotion:view', 'product:promotion:add', 'product:promotion:edit', 'product:promotion:status', 'product:promotion:delete', 'product:stock:view', 'product:stock:adjust', 'menu:delivery', 'page:delivery:list', 'delivery:list', 'delivery:view', 'delivery:check', 'delivery:sign', 'delivery:exception');

INSERT INTO `t_role_permission` (`role_id`, `permission_id`)
SELECT r.id, p.id FROM `t_role` r CROSS JOIN `t_permission` p
WHERE r.role IN ('sales_consultant', 'sales_manager', 'marketing_specialist')
  AND p.code IN ('menu:follow', 'page:follow:list', 'follow-task:list', 'follow-task:view',
                 'follow-task:create', 'follow-task:update', 'follow-task:cancel',
                 'follow-task:complete', 'communication-record:list',
                 'communication-record:create', 'communication-record:correct',
                 'communication-record:void');

-- ==================== Dictionary Types ====================
MERGE INTO t_dic_type (id, type_code, type_name, remark) KEY(id) VALUES (1, 'sex', '性别', NULL);
MERGE INTO t_dic_type (id, type_code, type_name, remark) KEY(id) VALUES (2, 'appellation', '称呼', NULL);
MERGE INTO t_dic_type (id, type_code, type_name, remark) KEY(id) VALUES (3, 'clueState', '线索状态', NULL);
MERGE INTO t_dic_type (id, type_code, type_name, remark) KEY(id) VALUES (4, 'returnPriority', '回访优先级', NULL);
MERGE INTO t_dic_type (id, type_code, type_name, remark) KEY(id) VALUES (5, 'returnState', '回访状态', NULL);
MERGE INTO t_dic_type (id, type_code, type_name, remark) KEY(id) VALUES (6, 'source', '来源', NULL);
MERGE INTO t_dic_type (id, type_code, type_name, remark) KEY(id) VALUES (7, 'stage', '阶段', NULL);
MERGE INTO t_dic_type (id, type_code, type_name, remark) KEY(id) VALUES (8, 'transactionType', '交易类型', NULL);
MERGE INTO t_dic_type (id, type_code, type_name, remark) KEY(id) VALUES (9, 'intentionState', '意向状态', NULL);
MERGE INTO t_dic_type (id, type_code, type_name, remark) KEY(id) VALUES (10, 'needLoan', '是否贷款', NULL);
MERGE INTO t_dic_type (id, type_code, type_name, remark) KEY(id) VALUES (11, 'noteWay', '跟踪方式', NULL);
MERGE INTO t_dic_type (id, type_code, type_name, remark) KEY(id) VALUES (12, 'userState', '用户状态', NULL);
MERGE INTO t_dic_type (id, type_code, type_name, remark) KEY(id) VALUES (13, 'educational', '学历', NULL);
UPDATE t_dic_type
SET built_in = 1
WHERE type_code IN ('sex', 'appellation', 'clueState', 'returnPriority', 'returnState',
                    'source', 'stage', 'transactionType', 'intentionState',
                    'needLoan', 'noteWay', 'userState', 'educational');

-- ==================== Dictionary Values ====================
-- Sex
MERGE INTO t_dic_value (id, type_code, type_value, value_code, `order`, remark) KEY(id) VALUES (1, 'sex', '男', 'male', 1, NULL);
MERGE INTO t_dic_value (id, type_code, type_value, value_code, `order`, remark) KEY(id) VALUES (2, 'sex', '女', 'female', 2, NULL);

-- Appellation
MERGE INTO t_dic_value (id, type_code, type_value, value_code, `order`, remark) KEY(id) VALUES (3, 'appellation', '先生', 'mr', 1, NULL);
MERGE INTO t_dic_value (id, type_code, type_value, value_code, `order`, remark) KEY(id) VALUES (4, 'appellation', '女士', 'ms', 2, NULL);
MERGE INTO t_dic_value (id, type_code, type_value, value_code, `order`, remark) KEY(id) VALUES (5, 'appellation', '教授', 'professor', 3, NULL);

-- Clue State
MERGE INTO t_dic_value (id, type_code, type_value, value_code, `order`, remark) KEY(id) VALUES (6, 'clueState', '试图联系', 'attempt_contact', 1, NULL);
MERGE INTO t_dic_value (id, type_code, type_value, value_code, `order`, remark) KEY(id) VALUES (7, 'clueState', '将来联系', 'future_contact', 2, NULL);
MERGE INTO t_dic_value (id, type_code, type_value, value_code, `order`, remark) KEY(id) VALUES (8, 'clueState', '已联系', 'contacted', 3, NULL);
MERGE INTO t_dic_value (id, type_code, type_value, value_code, `order`, remark) KEY(id) VALUES (9, 'clueState', '已转客户', 'converted', 0, NULL);
MERGE INTO t_dic_value (id, type_code, type_value, value_code, `order`, remark) KEY(id) VALUES (10, 'clueState', '虚假线索', 'fake', 4, NULL);
MERGE INTO t_dic_value (id, type_code, type_value, value_code, `order`, remark) KEY(id) VALUES (11, 'clueState', '丢失线索', 'lost', 5, NULL);
MERGE INTO t_dic_value (id, type_code, type_value, value_code, `order`, remark) KEY(id) VALUES (68, 'clueState', '关闭', 'closed', 8, NULL);

-- Source
MERGE INTO t_dic_value (id, type_code, type_value, value_code, `order`, remark) KEY(id) VALUES (12, 'source', '网络广告', 'online_ad', 1, NULL);
MERGE INTO t_dic_value (id, type_code, type_value, value_code, `order`, remark) KEY(id) VALUES (13, 'source', '懂车帝', 'dongchedi', 2, NULL);
MERGE INTO t_dic_value (id, type_code, type_value, value_code, `order`, remark) KEY(id) VALUES (14, 'source', '员工介绍', 'employee_referral', 3, NULL);
MERGE INTO t_dic_value (id, type_code, type_value, value_code, `order`, remark) KEY(id) VALUES (15, 'source', '门店参观', 'store_visit', 4, NULL);
MERGE INTO t_dic_value (id, type_code, type_value, value_code, `order`, remark) KEY(id) VALUES (16, 'source', '官方网站', 'official_website', 5, NULL);

-- Intention State
MERGE INTO t_dic_value (id, type_code, type_value, value_code, `order`, remark) KEY(id) VALUES (17, 'intentionState', '有意向', 'interested', 1, NULL);
MERGE INTO t_dic_value (id, type_code, type_value, value_code, `order`, remark) KEY(id) VALUES (18, 'intentionState', '无意向', 'not_interested', 2, NULL);
MERGE INTO t_dic_value (id, type_code, type_value, value_code, `order`, remark) KEY(id) VALUES (19, 'intentionState', '意向不明', 'unknown', 3, NULL);

-- Need Loan
MERGE INTO t_dic_value (id, type_code, type_value, value_code, `order`, remark) KEY(id) VALUES (20, 'needLoan', '需要', 'required', 1, NULL);
MERGE INTO t_dic_value (id, type_code, type_value, value_code, `order`, remark) KEY(id) VALUES (21, 'needLoan', '不需要', 'not_required', 2, NULL);

-- Stage
MERGE INTO t_dic_value (id, type_code, type_value, value_code, `order`, remark) KEY(id) VALUES (22, 'stage', 'QUOTATION', 'quotation', 1, '待报价');
MERGE INTO t_dic_value (id, type_code, type_value, value_code, `order`, remark) KEY(id) VALUES (23, 'stage', 'PENDING', 'pending', 2, '待审批');
MERGE INTO t_dic_value (id, type_code, type_value, value_code, `order`, remark) KEY(id) VALUES (24, 'stage', 'APPROVED', 'approved', 3, '已审批');
MERGE INTO t_dic_value (id, type_code, type_value, value_code, `order`, remark) KEY(id) VALUES (25, 'stage', 'PAYMENT', 'payment', 4, '待收款');
MERGE INTO t_dic_value (id, type_code, type_value, value_code, `order`, remark) KEY(id) VALUES (26, 'stage', 'COMPLETED', 'completed', 5, '已完成');
MERGE INTO t_dic_value (id, type_code, type_value, value_code, `order`, remark) KEY(id) VALUES (27, 'stage', 'LOST', 'lost', 6, '丢失关闭');
MERGE INTO t_dic_value (id, type_code, type_value, value_code, `order`, remark) KEY(id) VALUES (66, 'stage', 'CANCELLED', 'cancelled', 7, '已取消');

-- Note Way
MERGE INTO t_dic_value (id, type_code, type_value, value_code, `order`, remark) KEY(id) VALUES (28, 'noteWay', '电话', 'phone', 1, NULL);
MERGE INTO t_dic_value (id, type_code, type_value, value_code, `order`, remark) KEY(id) VALUES (29, 'noteWay', '微信', 'wechat', 2, NULL);
MERGE INTO t_dic_value (id, type_code, type_value, value_code, `order`, remark) KEY(id) VALUES (30, 'noteWay', 'QQ', 'qq', 3, NULL);
MERGE INTO t_dic_value (id, type_code, type_value, value_code, `order`, remark) KEY(id) VALUES (31, 'noteWay', '面聊', 'in_person', 4, NULL);
MERGE INTO t_dic_value (id, type_code, type_value, value_code, `order`, remark) KEY(id) VALUES (32, 'noteWay', '其他', 'other', 5, NULL);

-- User State
MERGE INTO t_dic_value (id, type_code, type_value, value_code, `order`, remark) KEY(id) VALUES (33, 'userState', '正常', 'normal', 1, NULL);
MERGE INTO t_dic_value (id, type_code, type_value, value_code, `order`, remark) KEY(id) VALUES (34, 'userState', '锁定', 'locked', 2, NULL);
MERGE INTO t_dic_value (id, type_code, type_value, value_code, `order`, remark) KEY(id) VALUES (35, 'userState', '禁用', 'disabled', 3, NULL);

-- Return Priority
MERGE INTO t_dic_value (id, type_code, type_value, value_code, `order`, remark) KEY(id) VALUES (36, 'returnPriority', '最高', 'highest', 1, NULL);
MERGE INTO t_dic_value (id, type_code, type_value, value_code, `order`, remark) KEY(id) VALUES (37, 'returnPriority', '高', 'high', 2, NULL);
MERGE INTO t_dic_value (id, type_code, type_value, value_code, `order`, remark) KEY(id) VALUES (38, 'returnPriority', '常规', 'normal', 3, NULL);

-- Return State
MERGE INTO t_dic_value (id, type_code, type_value, value_code, `order`, remark) KEY(id) VALUES (39, 'returnState', '未启动', 'not_started', 1, NULL);
MERGE INTO t_dic_value (id, type_code, type_value, value_code, `order`, remark) KEY(id) VALUES (40, 'returnState', '进行中', 'in_progress', 2, NULL);
MERGE INTO t_dic_value (id, type_code, type_value, value_code, `order`, remark) KEY(id) VALUES (41, 'returnState', '完成', 'completed', 3, NULL);

-- Educational
MERGE INTO t_dic_value (id, type_code, type_value, value_code, `order`, remark) KEY(id) VALUES (42, 'educational', '大学', 'university', 1, NULL);
MERGE INTO t_dic_value (id, type_code, type_value, value_code, `order`, remark) KEY(id) VALUES (43, 'educational', '研究生', 'postgraduate', 2, NULL);
UPDATE t_dic_value
SET built_in = 1
WHERE type_code IN ('sex', 'appellation', 'clueState', 'returnPriority', 'returnState',
                    'source', 'stage', 'transactionType', 'intentionState',
                    'needLoan', 'noteWay', 'userState', 'educational');

-- ==================== Product Categories ====================
MERGE INTO t_product_category (id, name, code, description, sort, status, create_time, update_time) KEY(id)
VALUES (1, 'SUV', 'SUV', '运动型多用途汽车', 1, '启用', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

MERGE INTO t_product_category (id, name, code, description, sort, status, create_time, update_time) KEY(id)
VALUES (2, '轿车', 'SEDAN', '三厢式乘用车', 2, '启用', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

MERGE INTO t_product_category (id, name, code, description, sort, status, create_time, update_time) KEY(id)
VALUES (3, '电动轿车', 'ELECTRIC_SEDAN', '纯电动驱动的三厢式乘用车', 3, '启用', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- ==================== Products ====================
MERGE INTO t_product (id, sku, name, category_id, specification, price, stock, min_stock, status, create_time, update_time) KEY(id)
VALUES (1, 'BMW-X5-2023', '宝马 X5', 1, '2023款 xDrive40i 尊享型', 569800.00, 15, 5, 'ON_SALE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

MERGE INTO t_product (id, sku, name, category_id, specification, price, stock, min_stock, status, create_time, update_time) KEY(id)
VALUES (2, 'BENZ-E-2023', '奔驰 E级', 2, '2023款 E 300 L 运动豪华型', 499800.00, 15, 6, 'ON_SALE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

MERGE INTO t_product (id, sku, name, category_id, specification, price, stock, min_stock, status, create_time, update_time) KEY(id)
VALUES (3, 'AUDI-Q5-2023', '奥迪 Q5L', 1, '2023款 40 TFSI 荣享时尚型', 399800.00, 16, 6, 'ON_SALE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

MERGE INTO t_product (id, sku, name, category_id, specification, price, stock, min_stock, status, create_time, update_time) KEY(id)
VALUES (4, 'BMW-5-2023', '宝马 5系', 2, '2023款 530Li 行政型', 459800.00, 18, 7, 'ON_SALE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

MERGE INTO t_product (id, sku, name, category_id, specification, price, stock, min_stock, status, create_time, update_time) KEY(id)
VALUES (5, 'TESLA-MODEL3-2023', '特斯拉 Model 3', 3, '2023款 后轮驱动版', 259900.00, 30, 15, 'ON_SALE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- ==================== Activities ====================
MERGE INTO t_activity (id, owner_id, name, start_time, end_time, cost, description, create_time, create_by, edit_time, edit_by) KEY(id)
VALUES (1, 1, '春季汽车展销会', '2025-03-15 09:00:00', '2025-03-17 18:00:00', 50000.00, '春季大型汽车展销活动', CURRENT_TIMESTAMP, 1, NULL, NULL);

MERGE INTO t_activity (id, owner_id, name, start_time, end_time, cost, description, create_time, create_by, edit_time, edit_by) KEY(id)
VALUES (2, 2, '豪华车试驾体验', '2025-04-01 10:00:00', '2025-04-30 17:00:00', 30000.00, '提供豪华车型免费试驾服务', CURRENT_TIMESTAMP, 2, NULL, NULL);

MERGE INTO t_activity (id, owner_id, name, start_time, end_time, cost, description, create_time, create_by, edit_time, edit_by) KEY(id)
VALUES (3, 1, '夏季购车优惠节', '2025-06-01 09:00:00', '2025-06-30 20:00:00', 80000.00, '夏季购车大优惠', CURRENT_TIMESTAMP, 1, NULL, NULL);

-- ==================== Activity Remarks ====================
MERGE INTO t_activity_remark (id, activity_id, note_content, create_time, create_by, edit_time, edit_by, deleted) KEY(id)
VALUES (1, 1, '活动场地已确认', CURRENT_TIMESTAMP, 1, NULL, NULL, 0);

MERGE INTO t_activity_remark (id, activity_id, note_content, create_time, create_by, edit_time, edit_by, deleted) KEY(id)
VALUES (2, 1, '已联系15家汽车品牌参展', CURRENT_TIMESTAMP, 1, NULL, NULL, 0);

MERGE INTO t_activity_remark (id, activity_id, note_content, create_time, create_by, edit_time, edit_by, deleted) KEY(id)
VALUES (3, 2, '试驾路线规划完成', CURRENT_TIMESTAMP, 2, NULL, NULL, 0);

-- ==================== Clues ====================
MERGE INTO t_clue (id, owner_id, activity_id, full_name, appellation, phone, weixin, qq, email, age, job, year_income, address, need_loan, intention_state, intention_product, state, source, description, next_contact_time, create_time, create_by, edit_time, edit_by) KEY(id)
VALUES (1, 1, 1, '王杰', 3, '13700001001', 'wx_wangjie', '123456789', 'wangjie@test.com', 32, '软件工程师', 300000.00, '北京市朝阳区建国路88号', 20, 17, 1, 8, 12, '对宝马X5感兴趣', '2025-06-25 10:00:00', CURRENT_TIMESTAMP, 1, NULL, NULL);

MERGE INTO t_clue (id, owner_id, activity_id, full_name, appellation, phone, weixin, qq, email, age, job, year_income, address, need_loan, intention_state, intention_product, state, source, description, next_contact_time, create_time, create_by, edit_time, edit_by) KEY(id)
VALUES (2, 2, 2, '李娜', 4, '13800001002', 'wx_lina', '987654321', 'lina@test.com', 28, '金融分析师', 400000.00, '上海市浦东新区陆家嘴金融中心', 21, 17, 2, 6, 13, '想购买豪华轿车', '2025-06-27 15:30:00', CURRENT_TIMESTAMP, 2, NULL, NULL);

MERGE INTO t_clue (id, owner_id, activity_id, full_name, appellation, phone, weixin, qq, email, age, job, year_income, address, need_loan, intention_state, intention_product, state, source, description, next_contact_time, create_time, create_by, edit_time, edit_by) KEY(id)
VALUES (3, 3, 3, '张伟', 3, '13900001003', 'wx_zhangwei', '456789123', 'zhangwei@test.com', 35, '企业高管', 600000.00, '广州市天河区珠江新城', 20, 18, 3, 6, 14, '想要购买奔驰E级作为商务用车', '2025-06-23 09:15:00', CURRENT_TIMESTAMP, 1, NULL, NULL);

-- ==================== Clue Remarks ====================
MERGE INTO t_clue_remark (id, clue_id, note_way, note_content, create_time, create_by, edit_time, edit_by, deleted) KEY(id)
VALUES (1, 1, 28, '首次电话联系，客户表示对宝马X5很感兴趣', CURRENT_TIMESTAMP, 1, NULL, NULL, 0);

MERGE INTO t_clue_remark (id, clue_id, note_way, note_content, create_time, create_by, edit_time, edit_by, deleted) KEY(id)
VALUES (2, 1, 29, '通过微信发送了宝马X5的产品资料和价格表', CURRENT_TIMESTAMP, 1, NULL, NULL, 0);

MERGE INTO t_clue_remark (id, clue_id, note_way, note_content, create_time, create_by, edit_time, edit_by, deleted) KEY(id)
VALUES (3, 2, 28, '电话联系，客户工作繁忙，约定下周面谈', CURRENT_TIMESTAMP, 2, NULL, NULL, 0);

-- ==================== Customers ====================
MERGE INTO t_customer (id, clue_id, product, description, next_contact_time, create_time, create_by, edit_time, edit_by) KEY(id)
VALUES (1, 1, 1, '软件工程师，购买宝马X5，已签约成功', '2025-06-25 10:00:00', CURRENT_TIMESTAMP, 1, NULL, NULL);

MERGE INTO t_customer (id, clue_id, product, description, next_contact_time, create_time, create_by, edit_time, edit_by) KEY(id)
VALUES (2, 2, 2, '金融分析师，购买奔驰E级，已签约', '2025-06-30 14:00:00', CURRENT_TIMESTAMP, 2, NULL, NULL);

UPDATE t_customer
SET owner_id = (SELECT owner_id FROM t_clue WHERE t_clue.id = t_customer.clue_id),
    activity_id = (SELECT activity_id FROM t_clue WHERE t_clue.id = t_customer.clue_id),
    customer_name = (SELECT full_name FROM t_clue WHERE t_clue.id = t_customer.clue_id),
    appellation = (SELECT appellation FROM t_clue WHERE t_clue.id = t_customer.clue_id),
    phone = (SELECT phone FROM t_clue WHERE t_clue.id = t_customer.clue_id),
    weixin = (SELECT weixin FROM t_clue WHERE t_clue.id = t_customer.clue_id),
    qq = (SELECT qq FROM t_clue WHERE t_clue.id = t_customer.clue_id),
    email = (SELECT email FROM t_clue WHERE t_clue.id = t_customer.clue_id),
    age = (SELECT age FROM t_clue WHERE t_clue.id = t_customer.clue_id),
    job = (SELECT job FROM t_clue WHERE t_clue.id = t_customer.clue_id),
    year_income = (SELECT year_income FROM t_clue WHERE t_clue.id = t_customer.clue_id),
    address = (SELECT address FROM t_clue WHERE t_clue.id = t_customer.clue_id),
    need_loan = (SELECT need_loan FROM t_clue WHERE t_clue.id = t_customer.clue_id),
    intention_state = (SELECT intention_state FROM t_clue WHERE t_clue.id = t_customer.clue_id),
    source = (SELECT source FROM t_clue WHERE t_clue.id = t_customer.clue_id),
    original_clue_source = (SELECT source FROM t_clue WHERE t_clue.id = t_customer.clue_id),
    customer_status = 'INTENTION'
WHERE clue_id IS NOT NULL;

-- ==================== Customer Remarks ====================
MERGE INTO t_customer_remark (id, customer_id, note_way, note_content, create_by, create_time, edit_time, edit_by, deleted) KEY(id)
VALUES (1, 1, 28, '客户签约成功，选择宝马X5全款购车', 1, CURRENT_TIMESTAMP, NULL, NULL, 0);

MERGE INTO t_customer_remark (id, customer_id, note_way, note_content, create_by, create_time, edit_time, edit_by, deleted) KEY(id)
VALUES (2, 2, 31, '客户到店签约，选择奔驰E级', 2, CURRENT_TIMESTAMP, NULL, NULL, 0);

-- ==================== Transactions ====================
MERGE INTO t_tran (id, tran_no, customer_id, money, expected_date, stage, description, next_contact_time, create_time, create_by, edit_time, edit_by) KEY(id)
VALUES (1, 'T2025061800001', 1, 569800.00, '2025-07-15 00:00:00', 'QUOTATION', '宝马X5交易，待报价', '2025-06-25 10:00:00', CURRENT_TIMESTAMP, 1, NULL, NULL);

MERGE INTO t_tran (id, tran_no, customer_id, money, expected_date, stage, description, next_contact_time, create_time, create_by, edit_time, edit_by) KEY(id)
VALUES (2, 'T2025061800002', 2, 499800.00, '2025-08-01 00:00:00', 'PAYMENT', '奔驰E级交易，待收款', '2025-06-28 14:00:00', CURRENT_TIMESTAMP, 2, NULL, NULL);

MERGE INTO t_tran (id, tran_no, customer_id, money, expected_date, stage, description, next_contact_time, create_time, create_by, edit_time, edit_by) KEY(id)
VALUES (3, 'T2025061800003', 1, 399800.00, '2025-07-10 00:00:00', 'COMPLETED', '奥迪Q5L交易，已完成', NULL, CURRENT_TIMESTAMP, 1, NULL, NULL);

-- ==================== Transaction History ====================
MERGE INTO t_tran_history (id, tran_id, stage, money, expected_date, create_time, create_by) KEY(id)
VALUES (1, 1, 'QUOTATION', 569800.00, '2025-07-15 00:00:00', CURRENT_TIMESTAMP, 1);

MERGE INTO t_tran_history (id, tran_id, stage, money, expected_date, create_time, create_by) KEY(id)
VALUES (2, 2, 'QUOTATION', 499800.00, '2025-08-01 00:00:00', CURRENT_TIMESTAMP, 2);

MERGE INTO t_tran_history (id, tran_id, stage, money, expected_date, create_time, create_by) KEY(id)
VALUES (3, 2, 'PAYMENT', 499800.00, '2025-08-01 00:00:00', CURRENT_TIMESTAMP, 2);

-- ==================== Transaction Products ====================
MERGE INTO t_tran_product (id, tran_id, product_id, quantity, price, product_sku, product_name, product_specification, guide_price, create_time, create_by) KEY(id)
VALUES (1, 1, 1, 1, 569800.00, 'BMW-X5-2023', '宝马 X5', '2023款 xDrive40i 尊享型', 569800.00, CURRENT_TIMESTAMP, 1);

MERGE INTO t_tran_product (id, tran_id, product_id, quantity, price, product_sku, product_name, product_specification, guide_price, create_time, create_by) KEY(id)
VALUES (2, 2, 2, 1, 499800.00, 'BENZ-E-2023', '奔驰 E级', '2023款 E 300 L 运动豪华型', 499800.00, CURRENT_TIMESTAMP, 2);

MERGE INTO t_tran_product (id, tran_id, product_id, quantity, price, product_sku, product_name, product_specification, guide_price, create_time, create_by) KEY(id)
VALUES (3, 3, 3, 1, 399800.00, 'AUDI-Q5-2023', '奥迪 Q5L', '2023款 40 TFSI 荣享时尚型', 399800.00, CURRENT_TIMESTAMP, 1);

-- ==================== Transaction Remarks ====================
MERGE INTO t_tran_remark (id, tran_id, note_way, note_content, create_time, create_by, edit_time, edit_by, deleted) KEY(id)
VALUES (1, 1, 28, '客户确认购买宝马X5', CURRENT_TIMESTAMP, 1, NULL, NULL, 0);

MERGE INTO t_tran_remark (id, tran_id, note_way, note_content, create_time, create_by, edit_time, edit_by, deleted) KEY(id)
VALUES (2, 2, 31, '客户到店签约奔驰E级', CURRENT_TIMESTAMP, 2, NULL, NULL, 0);

-- ==================== Product Promotions ====================
MERGE INTO t_product_promotion (id, product_id, code, name, type, discount, rule_summary, applicable_store, customer_type, applicable_channel, inventory_scope, stackable, priority, budget_limit, used_budget, usage_limit, used_count, start_time, end_time, status, create_time, update_time) KEY(id)
VALUES (1, 1, 'PROMO-BMW-X5-SEED', '宝马X5五一促销', 'PERCENTAGE', 0.95, '报价按95折计算', 'ALL', 'ALL', 'ALL', 'ALL', FALSE, 10, 500000.00, 0.00, 50, 0, '2025-04-28 00:00:00', '2025-05-05 23:59:59', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

MERGE INTO t_product_promotion (id, product_id, code, name, type, discount, rule_summary, applicable_store, customer_type, applicable_channel, inventory_scope, stackable, priority, budget_limit, used_budget, usage_limit, used_count, start_time, end_time, status, create_time, update_time) KEY(id)
VALUES (2, 5, 'PROMO-TESLA-M3-SEED', '特斯拉Model 3购车补贴', 'AMOUNT', 20000.00, '每台直减20000元', 'ALL', 'ALL', 'ALL', 'ALL', FALSE, 8, 600000.00, 0.00, 30, 0, '2025-05-01 00:00:00', '2025-06-30 23:59:59', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- ==================== Transaction Approvals ====================
MERGE INTO t_tran_approve (id, tran_id, approve_result, approve_comment, approve_time, approve_by, create_time, create_by) KEY(id)
VALUES (1, 2, 1, '审批通过，同意交易', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP, 1);

-- ==================== Transaction Invoices ====================
MERGE INTO t_tran_invoice (id, tran_id, invoice_no, type, title, tax_number, bank_name, bank_account, address, phone, amount, status, remark, issue_time, create_time, create_by, edit_time, edit_by) KEY(id)
VALUES (1, 3, 'INV20250618001', 'VAT_NORMAL', '宝马X5发票', '91110000MA01XXXX', '中国银行', '6222021234567890123', '北京市朝阳区', '010-12345678', 399800.00, 'ISSUED', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, NULL, NULL);

-- ==================== Audit Permissions ====================
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
VALUES ('审计日志', 'menu:audit', NULL, 'menu', NULL, 10, 'ShieldCheck', 1);

INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '登录记录', 'page:audit:login', '/dashboard/audit/login', 'menu', id, 1, 'KeyRound', 1 FROM `t_permission` WHERE code = 'menu:audit';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '操作记录', 'page:audit:operation', '/dashboard/audit/operation', 'menu', id, 2, 'ClipboardList', 1 FROM `t_permission` WHERE code = 'menu:audit';

INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '登录记录-列表', 'audit:login:list', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'page:audit:login';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '登录记录-详情', 'audit:login:detail', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'page:audit:login';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '登录记录-导出', 'audit:login:export', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'page:audit:login';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '操作记录-列表', 'audit:operation:list', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'page:audit:operation';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '操作记录-详情', 'audit:operation:detail', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'page:audit:operation';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '操作记录-导出', 'audit:operation:export', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'page:audit:operation';

INSERT INTO `t_role_permission` (`role_id`, `permission_id`)
SELECT r.id, p.id FROM `t_role` r CROSS JOIN `t_permission` p
WHERE r.role = 'admin' AND p.code LIKE 'audit:%';
INSERT INTO `t_role_permission` (`role_id`, `permission_id`)
SELECT r.id, p.id FROM `t_role` r CROSS JOIN `t_permission` p
WHERE r.role = 'admin' AND p.code IN ('menu:audit', 'page:audit:login', 'page:audit:operation');
