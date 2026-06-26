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
    `start_time`  datetime                                                      NULL DEFAULT NULL COMMENT '活动开始时间',
    `end_time`    datetime                                                      NULL DEFAULT NULL COMMENT '活动结束时间',
    `cost`        decimal(11, 2)                                                NULL DEFAULT NULL COMMENT '活动预算',
    `description` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '活动描述',
    `create_time` datetime                                                      NULL DEFAULT NULL COMMENT '活动创建时间',
    `create_by`   int                                                           NULL DEFAULT NULL COMMENT '活动创建人',
    `edit_time`   datetime                                                      NULL DEFAULT NULL COMMENT '活动编辑时间',
    `edit_by`     int                                                           NULL DEFAULT NULL COMMENT '活动编辑人',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `owner` (`owner_id` ASC) USING BTREE,
    INDEX `create_by` (`create_by` ASC) USING BTREE,
    INDEX `edit_by` (`edit_by` ASC) USING BTREE
) ENGINE = InnoDB
  AUTO_INCREMENT = 49
  CHARACTER SET = utf8mb3
  COLLATE = utf8mb3_general_ci COMMENT = '市场活动表'
  ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of t_activity
-- ----------------------------

INSERT INTO `t_activity`
(id, owner_id, name, start_time, end_time, cost, description, create_time, create_by, edit_time, edit_by)
VALUES
(1, 5, '六月店庆新车品鉴会', '2026-06-06 09:30:00', '2026-06-07 18:00:00', 38000.00,
 '面向近三个月高意向客户的店内品鉴活动，设置车型讲解、金融咨询和置换评估。',
 '2026-05-12 10:00:00', 5, '2026-06-03 16:20:00', 5),
(2, 9, '周末SUV试驾专场', '2026-06-13 09:00:00', '2026-06-14 17:30:00', 12000.00,
 '按预约时段组织SUV道路试驾，重点覆盖家庭增购与换购客户。',
 '2026-05-28 14:10:00', 9, '2026-06-10 11:00:00', 5),
(3, 5, '企业客户用车方案说明会', '2026-06-18 14:00:00', '2026-06-18 17:00:00', 8000.00,
 '邀请园区企业行政与采购负责人，介绍商务车型、开票及批量采购方案。',
 '2026-06-01 09:30:00', 5, NULL, NULL),
(4, 9, '老客户夏季用车关怀日', '2026-07-04 09:30:00', '2026-07-04 17:00:00', 15000.00,
 '为已交付客户提供免费车况检查，并收集增购、换购和转介绍线索。',
 '2026-06-15 10:20:00', 9, NULL, NULL);

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
    INDEX `t_activity_remark_ibfk_3` (`edit_by` ASC) USING BTREE
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
    INDEX `t_clue_remark_ibfk_4` (`note_way` ASC) USING BTREE
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
    `id`        int                                                           NOT NULL AUTO_INCREMENT COMMENT '主键，自动增长，字典类型ID',
    `type_code` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci  NOT NULL COMMENT '字典类型代码',
    `type_name` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci  NULL DEFAULT NULL COMMENT '字典类型名称',
    `remark`    varchar(128) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `code` (`type_code` ASC) USING BTREE
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


-- ----------------------------
-- Table structure for t_dic_value
-- ----------------------------
DROP TABLE IF EXISTS `t_dic_value`;
CREATE TABLE `t_dic_value`
(
    `id`         int                                                          NOT NULL AUTO_INCREMENT COMMENT '主键，自动增长，字典值ID',
    `type_code`  varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '字典类型代码',
    `type_value` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '字典值',
    `value_code` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '稳定业务编码',
    `order`      int                                                          NULL DEFAULT NULL COMMENT '字典值排序',
    `remark`     varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE KEY `uk_type_value_code` (`type_code`, `value_code`),
    INDEX `t_dic_value_ibfk_1` (`type_code` ASC) USING BTREE
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

-- ----------------------------
-- Table structure for t_permission
-- ----------------------------
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
    `enabled`   tinyint(1)                                                    NOT NULL DEFAULT 1,
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE KEY `uk_permission_code` (`code`),
    KEY `idx_permission_parent` (`parent_id`),
    CONSTRAINT `chk_permission_type` CHECK (`type` IN ('menu', 'button')),
    CONSTRAINT `fk_permission_parent` FOREIGN KEY (`parent_id`) REFERENCES `t_permission` (`id`) ON DELETE RESTRICT
) ENGINE = InnoDB
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT = '权限表'
  ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of t_permission
