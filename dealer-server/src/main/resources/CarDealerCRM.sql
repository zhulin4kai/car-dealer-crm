SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for t_activity
-- ----------------------------
DROP TABLE IF EXISTS `t_activity`;
CREATE TABLE `t_activity`
(
    `id`          int                                                           NOT NULL AUTO_INCREMENT COMMENT '主键，自动增长，活动ID',
    `owner_id`    int                                                           NULL DEFAULT NULL COMMENT '活动所属人ID',
    `name`        varchar(128) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '活动名称',
    `status`      varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci  NOT NULL DEFAULT 'DRAFT' COMMENT '活动状态稳定编码',
    `channel`     varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci  NOT NULL DEFAULT 'OFFLINE_EVENT' COMMENT '活动渠道',
    `target_model` varchar(128) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '目标车型',
    `start_time`  datetime                                                      NULL DEFAULT NULL COMMENT '活动开始时间',
    `end_time`    datetime                                                      NULL DEFAULT NULL COMMENT '活动结束时间',
    `cost`        decimal(11, 2)                                                NULL DEFAULT NULL COMMENT '活动预算',
    `actual_cost` decimal(11, 2)                                                NULL DEFAULT NULL COMMENT '活动实际成本',
    `description` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '活动描述',
    `result_summary` varchar(500) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '复盘结果摘要',
    `review_conclusion` varchar(500) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '复盘结论',
    `reviewed_by` int                                                           NULL DEFAULT NULL COMMENT '复盘人',
    `reviewed_time` datetime                                                    NULL DEFAULT NULL COMMENT '复盘时间',
    `closed_reason` varchar(500) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '关闭原因',
    `canceled_reason` varchar(500) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '取消原因',
    `create_time` datetime                                                      NULL DEFAULT NULL COMMENT '活动创建时间',
    `create_by`   int                                                           NULL DEFAULT NULL COMMENT '活动创建人',
    `edit_time`   datetime                                                      NULL DEFAULT NULL COMMENT '活动编辑时间',
    `edit_by`     int                                                           NULL DEFAULT NULL COMMENT '活动编辑人',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `owner` (`owner_id` ASC) USING BTREE,
    INDEX `idx_activity_status_time` (`status`, `start_time`, `id`) USING BTREE,
    INDEX `create_by` (`create_by` ASC) USING BTREE,
    INDEX `edit_by` (`edit_by` ASC) USING BTREE,
    CONSTRAINT `chk_activity_status` CHECK (`status` IN ('DRAFT', 'PLANNED', 'ONGOING', 'ENDED', 'REVIEWED', 'CLOSED', 'CANCELED')),
    CONSTRAINT `chk_activity_cost` CHECK (`cost` IS NULL OR `cost` >= 0),
    CONSTRAINT `chk_activity_actual_cost` CHECK (`actual_cost` IS NULL OR `actual_cost` >= 0),
    CONSTRAINT `chk_activity_time_range` CHECK (`start_time` IS NULL OR `end_time` IS NULL OR `end_time` > `start_time`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 49
  CHARACTER SET = utf8mb3
  COLLATE = utf8mb3_general_ci COMMENT = '市场活动表'
  ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of t_activity
-- ----------------------------

INSERT INTO `t_activity`
(id, owner_id, name, status, channel, target_model, start_time, end_time, cost, actual_cost, description,
 result_summary, review_conclusion, reviewed_by, reviewed_time, create_time, create_by, edit_time, edit_by)
VALUES
(1, 5, '六月店庆新车品鉴会', 'REVIEWED', '店内活动', '中高端新能源与豪华SUV',
 '2026-06-06 09:30:00', '2026-06-07 18:00:00', 38000.00, 39500.00,
 '面向近三个月高意向客户的店内品鉴活动，设置车型讲解、金融咨询和置换评估。',
 '到店率和试驾转化达到预期，金融咨询席位不足。', '后续同类活动需提前配置金融顾问和置换评估工位。', 5, '2026-06-08 18:20:00',
 '2026-05-12 10:00:00', 5, '2026-06-08 18:20:00', 5),
(2, 9, '周末SUV试驾专场', 'REVIEWED', '试驾专场', 'SUV',
 '2026-06-13 09:00:00', '2026-06-14 17:30:00', 12000.00, 12800.00,
 '按预约时段组织SUV道路试驾，重点覆盖家庭增购与换购客户。',
 '试驾完成率较高，企业客户转化仍需后续跟进。', '保留周末分时段预约，补充企业采购场景资料。', 9, '2026-06-15 18:00:00',
 '2026-05-28 14:10:00', 9, '2026-06-15 18:00:00', 5),
(3, 5, '企业客户用车方案说明会', 'ENDED', '企业说明会', '商务车型',
 '2026-06-18 14:00:00', '2026-06-18 17:00:00', 8000.00, NULL,
 '邀请园区企业行政与采购负责人，介绍商务车型、开票及批量采购方案。',
 NULL, NULL, NULL, NULL, '2026-06-01 09:30:00', 5, NULL, NULL),
(4, 9, '老客户夏季用车关怀日', 'PLANNED', '老客户活动', '增购与转介绍',
 '2026-07-04 09:30:00', '2026-07-04 17:00:00', 15000.00, NULL,
 '为已交付客户提供免费车况检查，并收集增购、换购和转介绍线索。',
 NULL, NULL, NULL, NULL, '2026-06-15 10:20:00', 9, NULL, NULL);

-- ----------------------------
-- Table structure for t_activity_remark
-- ----------------------------
DROP TABLE IF EXISTS `t_activity_remark`;
CREATE TABLE `t_activity_remark`
(
    `id`           int                                                           NOT NULL AUTO_INCREMENT COMMENT '主键，自动增长，活动备注ID',
    `activity_id`  int                                                           NULL DEFAULT NULL COMMENT '活动ID',
    `note_content` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '备注内容',
    `create_time`  datetime                                                      NULL DEFAULT NULL COMMENT '备注创建时间',
    `create_by`    int                                                           NULL DEFAULT NULL COMMENT '备注创建人',
    `edit_time`    datetime                                                      NULL DEFAULT NULL COMMENT '备注编辑时间',
    `edit_by`      int                                                           NULL DEFAULT NULL COMMENT '备注编辑人',
    `deleted`      int                                                           NULL DEFAULT NULL COMMENT '删除状态（0正常，1删除）',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `activity_id` (`activity_id` ASC) USING BTREE,
    INDEX `t_activity_remark_ibfk_2` (`create_by` ASC) USING BTREE,
    INDEX `t_activity_remark_ibfk_3` (`edit_by` ASC) USING BTREE,
    CONSTRAINT `fk_activity_remark_activity` FOREIGN KEY (`activity_id`) REFERENCES `t_activity` (`id`) ON DELETE RESTRICT
) ENGINE = InnoDB
  AUTO_INCREMENT = 7
  CHARACTER SET = utf8mb3
  COLLATE = utf8mb3_general_ci COMMENT = '市场活动备注表'
  ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of t_activity_remark
-- ----------------------------

INSERT INTO `t_activity_remark`
(id, activity_id, note_content, create_time, create_by, edit_time, edit_by, deleted)
VALUES
(1, 1, '已确认邀约名单86人，其中高意向客户31人。', '2026-05-20 11:00:00', 5, NULL, NULL, 0),
(2, 1, '现场到店47组，新增有效线索19条，后续由销售经理统一分配。', '2026-06-08 09:20:00', 5, NULL, NULL, 0),
(3, 2, '两天共安排试驾26组，需为3组客户补做置换评估。', '2026-06-15 10:30:00', 9, NULL, NULL, 0),
(4, 3, '已确认12家企业参会，财务将现场解答专票与付款流程。', '2026-06-16 15:40:00', 5, NULL, NULL, 0),
(5, 4, '售后合作方已确认检测工位和人员安排。', '2026-06-18 09:10:00', 9, NULL, NULL, 0);

-- ----------------------------
-- Table structure for t_clue
-- ----------------------------
DROP TABLE IF EXISTS `t_clue`;
CREATE TABLE `t_clue`
(
    `id`                int                                                           NOT NULL AUTO_INCREMENT COMMENT '主键，自动增长，线索ID',
    `owner_id`          int                                                           NULL DEFAULT NULL COMMENT '线索所属人ID',
    `activity_id`       int                                                           NULL DEFAULT NULL COMMENT '活动ID',
    `activity_name_snapshot` varchar(128) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '来源活动名称快照',
    `full_name`         varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci  NULL DEFAULT NULL COMMENT '姓名',
    `appellation`       int                                                           NULL DEFAULT NULL COMMENT '称呼',
    `phone`             varchar(18) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci  NULL DEFAULT NULL COMMENT '手机号',
    `weixin`            varchar(128) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '微信号',
    `qq`                varchar(20) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci  NULL DEFAULT NULL COMMENT 'QQ号',
    `email`             varchar(128) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '邮箱',
    `age`               int                                                           NULL DEFAULT NULL COMMENT '年龄',
    `job`               varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci  NULL DEFAULT NULL COMMENT '职业',
    `year_income`       decimal(10, 2)                                                NULL DEFAULT NULL COMMENT '年收入',
    `address`           varchar(128) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '地址',
    `need_loan`         int                                                           NULL DEFAULT NULL COMMENT '是否需要贷款（0不需要，1需要）',
    `intention_state`   int                                                           NULL DEFAULT NULL COMMENT '意向状态',
    `intention_product` int                                                           NULL DEFAULT NULL COMMENT '意向产品',
    `state`             int                                                           NULL DEFAULT NULL COMMENT '线索状态',
    `source`            int                                                           NULL DEFAULT NULL COMMENT '线索来源',
    `description`       varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '线索描述',
    `next_contact_time` datetime                                                      NULL DEFAULT NULL COMMENT '下次联系时间',
    `last_follow_time`  datetime                                                      NULL DEFAULT NULL COMMENT '最近跟进时间',
    `last_follow_summary` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '最近跟进摘要',
    `create_time`       datetime                                                      NULL DEFAULT NULL COMMENT '创建时间',
    `create_by`         int                                                           NULL DEFAULT NULL COMMENT '创建人',
    `edit_time`         datetime                                                      NULL DEFAULT NULL COMMENT '编辑时间',
    `edit_by`           int                                                           NULL DEFAULT NULL COMMENT '编辑人',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE KEY `uk_clue_phone` (`phone`),
    INDEX `appellation` (`appellation` ASC) USING BTREE,
    INDEX `state` (`state` ASC) USING BTREE,
    INDEX `source` (`source` ASC) USING BTREE,
    INDEX `owner` (`owner_id` ASC) USING BTREE,
    INDEX `create_by` (`create_by` ASC) USING BTREE,
    INDEX `edit_by` (`edit_by` ASC) USING BTREE,
    INDEX `t_clue_ibfk_7` (`activity_id` ASC) USING BTREE,
    INDEX `t_clue_ibfk_8` (`need_loan` ASC) USING BTREE,
    INDEX `t_clue_ibfk_9` (`intention_state` ASC) USING BTREE,
    INDEX `t_clue_ibfk_10` (`intention_product` ASC) USING BTREE
) ENGINE = InnoDB
  AUTO_INCREMENT = 1144
  CHARACTER SET = utf8mb3
  COLLATE = utf8mb3_general_ci COMMENT = '线索表'
  ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of t_clue
-- ----------------------------

INSERT INTO `t_clue`
(id, owner_id, activity_id, full_name, appellation, phone, weixin, qq, email, age, job,
 year_income, address, need_loan, intention_state, intention_product, state, source,
 description, next_contact_time, create_time, create_by, edit_time, edit_by)
VALUES
(1, 2, 2, '王杰', 18, '13000002001', 'demo_wangjie', NULL, 'wangjie@example.com', 32,
 '软件工程师', 420000.00, '深圳市南山区', 49, 46, 1, -1, 45,
 '周末试驾后确认宝马X5，关注金融月供与提车时间。', NULL,
 '2026-05-16 14:20:00', 9, '2026-05-18 16:15:00', 2),
(2, 3, 1, '李娜', 41, '13000002002', 'demo_lina', NULL, 'lina@example.com', 29,
 '金融分析师', 480000.00, '深圳市福田区', 50, 46, 7, -1, 3,
 '店庆活动到店，意向奥迪A6L，比较全款与低息分期。', NULL,
 '2026-06-06 11:05:00', 5, '2026-06-12 09:50:00', 3),
(3, 8, 3, '张伟', 18, '13000002003', 'demo_zhangwei', NULL, 'zhangwei@example.com', 41,
 '企业行政负责人', 360000.00, '深圳市宝安区', 50, 46, 9, -1, 23,
 '企业商务接待用车，需增值税专票和对公付款。', NULL,
 '2026-06-18 14:35:00', 5, '2026-06-20 09:15:00', 8),
(4, 8, 2, '刘芳', 41, '13000002004', 'demo_liufang', NULL, 'liufang@example.com', 37,
 '医生', 520000.00, '深圳市罗湖区', 49, 47, 12, -1, 14,
 '关注雷克萨斯NX混动版，要求六月中旬前提车。', NULL,
 '2026-05-10 10:10:00', 9, '2026-06-10 18:05:00', 8),
(5, 2, 1, '陈明', 18, '13000002005', 'demo_chenming', NULL, 'chenming@example.com', 44,
 '高校教师', 310000.00, '深圳市龙华区', 50, 46, 6, -1, 39,
 '家庭增购SUV，重点比较后排空间和主动安全配置。', NULL,
 '2026-03-18 09:20:00', 5, '2026-03-20 14:30:00', 2),
(6, 2, NULL, '赵静', 41, '13000002006', 'demo_zhaojing', NULL, 'zhaojing@example.com', 35,
 '建筑设计公司负责人', 650000.00, '深圳市南山区', 50, 46, 7, -1, 39,
 '老客转介绍，企业置换购车，需对公付款和专票。', NULL,
 '2026-02-24 15:40:00', 2, '2026-02-26 09:20:00', 2),
(7, 3, 1, '黄强', 18, '13000002007', 'demo_huangqiang', NULL, 'huangqiang@example.com', 39,
 '律师', 720000.00, '深圳市福田区', 49, 46, 4, -1, 3,
 '意向宝马5系商务用车，重视内饰和金融提前还款条款。', NULL,
 '2026-05-25 16:10:00', 5, '2026-05-28 10:20:00', 3),
(8, 3, 2, '郑丽', 41, '13000002008', 'demo_zhengli', NULL, 'zhengli@example.com', 33,
 '市场经理', 450000.00, '深圳市龙岗区', 49, 46, 10, -1, 45,
 '原意向奔驰GLC，已支付定金后因家庭预算变化取消。', NULL,
 '2026-05-20 13:30:00', 9, '2026-05-22 14:40:00', 3),
(9, 2, 2, '孙宇', 18, '13000002009', 'demo_sunyu', NULL, 'sunyu@example.com', 36,
 '产品经理', 500000.00, '深圳市南山区', 49, 46, 11, 27, 45,
 '试驾后偏好混动车型，等待旧车评估结果。', '2026-06-23 11:00:00',
 '2026-06-14 15:10:00', 9, '2026-06-20 17:25:00', 2),
(10, 8, NULL, '吴佳', 41, '13000002010', 'demo_wujia', NULL, 'wujia@example.com', 30,
 '室内设计师', 280000.00, '深圳市宝安区', 49, 48, 3, 10, 43,
 '官网留资，预算35万元左右，希望预约宝马3系试驾。', '2026-06-24 15:00:00',
 '2026-06-20 10:45:00', 8, NULL, NULL);

-- ----------------------------
-- Table structure for t_clue_remark
-- ----------------------------
DROP TABLE IF EXISTS `t_clue_remark`;
CREATE TABLE `t_clue_remark`
(
    `id`           int                                                           NOT NULL AUTO_INCREMENT COMMENT '主键，自动增长，线索备注ID',
    `clue_id`      int                                                           NULL DEFAULT NULL COMMENT '线索ID',
    `note_way`     int                                                           NULL DEFAULT NULL COMMENT '跟踪方式',
    `note_content` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '跟踪内容',
    `create_time`  datetime                                                      NULL DEFAULT NULL COMMENT '跟踪时间',
    `create_by`    int                                                           NULL DEFAULT NULL COMMENT '跟踪人',
    `edit_time`    datetime                                                      NULL DEFAULT NULL COMMENT '编辑时间',
    `edit_by`      int                                                           NULL DEFAULT NULL COMMENT '编辑人',
    `deleted`      int                                                           NULL DEFAULT NULL COMMENT '删除状态（0正常，1删除）',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `create_by` (`create_by` ASC) USING BTREE,
    INDEX `edit_by` (`edit_by` ASC) USING BTREE,
    INDEX `clue_id` (`clue_id` ASC) USING BTREE,
    INDEX `t_clue_remark_ibfk_4` (`note_way` ASC) USING BTREE,
    CONSTRAINT `fk_clue_remark_clue` FOREIGN KEY (`clue_id`) REFERENCES `t_clue` (`id`) ON DELETE RESTRICT
) ENGINE = InnoDB
  AUTO_INCREMENT = 17
  CHARACTER SET = utf8mb3
  COLLATE = utf8mb3_general_ci COMMENT = '线索跟踪记录表'
  ROW_FORMAT = DYNAMIC;
-- ----------------------------
-- Records of t_clue_remark
-- ----------------------------

INSERT INTO `t_clue_remark`
(id, clue_id, note_way, note_content, create_time, create_by, edit_time, edit_by, deleted)
VALUES
(1, 1, 64, '完成宝马X5试驾，客户认可空间，要求测算三年分期月供。', '2026-05-16 15:30:00', 2, NULL, NULL, 0),
(2, 2, 61, '确认客户预算和付款方式，约定发送A6L两套报价。', '2026-06-07 10:15:00', 3, NULL, NULL, 0),
(3, 3, 64, '企业客户到店确认配置，需走内部采购审批。', '2026-06-18 15:20:00', 8, NULL, NULL, 0),
(4, 4, 61, '告知目标配置暂无现车，客户无法接受预计交付周期。', '2026-06-10 17:45:00', 8, NULL, NULL, 0),
(5, 5, 64, '比较Q5L与X3后选择Q5L，进入正式报价。', '2026-03-20 14:10:00', 2, NULL, NULL, 0),
(6, 6, 61, '转介绍客户信息已核实，安排企业置换评估。', '2026-02-24 16:00:00', 2, NULL, NULL, 0),
(7, 7, 62, '发送宝马5系配置与金融条款，客户确认到店时间。', '2026-05-26 11:40:00', 3, NULL, NULL, 0),
(8, 8, 64, '完成GLC试驾并收集置换车信息，客户购买意向明确。', '2026-05-21 16:10:00', 3, NULL, NULL, 0),
(9, 9, 61, '客户等待旧车评估价格，约定周二再次联系。', '2026-06-20 17:25:00', 2, NULL, NULL, 0),
(10, 10, 62, '已发送宝马3系配置资料，等待客户确认试驾时段。', '2026-06-20 11:05:00', 8, NULL, NULL, 0);

-- ----------------------------
-- Table structure for t_customer
-- ----------------------------
DROP TABLE IF EXISTS `t_customer`;
CREATE TABLE `t_customer`
(
    `id`                int                                                           NOT NULL AUTO_INCREMENT COMMENT '主键，自动增长，客户ID',
    `clue_id`           int                                                           NULL DEFAULT NULL COMMENT '线索ID',
    `owner_id`          int                                                           NULL DEFAULT NULL COMMENT '当前客户负责人ID',
    `activity_id`       int                                                           NULL DEFAULT NULL COMMENT '来源活动ID',
    `activity_name_snapshot` varchar(128) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '来源活动名称快照',
    `customer_name`     varchar(128) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '客户姓名或组织名称',
    `appellation`       int                                                           NULL DEFAULT NULL COMMENT '称呼',
    `phone`             varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci  NULL DEFAULT NULL COMMENT '手机号',
    `weixin`            varchar(128) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '微信号',
    `qq`                varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci  NULL DEFAULT NULL COMMENT 'QQ号',
    `email`             varchar(128) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '邮箱',
    `age`               int                                                           NULL DEFAULT NULL COMMENT '年龄',
    `job`               varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci  NULL DEFAULT NULL COMMENT '职业',
    `year_income`       decimal(10, 2)                                                NULL DEFAULT NULL COMMENT '年收入',
    `address`           varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '地址',
    `need_loan`         int                                                           NULL DEFAULT NULL COMMENT '是否需要贷款',
    `intention_state`   int                                                           NULL DEFAULT NULL COMMENT '意向状态',
    `source`            int                                                           NULL DEFAULT NULL COMMENT '当前客户来源',
    `original_clue_source` int                                                        NULL DEFAULT NULL COMMENT '原始线索来源快照',
    `product`           bigint                                                        NULL DEFAULT NULL COMMENT '选购产品ID',
    `customer_status`   varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci  NULL DEFAULT 'INTENTION' COMMENT '客户经营状态',
    `merged_to_customer_id` int                                                       NULL DEFAULT NULL COMMENT '合并目标客户ID',
    `merge_reason`      varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '合并原因',
    `merge_time`        datetime                                                      NULL DEFAULT NULL COMMENT '合并时间',
    `merge_by`          int                                                           NULL DEFAULT NULL COMMENT '合并操作人',
    `description`       varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '客户描述',
    `next_contact_time` datetime                                                      NULL DEFAULT NULL COMMENT '下次联系时间',
    `last_follow_time`  datetime                                                      NULL DEFAULT NULL COMMENT '最近跟进时间',
    `last_follow_summary` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '最近跟进摘要',
    `create_time`       datetime                                                      NULL DEFAULT NULL COMMENT '创建时间',
    `create_by`         int                                                           NULL DEFAULT NULL COMMENT '创建人',
    `edit_time`         datetime                                                      NULL DEFAULT NULL COMMENT '编辑时间',
    `edit_by`           int                                                           NULL DEFAULT NULL COMMENT '编辑人',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `t_customer_ibfk_1` (`clue_id` ASC) USING BTREE,
    INDEX `t_customer_ibfk_2` (`product` ASC) USING BTREE,
    INDEX `t_customer_ibfk_3` (`create_by` ASC) USING BTREE,
    INDEX `t_customer_ibfk_4` (`edit_by` ASC) USING BTREE,
    INDEX `idx_customer_owner_status` (`owner_id` ASC, `customer_status` ASC) USING BTREE,
    INDEX `idx_customer_phone` (`phone` ASC) USING BTREE,
    INDEX `idx_customer_weixin` (`weixin` ASC) USING BTREE
) ENGINE = InnoDB
  AUTO_INCREMENT = 12
  CHARACTER SET = utf8mb3
  COLLATE = utf8mb3_general_ci COMMENT = '客户表'
  ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of t_customer
-- ----------------------------

INSERT INTO `t_customer`
(id, clue_id, product, description, next_contact_time, create_time, create_by, edit_time, edit_by)
VALUES
(1, 5, 6, '家庭增购客户，关注空间与安全配置；奥迪Q5L已完成交付，待做交付后满意度回访。', '2026-07-18 10:00:00', '2026-03-20 14:30:00', 2, '2026-04-18 16:45:00', 2),
(2, 1, 1, '互联网行业客户，宝马X5金融分期方案已签署，待支付首付款并确认提车资料。', '2026-06-23 10:00:00', '2026-05-18 16:15:00', 2, '2026-06-20 11:30:00', 2),
(3, 7, 4, '律师事务所合伙人，偏好商务轿车，宝马530Li价格审批已通过，待确认合同。', '2026-06-22 15:30:00', '2026-05-28 10:20:00', 3, '2026-06-19 17:10:00', 4),
(4, 3, 9, '企业行政负责人，公司采购奔驰E级用于商务接待，开票资料已预审。', '2026-06-24 14:00:00', '2026-06-06 11:45:00', 8, '2026-06-20 09:15:00', 8),
(5, 2, 7, '金融从业客户，在奥迪A6L全款与低息分期之间比较，重视交付周期。', '2026-06-23 16:00:00', '2026-06-12 09:50:00', 3, '2026-06-18 13:40:00', 3),
(6, 4, 12, '医生家庭用户，原意向雷克萨斯NX，因现车周期过长流失，保留后续换购跟进。', '2026-09-10 11:00:00', '2026-05-10 10:10:00', 8, '2026-06-10 18:05:00', 8),
(7, 6, 7, '建筑设计公司负责人，奥迪A6L置换购车已交付，进入老客维系阶段。', '2026-09-28 15:30:00', '2026-02-26 09:20:00', 2, '2026-03-28 17:30:00', 2),
(8, 8, 10, '市场管理人员，奔驰GLC订单已取消并完成退款，后续关注预算更低的SUV。', '2026-08-15 13:00:00', '2026-05-22 14:40:00', 3, '2026-06-16 14:20:00', 3);

UPDATE `t_customer` c
    JOIN `t_clue` cl ON c.`clue_id` = cl.`id`
SET c.`owner_id` = cl.`owner_id`,
    c.`activity_id` = cl.`activity_id`,
    c.`customer_name` = cl.`full_name`,
    c.`appellation` = cl.`appellation`,
    c.`phone` = cl.`phone`,
    c.`weixin` = cl.`weixin`,
    c.`qq` = cl.`qq`,
    c.`email` = cl.`email`,
    c.`age` = cl.`age`,
    c.`job` = cl.`job`,
    c.`year_income` = cl.`year_income`,
    c.`address` = cl.`address`,
    c.`need_loan` = cl.`need_loan`,
    c.`intention_state` = cl.`intention_state`,
    c.`source` = cl.`source`,
    c.`original_clue_source` = cl.`source`,
    c.`customer_status` = 'INTENTION';

-- ----------------------------
-- Table structure for t_customer_remark
-- ----------------------------
DROP TABLE IF EXISTS `t_customer_remark`;
CREATE TABLE `t_customer_remark`
(
    `id`           int                                                           NOT NULL AUTO_INCREMENT COMMENT '主键，自动增长，客户备注ID',
    `customer_id`  int                                                           NULL DEFAULT NULL COMMENT '客户ID',
    `note_way`     int                                                           NULL DEFAULT NULL COMMENT '跟踪方式',
    `note_content` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '跟踪内容',
    `create_by`    int                                                           NULL DEFAULT NULL COMMENT '跟踪人',
    `create_time`  datetime                                                      NULL DEFAULT NULL COMMENT '跟踪时间',
    `edit_time`    datetime                                                      NULL DEFAULT NULL COMMENT '编辑时间',
    `edit_by`      int                                                           NULL DEFAULT NULL COMMENT '编辑人',
    `deleted`      int                                                           NULL DEFAULT NULL COMMENT '删除状态（0正常，1删除）',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `t_customer_remark_ibfk_1` (`customer_id` ASC) USING BTREE,
    INDEX `t_customer_remark_ibfk_2` (`note_way` ASC) USING BTREE,
    INDEX `t_customer_remark_ibfk_3` (`create_by` ASC) USING BTREE,
    INDEX `t_customer_remark_ibfk_4` (`edit_by` ASC) USING BTREE
) ENGINE = InnoDB
  AUTO_INCREMENT = 23
  CHARACTER SET = utf8mb3
  COLLATE = utf8mb3_general_ci COMMENT = '客户跟踪记录表'
  ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of t_customer_remark
-- ----------------------------

INSERT INTO `t_customer_remark`
(id, customer_id, note_way, note_content, create_by, create_time, edit_time, edit_by, deleted)
VALUES
(1, 1, 64, '完成车辆交付，客户核对随车资料、钥匙和赠送装潢，签署交车确认单。', 2, '2026-04-18 16:40:00', NULL, NULL, 0),
(2, 1, 62, '交付三日回访，客户反馈车机使用正常，已预约三个月后用车回访。', 2, '2026-04-21 11:20:00', NULL, NULL, 0),
(3, 2, 61, '确认金融方案审批通过，提醒客户准备首付款及上牌所需身份证明。', 2, '2026-06-20 11:30:00', NULL, NULL, 0),
(4, 3, 62, '发送经理审批后的最终报价，客户要求保留至本周三。', 3, '2026-06-19 17:20:00', NULL, NULL, 0),
(5, 4, 64, '企业采购负责人到店确认车辆配置，补充公司开票和付款授权资料。', 8, '2026-06-20 09:15:00', NULL, NULL, 0),
(6, 5, 61, '客户比较全款与两年低息方案，约定下次联系时确认付款方式。', 3, '2026-06-18 15:05:00', NULL, NULL, 0),
(7, 6, 61, '客户确认不再等待现车，本次交易关闭；同意三个月后有合适现车再联系。', 8, '2026-06-10 18:05:00', NULL, NULL, 0),
(8, 7, 64, '车辆完成交付，旧车置换资料和新车发票均已交接。', 2, '2026-03-28 17:30:00', NULL, NULL, 0),
(9, 8, 61, '客户提出取消订单，已说明退款时效并提交财务处理。', 3, '2026-06-15 10:40:00', NULL, NULL, 0),
(10, 8, 62, '通知客户定金已原路退回，客户确认到账。', 3, '2026-06-16 14:20:00', NULL, NULL, 0);

-- ----------------------------
-- Table structure for t_customer_owner_history
-- ----------------------------
DROP TABLE IF EXISTS `t_customer_owner_history`;
CREATE TABLE `t_customer_owner_history`
(
    `id`            int                                                           NOT NULL AUTO_INCREMENT COMMENT '主键',
    `customer_id`   int                                                           NOT NULL COMMENT '客户ID',
    `from_owner_id` int                                                           NULL DEFAULT NULL COMMENT '原负责人ID',
    `to_owner_id`   int                                                           NOT NULL COMMENT '新负责人ID',
    `reason`        varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '转移原因',
    `operator_id`   int                                                           NOT NULL COMMENT '操作人ID',
    `transfer_time` datetime                                                      NOT NULL COMMENT '转移时间',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `idx_customer_owner_history_customer` (`customer_id` ASC) USING BTREE,
    INDEX `idx_customer_owner_history_from_owner` (`from_owner_id` ASC) USING BTREE,
    INDEX `idx_customer_owner_history_to_owner` (`to_owner_id` ASC) USING BTREE,
    INDEX `idx_customer_owner_history_operator` (`operator_id` ASC) USING BTREE
) ENGINE = InnoDB
  CHARACTER SET = utf8mb3
  COLLATE = utf8mb3_general_ci COMMENT = '客户归属转移历史表'
  ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for t_dic_type
-- ----------------------------
DROP TABLE IF EXISTS `t_dic_type`;
CREATE TABLE `t_dic_type`
(
    `id`                int                                                           NOT NULL AUTO_INCREMENT COMMENT '主键，自动增长，字典类型ID',
    `type_code`         varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci  NOT NULL COMMENT '稳定字典类型编码',
    `type_name`         varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci  NULL DEFAULT NULL COMMENT '字典类型展示名称',
    `applicable_module` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci  NULL DEFAULT NULL COMMENT '适用模块',
    `enabled`           tinyint                                                       NOT NULL DEFAULT 1 COMMENT '是否启用：1启用，0停用',
    `built_in`          tinyint                                                       NOT NULL DEFAULT 0 COMMENT '是否系统内置',
    `disable_reason`    varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '停用原因',
    `disabled_by`       int                                                           NULL DEFAULT NULL COMMENT '停用操作人',
    `disabled_time`     datetime                                                      NULL DEFAULT NULL COMMENT '停用时间',
    `remark`            varchar(128) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE KEY `uk_dic_type_code` (`type_code`),
    KEY `idx_dic_type_enabled` (`enabled`),
    CONSTRAINT `chk_dic_type_enabled` CHECK (`enabled` IN (0, 1)),
    CONSTRAINT `chk_dic_type_built_in` CHECK (`built_in` IN (0, 1))
) ENGINE = InnoDB
  AUTO_INCREMENT = 14
  CHARACTER SET = utf8mb3
  COLLATE = utf8mb3_general_ci COMMENT = '字典类型表'
  ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of t_dic_type
-- ----------------------------
INSERT INTO `t_dic_type` (id, type_code, type_name, remark)
VALUES (1, 'sex', '性别', NULL),
       (2, 'appellation', '称呼', NULL),
       (3, 'clueState', '线索状态', NULL),
       (4, 'returnPriority', '回访优先级', NULL),
       (5, 'returnState', '回访状态', NULL),
       (6, 'source', '来源', NULL),
       (7, 'stage', '阶段', NULL),
       (8, 'transactionType', '交易类型', NULL),
       (9, 'intentionState', '意向状态', NULL),
       (10, 'needLoan', '是否贷款', NULL),
       (11, 'educational', '学历', NULL),
       (12, 'userState', '用户状态', NULL),
       (13, 'noteWay', '跟踪方式', NULL);

UPDATE `t_dic_type`
SET `built_in` = 1
WHERE `type_code` IN ('sex', 'appellation', 'clueState', 'returnPriority', 'returnState',
                      'source', 'stage', 'transactionType', 'intentionState',
                      'needLoan', 'educational', 'userState', 'noteWay');


-- ----------------------------
-- Table structure for t_dic_value
-- ----------------------------
DROP TABLE IF EXISTS `t_dic_value`;
CREATE TABLE `t_dic_value`
(
    `id`                int                                                           NOT NULL AUTO_INCREMENT COMMENT '主键，自动增长，字典值ID',
    `type_code`         varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci  NOT NULL COMMENT '稳定字典类型编码',
    `type_value`        varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci  NULL DEFAULT NULL COMMENT '字典展示名称',
    `value_code`        varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci  NOT NULL COMMENT '稳定业务编码',
    `order`             int                                                           NULL DEFAULT NULL COMMENT '字典值排序',
    `applicable_module` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci  NULL DEFAULT NULL COMMENT '适用模块',
    `enabled`           tinyint                                                       NOT NULL DEFAULT 1 COMMENT '是否启用：1启用，0停用',
    `built_in`          tinyint                                                       NOT NULL DEFAULT 0 COMMENT '是否系统内置',
    `disable_reason`    varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '停用原因',
    `disabled_by`       int                                                           NULL DEFAULT NULL COMMENT '停用操作人',
    `disabled_time`     datetime                                                      NULL DEFAULT NULL COMMENT '停用时间',
    `remark`            varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci  NULL DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE KEY `uk_type_value_code` (`type_code`, `value_code`),
    KEY `idx_dic_value_type_enabled` (`type_code`, `enabled`, `order`),
    CONSTRAINT `chk_dic_value_enabled` CHECK (`enabled` IN (0, 1)),
    CONSTRAINT `chk_dic_value_built_in` CHECK (`built_in` IN (0, 1)),
    CONSTRAINT `fk_dic_value_type_code` FOREIGN KEY (`type_code`) REFERENCES `t_dic_type` (`type_code`) ON DELETE RESTRICT
) ENGINE = InnoDB
  AUTO_INCREMENT = 66
  CHARACTER SET = utf8mb3
  COLLATE = utf8mb3_general_ci COMMENT = '字典值表'
  ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of t_dic_value
-- ----------------------------
INSERT INTO `t_dic_value` (`id`, `type_code`, `type_value`, `value_code`, `order`, `remark`) VALUES
(-1, 'clueState', '已转客户', 'converted', 0, NULL),
(1, 'clueState', '虚假线索', 'fake', 4, NULL),
(2, 'source', '知乎', 'zhihu', 8, NULL),
(3, 'source', '车展会', 'auto_show', 11, NULL),
(4, 'returnPriority', '最高', 'highest', 2, NULL),
(5, 'appellation', '教授', 'professor', 5, NULL),
(6, 'clueState', '将来联系', 'future_contact', 2, NULL),
(7, 'clueState', '丢失线索', 'lost', 5, NULL),
(8, 'returnState', '未启动', 'not_started', 1, NULL),
(10, 'clueState', '试图联系', 'attempt_contact', 1, NULL),
(11, 'appellation', '博士', 'doctor', 4, NULL),
(12, 'stage', 'QUOTATION', 'quotation', 1, '待报价'),
(14, 'source', '汽车之家', 'autohome', 14, NULL),
(15, 'returnPriority', '低', 'low', 3, NULL),
(16, 'source', '网络广告', 'online_ad', 1, NULL),
(17, 'source', '视频直播', 'live_stream', 9, NULL),
(18, 'appellation', '先生', 'mr', 1, NULL),
(19, 'returnPriority', '高', 'high', 1, NULL),
(20, 'appellation', '夫人', 'mrs', 2, NULL),
(21, 'stage', 'LOST', 'lost', 6, '丢失关闭'),
(22, 'source', '地图', 'map', 13, NULL),
(23, 'source', '合作伙伴', 'partner', 6, NULL),
(24, 'clueState', '未联系', 'uncontacted', 6, NULL),
(25, 'source', '朋友圈', 'wechat_moments', 10, NULL),
(26, 'returnState', '进行中', 'in_progress', 3, NULL),
(27, 'clueState', '已联系', 'contacted', 3, NULL),
(28, 'returnState', '推迟', 'deferred', 2, NULL),
(29, 'returnState', '完成', 'completed', 4, NULL),
(30, 'clueState', '需要条件', 'conditional', 7, NULL),
(32, 'returnState', '等待某人', 'waiting', 5, NULL),
(33, 'source', '懂车帝', 'dongchedi', 2, NULL),
(34, 'returnPriority', '常规', 'normal', 5, NULL),
(35, 'stage', 'APPROVED', 'approved', 3, '已审批'),
(36, 'source', '易车网', 'yiche', 12, NULL),
(37, 'stage', 'PENDING', 'pending', 2, '待审批'),
(38, 'returnPriority', '最低', 'lowest', 4, NULL),
(39, 'source', '员工介绍', 'employee_referral', 3, NULL),
(40, 'stage', 'PAYMENT', 'payment', 4, '待收款'),
(41, 'appellation', '女士', 'ms', 3, NULL),
(42, 'stage', 'COMPLETED', 'completed', 5, '已完成'),
(66, 'stage', 'CANCELLED', 'cancelled', 7, '已取消'),
(67, 'stage', 'CLOSED', 'closed', 8, '已关闭'),
(68, 'clueState', '关闭', 'closed', 8, NULL),
(43, 'source', '官方网站', 'official_website', 5, NULL),
(44, 'source', '公众号', 'wechat_official', 7, NULL),
(45, 'source', '门店参观', 'store_visit', 4, NULL),
(46, 'intentionState', '有意向', 'interested', 1, NULL),
(47, 'intentionState', '无意向', 'not_interested', 2, NULL),
(48, 'intentionState', '意向不明', 'unknown', 3, NULL),
(49, 'needLoan', '需要', 'required', 1, NULL),
(50, 'needLoan', '不需要', 'not_required', 2, NULL),
(51, 'sex', '男', 'male', 1, NULL),
(52, 'sex', '女', 'female', 2, NULL),
(53, 'educational', '小学', 'primary', 1, NULL),
(54, 'educational', '初中', 'middle_school', 2, NULL),
(55, 'educational', '高中', 'high_school', 3, NULL),
(56, 'educational', '大学', 'university', 4, NULL),
(57, 'educational', '研究生', 'postgraduate', 5, NULL),
(58, 'userState', '正常', 'normal', 1, NULL),
(59, 'userState', '锁定', 'locked', 2, NULL),
(60, 'userState', '禁用', 'disabled', 3, NULL),
(61, 'noteWay', '电话', 'phone', 1, NULL),
(62, 'noteWay', '微信', 'wechat', 2, NULL),
(63, 'noteWay', 'QQ', 'qq', 3, NULL),
(64, 'noteWay', '面聊', 'in_person', 4, NULL),
(65, 'noteWay', '其他', 'other', 5, NULL);

UPDATE `t_dic_value`
SET `built_in` = 1
WHERE `type_code` IN ('sex', 'appellation', 'clueState', 'returnPriority', 'returnState',
                      'source', 'stage', 'transactionType', 'intentionState',
                      'needLoan', 'educational', 'userState', 'noteWay');

-- ----------------------------
-- Table structure for t_permission
-- ----------------------------
DROP TRIGGER IF EXISTS `trg_permission_no_self_parent_bi`;
DROP TRIGGER IF EXISTS `trg_permission_no_self_parent_bu`;
DROP TABLE IF EXISTS `t_permission`;
CREATE TABLE `t_permission`
(
    `id`        int                                                           NOT NULL AUTO_INCREMENT,
    `name`      varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  NOT NULL,
    `code`      varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  NOT NULL,
    `url`       varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
    `type`      varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  NOT NULL,
    `parent_id` int                                                           NULL DEFAULT NULL,
    `order_no`  int                                                           NULL DEFAULT NULL,
    `icon`      varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
    `module`    varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  NOT NULL DEFAULT 'system',
    `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
    `sensitivity_level` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'NORMAL',
    `delegable` tinyint(1)                                                     NOT NULL DEFAULT 0,
    `enabled`   tinyint(1)                                                    NOT NULL DEFAULT 1,
    `version`   int                                                           NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE KEY `uk_permission_code` (`code`),
    KEY `idx_permission_parent` (`parent_id`),
    CONSTRAINT `chk_permission_type` CHECK (`type` IN ('menu', 'button')),
    CONSTRAINT `chk_permission_sensitivity` CHECK (`sensitivity_level` IN ('NORMAL', 'SENSITIVE', 'PROTECTED')),
    CONSTRAINT `chk_permission_delegable` CHECK (`delegable` IN (0, 1)),
    CONSTRAINT `chk_permission_version` CHECK (`version` >= 0),
    CONSTRAINT `fk_permission_parent` FOREIGN KEY (`parent_id`) REFERENCES `t_permission` (`id`) ON DELETE RESTRICT
) ENGINE = InnoDB
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT = '权限表'
  ROW_FORMAT = DYNAMIC;

DELIMITER $$
CREATE TRIGGER `trg_permission_no_self_parent_bi`
BEFORE INSERT ON `t_permission`
FOR EACH ROW
BEGIN
    IF NEW.`parent_id` IS NOT NULL AND NEW.`parent_id` = NEW.`id` THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'permission parent cannot reference itself';
    END IF;
END$$
CREATE TRIGGER `trg_permission_no_self_parent_bu`
BEFORE UPDATE ON `t_permission`
FOR EACH ROW
BEGIN
    IF NEW.`parent_id` IS NOT NULL AND NEW.`parent_id` = NEW.`id` THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'permission parent cannot reference itself';
    END IF;
END$$
DELIMITER ;

-- ----------------------------
-- Records of t_permission
-- ----------------------------
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`) VALUES
('仪表盘', 'menu:dashboard', '/dashboard', 'menu', NULL, 0, 'Gauge', 1),
('市场活动', 'menu:activity', NULL, 'menu', NULL, 1, 'OfficeBuilding', 1),
('线索管理', 'menu:clue', NULL, 'menu', NULL, 2, 'Magnet', 1),
('客户管理', 'menu:customer', NULL, 'menu', NULL, 3, 'User', 1),
('交易管理', 'menu:tran', NULL, 'menu', NULL, 4, 'Wallet', 1),
('商机管理', 'menu:opportunity', NULL, 'menu', NULL, 5, 'Target', 1),
('试驾管理', 'menu:test-drive', NULL, 'menu', NULL, 6, 'Car', 1),
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
SELECT '交易管理', 'page:tran:list', '/dashboard/tran', 'menu', id, 1, 'Coin', 1 FROM `t_permission` WHERE code = 'menu:tran';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '商机管理', 'page:opportunity:list', '/dashboard/opportunity', 'menu', id, 1, 'Target', 1 FROM `t_permission` WHERE code = 'menu:opportunity';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '试驾管理', 'page:test-drive:list', '/dashboard/test-drive', 'menu', id, 1, 'Car', 1 FROM `t_permission` WHERE code = 'menu:test-drive';
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
SELECT '用户管理-敏感资料查看', 'user:sensitive:view', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'page:user:list';
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
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
VALUES ('AI 业务助手', 'menu:ai', '/dashboard/ai', 'menu', NULL, 7, 'Sparkles', 1);
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT 'AI 助手-使用', 'ai:assistant:use', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'menu:ai';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT 'AI Run-查看', 'ai:run:view', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'menu:ai';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT 'AI 工具-执行', 'ai:tool:execute', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'menu:ai';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT 'AI 提议-确认', 'ai:proposal:confirm', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'menu:ai';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT 'AI 工作流-查看', 'ai:workflow:view', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'menu:ai';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT 'AI 工作流-管理', 'ai:workflow:manage', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'menu:ai';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT 'AI 主动提醒-查看', 'ai:proactive:view', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'menu:ai';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT 'AI 主动提醒-使用', 'ai:proactive:use', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'menu:ai';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT 'AI 模型配置-查看', 'ai:provider-config:view', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'menu:ai';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT 'AI 模型配置-管理', 'ai:provider-config:manage', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'menu:ai';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT 'AI 模型配置-轮换密钥', 'ai:provider-config:rotate-key', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'menu:ai';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT 'AI 策略-查看', 'ai:policy:view', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'menu:ai';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT 'AI 策略-管理', 'ai:policy:manage', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'menu:ai';

INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
VALUES ('组织架构', 'menu:organization', NULL, 'menu', NULL, 2, 'Network', 1);
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '组织架构', 'page:organization:list', '/dashboard/organization', 'menu', id, 1, 'Network', 1
FROM `t_permission` WHERE code = 'menu:organization';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '组织架构-列表', 'organization:list', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'page:organization:list';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '组织架构-查看', 'organization:view', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'page:organization:list';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '组织架构-新增', 'organization:add', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'page:organization:list';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '组织架构-编辑', 'organization:edit', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'page:organization:list';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '组织架构-状态', 'organization:status', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'page:organization:list';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '岗位-列表', 'position:list', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'page:organization:list';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '岗位-新增', 'position:add', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'page:organization:list';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '岗位-编辑', 'position:edit', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'page:organization:list';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '岗位-状态', 'position:status', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'page:organization:list';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '员工-任职调整', 'employee:assignment', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'page:organization:list';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '员工-汇报关系', 'employee:reporting', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'page:organization:list';

INSERT INTO `t_permission` (`name`,`code`,`url`,`type`,`parent_id`,`order_no`,`icon`,`enabled`)
VALUES ('权限管理','menu:access',NULL,'menu',NULL,3,'Shield',1);
INSERT INTO `t_permission` (`name`,`code`,`url`,`type`,`parent_id`,`order_no`,`icon`,`enabled`)
SELECT '角色管理','page:role:list','/dashboard/role','menu',id,1,'Users',1 FROM t_permission WHERE code='menu:access';
INSERT INTO `t_permission` (`name`,`code`,`url`,`type`,`parent_id`,`order_no`,`icon`,`enabled`)
SELECT '权限目录','page:permission:list','/dashboard/permission','menu',id,2,'KeyRound',1 FROM t_permission WHERE code='menu:access';
INSERT INTO `t_permission` (`name`,`code`,`url`,`type`,`parent_id`,`module`,`description`,`sensitivity_level`,`delegable`,`enabled`)
SELECT v.name,v.code,NULL,'button',p.id,'access',v.name,
  CASE WHEN v.code IN ('role:status','role:permission:manage') THEN 'PROTECTED' ELSE 'NORMAL' END,
  CASE WHEN v.code IN ('role:status','role:permission:manage') THEN 0 ELSE 1 END,1 FROM t_permission p JOIN (
 SELECT '角色-列表' name,'role:list' code UNION ALL SELECT '角色-查看','role:view' UNION ALL SELECT '角色-新增','role:add'
 UNION ALL SELECT '角色-编辑','role:edit' UNION ALL SELECT '角色-复制','role:copy' UNION ALL SELECT '角色-状态','role:status'
 UNION ALL SELECT '角色-权限矩阵','role:permission:manage'
) v ON p.code='page:role:list';
INSERT INTO `t_permission` (`name`,`code`,`url`,`type`,`parent_id`,`module`,`description`,`sensitivity_level`,`delegable`,`enabled`)
SELECT '权限目录-列表','permission:list',NULL,'button',id,'access','权限目录-列表','NORMAL',1,1 FROM t_permission WHERE code='page:permission:list';
INSERT INTO `t_permission` (`name`,`code`,`url`,`type`,`parent_id`,`enabled`)
SELECT '用户管理-个人权限','user:permission',NULL,'button',id,1 FROM t_permission WHERE code='page:user:list';

-- Task 10：权限目录元数据。模块从稳定 code 推导；高风险授权默认不可委派。
UPDATE `t_permission`
SET `module` = CASE
    WHEN `code` LIKE 'menu:%' THEN SUBSTRING_INDEX(`code`, ':', -1)
    WHEN `code` LIKE 'page:%' THEN SUBSTRING_INDEX(SUBSTRING_INDEX(`code`, ':', 2), ':', -1)
    ELSE SUBSTRING_INDEX(`code`, ':', 1)
END,
    `description` = `name`;

UPDATE `t_permission`
SET `sensitivity_level` = 'SENSITIVE'
WHERE `code` LIKE '%:delete'
   OR `code` LIKE '%:export'
   OR `code` LIKE '%:import'
   OR `code` LIKE '%:approve'
   OR `code` LIKE '%:payment%'
   OR `code` LIKE '%:refund%'
   OR `code` LIKE '%:invoice%'
   OR `code` LIKE '%:sensitive%'
   OR `code` LIKE '%:adjust';

UPDATE `t_permission`
SET `sensitivity_level` = 'PROTECTED'
WHERE `code` IN ('user:role', 'user:status', 'user:password', 'user:delete',
                 'ai:provider-config:manage', 'ai:provider-config:rotate-key',
                 'organization:edit', 'organization:status', 'position:status',
                 'employee:assignment', 'employee:reporting');

UPDATE `t_permission`
SET `delegable` = CASE WHEN `sensitivity_level` = 'NORMAL' THEN 1 ELSE 0 END;

-- Task 11 稳定元数据不得被通用 code 推导覆盖。
UPDATE `t_permission`
SET `module` = CASE
        WHEN `code` LIKE 'organization:%' THEN 'organization'
        WHEN `code` LIKE 'position:%' THEN 'position'
        ELSE 'employee'
    END,
    `sensitivity_level` = CASE
        WHEN `code` IN ('organization:list', 'organization:view', 'position:list') THEN 'NORMAL'
        WHEN `code` IN ('organization:add', 'position:add', 'position:edit') THEN 'SENSITIVE'
        ELSE 'PROTECTED'
    END,
    `delegable` = CASE
        WHEN `code` IN ('organization:list', 'organization:view', 'position:list') THEN 1
        ELSE 0
    END
WHERE `code` IN ('organization:list', 'organization:view', 'organization:add', 'organization:edit',
                 'organization:status', 'position:list', 'position:add', 'position:edit',
                 'position:status', 'employee:assignment', 'employee:reporting');

UPDATE `t_permission`
SET `module`=CASE WHEN `code`='user:permission' THEN 'user' ELSE 'access' END,
    `sensitivity_level`=CASE WHEN `code` IN ('role:status','role:permission:manage','user:permission') THEN 'PROTECTED' ELSE 'NORMAL' END,
    `delegable`=CASE WHEN `code` IN ('role:status','role:permission:manage','user:permission') THEN 0 ELSE 1 END
WHERE `code` IN ('role:list','role:view','role:add','role:edit','role:copy','role:status',
                 'role:permission:manage','permission:list','user:permission');

-- ----------------------------
-- Table structure for t_role
-- ----------------------------
DROP TABLE IF EXISTS `t_role`;
CREATE TABLE `t_role`
(
    `id`        int                                                          NOT NULL AUTO_INCREMENT,
    `role`      varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
    `role_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
    `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
    `protected_role` tinyint(1)                                              NOT NULL DEFAULT 0,
    `authorization_level` int                                               NOT NULL DEFAULT 0,
    `default_data_scope` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'SELF',
    `scope_type` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'GLOBAL',
    `enabled`   tinyint(1)                                                   NOT NULL DEFAULT 1,
    `version`   int                                                          NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE KEY `uk_role_code` (`role`),
    CONSTRAINT `chk_role_protected` CHECK (`protected_role` IN (0, 1)),
    CONSTRAINT `chk_role_authorization_level` CHECK (`authorization_level` >= 0),
    CONSTRAINT `chk_role_data_scope` CHECK (`default_data_scope` IN ('SELF', 'DIRECT_REPORTS', 'REPORTING_TREE', 'PRIMARY_ORG', 'ORG_TREE', 'CUSTOM_ORGS', 'GLOBAL')),
    CONSTRAINT `chk_role_scope_type` CHECK (`scope_type` IN ('GLOBAL', 'ORGANIZATION')),
    CONSTRAINT `chk_role_enabled` CHECK (`enabled` IN (0, 1)),
    CONSTRAINT `chk_role_version` CHECK (`version` >= 0)
) ENGINE = InnoDB
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT = '角色表'
  ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of t_role
-- ----------------------------
INSERT INTO `t_role` (`role`, `role_name`, `enabled`) VALUES
('admin', '系统管理员', 1),
('sales_consultant', '销售顾问', 1),
('sales_manager', '销售经理', 1),
('marketing_specialist', '市场专员', 1),
('finance_specialist', '财务专员', 1),
('inventory_specialist', '库存专员', 1);

UPDATE `t_role`
SET `description` = `role_name`,
    `protected_role` = CASE WHEN `role` = 'admin' THEN 1 ELSE 0 END,
    `authorization_level` = CASE `role`
        WHEN 'admin' THEN 100
        WHEN 'sales_manager' THEN 60
        WHEN 'finance_specialist' THEN 50
        WHEN 'marketing_specialist' THEN 40
        WHEN 'inventory_specialist' THEN 40
        WHEN 'sales_consultant' THEN 30
        ELSE 0
    END,
    `default_data_scope` = CASE `role`
        WHEN 'admin' THEN 'GLOBAL'
        WHEN 'sales_manager' THEN 'REPORTING_TREE'
        WHEN 'marketing_specialist' THEN 'PRIMARY_ORG'
        WHEN 'finance_specialist' THEN 'PRIMARY_ORG'
        WHEN 'inventory_specialist' THEN 'PRIMARY_ORG'
        ELSE 'SELF'
    END;

-- ----------------------------
-- Table structure for t_role_permission
-- ----------------------------
DROP TABLE IF EXISTS `t_role_permission`;
CREATE TABLE `t_role_permission`
(
    `role_id`       int NOT NULL,
    `permission_id` int NOT NULL,
    `delegable`     tinyint(1) NOT NULL DEFAULT 0,
    `data_scope_code` varchar(32) NOT NULL DEFAULT 'SELF',
    PRIMARY KEY (`role_id`, `permission_id`) USING BTREE,
    INDEX `idx_role_permission_permission` (`permission_id` ASC) USING BTREE,
    CONSTRAINT `fk_role_permission_role` FOREIGN KEY (`role_id`) REFERENCES `t_role` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_role_permission_permission` FOREIGN KEY (`permission_id`) REFERENCES `t_permission` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `chk_role_permission_delegable` CHECK (`delegable` IN (0, 1)),
    CONSTRAINT `chk_role_permission_data_scope` CHECK (`data_scope_code` IN ('SELF', 'DIRECT_REPORTS', 'REPORTING_TREE', 'PRIMARY_ORG', 'ORG_TREE', 'CUSTOM_ORGS', 'GLOBAL'))
) ENGINE = InnoDB
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT = '角色权限关系表'
  ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of t_role_permission
-- ----------------------------
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

INSERT INTO `t_role_permission` (`role_id`, `permission_id`)
SELECT r.id, p.id FROM `t_role` r CROSS JOIN `t_permission` p
WHERE r.role IN ('sales_consultant', 'sales_manager', 'marketing_specialist', 'finance_specialist', 'inventory_specialist')
  AND p.code IN ('menu:ai', 'ai:assistant:use', 'ai:run:view', 'ai:tool:execute',
                 'ai:workflow:view', 'ai:workflow:manage',
                 'ai:proactive:view', 'ai:proactive:use');

INSERT INTO `t_role_permission` (`role_id`, `permission_id`)
SELECT r.id, p.id FROM `t_role` r CROSS JOIN `t_permission` p
WHERE r.role IN ('sales_consultant', 'sales_manager', 'marketing_specialist')
  AND p.code IN ('ai:proposal:confirm');

UPDATE `t_role_permission` rp
INNER JOIN `t_permission` p ON p.id = rp.permission_id
INNER JOIN `t_role` r ON r.id = rp.role_id
SET rp.delegable = p.delegable,
    rp.data_scope_code = r.default_data_scope;

-- ----------------------------
-- Table structure for t_tran
-- ----------------------------
DROP TABLE IF EXISTS `t_tran`;
CREATE TABLE `t_tran`
(
    `id`                int                                                           NOT NULL AUTO_INCREMENT COMMENT '主键，自动增长，交易ID',
    `tran_no`           varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '交易流水号',
    `customer_id`       int                                                           NULL DEFAULT NULL COMMENT '客户ID',
    `money`             decimal(10, 2)                                                NULL DEFAULT NULL COMMENT '交易金额',
    `expected_date`     datetime                                                      NULL DEFAULT NULL COMMENT '预计成交日期',
    `stage`             varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci  NULL DEFAULT NULL COMMENT '交易所处阶段',
    `description`       varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '交易描述',
    `next_contact_time` datetime                                                      NULL DEFAULT NULL COMMENT '下次联系时间',
    `create_time`       datetime                                                      NULL DEFAULT NULL COMMENT '创建时间',
    `create_by`         int                                                           NULL DEFAULT NULL COMMENT '创建人',
    `edit_time`         datetime                                                      NULL DEFAULT NULL COMMENT '编辑时间',
    `edit_by`           int                                                           NULL DEFAULT NULL COMMENT '编辑人',
    `version`           int                                                           NOT NULL DEFAULT 0 COMMENT '版本号',
    `promotion_id`      bigint                                                        NULL DEFAULT NULL COMMENT '结算使用的促销ID',
    `original_amount`   decimal(10, 2)                                                NULL DEFAULT NULL COMMENT '结算原价总和',
    `discount_amount`   decimal(10, 2)                                                NOT NULL DEFAULT 0 COMMENT '结算优惠金额',
    `promotion_snapshot` text                                                         NULL DEFAULT NULL COMMENT '结算时促销快照JSON',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE KEY `uk_tran_no` (`tran_no`),
    INDEX `t_tran_ibfk_1` (`customer_id` ASC) USING BTREE,
    INDEX `t_tran_ibfk_2` (`stage` ASC) USING BTREE,
    INDEX `t_tran_ibfk_3` (`create_by` ASC) USING BTREE,
    INDEX `t_tran_ibfk_4` (`edit_by` ASC) USING BTREE
) ENGINE = InnoDB
  AUTO_INCREMENT = 7
  CHARACTER SET = utf8mb3
  COLLATE = utf8mb3_general_ci COMMENT = '交易表'
  ROW_FORMAT = DYNAMIC;
-- ----------------------------
-- Records of t_tran
-- ----------------------------

INSERT INTO `t_tran`
(`id`, `tran_no`, `customer_id`, `money`, `expected_date`, `stage`,
 `description`, `next_contact_time`, `create_time`, `create_by`, `edit_time`, `edit_by`)
VALUES
(1, 'XS202604080001', 1, 386800.00, '2026-04-18 00:00:00', 'COMPLETED',
 '奥迪Q5L 40 TFSI豪华动感型，全款，含基础装潢包。', NULL,
 '2026-04-08 10:12:00', 2, '2026-04-18 16:40:00', 2),
(2, 'XS202605220001', 2, 579800.00, '2026-06-28 00:00:00', 'PAYMENT',
 '宝马X5 xDrive30Li尊享型，金融分期，已收定金，待支付首付款。', '2026-06-23 10:00:00',
 '2026-05-22 14:25:00', 2, '2026-06-20 11:30:00', 2),
(3, 'XS202606030001', 3, 439900.00, '2026-07-05 00:00:00', 'APPROVED',
 '宝马530Li领先型，优惠方案已审批，待客户确认合同。', '2026-06-22 15:30:00',
 '2026-06-03 09:45:00', 3, '2026-06-19 17:10:00', 4),
(4, 'XS202606120001', 4, 509800.00, '2026-07-20 00:00:00', 'PENDING',
 '奔驰E 300 L豪华型，企业购车，折扣超销售权限，等待经理审批。', '2026-06-24 14:00:00',
 '2026-06-12 11:06:00', 8, '2026-06-20 09:15:00', 8),
(5, 'XS202606180001', 5, 469800.00, '2026-07-12 00:00:00', 'QUOTATION',
 '奥迪A6L 45 TFSI臻选动感型，客户比较全款与分期方案。', '2026-06-23 16:00:00',
 '2026-06-18 13:40:00', 3, NULL, NULL),
(6, 'XS202605150001', 6, 398800.00, '2026-06-15 00:00:00', 'LOST',
 '雷克萨斯NX 350h创驰版，客户因交付周期选择其他门店。', NULL,
 '2026-05-15 10:30:00', 8, '2026-06-10 18:05:00', 8),
(7, 'XS202603110001', 7, 455800.00, '2026-03-28 00:00:00', 'COMPLETED',
 '奥迪A6L 45 TFSI，置换购车，已完成收款和车辆交付。', NULL,
 '2026-03-11 15:20:00', 2, '2026-03-28 17:30:00', 2),
(8, 'XS202605300001', 8, 488000.00, '2026-06-30 00:00:00', 'CANCELLED',
 '奔驰GLC 300 L动感型，客户主动取消，定金已原路退回。', NULL,
 '2026-05-30 12:10:00', 3, '2026-06-16 14:20:00', 3);

-- ----------------------------
-- Table structure for t_tran_history
-- ----------------------------
DROP TABLE IF EXISTS `t_tran_history`;
CREATE TABLE `t_tran_history`
(
    `id`            int            NOT NULL AUTO_INCREMENT COMMENT '主键，自动增长，交易记录ID',
    `tran_id`       int            NULL DEFAULT NULL COMMENT '交易ID',
    `stage`         varchar(32)    NULL DEFAULT NULL COMMENT '交易阶段',
    `reason`        varchar(500)   NULL DEFAULT NULL COMMENT '状态变化原因',
    `money`         decimal(10, 2) NULL DEFAULT NULL COMMENT '交易金额',
    `expected_date` datetime       NULL DEFAULT NULL COMMENT '交易预计成交时间',
    `create_time`   datetime       NULL DEFAULT NULL COMMENT '创建时间',
    `create_by`     int            NULL DEFAULT NULL COMMENT '创建人',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `t_tran_history_ibfk_1` (`tran_id` ASC) USING BTREE,
    INDEX `t_tran_history_ibfk_2` (`stage` ASC) USING BTREE,
    INDEX `t_tran_history_ibfk_3` (`create_by` ASC) USING BTREE
) ENGINE = InnoDB
  AUTO_INCREMENT = 7
  CHARACTER SET = utf8mb3
  COLLATE = utf8mb3_general_ci COMMENT = '交易历史记录表'
  ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of t_tran_history
-- ----------------------------

INSERT INTO `t_tran_history`
(`id`, `tran_id`, `stage`, `reason`, `money`, `expected_date`, `create_time`, `create_by`)
VALUES
(1, 1, 'QUOTATION', NULL, 386800.00, '2026-04-18 00:00:00', '2026-04-08 10:12:00', 2),
(2, 1, 'PENDING',   NULL, 386800.00, '2026-04-18 00:00:00', '2026-04-09 09:30:00', 2),
(3, 1, 'APPROVED',  NULL, 386800.00, '2026-04-18 00:00:00', '2026-04-09 15:20:00', 4),
(4, 1, 'PAYMENT',   NULL, 386800.00, '2026-04-18 00:00:00', '2026-04-10 10:10:00', 2),
(5, 1, 'COMPLETED', NULL, 386800.00, '2026-04-18 00:00:00', '2026-04-18 16:40:00', 2),
(6, 2, 'QUOTATION', NULL, 579800.00, '2026-06-28 00:00:00', '2026-05-22 14:25:00', 2),
(7, 2, 'PENDING',   NULL, 579800.00, '2026-06-28 00:00:00', '2026-06-02 11:00:00', 2),
(8, 2, 'APPROVED',  NULL, 579800.00, '2026-06-28 00:00:00', '2026-06-03 09:15:00', 4),
(9, 2, 'PAYMENT',   NULL, 579800.00, '2026-06-28 00:00:00', '2026-06-20 11:30:00', 2),
(10, 3, 'QUOTATION', NULL, 439900.00, '2026-07-05 00:00:00', '2026-06-03 09:45:00', 3),
(11, 3, 'PENDING',   NULL, 439900.00, '2026-07-05 00:00:00', '2026-06-18 16:20:00', 3),
(12, 3, 'APPROVED',  NULL, 439900.00, '2026-07-05 00:00:00', '2026-06-19 17:10:00', 4),
(13, 4, 'QUOTATION', NULL, 509800.00, '2026-07-20 00:00:00', '2026-06-12 11:06:00', 8),
(14, 4, 'PENDING',   NULL, 509800.00, '2026-07-20 00:00:00', '2026-06-20 09:15:00', 8),
(15, 6, 'QUOTATION', NULL, 398800.00, '2026-06-15 00:00:00', '2026-05-15 10:30:00', 8),
(16, 6, 'LOST',      '审批未通过', 398800.00, '2026-06-15 00:00:00', '2026-06-10 18:05:00', 8),
(17, 8, 'QUOTATION', NULL, 488000.00, '2026-06-30 00:00:00', '2026-05-30 12:10:00', 3),
(18, 8, 'CANCELLED', '客户取消', 488000.00, '2026-06-30 00:00:00', '2026-06-16 14:20:00', 3);

-- ----------------------------
-- Table structure for t_quote
-- ----------------------------
DROP TABLE IF EXISTS `t_quote`;
CREATE TABLE `t_quote`
(
    `id`                 BIGINT      NOT NULL AUTO_INCREMENT COMMENT '报价ID',
    `quote_no`           VARCHAR(64) NOT NULL COMMENT '报价单号',
    `customer_id`        INT         NOT NULL COMMENT '客户ID',
    `opportunity_id`     BIGINT       DEFAULT NULL COMMENT '商机ID',
    `current_version_id` BIGINT       DEFAULT NULL COMMENT '当前报价版本ID',
    `status`             VARCHAR(50) NOT NULL COMMENT '报价状态稳定编码',
    `remark`             VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `create_time`        DATETIME     DEFAULT NULL COMMENT '创建时间',
    `create_by`          INT          DEFAULT NULL COMMENT '创建人',
    `update_time`        DATETIME     DEFAULT NULL COMMENT '更新时间',
    `update_by`          INT          DEFAULT NULL COMMENT '更新人',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_quote_no` (`quote_no`),
    KEY `idx_quote_customer_status` (`customer_id`, `status`),
    KEY `idx_quote_current_version` (`current_version_id`),
    CONSTRAINT `chk_quote_status` CHECK (`status` IN (
        'DRAFT', 'PENDING_SUBMIT', 'PENDING_APPROVAL', 'REJECTED',
        'PENDING_CUSTOMER_CONFIRMATION', 'ACCEPTED', 'REFUSED',
        'EXPIRED', 'VOIDED', 'CONVERTED_TO_ORDER'
    ))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='报价主表';

-- ----------------------------
-- Table structure for t_quote_version
-- ----------------------------
DROP TABLE IF EXISTS `t_quote_version`;
CREATE TABLE `t_quote_version`
(
    `id`           BIGINT         NOT NULL AUTO_INCREMENT COMMENT '报价版本ID',
    `quote_id`     BIGINT         NOT NULL COMMENT '报价ID',
    `version_no`   INT            NOT NULL COMMENT '报价内版本号',
    `valid_until`  DATETIME       NOT NULL COMMENT '报价有效期',
    `total_amount` DECIMAL(10, 2) NOT NULL COMMENT '报价总额',
    `remark`       VARCHAR(500)   DEFAULT NULL COMMENT '版本备注',
    `create_time`  DATETIME       DEFAULT NULL COMMENT '创建时间',
    `create_by`    INT            DEFAULT NULL COMMENT '创建人',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_quote_version_no` (`quote_id`, `version_no`),
    KEY `idx_quote_version_quote` (`quote_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='报价版本表';

-- ----------------------------
-- Table structure for t_quote_status_history
-- ----------------------------
DROP TABLE IF EXISTS `t_quote_status_history`;
CREATE TABLE `t_quote_status_history`
(
    `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '报价状态历史ID',
    `quote_id`    BIGINT      NOT NULL COMMENT '报价ID',
    `from_status` VARCHAR(50) DEFAULT NULL COMMENT '原报价状态',
    `to_status`   VARCHAR(50) NOT NULL COMMENT '目标报价状态',
    `reason`      VARCHAR(500) NOT NULL COMMENT '状态变化原因',
    `confirmed_by_name` VARCHAR(100) DEFAULT NULL COMMENT '客户确认人',
    `confirmed_at` DATETIME DEFAULT NULL COMMENT '客户确认时间',
    `confirmation_method` VARCHAR(50) DEFAULT NULL COMMENT '客户确认方式',
    `confirmation_evidence` VARCHAR(500) DEFAULT NULL COMMENT '客户确认凭证',
    `proxy_confirm_reason` VARCHAR(500) DEFAULT NULL COMMENT '代确认原因',
    `create_time` DATETIME    DEFAULT NULL COMMENT '创建时间',
    `create_by`   INT         DEFAULT NULL COMMENT '创建人',
    PRIMARY KEY (`id`),
    KEY `idx_quote_status_history_quote` (`quote_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='报价状态历史表';

-- ----------------------------
-- Table structure for t_tran_product
-- ----------------------------
DROP TABLE IF EXISTS `t_tran_product`;
CREATE TABLE `t_tran_product`
(
    `id`          int            NOT NULL AUTO_INCREMENT COMMENT '主键',
    `tran_id`     int            NOT NULL COMMENT '交易ID',
    `product_id`  bigint         NOT NULL COMMENT '产品ID',
    `quantity`    int            NOT NULL COMMENT '数量',
    `price`       decimal(10, 2) NOT NULL COMMENT '单价',
    `product_sku` varchar(100)   NULL DEFAULT NULL COMMENT '商品编码快照',
    `product_name` varchar(255)  NULL DEFAULT NULL COMMENT '商品名称快照',
    `product_specification` varchar(255) NULL DEFAULT NULL COMMENT '商品配置快照',
    `guide_price` decimal(10, 2) NULL DEFAULT NULL COMMENT '指导价快照',
    `create_time` datetime       NULL DEFAULT NULL COMMENT '创建时间',
    `create_by`   int            NULL DEFAULT NULL COMMENT '创建人',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `t_tran_product_ibfk_1` (`tran_id` ASC) USING BTREE,
    INDEX `t_tran_product_ibfk_2` (`product_id` ASC) USING BTREE,
    INDEX `t_tran_product_ibfk_3` (`create_by` ASC) USING BTREE
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  CHARACTER SET = utf8mb3
  COLLATE = utf8mb3_general_ci COMMENT = '交易产品关联表'
  ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of t_tran_product
-- ----------------------------

INSERT INTO `t_tran_product`
(`id`, `tran_id`, `product_id`, `quantity`, `price`, `product_sku`, `product_name`, `product_specification`, `guide_price`, `create_time`, `create_by`)
VALUES
(1, 1, 6, 1, 386800.00, 'AUDI-Q5L-FY-25-40', '奥迪 Q5L', '2025款 40 TFSI 豪华动感型', 426800.00, '2026-04-08 10:12:00', 2),
(2, 2, 1, 1, 579800.00, 'BMW-X5-G05-25-30LI', '宝马 X5', '2025款 xDrive30Li 尊享型 M运动套装', 599000.00, '2026-05-22 14:25:00', 2),
(3, 3, 4, 1, 439900.00, 'BMW-530LI-G68-25-L', '宝马 5系', '2025款 530Li 领先型 豪华套装', 485900.00, '2026-06-03 09:45:00', 3),
(4, 4, 9, 1, 509800.00, 'BENZ-E300L-W214-25', '奔驰 E级', '2025款 E 300 L 豪华型', 529800.00, '2026-06-12 11:06:00', 8),
(5, 5, 7, 1, 469800.00, 'AUDI-A6L-C8-25-45', '奥迪 A6L', '2025款 45 TFSI 臻选动感型', 479900.00, '2026-06-18 13:40:00', 3),
(6, 6, 12, 1, 398800.00, 'LEXUS-NX350H-25-C', '雷克萨斯 NX', '2025款 350h 创驰版', 388800.00, '2026-05-15 10:30:00', 8),
(7, 7, 7, 1, 455800.00, 'AUDI-A6L-C8-25-45', '奥迪 A6L', '2025款 45 TFSI 臻选动感型', 479900.00, '2026-03-11 15:20:00', 2),
(8, 8, 10, 1, 488000.00, 'BENZ-GLC300L-X254-25', '奔驰 GLC', '2025款 GLC 300 L 4MATIC 动感型', 479300.00, '2026-05-30 12:10:00', 3);

-- ----------------------------
-- Table structure for t_tran_invoice
-- ----------------------------
DROP TABLE IF EXISTS `t_tran_invoice`;
CREATE TABLE `t_tran_invoice`
(
    `id`           int            NOT NULL AUTO_INCREMENT COMMENT '主键',
    `tran_id`      int            NOT NULL COMMENT '交易ID',
    `invoice_no`   varchar(32)    NOT NULL COMMENT '发票号码',
    `type`         varchar(20)    NOT NULL COMMENT '发票类型：VAT_NORMAL-增值税普通发票, VAT_SPECIAL-增值税专用发票',
    `title`        varchar(128)   NOT NULL COMMENT '发票抬头',
    `tax_number`   varchar(32)    NOT NULL COMMENT '纳税人识别号',
    `bank_name`    varchar(128)   NULL DEFAULT NULL COMMENT '开户行',
    `bank_account` varchar(32)    NULL DEFAULT NULL COMMENT '银行账号',
    `address`      varchar(255)   NULL DEFAULT NULL COMMENT '注册地址',
    `phone`        varchar(20)    NULL DEFAULT NULL COMMENT '注册电话',
    `original_invoice_id` int      NULL DEFAULT NULL COMMENT '原发票ID，红冲或重开时关联原票/红冲记录',
    `amount`       decimal(10, 2) NOT NULL COMMENT '发票金额',
    `status`       varchar(20)    NOT NULL COMMENT '发票状态：PENDING-待开具, ISSUING-开票中, ISSUED-已开具, FAILED-开票失败, VOIDED-已作废, PARTIAL_RED_REVERSED-部分红冲, RED_REVERSED-已红冲, NOT_REQUIRED-无需开票',
    `remark`       varchar(255)   NULL DEFAULT NULL COMMENT '备注信息',
    `issue_time`   datetime       NULL DEFAULT NULL COMMENT '开具时间',
    `create_time`  datetime       NULL DEFAULT NULL COMMENT '创建时间',
    `create_by`    int            NULL DEFAULT NULL COMMENT '创建人',
    `edit_time`    datetime       NULL DEFAULT NULL COMMENT '编辑时间',
    `edit_by`      int            NULL DEFAULT NULL COMMENT '编辑人',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE KEY `uk_invoice_no` (`invoice_no`),
    INDEX `t_tran_invoice_ibfk_1` (`tran_id` ASC) USING BTREE,
    INDEX `idx_tran_invoice_original` (`original_invoice_id` ASC) USING BTREE,
    INDEX `t_tran_invoice_ibfk_2` (`create_by` ASC) USING BTREE,
    INDEX `t_tran_invoice_ibfk_3` (`edit_by` ASC) USING BTREE,
    CONSTRAINT `chk_tran_invoice_status` CHECK (`status` IN ('PENDING', 'ISSUING', 'ISSUED', 'FAILED', 'VOIDED', 'PARTIAL_RED_REVERSED', 'RED_REVERSED', 'NOT_REQUIRED'))
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  CHARACTER SET = utf8mb3
  COLLATE = utf8mb3_general_ci COMMENT = '交易发票表'
  ROW_FORMAT = DYNAMIC;


-- ----------------------------
-- Records of t_tran_invoice
-- ----------------------------
INSERT INTO `t_tran_invoice`
(`id`, `tran_id`, `invoice_no`, `type`, `title`, `tax_number`, `bank_name`,
 `bank_account`, `address`, `phone`, `original_invoice_id`, `amount`, `status`, `remark`, `issue_time`,
 `create_time`, `create_by`, `edit_time`, `edit_by`)
VALUES
(1, 1, '044002600111', 'VAT_NORMAL', '陈明', 'PERS-440305-0001', NULL, NULL, NULL,
 '13800002001', NULL, 386800.00, 'ISSUED', '机动车销售统一发票已随交车资料交付。',
 '2026-04-12 10:25:00', '2026-04-10 15:10:00', 6, '2026-04-12 10:25:00', 6),
(2, 7, '044002600086', 'VAT_SPECIAL', '深圳市远景建筑设计有限公司',
 '91440300MA5F8X2K7R', '招商银行深圳科技园支行', '755900010012345',
 '深圳市南山区科苑路88号', '0755-86661234', NULL, 455800.00, 'ISSUED',
 '企业抬头及税号已由财务复核。', '2026-03-20 09:35:00',
 '2026-03-19 16:20:00', 6, '2026-03-20 09:35:00', 6);

-- ----------------------------
-- Table structure for t_product
-- ----------------------------
DROP TABLE IF EXISTS `t_product`;
CREATE TABLE `t_product`
(
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '商品的唯一标识符',
    `sku`           VARCHAR(255) NOT NULL COMMENT '商品的库存单位',
    `name`          VARCHAR(255) NOT NULL COMMENT '商品名称',
  `category_id` BIGINT DEFAULT NULL COMMENT '商品类别ID',
    `specification` VARCHAR(255)   DEFAULT NULL COMMENT '商品规格',
    `price`         DECIMAL(10, 2) NOT NULL DEFAULT 0 COMMENT '商品价格',
    `stock`         INT          NOT NULL DEFAULT 0 COMMENT '当前商品库存量',
    `min_stock`     INT            DEFAULT NULL COMMENT '商品的最低库存警戒值',
    `status`        VARCHAR(50)  NOT NULL DEFAULT 'OFF_SALE' COMMENT '商品状态，如ON_SALE、OFF_SALE',
    `create_time`   DATETIME       DEFAULT NULL COMMENT '商品信息的创建时间',
    `update_time`   DATETIME       DEFAULT NULL COMMENT '商品信息的最后更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_product_sku` (`sku`),
    CONSTRAINT `chk_product_price_nonneg` CHECK (`price` >= 0),
    CONSTRAINT `chk_product_stock_nonneg` CHECK (`stock` >= 0),
    CONSTRAINT `chk_product_status_code` CHECK (`status` IN ('ON_SALE', 'OFF_SALE'))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='商品表';


INSERT INTO `t_product`
(`id`, `sku`, `name`, `category_id`, `specification`, `price`, `stock`, `min_stock`, `status`, `create_time`, `update_time`)
VALUES
(1, 'BMW-X5-G05-25-30LI', '宝马 X5', 1, '2025款 xDrive30Li 尊享型 M运动套装', 599000.00, 3, 2, 'ON_SALE', '2025-09-05 10:00:00', '2026-06-18 15:20:00'),
(2, 'BMW-X3-G48-25-30LI', '宝马 X3', 1, '2025款 xDrive30Li 尊享型', 449900.00, 5, 2, 'ON_SALE', '2025-11-12 10:00:00', '2026-06-18 15:20:00'),
(3, 'BMW-325LI-G28-25-M', '宝马 3系', 2, '2025款 325Li M运动曜夜套装', 369900.00, 4, 2, 'ON_SALE', '2025-08-18 10:00:00', '2026-06-18 15:20:00'),
(4, 'BMW-530LI-G68-25-L', '宝马 5系', 2, '2025款 530Li 领先型 豪华套装', 485900.00, 2, 2, 'ON_SALE', '2025-10-09 10:00:00', '2026-06-18 15:20:00'),
(5, 'AUDI-A4L-B9-25-40', '奥迪 A4L', 2, '2025款 40 TFSI 豪华动感型', 343800.00, 6, 2, 'ON_SALE', '2025-07-21 10:00:00', '2026-06-18 15:20:00'),
(6, 'AUDI-Q5L-FY-25-40', '奥迪 Q5L', 1, '2025款 40 TFSI 豪华动感型', 426800.00, 4, 2, 'ON_SALE', '2025-07-21 10:00:00', '2026-06-18 15:20:00'),
(7, 'AUDI-A6L-C8-25-45', '奥迪 A6L', 2, '2025款 45 TFSI 臻选动感型', 479900.00, 3, 2, 'ON_SALE', '2025-09-16 10:00:00', '2026-06-18 15:20:00'),
(8, 'BENZ-C260L-W206-25', '奔驰 C级', 2, '2025款 C 260 L 运动版', 353300.00, 5, 2, 'ON_SALE', '2025-08-08 10:00:00', '2026-06-18 15:20:00'),
(9, 'BENZ-E300L-W214-25', '奔驰 E级', 2, '2025款 E 300 L 豪华型', 529800.00, 2, 2, 'ON_SALE', '2025-10-15 10:00:00', '2026-06-18 15:20:00'),
(10, 'BENZ-GLC300L-X254-25', '奔驰 GLC', 1, '2025款 GLC 300 L 4MATIC 动感型', 479300.00, 4, 2, 'ON_SALE', '2025-10-15 10:00:00', '2026-06-18 15:20:00'),
(11, 'LEXUS-ES300H-25-P', '雷克萨斯 ES', 4, '2025款 300h 尊享版', 399900.00, 3, 1, 'ON_SALE', '2025-12-02 10:00:00', '2026-06-18 15:20:00'),
(12, 'LEXUS-NX350H-25-C', '雷克萨斯 NX', 4, '2025款 350h 创驰版', 388800.00, 2, 1, 'ON_SALE', '2025-12-02 10:00:00', '2026-06-18 15:20:00');

-- ----------------------------
-- Table structure for t_opportunity
-- ----------------------------
DROP TABLE IF EXISTS `t_opportunity_stage_history`;
DROP TABLE IF EXISTS `t_opportunity`;
CREATE TABLE `t_opportunity`
(
    `id`                  BIGINT        NOT NULL AUTO_INCREMENT COMMENT '商机ID',
    `opportunity_no`      VARCHAR(64)   NOT NULL COMMENT '商机业务编号',
    `customer_id`         INT           NOT NULL COMMENT '客户ID',
    `clue_id`             INT           DEFAULT NULL COMMENT '来源线索ID',
    `owner_id`            INT           NOT NULL COMMENT '商机负责人',
    `product_id`          BIGINT        DEFAULT NULL COMMENT '意向车型商品ID',
    `source_type`         VARCHAR(64)   DEFAULT NULL COMMENT '商机来源稳定编码',
    `stage`               VARCHAR(50)   NOT NULL COMMENT '商机销售阶段稳定编码',
    `requirement`         VARCHAR(1000) NOT NULL COMMENT '购车需求',
    `expected_amount`     DECIMAL(10, 2) DEFAULT NULL COMMENT '销售预测金额',
    `expected_close_date` DATE          DEFAULT NULL COMMENT '预计成交日期',
    `next_action_time`    DATE          DEFAULT NULL COMMENT '下一步跟进日期',
    `last_follow_time`    DATETIME      DEFAULT NULL COMMENT '最近跟进时间',
    `last_follow_summary` VARCHAR(255)  DEFAULT NULL COMMENT '最近跟进摘要',
    `lost_reason`         VARCHAR(500)  DEFAULT NULL COMMENT '输单/搁置/关闭原因',
    `lost_competitor`     VARCHAR(255)  DEFAULT NULL COMMENT '输单竞品',
    `result_remark`       VARCHAR(500)  DEFAULT NULL COMMENT '结果备注',
    `order_tran_id`       INT           DEFAULT NULL COMMENT '赢单关联交易ID',
    `version`             INT           NOT NULL DEFAULT 0 COMMENT '并发版本',
    `create_time`         DATETIME      DEFAULT NULL COMMENT '创建时间',
    `create_by`           INT           DEFAULT NULL COMMENT '创建人',
    `update_time`         DATETIME      DEFAULT NULL COMMENT '更新时间',
    `update_by`           INT           DEFAULT NULL COMMENT '更新人',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_opportunity_no` (`opportunity_no`),
    KEY `idx_opportunity_customer_stage` (`customer_id`, `stage`),
    KEY `idx_opportunity_owner_stage` (`owner_id`, `stage`),
    KEY `idx_opportunity_product` (`product_id`),
    KEY `idx_opportunity_order_tran` (`order_tran_id`),
    CONSTRAINT `chk_opportunity_stage` CHECK (`stage` IN (
        'INITIAL_CONTACT', 'NEEDS_ANALYSIS', 'VEHICLE_MATCHING', 'TEST_DRIVE_INVITED',
        'QUOTING', 'NEGOTIATION', 'PENDING_APPROVAL', 'WON', 'LOST', 'SHELVED', 'CLOSED'
    )),
    CONSTRAINT `chk_opportunity_expected_amount` CHECK (`expected_amount` IS NULL OR `expected_amount` >= 0)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='商机表';

CREATE TABLE `t_opportunity_stage_history`
(
    `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '商机阶段历史ID',
    `opportunity_id` BIGINT       NOT NULL COMMENT '商机ID',
    `from_stage`     VARCHAR(50)  DEFAULT NULL COMMENT '原阶段',
    `to_stage`       VARCHAR(50)  NOT NULL COMMENT '目标阶段',
    `reason`         VARCHAR(500) NOT NULL COMMENT '推进依据或原因',
    `operate_by`     INT          NOT NULL COMMENT '操作人',
    `operate_time`   DATETIME     NOT NULL COMMENT '操作时间',
    PRIMARY KEY (`id`),
    KEY `idx_opportunity_history_opportunity` (`opportunity_id`, `operate_time`),
    CONSTRAINT `chk_opportunity_history_to_stage` CHECK (`to_stage` IN (
        'INITIAL_CONTACT', 'NEEDS_ANALYSIS', 'VEHICLE_MATCHING', 'TEST_DRIVE_INVITED',
        'QUOTING', 'NEGOTIATION', 'PENDING_APPROVAL', 'WON', 'LOST', 'SHELVED', 'CLOSED'
    )),
    CONSTRAINT `chk_opportunity_history_from_stage` CHECK (`from_stage` IS NULL OR `from_stage` IN (
        'INITIAL_CONTACT', 'NEEDS_ANALYSIS', 'VEHICLE_MATCHING', 'TEST_DRIVE_INVITED',
        'QUOTING', 'NEGOTIATION', 'PENDING_APPROVAL', 'WON', 'LOST', 'SHELVED', 'CLOSED'
    ))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='商机阶段历史表';

-- ----------------------------
-- Table structure for t_quote_version_item
-- ----------------------------
DROP TABLE IF EXISTS `t_quote_version_item`;
CREATE TABLE `t_quote_version_item`
(
    `id`                    BIGINT         NOT NULL AUTO_INCREMENT COMMENT '报价版本商品ID',
    `quote_version_id`      BIGINT         NOT NULL COMMENT '报价版本ID',
    `product_id`            BIGINT         NOT NULL COMMENT '商品ID',
    `product_sku`           VARCHAR(100)   DEFAULT NULL COMMENT '商品编码快照',
    `product_name`          VARCHAR(255)   DEFAULT NULL COMMENT '商品名称快照',
    `product_specification` VARCHAR(255)   DEFAULT NULL COMMENT '商品配置快照',
    `guide_price`           DECIMAL(10, 2) DEFAULT NULL COMMENT '指导价快照',
    `unit_price`            DECIMAL(10, 2) NOT NULL COMMENT '报价单价',
    `quantity`              INT            NOT NULL COMMENT '数量',
    `line_amount`           DECIMAL(10, 2) NOT NULL COMMENT '行金额',
    `promotion_id`          BIGINT         DEFAULT NULL COMMENT '促销ID快照',
    `promotion_code`        VARCHAR(64)    DEFAULT NULL COMMENT '促销编码快照',
    `promotion_name`        VARCHAR(255)   DEFAULT NULL COMMENT '促销名称快照',
    `promotion_rule_summary` VARCHAR(500)  DEFAULT NULL COMMENT '促销规则摘要快照',
    `promotion_amount`      DECIMAL(10, 2) DEFAULT NULL COMMENT '促销金额快照',
    `promotion_snapshot`    TEXT           DEFAULT NULL COMMENT '促销完整快照JSON',
    `create_time`           DATETIME       DEFAULT NULL COMMENT '创建时间',
    `create_by`             INT            DEFAULT NULL COMMENT '创建人',
    PRIMARY KEY (`id`),
    KEY `idx_quote_item_version` (`quote_version_id`),
    KEY `idx_quote_item_product` (`product_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='报价版本商品快照表';

-- ----------------------------
-- Table structure for t_product_category

-- ----------------------------
-- Table structure for t_product_category
-- ----------------------------
DROP TABLE IF EXISTS `t_product_category`;
CREATE TABLE `t_product_category`
(
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '分类的唯一标识符',
    `name`        VARCHAR(255) NOT NULL COMMENT '分类名称',
    `code`        VARCHAR(100) DEFAULT NULL COMMENT '分类编码',
    `description` TEXT         DEFAULT NULL COMMENT '分类描述',
    `sort`        INT          DEFAULT 0 COMMENT '排序字段',
    `status`      VARCHAR(50)  DEFAULT NULL COMMENT '状态，如启用、禁用',
    `create_time` DATETIME     DEFAULT NULL COMMENT '创建时间',
    `update_time` DATETIME     DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_category_code` (`code`) COMMENT '分类编码唯一约束'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='产品分类表';

-- ----------------------------
-- Records of t_product_category
-- ----------------------------

INSERT INTO `t_product_category`
(`id`, `name`, `code`, `description`, `sort`, `status`, `create_time`, `update_time`)
VALUES
(1, 'SUV', 'SUV', '燃油及轻混运动型多用途乘用车', 1, '启用', '2025-01-06 09:00:00', '2026-06-18 15:00:00'),
(2, '轿车', 'SEDAN', '三厢或掀背乘用车', 2, '启用', '2025-01-06 09:00:00', '2026-06-18 15:00:00'),
(3, 'MPV', 'MPV', '多用途乘用车', 3, '启用', '2025-01-06 09:00:00', '2026-06-18 15:00:00'),
(4, '新能源车', 'NEW_ENERGY', '纯电、插混及油电混动车型', 4, '启用', '2025-01-06 09:00:00', '2026-06-18 15:00:00'),
(5, '性能车', 'PERFORMANCE', '高性能及个性化车型', 5, '启用', '2025-01-06 09:00:00', '2026-06-18 15:00:00');

ALTER TABLE `t_product`
    ADD CONSTRAINT `fk_product_category` FOREIGN KEY (`category_id`) REFERENCES `t_product_category` (`id`) ON DELETE RESTRICT;
ALTER TABLE `t_customer`
    ADD CONSTRAINT `fk_customer_product` FOREIGN KEY (`product`) REFERENCES `t_product` (`id`) ON DELETE RESTRICT;

-- ----------------------------
-- Table structure for t_product_promotion
-- ----------------------------
DROP TABLE IF EXISTS `t_product_promotion_usage`;
DROP TABLE IF EXISTS `t_product_promotion`;
CREATE TABLE `t_product_promotion`
(
    `id`                 BIGINT       NOT NULL AUTO_INCREMENT COMMENT '促销活动的唯一标识符',
    `product_id`         BIGINT       NOT NULL COMMENT '关联的产品ID',
    `code`               VARCHAR(64)  NOT NULL COMMENT '促销稳定编码',
    `name`               VARCHAR(255) NOT NULL COMMENT '促销名称',
    `type`               VARCHAR(50)  NOT NULL COMMENT '促销类型稳定编码',
    `discount`           DECIMAL(10, 2) NOT NULL DEFAULT 0 COMMENT '折扣比例、金额或权益成本',
    `rule_summary`       VARCHAR(500) NOT NULL COMMENT '促销规则摘要',
    `applicable_store`   VARCHAR(64)  NOT NULL DEFAULT 'ALL' COMMENT '适用门店范围',
    `customer_type`      VARCHAR(64)  NOT NULL DEFAULT 'ALL' COMMENT '适用客户类型',
    `applicable_channel` VARCHAR(64)  NOT NULL DEFAULT 'ALL' COMMENT '适用渠道',
    `inventory_scope`    VARCHAR(64)  NOT NULL DEFAULT 'ALL' COMMENT '适用库存范围',
    `stackable`          TINYINT      NOT NULL DEFAULT 0 COMMENT '是否允许叠加',
    `priority`           INT          NOT NULL DEFAULT 0 COMMENT '叠加优先级',
    `budget_limit`       DECIMAL(10, 2) DEFAULT NULL COMMENT '预算上限',
    `used_budget`        DECIMAL(10, 2) NOT NULL DEFAULT 0 COMMENT '已使用预算',
    `usage_limit`        INT            DEFAULT NULL COMMENT '使用名额上限',
    `used_count`         INT          NOT NULL DEFAULT 0 COMMENT '已使用次数',
    `start_time`         DATETIME     NOT NULL COMMENT '促销开始时间',
    `end_time`           DATETIME     NOT NULL COMMENT '促销结束时间',
    `status`             VARCHAR(50)  NOT NULL COMMENT '促销状态稳定编码',
    `pause_reason`       VARCHAR(500) DEFAULT NULL COMMENT '暂停原因',
    `end_reason`         VARCHAR(500) DEFAULT NULL COMMENT '结束原因',
    `void_reason`        VARCHAR(500) DEFAULT NULL COMMENT '作废原因',
    `create_time`        DATETIME     DEFAULT NULL COMMENT '创建时间',
    `update_time`        DATETIME     DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_product_promotion_code` (`code`),
    KEY `idx_product_promotion_product` (`product_id`),
    KEY `idx_product_promotion_status_time` (`status`, `start_time`, `end_time`),
    CONSTRAINT `fk_product_promotion_product` FOREIGN KEY (`product_id`) REFERENCES `t_product` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `chk_product_promotion_status` CHECK (`status` IN ('DRAFT', 'PENDING_EFFECTIVE', 'ACTIVE', 'PAUSED', 'ENDED', 'VOIDED', 'EXHAUSTED')),
    CONSTRAINT `chk_product_promotion_type` CHECK (`type` IN ('AMOUNT', 'PERCENTAGE', 'EXCHANGE_SUBSIDY', 'FINANCE_SUBSIDY', 'GIFT', 'MAINTENANCE', 'INSURANCE_SUBSIDY', 'LIMITED_TIME', 'INVENTORY_CLEARANCE')),
    CONSTRAINT `chk_product_promotion_time` CHECK (`end_time` > `start_time`),
    CONSTRAINT `chk_product_promotion_discount` CHECK (
        (`type` = 'PERCENTAGE' AND `discount` > 0 AND `discount` < 1)
        OR (`type` IN ('AMOUNT', 'EXCHANGE_SUBSIDY', 'FINANCE_SUBSIDY', 'INSURANCE_SUBSIDY', 'LIMITED_TIME', 'INVENTORY_CLEARANCE') AND `discount` > 0)
        OR (`type` IN ('GIFT', 'MAINTENANCE') AND `discount` >= 0)
    ),
    CONSTRAINT `chk_product_promotion_budget` CHECK (`budget_limit` IS NULL OR (`budget_limit` > 0 AND `used_budget` <= `budget_limit`)),
    CONSTRAINT `chk_product_promotion_usage` CHECK (`usage_limit` IS NULL OR (`usage_limit` > 0 AND `used_count` <= `usage_limit`)),
    CONSTRAINT `chk_product_promotion_used_non_negative` CHECK (`used_budget` >= 0 AND `used_count` >= 0)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='产品促销表';

-- ----------------------------
-- Records of t_product_promotion
-- ----------------------------

INSERT INTO `t_product_promotion`
(`id`, `product_id`, `code`, `name`, `type`, `discount`, `rule_summary`, `applicable_store`,
 `customer_type`, `applicable_channel`, `inventory_scope`, `stackable`, `priority`, `budget_limit`,
 `used_budget`, `usage_limit`, `used_count`, `start_time`, `end_time`, `status`, `create_time`, `update_time`)
VALUES
(1, 1, 'PROMO-BMW-X5-202606', '宝马X5六月现车补贴', 'AMOUNT', 12000.00, '每台现车直减12000元', 'ALL', 'ALL', 'ALL', 'IN_STOCK', 0, 10, 300000.00, 0.00, 20, 0, '2026-06-01 00:00:00', '2026-06-30 23:59:59', 'ACTIVE', '2026-05-20 10:00:00', '2026-06-01 09:00:00'),
(2, 11, 'PROMO-LEXUS-ES-202606', '雷克萨斯ES新能源置换补贴', 'EXCHANGE_SUBSIDY', 8000.00, '符合置换客户每台补贴8000元', 'ALL', 'REPLACEMENT', 'ALL', 'ALL', 0, 8, 200000.00, 0.00, 25, 0, '2026-06-15 00:00:00', '2026-08-31 23:59:59', 'ACTIVE', '2026-06-05 14:00:00', '2026-06-15 09:00:00'),
(3, 9, 'PROMO-BENZ-E-FINANCE-202607', '奔驰E级暑期金融贴息', 'FINANCE_SUBSIDY', 6000.00, '金融方案成交每台贴息6000元', 'ALL', 'ALL', 'ALL', 'ALL', 0, 5, 180000.00, 0.00, 30, 0, '2026-07-01 00:00:00', '2026-08-31 23:59:59', 'PENDING_EFFECTIVE', '2026-06-18 11:00:00', '2026-06-18 11:00:00');

CREATE TABLE `t_product_promotion_usage`
(
    `id`              BIGINT         NOT NULL AUTO_INCREMENT COMMENT '促销使用流水ID',
    `promotion_id`    BIGINT         NOT NULL COMMENT '促销ID',
    `source_type`     VARCHAR(50)    NOT NULL COMMENT '来源类型，如QUOTE或TRAN',
    `source_id`       BIGINT         NOT NULL COMMENT '来源业务ID',
    `discount_amount` DECIMAL(10, 2) NOT NULL DEFAULT 0 COMMENT '本次优惠金额',
    `create_time`     DATETIME       NOT NULL COMMENT '创建时间',
    `create_by`       INT            DEFAULT NULL COMMENT '操作人',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_product_promotion_usage_source` (`promotion_id`, `source_type`, `source_id`),
    KEY `idx_product_promotion_usage_source` (`source_type`, `source_id`),
    CONSTRAINT `fk_product_promotion_usage_promotion` FOREIGN KEY (`promotion_id`) REFERENCES `t_product_promotion` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `chk_product_promotion_usage_amount` CHECK (`discount_amount` >= 0)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='促销使用流水表';

-- ----------------------------
-- Table structure for t_product_stock_record
-- ----------------------------
DROP TABLE IF EXISTS `t_delivery_check_item`;
DROP TABLE IF EXISTS `t_delivery`;
DROP TABLE IF EXISTS `t_test_drive_status_history`;
DROP TABLE IF EXISTS `t_test_drive_vehicle_hold`;
DROP TABLE IF EXISTS `t_test_drive`;
DROP TABLE IF EXISTS `t_product_vehicle`;
CREATE TABLE `t_product_vehicle`
(
    `id`            BIGINT      NOT NULL AUTO_INCREMENT COMMENT '库存车辆实例ID',
    `product_id`    BIGINT      NOT NULL COMMENT '关联商品ID',
    `vin`           VARCHAR(64) NOT NULL COMMENT '车辆唯一识别码',
    `color`         VARCHAR(64) NOT NULL COMMENT '车辆颜色',
    `configuration` VARCHAR(255) DEFAULT NULL COMMENT '车辆配置',
    `location`      VARCHAR(128) NOT NULL COMMENT '门店或库位',
    `status`        VARCHAR(50) NOT NULL COMMENT '库存车辆状态',
    `hold_type`     VARCHAR(50) DEFAULT NULL COMMENT '占用类型',
    `source_type`   VARCHAR(50) DEFAULT NULL COMMENT '业务来源类型',
    `source_id`     BIGINT DEFAULT NULL COMMENT '业务来源ID',
    `hold_until`    DATETIME DEFAULT NULL COMMENT '预计释放或交付时间',
    `create_time`   DATETIME DEFAULT NULL COMMENT '创建时间',
    `create_by`     INT DEFAULT NULL COMMENT '创建人',
    `update_time`   DATETIME DEFAULT NULL COMMENT '更新时间',
    `update_by`     INT DEFAULT NULL COMMENT '更新人',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_product_vehicle_vin` (`vin`),
    KEY `idx_product_vehicle_product_status` (`product_id`, `status`),
    KEY `idx_product_vehicle_source` (`source_type`, `source_id`),
    CONSTRAINT `chk_product_vehicle_status` CHECK (`status` IN (
        'PENDING_INBOUND', 'AVAILABLE', 'TEST_DRIVE_RESERVED', 'SALES_LOCKED',
        'ORDER_RESERVED', 'PENDING_DELIVERY', 'OUTBOUND', 'DELIVERED',
        'INVENTORY_EXCEPTION', 'UNAVAILABLE'
    ))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='库存车辆实例表';

DROP TABLE IF EXISTS `t_test_drive`;
CREATE TABLE `t_test_drive`
(
    `id`                      BIGINT       NOT NULL AUTO_INCREMENT COMMENT '试驾记录ID',
    `test_drive_no`           VARCHAR(64)  NOT NULL COMMENT '试驾业务编号',
    `customer_id`             INT          NOT NULL COMMENT '客户ID',
    `opportunity_id`          BIGINT       DEFAULT NULL COMMENT '关联商机ID',
    `vehicle_id`              BIGINT       NOT NULL COMMENT '试驾车辆实例ID',
    `owner_id`                INT          NOT NULL COMMENT '负责销售',
    `planned_start_time`      DATETIME     NOT NULL COMMENT '预约开始时间',
    `planned_end_time`        DATETIME     NOT NULL COMMENT '预约结束时间',
    `actual_arrive_time`      DATETIME     DEFAULT NULL COMMENT '到店时间',
    `actual_start_time`       DATETIME     DEFAULT NULL COMMENT '实际开始时间',
    `actual_end_time`         DATETIME     DEFAULT NULL COMMENT '实际结束时间',
    `safety_confirmed_at`     DATETIME     DEFAULT NULL COMMENT '安全确认时间',
    `safety_confirmed_by`     INT          DEFAULT NULL COMMENT '安全确认人',
    `check_in_by`             INT          DEFAULT NULL COMMENT '签到人',
    `customer_confirm_method` VARCHAR(50)  DEFAULT NULL COMMENT '客户确认方式',
    `status`                  VARCHAR(50)  NOT NULL COMMENT '试驾状态稳定编码',
    `contact_name`            VARCHAR(100) NOT NULL COMMENT '客户联系人',
    `contact_phone`           VARCHAR(50)  NOT NULL COMMENT '客户联系电话',
    `result`                  VARCHAR(100) DEFAULT NULL COMMENT '试驾结果编码或摘要',
    `customer_feedback`       VARCHAR(1000) DEFAULT NULL COMMENT '客户反馈',
    `next_action`             VARCHAR(500) DEFAULT NULL COMMENT '下一步动作',
    `cancel_type`             VARCHAR(50)  DEFAULT NULL COMMENT '取消或爽约类型',
    `cancel_reason`           VARCHAR(500) DEFAULT NULL COMMENT '取消或爽约原因',
    `remark`                  VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `reschedule_count`        INT          NOT NULL DEFAULT 0 COMMENT '改期次数',
    `version`                 INT          NOT NULL DEFAULT 0 COMMENT '并发版本',
    `create_time`             DATETIME     DEFAULT NULL COMMENT '创建时间',
    `create_by`               INT          DEFAULT NULL COMMENT '创建人',
    `update_time`             DATETIME     DEFAULT NULL COMMENT '更新时间',
    `update_by`               INT          DEFAULT NULL COMMENT '更新人',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_test_drive_no` (`test_drive_no`),
    KEY `idx_test_drive_customer_status` (`customer_id`, `status`),
    KEY `idx_test_drive_opportunity` (`opportunity_id`),
    KEY `idx_test_drive_vehicle_time` (`vehicle_id`, `planned_start_time`, `planned_end_time`),
    KEY `idx_test_drive_owner_time` (`owner_id`, `planned_start_time`, `planned_end_time`),
    CONSTRAINT `chk_test_drive_status` CHECK (`status` IN (
        'PENDING_CONFIRM', 'SCHEDULED', 'RESCHEDULED', 'CHECKED_IN',
        'COMPLETED', 'CANCELED', 'NO_SHOW', 'EXCEPTION_CLOSED'
    )),
    CONSTRAINT `chk_test_drive_time_range` CHECK (`planned_start_time` < `planned_end_time`),
    CONSTRAINT `chk_test_drive_reschedule_count` CHECK (`reschedule_count` >= 0)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='试驾预约与执行表';

DROP TABLE IF EXISTS `t_test_drive_vehicle_hold`;
CREATE TABLE `t_test_drive_vehicle_hold`
(
    `id`            BIGINT      NOT NULL AUTO_INCREMENT COMMENT '试驾车辆时间占用ID',
    `test_drive_id` BIGINT      NOT NULL COMMENT '试驾记录ID',
    `vehicle_id`    BIGINT      NOT NULL COMMENT '试驾车辆实例ID',
    `start_time`    DATETIME    NOT NULL COMMENT '占用开始时间',
    `end_time`      DATETIME    NOT NULL COMMENT '占用结束时间',
    `status`        VARCHAR(30) NOT NULL COMMENT '占用状态',
    `release_reason` VARCHAR(500) DEFAULT NULL COMMENT '释放原因',
    `release_time`  DATETIME    DEFAULT NULL COMMENT '释放时间',
    `create_time`   DATETIME    DEFAULT NULL COMMENT '创建时间',
    `create_by`     INT         DEFAULT NULL COMMENT '创建人',
    `update_time`   DATETIME    DEFAULT NULL COMMENT '更新时间',
    `update_by`     INT         DEFAULT NULL COMMENT '更新人',
    PRIMARY KEY (`id`),
    KEY `idx_test_drive_hold_vehicle_time` (`vehicle_id`, `status`, `start_time`, `end_time`),
    KEY `idx_test_drive_hold_drive` (`test_drive_id`, `status`),
    CONSTRAINT `chk_test_drive_hold_status` CHECK (`status` IN ('ACTIVE', 'RELEASED')),
    CONSTRAINT `chk_test_drive_hold_time_range` CHECK (`start_time` < `end_time`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='试驾车辆时间占用表';

DROP TABLE IF EXISTS `t_test_drive_status_history`;
CREATE TABLE `t_test_drive_status_history`
(
    `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '试驾状态历史ID',
    `test_drive_id`  BIGINT       NOT NULL COMMENT '试驾记录ID',
    `from_status`    VARCHAR(50)  DEFAULT NULL COMMENT '原状态',
    `to_status`      VARCHAR(50)  NOT NULL COMMENT '目标状态',
    `action_type`    VARCHAR(50)  NOT NULL COMMENT '动作类型',
    `reason`         VARCHAR(500) DEFAULT NULL COMMENT '原因或摘要',
    `old_start_time` DATETIME     DEFAULT NULL COMMENT '原预约开始时间',
    `old_end_time`   DATETIME     DEFAULT NULL COMMENT '原预约结束时间',
    `new_start_time` DATETIME     DEFAULT NULL COMMENT '新预约开始时间',
    `new_end_time`   DATETIME     DEFAULT NULL COMMENT '新预约结束时间',
    `operate_by`     INT          NOT NULL COMMENT '操作人',
    `operate_time`   DATETIME     NOT NULL COMMENT '操作时间',
    PRIMARY KEY (`id`),
    KEY `idx_test_drive_history_drive` (`test_drive_id`, `operate_time`),
    CONSTRAINT `chk_test_drive_history_to_status` CHECK (`to_status` IN (
        'PENDING_CONFIRM', 'SCHEDULED', 'RESCHEDULED', 'CHECKED_IN',
        'COMPLETED', 'CANCELED', 'NO_SHOW', 'EXCEPTION_CLOSED'
    )),
    CONSTRAINT `chk_test_drive_history_from_status` CHECK (`from_status` IS NULL OR `from_status` IN (
        'PENDING_CONFIRM', 'SCHEDULED', 'RESCHEDULED', 'CHECKED_IN',
        'COMPLETED', 'CANCELED', 'NO_SHOW', 'EXCEPTION_CLOSED'
    ))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='试驾状态历史表';

-- ----------------------------
-- Table structure for t_follow_task
-- ----------------------------
DROP TABLE IF EXISTS `t_communication_record`;
DROP TABLE IF EXISTS `t_follow_task`;
CREATE TABLE `t_follow_task`
(
    `id`                      BIGINT       NOT NULL AUTO_INCREMENT COMMENT '跟进任务ID',
    `title`                   VARCHAR(128) NOT NULL COMMENT '任务标题',
    `task_type`               VARCHAR(64)  NOT NULL COMMENT '任务类型稳定编码',
    `related_object_type`     VARCHAR(32)  NOT NULL COMMENT '关联对象类型',
    `related_object_id`       BIGINT       NOT NULL COMMENT '关联对象ID',
    `owner_id`                INT          NOT NULL COMMENT '负责人',
    `priority`                VARCHAR(32)  NOT NULL DEFAULT 'NORMAL' COMMENT '优先级',
    `due_time`                DATETIME     NOT NULL COMMENT '计划时间',
    `remind_time`             DATETIME     DEFAULT NULL COMMENT '提醒时间',
    `status`                  VARCHAR(32)  NOT NULL COMMENT '任务状态',
    `result`                  VARCHAR(500) DEFAULT NULL COMMENT '完成结果',
    `postpone_reason`         VARCHAR(500) DEFAULT NULL COMMENT '延期原因',
    `original_due_time`       DATETIME     DEFAULT NULL COMMENT '原计划时间',
    `postpone_count`          INT          NOT NULL DEFAULT 0 COMMENT '延期次数',
    `cancel_reason`           VARCHAR(500) DEFAULT NULL COMMENT '取消原因',
    `communication_record_id` BIGINT       DEFAULT NULL COMMENT '完成关联沟通记录ID',
    `completed_time`          DATETIME     DEFAULT NULL COMMENT '完成时间',
    `completed_by`            INT          DEFAULT NULL COMMENT '完成人',
    `version`                 INT          NOT NULL DEFAULT 0 COMMENT '并发版本',
    `create_time`             DATETIME     DEFAULT NULL COMMENT '创建时间',
    `create_by`               INT          DEFAULT NULL COMMENT '创建人',
    `update_time`             DATETIME     DEFAULT NULL COMMENT '更新时间',
    `update_by`               INT          DEFAULT NULL COMMENT '更新人',
    PRIMARY KEY (`id`),
    KEY `idx_follow_task_owner_due` (`owner_id`, `status`, `due_time`, `id`),
    KEY `idx_follow_task_object` (`related_object_type`, `related_object_id`, `due_time`, `id`),
    CONSTRAINT `chk_follow_task_object_type` CHECK (`related_object_type` IN ('CLUE', 'CUSTOMER', 'OPPORTUNITY', 'TEST_DRIVE', 'ORDER')),
    CONSTRAINT `chk_follow_task_status` CHECK (`status` IN ('PENDING', 'IN_PROGRESS', 'POSTPONED', 'OVERDUE', 'COMPLETED', 'CANCELLED', 'CLOSED')),
    CONSTRAINT `chk_follow_task_priority` CHECK (`priority` IN ('LOW', 'NORMAL', 'HIGH', 'URGENT')),
    CONSTRAINT `chk_follow_task_postpone_count` CHECK (`postpone_count` >= 0)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='跟进任务表';

CREATE TABLE `t_communication_record`
(
    `id`                   BIGINT       NOT NULL AUTO_INCREMENT COMMENT '沟通记录ID',
    `follow_task_id`       BIGINT       DEFAULT NULL COMMENT '关联跟进任务ID',
    `parent_record_id`     BIGINT       DEFAULT NULL COMMENT '更正前沟通记录ID',
    `related_object_type`  VARCHAR(32)  NOT NULL COMMENT '关联对象类型',
    `related_object_id`    BIGINT       NOT NULL COMMENT '关联对象ID',
    `owner_id`             INT          NOT NULL COMMENT '负责人',
    `communication_method` VARCHAR(32)  NOT NULL COMMENT '沟通方式',
    `communication_time`   DATETIME     NOT NULL COMMENT '沟通时间',
    `summary`              VARCHAR(500) NOT NULL COMMENT '沟通摘要',
    `customer_feedback`    VARCHAR(500) DEFAULT NULL COMMENT '客户反馈',
    `next_action`          VARCHAR(500) DEFAULT NULL COMMENT '下一步动作',
    `next_follow_time`     DATETIME     DEFAULT NULL COMMENT '下次跟进时间',
    `status`               VARCHAR(32)  NOT NULL COMMENT '记录状态',
    `correction_reason`    VARCHAR(500) DEFAULT NULL COMMENT '更正原因',
    `void_reason`          VARCHAR(500) DEFAULT NULL COMMENT '作废原因',
    `version`              INT          NOT NULL DEFAULT 0 COMMENT '并发版本',
    `create_time`          DATETIME     DEFAULT NULL COMMENT '创建时间',
    `create_by`            INT          DEFAULT NULL COMMENT '创建人',
    `update_time`          DATETIME     DEFAULT NULL COMMENT '更新时间',
    `update_by`            INT          DEFAULT NULL COMMENT '更新人',
    PRIMARY KEY (`id`),
    KEY `idx_comm_record_owner_time` (`owner_id`, `communication_time`, `id`),
    KEY `idx_comm_record_object` (`related_object_type`, `related_object_id`, `communication_time`, `id`),
    KEY `idx_comm_record_task` (`follow_task_id`, `status`),
    KEY `idx_comm_record_parent` (`parent_record_id`),
    CONSTRAINT `fk_comm_record_task` FOREIGN KEY (`follow_task_id`) REFERENCES `t_follow_task` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_comm_record_parent` FOREIGN KEY (`parent_record_id`) REFERENCES `t_communication_record` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `chk_comm_record_object_type` CHECK (`related_object_type` IN ('CLUE', 'CUSTOMER', 'OPPORTUNITY', 'TEST_DRIVE', 'ORDER')),
    CONSTRAINT `chk_comm_record_status` CHECK (`status` IN ('ACTIVE', 'CORRECTED', 'VOIDED')),
    CONSTRAINT `chk_comm_record_method` CHECK (`communication_method` IN ('PHONE', 'STORE_VISIT', 'WECHAT', 'SMS', 'EMAIL', 'OTHER'))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='沟通记录表';

DROP TABLE IF EXISTS `t_product_stock_record`;
CREATE TABLE `t_product_stock_record`
(
    `id`                BIGINT NOT NULL AUTO_INCREMENT COMMENT '库存记录的唯一标识符',
    `product_id`        BIGINT NOT NULL COMMENT '关联的产品ID',
    `vehicle_id`        BIGINT DEFAULT NULL COMMENT '关联库存车辆实例ID',
    `quantity`          INT DEFAULT NULL COMMENT '变动数量',
    `type`              VARCHAR(50) DEFAULT NULL COMMENT '记录类型，如入库、占用、释放、出库',
    `source_type`       VARCHAR(50) DEFAULT NULL COMMENT '业务来源类型',
    `source_id`         BIGINT DEFAULT NULL COMMENT '业务来源ID',
    `before_status`     VARCHAR(50) DEFAULT NULL COMMENT '变更前车辆状态',
    `after_status`      VARCHAR(50) DEFAULT NULL COMMENT '变更后车辆状态',
    `related_record_id` BIGINT DEFAULT NULL COMMENT '关联原库存流水ID',
    `remark`            TEXT DEFAULT NULL COMMENT '备注信息',
    `create_time`       DATETIME DEFAULT NULL COMMENT '记录创建时间',
    `create_by`         INT DEFAULT NULL COMMENT '创建人',
    PRIMARY KEY (`id`),
    KEY `idx_product_id` (`product_id`) COMMENT '产品ID索引',
    KEY `idx_stock_record_vehicle` (`vehicle_id`),
    KEY `idx_stock_record_source` (`source_type`, `source_id`),
    KEY `idx_stock_record_related` (`related_record_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='产品库存记录表';


-- ----------------------------
-- Records of t_product_stock_record
-- ----------------------------
INSERT INTO `t_product_stock_record`
(`id`, `product_id`, `quantity`, `type`, `remark`, `create_time`)
VALUES
(1, 6, 5, 'INBOUND', '厂家到店批次 RK20260402，验收入库5台。', '2026-04-02 14:20:00'),
(2, 6, -1, 'OUTBOUND', '交易 XS202604080001 完成交付出库。', '2026-04-18 15:50:00'),
(3, 1, 4, 'INBOUND', '厂家到店批次 RK20260518，验收入库4台。', '2026-05-18 11:10:00'),
(4, 1, -1, 'RESERVE', '交易 XS202605220001 锁定现车1台，待收首付款。', '2026-06-20 11:35:00'),
(5, 7, 4, 'INBOUND', '厂家到店批次 RK20260220，验收入库4台。', '2026-02-20 16:30:00'),
(6, 7, -1, 'OUTBOUND', '交易 XS202603110001 完成交付出库。', '2026-03-28 16:55:00'),
(7, 9, 3, 'INBOUND', '厂家到店批次 RK20260608，验收入库3台。', '2026-06-08 13:45:00'),
(8, 9, -1, 'RESERVE', '交易 XS202606120001 预留现车1台，审批未完成。', '2026-06-20 09:20:00');

DROP TABLE IF EXISTS `t_delivery`;
CREATE TABLE `t_delivery`
(
    `id`                    BIGINT NOT NULL AUTO_INCREMENT COMMENT '交付记录ID',
    `tran_id`               INT    NOT NULL COMMENT '交易ID',
    `customer_id`           INT    NOT NULL COMMENT '客户ID',
    `vehicle_id`            BIGINT NOT NULL COMMENT '库存车辆实例ID',
    `status`                VARCHAR(50) NOT NULL COMMENT '交付状态',
    `planned_delivery_time` DATETIME NOT NULL COMMENT '预计交付时间',
    `actual_delivery_time`  DATETIME DEFAULT NULL COMMENT '实际交付时间',
    `responsible_user_id`   INT DEFAULT NULL COMMENT '负责人',
    `signer_name`           VARCHAR(100) DEFAULT NULL COMMENT '签收人',
    `signed_at`             DATETIME DEFAULT NULL COMMENT '签收时间',
    `sign_method`           VARCHAR(50) DEFAULT NULL COMMENT '签收方式',
    `sign_evidence`         VARCHAR(500) DEFAULT NULL COMMENT '签收凭证',
    `exception_type`        VARCHAR(50) DEFAULT NULL COMMENT '异常类型',
    `exception_reason`      VARCHAR(500) DEFAULT NULL COMMENT '异常或取消原因',
    `create_time`           DATETIME DEFAULT NULL COMMENT '创建时间',
    `create_by`             INT DEFAULT NULL COMMENT '创建人',
    `update_time`           DATETIME DEFAULT NULL COMMENT '更新时间',
    `update_by`             INT DEFAULT NULL COMMENT '更新人',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_delivery_tran` (`tran_id`),
    KEY `idx_delivery_customer` (`customer_id`),
    KEY `idx_delivery_vehicle` (`vehicle_id`),
    KEY `idx_delivery_status_time` (`status`, `planned_delivery_time`),
    CONSTRAINT `chk_delivery_status` CHECK (`status` IN (
        'PENDING_PREPARE', 'PREPARING', 'WAITING_CUSTOMER', 'WAITING_DELIVERY',
        'DELIVERING', 'SIGNED', 'COMPLETED', 'EXCEPTION', 'CANCELLED'
    ))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='交付记录表';

DROP TABLE IF EXISTS `t_delivery_check_item`;
CREATE TABLE `t_delivery_check_item`
(
    `id`                  BIGINT NOT NULL AUTO_INCREMENT COMMENT '交付准备项ID',
    `delivery_id`         BIGINT NOT NULL COMMENT '交付记录ID',
    `item_code`           VARCHAR(64) NOT NULL COMMENT '准备项编码',
    `item_name`           VARCHAR(100) NOT NULL COMMENT '准备项名称',
    `status`              VARCHAR(30) NOT NULL COMMENT '准备项状态',
    `responsible_user_id` INT DEFAULT NULL COMMENT '责任人',
    `completed_time`      DATETIME DEFAULT NULL COMMENT '完成时间',
    `remark`              VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `create_time`         DATETIME DEFAULT NULL COMMENT '创建时间',
    `create_by`           INT DEFAULT NULL COMMENT '创建人',
    `update_time`         DATETIME DEFAULT NULL COMMENT '更新时间',
    `update_by`           INT DEFAULT NULL COMMENT '更新人',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_delivery_check_item_code` (`delivery_id`, `item_code`),
    KEY `idx_delivery_check_status` (`delivery_id`, `status`),
    CONSTRAINT `chk_delivery_check_status` CHECK (`status` IN ('PENDING', 'COMPLETED', 'BLOCKED'))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='交付准备检查项表';

-- ----------------------------
-- Table structure for t_tran_approve
-- ----------------------------
DROP TABLE IF EXISTS `t_tran_approve`;
CREATE TABLE `t_tran_approve`
(
    `id`              int                                                           NOT NULL AUTO_INCREMENT COMMENT '主键，自动增长，审批ID',
    `tran_id`         int                                                           NOT NULL COMMENT '交易ID',
    `approve_result`  tinyint(1)                                                    NOT NULL COMMENT '审批结果：1-通过，0-拒绝',
    `approve_comment` varchar(500) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '审批意见',
    `approve_time`    datetime                                                      NULL DEFAULT NULL COMMENT '审批时间',
    `approve_by`      int                                                           NULL DEFAULT NULL COMMENT '审批人',
    `create_time`     datetime                                                      NULL DEFAULT NULL COMMENT '创建时间',
    `create_by`       int                                                           NULL DEFAULT NULL COMMENT '创建人',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE KEY `uk_tran_id` (`tran_id`),
    INDEX `idx_approve_by` (`approve_by` ASC) USING BTREE
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  CHARACTER SET = utf8mb3
  COLLATE = utf8mb3_general_ci COMMENT = '交易审批表'
  ROW_FORMAT = DYNAMIC;


-- ----------------------------
-- Records of t_tran_approve
-- ----------------------------
INSERT INTO `t_tran_approve`
(`id`, `tran_id`, `approve_result`, `approve_comment`, `approve_time`, `approve_by`, `create_time`, `create_by`)
VALUES
(1, 1, 1, '优惠金额在店长授权范围内，毛利满足门店要求，同意成交。', '2026-04-09 15:20:00', 4, '2026-04-09 09:30:00', 2),
(2, 2, 1, '金融方案和车辆资源已确认，同意锁车并进入收款阶段。', '2026-06-03 09:15:00', 4, '2026-06-02 11:00:00', 2),
(3, 3, 1, '客户职业及付款计划明确，同意最终报价。', '2026-06-19 17:10:00', 4, '2026-06-18 16:20:00', 3),
(4, 6, 0, '客户要求的交付日期无法保证，不建议继续占用车辆资源。', '2026-06-09 16:40:00', 4, '2026-06-09 10:15:00', 8);

DROP TABLE IF EXISTS `t_employee_reporting`;
DROP TABLE IF EXISTS `t_employee_assignment`;
DROP TABLE IF EXISTS `t_organization_unit`;
DROP TABLE IF EXISTS `t_position`;
DROP TABLE IF EXISTS `t_employee`;
DROP TABLE IF EXISTS `t_user_role`;
DROP TABLE IF EXISTS `t_credential_delivery_outbox`;
DROP TABLE IF EXISTS `t_account_credential`;
DROP TABLE IF EXISTS `t_password_history`;
DROP TABLE IF EXISTS `t_login_identifier`;
DROP TABLE IF EXISTS `t_user_session`;
DROP TABLE IF EXISTS `t_user`;
create table t_user
(
    id                     int auto_increment comment '主键，自动增长，用户ID'
        primary key,
    login_act              varchar(32) null comment '登录账号',
    login_pwd              varchar(64) null comment '登录密码',
    name                   varchar(64) null comment '用户姓名',
    phone                  varchar(18) null comment '用户手机',
    email                  varchar(64) null comment '用户邮箱',
    avatar_url             varchar(500) null comment '系统账号个人头像',
    profile_version        int not null default 0 comment '系统账号个人资料并发版本',
    account_no_expired     int         null comment '账户是否没有过期，0已过期 1正常',
    credentials_no_expired int         null comment '密码是否没有过期，0已过期 1正常',
    account_no_locked      int         null comment '账号是否没有锁定，0已锁定 1正常',
    account_enabled        int         null comment '账号是否启用，0禁用 1启用',
    create_time            datetime    null comment '创建时间',
    create_by              int         null comment '创建人',
    edit_time              datetime    null comment '编辑时间',
    edit_by                int         null comment '编辑人',
    last_login_time        datetime    null comment '最近登录时间',
    account_type           varchar(16) not null default 'HUMAN' comment '账号类型：SYSTEM-系统恢复账号，HUMAN-人员账号',
    protected_account      tinyint(1)  not null default 0 comment '是否为受保护恢复账号：0否，1是',
    version                int         not null default 0 comment '并发更新版本',
    authorization_version  int         not null default 0 comment '授权配置并发版本，仅授权事实变化时递增',
    auth_version           bigint      not null default 0 comment '认证安全版本，安全变更时递增',
    session_revision       bigint      not null default 0 comment '会话列表命令独立并发版本',
    account_status         varchar(16) not null default 'ACTIVE' comment 'INVITED/ACTIVE/DISABLED',
    must_change_password   tinyint(1) not null default 0 comment '是否必须首次改密',
    failed_login_count     int not null default 0 comment '连续登录失败次数',
    auto_locked_until      datetime null comment '自动锁定截止时间',
    manual_locked          tinyint(1) not null default 0 comment '人工锁定事实',
    manual_lock_reason     varchar(500) null comment '人工锁定原因',
    manual_locked_by       int null comment '人工锁定操作者',
    manual_locked_at       datetime null comment '人工锁定时间',
    account_expires_at     datetime null comment '账号到期时间',
    password_expires_at    datetime null comment '密码到期时间',
    constraint email
        unique (email),
    constraint login_act
        unique (login_act),
    constraint phone
        unique (phone),
    constraint chk_user_account_type check (account_type in ('SYSTEM', 'HUMAN')),
    constraint chk_user_protected_account check (protected_account in (0, 1)),
    constraint chk_user_account_protection check (
        (account_type = 'SYSTEM' and protected_account = 1)
        or (account_type = 'HUMAN' and protected_account = 0)
    ),
    constraint chk_user_recovery_login_act check (
        (protected_account = 1 and login_act is not null and lower(login_act) = 'admin')
        or (protected_account = 0 and (login_act is null or lower(login_act) <> 'admin'))
    ),
    constraint chk_user_version check (version >= 0),
    constraint chk_user_authorization_version check (authorization_version >= 0),
    constraint chk_user_auth_version check (auth_version >= 0)
    ,constraint chk_user_session_revision check (session_revision >= 0)
    ,constraint chk_user_profile_version check (profile_version >= 0)
    ,constraint chk_user_account_status check (account_status in ('INVITED','ACTIVE','DISABLED'))
    ,constraint chk_user_must_change_password check (must_change_password in (0,1))
    ,constraint chk_user_failed_login_count check (failed_login_count >= 0)
    ,constraint chk_user_manual_locked check (manual_locked in (0,1))
)ENGINE = InnoDB
 AUTO_INCREMENT = 1
 CHARACTER SET = utf8mb3
 COLLATE = utf8mb3_general_ci COMMENT = '用户表'
 ROW_FORMAT = DYNAMIC;

CREATE INDEX `idx_user_workspace_status`
  ON `t_user` (`account_status`,`manual_locked`,`auto_locked_until`,`id`);
CREATE INDEX `idx_user_workspace_last_login`
  ON `t_user` (`last_login_time`,`id`);

CREATE TABLE `t_user_session` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `session_id` varchar(64) NOT NULL COMMENT '不可猜测会话标识',
  `user_id` int NOT NULL,
  `token_digest` varchar(64) NOT NULL COMMENT '原JWT的HMAC摘要',
  `issued_auth_version` bigint NOT NULL,
  `remember_me` tinyint(1) NOT NULL DEFAULT 0,
  `device_summary` varchar(128) NOT NULL,
  `client_summary` varchar(128) NULL,
  `network_summary` varchar(128) NULL,
  `login_time` datetime NOT NULL,
  `last_activity_time` datetime NOT NULL,
  `idle_expires_at` datetime NOT NULL,
  `absolute_expires_at` datetime NOT NULL,
  `revoked_at` datetime NULL,
  `revoked_by` int NULL,
  `revoke_reason` varchar(500) NULL,
  `revoke_type` varchar(32) NULL,
  `version` int NOT NULL DEFAULT 0,
  `create_time` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_session_id` (`session_id`),
  UNIQUE KEY `uk_user_session_token_digest` (`token_digest`),
  KEY `idx_user_session_user_active` (`user_id`,`revoked_at`,`login_time`,`session_id`),
  KEY `idx_user_session_retention` (`revoked_at`,`id`),
  CONSTRAINT `fk_user_session_user` FOREIGN KEY (`user_id`) REFERENCES `t_user` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `chk_user_session_remember` CHECK (`remember_me` IN (0,1)),
  CONSTRAINT `chk_user_session_version` CHECK (`version` >= 0),
  CONSTRAINT `chk_user_session_times` CHECK (`login_time` <= `last_activity_time` AND `last_activity_time` < `idle_expires_at` AND `idle_expires_at` <= `absolute_expires_at`),
  CONSTRAINT `chk_user_session_revocation` CHECK ((`revoked_at` IS NULL AND `revoke_reason` IS NULL AND `revoke_type` IS NULL) OR (`revoked_at` IS NOT NULL AND `revoke_reason` IS NOT NULL AND `revoke_type` IS NOT NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='独立登录会话与撤销事实';

DROP TABLE IF EXISTS `t_clue_owner_history`;
CREATE TABLE `t_clue_owner_history`
(
    `id`             int auto_increment comment '主键，自动增长，线索责任历史ID'
        primary key,
    `clue_id`        int          not null comment '线索ID',
    `from_owner_id`  int          null comment '原负责人ID',
    `to_owner_id`    int          not null comment '新负责人ID',
    `assigned_by`    int          not null comment '操作人ID',
    `reason`         varchar(500) not null comment '分配或转派原因',
    `assigned_time`  datetime     not null comment '分配或转派时间',
    KEY `idx_clue_owner_history_clue` (`clue_id`),
    KEY `idx_clue_owner_history_from_owner` (`from_owner_id`),
    KEY `idx_clue_owner_history_to_owner` (`to_owner_id`),
    KEY `idx_clue_owner_history_assigned_by` (`assigned_by`),
    CONSTRAINT `fk_clue_owner_history_clue` FOREIGN KEY (`clue_id`) REFERENCES `t_clue` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_clue_owner_history_from_owner` FOREIGN KEY (`from_owner_id`) REFERENCES `t_user` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_clue_owner_history_to_owner` FOREIGN KEY (`to_owner_id`) REFERENCES `t_user` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_clue_owner_history_assigned_by` FOREIGN KEY (`assigned_by`) REFERENCES `t_user` (`id`) ON DELETE RESTRICT
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  CHARACTER SET = utf8mb3
  COLLATE = utf8mb3_general_ci COMMENT = '线索责任归属历史表'
  ROW_FORMAT = DYNAMIC;


INSERT INTO t_user
(id, login_act, login_pwd, name, phone, email, account_no_expired, credentials_no_expired,
 account_no_locked, account_enabled, create_time, create_by, edit_time, edit_by, last_login_time,
 account_type, protected_account, version, authorization_version, auth_version)
VALUES
(1, 'admin', '$2y$12$s4SOuAYn1qhEjBjKwvawR.djU.vjb4DIVZbsdZfLi.idWdyGinyCS', '系统管理员', '13800001001', 'admin@qicheng-auto.example', 1, 1, 0, 1, '2025-01-06 09:00:00', NULL, '2026-06-01 10:30:00', 1, NULL, 'SYSTEM', 1, 0, 0, 0),
(2, 'chenchen', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '陈晨', '13800001002', 'chen.chen@qicheng-auto.example', 1, 1, 1, 1, '2025-02-10 09:15:00', 1, NULL, NULL, '2026-06-21 08:51:00', 'HUMAN', 0, 0, 0, 0),
(3, 'wanglei', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '王磊', '13800001003', 'wang.lei@qicheng-auto.example', 1, 1, 1, 1, '2025-03-03 10:20:00', 1, NULL, NULL, '2026-06-20 18:20:00', 'HUMAN', 0, 0, 0, 0),
(4, 'limin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '李敏', '13800001004', 'li.min@qicheng-auto.example', 1, 1, 1, 1, '2025-01-15 14:00:00', 1, NULL, NULL, '2026-06-21 09:03:00', 'HUMAN', 0, 0, 0, 0),
(5, 'zhouqi', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '周琪', '13800001005', 'zhou.qi@qicheng-auto.example', 1, 1, 1, 1, '2025-04-08 11:30:00', 1, NULL, NULL, '2026-06-20 17:42:00', 'HUMAN', 0, 0, 0, 0),
(6, 'zhaoqian', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '赵倩', '13800001006', 'zhao.qian@qicheng-auto.example', 1, 1, 1, 1, '2025-02-18 09:40:00', 1, NULL, NULL, '2026-06-21 08:58:00', 'HUMAN', 0, 0, 0, 0),
(7, 'sunqiang', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '孙强', '13800001007', 'sun.qiang@qicheng-auto.example', 1, 1, 1, 1, '2025-05-12 13:20:00', 1, NULL, NULL, '2026-06-20 19:05:00', 'HUMAN', 0, 0, 0, 0),
(8, 'wuyue', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '吴悦', '13800001008', 'wu.yue@qicheng-auto.example', 1, 1, 1, 1, '2025-06-09 10:10:00', 4, NULL, NULL, '2026-06-21 08:47:00', 'HUMAN', 0, 0, 0, 0),
(9, 'liujia', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '刘佳', '13800001009', 'liu.jia@qicheng-auto.example', 1, 1, 1, 1, '2025-08-04 15:30:00', 4, NULL, NULL, '2026-06-20 18:36:00', 'HUMAN', 0, 0, 0, 0),
(10, 'hejun', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '何军', '13800001010', 'he.jun@qicheng-auto.example', 1, 1, 0, 0, '2025-09-15 09:00:00', 1, '2026-06-10 18:00:00', 1, '2026-06-10 17:42:00', 'HUMAN', 0, 0, 0, 0);

-- 固定恢复账号不随仓库交付任何可用初始密码；必须先通过外部密钥保护的 break-glass 轮换密码并解锁。
UPDATE t_user
SET must_change_password=1,
    manual_locked=1,
    manual_lock_reason='INITIAL_BREAK_GLASS_REQUIRED',
    account_no_locked=0,
    last_login_time=NULL
WHERE id=1 AND BINARY login_act=BINARY 'admin'
  AND account_type='SYSTEM' AND protected_account=1;

CREATE TABLE `t_employee`
(
    `id`                 int          NOT NULL AUTO_INCREMENT,
    `user_id`            int          NULL,
    `employee_no`        varchar(32)  NOT NULL,
    `name`               varchar(64)  NOT NULL,
    `phone`              varchar(18)  NULL,
    `email`              varchar(64)  NULL,
    `avatar_url`         varchar(500) NULL,
    `employment_status`  varchar(16)  NOT NULL DEFAULT 'ACTIVE',
    `profile_completed`  tinyint(1)   NOT NULL DEFAULT 0,
    `hire_date`          date         NULL,
    `leave_date`         date         NULL,
    `version`            int          NOT NULL DEFAULT 0,
    `profile_version`    int          NOT NULL DEFAULT 0,
    `phone_verified`     tinyint(1)   NOT NULL DEFAULT 0,
    `email_verified`     tinyint(1)   NOT NULL DEFAULT 0,
    `create_time`        datetime     NOT NULL,
    `create_by`          int          NULL,
    `edit_time`          datetime     NULL,
    `edit_by`            int          NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_employee_user` (`user_id`),
    UNIQUE KEY `uk_employee_no` (`employee_no`),
    UNIQUE KEY `uk_employee_phone` (`phone`),
    UNIQUE KEY `uk_employee_email` (`email`),
    CONSTRAINT `fk_employee_user` FOREIGN KEY (`user_id`) REFERENCES `t_user` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `chk_employee_status` CHECK (`employment_status` IN ('PENDING', 'ACTIVE', 'HANDOVER', 'LEFT')),
    CONSTRAINT `chk_employee_profile_completed` CHECK (`profile_completed` IN (0, 1)),
    CONSTRAINT `chk_employee_dates` CHECK (`leave_date` IS NULL OR `hire_date` IS NULL OR `leave_date` >= `hire_date`),
    CONSTRAINT `chk_employee_version` CHECK (`version` >= 0)
    ,CONSTRAINT `chk_employee_profile_version` CHECK (`profile_version` >= 0)
    ,CONSTRAINT `chk_employee_phone_verified` CHECK (`phone_verified` IN (0,1))
    ,CONSTRAINT `chk_employee_email_verified` CHECK (`email_verified` IN (0,1))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '员工档案表';

CREATE TABLE `t_account_credential` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` int NOT NULL,
  `purpose` varchar(24) NOT NULL,
  `token_digest` varchar(64) NOT NULL,
  `status` varchar(16) NOT NULL,
  `active_marker` tinyint(1) NULL,
  `expires_at` datetime NOT NULL,
  `consumed_at` datetime NULL,
  `revoked_at` datetime NULL,
  `issued_by` int NULL,
  `reason` varchar(500) NOT NULL,
  `target_value_digest` varchar(64) NULL COMMENT '联系方式验证目标的 HMAC 摘要，不保存明文',
  `target_profile_version` int NULL COMMENT '联系方式验证签发时的员工资料版本',
  `version` int NOT NULL DEFAULT 0,
  `create_time` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_account_credential_digest` (`token_digest`),
  UNIQUE KEY `uk_account_credential_active` (`user_id`,`purpose`,`active_marker`),
  CONSTRAINT `fk_account_credential_user` FOREIGN KEY (`user_id`) REFERENCES `t_user` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `chk_account_credential_purpose` CHECK (`purpose` IN ('INVITATION','SELF_RESET','ADMIN_RESET','PHONE_VERIFY','EMAIL_VERIFY','BREAK_GLASS')),
  CONSTRAINT `chk_account_credential_status` CHECK (`status` IN ('ISSUED','CONSUMED','REVOKED')),
  CONSTRAINT `chk_account_credential_contact_binding` CHECK (((`purpose` IN ('PHONE_VERIFY','EMAIL_VERIFY')) AND (`status` <> 'ISSUED' OR (`target_value_digest` IS NOT NULL AND `target_profile_version` IS NOT NULL))) OR ((`purpose` NOT IN ('PHONE_VERIFY','EMAIL_VERIFY')) AND `target_value_digest` IS NULL AND `target_profile_version` IS NULL)),
  CONSTRAINT `chk_account_credential_version` CHECK (`version` >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='账号一次性凭证事实';

CREATE TABLE `t_credential_delivery_outbox` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `message_id` varchar(36) NOT NULL,
  `credential_id` bigint NOT NULL,
  `user_id` int NOT NULL,
  `purpose` varchar(24) NOT NULL,
  `derivation_nonce` varchar(64) NULL COMMENT '使用独立部署密钥派生原始凭证的随机 nonce，终态清除',
  `phone_digest` varchar(64) NULL,
  `email_digest` varchar(64) NULL,
  `status` varchar(16) NOT NULL,
  `attempt_count` int NOT NULL DEFAULT 0,
  `next_attempt_at` datetime NOT NULL,
  `claimed_at` datetime NULL,
  `delivered_at` datetime NULL,
  `failed_at` datetime NULL,
  `last_error_code` varchar(64) NULL,
  `version` int NOT NULL DEFAULT 0,
  `create_time` datetime NOT NULL,
  `edit_time` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_credential_delivery_message` (`message_id`),
  UNIQUE KEY `uk_credential_delivery_credential` (`credential_id`),
  KEY `idx_credential_delivery_due` (`status`,`next_attempt_at`,`id`),
  CONSTRAINT `fk_credential_delivery_credential` FOREIGN KEY (`credential_id`) REFERENCES `t_account_credential` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_credential_delivery_user` FOREIGN KEY (`user_id`) REFERENCES `t_user` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `chk_credential_delivery_status` CHECK (`status` IN ('PENDING','PROCESSING','RETRY','DELIVERED','FAILED')),
  CONSTRAINT `chk_credential_delivery_attempt` CHECK (`attempt_count` >= 0 AND `version` >= 0),
  CONSTRAINT `chk_credential_delivery_contact` CHECK (`phone_digest` IS NOT NULL OR `email_digest` IS NOT NULL),
  CONSTRAINT `chk_credential_delivery_nonce` CHECK (((`status` IN ('PENDING','PROCESSING','RETRY')) AND `derivation_nonce` IS NOT NULL) OR ((`status` IN ('DELIVERED','FAILED')) AND `derivation_nonce` IS NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='提交后一次性凭证投递 Outbox';

CREATE TABLE `t_password_history` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` int NOT NULL,
  `password_hash` varchar(64) NOT NULL,
  `changed_by` int NULL,
  `change_reason` varchar(64) NOT NULL,
  `changed_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_password_history_user_time` (`user_id`,`changed_at`,`id`),
  CONSTRAINT `fk_password_history_user` FOREIGN KEY (`user_id`) REFERENCES `t_user` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='不可变密码历史';

CREATE TABLE `t_login_identifier` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` int NOT NULL,
  `login_act` varchar(32) NOT NULL,
  `status` varchar(16) NOT NULL,
  `active_marker` tinyint(1) NULL,
  `retired_at` datetime NULL,
  `changed_by` int NULL,
  `reason` varchar(500) NOT NULL,
  `version` int NOT NULL DEFAULT 0,
  `create_time` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_login_identifier_login_act` (`login_act`),
  UNIQUE KEY `uk_login_identifier_active_user` (`user_id`,`active_marker`),
  CONSTRAINT `fk_login_identifier_user` FOREIGN KEY (`user_id`) REFERENCES `t_user` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_login_identifier_changed_by` FOREIGN KEY (`changed_by`) REFERENCES `t_user` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `chk_login_identifier_state` CHECK (
    (`status`='ACTIVE' AND `active_marker`=1 AND `retired_at` IS NULL)
    OR (`status`='RETIRED' AND `active_marker` IS NULL AND `retired_at` IS NOT NULL)
  ),
  CONSTRAINT `chk_login_identifier_version` CHECK (`version` >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='登录账号永久归属事实';

INSERT INTO `t_login_identifier`
  (`user_id`,`login_act`,`status`,`active_marker`,`retired_at`,`changed_by`,`reason`,`version`,`create_time`)
SELECT `id`,`login_act`,'ACTIVE',1,NULL,`create_by`,'完整初始化登录账号永久归属',0,`create_time`
FROM `t_user`;

CREATE TABLE `t_organization_unit`
(
    `id`                 int          NOT NULL AUTO_INCREMENT,
    `code`               varchar(64)  NOT NULL,
    `name`               varchar(64)  NOT NULL,
    `type`               varchar(16)  NOT NULL,
    `parent_id`          int          NULL,
    `leader_employee_id` int          NULL,
    `order_no`           int          NOT NULL DEFAULT 0,
    `placeholder` tinyint(1) NOT NULL DEFAULT 0,
    `enabled`            tinyint(1)   NOT NULL DEFAULT 1,
    `active_root_marker` tinyint(1) GENERATED ALWAYS AS (
      CASE WHEN `type`='COMPANY' AND `parent_id` IS NULL
        AND `placeholder`=0 AND `enabled`=1 THEN 1 ELSE NULL END
    ) STORED,
    `version`            int          NOT NULL DEFAULT 0,
    `create_time`        datetime     NOT NULL,
    `create_by`          int          NULL,
    `edit_time`          datetime     NULL,
    `edit_by`            int          NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_organization_unit_code` (`code`),
    UNIQUE KEY `uk_organization_unit_active_root` (`active_root_marker`),
    KEY `idx_organization_unit_parent_order` (`parent_id`, `order_no`, `id`),
    KEY `idx_organization_unit_leader` (`leader_employee_id`),
    CONSTRAINT `fk_organization_unit_parent` FOREIGN KEY (`parent_id`) REFERENCES `t_organization_unit` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_organization_unit_leader` FOREIGN KEY (`leader_employee_id`) REFERENCES `t_employee` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `chk_organization_unit_type` CHECK (`type` IN ('COMPANY', 'STORE', 'DEPARTMENT', 'TEAM')),
    CONSTRAINT `chk_organization_unit_placeholder` CHECK (`placeholder` IN (0, 1)),
    CONSTRAINT `chk_organization_unit_enabled` CHECK (`enabled` IN (0, 1)),
    CONSTRAINT `chk_organization_unit_hierarchy` CHECK (
      (`type`='COMPANY' AND `parent_id` IS NULL)
      OR (`type`<>'COMPANY' AND `parent_id` IS NOT NULL)
    ),
    CONSTRAINT `chk_organization_unit_order` CHECK (`order_no` >= 0),
    CONSTRAINT `chk_organization_unit_version` CHECK (`version` >= 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '组织单元表';

CREATE TABLE `t_role_organization`
(
    `role_id` int NOT NULL,
    `organization_unit_id` int NOT NULL,
    PRIMARY KEY (`role_id`, `organization_unit_id`),
    CONSTRAINT `fk_role_organization_role` FOREIGN KEY (`role_id`) REFERENCES `t_role` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_role_organization_unit` FOREIGN KEY (`organization_unit_id`) REFERENCES `t_organization_unit` (`id`) ON DELETE RESTRICT
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '角色适用组织';

CREATE TABLE `t_role_permission_organization`
(
    `role_id` int NOT NULL,
    `permission_id` int NOT NULL,
    `organization_unit_id` int NOT NULL,
    PRIMARY KEY (`role_id`, `permission_id`, `organization_unit_id`),
    CONSTRAINT `fk_role_permission_org_permission` FOREIGN KEY (`role_id`, `permission_id`)
      REFERENCES `t_role_permission` (`role_id`, `permission_id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_role_permission_org_unit` FOREIGN KEY (`organization_unit_id`)
      REFERENCES `t_organization_unit` (`id`) ON DELETE RESTRICT
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '角色权限指定组织范围';

CREATE TABLE `t_position`
(
    `id`             int          NOT NULL AUTO_INCREMENT,
    `code`           varchar(64)  NOT NULL,
    `name`           varchar(64)  NOT NULL,
    `description`    varchar(255) NULL,
    `position_level` int          NOT NULL DEFAULT 0,
    `built_in`       tinyint(1)   NOT NULL DEFAULT 0,
    `enabled`        tinyint(1)   NOT NULL DEFAULT 1,
    `version`        int          NOT NULL DEFAULT 0,
    `create_time`    datetime     NOT NULL,
    `create_by`      int          NULL,
    `edit_time`      datetime     NULL,
    `edit_by`        int          NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_position_code` (`code`),
    KEY `idx_position_level_code` (`position_level`, `code`, `id`),
    CONSTRAINT `chk_position_level` CHECK (`position_level` >= 0),
    CONSTRAINT `chk_position_built_in` CHECK (`built_in` IN (0, 1)),
    CONSTRAINT `chk_position_enabled` CHECK (`enabled` IN (0, 1)),
    CONSTRAINT `chk_position_version` CHECK (`version` >= 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '岗位目录表';

CREATE TABLE `t_employee_assignment`
(
    `id`                    int          NOT NULL AUTO_INCREMENT,
    `employee_id`           int          NOT NULL,
    `organization_unit_id`  int          NOT NULL,
    `position_id`           int          NOT NULL,
    `assignment_type`       varchar(16)  NOT NULL,
    `status`                varchar(16)  NOT NULL,
    `active_primary_marker` tinyint(1)   NULL,
    `effective_from`        datetime     NOT NULL,
    `effective_to`          datetime     NULL,
    `reason`                varchar(500) NOT NULL,
    `version`               int          NOT NULL DEFAULT 0,
    `create_time`           datetime     NOT NULL,
    `create_by`             int          NULL,
    `edit_time`             datetime     NULL,
    `edit_by`               int          NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_employee_active_primary` (`employee_id`, `active_primary_marker`),
    KEY `idx_employee_assignment_effective` (`employee_id`, `status`, `effective_from`, `effective_to`, `id`),
    KEY `idx_employee_assignment_org` (`organization_unit_id`, `status`, `id`),
    KEY `idx_employee_assignment_position` (`position_id`, `status`, `id`),
    KEY `idx_employee_assignment_workspace_org` (`organization_unit_id`, `assignment_type`, `status`, `active_primary_marker`, `employee_id`),
    KEY `idx_employee_assignment_workspace_position` (`position_id`, `assignment_type`, `status`, `active_primary_marker`, `employee_id`),
    CONSTRAINT `fk_employee_assignment_employee` FOREIGN KEY (`employee_id`) REFERENCES `t_employee` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_employee_assignment_org` FOREIGN KEY (`organization_unit_id`) REFERENCES `t_organization_unit` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_employee_assignment_position` FOREIGN KEY (`position_id`) REFERENCES `t_position` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `chk_employee_assignment_type` CHECK (`assignment_type` IN ('PRIMARY', 'SECONDARY', 'ACTING')),
    CONSTRAINT `chk_employee_assignment_status` CHECK (`status` IN ('PLANNED', 'ACTIVE', 'ENDED', 'CANCELLED')),
    CONSTRAINT `chk_employee_assignment_period` CHECK (`effective_to` IS NULL OR `effective_to` > `effective_from`),
    CONSTRAINT `chk_employee_assignment_primary_marker` CHECK (
        (`assignment_type` = 'PRIMARY' AND `status` = 'ACTIVE' AND `active_primary_marker` = 1)
        OR ((`assignment_type` <> 'PRIMARY' OR `status` <> 'ACTIVE') AND `active_primary_marker` IS NULL)
    ),
    CONSTRAINT `chk_employee_assignment_version` CHECK (`version` >= 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '员工任职事实表';

CREATE TABLE `t_employee_reporting`
(
    `id`                      int          NOT NULL AUTO_INCREMENT,
    `subordinate_employee_id` int          NOT NULL,
    `manager_employee_id`     int          NOT NULL,
    `relation_type`           varchar(16)  NOT NULL,
    `status`                  varchar(16)  NOT NULL,
    `active_direct_marker`    tinyint(1)   NULL,
    `effective_from`          datetime     NOT NULL,
    `effective_to`            datetime     NULL,
    `reason`                  varchar(500) NOT NULL,
    `version`                 int          NOT NULL DEFAULT 0,
    `create_time`             datetime     NOT NULL,
    `create_by`               int          NULL,
    `edit_time`               datetime     NULL,
    `edit_by`                 int          NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_employee_active_direct_manager` (`subordinate_employee_id`, `active_direct_marker`),
    KEY `idx_employee_reporting_manager` (`manager_employee_id`, `status`, `effective_from`, `effective_to`, `id`),
    KEY `idx_employee_reporting_workspace_manager` (`manager_employee_id`, `relation_type`, `status`, `active_direct_marker`, `subordinate_employee_id`),
    CONSTRAINT `fk_employee_reporting_subordinate` FOREIGN KEY (`subordinate_employee_id`) REFERENCES `t_employee` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_employee_reporting_manager` FOREIGN KEY (`manager_employee_id`) REFERENCES `t_employee` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `chk_employee_reporting_not_self` CHECK (`subordinate_employee_id` <> `manager_employee_id`),
    CONSTRAINT `chk_employee_reporting_type` CHECK (`relation_type` IN ('DIRECT', 'ACTING')),
    CONSTRAINT `chk_employee_reporting_status` CHECK (`status` IN ('PLANNED', 'ACTIVE', 'ENDED', 'CANCELLED')),
    CONSTRAINT `chk_employee_reporting_period` CHECK (`effective_to` IS NULL OR `effective_to` > `effective_from`),
    CONSTRAINT `chk_employee_reporting_acting_finite` CHECK (`relation_type` <> 'ACTING' OR `effective_to` IS NOT NULL),
    CONSTRAINT `chk_employee_reporting_direct_marker` CHECK (
        (`relation_type` = 'DIRECT' AND `status` = 'ACTIVE' AND `active_direct_marker` = 1)
        OR ((`relation_type` <> 'DIRECT' OR `status` <> 'ACTIVE') AND `active_direct_marker` IS NULL)
    ),
    CONSTRAINT `chk_employee_reporting_version` CHECK (`version` >= 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '员工汇报关系事实表';

INSERT INTO `t_organization_unit`
(`id`, `code`, `name`, `type`, `parent_id`, `leader_employee_id`, `order_no`, `placeholder`,
 `enabled`, `version`, `create_time`, `create_by`)
VALUES
(1, 'DEFAULT_COMPANY', '启程汽车', 'COMPANY', NULL, NULL, 0, 0, 1, 0, NOW(), 1),
(2, 'UNASSIGNED_ORG', '待分配组织', 'TEAM', 1, NULL, 999, 1, 1, 0, NOW(), 1);

INSERT INTO `t_position`
(`id`, `code`, `name`, `description`, `position_level`, `built_in`, `enabled`, `version`, `create_time`, `create_by`)
VALUES
(1, 'UNASSIGNED_POSITION', '待分配岗位', '待分配占位岗位，完成员工资料补录后应替换。', 0, 1, 1, 0, NOW(), 1),
(2, 'DEMO_STAFF', '演示员工', '完整初始化普通员工岗位。', 30, 0, 1, 0, NOW(), 1),
(3, 'DEMO_MANAGER', '演示主管', '完整初始化主管岗位。', 60, 0, 1, 0, NOW(), 1),
(4, 'DEMO_ADMIN', '演示管理员', '完整初始化普通管理员岗位。', 100, 0, 1, 0, NOW(), 1);

INSERT INTO `t_employee`
(`user_id`, `employee_no`, `name`, `phone`, `email`, `employment_status`, `profile_completed`,
 `hire_date`, `version`, `phone_verified`, `email_verified`, `create_time`, `create_by`)
SELECT u.id, CONCAT('EMP-', LPAD(u.id, 6, '0')), u.name, u.phone, u.email, 'ACTIVE', 0,
       DATE(u.create_time), 0, IF(u.phone IS NULL,0,1), IF(u.email IS NULL,0,1), NOW(), 1
FROM `t_user` u
WHERE u.account_type = 'HUMAN';

INSERT INTO `t_employee_assignment`
(`employee_id`, `organization_unit_id`, `position_id`, `assignment_type`, `status`,
 `active_primary_marker`, `effective_from`, `reason`, `version`, `create_time`, `create_by`)
SELECT e.id, 1,
       CASE WHEN u.login_act='limin' THEN 4 ELSE 2 END,
       'PRIMARY', 'ACTIVE', 1, COALESCE(u.create_time, NOW()),
       '完整初始化主要任职', 0, NOW(), 1
FROM `t_employee` e
INNER JOIN `t_user` u ON u.id = e.user_id;

-- 根公司负责人必须是具备真实员工事实的 HUMAN 管理员，否则四态门禁会正确判定为 UNINITIALIZED。
UPDATE `t_organization_unit` organization_unit
INNER JOIN `t_employee` employee ON employee.user_id = (
    SELECT id FROM `t_user` WHERE login_act = 'limin' AND account_type = 'HUMAN' LIMIT 1
)
SET organization_unit.leader_employee_id = employee.id,
    organization_unit.edit_time = NOW(),
    organization_unit.edit_by = 1
WHERE organization_unit.code = 'DEFAULT_COMPANY'
  AND organization_unit.type = 'COMPANY'
  AND organization_unit.parent_id IS NULL;

INSERT INTO `t_clue_owner_history`
(`clue_id`, `from_owner_id`, `to_owner_id`, `assigned_by`, `reason`, `assigned_time`)
SELECT id, NULL, owner_id, COALESCE(create_by, owner_id), '初始化线索责任归属', COALESCE(create_time, NOW())
FROM `t_clue`
WHERE owner_id IS NOT NULL;

DROP TABLE IF EXISTS `t_user_role`;
create table t_user_role
(
    id bigint not null auto_increment,
    user_id int not null,
    role_id int not null,
    granted_by int null,
    reason varchar(500) null,
    effective_from datetime null,
    effective_to datetime null,
    active_marker tinyint(1) null default 1,
    version int not null default 0,
    primary key (id),
    unique key uk_user_role_active (user_id, role_id, active_marker),
    constraint fk_user_role_user foreign key (user_id) references t_user (id) on delete restrict,
    constraint fk_user_role_role foreign key (role_id) references t_role (id) on delete restrict,
    constraint fk_user_role_granted_by foreign key (granted_by) references t_user (id) on delete restrict,
    constraint chk_user_role_period check (effective_to is null or effective_from is null or effective_to > effective_from)
)ENGINE = InnoDB
 CHARACTER SET = utf8mb3
 COLLATE = utf8mb3_general_ci COMMENT = '用户角色关系表'
 ROW_FORMAT = DYNAMIC;

create index t_user_role_ibfk_2
    on t_user_role (role_id);

INSERT INTO t_user_role (user_id, role_id)
SELECT u.id, r.id FROM t_user u CROSS JOIN t_role r
WHERE u.login_act = 'admin' AND r.role = 'admin';
INSERT INTO t_user_role (user_id, role_id)
SELECT u.id, r.id FROM t_user u CROSS JOIN t_role r
WHERE u.login_act = 'chenchen' AND r.role = 'sales_consultant';
INSERT INTO t_user_role (user_id, role_id)
SELECT u.id, r.id FROM t_user u CROSS JOIN t_role r
WHERE u.login_act = 'wanglei' AND r.role = 'sales_consultant';
INSERT INTO t_user_role (user_id, role_id)
SELECT u.id, r.id FROM t_user u CROSS JOIN t_role r
WHERE u.login_act = 'limin' AND r.role = 'sales_manager';
INSERT INTO t_user_role (user_id, role_id)
SELECT u.id, r.id FROM t_user u CROSS JOIN t_role r
WHERE u.login_act = 'limin' AND r.role = 'admin';
INSERT INTO t_user_role (user_id, role_id)
SELECT u.id, r.id FROM t_user u CROSS JOIN t_role r
WHERE u.login_act = 'zhouqi' AND r.role = 'marketing_specialist';
INSERT INTO t_user_role (user_id, role_id)
SELECT u.id, r.id FROM t_user u CROSS JOIN t_role r
WHERE u.login_act = 'zhaoqian' AND r.role = 'finance_specialist';
INSERT INTO t_user_role (user_id, role_id)
SELECT u.id, r.id FROM t_user u CROSS JOIN t_role r
WHERE u.login_act = 'sunqiang' AND r.role = 'inventory_specialist';
INSERT INTO t_user_role (user_id, role_id)
SELECT u.id, r.id FROM t_user u CROSS JOIN t_role r
WHERE u.login_act = 'wuyue' AND r.role = 'sales_consultant';
INSERT INTO t_user_role (user_id, role_id)
SELECT u.id, r.id FROM t_user u CROSS JOIN t_role r
WHERE u.login_act = 'liujia' AND r.role = 'marketing_specialist';
INSERT INTO t_user_role (user_id, role_id)
SELECT u.id, r.id FROM t_user u CROSS JOIN t_role r
WHERE u.login_act = 'hejun' AND r.role = 'sales_consultant';

UPDATE t_user_role
SET granted_by = 1,
    reason = '初始化角色关系',
    effective_from = COALESCE((SELECT create_time FROM t_user WHERE t_user.id = t_user_role.user_id), NOW());

-- 完整新库必须至少存在一名可完成安全设置的 HUMAN 管理员；首次登录后强制改密，验证完成前保持 PENDING。
UPDATE t_user
SET must_change_password = 1
WHERE login_act = 'limin' AND account_type = 'HUMAN' AND protected_account = 0;

DROP TABLE IF EXISTS `t_authorization_history`;
DROP TABLE IF EXISTS `t_user_permission`;
CREATE TABLE `t_user_permission`
(
    `id` bigint NOT NULL AUTO_INCREMENT,
    `user_id` int NOT NULL,
    `permission_id` int NOT NULL,
    `effect` varchar(16) NOT NULL,
    `data_scope_code` varchar(32) NULL,
    `effective_from` datetime NOT NULL,
    `effective_to` datetime NULL,
    `active_marker` tinyint(1) NULL DEFAULT 1,
    `reason` varchar(500) NOT NULL,
    `granted_by` int NOT NULL,
    `version` int NOT NULL,
    `create_time` datetime NOT NULL,
    `update_time` datetime NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_permission_current` (`user_id`, `permission_id`),
    KEY `idx_user_permission_effective` (`user_id`, `active_marker`, `effective_from`, `effective_to`, `permission_id`, `version`),
    CONSTRAINT `fk_user_permission_user` FOREIGN KEY (`user_id`) REFERENCES `t_user` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_user_permission_permission` FOREIGN KEY (`permission_id`) REFERENCES `t_permission` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_user_permission_granted_by` FOREIGN KEY (`granted_by`) REFERENCES `t_user` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `chk_user_permission_effect` CHECK (`effect` IN ('GRANT', 'DENY')),
    CONSTRAINT `chk_user_permission_scope` CHECK ((`effect` = 'GRANT' AND `data_scope_code` IS NOT NULL AND `data_scope_code` IN ('SELF', 'DIRECT_REPORTS', 'REPORTING_TREE', 'PRIMARY_ORG', 'ORG_TREE', 'CUSTOM_ORGS', 'GLOBAL')) OR (`effect` = 'DENY' AND `data_scope_code` IS NULL)),
    CONSTRAINT `chk_user_permission_period` CHECK (`effective_to` IS NULL OR `effective_to` > `effective_from`),
    CONSTRAINT `chk_user_permission_version` CHECK (`version` >= 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户个人授权当前态表（version CAS）';

CREATE TABLE `t_user_permission_organization`
(
    `user_permission_id` bigint NOT NULL,
    `organization_unit_id` int NOT NULL,
    PRIMARY KEY (`user_permission_id`, `organization_unit_id`),
    CONSTRAINT `fk_user_permission_org_permission` FOREIGN KEY (`user_permission_id`)
      REFERENCES `t_user_permission` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_user_permission_org_unit` FOREIGN KEY (`organization_unit_id`)
      REFERENCES `t_organization_unit` (`id`) ON DELETE RESTRICT
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户个人权限指定组织范围';

CREATE TABLE `t_authorization_history`
(
    `id` bigint NOT NULL AUTO_INCREMENT,
    `subject_type` varchar(32) NOT NULL,
    `subject_id` varchar(64) NOT NULL,
    `change_type` varchar(16) NOT NULL,
    `target_user_id` int NULL,
    `role_id` int NULL,
    `permission_id` int NULL,
    `effect` varchar(16) NULL,
    `data_scope_code` varchar(32) NULL,
    `effective_from` datetime NULL,
    `effective_to` datetime NULL,
    `before_value` varchar(2048) NULL,
    `after_value` varchar(2048) NULL,
    `reason` varchar(500) NOT NULL,
    `operator_id` int NOT NULL,
    `occurred_time` datetime NOT NULL,
    `request_id` varchar(64) NULL,
    `affected_user_ids` mediumtext NULL,
    `affected_users_snapshot` mediumtext NULL,
    PRIMARY KEY (`id`),
    KEY `idx_authorization_history_subject` (`subject_type`, `subject_id`, `occurred_time`, `id`),
    KEY `idx_authorization_history_target` (`target_user_id`, `occurred_time`, `id`),
    CONSTRAINT `fk_authorization_history_target_user` FOREIGN KEY (`target_user_id`) REFERENCES `t_user` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_authorization_history_role` FOREIGN KEY (`role_id`) REFERENCES `t_role` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_authorization_history_permission` FOREIGN KEY (`permission_id`) REFERENCES `t_permission` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_authorization_history_operator` FOREIGN KEY (`operator_id`) REFERENCES `t_user` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `chk_authorization_history_subject` CHECK (`subject_type` IN ('ROLE', 'ROLE_PERMISSION', 'USER_ROLE', 'USER_PERMISSION', 'ORGANIZATION_UNIT', 'POSITION', 'ORGANIZATION_ASSIGNMENT', 'REPORTING_RELATION')),
    CONSTRAINT `chk_authorization_history_change` CHECK (`change_type` IN ('CREATE', 'UPDATE', 'ENABLE', 'DISABLE', 'ASSIGN', 'UNASSIGN', 'GRANT', 'DENY', 'REVOKE', 'EXPIRE')),
    CONSTRAINT `chk_authorization_history_effect` CHECK (`effect` IS NULL OR `effect` IN ('GRANT', 'DENY')),
    CONSTRAINT `chk_authorization_history_subject_ids` CHECK (
      (`subject_type` <> 'ROLE' OR `role_id` IS NOT NULL) AND
      (`subject_type` <> 'ROLE_PERMISSION' OR (`role_id` IS NOT NULL AND `permission_id` IS NOT NULL)) AND
      (`subject_type` <> 'USER_ROLE' OR (`target_user_id` IS NOT NULL AND `role_id` IS NOT NULL)) AND
      (`subject_type` <> 'USER_PERMISSION' OR (`target_user_id` IS NOT NULL AND `permission_id` IS NOT NULL AND `effect` IS NOT NULL))
    ),
    CONSTRAINT `chk_authorization_history_user_permission_scope` CHECK (
      `subject_type` <> 'USER_PERMISSION' OR
      (`effect` = 'GRANT' AND `data_scope_code` IS NOT NULL) OR
      (`effect` = 'DENY' AND `data_scope_code` IS NULL)
    ),
    CONSTRAINT `chk_authorization_history_period` CHECK (`effective_to` IS NULL OR `effective_from` IS NULL OR `effective_to` > `effective_from`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '授权变化不可变历史表';

CREATE TABLE `t_authorization_graph_lock`
(
    `lock_name` varchar(64) NOT NULL,
    PRIMARY KEY (`lock_name`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '授权关系图串行化锁';

CREATE TABLE `t_user_lifecycle_event`
(
    `id` bigint NOT NULL AUTO_INCREMENT,
    `operation_id` varchar(64) NOT NULL,
    `request_id` varchar(64) NOT NULL,
    `action` varchar(32) NOT NULL,
    `user_id` int NOT NULL,
    `employee_id` int NOT NULL,
    `before_value` mediumtext NOT NULL,
    `after_value` mediumtext NOT NULL,
    `reason` varchar(500) NOT NULL,
    `operator_id` int NOT NULL,
    `occurred_time` datetime NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_lifecycle_operation` (`operation_id`),
    KEY `idx_user_lifecycle_target_time` (`user_id`,`occurred_time`,`id`),
    CONSTRAINT `fk_user_lifecycle_user` FOREIGN KEY (`user_id`) REFERENCES `t_user` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_user_lifecycle_employee` FOREIGN KEY (`employee_id`) REFERENCES `t_employee` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_user_lifecycle_operator` FOREIGN KEY (`operator_id`) REFERENCES `t_user` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `chk_user_lifecycle_action` CHECK (`action` IN ('TRANSFER','DEPARTURE_START','HANDOVER_CONFIRM','DEPARTURE_COMPLETE','REHIRE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='人员生命周期不可变事件历史';

CREATE TABLE `t_user_lifecycle_snapshot`
(
    `id` bigint NOT NULL AUTO_INCREMENT,
    `token_digest` varchar(64) NOT NULL,
    `user_id` int NOT NULL,
    `employee_id` int NOT NULL,
    `employee_version` int NOT NULL,
    `reason_digest` varchar(64) NOT NULL,
    `fact_digest` varchar(64) NOT NULL,
    `expires_at` datetime NOT NULL,
    `consumed_at` datetime NULL,
    `version` int NOT NULL DEFAULT 0,
    `create_time` datetime NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_lifecycle_snapshot_token` (`token_digest`),
    KEY `idx_user_lifecycle_snapshot_expiry` (`expires_at`,`consumed_at`,`id`),
    CONSTRAINT `fk_user_lifecycle_snapshot_user` FOREIGN KEY (`user_id`) REFERENCES `t_user` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_user_lifecycle_snapshot_employee` FOREIGN KEY (`employee_id`) REFERENCES `t_employee` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='一次性离职预检快照';

INSERT INTO `t_authorization_graph_lock` (`lock_name`) VALUES ('ORGANIZATION_HIERARCHY'), ('REPORTING_GRAPH'), ('AVAILABLE_ADMIN_GUARD'), ('AUTHORIZATION_MEMBERSHIP_GUARD'), ('TEST_DRIVE_SCHEDULE_GUARD'), ('LOGIN_IDENTIFIER_GUARD');

DROP TRIGGER IF EXISTS `trg_authorization_history_no_update`;
DROP TRIGGER IF EXISTS `trg_authorization_history_no_delete`;
DROP TRIGGER IF EXISTS `trg_user_lifecycle_event_no_update`;
DROP TRIGGER IF EXISTS `trg_user_lifecycle_event_no_delete`;
DROP TRIGGER IF EXISTS `trg_login_identifier_immutable_bu`;
DROP TRIGGER IF EXISTS `trg_login_identifier_immutable_bd`;
DROP TRIGGER IF EXISTS `trg_recovery_account_identity_bi`;
DROP TRIGGER IF EXISTS `trg_recovery_account_identity_bu`;
DROP TRIGGER IF EXISTS `trg_recovery_account_identity_bd`;
DELIMITER $$
CREATE TRIGGER `trg_authorization_history_no_update`
BEFORE UPDATE ON `t_authorization_history`
FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'authorization history is immutable';
END$$
CREATE TRIGGER `trg_authorization_history_no_delete`
BEFORE DELETE ON `t_authorization_history`
FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'authorization history is immutable';
END$$
CREATE TRIGGER `trg_user_lifecycle_event_no_update`
BEFORE UPDATE ON `t_user_lifecycle_event`
FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'user lifecycle event is immutable';
END$$
CREATE TRIGGER `trg_user_lifecycle_event_no_delete`
BEFORE DELETE ON `t_user_lifecycle_event`
FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'user lifecycle event is immutable';
END$$
CREATE TRIGGER `trg_login_identifier_immutable_bu`
BEFORE UPDATE ON `t_login_identifier`
FOR EACH ROW
BEGIN
    IF NOT (NEW.`user_id` <=> OLD.`user_id`)
       OR NOT (NEW.`login_act` <=> OLD.`login_act`)
       OR NOT (NEW.`create_time` <=> OLD.`create_time`) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'login identifier ownership is immutable';
    END IF;
END$$
CREATE TRIGGER `trg_login_identifier_immutable_bd`
BEFORE DELETE ON `t_login_identifier`
FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'login identifier ownership is immutable';
END$$
CREATE TRIGGER `trg_recovery_account_identity_bi`
BEFORE INSERT ON `t_user`
FOR EACH ROW
BEGIN
    IF NEW.`id`=1 OR BINARY NEW.`login_act`=BINARY 'admin' OR NEW.`protected_account`=1 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'fixed recovery account identity cannot be inserted or copied';
    END IF;
END$$
CREATE TRIGGER `trg_recovery_account_identity_bu`
BEFORE UPDATE ON `t_user`
FOR EACH ROW
BEGIN
    IF OLD.`id`=1 OR NEW.`id`=1
       OR BINARY OLD.`login_act`=BINARY 'admin' OR BINARY NEW.`login_act`=BINARY 'admin'
       OR OLD.`protected_account`=1 OR NEW.`protected_account`=1 THEN
        IF NOT (
          OLD.`id`=1 AND BINARY OLD.`login_act`=BINARY 'admin' AND OLD.`account_type`='SYSTEM' AND OLD.`protected_account`=1
          AND NEW.`id`=1 AND BINARY NEW.`login_act`=BINARY 'admin' AND NEW.`account_type`='SYSTEM' AND NEW.`protected_account`=1
        ) THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'fixed recovery account identity is immutable';
        END IF;
    END IF;
END$$
CREATE TRIGGER `trg_recovery_account_identity_bd`
BEFORE DELETE ON `t_user`
FOR EACH ROW
BEGIN
    IF OLD.`id`=1 OR BINARY OLD.`login_act`=BINARY 'admin' OR OLD.`protected_account`=1 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'protected recovery account is immutable';
    END IF;
END$$
DELIMITER ;

DROP TABLE IF EXISTS `t_tran_remark`;
create table t_tran_remark
(
    id           int auto_increment comment '主键，自动增长，交易备注ID'
        primary key,
    tran_id      int          null comment '交易ID',
    note_way     int          null comment '跟踪方式',
    note_content varchar(255) null comment '跟踪内容',
    create_time  datetime     null comment '跟踪时间',
    create_by    int          null comment '跟踪人',
    edit_time    datetime     null comment '编辑时间',
    edit_by      int          null comment '编辑人',
    deleted      int          null comment '删除状态（0正常，1删除）'
)ENGINE = InnoDB
 AUTO_INCREMENT = 1
 CHARACTER SET = utf8mb3
 COLLATE = utf8mb3_general_ci COMMENT = '交易跟踪记录表'
 ROW_FORMAT = DYNAMIC;

create index t_tran_remark_ibfk_1
    on t_tran_remark (tran_id);

create index t_tran_remark_ibfk_2
    on t_tran_remark (note_way);

create index t_tran_remark_ibfk_3
    on t_tran_remark (create_by);

create index t_tran_remark_ibfk_4
    on t_tran_remark (edit_by);

ALTER TABLE t_tran_remark
    ADD CONSTRAINT `fk_tran_remark_tran` FOREIGN KEY (`tran_id`) REFERENCES `t_tran` (`id`) ON DELETE RESTRICT;

INSERT INTO `t_tran_remark`
(`id`, `tran_id`, `note_way`, `note_content`, `create_time`, `create_by`, `edit_time`, `edit_by`, `deleted`)
VALUES
(1, 2, 61, '银行已完成金融预审，客户需在6月25日前支付首付款。', '2026-06-20 11:30:00', 2, NULL, NULL, 0),
(2, 3, 62, '最终报价已发送，客户正在与家人确认内饰颜色。', '2026-06-19 17:20:00', 3, NULL, NULL, 0),
(3, 4, 64, '企业采购负责人补齐营业执照及开票资料，已提交经理审批。', '2026-06-20 09:15:00', 8, NULL, NULL, 0),
(4, 5, 61, '客户希望同时测算两年低息和全款优惠，财务测算后再回访。', '2026-06-18 15:05:00', 3, NULL, NULL, 0),
(5, 6, 61, '确认客户已选择其他门店现车，本交易标记流失。', '2026-06-10 18:05:00', 8, NULL, NULL, 0),
(6, 8, 62, '退款到账截图已发送，客户确认无异议。', '2026-06-16 14:20:00', 3, NULL, NULL, 0);

-- ----------------------------
-- Table structure for t_operation_log
-- ----------------------------
DROP TABLE IF EXISTS `t_operation_log`;
CREATE TABLE `t_operation_log`
(
    `id`          int          NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`     int          NULL DEFAULT NULL COMMENT '操作用户ID',
    `user_name`   varchar(64)  NULL DEFAULT NULL COMMENT '操作用户名',
    `action_code` varchar(64)  NOT NULL COMMENT '审计动作代码',
    `object_type` varchar(64)  NULL DEFAULT NULL COMMENT '业务对象类型',
    `module_name` varchar(64)  NULL DEFAULT NULL COMMENT '模块名称',
    `resource_id` varchar(64)  NULL DEFAULT NULL COMMENT '业务资源ID',
    `result`      varchar(32)  NULL DEFAULT NULL COMMENT '操作结果',
    `detail`      varchar(2048) NULL DEFAULT NULL COMMENT '结构化审计摘要JSON',
    `ip`          varchar(64)  NULL DEFAULT NULL COMMENT '操作IP',
    `request_id`  varchar(64)  NULL DEFAULT NULL COMMENT '请求标识',
    `create_time` datetime     NULL DEFAULT NULL COMMENT '创建时间',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `idx_operation_log_time` (`create_time` ASC, `id` ASC) USING BTREE,
    INDEX `idx_operation_log_query` (`module_name` ASC, `action_code` ASC, `user_id` ASC, `result` ASC) USING BTREE,
    INDEX `idx_operation_log_user_history` (`resource_id` ASC, `action_code` ASC, `create_time` ASC, `id` ASC) USING BTREE
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  CHARACTER SET = utf8mb3
  COLLATE = utf8mb3_general_ci COMMENT = '操作审计日志表'
  ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for t_login_log
-- ----------------------------
DROP TABLE IF EXISTS `t_login_log`;
CREATE TABLE `t_login_log`
(
    `id`             int          NOT NULL AUTO_INCREMENT COMMENT '主键',
    `login_act`      varchar(64)  NOT NULL COMMENT '登录账号',
    `user_id`        int          NULL DEFAULT NULL COMMENT '可识别用户ID',
    `user_name`      varchar(64)  NULL DEFAULT NULL COMMENT '可识别用户名',
    `result`         varchar(32)  NOT NULL COMMENT '登录结果',
    `reason_code`    varchar(64)  NOT NULL COMMENT '稳定原因编码',
    `reason_message` varchar(255) NULL DEFAULT NULL COMMENT '管理员可见原因说明',
    `ip`             varchar(64)  NULL DEFAULT NULL COMMENT '客户端IP',
    `browser`        varchar(128) NULL DEFAULT NULL COMMENT '浏览器',
    `os`             varchar(128) NULL DEFAULT NULL COMMENT '操作系统',
    `request_id`     varchar(64)  NULL DEFAULT NULL COMMENT '请求标识',
    `create_time`    datetime     NOT NULL COMMENT '登录时间',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `idx_login_log_time` (`create_time` ASC, `id` ASC) USING BTREE,
    INDEX `idx_login_log_query` (`login_act` ASC, `user_id` ASC, `result` ASC, `reason_code` ASC) USING BTREE
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  CHARACTER SET = utf8mb3
  COLLATE = utf8mb3_general_ci COMMENT = '登录审计日志表'
  ROW_FORMAT = DYNAMIC;


-- ----------------------------
-- Records of t_operation_log
-- ----------------------------
INSERT INTO `t_operation_log`
(`id`, `user_id`, `user_name`, `action_code`, `object_type`, `module_name`, `resource_id`, `result`, `detail`, `ip`, `request_id`, `create_time`)
VALUES
(1, 2, '陈晨', 'TRAN_CREATE', 'TRAN', '交易管理', '2', 'SUCCESS', '{"tranNo":"XS202605220001","customerId":2}', '10.20.1.32', 'seed-op-1', '2026-05-22 14:25:00'),
(2, 4, '李敏', 'TRAN_APPROVE', 'TRAN', '交易审批', '3', 'SUCCESS', '{"result":"APPROVED","money":439900.00}', '10.20.1.18', 'seed-op-2', '2026-06-19 17:10:00'),
(3, 6, '赵倩', 'PAYMENT_CONFIRM', 'PAYMENT', '收款管理', '4', 'SUCCESS', '{"paymentNo":"SK202606160001","amount":20000.00}', '10.20.2.11', 'seed-op-3', '2026-06-16 14:05:00'),
(4, 6, '赵倩', 'PAYMENT_REFUND', 'PAYMENT', '退款管理', '5', 'SUCCESS', '{"paymentNo":"TK202606160001","amount":20000.00}', '10.20.2.11', 'seed-op-4', '2026-06-16 14:18:00'),
(5, 7, '孙强', 'PRODUCT_STOCK_RESERVE', 'PRODUCT', '库存管理', '9', 'SUCCESS', '{"tranNo":"XS202606120001","quantity":1}', '10.20.3.25', 'seed-op-5', '2026-06-20 09:20:00');

-- ----------------------------
-- Audit permissions
-- ----------------------------
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

-- ----------------------------
-- Table structure for t_payment
-- ----------------------------
DROP TABLE IF EXISTS `t_payment`;
CREATE TABLE `t_payment`
(
    `id`              int            NOT NULL AUTO_INCREMENT COMMENT '主键',
    `tran_id`         int            NOT NULL COMMENT '交易ID',
    `payment_no`      varchar(64)    NOT NULL COMMENT '支付流水号',
    `amount`          decimal(10, 2) NOT NULL DEFAULT 0 COMMENT '支付金额',
    `payment_method`  varchar(32)    NOT NULL COMMENT '支付方式',
    `payment_type`    varchar(32)    NOT NULL COMMENT '支付类型',
    `payment_status`  varchar(32)    NOT NULL DEFAULT 'PENDING' COMMENT '支付状态',
    `payment_time`    datetime       NULL DEFAULT NULL COMMENT '支付时间',
    `transaction_ref` varchar(128)   NULL DEFAULT NULL COMMENT '第三方交易参考号',
    `idempotency_key` varchar(160)   NULL DEFAULT NULL COMMENT '收款退款幂等键',
    `remark`          varchar(255)   NULL DEFAULT NULL COMMENT '备注',
    `create_time`     datetime       NULL DEFAULT NULL COMMENT '创建时间',
    `create_by`       int            NULL DEFAULT NULL COMMENT '创建人',
    `edit_time`       datetime       NULL DEFAULT NULL COMMENT '编辑时间',
    `edit_by`         int            NULL DEFAULT NULL COMMENT '编辑人',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `uk_payment_no` (`payment_no` ASC) USING BTREE,
    UNIQUE INDEX `uk_payment_transaction_ref` (`transaction_ref` ASC) USING BTREE,
    UNIQUE INDEX `uk_payment_idempotency_key` (`idempotency_key` ASC) USING BTREE,
    INDEX `idx_tran_id` (`tran_id` ASC) USING BTREE,
    CONSTRAINT `chk_payment_method` CHECK (`payment_method` IN ('CASH', 'BANK_TRANSFER', 'WECHAT', 'ALIPAY', 'CHECK', 'OTHER')),
    CONSTRAINT `chk_payment_type` CHECK (`payment_type` IN ('DEPOSIT', 'INSTALLMENT', 'FULL', 'BALANCE', 'REFUND')),
    CONSTRAINT `chk_payment_status` CHECK (`payment_status` IN ('PENDING', 'COMPLETED', 'FAILED', 'REVERSED', 'VOIDED'))
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  CHARACTER SET = utf8mb3
  COLLATE = utf8mb3_general_ci COMMENT = '支付表'
  ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of t_payment
-- ----------------------------
INSERT INTO `t_payment`
(`id`, `tran_id`, `payment_no`, `amount`, `payment_method`, `payment_type`,
 `payment_status`, `payment_time`, `transaction_ref`, `idempotency_key`, `remark`, `create_time`,
 `create_by`, `edit_time`, `edit_by`)
VALUES
(1, 1, 'SK202604100001', 386800.00, 'BANK_TRANSFER', 'FULL', 'COMPLETED',
 '2026-04-10 10:08:00', 'CMB2026041008392101', 'PAYMENT:REF:BANK_TRANSFER:CMB2026041008392101', '全款到账，付款人为客户本人。',
 '2026-04-10 10:10:00', 6, NULL, NULL),
(2, 2, 'SK202606160001', 50000.00, 'BANK_TRANSFER', 'DEPOSIT', 'COMPLETED',
 '2026-06-16 14:03:00', 'CMB2026061614075532', 'PAYMENT:REF:BANK_TRANSFER:CMB2026061614075532', '购车定金到账，待支付首付款。',
 '2026-06-16 14:05:00', 6, NULL, NULL),
(3, 7, 'SK202603180001', 455800.00, 'BANK_TRANSFER', 'FULL', 'COMPLETED',
 '2026-03-18 16:12:00', 'BOC2026031816129088', 'PAYMENT:REF:BANK_TRANSFER:BOC2026031816129088', '企业账户全款到账。',
 '2026-03-18 16:15:00', 6, NULL, NULL),
(4, 8, 'SK202606020001', 20000.00, 'WECHAT', 'DEPOSIT', 'COMPLETED',
 '2026-06-02 11:22:00', 'WX202606021122384921', 'PAYMENT:REF:WECHAT:WX202606021122384921', '订单取消，原支付记录已退款。',
 '2026-06-02 11:24:00', 6, '2026-06-16 14:18:00', 6),
(5, 8, 'TK202606160001', -20000.00, 'WECHAT', 'REFUND', 'COMPLETED',
 '2026-06-16 14:16:00', 'WXRF2026061614160921', 'REFUND:REF:WECHAT:WXRF2026061614160921', '定金原路退回。',
 '2026-06-16 14:18:00', 6, NULL, NULL);

-- ----------------------------
-- Table structure for t_refund_request
-- ----------------------------
DROP TABLE IF EXISTS `t_refund_request`;
CREATE TABLE `t_refund_request`
(
    `id`                  int            NOT NULL AUTO_INCREMENT COMMENT '主键',
    `tran_id`             int            NOT NULL COMMENT '交易ID',
    `original_payment_id` int            NOT NULL COMMENT '原收款ID',
    `refund_payment_id`   int            NULL DEFAULT NULL COMMENT '退款流水ID',
    `amount`              decimal(10, 2) NOT NULL COMMENT '退款金额',
    `refund_type`         varchar(32)    NOT NULL COMMENT '退款类型',
    `reason`              varchar(500)   NOT NULL COMMENT '退款原因',
    `status`              varchar(32)    NOT NULL COMMENT '申请状态',
    `requested_by`        int            NULL DEFAULT NULL COMMENT '申请人',
    `requested_time`      datetime       NULL DEFAULT NULL COMMENT '申请时间',
    `approved_by`         int            NULL DEFAULT NULL COMMENT '审批人',
    `approved_time`       datetime       NULL DEFAULT NULL COMMENT '审批时间',
    `approve_comment`     varchar(500)   NULL DEFAULT NULL COMMENT '审批意见',
    `executed_by`         int            NULL DEFAULT NULL COMMENT '执行人',
    `execution_started_time` datetime     NULL DEFAULT NULL COMMENT '执行开始时间',
    `executed_time`       datetime       NULL DEFAULT NULL COMMENT '执行时间',
    `execution_ref`       varchar(128)   NULL DEFAULT NULL COMMENT '退款执行参考号',
    `execution_remark`    varchar(500)   NULL DEFAULT NULL COMMENT '退款执行备注',
    `failure_reason`      varchar(500)   NULL DEFAULT NULL COMMENT '退款执行失败原因',
    `create_time`         datetime       NULL DEFAULT NULL COMMENT '创建时间',
    `create_by`           int            NULL DEFAULT NULL COMMENT '创建人',
    `edit_time`           datetime       NULL DEFAULT NULL COMMENT '编辑时间',
    `edit_by`             int            NULL DEFAULT NULL COMMENT '编辑人',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `idx_refund_tran_id` (`tran_id` ASC) USING BTREE,
    INDEX `idx_refund_original_payment` (`original_payment_id` ASC) USING BTREE,
    CONSTRAINT `fk_refund_request_tran` FOREIGN KEY (`tran_id`) REFERENCES `t_tran` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_refund_request_original_payment` FOREIGN KEY (`original_payment_id`) REFERENCES `t_payment` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_refund_request_refund_payment` FOREIGN KEY (`refund_payment_id`) REFERENCES `t_payment` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `chk_refund_request_type` CHECK (`refund_type` IN ('ORDER_CANCEL', 'OVERPAY', 'PRICE_ADJUSTMENT', 'CUSTOMER_BREACH', 'INTERNAL_CORRECTION')),
    CONSTRAINT `chk_refund_request_status` CHECK (`status` IN ('PENDING_APPROVAL', 'PENDING_EXECUTION', 'EXECUTING', 'COMPLETED', 'REJECTED', 'FAILED', 'CANCELLED'))
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  CHARACTER SET = utf8mb3
  COLLATE = utf8mb3_general_ci COMMENT = '退款申请表'
  ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of t_refund_request
-- ----------------------------
INSERT INTO `t_refund_request`
(`id`, `tran_id`, `original_payment_id`, `refund_payment_id`, `amount`, `refund_type`,
 `reason`, `status`, `requested_by`, `requested_time`, `approved_by`, `approved_time`,
 `approve_comment`, `executed_by`, `execution_started_time`, `executed_time`,
 `execution_ref`, `execution_remark`, `failure_reason`, `create_time`, `create_by`, `edit_time`, `edit_by`)
VALUES
(1, 8, 4, 5, 20000.00, 'ORDER_CANCEL', '客户取消订单，定金原路退回。', 'COMPLETED',
 3, '2026-06-16 14:10:00', 4, '2026-06-16 14:12:00',
 '同意按取消订单流程退款。', 6, '2026-06-16 14:16:00', '2026-06-16 14:18:00',
 'WXRF2026061614160921', '定金原路退回。', NULL,
 '2026-06-16 14:10:00', 3, '2026-06-16 14:18:00', 6);

-- ----------------------------
-- Table structure for t_ai_run
-- ----------------------------
DROP TABLE IF EXISTS `t_ai_proactive_event`;
DROP TABLE IF EXISTS `t_ai_workflow_step`;
DROP TABLE IF EXISTS `t_ai_execution_event`;
DROP TABLE IF EXISTS `t_ai_approval`;
DROP TABLE IF EXISTS `t_ai_action_proposal`;
DROP TABLE IF EXISTS `t_ai_tool_call`;
DROP TABLE IF EXISTS `t_ai_message`;
DROP TABLE IF EXISTS `t_ai_run_event`;
DROP TABLE IF EXISTS `t_ai_proactive_subscription`;
DROP TABLE IF EXISTS `t_ai_workflow`;
DROP TABLE IF EXISTS `t_ai_provider_config`;
DROP TABLE IF EXISTS `t_ai_assistant_policy`;
DROP TABLE IF EXISTS `t_ai_run`;
DROP TABLE IF EXISTS `t_ai_conversation`;
CREATE TABLE `t_ai_conversation`
(
    `id`                  BIGINT       NOT NULL AUTO_INCREMENT COMMENT 'AI 会话 ID',
    `conversation_no`     VARCHAR(64)  NOT NULL COMMENT 'AI 会话业务编号',
    `user_id`             INT          NOT NULL COMMENT '会话 owner 用户 ID',
    `title`               VARCHAR(128) NOT NULL COMMENT '会话标题',
    `status`              VARCHAR(32)  NOT NULL COMMENT '会话状态',
    `entry_point`         VARCHAR(32)  NOT NULL COMMENT '入口: PAGE/SIDE_PANEL',
    `context_object_type` VARCHAR(64)  NULL DEFAULT NULL COMMENT '上下文对象类型',
    `context_object_id`   VARCHAR(64)  NULL DEFAULT NULL COMMENT '上下文对象 ID',
    `summary_text`        VARCHAR(8000) NULL DEFAULT NULL COMMENT '脱敏会话摘要',
    `last_run_no`         VARCHAR(64)  NULL DEFAULT NULL COMMENT '最近 AI Run 编号',
    `last_message_time`   DATETIME     NULL DEFAULT NULL COMMENT '最近消息时间',
    `create_time`         DATETIME     NOT NULL COMMENT '创建时间',
    `create_by`           INT          NOT NULL COMMENT '创建人',
    `edit_time`           DATETIME     NULL DEFAULT NULL COMMENT '编辑时间',
    `edit_by`             INT          NULL DEFAULT NULL COMMENT '编辑人',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `uk_ai_conversation_no` (`conversation_no` ASC) USING BTREE,
    INDEX `idx_ai_conversation_user_time` (`user_id` ASC, `last_message_time` DESC, `create_time` DESC, `id` DESC) USING BTREE,
    INDEX `idx_ai_conversation_context` (`user_id` ASC, `context_object_type` ASC, `context_object_id` ASC, `status` ASC) USING BTREE,
    CONSTRAINT `fk_ai_conversation_user` FOREIGN KEY (`user_id`) REFERENCES `t_user` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `chk_ai_conversation_status` CHECK (`status` IN ('ACTIVE', 'ARCHIVED')),
    CONSTRAINT `chk_ai_conversation_entry_point` CHECK (`entry_point` IN ('PAGE', 'SIDE_PANEL'))
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  CHARACTER SET = utf8mb3
  COLLATE = utf8mb3_general_ci COMMENT = 'AI 多轮会话表'
  ROW_FORMAT = DYNAMIC;

CREATE TABLE `t_ai_run`
(
    `id`                  BIGINT       NOT NULL AUTO_INCREMENT COMMENT 'AI Run ID',
    `run_no`              VARCHAR(64)  NOT NULL COMMENT 'AI Run业务编号',
    `conversation_id`     BIGINT       NOT NULL COMMENT 'AI 会话 ID',
    `parent_run_id`       BIGINT       NULL DEFAULT NULL COMMENT '上一轮 Run ID',
    `turn_no`             INT          NOT NULL COMMENT '会话内轮次',
    `user_id`             INT          NOT NULL COMMENT '发起用户ID',
    `user_name`           VARCHAR(64)  NULL DEFAULT NULL COMMENT '发起用户名称摘要',
    `entry_point`         VARCHAR(32)  NOT NULL COMMENT '入口: PAGE/SIDE_PANEL',
    `context_object_type` VARCHAR(64)  NULL DEFAULT NULL COMMENT '上下文对象类型',
    `context_object_id`   VARCHAR(64)  NULL DEFAULT NULL COMMENT '上下文对象ID',
    `prompt_summary`      VARCHAR(4000) NOT NULL COMMENT '用户问题的安全展示文本',
    `status`              VARCHAR(32)  NOT NULL COMMENT 'Run状态',
    `error_code`          VARCHAR(64)  NULL DEFAULT NULL COMMENT '失败错误码',
    `error_message`       VARCHAR(255) NULL DEFAULT NULL COMMENT '失败摘要',
    `started_time`        DATETIME     NULL DEFAULT NULL COMMENT '开始时间',
    `completed_time`      DATETIME     NULL DEFAULT NULL COMMENT '完成时间',
    `expires_time`        DATETIME     NULL DEFAULT NULL COMMENT '过期时间',
    `context_active`      TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '是否属于当前有效上下文分支',
    `invalidation_reason` VARCHAR(255) NULL DEFAULT NULL COMMENT '退出上下文原因',
    `create_time`         DATETIME     NOT NULL COMMENT '创建时间',
    `create_by`           INT          NOT NULL COMMENT '创建人',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `uk_ai_run_no` (`run_no` ASC) USING BTREE,
    UNIQUE INDEX `uk_ai_run_conversation_turn` (`conversation_id` ASC, `turn_no` ASC) USING BTREE,
    INDEX `idx_ai_run_user_time` (`user_id` ASC, `create_time` DESC, `id` DESC) USING BTREE,
    INDEX `idx_ai_run_status` (`status` ASC, `create_time` DESC) USING BTREE,
    INDEX `idx_ai_run_context` (`context_object_type` ASC, `context_object_id` ASC) USING BTREE,
    INDEX `idx_ai_run_conversation_turn` (`conversation_id` ASC, `turn_no` DESC, `id` DESC) USING BTREE,
    INDEX `idx_ai_run_parent` (`parent_run_id` ASC) USING BTREE,
    CONSTRAINT `fk_ai_run_conversation` FOREIGN KEY (`conversation_id`) REFERENCES `t_ai_conversation` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_ai_run_parent` FOREIGN KEY (`parent_run_id`) REFERENCES `t_ai_run` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_ai_run_user` FOREIGN KEY (`user_id`) REFERENCES `t_user` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `chk_ai_run_status` CHECK (`status` IN ('CREATED', 'RUNNING', 'WAITING_FOR_APPROVAL', 'COMPLETED', 'FAILED', 'CANCELLED', 'EXPIRED')),
    CONSTRAINT `chk_ai_run_entry_point` CHECK (`entry_point` IN ('PAGE', 'SIDE_PANEL'))
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  CHARACTER SET = utf8mb3
  COLLATE = utf8mb3_general_ci COMMENT = 'AI运行追踪表'
  ROW_FORMAT = DYNAMIC;

CREATE TABLE `t_ai_run_event`
(
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT 'AI Run 事件 ID',
    `run_id`        BIGINT       NOT NULL COMMENT 'AI Run ID',
    `event_id`      VARCHAR(64)  NOT NULL COMMENT '外部稳定事件 ID',
    `sequence_no`   INT          NOT NULL COMMENT 'Run 内事件序号',
    `event_type`    VARCHAR(64)  NOT NULL COMMENT '事件类型',
    `payload_json`  TEXT         NOT NULL COMMENT '脱敏事件载荷',
    `occurred_time` DATETIME     NOT NULL COMMENT '事件发生时间',
    `create_time`   DATETIME     NOT NULL COMMENT '落库时间',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `uk_ai_run_event_sequence` (`run_id` ASC, `sequence_no` ASC) USING BTREE,
    UNIQUE INDEX `uk_ai_run_event_id` (`run_id` ASC, `event_id` ASC) USING BTREE,
    CONSTRAINT `fk_ai_run_event_run` FOREIGN KEY (`run_id`) REFERENCES `t_ai_run` (`id`) ON DELETE RESTRICT
) ENGINE = InnoDB
  CHARACTER SET = utf8mb3
  COLLATE = utf8mb3_general_ci COMMENT = 'AI Run 可重放事件表'
  ROW_FORMAT = DYNAMIC;

CREATE TABLE `t_ai_assistant_policy`
(
    `id`                     BIGINT       NOT NULL COMMENT '全局策略固定 ID',
    `enabled_tools`          TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '是否允许工具调用',
    `allowed_tool_names`     TEXT         NOT NULL COMMENT '允许工具名 JSON 数组',
    `proposals_enabled`      TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '是否允许低风险 Proposal',
    `max_tool_calls_per_run` INT          NOT NULL COMMENT '单次 Run 最大工具调用数',
    `safety_mode`            VARCHAR(32)  NOT NULL COMMENT '安全模式',
    `network_mode`           VARCHAR(32)  NOT NULL COMMENT '联网模式',
    `context_message_limit`  INT          NOT NULL COMMENT '上下文消息条数',
    `summary_max_chars`      INT          NOT NULL COMMENT '会话摘要最大字符数',
    `max_run_seconds`        INT          NOT NULL COMMENT '单次 Run 最长秒数',
    `version`                INT          NOT NULL COMMENT '乐观锁版本',
    `create_time`            DATETIME     NOT NULL COMMENT '创建时间',
    `create_by`              INT          NOT NULL COMMENT '创建人',
    `edit_time`              DATETIME     NULL DEFAULT NULL COMMENT '编辑时间',
    `edit_by`                INT          NULL DEFAULT NULL COMMENT '编辑人',
    PRIMARY KEY (`id`) USING BTREE,
    CONSTRAINT `chk_ai_policy_singleton` CHECK (`id` = 1),
    CONSTRAINT `chk_ai_policy_safety_mode` CHECK (`safety_mode` IN ('STRICT', 'STANDARD')),
    CONSTRAINT `chk_ai_policy_network_mode` CHECK (`network_mode` IN ('DISABLED', 'PROVIDER_ONLY')),
    CONSTRAINT `chk_ai_policy_context_limit` CHECK (`context_message_limit` BETWEEN 1 AND 8)
) ENGINE = InnoDB
  CHARACTER SET = utf8mb3
  COLLATE = utf8mb3_general_ci COMMENT = 'AI 助手全局策略表'
  ROW_FORMAT = DYNAMIC;

INSERT INTO `t_ai_assistant_policy`
(`id`, `enabled_tools`, `allowed_tool_names`, `proposals_enabled`, `max_tool_calls_per_run`,
 `safety_mode`, `network_mode`, `context_message_limit`, `summary_max_chars`, `max_run_seconds`,
 `version`, `create_time`, `create_by`, `edit_time`, `edit_by`)
VALUES
(1, 1, '["create_communication_record_proposal","create_follow_task_proposal","get_business_overview","get_customer_profile","get_delivery_detail","get_inventory_alerts","get_opportunity_detail","get_quote_detail","get_test_drive_detail","get_transaction_detail","list_my_followups","list_pending_transaction_approvals","resolve_vehicle_product","search_customers"]',
 1, 8, 'STRICT', 'PROVIDER_ONLY', 8, 2000, 120, 1, CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP, 1);

CREATE TABLE `t_ai_provider_config`
(
    `id`                       BIGINT        NOT NULL AUTO_INCREMENT COMMENT 'AI 模型配置 ID',
    `config_no`                VARCHAR(64)   NOT NULL COMMENT 'AI 模型配置业务编号',
    `provider_name`            VARCHAR(64)   NOT NULL COMMENT 'Provider 名称',
    `provider_format`          VARCHAR(32)   NOT NULL COMMENT 'Provider 协议格式',
    `base_url`                 VARCHAR(255)  NOT NULL COMMENT 'Provider Base URL',
    `model_name`               VARCHAR(128)  NOT NULL COMMENT '模型名称',
    `model_display_name`       VARCHAR(128)  NOT NULL COMMENT '模型展示名称',
    `encrypted_api_key`        VARCHAR(1000) NOT NULL COMMENT '加密后的 API Key',
    `api_key_nonce`            VARCHAR(128)  NOT NULL COMMENT 'API Key 加密 nonce',
    `masked_api_key`           VARCHAR(64)   NOT NULL COMMENT 'API Key 掩码',
    `enabled`                  TINYINT(1)    NOT NULL DEFAULT 0 COMMENT '是否启用',
    `test_status`              VARCHAR(32)   NOT NULL COMMENT '测试状态',
    `last_test_time`           DATETIME      NULL DEFAULT NULL COMMENT '最近测试时间',
    `last_test_error_code`     VARCHAR(64)   NULL DEFAULT NULL COMMENT '最近测试错误码',
    `last_test_message`        VARCHAR(255)  NULL DEFAULT NULL COMMENT '最近测试摘要',
    `timeout_seconds`          INT           NOT NULL COMMENT '超时秒数',
    `max_output_tokens`        INT           NOT NULL COMMENT '最大输出 token',
    `temperature`              DECIMAL(4, 2) NOT NULL COMMENT '采样温度',
    `create_time`              DATETIME      NOT NULL COMMENT '创建时间',
    `create_by`                INT           NOT NULL COMMENT '创建人',
    `edit_time`                DATETIME      NULL DEFAULT NULL COMMENT '编辑时间',
    `edit_by`                  INT           NULL DEFAULT NULL COMMENT '编辑人',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `uk_ai_provider_config_no` (`config_no` ASC) USING BTREE,
    INDEX `idx_ai_provider_enabled` (`enabled` ASC, `edit_time` DESC, `id` DESC) USING BTREE,
    CONSTRAINT `chk_ai_provider_format` CHECK (`provider_format` IN ('OPENAI_COMPATIBLE', 'ANTHROPIC')),
    CONSTRAINT `chk_ai_provider_test_status` CHECK (`test_status` IN ('UNTESTED', 'SUCCESS', 'FAILED'))
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  CHARACTER SET = utf8mb3
  COLLATE = utf8mb3_general_ci COMMENT = 'AI 模型供应商配置表'
  ROW_FORMAT = DYNAMIC;

CREATE TABLE `t_ai_message`
(
    `id`              BIGINT        NOT NULL AUTO_INCREMENT COMMENT 'AI消息ID',
    `message_no`      VARCHAR(64)   NOT NULL COMMENT 'AI 消息业务编号',
    `conversation_id` BIGINT        NOT NULL COMMENT 'AI 会话 ID',
    `run_id`          BIGINT        NOT NULL COMMENT 'AI Run ID',
    `role`            VARCHAR(32)   NOT NULL COMMENT '消息角色',
    `sequence_no`     INT           NOT NULL COMMENT 'Run内序号',
    `visible_to_user` TINYINT(1)    NOT NULL DEFAULT 1 COMMENT '是否进入会话上下文和用户可见历史',
    `status`          VARCHAR(32)   NOT NULL DEFAULT 'ACTIVE' COMMENT '消息修订状态',
    `revision_no`     INT           NOT NULL DEFAULT 1 COMMENT '消息修订号',
    `supersedes_message_id` BIGINT  NULL DEFAULT NULL COMMENT '被替代的上一版消息 ID',
    `included_in_context` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否进入模型上下文',
    `version`         INT           NOT NULL DEFAULT 1 COMMENT '乐观锁版本',
    `content_summary` TEXT          NOT NULL COMMENT '消息安全展示文本',
    `create_time`     DATETIME      NOT NULL COMMENT '创建时间',
    `create_by`       INT           NOT NULL COMMENT '创建人',
    `edit_time`       DATETIME      NULL DEFAULT NULL COMMENT '编辑时间',
    `edit_by`         INT           NULL DEFAULT NULL COMMENT '编辑人',
    `withdrawn_time`  DATETIME      NULL DEFAULT NULL COMMENT '撤回时间',
    `withdrawn_by`    INT           NULL DEFAULT NULL COMMENT '撤回人',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `uk_ai_message_run_seq` (`run_id` ASC, `sequence_no` ASC) USING BTREE,
    UNIQUE INDEX `uk_ai_message_no` (`message_no` ASC) USING BTREE,
    INDEX `idx_ai_message_conversation_time` (`conversation_id` ASC, `visible_to_user` ASC, `create_time` ASC, `id` ASC) USING BTREE,
    CONSTRAINT `fk_ai_message_conversation` FOREIGN KEY (`conversation_id`) REFERENCES `t_ai_conversation` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_ai_message_run` FOREIGN KEY (`run_id`) REFERENCES `t_ai_run` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_ai_message_supersedes` FOREIGN KEY (`supersedes_message_id`) REFERENCES `t_ai_message` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `chk_ai_message_role` CHECK (`role` IN ('USER', 'ASSISTANT', 'SYSTEM', 'TOOL')),
    CONSTRAINT `chk_ai_message_status` CHECK (`status` IN ('ACTIVE', 'SUPERSEDED', 'WITHDRAWN'))
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  CHARACTER SET = utf8mb3
  COLLATE = utf8mb3_general_ci COMMENT = 'AI消息追踪表'
  ROW_FORMAT = DYNAMIC;

CREATE TABLE `t_ai_workflow`
(
    `id`                  BIGINT       NOT NULL AUTO_INCREMENT COMMENT 'AI 工作流 ID',
    `workflow_no`         VARCHAR(64)  NOT NULL COMMENT 'AI 工作流业务编号',
    `run_id`              BIGINT       NOT NULL COMMENT 'AI Run ID',
    `user_id`             INT          NOT NULL COMMENT '工作流 owner 用户 ID',
    `workflow_type`       VARCHAR(64)  NOT NULL COMMENT '工作流类型',
    `title`               VARCHAR(128) NOT NULL COMMENT '工作流标题',
    `status`              VARCHAR(32)  NOT NULL COMMENT '工作流状态',
    `current_step_no`     INT          NULL DEFAULT NULL COMMENT '当前步骤号',
    `context_object_type` VARCHAR(64)  NULL DEFAULT NULL COMMENT '上下文对象类型',
    `context_object_id`   VARCHAR(64)  NULL DEFAULT NULL COMMENT '上下文对象 ID',
    `pause_reason`        VARCHAR(500) NULL DEFAULT NULL COMMENT '暂停原因',
    `error_code`          VARCHAR(64)  NULL DEFAULT NULL COMMENT '错误码',
    `error_message`       VARCHAR(255) NULL DEFAULT NULL COMMENT '错误摘要',
    `started_time`        DATETIME     NULL DEFAULT NULL COMMENT '开始时间',
    `paused_time`         DATETIME     NULL DEFAULT NULL COMMENT '暂停时间',
    `resumed_time`        DATETIME     NULL DEFAULT NULL COMMENT '恢复时间',
    `completed_time`      DATETIME     NULL DEFAULT NULL COMMENT '完成时间',
    `expires_time`        DATETIME     NULL DEFAULT NULL COMMENT '过期时间',
    `create_time`         DATETIME     NOT NULL COMMENT '创建时间',
    `create_by`           INT          NOT NULL COMMENT '创建人',
    `edit_time`           DATETIME     NULL DEFAULT NULL COMMENT '编辑时间',
    `edit_by`             INT          NULL DEFAULT NULL COMMENT '编辑人',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `uk_ai_workflow_no` (`workflow_no` ASC) USING BTREE,
    INDEX `idx_ai_workflow_run` (`run_id` ASC, `create_time` ASC, `id` ASC) USING BTREE,
    INDEX `idx_ai_workflow_user_status` (`user_id` ASC, `status` ASC, `create_time` DESC) USING BTREE,
    CONSTRAINT `fk_ai_workflow_run` FOREIGN KEY (`run_id`) REFERENCES `t_ai_run` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_ai_workflow_user` FOREIGN KEY (`user_id`) REFERENCES `t_user` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `chk_ai_workflow_type` CHECK (`workflow_type` IN ('CUSTOMER_FOLLOW_UP', 'TRANSACTION_GAP_REVIEW', 'INVENTORY_RISK_REVIEW')),
    CONSTRAINT `chk_ai_workflow_status` CHECK (`status` IN ('CREATED', 'RUNNING', 'PAUSED', 'WAITING_USER_CONFIRMATION', 'COMPLETED', 'FAILED', 'CANCELLED', 'EXPIRED'))
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  CHARACTER SET = utf8mb3
  COLLATE = utf8mb3_general_ci COMMENT = 'AI 受控工作流表'
  ROW_FORMAT = DYNAMIC;

CREATE TABLE `t_ai_tool_call`
(
    `id`              BIGINT        NOT NULL AUTO_INCREMENT COMMENT 'AI 工具调用 ID',
    `run_id`          BIGINT        NOT NULL COMMENT 'AI Run ID',
    `tool_name`       VARCHAR(128)  NOT NULL COMMENT '工具名',
    `permission_code` VARCHAR(128)  NOT NULL COMMENT '权限码',
    `risk_level`      VARCHAR(32)   NOT NULL COMMENT '风险等级',
    `input_summary`   VARCHAR(1000) NOT NULL COMMENT '输入安全摘要',
    `output_summary`  VARCHAR(1000) NULL DEFAULT NULL COMMENT '输出安全摘要',
    `object_refs`     VARCHAR(1000) NULL DEFAULT NULL COMMENT '对象引用摘要',
    `display_payload_json` TEXT NULL DEFAULT NULL COMMENT '前端展示用脱敏结构化结果',
    `result_status`   VARCHAR(32)   NOT NULL COMMENT '结果状态',
    `error_code`      VARCHAR(64)   NULL DEFAULT NULL COMMENT '错误码',
    `duration_ms`     INT           NULL DEFAULT NULL COMMENT '耗时毫秒',
    `started_time`    DATETIME      NOT NULL COMMENT '开始时间',
    `completed_time`  DATETIME      NULL DEFAULT NULL COMMENT '完成时间',
    `create_by`       INT           NOT NULL COMMENT '创建人',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `idx_ai_tool_run` (`run_id` ASC, `started_time` ASC, `id` ASC) USING BTREE,
    INDEX `idx_ai_tool_name` (`tool_name` ASC, `started_time` DESC) USING BTREE,
    CONSTRAINT `fk_ai_tool_run` FOREIGN KEY (`run_id`) REFERENCES `t_ai_run` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `chk_ai_tool_risk` CHECK (`risk_level` IN ('READONLY', 'LOW', 'MEDIUM', 'HIGH')),
    CONSTRAINT `chk_ai_tool_result` CHECK (`result_status` IN ('STARTED', 'SUCCESS', 'FAILED'))
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  CHARACTER SET = utf8mb3
  COLLATE = utf8mb3_general_ci COMMENT = 'AI 工具调用追踪表'
  ROW_FORMAT = DYNAMIC;

CREATE TABLE `t_ai_action_proposal`
(
    `id`                  BIGINT        NOT NULL AUTO_INCREMENT COMMENT 'AI动作提议ID',
    `run_id`              BIGINT        NOT NULL COMMENT 'AI Run ID',
    `proposal_type`       VARCHAR(128)  NOT NULL COMMENT '提议类型',
    `status`              VARCHAR(32)   NOT NULL COMMENT '提议状态',
    `risk_level`          VARCHAR(32)   NOT NULL COMMENT '风险等级',
    `permission_code`     VARCHAR(128)  NOT NULL COMMENT '执行所需权限码',
    `related_object_type` VARCHAR(64)   NOT NULL COMMENT '关联对象类型',
    `related_object_id`   VARCHAR(64)   NOT NULL COMMENT '关联对象ID',
    `normalized_params`   TEXT          NOT NULL COMMENT '后端规范化参数',
    `params_hash`         VARCHAR(128)  NOT NULL COMMENT '规范化参数哈希',
    `params_summary`      VARCHAR(1000) NOT NULL COMMENT '参数展示摘要',
    `impact_summary`      VARCHAR(1000) NOT NULL COMMENT '影响说明',
    `expires_time`        DATETIME      NOT NULL COMMENT '过期时间',
    `confirmed_time`      DATETIME      NULL DEFAULT NULL COMMENT '确认时间',
    `executed_time`       DATETIME      NULL DEFAULT NULL COMMENT '执行时间',
    `result_summary`      VARCHAR(1000) NULL DEFAULT NULL COMMENT '执行结果摘要',
    `error_code`          VARCHAR(64)   NULL DEFAULT NULL COMMENT '错误码',
    `create_time`         DATETIME      NOT NULL COMMENT '创建时间',
    `create_by`           INT           NOT NULL COMMENT '创建人',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `idx_ai_proposal_run` (`run_id` ASC, `create_time` ASC, `id` ASC) USING BTREE,
    INDEX `idx_ai_proposal_status` (`status` ASC, `expires_time` ASC) USING BTREE,
    CONSTRAINT `fk_ai_proposal_run` FOREIGN KEY (`run_id`) REFERENCES `t_ai_run` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `chk_ai_proposal_type` CHECK (`proposal_type` IN ('create_communication_record_proposal', 'create_follow_task_proposal')),
    CONSTRAINT `chk_ai_proposal_status` CHECK (`status` IN ('PENDING_CONFIRMATION', 'CONFIRMED', 'REJECTED', 'EXPIRED', 'EXECUTED', 'FAILED')),
    CONSTRAINT `chk_ai_proposal_risk` CHECK (`risk_level` IN ('LOW'))
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  CHARACTER SET = utf8mb3
  COLLATE = utf8mb3_general_ci COMMENT = 'AI低风险动作提议表'
  ROW_FORMAT = DYNAMIC;

CREATE TABLE `t_ai_workflow_step`
(
    `id`             BIGINT        NOT NULL AUTO_INCREMENT COMMENT 'AI 工作流步骤 ID',
    `workflow_id`    BIGINT        NOT NULL COMMENT 'AI 工作流 ID',
    `step_no`        INT           NOT NULL COMMENT '步骤号',
    `step_type`      VARCHAR(64)   NOT NULL COMMENT '步骤类型',
    `title`          VARCHAR(128)  NOT NULL COMMENT '步骤标题',
    `status`         VARCHAR(32)   NOT NULL COMMENT '步骤状态',
    `tool_name`      VARCHAR(128)  NULL DEFAULT NULL COMMENT '关联工具名',
    `proposal_id`    BIGINT        NULL DEFAULT NULL COMMENT '关联 AI 提议 ID',
    `input_summary`  VARCHAR(1000) NULL DEFAULT NULL COMMENT '输入摘要',
    `output_summary` VARCHAR(1000) NULL DEFAULT NULL COMMENT '输出摘要',
    `error_code`     VARCHAR(64)   NULL DEFAULT NULL COMMENT '错误码',
    `started_time`   DATETIME      NULL DEFAULT NULL COMMENT '开始时间',
    `completed_time` DATETIME      NULL DEFAULT NULL COMMENT '完成时间',
    `create_time`    DATETIME      NOT NULL COMMENT '创建时间',
    `create_by`      INT           NOT NULL COMMENT '创建人',
    `edit_time`      DATETIME      NULL DEFAULT NULL COMMENT '编辑时间',
    `edit_by`        INT           NULL DEFAULT NULL COMMENT '编辑人',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `uk_ai_workflow_step_no` (`workflow_id` ASC, `step_no` ASC) USING BTREE,
    CONSTRAINT `fk_ai_workflow_step_workflow` FOREIGN KEY (`workflow_id`) REFERENCES `t_ai_workflow` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_ai_workflow_step_proposal` FOREIGN KEY (`proposal_id`) REFERENCES `t_ai_action_proposal` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `chk_ai_workflow_step_status` CHECK (`status` IN ('PENDING', 'RUNNING', 'WAITING_USER_CONFIRMATION', 'COMPLETED', 'FAILED', 'CANCELLED', 'EXPIRED'))
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  CHARACTER SET = utf8mb3
  COLLATE = utf8mb3_general_ci COMMENT = 'AI 工作流步骤表'
  ROW_FORMAT = DYNAMIC;

CREATE TABLE `t_ai_approval`
(
    `id`                 BIGINT       NOT NULL AUTO_INCREMENT COMMENT 'AI 提议确认 ID',
    `run_id`             BIGINT       NOT NULL COMMENT 'AI Run ID',
    `proposal_id`        BIGINT       NOT NULL COMMENT 'AI动作提议ID',
    `decision`           VARCHAR(32)  NOT NULL COMMENT '确认决定',
    `permission_summary` VARCHAR(500) NOT NULL COMMENT '确认时权限摘要',
    `reason`             VARCHAR(500) NULL DEFAULT NULL COMMENT '拒绝或失败原因',
    `result_status`      VARCHAR(32)  NOT NULL COMMENT '处理结果',
    `approved_time`      DATETIME     NOT NULL COMMENT '确认时间',
    `approved_by`        INT          NOT NULL COMMENT '确认人',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `idx_ai_approval_run` (`run_id` ASC, `approved_time` DESC) USING BTREE,
    CONSTRAINT `fk_ai_approval_run` FOREIGN KEY (`run_id`) REFERENCES `t_ai_run` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_ai_approval_proposal` FOREIGN KEY (`proposal_id`) REFERENCES `t_ai_action_proposal` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_ai_approval_user` FOREIGN KEY (`approved_by`) REFERENCES `t_user` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `chk_ai_approval_decision` CHECK (`decision` IN ('CONFIRMED', 'REJECTED', 'EXPIRED')),
    CONSTRAINT `chk_ai_approval_result` CHECK (`result_status` IN ('SUCCESS', 'FAILED', 'IGNORED'))
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  CHARACTER SET = utf8mb3
  COLLATE = utf8mb3_general_ci COMMENT = 'AI 提议确认表'
  ROW_FORMAT = DYNAMIC;

CREATE TABLE `t_ai_execution_event`
(
    `id`                  BIGINT        NOT NULL AUTO_INCREMENT COMMENT 'AI执行事件ID',
    `run_id`              BIGINT        NOT NULL COMMENT 'AI Run ID',
    `proposal_id`         BIGINT        NULL DEFAULT NULL COMMENT 'AI动作提议ID',
    `event_type`          VARCHAR(64)   NOT NULL COMMENT '事件类型',
    `result_status`       VARCHAR(32)   NOT NULL COMMENT '结果状态',
    `object_type`         VARCHAR(64)   NULL DEFAULT NULL COMMENT '业务对象类型',
    `object_id`           VARCHAR(64)   NULL DEFAULT NULL COMMENT '业务对象ID',
    `summary`             VARCHAR(1000) NOT NULL COMMENT '事件安全摘要',
    `error_code`          VARCHAR(64)   NULL DEFAULT NULL COMMENT '错误码',
    `occurred_time`       DATETIME      NOT NULL COMMENT '发生时间',
    `create_by`           INT           NOT NULL COMMENT '创建人',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `idx_ai_execution_run` (`run_id` ASC, `occurred_time` ASC, `id` ASC) USING BTREE,
    INDEX `idx_ai_execution_object` (`object_type` ASC, `object_id` ASC) USING BTREE,
    CONSTRAINT `fk_ai_execution_run` FOREIGN KEY (`run_id`) REFERENCES `t_ai_run` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_ai_execution_proposal` FOREIGN KEY (`proposal_id`) REFERENCES `t_ai_action_proposal` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `chk_ai_execution_result` CHECK (`result_status` IN ('SUCCESS', 'FAILED', 'SKIPPED'))
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  CHARACTER SET = utf8mb3
  COLLATE = utf8mb3_general_ci COMMENT = 'AI执行事件表'
  ROW_FORMAT = DYNAMIC;

CREATE TABLE `t_ai_proactive_subscription`
(
    `id`                       BIGINT        NOT NULL AUTO_INCREMENT COMMENT 'AI 主动提醒订阅 ID',
    `subscription_no`          VARCHAR(64)   NOT NULL COMMENT '订阅业务编号',
    `user_id`                  INT           NOT NULL COMMENT '订阅 owner 用户 ID',
    `subscription_type`        VARCHAR(64)   NOT NULL COMMENT '订阅类型',
    `status`                   VARCHAR(32)   NOT NULL COMMENT '订阅状态',
    `frequency`                VARCHAR(32)   NOT NULL COMMENT '频率',
    `quiet_start_time`         VARCHAR(5)    NULL DEFAULT NULL COMMENT '静默开始 HH:mm',
    `quiet_end_time`           VARCHAR(5)    NULL DEFAULT NULL COMMENT '静默结束 HH:mm',
    `daily_limit`              INT           NOT NULL DEFAULT 5 COMMENT '每日提醒上限',
    `max_results`              INT           NOT NULL DEFAULT 10 COMMENT '单次结果上限',
    `duplicate_window_minutes` INT           NOT NULL DEFAULT 60 COMMENT '重复合并窗口分钟',
    `config_summary`           VARCHAR(1000) NULL DEFAULT NULL COMMENT '配置摘要',
    `last_triggered_time`      DATETIME      NULL DEFAULT NULL COMMENT '上次触发时间',
    `next_trigger_time`        DATETIME      NULL DEFAULT NULL COMMENT '下次触发时间',
    `create_time`              DATETIME      NOT NULL COMMENT '创建时间',
    `create_by`                INT           NOT NULL COMMENT '创建人',
    `edit_time`                DATETIME      NULL DEFAULT NULL COMMENT '编辑时间',
    `edit_by`                  INT           NULL DEFAULT NULL COMMENT '编辑人',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `uk_ai_proactive_subscription_no` (`subscription_no` ASC) USING BTREE,
    INDEX `idx_ai_proactive_subscription_user` (`user_id` ASC, `status` ASC, `next_trigger_time` ASC) USING BTREE,
    CONSTRAINT `fk_ai_proactive_subscription_user` FOREIGN KEY (`user_id`) REFERENCES `t_user` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `chk_ai_proactive_subscription_type` CHECK (`subscription_type` IN ('FOLLOW_UP_REMINDER', 'TRANSACTION_EXCEPTION', 'INVENTORY_ALERT', 'DAILY_SUMMARY', 'PERIODIC_SALES_ANALYSIS')),
    CONSTRAINT `chk_ai_proactive_subscription_status` CHECK (`status` IN ('ACTIVE', 'PAUSED', 'CANCELLED')),
    CONSTRAINT `chk_ai_proactive_frequency` CHECK (`frequency` IN ('REALTIME_LIMITED', 'DAILY', 'WEEKLY', 'MONTHLY'))
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  CHARACTER SET = utf8mb3
  COLLATE = utf8mb3_general_ci COMMENT = 'AI 主动提醒订阅表'
  ROW_FORMAT = DYNAMIC;

CREATE TABLE `t_ai_proactive_event`
(
    `id`              BIGINT        NOT NULL AUTO_INCREMENT COMMENT 'AI 主动提醒事件 ID',
    `event_no`        VARCHAR(64)   NOT NULL COMMENT '提醒事件业务编号',
    `subscription_id` BIGINT        NOT NULL COMMENT 'AI 主动提醒订阅 ID',
    `user_id`         INT           NOT NULL COMMENT '事件 owner 用户 ID',
    `event_type`      VARCHAR(64)   NOT NULL COMMENT '事件类型',
    `status`          VARCHAR(32)   NOT NULL COMMENT '事件状态',
    `title`           VARCHAR(128)  NOT NULL COMMENT '标题',
    `summary`         VARCHAR(1000) NOT NULL COMMENT '安全摘要',
    `detail_summary`  VARCHAR(2000) NULL DEFAULT NULL COMMENT '详情安全摘要',
    `object_type`     VARCHAR(64)   NULL DEFAULT NULL COMMENT '关联对象类型',
    `object_id`       VARCHAR(64)   NULL DEFAULT NULL COMMENT '关联对象 ID',
    `severity`        VARCHAR(32)   NOT NULL COMMENT '严重程度',
    `generated_time`  DATETIME      NOT NULL COMMENT '生成时间',
    `delivered_time`  DATETIME      NULL DEFAULT NULL COMMENT '送达时间',
    `error_code`      VARCHAR(64)   NULL DEFAULT NULL COMMENT '错误码',
    `create_time`     DATETIME      NOT NULL COMMENT '创建时间',
    `create_by`       INT           NOT NULL COMMENT '创建人',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `uk_ai_proactive_event_no` (`event_no` ASC) USING BTREE,
    INDEX `idx_ai_proactive_event_user` (`user_id` ASC, `generated_time` DESC, `id` DESC) USING BTREE,
    INDEX `idx_ai_proactive_event_subscription` (`subscription_id` ASC, `generated_time` DESC, `id` DESC) USING BTREE,
    INDEX `idx_ai_proactive_event_object` (`object_type` ASC, `object_id` ASC, `generated_time` DESC) USING BTREE,
    CONSTRAINT `fk_ai_proactive_event_subscription` FOREIGN KEY (`subscription_id`) REFERENCES `t_ai_proactive_subscription` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_ai_proactive_event_user` FOREIGN KEY (`user_id`) REFERENCES `t_user` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `chk_ai_proactive_event_status` CHECK (`status` IN ('CREATED', 'GENERATING', 'READY', 'NO_DATA', 'FAILED', 'SKIPPED'))
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  CHARACTER SET = utf8mb3
  COLLATE = utf8mb3_general_ci COMMENT = 'AI 主动提醒事件表'
  ROW_FORMAT = DYNAMIC;

ALTER TABLE `t_customer`
    ADD CONSTRAINT `fk_customer_clue` FOREIGN KEY (`clue_id`) REFERENCES `t_clue` (`id`) ON DELETE RESTRICT;
ALTER TABLE `t_customer`
    ADD CONSTRAINT `fk_customer_owner` FOREIGN KEY (`owner_id`) REFERENCES `t_user` (`id`) ON DELETE RESTRICT;
ALTER TABLE `t_customer`
    ADD CONSTRAINT `fk_customer_merged_to` FOREIGN KEY (`merged_to_customer_id`) REFERENCES `t_customer` (`id`) ON DELETE RESTRICT;
ALTER TABLE `t_customer_remark`
    ADD CONSTRAINT `fk_customer_remark_customer` FOREIGN KEY (`customer_id`) REFERENCES `t_customer` (`id`) ON DELETE RESTRICT;
ALTER TABLE `t_customer_owner_history`
    ADD CONSTRAINT `fk_customer_owner_history_customer` FOREIGN KEY (`customer_id`) REFERENCES `t_customer` (`id`) ON DELETE RESTRICT;
ALTER TABLE `t_customer_owner_history`
    ADD CONSTRAINT `fk_customer_owner_history_from_user` FOREIGN KEY (`from_owner_id`) REFERENCES `t_user` (`id`) ON DELETE RESTRICT;
ALTER TABLE `t_customer_owner_history`
    ADD CONSTRAINT `fk_customer_owner_history_to_user` FOREIGN KEY (`to_owner_id`) REFERENCES `t_user` (`id`) ON DELETE RESTRICT;
ALTER TABLE `t_customer_owner_history`
    ADD CONSTRAINT `fk_customer_owner_history_operator` FOREIGN KEY (`operator_id`) REFERENCES `t_user` (`id`) ON DELETE RESTRICT;
ALTER TABLE `t_tran`
    ADD CONSTRAINT `fk_tran_customer` FOREIGN KEY (`customer_id`) REFERENCES `t_customer` (`id`) ON DELETE RESTRICT;
ALTER TABLE `t_tran_history`
    ADD CONSTRAINT `fk_tran_history_tran` FOREIGN KEY (`tran_id`) REFERENCES `t_tran` (`id`) ON DELETE RESTRICT;
ALTER TABLE `t_quote`
    ADD CONSTRAINT `fk_quote_customer` FOREIGN KEY (`customer_id`) REFERENCES `t_customer` (`id`) ON DELETE RESTRICT;
ALTER TABLE `t_quote`
    ADD CONSTRAINT `fk_quote_opportunity` FOREIGN KEY (`opportunity_id`) REFERENCES `t_opportunity` (`id`) ON DELETE RESTRICT;
ALTER TABLE `t_quote`
    ADD CONSTRAINT `fk_quote_current_version` FOREIGN KEY (`current_version_id`) REFERENCES `t_quote_version` (`id`) ON DELETE RESTRICT;
ALTER TABLE `t_opportunity`
    ADD CONSTRAINT `fk_opportunity_customer` FOREIGN KEY (`customer_id`) REFERENCES `t_customer` (`id`) ON DELETE RESTRICT;
ALTER TABLE `t_opportunity`
    ADD CONSTRAINT `fk_opportunity_clue` FOREIGN KEY (`clue_id`) REFERENCES `t_clue` (`id`) ON DELETE RESTRICT;
ALTER TABLE `t_opportunity`
    ADD CONSTRAINT `fk_opportunity_owner` FOREIGN KEY (`owner_id`) REFERENCES `t_user` (`id`) ON DELETE RESTRICT;
ALTER TABLE `t_opportunity`
    ADD CONSTRAINT `fk_opportunity_product` FOREIGN KEY (`product_id`) REFERENCES `t_product` (`id`) ON DELETE RESTRICT;
ALTER TABLE `t_opportunity`
    ADD CONSTRAINT `fk_opportunity_order_tran` FOREIGN KEY (`order_tran_id`) REFERENCES `t_tran` (`id`) ON DELETE RESTRICT;
ALTER TABLE `t_opportunity_stage_history`
    ADD CONSTRAINT `fk_opportunity_history_opportunity` FOREIGN KEY (`opportunity_id`) REFERENCES `t_opportunity` (`id`) ON DELETE RESTRICT;
ALTER TABLE `t_opportunity_stage_history`
    ADD CONSTRAINT `fk_opportunity_history_operator` FOREIGN KEY (`operate_by`) REFERENCES `t_user` (`id`) ON DELETE RESTRICT;
ALTER TABLE `t_quote_version`
    ADD CONSTRAINT `fk_quote_version_quote` FOREIGN KEY (`quote_id`) REFERENCES `t_quote` (`id`) ON DELETE RESTRICT;
ALTER TABLE `t_quote_version_item`
    ADD CONSTRAINT `fk_quote_item_version` FOREIGN KEY (`quote_version_id`) REFERENCES `t_quote_version` (`id`) ON DELETE RESTRICT;
ALTER TABLE `t_quote_version_item`
    ADD CONSTRAINT `fk_quote_item_product` FOREIGN KEY (`product_id`) REFERENCES `t_product` (`id`) ON DELETE RESTRICT;
ALTER TABLE `t_quote_status_history`
    ADD CONSTRAINT `fk_quote_status_history_quote` FOREIGN KEY (`quote_id`) REFERENCES `t_quote` (`id`) ON DELETE RESTRICT;
ALTER TABLE `t_tran_product`
    ADD CONSTRAINT `fk_tran_product_tran` FOREIGN KEY (`tran_id`) REFERENCES `t_tran` (`id`) ON DELETE RESTRICT;
ALTER TABLE `t_tran_product`
    ADD CONSTRAINT `fk_tran_product_product` FOREIGN KEY (`product_id`) REFERENCES `t_product` (`id`) ON DELETE RESTRICT;
ALTER TABLE `t_tran_invoice`
    ADD CONSTRAINT `fk_tran_invoice_tran` FOREIGN KEY (`tran_id`) REFERENCES `t_tran` (`id`) ON DELETE RESTRICT;
ALTER TABLE `t_tran_invoice`
    ADD CONSTRAINT `fk_tran_invoice_original` FOREIGN KEY (`original_invoice_id`) REFERENCES `t_tran_invoice` (`id`) ON DELETE RESTRICT;
ALTER TABLE `t_tran_approve`
    ADD CONSTRAINT `fk_tran_approve_tran` FOREIGN KEY (`tran_id`) REFERENCES `t_tran` (`id`) ON DELETE RESTRICT;
ALTER TABLE `t_payment`
    ADD CONSTRAINT `fk_payment_tran` FOREIGN KEY (`tran_id`) REFERENCES `t_tran` (`id`) ON DELETE RESTRICT;
ALTER TABLE `t_product_stock_record`
    ADD CONSTRAINT `fk_stock_record_product` FOREIGN KEY (`product_id`) REFERENCES `t_product` (`id`) ON DELETE RESTRICT;
ALTER TABLE `t_product_vehicle`
    ADD CONSTRAINT `fk_product_vehicle_product` FOREIGN KEY (`product_id`) REFERENCES `t_product` (`id`) ON DELETE RESTRICT;
ALTER TABLE `t_product_stock_record`
    ADD CONSTRAINT `fk_stock_record_vehicle` FOREIGN KEY (`vehicle_id`) REFERENCES `t_product_vehicle` (`id`) ON DELETE RESTRICT;
ALTER TABLE `t_product_stock_record`
    ADD CONSTRAINT `fk_stock_record_related` FOREIGN KEY (`related_record_id`) REFERENCES `t_product_stock_record` (`id`) ON DELETE RESTRICT;
ALTER TABLE `t_test_drive`
    ADD CONSTRAINT `fk_test_drive_customer` FOREIGN KEY (`customer_id`) REFERENCES `t_customer` (`id`) ON DELETE RESTRICT;
ALTER TABLE `t_test_drive`
    ADD CONSTRAINT `fk_test_drive_opportunity` FOREIGN KEY (`opportunity_id`) REFERENCES `t_opportunity` (`id`) ON DELETE RESTRICT;
ALTER TABLE `t_test_drive`
    ADD CONSTRAINT `fk_test_drive_vehicle` FOREIGN KEY (`vehicle_id`) REFERENCES `t_product_vehicle` (`id`) ON DELETE RESTRICT;
ALTER TABLE `t_test_drive`
    ADD CONSTRAINT `fk_test_drive_owner` FOREIGN KEY (`owner_id`) REFERENCES `t_user` (`id`) ON DELETE RESTRICT;
ALTER TABLE `t_test_drive_vehicle_hold`
    ADD CONSTRAINT `fk_test_drive_hold_drive` FOREIGN KEY (`test_drive_id`) REFERENCES `t_test_drive` (`id`) ON DELETE RESTRICT;
ALTER TABLE `t_test_drive_vehicle_hold`
    ADD CONSTRAINT `fk_test_drive_hold_vehicle` FOREIGN KEY (`vehicle_id`) REFERENCES `t_product_vehicle` (`id`) ON DELETE RESTRICT;
ALTER TABLE `t_test_drive_status_history`
    ADD CONSTRAINT `fk_test_drive_history_drive` FOREIGN KEY (`test_drive_id`) REFERENCES `t_test_drive` (`id`) ON DELETE RESTRICT;
ALTER TABLE `t_test_drive_status_history`
    ADD CONSTRAINT `fk_test_drive_history_operator` FOREIGN KEY (`operate_by`) REFERENCES `t_user` (`id`) ON DELETE RESTRICT;
ALTER TABLE `t_delivery`
    ADD CONSTRAINT `fk_delivery_tran` FOREIGN KEY (`tran_id`) REFERENCES `t_tran` (`id`) ON DELETE RESTRICT;
ALTER TABLE `t_delivery`
    ADD CONSTRAINT `fk_delivery_customer` FOREIGN KEY (`customer_id`) REFERENCES `t_customer` (`id`) ON DELETE RESTRICT;
ALTER TABLE `t_delivery`
    ADD CONSTRAINT `fk_delivery_vehicle` FOREIGN KEY (`vehicle_id`) REFERENCES `t_product_vehicle` (`id`) ON DELETE RESTRICT;
ALTER TABLE `t_delivery`
    ADD CONSTRAINT `fk_delivery_responsible_user` FOREIGN KEY (`responsible_user_id`) REFERENCES `t_user` (`id`) ON DELETE RESTRICT;
ALTER TABLE `t_delivery_check_item`
    ADD CONSTRAINT `fk_delivery_check_delivery` FOREIGN KEY (`delivery_id`) REFERENCES `t_delivery` (`id`) ON DELETE RESTRICT;
ALTER TABLE `t_delivery_check_item`
    ADD CONSTRAINT `fk_delivery_check_responsible_user` FOREIGN KEY (`responsible_user_id`) REFERENCES `t_user` (`id`) ON DELETE RESTRICT;

SET FOREIGN_KEY_CHECKS = 1;
