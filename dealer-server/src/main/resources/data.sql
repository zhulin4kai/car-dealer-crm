-- Test Data for H2 Database
-- Password for all users: 123456 (BCrypt hash)

-- ==================== Users ====================
INSERT INTO t_user (id, login_act, login_pwd, name, phone, email, account_no_expired, credentials_no_expired, account_no_locked, account_enabled, create_time, create_by, edit_time, edit_by, last_login_time)
VALUES (1, 'admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '管理员', '13700000000', 'admin@test.com', 1, 1, 1, 1, CURRENT_TIMESTAMP, NULL, NULL, NULL, CURRENT_TIMESTAMP);

INSERT INTO t_user (id, login_act, login_pwd, name, phone, email, account_no_expired, credentials_no_expired, account_no_locked, account_enabled, create_time, create_by, edit_time, edit_by, last_login_time)
VALUES (2, 'zhangsan', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '张三', '13800000001', 'zhangsan@test.com', 1, 1, 1, 1, CURRENT_TIMESTAMP, 1, NULL, NULL, NULL);

INSERT INTO t_user (id, login_act, login_pwd, name, phone, email, account_no_expired, credentials_no_expired, account_no_locked, account_enabled, create_time, create_by, edit_time, edit_by, last_login_time)
VALUES (3, 'lisi', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '李四', '13800000002', 'lisi@test.com', 1, 1, 1, 1, CURRENT_TIMESTAMP, 1, NULL, NULL, NULL);

-- ==================== Roles ====================
INSERT INTO t_role (id, role, role_name) VALUES (1, 'admin', '管理员');
INSERT INTO t_role (id, role, role_name) VALUES (2, 'user', '普通用户');
INSERT INTO t_role (id, role, role_name) VALUES (3, 'saler', '销售员');

-- ==================== User Roles ====================
INSERT INTO t_user_role (id, user_id, role_id) VALUES (1, 1, 1);
INSERT INTO t_user_role (id, user_id, role_id) VALUES (2, 2, 2);
INSERT INTO t_user_role (id, user_id, role_id) VALUES (3, 3, 3);

-- ==================== Permissions ====================
-- Menu: 市场活动
INSERT INTO t_permission (id, name, code, url, type, parent_id, order_no, icon) VALUES (1, '市场活动', NULL, NULL, 'menu', 0, 1, 'OfficeBuilding');
INSERT INTO t_permission (id, name, code, url, type, parent_id, order_no, icon) VALUES (2, '市场活动', NULL, '/dashboard/activity', 'menu', 1, 1, 'CreditCard');
INSERT INTO t_permission (id, name, code, url, type, parent_id, order_no, icon) VALUES (3, '市场活动-列表', 'activity:list', NULL, 'button', 2, NULL, NULL);
INSERT INTO t_permission (id, name, code, url, type, parent_id, order_no, icon) VALUES (4, '市场活动-录入', 'activity:add', NULL, 'button', 2, NULL, NULL);
INSERT INTO t_permission (id, name, code, url, type, parent_id, order_no, icon) VALUES (5, '市场活动-编辑', 'activity:edit', NULL, 'button', 2, NULL, NULL);
INSERT INTO t_permission (id, name, code, url, type, parent_id, order_no, icon) VALUES (6, '市场活动-查看', 'activity:view', NULL, 'button', 2, NULL, NULL);
INSERT INTO t_permission (id, name, code, url, type, parent_id, order_no, icon) VALUES (7, '市场活动-删除', 'activity:delete', NULL, 'button', 2, NULL, NULL);

-- Menu: 线索管理
INSERT INTO t_permission (id, name, code, url, type, parent_id, order_no, icon) VALUES (10, '线索管理', NULL, NULL, 'menu', 0, 2, 'Magnet');
INSERT INTO t_permission (id, name, code, url, type, parent_id, order_no, icon) VALUES (12, '线索管理', NULL, '/dashboard/clue', 'menu', 10, 1, 'Paperclip');
INSERT INTO t_permission (id, name, code, url, type, parent_id, order_no, icon) VALUES (13, '线索管理-列表', 'clue:list', NULL, 'button', 12, NULL, NULL);
INSERT INTO t_permission (id, name, code, url, type, parent_id, order_no, icon) VALUES (14, '线索管理-录入', 'clue:add', NULL, 'button', 12, NULL, NULL);
INSERT INTO t_permission (id, name, code, url, type, parent_id, order_no, icon) VALUES (15, '线索管理-编辑', 'clue:edit', NULL, 'button', 12, NULL, NULL);
INSERT INTO t_permission (id, name, code, url, type, parent_id, order_no, icon) VALUES (16, '线索管理-查看', 'clue:view', NULL, 'button', 12, NULL, NULL);
INSERT INTO t_permission (id, name, code, url, type, parent_id, order_no, icon) VALUES (17, '线索管理-删除', 'clue:delete', NULL, 'button', 12, NULL, NULL);

-- Menu: 客户管理
INSERT INTO t_permission (id, name, code, url, type, parent_id, order_no, icon) VALUES (19, '客户管理', NULL, NULL, 'menu', 0, 3, 'User');
INSERT INTO t_permission (id, name, code, url, type, parent_id, order_no, icon) VALUES (20, '客户管理', NULL, '/dashboard/customer', 'menu', 19, 1, 'UserFilled');
INSERT INTO t_permission (id, name, code, url, type, parent_id, order_no, icon) VALUES (21, '客户管理-列表', 'customer:list', NULL, 'button', 20, NULL, NULL);
INSERT INTO t_permission (id, name, code, url, type, parent_id, order_no, icon) VALUES (22, '客户管理-查看', 'customer:view', NULL, 'button', 20, NULL, NULL);

-- Menu: 交易管理
INSERT INTO t_permission (id, name, code, url, type, parent_id, order_no, icon) VALUES (24, '交易管理', NULL, NULL, 'menu', 0, 4, 'Wallet');
INSERT INTO t_permission (id, name, code, url, type, parent_id, order_no, icon) VALUES (25, '交易管理', NULL, '/dashboard/tran', 'menu', 24, 1, 'Coin');
INSERT INTO t_permission (id, name, code, url, type, parent_id, order_no, icon) VALUES (26, '交易管理-列表', 'tran:list', NULL, 'button', 25, NULL, NULL);
INSERT INTO t_permission (id, name, code, url, type, parent_id, order_no, icon) VALUES (27, '交易管理-查看', 'tran:view', NULL, 'button', 25, NULL, NULL);

-- Menu: 产品管理
INSERT INTO t_permission (id, name, code, url, type, parent_id, order_no, icon) VALUES (28, '产品管理', NULL, NULL, 'menu', 0, 5, 'Memo');
INSERT INTO t_permission (id, name, code, url, type, parent_id, order_no, icon) VALUES (29, '产品管理', NULL, '/dashboard/product', 'menu', 28, 1, 'SetUp');
INSERT INTO t_permission (id, name, code, url, type, parent_id, order_no, icon) VALUES (30, '产品管理-列表', 'product:list', NULL, 'button', 29, NULL, NULL);
INSERT INTO t_permission (id, name, code, url, type, parent_id, order_no, icon) VALUES (31, '产品管理-录入', 'product:add', NULL, 'button', 29, NULL, NULL);
INSERT INTO t_permission (id, name, code, url, type, parent_id, order_no, icon) VALUES (32, '产品管理-编辑', 'product:edit', NULL, 'button', 29, NULL, NULL);
INSERT INTO t_permission (id, name, code, url, type, parent_id, order_no, icon) VALUES (33, '产品管理-查看', 'product:view', NULL, 'button', 29, NULL, NULL);
INSERT INTO t_permission (id, name, code, url, type, parent_id, order_no, icon) VALUES (34, '产品管理-删除', 'product:delete', NULL, 'button', 29, NULL, NULL);

-- Menu: 字典管理
INSERT INTO t_permission (id, name, code, url, type, parent_id, order_no, icon) VALUES (35, '字典管理', NULL, NULL, 'menu', 0, 6, 'Grid');
INSERT INTO t_permission (id, name, code, url, type, parent_id, order_no, icon) VALUES (36, '字典类型', NULL, '/dashboard/dict/type', 'menu', 35, 1, 'Postcard');
INSERT INTO t_permission (id, name, code, url, type, parent_id, order_no, icon) VALUES (37, '字典类型-列表', 'dict/type:list', NULL, 'button', 36, NULL, NULL);
INSERT INTO t_permission (id, name, code, url, type, parent_id, order_no, icon) VALUES (42, '字典数据', '', '/dashboard/dict/value', 'menu', 35, 2, 'DataAnalysis');
INSERT INTO t_permission (id, name, code, url, type, parent_id, order_no, icon) VALUES (43, '字典数据-列表', 'dict/value:list', NULL, 'button', 42, NULL, NULL);

-- Menu: 用户管理
INSERT INTO t_permission (id, name, code, url, type, parent_id, order_no, icon) VALUES (48, '用户管理', NULL, NULL, 'menu', 0, 7, 'Stamp');
INSERT INTO t_permission (id, name, code, url, type, parent_id, order_no, icon) VALUES (49, '用户管理', NULL, '/dashboard/user', 'menu', 48, 1, 'User');
INSERT INTO t_permission (id, name, code, url, type, parent_id, order_no, icon) VALUES (50, '用户管理-列表', 'user:list', NULL, 'button', 49, NULL, NULL);
INSERT INTO t_permission (id, name, code, url, type, parent_id, order_no, icon) VALUES (51, '用户管理-录入', 'user:add', NULL, 'button', 49, NULL, NULL);
INSERT INTO t_permission (id, name, code, url, type, parent_id, order_no, icon) VALUES (52, '用户管理-编辑', 'user:edit', NULL, 'button', 49, NULL, NULL);
INSERT INTO t_permission (id, name, code, url, type, parent_id, order_no, icon) VALUES (53, '用户管理-查看', 'user:view', NULL, 'button', 49, NULL, NULL);
INSERT INTO t_permission (id, name, code, url, type, parent_id, order_no, icon) VALUES (54, '用户管理-删除', 'user:delete', NULL, 'button', 49, NULL, NULL);

-- Menu: 系统管理
INSERT INTO t_permission (id, name, code, url, type, parent_id, order_no, icon) VALUES (55, '系统管理', NULL, NULL, 'menu', 0, 8, 'Setting');
INSERT INTO t_permission (id, name, code, url, type, parent_id, order_no, icon) VALUES (56, '系统管理', NULL, '/dashboard/system', 'menu', 55, 1, 'Tools');
INSERT INTO t_permission (id, name, code, url, type, parent_id, order_no, icon) VALUES (57, '系统管理-列表', 'system:list', NULL, 'button', 56, NULL, NULL);
INSERT INTO t_permission (id, name, code, url, type, parent_id, order_no, icon) VALUES (58, '系统管理-录入', 'system:add', NULL, 'button', 56, NULL, NULL);
INSERT INTO t_permission (id, name, code, url, type, parent_id, order_no, icon) VALUES (59, '系统管理-编辑', 'system:edit', NULL, 'button', 56, NULL, NULL);
INSERT INTO t_permission (id, name, code, url, type, parent_id, order_no, icon) VALUES (60, '系统管理-查看', 'system:view', NULL, 'button', 56, NULL, NULL);
INSERT INTO t_permission (id, name, code, url, type, parent_id, order_no, icon) VALUES (61, '系统管理-删除', 'system:delete', NULL, 'button', 56, NULL, NULL);

-- ==================== Role Permissions ====================
-- Admin role: all permissions
INSERT INTO t_role_permission (id, role_id, permission_id) VALUES (1, 1, 1);
INSERT INTO t_role_permission (id, role_id, permission_id) VALUES (2, 1, 2);
INSERT INTO t_role_permission (id, role_id, permission_id) VALUES (3, 1, 3);
INSERT INTO t_role_permission (id, role_id, permission_id) VALUES (4, 1, 4);
INSERT INTO t_role_permission (id, role_id, permission_id) VALUES (5, 1, 5);
INSERT INTO t_role_permission (id, role_id, permission_id) VALUES (6, 1, 6);
INSERT INTO t_role_permission (id, role_id, permission_id) VALUES (7, 1, 7);
INSERT INTO t_role_permission (id, role_id, permission_id) VALUES (8, 1, 10);
INSERT INTO t_role_permission (id, role_id, permission_id) VALUES (9, 1, 12);
INSERT INTO t_role_permission (id, role_id, permission_id) VALUES (10, 1, 13);
INSERT INTO t_role_permission (id, role_id, permission_id) VALUES (11, 1, 14);
INSERT INTO t_role_permission (id, role_id, permission_id) VALUES (12, 1, 15);
INSERT INTO t_role_permission (id, role_id, permission_id) VALUES (13, 1, 16);
INSERT INTO t_role_permission (id, role_id, permission_id) VALUES (14, 1, 17);
INSERT INTO t_role_permission (id, role_id, permission_id) VALUES (15, 1, 19);
INSERT INTO t_role_permission (id, role_id, permission_id) VALUES (16, 1, 20);
INSERT INTO t_role_permission (id, role_id, permission_id) VALUES (17, 1, 21);
INSERT INTO t_role_permission (id, role_id, permission_id) VALUES (18, 1, 22);
INSERT INTO t_role_permission (id, role_id, permission_id) VALUES (19, 1, 24);
INSERT INTO t_role_permission (id, role_id, permission_id) VALUES (20, 1, 25);
INSERT INTO t_role_permission (id, role_id, permission_id) VALUES (21, 1, 26);
INSERT INTO t_role_permission (id, role_id, permission_id) VALUES (22, 1, 27);
INSERT INTO t_role_permission (id, role_id, permission_id) VALUES (23, 1, 28);
INSERT INTO t_role_permission (id, role_id, permission_id) VALUES (24, 1, 29);
INSERT INTO t_role_permission (id, role_id, permission_id) VALUES (25, 1, 30);
INSERT INTO t_role_permission (id, role_id, permission_id) VALUES (26, 1, 31);
INSERT INTO t_role_permission (id, role_id, permission_id) VALUES (27, 1, 32);
INSERT INTO t_role_permission (id, role_id, permission_id) VALUES (28, 1, 33);
INSERT INTO t_role_permission (id, role_id, permission_id) VALUES (29, 1, 34);
INSERT INTO t_role_permission (id, role_id, permission_id) VALUES (30, 1, 35);
INSERT INTO t_role_permission (id, role_id, permission_id) VALUES (31, 1, 36);
INSERT INTO t_role_permission (id, role_id, permission_id) VALUES (32, 1, 37);
INSERT INTO t_role_permission (id, role_id, permission_id) VALUES (33, 1, 42);
INSERT INTO t_role_permission (id, role_id, permission_id) VALUES (34, 1, 43);
INSERT INTO t_role_permission (id, role_id, permission_id) VALUES (35, 1, 48);
INSERT INTO t_role_permission (id, role_id, permission_id) VALUES (36, 1, 49);
INSERT INTO t_role_permission (id, role_id, permission_id) VALUES (37, 1, 50);
INSERT INTO t_role_permission (id, role_id, permission_id) VALUES (38, 1, 51);
INSERT INTO t_role_permission (id, role_id, permission_id) VALUES (39, 1, 52);
INSERT INTO t_role_permission (id, role_id, permission_id) VALUES (40, 1, 53);
INSERT INTO t_role_permission (id, role_id, permission_id) VALUES (41, 1, 54);
INSERT INTO t_role_permission (id, role_id, permission_id) VALUES (42, 1, 55);
INSERT INTO t_role_permission (id, role_id, permission_id) VALUES (43, 1, 56);
INSERT INTO t_role_permission (id, role_id, permission_id) VALUES (44, 1, 57);
INSERT INTO t_role_permission (id, role_id, permission_id) VALUES (45, 1, 58);
INSERT INTO t_role_permission (id, role_id, permission_id) VALUES (46, 1, 59);
INSERT INTO t_role_permission (id, role_id, permission_id) VALUES (47, 1, 60);
INSERT INTO t_role_permission (id, role_id, permission_id) VALUES (48, 1, 61);

-- User role: limited permissions (clue and customer only)
INSERT INTO t_role_permission (id, role_id, permission_id) VALUES (50, 2, 10);
INSERT INTO t_role_permission (id, role_id, permission_id) VALUES (51, 2, 12);
INSERT INTO t_role_permission (id, role_id, permission_id) VALUES (52, 2, 13);
INSERT INTO t_role_permission (id, role_id, permission_id) VALUES (53, 2, 14);
INSERT INTO t_role_permission (id, role_id, permission_id) VALUES (54, 2, 15);
INSERT INTO t_role_permission (id, role_id, permission_id) VALUES (55, 2, 16);
INSERT INTO t_role_permission (id, role_id, permission_id) VALUES (56, 2, 17);
INSERT INTO t_role_permission (id, role_id, permission_id) VALUES (57, 2, 19);
INSERT INTO t_role_permission (id, role_id, permission_id) VALUES (58, 2, 20);
INSERT INTO t_role_permission (id, role_id, permission_id) VALUES (59, 2, 21);
INSERT INTO t_role_permission (id, role_id, permission_id) VALUES (60, 2, 22);

-- Saler role: clue, customer, tran permissions
INSERT INTO t_role_permission (id, role_id, permission_id) VALUES (70, 3, 10);
INSERT INTO t_role_permission (id, role_id, permission_id) VALUES (71, 3, 12);
INSERT INTO t_role_permission (id, role_id, permission_id) VALUES (72, 3, 13);
INSERT INTO t_role_permission (id, role_id, permission_id) VALUES (73, 3, 14);
INSERT INTO t_role_permission (id, role_id, permission_id) VALUES (74, 3, 15);
INSERT INTO t_role_permission (id, role_id, permission_id) VALUES (75, 3, 16);
INSERT INTO t_role_permission (id, role_id, permission_id) VALUES (76, 3, 17);
INSERT INTO t_role_permission (id, role_id, permission_id) VALUES (77, 3, 19);
INSERT INTO t_role_permission (id, role_id, permission_id) VALUES (78, 3, 20);
INSERT INTO t_role_permission (id, role_id, permission_id) VALUES (79, 3, 21);
INSERT INTO t_role_permission (id, role_id, permission_id) VALUES (80, 3, 22);
INSERT INTO t_role_permission (id, role_id, permission_id) VALUES (81, 3, 24);
INSERT INTO t_role_permission (id, role_id, permission_id) VALUES (82, 3, 25);
INSERT INTO t_role_permission (id, role_id, permission_id) VALUES (83, 3, 26);
INSERT INTO t_role_permission (id, role_id, permission_id) VALUES (84, 3, 27);

-- ==================== Dictionary Types ====================
INSERT INTO t_dic_type (id, type_code, type_name, remark) VALUES (1, 'sex', '性别', NULL);
INSERT INTO t_dic_type (id, type_code, type_name, remark) VALUES (2, 'appellation', '称呼', NULL);
INSERT INTO t_dic_type (id, type_code, type_name, remark) VALUES (3, 'clueState', '线索状态', NULL);
INSERT INTO t_dic_type (id, type_code, type_name, remark) VALUES (4, 'returnPriority', '回访优先级', NULL);
INSERT INTO t_dic_type (id, type_code, type_name, remark) VALUES (5, 'returnState', '回访状态', NULL);
INSERT INTO t_dic_type (id, type_code, type_name, remark) VALUES (6, 'source', '来源', NULL);
INSERT INTO t_dic_type (id, type_code, type_name, remark) VALUES (7, 'stage', '阶段', NULL);
INSERT INTO t_dic_type (id, type_code, type_name, remark) VALUES (8, 'transactionType', '交易类型', NULL);
INSERT INTO t_dic_type (id, type_code, type_name, remark) VALUES (9, 'intentionState', '意向状态', NULL);
INSERT INTO t_dic_type (id, type_code, type_name, remark) VALUES (10, 'needLoan', '是否贷款', NULL);
INSERT INTO t_dic_type (id, type_code, type_name, remark) VALUES (11, 'noteWay', '跟踪方式', NULL);
INSERT INTO t_dic_type (id, type_code, type_name, remark) VALUES (12, 'userState', '用户状态', NULL);
INSERT INTO t_dic_type (id, type_code, type_name, remark) VALUES (13, 'educational', '学历', NULL);

-- ==================== Dictionary Values ====================
-- Sex
INSERT INTO t_dic_value (id, type_code, type_value, `order`, remark) VALUES (1, 'sex', '男', 1, NULL);
INSERT INTO t_dic_value (id, type_code, type_value, `order`, remark) VALUES (2, 'sex', '女', 2, NULL);

-- Appellation
INSERT INTO t_dic_value (id, type_code, type_value, `order`, remark) VALUES (3, 'appellation', '先生', 1, NULL);
INSERT INTO t_dic_value (id, type_code, type_value, `order`, remark) VALUES (4, 'appellation', '女士', 2, NULL);
INSERT INTO t_dic_value (id, type_code, type_value, `order`, remark) VALUES (5, 'appellation', '教授', 3, NULL);

-- Clue State
INSERT INTO t_dic_value (id, type_code, type_value, `order`, remark) VALUES (6, 'clueState', '试图联系', 1, NULL);
INSERT INTO t_dic_value (id, type_code, type_value, `order`, remark) VALUES (7, 'clueState', '将来联系', 2, NULL);
INSERT INTO t_dic_value (id, type_code, type_value, `order`, remark) VALUES (8, 'clueState', '已联系', 3, NULL);
INSERT INTO t_dic_value (id, type_code, type_value, `order`, remark) VALUES (9, 'clueState', '已转客户', 0, NULL);
INSERT INTO t_dic_value (id, type_code, type_value, `order`, remark) VALUES (10, 'clueState', '虚假线索', 4, NULL);
INSERT INTO t_dic_value (id, type_code, type_value, `order`, remark) VALUES (11, 'clueState', '丢失线索', 5, NULL);

-- Source
INSERT INTO t_dic_value (id, type_code, type_value, `order`, remark) VALUES (12, 'source', '网络广告', 1, NULL);
INSERT INTO t_dic_value (id, type_code, type_value, `order`, remark) VALUES (13, 'source', '懂车帝', 2, NULL);
INSERT INTO t_dic_value (id, type_code, type_value, `order`, remark) VALUES (14, 'source', '员工介绍', 3, NULL);
INSERT INTO t_dic_value (id, type_code, type_value, `order`, remark) VALUES (15, 'source', '门店参观', 4, NULL);
INSERT INTO t_dic_value (id, type_code, type_value, `order`, remark) VALUES (16, 'source', '官方网站', 5, NULL);

-- Intention State
INSERT INTO t_dic_value (id, type_code, type_value, `order`, remark) VALUES (17, 'intentionState', '有意向', 1, NULL);
INSERT INTO t_dic_value (id, type_code, type_value, `order`, remark) VALUES (18, 'intentionState', '无意向', 2, NULL);
INSERT INTO t_dic_value (id, type_code, type_value, `order`, remark) VALUES (19, 'intentionState', '意向不明', 3, NULL);

-- Need Loan
INSERT INTO t_dic_value (id, type_code, type_value, `order`, remark) VALUES (20, 'needLoan', '需要', 1, NULL);
INSERT INTO t_dic_value (id, type_code, type_value, `order`, remark) VALUES (21, 'needLoan', '不需要', 2, NULL);

-- Stage
INSERT INTO t_dic_value (id, type_code, type_value, `order`, remark) VALUES (22, 'stage', '01创建交易', 1, NULL);
INSERT INTO t_dic_value (id, type_code, type_value, `order`, remark) VALUES (23, 'stage', '02确认清单', 3, NULL);
INSERT INTO t_dic_value (id, type_code, type_value, `order`, remark) VALUES (24, 'stage', '03交付定金', 4, NULL);
INSERT INTO t_dic_value (id, type_code, type_value, `order`, remark) VALUES (25, 'stage', '04产品检验', 5, NULL);
INSERT INTO t_dic_value (id, type_code, type_value, `order`, remark) VALUES (26, 'stage', '05付款成交', 6, NULL);
INSERT INTO t_dic_value (id, type_code, type_value, `order`, remark) VALUES (27, 'stage', '06丢失关闭', 7, NULL);

-- Note Way
INSERT INTO t_dic_value (id, type_code, type_value, `order`, remark) VALUES (28, 'noteWay', '电话', 1, NULL);
INSERT INTO t_dic_value (id, type_code, type_value, `order`, remark) VALUES (29, 'noteWay', '微信', 2, NULL);
INSERT INTO t_dic_value (id, type_code, type_value, `order`, remark) VALUES (30, 'noteWay', 'QQ', 3, NULL);
INSERT INTO t_dic_value (id, type_code, type_value, `order`, remark) VALUES (31, 'noteWay', '面聊', 4, NULL);
INSERT INTO t_dic_value (id, type_code, type_value, `order`, remark) VALUES (32, 'noteWay', '其他', 5, NULL);

-- User State
INSERT INTO t_dic_value (id, type_code, type_value, `order`, remark) VALUES (33, 'userState', '正常', 1, NULL);
INSERT INTO t_dic_value (id, type_code, type_value, `order`, remark) VALUES (34, 'userState', '锁定', 2, NULL);
INSERT INTO t_dic_value (id, type_code, type_value, `order`, remark) VALUES (35, 'userState', '禁用', 3, NULL);

-- Return Priority
INSERT INTO t_dic_value (id, type_code, type_value, `order`, remark) VALUES (36, 'returnPriority', '最高', 1, NULL);
INSERT INTO t_dic_value (id, type_code, type_value, `order`, remark) VALUES (37, 'returnPriority', '高', 2, NULL);
INSERT INTO t_dic_value (id, type_code, type_value, `order`, remark) VALUES (38, 'returnPriority', '常规', 3, NULL);

-- Return State
INSERT INTO t_dic_value (id, type_code, type_value, `order`, remark) VALUES (39, 'returnState', '未启动', 1, NULL);
INSERT INTO t_dic_value (id, type_code, type_value, `order`, remark) VALUES (40, 'returnState', '进行中', 2, NULL);
INSERT INTO t_dic_value (id, type_code, type_value, `order`, remark) VALUES (41, 'returnState', '完成', 3, NULL);

-- Educational
INSERT INTO t_dic_value (id, type_code, type_value, `order`, remark) VALUES (42, 'educational', '大学', 1, NULL);
INSERT INTO t_dic_value (id, type_code, type_value, `order`, remark) VALUES (43, 'educational', '研究生', 2, NULL);

-- ==================== Product Categories ====================
INSERT INTO t_product_category (id, name, code, description, sort, status, create_time, update_time)
VALUES (1, 'SUV', 'SUV', '运动型多用途汽车', 1, '启用', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO t_product_category (id, name, code, description, sort, status, create_time, update_time)
VALUES (2, '轿车', 'SEDAN', '三厢式乘用车', 2, '启用', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO t_product_category (id, name, code, description, sort, status, create_time, update_time)
VALUES (3, '电动轿车', 'ELECTRIC_SEDAN', '纯电动驱动的三厢式乘用车', 3, '启用', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- ==================== Products ====================
INSERT INTO t_product (id, sku, name, category, specification, price, stock, min_stock, status, create_time, update_time)
VALUES (1, 'BMW-X5-2023', '宝马 X5', 'SUV', '2023款 xDrive40i 尊享型', 569800.00, 15, 5, '上架', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO t_product (id, sku, name, category, specification, price, stock, min_stock, status, create_time, update_time)
VALUES (2, 'BENZ-E-2023', '奔驰 E级', '轿车', '2023款 E 300 L 运动豪华型', 499800.00, 15, 6, '上架', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO t_product (id, sku, name, category, specification, price, stock, min_stock, status, create_time, update_time)
VALUES (3, 'AUDI-Q5-2023', '奥迪 Q5L', 'SUV', '2023款 40 TFSI 荣享时尚型', 399800.00, 16, 6, '上架', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO t_product (id, sku, name, category, specification, price, stock, min_stock, status, create_time, update_time)
VALUES (4, 'BMW-5-2023', '宝马 5系', '轿车', '2023款 530Li 行政型', 459800.00, 18, 7, '上架', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO t_product (id, sku, name, category, specification, price, stock, min_stock, status, create_time, update_time)
VALUES (5, 'TESLA-MODEL3-2023', '特斯拉 Model 3', '电动轿车', '2023款 后轮驱动版', 259900.00, 30, 15, '上架', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- ==================== Activities ====================
INSERT INTO t_activity (id, owner_id, name, start_time, end_time, cost, description, create_time, create_by, edit_time, edit_by)
VALUES (1, 1, '春季汽车展销会', '2025-03-15 09:00:00', '2025-03-17 18:00:00', 50000.00, '春季大型汽车展销活动', CURRENT_TIMESTAMP, 1, NULL, NULL);

INSERT INTO t_activity (id, owner_id, name, start_time, end_time, cost, description, create_time, create_by, edit_time, edit_by)
VALUES (2, 2, '豪华车试驾体验', '2025-04-01 10:00:00', '2025-04-30 17:00:00', 30000.00, '提供豪华车型免费试驾服务', CURRENT_TIMESTAMP, 2, NULL, NULL);

INSERT INTO t_activity (id, owner_id, name, start_time, end_time, cost, description, create_time, create_by, edit_time, edit_by)
VALUES (3, 1, '夏季购车优惠节', '2025-06-01 09:00:00', '2025-06-30 20:00:00', 80000.00, '夏季购车大优惠', CURRENT_TIMESTAMP, 1, NULL, NULL);

-- ==================== Activity Remarks ====================
INSERT INTO t_activity_remark (id, activity_id, note_content, create_time, create_by, edit_time, edit_by, deleted)
VALUES (1, 1, '活动场地已确认', CURRENT_TIMESTAMP, 1, NULL, NULL, 0);

INSERT INTO t_activity_remark (id, activity_id, note_content, create_time, create_by, edit_time, edit_by, deleted)
VALUES (2, 1, '已联系15家汽车品牌参展', CURRENT_TIMESTAMP, 1, NULL, NULL, 0);

INSERT INTO t_activity_remark (id, activity_id, note_content, create_time, create_by, edit_time, edit_by, deleted)
VALUES (3, 2, '试驾路线规划完成', CURRENT_TIMESTAMP, 2, NULL, NULL, 0);

-- ==================== Clues ====================
INSERT INTO t_clue (id, owner_id, activity_id, full_name, appellation, phone, weixin, qq, email, age, job, year_income, address, need_loan, intention_state, intention_product, state, source, description, next_contact_time, create_time, create_by, edit_time, edit_by)
VALUES (1, 1, 1, '王杰', 3, '13700001001', 'wx_wangjie', '123456789', 'wangjie@test.com', 32, '软件工程师', 300000.00, '北京市朝阳区建国路88号', 20, 17, 1, 8, 12, '对宝马X5感兴趣', '2025-06-25 10:00:00', CURRENT_TIMESTAMP, 1, NULL, NULL);

INSERT INTO t_clue (id, owner_id, activity_id, full_name, appellation, phone, weixin, qq, email, age, job, year_income, address, need_loan, intention_state, intention_product, state, source, description, next_contact_time, create_time, create_by, edit_time, edit_by)
VALUES (2, 2, 2, '李娜', 4, '13800001002', 'wx_lina', '987654321', 'lina@test.com', 28, '金融分析师', 400000.00, '上海市浦东新区陆家嘴金融中心', 21, 17, 2, 6, 13, '想购买豪华轿车', '2025-06-27 15:30:00', CURRENT_TIMESTAMP, 2, NULL, NULL);

INSERT INTO t_clue (id, owner_id, activity_id, full_name, appellation, phone, weixin, qq, email, age, job, year_income, address, need_loan, intention_state, intention_product, state, source, description, next_contact_time, create_time, create_by, edit_time, edit_by)
VALUES (3, 3, 3, '张伟', 3, '13900001003', 'wx_zhangwei', '456789123', 'zhangwei@test.com', 35, '企业高管', 600000.00, '广州市天河区珠江新城', 20, 18, 3, 6, 14, '想要购买奔驰E级作为商务用车', '2025-06-23 09:15:00', CURRENT_TIMESTAMP, 1, NULL, NULL);

-- ==================== Clue Remarks ====================
INSERT INTO t_clue_remark (id, clue_id, note_way, note_content, create_time, create_by, edit_time, edit_by, deleted)
VALUES (1, 1, 28, '首次电话联系，客户表示对宝马X5很感兴趣', CURRENT_TIMESTAMP, 1, NULL, NULL, 0);

INSERT INTO t_clue_remark (id, clue_id, note_way, note_content, create_time, create_by, edit_time, edit_by, deleted)
VALUES (2, 1, 29, '通过微信发送了宝马X5的产品资料和价格表', CURRENT_TIMESTAMP, 1, NULL, NULL, 0);

INSERT INTO t_clue_remark (id, clue_id, note_way, note_content, create_time, create_by, edit_time, edit_by, deleted)
VALUES (3, 2, 28, '电话联系，客户工作繁忙，约定下周面谈', CURRENT_TIMESTAMP, 2, NULL, NULL, 0);

-- ==================== Customers ====================
INSERT INTO t_customer (id, clue_id, product, description, next_contact_time, create_time, create_by, edit_time, edit_by)
VALUES (1, 1, 1, '软件工程师，购买宝马X5，已签约成功', '2025-06-25 10:00:00', CURRENT_TIMESTAMP, 1, NULL, NULL);

INSERT INTO t_customer (id, clue_id, product, description, next_contact_time, create_time, create_by, edit_time, edit_by)
VALUES (2, 2, 2, '金融分析师，购买奔驰E级，已签约', '2025-06-30 14:00:00', CURRENT_TIMESTAMP, 2, NULL, NULL);

-- ==================== Customer Remarks ====================
INSERT INTO t_customer_remark (id, customer_id, note_way, note_content, create_by, create_time, edit_time, edit_by, deleted)
VALUES (1, 1, 28, '客户签约成功，选择宝马X5全款购车', 1, CURRENT_TIMESTAMP, NULL, NULL, 0);

INSERT INTO t_customer_remark (id, customer_id, note_way, note_content, create_by, create_time, edit_time, edit_by, deleted)
VALUES (2, 2, 31, '客户到店签约，选择奔驰E级', 2, CURRENT_TIMESTAMP, NULL, NULL, 0);

-- ==================== Transactions ====================
INSERT INTO t_tran (id, tran_no, customer_id, money, expected_date, stage, description, next_contact_time, create_time, create_by, edit_time, edit_by)
VALUES (1, 'T2025061800001', 1, 569800.00, '2025-07-15 00:00:00', 22, '宝马X5交易，已创建', '2025-06-25 10:00:00', CURRENT_TIMESTAMP, 1, NULL, NULL);

INSERT INTO t_tran (id, tran_no, customer_id, money, expected_date, stage, description, next_contact_time, create_time, create_by, edit_time, edit_by)
VALUES (2, 'T2025061800002', 2, 499800.00, '2025-08-01 00:00:00', 24, '奔驰E级交易，已交付定金', '2025-06-28 14:00:00', CURRENT_TIMESTAMP, 2, NULL, NULL);

INSERT INTO t_tran (id, tran_no, customer_id, money, expected_date, stage, description, next_contact_time, create_time, create_by, edit_time, edit_by)
VALUES (3, 'T2025061800003', 1, 399800.00, '2025-07-10 00:00:00', 26, '奥迪Q5L交易，已成交', NULL, CURRENT_TIMESTAMP, 1, NULL, NULL);

-- ==================== Transaction History ====================
INSERT INTO t_tran_history (id, tran_id, stage, money, expected_date, create_time, create_by)
VALUES (1, 1, 22, 569800.00, '2025-07-15 00:00:00', CURRENT_TIMESTAMP, 1);

INSERT INTO t_tran_history (id, tran_id, stage, money, expected_date, create_time, create_by)
VALUES (2, 2, 22, 499800.00, '2025-08-01 00:00:00', CURRENT_TIMESTAMP, 2);

INSERT INTO t_tran_history (id, tran_id, stage, money, expected_date, create_time, create_by)
VALUES (3, 2, 24, 499800.00, '2025-08-01 00:00:00', CURRENT_TIMESTAMP, 2);

-- ==================== Transaction Products ====================
INSERT INTO t_tran_product (id, tran_id, product_id, quantity, price, create_time, create_by)
VALUES (1, 1, 1, 1, 569800.00, CURRENT_TIMESTAMP, 1);

INSERT INTO t_tran_product (id, tran_id, product_id, quantity, price, create_time, create_by)
VALUES (2, 2, 2, 1, 499800.00, CURRENT_TIMESTAMP, 2);

INSERT INTO t_tran_product (id, tran_id, product_id, quantity, price, create_time, create_by)
VALUES (3, 3, 3, 1, 399800.00, CURRENT_TIMESTAMP, 1);

-- ==================== Transaction Remarks ====================
INSERT INTO t_tran_remark (id, tran_id, note_way, note_content, create_time, create_by, edit_time, edit_by, deleted)
VALUES (1, 1, 28, '客户确认购买宝马X5', CURRENT_TIMESTAMP, 1, NULL, NULL, 0);

INSERT INTO t_tran_remark (id, tran_id, note_way, note_content, create_time, create_by, edit_time, edit_by, deleted)
VALUES (2, 2, 31, '客户到店签约奔驰E级', CURRENT_TIMESTAMP, 2, NULL, NULL, 0);

-- ==================== Product Promotions ====================
INSERT INTO t_product_promotion (id, name, type, discount, start_time, end_time, status, create_time, update_time)
VALUES (1, '豪华车型五一促销', 'PERCENTAGE', 0.95, '2025-04-28 00:00:00', '2025-05-05 23:59:59', '进行中', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO t_product_promotion (id, name, type, discount, start_time, end_time, status, create_time, update_time)
VALUES (2, '电动车购车补贴', 'AMOUNT', 20000.00, '2025-05-01 00:00:00', '2025-06-30 23:59:59', '进行中', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- ==================== System Info ====================
INSERT INTO t_system_info (id, system_code, name, site, logo, title, description, keywords, shortcuticon, tel, weixin, email, address, version, closeMsg, isopen, create_time, create_by, edit_time, edit_by)
VALUES (1, 'CarSales_001', '豪华汽车销售系统', 'http://www.luxcars.com', '/logos/luxcars_logo.png', '豪华车销售', '提供高端豪华汽车销售服务', '豪华车,销售,宝马,奔驰,奥迪', '/icons/favicon.ico', '400-123-4567', 'luxcars_official', 'contact@luxcars.com', '中国北京市朝阳区', '1.0.0', NULL, 'y', CURRENT_TIMESTAMP, 1, NULL, NULL);

-- ==================== Transaction Approvals ====================
INSERT INTO t_tran_approve (id, tran_id, approve_result, approve_comment, approve_time, approve_by, create_time, create_by)
VALUES (1, 2, 1, '审批通过，同意交易', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP, 1);

-- ==================== Transaction Productions ====================
INSERT INTO t_tran_production (id, tran_product_id, status, description, create_time, create_by, update_time, update_by)
VALUES (1, 1, 'PENDING', '待生产', CURRENT_TIMESTAMP, 1, NULL, NULL);

-- ==================== Transaction Invoices ====================
INSERT INTO t_tran_invoice (id, tran_id, invoice_no, type, title, tax_number, bank_name, bank_account, address, phone, amount, status, remark, issue_time, create_time, create_by, update_time, update_by)
VALUES (1, 3, 'INV20250618001', 'VAT_NORMAL', '宝马X5发票', '91110000MA01XXXX', '中国银行', '6222021234567890123', '北京市朝阳区', '010-12345678', 399800.00, 'ISSUED', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, NULL, NULL);