-- ----------------------------
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`) VALUES
('仪表盘', 'menu:dashboard', '/dashboard', 'menu', NULL, 0, 'Gauge', 1),
('市场活动', 'menu:activity', NULL, 'menu', NULL, 1, 'OfficeBuilding', 1),
('线索管理', 'menu:clue', NULL, 'menu', NULL, 2, 'Magnet', 1),
('客户管理', 'menu:customer', NULL, 'menu', NULL, 3, 'User', 1),
('交易管理', 'menu:tran', NULL, 'menu', NULL, 4, 'Wallet', 1),
('产品管理', 'menu:product', NULL, 'menu', NULL, 5, 'Memo', 1),
('字典管理', 'menu:dict', NULL, 'menu', NULL, 6, 'Grid', 1),
('用户管理', 'menu:user', NULL, 'menu', NULL, 7, 'Stamp', 1);

INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '市场活动', 'page:activity:list', '/dashboard/activity', 'menu', id, 1, 'CreditCard', 1 FROM `t_permission` WHERE code = 'menu:activity';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '线索管理', 'page:clue:list', '/dashboard/clue', 'menu', id, 1, 'Paperclip', 1 FROM `t_permission` WHERE code = 'menu:clue';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '客户管理', 'page:customer:list', '/dashboard/customer', 'menu', id, 1, 'UserFilled', 1 FROM `t_permission` WHERE code = 'menu:customer';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '交易管理', 'page:tran:list', '/dashboard/tran', 'menu', id, 1, 'Coin', 1 FROM `t_permission` WHERE code = 'menu:tran';
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
SELECT '客户管理-列表', 'customer:list', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'page:customer:list';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '客户管理-查看', 'customer:view', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'page:customer:list';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '客户管理-导出', 'customer:export', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'page:customer:list';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '客户管理-转客户', 'customer:transfer', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'page:customer:list';
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
SELECT '交易管理-结算', 'tran:settle', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'page:tran:list';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '交易管理-重新提交', 'tran:resubmit', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'page:tran:list';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '交易管理-审批', 'tran:approve', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'page:tran:list';
INSERT INTO `t_permission` (`name`, `code`, `url`, `type`, `parent_id`, `order_no`, `icon`, `enabled`)
SELECT '交易管理-发票', 'tran:invoice', NULL, 'button', id, NULL, NULL, 1 FROM `t_permission` WHERE code = 'page:tran:list';
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

-- ----------------------------
-- Table structure for t_role
-- ----------------------------
DROP TABLE IF EXISTS `t_role`;
CREATE TABLE `t_role`
(
    `id`        int                                                          NOT NULL AUTO_INCREMENT,
    `role`      varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
    `role_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
    `enabled`   tinyint(1)                                                   NOT NULL DEFAULT 1,
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE KEY `uk_role_code` (`role`)
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

-- ----------------------------
-- Table structure for t_role_permission
-- ----------------------------
DROP TABLE IF EXISTS `t_role_permission`;
CREATE TABLE `t_role_permission`
(
    `role_id`       int NOT NULL,
    `permission_id` int NOT NULL,
    PRIMARY KEY (`role_id`, `permission_id`) USING BTREE,
    INDEX `idx_role_permission_permission` (`permission_id` ASC) USING BTREE,
    CONSTRAINT `fk_role_permission_role` FOREIGN KEY (`role_id`) REFERENCES `t_role` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_role_permission_permission` FOREIGN KEY (`permission_id`) REFERENCES `t_permission` (`id`) ON DELETE CASCADE
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
WHERE r.role = 'sales_consultant' AND p.code IN ('menu:activity', 'page:activity:list', 'activity:list', 'activity:view', 'menu:clue', 'page:clue:list', 'clue:list', 'clue:view', 'clue:add', 'clue:edit', 'menu:customer', 'page:customer:list', 'customer:list', 'customer:view', 'customer:transfer', 'menu:tran', 'page:tran:list', 'tran:list', 'tran:view', 'tran:create', 'tran:edit', 'tran:settle', 'tran:resubmit');

INSERT INTO `t_role_permission` (`role_id`, `permission_id`)
SELECT r.id, p.id FROM `t_role` r CROSS JOIN `t_permission` p
WHERE r.role = 'sales_manager' AND p.code IN ('menu:dashboard', 'menu:activity', 'page:activity:list', 'activity:list', 'activity:view', 'activity:add', 'activity:edit', 'activity:delete', 'menu:clue', 'page:clue:list', 'clue:list', 'clue:view', 'clue:add', 'clue:edit', 'clue:delete', 'clue:import', 'menu:customer', 'page:customer:list', 'customer:list', 'customer:view', 'customer:transfer', 'customer:export', 'menu:tran', 'page:tran:list', 'tran:list', 'tran:view', 'tran:create', 'tran:edit', 'tran:delete', 'tran:settle', 'tran:resubmit', 'tran:approve', 'statistic:view');

INSERT INTO `t_role_permission` (`role_id`, `permission_id`)
SELECT r.id, p.id FROM `t_role` r CROSS JOIN `t_permission` p
WHERE r.role = 'marketing_specialist' AND p.code IN ('menu:dashboard', 'menu:activity', 'page:activity:list', 'activity:list', 'activity:view', 'activity:add', 'activity:edit', 'activity:delete', 'menu:clue', 'page:clue:list', 'clue:list', 'clue:view', 'clue:add', 'clue:edit', 'clue:import', 'statistic:view');

INSERT INTO `t_role_permission` (`role_id`, `permission_id`)
SELECT r.id, p.id FROM `t_role` r CROSS JOIN `t_permission` p
WHERE r.role = 'finance_specialist' AND p.code IN ('menu:dashboard', 'menu:tran', 'page:tran:list', 'tran:list', 'tran:view', 'tran:invoice', 'tran:payment', 'tran:payment:confirm', 'tran:refund', 'tran:refund:approve', 'tran:refund:execute', 'statistic:view');

INSERT INTO `t_role_permission` (`role_id`, `permission_id`)
SELECT r.id, p.id FROM `t_role` r CROSS JOIN `t_permission` p
WHERE r.role = 'inventory_specialist' AND p.code IN ('menu:product', 'page:product:list', 'page:product:category', 'page:product:promotion', 'page:product:stock', 'product:list', 'product:view', 'product:add', 'product:edit', 'product:delete', 'product:category:list', 'product:category:view', 'product:category:add', 'product:category:edit', 'product:category:delete', 'product:promotion:list', 'product:promotion:view', 'product:promotion:add', 'product:promotion:edit', 'product:promotion:delete', 'product:stock:view', 'product:stock:adjust');

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
(`id`, `tran_id`, `stage`, `money`, `expected_date`, `create_time`, `create_by`)
VALUES
(1, 1, 'QUOTATION', 386800.00, '2026-04-18 00:00:00', '2026-04-08 10:12:00', 2),
(2, 1, 'PENDING',   386800.00, '2026-04-18 00:00:00', '2026-04-09 09:30:00', 2),
(3, 1, 'APPROVED',  386800.00, '2026-04-18 00:00:00', '2026-04-09 15:20:00', 4),
(4, 1, 'PAYMENT',   386800.00, '2026-04-18 00:00:00', '2026-04-10 10:10:00', 2),
(5, 1, 'COMPLETED', 386800.00, '2026-04-18 00:00:00', '2026-04-18 16:40:00', 2),
(6, 2, 'QUOTATION', 579800.00, '2026-06-28 00:00:00', '2026-05-22 14:25:00', 2),
(7, 2, 'PENDING',   579800.00, '2026-06-28 00:00:00', '2026-06-02 11:00:00', 2),
(8, 2, 'APPROVED',  579800.00, '2026-06-28 00:00:00', '2026-06-03 09:15:00', 4),
(9, 2, 'PAYMENT',   579800.00, '2026-06-28 00:00:00', '2026-06-20 11:30:00', 2),
(10, 3, 'QUOTATION', 439900.00, '2026-07-05 00:00:00', '2026-06-03 09:45:00', 3),
(11, 3, 'PENDING',   439900.00, '2026-07-05 00:00:00', '2026-06-18 16:20:00', 3),
(12, 3, 'APPROVED',  439900.00, '2026-07-05 00:00:00', '2026-06-19 17:10:00', 4),
(13, 4, 'QUOTATION', 509800.00, '2026-07-20 00:00:00', '2026-06-12 11:06:00', 8),
(14, 4, 'PENDING',   509800.00, '2026-07-20 00:00:00', '2026-06-20 09:15:00', 8),
(15, 6, 'QUOTATION', 398800.00, '2026-06-15 00:00:00', '2026-05-15 10:30:00', 8),
(16, 6, 'LOST',      398800.00, '2026-06-15 00:00:00', '2026-06-10 18:05:00', 8),
(17, 8, 'QUOTATION', 488000.00, '2026-06-30 00:00:00', '2026-05-30 12:10:00', 3),
(18, 8, 'CANCELLED', 488000.00, '2026-06-30 00:00:00', '2026-06-16 14:20:00', 3);

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
    `amount`       decimal(10, 2) NOT NULL COMMENT '发票金额',
    `status`       varchar(20)    NOT NULL COMMENT '发票状态：PENDING-待开具, ISSUED-已开具, VOID-已作废',
    `remark`       varchar(255)   NULL DEFAULT NULL COMMENT '备注信息',
    `issue_time`   datetime       NULL DEFAULT NULL COMMENT '开具时间',
    `create_time`  datetime       NULL DEFAULT NULL COMMENT '创建时间',
    `create_by`    int            NULL DEFAULT NULL COMMENT '创建人',
    `edit_time`    datetime       NULL DEFAULT NULL COMMENT '编辑时间',
    `edit_by`      int            NULL DEFAULT NULL COMMENT '编辑人',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE KEY `uk_invoice_no` (`invoice_no`),
    INDEX `t_tran_invoice_ibfk_1` (`tran_id` ASC) USING BTREE,
    INDEX `t_tran_invoice_ibfk_2` (`create_by` ASC) USING BTREE,
    INDEX `t_tran_invoice_ibfk_3` (`edit_by` ASC) USING BTREE
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
 `bank_account`, `address`, `phone`, `amount`, `status`, `remark`, `issue_time`,
 `create_time`, `create_by`, `edit_time`, `edit_by`)
VALUES
(1, 1, '044002600111', 'VAT_NORMAL', '陈明', 'PERS-440305-0001', NULL, NULL, NULL,
 '13800002001', 386800.00, 'ISSUED', '机动车销售统一发票已随交车资料交付。',
 '2026-04-12 10:25:00', '2026-04-10 15:10:00', 6, '2026-04-12 10:25:00', 6),
(2, 7, '044002600086', 'VAT_SPECIAL', '深圳市远景建筑设计有限公司',
 '91440300MA5F8X2K7R', '招商银行深圳科技园支行', '755900010012345',
 '深圳市南山区科苑路88号', '0755-86661234', 455800.00, 'ISSUED',
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
DROP TABLE IF EXISTS `t_product_promotion`;
CREATE TABLE `t_product_promotion`
(
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '促销活动的唯一标识符',
    `product_id`  BIGINT       NOT NULL COMMENT '关联的产品ID',
    `name`        VARCHAR(255) NOT NULL COMMENT '促销名称',
    `type`        VARCHAR(50)    DEFAULT NULL COMMENT '促销类型',
    `discount`    DECIMAL(10, 2) DEFAULT NULL COMMENT '折扣或金额',
    `start_time`  DATETIME       DEFAULT NULL COMMENT '促销开始时间',
    `end_time`    DATETIME       DEFAULT NULL COMMENT '促销结束时间',
    `status`      VARCHAR(50)    DEFAULT NULL COMMENT '促销状态',
    `create_time` DATETIME       DEFAULT NULL COMMENT '创建时间',
    `update_time` DATETIME       DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_product_promotion_product` (`product_id`),
    CONSTRAINT `fk_product_promotion_product` FOREIGN KEY (`product_id`) REFERENCES `t_product` (`id`) ON DELETE RESTRICT
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='产品促销表';

-- ----------------------------
-- Records of t_product_promotion
-- ----------------------------

INSERT INTO `t_product_promotion`
(`id`, `product_id`, `name`, `type`, `discount`, `start_time`, `end_time`, `status`, `create_time`, `update_time`)
VALUES
(1, 1, '宝马X5六月现车补贴', 'AMOUNT', 12000.00, '2026-06-01 00:00:00', '2026-06-30 23:59:59', '进行中', '2026-05-20 10:00:00', '2026-06-01 09:00:00'),
(2, 11, '雷克萨斯ES新能源置换补贴', 'AMOUNT', 8000.00, '2026-06-15 00:00:00', '2026-08-31 23:59:59', '进行中', '2026-06-05 14:00:00', '2026-06-15 09:00:00'),
(3, 9, '奔驰E级暑期金融贴息', 'AMOUNT', 6000.00, '2026-07-01 00:00:00', '2026-08-31 23:59:59', '待开始', '2026-06-18 11:00:00', '2026-06-18 11:00:00');

-- ----------------------------
-- Table structure for t_product_stock_record
-- ----------------------------
DROP TABLE IF EXISTS `t_product_stock_record`;
CREATE TABLE `t_product_stock_record`
(
    `id`          BIGINT NOT NULL AUTO_INCREMENT COMMENT '库存记录的唯一标识符',
    `product_id`  BIGINT NOT NULL COMMENT '关联的产品ID',
    `quantity`    INT         DEFAULT NULL COMMENT '变动数量',
    `type`        VARCHAR(50) DEFAULT NULL COMMENT '记录类型，如入库、出库',
    `remark`      TEXT        DEFAULT NULL COMMENT '备注信息',
    `create_time` DATETIME    DEFAULT NULL COMMENT '记录创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_product_id` (`product_id`) COMMENT '产品ID索引'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='产品库存记录表';


-- ----------------------------
-- Records of t_product_stock_record
-- ----------------------------
INSERT INTO `t_product_stock_record`
(`id`, `product_id`, `quantity`, `type`, `remark`, `create_time`)
VALUES
(1, 6, 5, '入库', '厂家到店批次 RK20260402，验收入库5台。', '2026-04-02 14:20:00'),
(2, 6, -1, '出库', '交易 XS202604080001 完成交付出库。', '2026-04-18 15:50:00'),
(3, 1, 4, '入库', '厂家到店批次 RK20260518，验收入库4台。', '2026-05-18 11:10:00'),
(4, 1, -1, '锁定', '交易 XS202605220001 锁定现车1台，待收首付款。', '2026-06-20 11:35:00'),
(5, 7, 4, '入库', '厂家到店批次 RK20260220，验收入库4台。', '2026-02-20 16:30:00'),
(6, 7, -1, '出库', '交易 XS202603110001 完成交付出库。', '2026-03-28 16:55:00'),
(7, 9, 3, '入库', '厂家到店批次 RK20260608，验收入库3台。', '2026-06-08 13:45:00'),
(8, 9, -1, '锁定', '交易 XS202606120001 预留现车1台，审批未完成。', '2026-06-20 09:20:00');

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

create table t_user
(
    id                     int auto_increment comment '主键，自动增长，用户ID'
        primary key,
    login_act              varchar(32) null comment '登录账号',
    login_pwd              varchar(64) null comment '登录密码',
    name                   varchar(32) null comment '用户姓名',
    phone                  varchar(18) null comment '用户手机',
    email                  varchar(64) null comment '用户邮箱',
    account_no_expired     int         null comment '账户是否没有过期，0已过期 1正常',
    credentials_no_expired int         null comment '密码是否没有过期，0已过期 1正常',
    account_no_locked      int         null comment '账号是否没有锁定，0已锁定 1正常',
    account_enabled        int         null comment '账号是否启用，0禁用 1启用',
    create_time            datetime    null comment '创建时间',
    create_by              int         null comment '创建人',
    edit_time              datetime    null comment '编辑时间',
    edit_by                int         null comment '编辑人',
    last_login_time        datetime    null comment '最近登录时间',
    constraint email
        unique (email),
    constraint login_act
        unique (login_act),
    constraint phone
        unique (phone)
)ENGINE = InnoDB
 AUTO_INCREMENT = 1
 CHARACTER SET = utf8mb3
 COLLATE = utf8mb3_general_ci COMMENT = '用户表'
 ROW_FORMAT = DYNAMIC;

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
 account_no_locked, account_enabled, create_time, create_by, edit_time, edit_by, last_login_time)
VALUES
(1, 'admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '系统管理员', '13800001001', 'admin@qicheng-auto.example', 1, 1, 1, 1, '2025-01-06 09:00:00', NULL, '2026-06-01 10:30:00', 1, '2026-06-21 08:42:00'),
(2, 'chenchen', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '陈晨', '13800001002', 'chen.chen@qicheng-auto.example', 1, 1, 1, 1, '2025-02-10 09:15:00', 1, NULL, NULL, '2026-06-21 08:51:00'),
(3, 'wanglei', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '王磊', '13800001003', 'wang.lei@qicheng-auto.example', 1, 1, 1, 1, '2025-03-03 10:20:00', 1, NULL, NULL, '2026-06-20 18:20:00'),
(4, 'limin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '李敏', '13800001004', 'li.min@qicheng-auto.example', 1, 1, 1, 1, '2025-01-15 14:00:00', 1, NULL, NULL, '2026-06-21 09:03:00'),
(5, 'zhouqi', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '周琪', '13800001005', 'zhou.qi@qicheng-auto.example', 1, 1, 1, 1, '2025-04-08 11:30:00', 1, NULL, NULL, '2026-06-20 17:42:00'),
(6, 'zhaoqian', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '赵倩', '13800001006', 'zhao.qian@qicheng-auto.example', 1, 1, 1, 1, '2025-02-18 09:40:00', 1, NULL, NULL, '2026-06-21 08:58:00'),
(7, 'sunqiang', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '孙强', '13800001007', 'sun.qiang@qicheng-auto.example', 1, 1, 1, 1, '2025-05-12 13:20:00', 1, NULL, NULL, '2026-06-20 19:05:00'),
(8, 'wuyue', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '吴悦', '13800001008', 'wu.yue@qicheng-auto.example', 1, 1, 1, 1, '2025-06-09 10:10:00', 4, NULL, NULL, '2026-06-21 08:47:00'),
(9, 'liujia', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '刘佳', '13800001009', 'liu.jia@qicheng-auto.example', 1, 1, 1, 1, '2025-08-04 15:30:00', 4, NULL, NULL, '2026-06-20 18:36:00'),
(10, 'hejun', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '何军', '13800001010', 'he.jun@qicheng-auto.example', 1, 1, 0, 0, '2025-09-15 09:00:00', 1, '2026-06-10 18:00:00', 1, '2026-06-10 17:42:00');

INSERT INTO `t_clue_owner_history`
(`clue_id`, `from_owner_id`, `to_owner_id`, `assigned_by`, `reason`, `assigned_time`)
SELECT id, NULL, owner_id, COALESCE(create_by, owner_id), '初始化线索责任归属', COALESCE(create_time, NOW())
FROM `t_clue`
WHERE owner_id IS NOT NULL;

create table t_user_role
(
    user_id int not null,
    role_id int not null,
    primary key (user_id, role_id),
    constraint fk_user_role_user foreign key (user_id) references t_user (id) on delete cascade,
    constraint fk_user_role_role foreign key (role_id) references t_role (id) on delete cascade
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
    `action_code` varchar(32)  NOT NULL COMMENT '审计动作代码',
    `module_name` varchar(64)  NULL DEFAULT NULL COMMENT '模块名称',
    `resource_id` varchar(64)  NULL DEFAULT NULL COMMENT '业务资源ID',
    `detail`      varchar(512) NULL DEFAULT NULL COMMENT '结构化审计摘要JSON',
    `ip`          varchar(64)  NULL DEFAULT NULL COMMENT '操作IP',
    `create_time` datetime     NULL DEFAULT NULL COMMENT '创建时间',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  CHARACTER SET = utf8mb3
  COLLATE = utf8mb3_general_ci COMMENT = '操作审计日志表'
  ROW_FORMAT = DYNAMIC;


-- ----------------------------
-- Records of t_operation_log
-- ----------------------------
INSERT INTO `t_operation_log`
(`id`, `user_id`, `user_name`, `action_code`, `module_name`, `resource_id`, `detail`, `ip`, `create_time`)
VALUES
(1, 2, '陈晨', 'TRAN_CREATE', '交易管理', '2', '{"tranNo":"XS202605220001","customerId":2}', '10.20.1.32', '2026-05-22 14:25:00'),
(2, 4, '李敏', 'TRAN_APPROVE', '交易审批', '3', '{"result":"APPROVED","money":439900.00}', '10.20.1.18', '2026-06-19 17:10:00'),
(3, 6, '赵倩', 'PAYMENT_CONFIRM', '收款管理', '4', '{"paymentNo":"SK202606160001","amount":20000.00}', '10.20.2.11', '2026-06-16 14:05:00'),
(4, 6, '赵倩', 'PAYMENT_REFUND', '退款管理', '5', '{"paymentNo":"TK202606160001","amount":20000.00}', '10.20.2.11', '2026-06-16 14:18:00'),
(5, 7, '孙强', 'STOCK_LOCK', '库存管理', '9', '{"tranNo":"XS202606120001","quantity":1}', '10.20.3.25', '2026-06-20 09:20:00');

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
    INDEX `idx_tran_id` (`tran_id` ASC) USING BTREE
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
(4, 8, 'SK202606020001', 20000.00, 'WECHAT', 'DEPOSIT', 'REFUNDED',
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
    `executed_time`       datetime       NULL DEFAULT NULL COMMENT '执行时间',
    `create_time`         datetime       NULL DEFAULT NULL COMMENT '创建时间',
    `create_by`           int            NULL DEFAULT NULL COMMENT '创建人',
    `edit_time`           datetime       NULL DEFAULT NULL COMMENT '编辑时间',
    `edit_by`             int            NULL DEFAULT NULL COMMENT '编辑人',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `idx_refund_tran_id` (`tran_id` ASC) USING BTREE,
    INDEX `idx_refund_original_payment` (`original_payment_id` ASC) USING BTREE,
    CONSTRAINT `fk_refund_request_tran` FOREIGN KEY (`tran_id`) REFERENCES `t_tran` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_refund_request_original_payment` FOREIGN KEY (`original_payment_id`) REFERENCES `t_payment` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_refund_request_refund_payment` FOREIGN KEY (`refund_payment_id`) REFERENCES `t_payment` (`id`) ON DELETE RESTRICT
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
 `approve_comment`, `executed_by`, `executed_time`, `create_time`, `create_by`, `edit_time`, `edit_by`)
VALUES
(1, 8, 4, 5, 20000.00, 'ORDER_CANCEL', '客户取消订单，定金原路退回。', 'EXECUTED',
 3, '2026-06-16 14:10:00', 4, '2026-06-16 14:12:00',
 '同意按取消订单流程退款。', 6, '2026-06-16 14:18:00',
 '2026-06-16 14:10:00', 3, '2026-06-16 14:18:00', 6);

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
ALTER TABLE `t_tran_product`
    ADD CONSTRAINT `fk_tran_product_tran` FOREIGN KEY (`tran_id`) REFERENCES `t_tran` (`id`) ON DELETE RESTRICT;
ALTER TABLE `t_tran_product`
    ADD CONSTRAINT `fk_tran_product_product` FOREIGN KEY (`product_id`) REFERENCES `t_product` (`id`) ON DELETE RESTRICT;
ALTER TABLE `t_tran_invoice`
    ADD CONSTRAINT `fk_tran_invoice_tran` FOREIGN KEY (`tran_id`) REFERENCES `t_tran` (`id`) ON DELETE RESTRICT;
ALTER TABLE `t_tran_approve`
    ADD CONSTRAINT `fk_tran_approve_tran` FOREIGN KEY (`tran_id`) REFERENCES `t_tran` (`id`) ON DELETE RESTRICT;
ALTER TABLE `t_payment`
    ADD CONSTRAINT `fk_payment_tran` FOREIGN KEY (`tran_id`) REFERENCES `t_tran` (`id`) ON DELETE RESTRICT;
ALTER TABLE `t_product_stock_record`
    ADD CONSTRAINT `fk_stock_record_product` FOREIGN KEY (`product_id`) REFERENCES `t_product` (`id`) ON DELETE RESTRICT;

SET FOREIGN_KEY_CHECKS = 1;
