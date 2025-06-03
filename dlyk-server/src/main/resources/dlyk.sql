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
INSERT INTO `t_activity` (id, owner_id, name, start_time, end_time, cost, description, create_time, create_by,
                          edit_time, edit_by)
VALUES (2, 2, '充话费-送手机', '2023-03-28 17:48:49', '2023-04-30 17:48:54', 9000.00, '充话费,送手机,充满送Iphone14',
        '2023-03-28 17:49:28', 1, '2023-05-15 00:10:52', 1),
       (7, 1, '抖音推广', '2023-04-01 12:03:09', '2023-04-30 12:03:09', 15800.00, '抖音直播推广活动1', NULL, 6, NULL,
        NULL),
       (8, 1, '抖音推广', '2023-04-01 12:03:09', '2023-04-30 12:03:09', 15800.00, '抖音直播推广活动2', NULL, 7,
        '2023-04-28 14:28:53', NULL),
       (10, 2, '11', '2023-04-11 00:30:58', '2023-04-30 00:31:00', 131.00, '12312312', NULL, NULL,
        '2023-04-28 13:33:56', NULL),
       (11, 3, '1213', '2023-04-01 00:00:00', '2023-04-30 00:31:13', 123123.00, '23123123', NULL, NULL, NULL, NULL),
       (12, 1, '抖音推广', '2023-04-01 12:03:09', '2023-04-01 12:03:09', 15800.00, '抖音直播推广活动1', NULL, NULL,
        NULL, NULL),
       (13, 1, '抖音推广', '2023-04-01 12:03:09', '2023-04-01 12:03:09', 15800.00, '抖音直播推广活动2', NULL, NULL,
        '2023-04-28 13:27:43', NULL),
       (14, 1, '抖音推广', '2023-04-01 12:03:09', '2023-04-01 12:03:09', 15800.00, '抖音直播推广活动1', NULL, NULL,
        NULL, NULL),
       (15, 1, '抖音推广', '2023-04-01 12:03:09', '2023-04-01 12:03:09', 15800.00, '抖音直播推广活动11111', NULL, NULL,
        '2023-04-26 17:09:49', NULL),
       (16, 1, '抖音推广', '2023-04-01 12:03:09', '2023-04-01 12:03:09', 15800.00, '抖音直播推广活动1', NULL, NULL,
        NULL, NULL),
       (17, 1, '抖音推广', '2023-04-01 12:03:09', '2023-04-01 12:03:09', 15800.00, '抖音直播推广活动2', NULL, NULL,
        NULL, NULL),
       (18, 1, '抖音推广', '2023-04-01 12:03:09', '2023-04-01 12:03:09', 15800.00, '抖音直播推广活动1', NULL, NULL,
        NULL, NULL),
       (19, 1, '抖音推广', '2023-04-01 12:03:09', '2023-04-01 12:03:09', 15800.00, '抖音直播推广活动2', NULL, NULL,
        NULL, NULL),
       (20, 1, '抖音推广', '2023-04-01 12:03:09', '2023-04-01 12:03:09', 15800.00, '抖音直播推广活动1', NULL, NULL,
        NULL, NULL),
       (21, 1, '抖音推广', '2023-04-01 12:03:09', '2023-04-01 12:03:09', 15800.00, '抖音直播推广活动2', NULL, NULL,
        NULL, NULL),
       (22, 1, '抖音推广', '2023-04-01 10:03:09', '2023-04-09 12:03:09', 15800.00, '抖音直播推广活动1', NULL, NULL,
        NULL, NULL),
       (23, 1, '抖音推广', '2023-04-01 10:03:09', '2023-04-11 12:03:09', 15800.00, '抖音直播推广活动2', NULL, NULL,
        NULL, NULL),
       (24, 1, '抖音推广', '2023-04-01 10:03:09', '2023-04-12 12:03:09', 15800.00, '抖音直播推广活动1', NULL, NULL,
        NULL, NULL),
       (25, 1, '抖音推广', '2023-04-01 12:03:09', '2023-04-01 12:03:09', 15800.00, '抖音直播推广活动2', NULL, NULL,
        NULL, NULL),
       (26, 1, '抖音推广', '2023-04-01 12:03:09', '2023-04-01 12:03:09', 15800.00, '抖音直播推广活动1', NULL, NULL,
        NULL, NULL),
       (27, 1, '抖音推广', '2023-04-01 12:03:09', '2023-04-01 12:03:09', 15800.00, '抖音直播推广活动2', NULL, NULL,
        NULL, NULL),
       (28, 1, '抖音推广', '2023-04-01 12:03:09', '2023-04-01 12:03:09', 15800.00, '抖音直播推广活动1', NULL, NULL,
        NULL, NULL),
       (29, 1, '抖音推广', '2023-04-01 12:03:09', '2023-04-01 12:03:09', 15800.00, '抖音直播推广活动2', NULL, NULL,
        NULL, NULL),
       (40, 1, '抖音推广11', '2023-04-01 12:03:09', '2023-04-01 12:03:09', 15800.00, '抖音直播推广活动1', NULL, NULL,
        NULL, NULL),
       (41, 1, '抖音推广11', '2023-04-01 12:03:09', '2023-04-01 12:03:09', 15800.00, '抖音直播推广活动2', NULL, NULL,
        NULL, NULL),
       (42, 1, '抖音推广11', '2023-04-01 12:03:09', '2023-04-01 12:03:09', 15800.00, '抖音直播推广活动1', NULL, NULL,
        NULL, NULL),
       (43, 1, '抖音推广11', '2023-04-01 12:03:09', '2023-04-01 12:03:09', 15800.00, '抖音直播推广活动2', NULL, NULL,
        NULL, NULL),
       (44, 1, '抖音推广11', '2023-04-01 12:03:09', '2023-04-01 12:03:09', 15800.00, '抖音直播推广活动1', NULL, NULL,
        NULL, NULL),
       (45, 1, '抖音推广11', '2022-04-01 12:03:09', '2023-04-01 12:03:09', 15800.00, '抖音直播推广活动2', NULL, NULL,
        NULL, NULL),
       (46, 3, '抖音短视频广告', '2023-11-14 00:00:00', '2023-11-30 00:00:00', 5000.00, '抖音短视频广告，宣传产品', NULL,
        NULL, NULL, NULL),
       (47, 3, '我去恶趣味', '2023-11-15 16:51:40', '2023-11-30 16:51:42', 231231.00, '色达所大所大所多', NULL, NULL,
        NULL, NULL),
       (48, NULL, NULL, '2023-06-30 00:44:19', NULL, NULL, NULL, '2023-06-27 22:27:15', NULL, NULL, NULL);
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
VALUES (1, 46, '1111111111111', '2023-05-17 14:07:48', 1, NULL, NULL, NULL),
       (2, 1, '2312323123123', '2023-05-17 14:25:30', 1, '2023-11-08 14:21:42', 1, NULL),
       (3, 2, '1231231', '2023-06-07 23:08:32', 1, NULL, NULL, NULL),
       (4, 2, '23123', '2023-06-07 23:08:57', 1, NULL, NULL, NULL),
       (5, 10, '恶趣味群二无群二群无', '2023-06-07 23:10:20', 1, NULL, NULL, NULL),
       (6, 1, '12而且我撒', '2023-08-04 22:30:47', 1, NULL, NULL, NULL);
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
VALUES (1, 1, 46, '王杰', 18, '13700000000', '13700000000', '13700000000', 'wangjie@163.com', 32, '工程师', 10.00,
        '北京亦庄', 50, 46, 5, 24, 3, '近期在看车', '2023-04-27 10:33:47', '2023-07-17 15:17:52', NULL,
        '2023-11-29 20:10:28', 2);
INSERT INTO `t_clue`
VALUES (2, 1, 47, '张峰', 41, '13700000001', '13700000001', NULL, NULL, 28, NULL, 8.00, '河北廊坊', 50, 47, 8, -1, 33,
        '通过打电话获取的线索', '2023-04-30 10:33:51', '2023-06-10 01:01:13', NULL, NULL, NULL);
INSERT INTO `t_clue`
VALUES (3, 2, 7, '张翔', 18, '13876903226', '13876903226', '123456', NULL, 26, NULL, 9.00, '天津和平', 50, 48, 10, 30,
        44, '有购车意向，需要跟踪', '2023-04-15 00:00:00', '2023-06-10 01:01:17', NULL, NULL, NULL);
INSERT INTO `t_clue`
VALUES (4, 1, 46, '王杰', 18, '13700000000', '13700000000', '13700000000', 'wangjie@163.com', 32, '工程师', 10.00,
        '北京亦庄', 50, 46, 5, 24, 3, '近期在看车', '2023-04-27 10:33:47', '2023-04-12 15:17:52', NULL, NULL, NULL);
INSERT INTO `t_clue`
VALUES (5, 1, 47, '张峰', 41, '13700000001', '13700000001', NULL, NULL, 28, NULL, 8.00, '河北廊坊', 50, 47, 8, -1, 33,
        '通过打电话获取的线索', '2023-04-30 10:33:51', '2023-06-10 01:01:19', NULL, NULL, NULL);
INSERT INTO `t_clue`
VALUES (6, 2, 7, '张翔', 18, '13876903226', '13876903226', NULL, NULL, 26, NULL, 9.00, '天津和平', 49, 48, 10, 30, 44,
        '有购车意向，需要跟踪', '2023-04-15 00:00:00', '2023-06-10 01:01:23', NULL, NULL, NULL);
INSERT INTO `t_clue`
VALUES (7, 1, 46, '王杰', 18, '13700000000', '13700000000', '13700000000', 'wangjie@163.com', 32, '工程师', 10.00,
        '北京亦庄', 49, 46, 5, 24, 3, '近期在看车', '2023-04-27 10:33:47', '2023-04-12 15:17:52', NULL, NULL, NULL);
INSERT INTO `t_clue`
VALUES (8, 1, 47, '张峰', 41, '13700000001', '13700000001', NULL, NULL, 28, NULL, 8.00, '河北廊坊', 49, 47, 8, 24, 33,
        '通过打电话获取的线索', '2023-04-30 10:33:51', '2023-06-10 01:01:25', NULL, NULL, NULL);
INSERT INTO `t_clue`
VALUES (9, 2, 7, '张翔', 18, '13876903226', '13876903226', NULL, NULL, 26, NULL, 9.00, '天津和平', 49, 48, 10, 30, 44,
        '有购车意向，需要跟踪', '2023-04-28 13:24:10', '2023-06-10 01:01:28', NULL, NULL, NULL);
INSERT INTO `t_clue`
VALUES (10, 1, 46, '王杰89890890', 18, '13700000000', '13700000000', '13700000000', 'wangjie@163.com', 32, '', 10.00,
        '北京亦庄', 49, 46, 5, -1, 3, '近期在看车', '2023-04-27 10:33:47', '2023-04-12 15:17:52', NULL, NULL, NULL);
INSERT INTO `t_clue`
VALUES (11, 1, 47, '张峰', 41, '13700000001', '13700000001', NULL, NULL, 28, NULL, 8.00, '河北廊坊', 49, 47, 8, 24, 33,
        '通过打电话获取的线索', '2023-04-30 10:33:51', '2023-06-10 01:01:30', NULL, NULL, NULL);
INSERT INTO `t_clue`
VALUES (12, 2, 7, '张翔', 18, '13876903226', '13876903226', NULL, NULL, 26, NULL, 9.00, '天津和平', 49, 48, 10, 30, 44,
        '有购车意向，需要跟踪', '2023-04-15 00:00:00', '2023-06-10 01:01:33', NULL, NULL, NULL);
INSERT INTO `t_clue`
VALUES (13, 1, 46, '王杰', 18, '13700000000', '13700000000', '13700000000', 'wangjie@163.com', 32, '工程师', 10.00,
        '北京亦庄', 49, 46, 5, -1, 3, '近期在看车', '2023-04-27 10:33:47', '2023-04-12 15:17:52', NULL, NULL, NULL);
INSERT INTO `t_clue`
VALUES (14, 1, 47, '张峰', 41, '13700000001', '13700000001', NULL, NULL, 28, NULL, 8.00, '河北廊坊', 49, 47, 8, -1, 33,
        '通过打电话获取的线索', '2023-04-30 10:33:51', '2023-06-10 01:01:36', NULL, NULL, NULL);
INSERT INTO `t_clue`
VALUES (15, 2, 7, '张翔', 18, '13876903226', '13876903226', NULL, NULL, 26, NULL, 9.00, '天津和平', 49, 48, 10, 30, 44,
        '有购车意向，需要跟踪', '2023-04-15 00:00:00', '2023-06-10 01:01:38', NULL, NULL, NULL);
INSERT INTO `t_clue`
VALUES (16, 1, 46, '王杰', 18, '13700000000', '13700000000', '13700000000', 'wangjie@163.com', 32, '工程师', 10.00,
        '北京亦庄', 49, 46, 5, 24, 3, '近期在看车', '2023-04-27 10:33:47', '2023-04-12 15:17:52', NULL, NULL, NULL);
INSERT INTO `t_clue`
VALUES (17, 1, 47, '张峰', 41, '13700000001', '13700000001', NULL, NULL, 28, NULL, 8.00, '河北廊坊', 49, 47, 8, -1, 33,
        '通过打电话获取的线索', '2023-04-30 10:33:51', '2023-06-10 01:01:41', NULL, NULL, NULL);
INSERT INTO `t_clue`
VALUES (18, 2, 7, '张翔', 18, '13876903226', '13876903226', NULL, NULL, 26, NULL, 9.00, '天津和平', 49, 48, 10, 30, 44,
        '有购车意向，需要跟踪', '2023-04-15 00:00:00', '2023-06-10 01:01:44', NULL, NULL, NULL);
INSERT INTO `t_clue`
VALUES (19, 1, 46, '王杰', 18, '13700000000', '13700000000', '13700000000', 'wangjie@163.com', 32, '工程师', 10.00,
        '北京亦庄', 49, 46, 5, -1, 3, '近期在看车', '2023-04-27 10:33:47', '2023-06-12 15:17:52', NULL, NULL, NULL);
INSERT INTO `t_clue`
VALUES (20, 1, 47, '张峰', 41, '13700000001', '13700000001', NULL, NULL, 28, NULL, 8.00, '河北廊坊', 49, 47, 8, 24, 33,
        '通过打电话获取的线索', '2023-04-30 10:33:51', '2023-06-10 01:01:46', NULL, NULL, NULL);
INSERT INTO `t_clue`
VALUES (21, 2, 7, '张翔', 18, '13876903226', '13876903226', NULL, NULL, 26, NULL, 9.00, '天津和平', 49, 48, 10, 30, 44,
        '有购车意向，需要跟踪', '2023-04-15 00:00:00', '2023-06-10 01:01:46', NULL, NULL, NULL);
INSERT INTO `t_clue`
VALUES (22, 1, 46, '王杰', 18, '13700000000', '13700000000', '13700000000', 'wangjie@163.com', 32, '工程师', 10.00,
        '北京亦庄', 49, 46, 5, 24, 3, '近期在看车', '2023-04-27 10:33:47', '2023-04-12 15:17:52', NULL, NULL, NULL);
INSERT INTO `t_clue`
VALUES (23, 1, 47, '张峰', 41, '13700000001', '13700000001', NULL, NULL, 28, NULL, 8.00, '河北廊坊', 49, 47, 8, -1, 33,
        '通过打电话获取的线索', '2023-04-30 10:33:51', '2023-06-10 01:01:46', NULL, NULL, NULL);
INSERT INTO `t_clue`
VALUES (24, 2, 7, '张翔', 18, '13876903226', '13876903226', NULL, NULL, 26, NULL, 9.00, '天津和平', 49, 48, 10, 30, 44,
        '有购车意向，需要跟踪', '2023-04-15 00:00:00', '2023-06-10 01:01:46', NULL, NULL, NULL);
INSERT INTO `t_clue`
VALUES (25, 1, 46, '王杰', 18, '13700000000', '13700000000', '13700000000', 'wangjie@163.com', 32, '工程师', 10.00,
        '北京亦庄', 49, 46, 5, 24, 3, '近期在看车', '2023-04-27 10:33:47', '2023-04-12 15:17:52', NULL, NULL, NULL);
INSERT INTO `t_clue`
VALUES (26, 1, 47, '张峰', 41, '13700000001', '13700000001', NULL, NULL, 28, NULL, 8.00, '河北廊坊', 49, 47, 8, 24, 33,
        '通过打电话获取的线索', '2023-04-30 10:33:51', '2023-06-10 01:01:46', NULL, NULL, NULL);
INSERT INTO `t_clue`
VALUES (27, 2, 7, '张翔', 18, '13876903226', '13876903226', NULL, NULL, 26, NULL, 9.00, '天津和平', 49, 48, 10, 30, 44,
        '有购车意向，需要跟踪', '2023-04-15 00:00:00', '2023-06-10 01:01:46', NULL, NULL, NULL);
INSERT INTO `t_clue`
VALUES (28, 1, 46, '王杰', 18, '13700000000', '13700000000', '13700000000', 'wangjie@163.com', 32, '工程师', 10.00,
        '北京亦庄', 49, 46, 5, 24, 3, '近期在看车', '2023-04-27 10:33:47', '2023-06-12 15:17:52', NULL, NULL, NULL);
INSERT INTO `t_clue`
VALUES (29, 1, 47, '张峰', 41, '13700000001', '13700000001', NULL, NULL, 28, NULL, 8.00, '河北廊坊', 49, 47, 8, 24, 33,
        '通过打电话获取的线索', '2023-04-30 10:33:51', '2023-06-10 01:01:46', NULL, NULL, NULL);
INSERT INTO `t_clue`
VALUES (31, 3, 2, '213213', 20, '12312312', '12312312', '2312312', '12312312', 12, '341231', 2131231.00, '12312312', 50,
        46, 7, 24, 44, 'asfeefsdewrewr', '2023-04-27 16:48:30', '2023-06-10 01:01:46', NULL, NULL, NULL);
INSERT INTO `t_clue`
VALUES (1109, 1, 46, '王杰', 18, '13700000000', '13700000000', '230989432', 'wangjie@163.com', 32, '工程师', 10.00,
        '北京亦庄', NULL, NULL, NULL, NULL, NULL, '近期在看车', '2023-11-27 20:33:25', NULL, NULL, NULL, NULL);
INSERT INTO `t_clue`
VALUES (1110, 1, 47, '张怡然', 41, '13700000001', '13700000001', NULL, NULL, 28, NULL, 8.00, '河北廊坊', NULL, NULL,
        NULL, NULL, NULL, '通过打电话获取的线索', '2023-11-30 10:33:51', NULL, NULL, NULL, NULL);
INSERT INTO `t_clue`
VALUES (1111, 2, 7, '张翔宇', 18, '13876903226', '13876903226', '1298094321', NULL, 26, NULL, 9.00, '天津和平', NULL,
        NULL, NULL, NULL, NULL, '有购车意向，需要跟踪', '2023-11-15 10:30:00', NULL, NULL, NULL, NULL);
INSERT INTO `t_clue`
VALUES (1112, 1, 46, '王世坤', 18, '13700000000', '13700000000', '209836613', 'wangjie@163.com', 32, '工程师', 10.00,
        '北京亦庄', NULL, NULL, NULL, NULL, NULL, '近期在看车', '2023-12-27 09:20:21', NULL, NULL, NULL, NULL);
INSERT INTO `t_clue`
VALUES (1113, 1, 47, '张珊珊', 41, '13700000001', '13700000001', NULL, NULL, 28, NULL, 8.00, '河北廊坊', NULL, NULL,
        NULL, NULL, NULL, '通过打电话获取的线索', '2023-11-30 13:33:51', NULL, NULL, NULL, NULL);
INSERT INTO `t_clue`
VALUES (1114, 1, 46, '王杰', 18, '13700000000', '13700000000', '230989432', 'wangjie@163.com', 32, '工程师', 10.00,
        '北京亦庄', NULL, NULL, NULL, NULL, NULL, '近期在看车', '2023-11-27 20:33:25', NULL, NULL, NULL, NULL);
INSERT INTO `t_clue`
VALUES (1115, 1, 47, '张怡然', 41, '13700000001', '13700000001', NULL, NULL, 28, NULL, 8.00, '河北廊坊', NULL, NULL,
        NULL, NULL, NULL, '通过打电话获取的线索', '2023-11-30 10:33:51', NULL, NULL, NULL, NULL);
INSERT INTO `t_clue`
VALUES (1116, 2, 7, '张翔宇', 18, '13876903226', '13876903226', '1298094321', NULL, 26, NULL, 9.00, '天津和平', NULL,
        NULL, NULL, NULL, NULL, '有购车意向，需要跟踪', '2023-11-15 10:30:00', NULL, NULL, NULL, NULL);
INSERT INTO `t_clue`
VALUES (1117, 1, 46, '王世坤', 18, '13700000000', '13700000000', '209836613', 'wangjie@163.com', 32, '工程师', 10.00,
        '北京亦庄', NULL, NULL, NULL, NULL, NULL, '近期在看车', '2023-12-27 09:20:21', NULL, NULL, NULL, NULL);
INSERT INTO `t_clue`
VALUES (1118, 1, 47, '张珊珊', 41, '13700000001', '13700000001', NULL, NULL, 28, NULL, 8.00, '河北廊坊', NULL, NULL,
        NULL, NULL, NULL, '通过打电话获取的线索', '2023-11-30 13:33:51', NULL, NULL, NULL, NULL);
INSERT INTO `t_clue`
VALUES (1119, 1, 46, '王杰', 18, '13700000000', '13700000000', '230989432', 'wangjie@163.com', 32, '工程师', 10.00,
        '北京亦庄', NULL, NULL, NULL, NULL, NULL, '近期在看车', '2023-11-27 20:33:25', NULL, NULL, NULL, NULL);
INSERT INTO `t_clue`
VALUES (1120, 1, 47, '张怡然', 41, '13700000001', '13700000001', NULL, NULL, 28, NULL, 8.00, '河北廊坊', NULL, NULL,
        NULL, NULL, NULL, '通过打电话获取的线索', '2023-11-30 10:33:51', NULL, NULL, NULL, NULL);
INSERT INTO `t_clue`
VALUES (1121, 2, 7, '张翔宇', 18, '13876903226', '13876903226', '1298094321', NULL, 26, NULL, 9.00, '天津和平', NULL,
        NULL, NULL, NULL, NULL, '有购车意向，需要跟踪', '2023-11-15 10:30:00', NULL, NULL, NULL, NULL);
INSERT INTO `t_clue`
VALUES (1122, 1, 46, '王世坤', 18, '13700000000', '13700000000', '209836613', 'wangjie@163.com', 32, '工程师', 10.00,
        '北京亦庄', NULL, NULL, NULL, NULL, NULL, '近期在看车', '2023-12-27 09:20:21', NULL, NULL, NULL, NULL);
INSERT INTO `t_clue`
VALUES (1123, 1, 47, '张珊珊', 41, '13700000001', '13700000001', NULL, NULL, 28, NULL, 8.00, '河北廊坊', NULL, NULL,
        NULL, NULL, NULL, '通过打电话获取的线索', '2023-11-30 13:33:51', NULL, NULL, NULL, NULL);
INSERT INTO `t_clue`
VALUES (1124, 1, 46, '王杰', 18, '13700000000', '13700000000', '230989432', 'wangjie@163.com', 32, '工程师', 10.00,
        '北京亦庄', NULL, NULL, NULL, NULL, NULL, '近期在看车', '2023-11-27 20:33:25', NULL, NULL, NULL, NULL);
INSERT INTO `t_clue`
VALUES (1125, 1, 47, '张怡然', 41, '13700000001', '13700000001', NULL, NULL, 28, NULL, 8.00, '河北廊坊', NULL, NULL,
        NULL, NULL, NULL, '通过打电话获取的线索', '2023-11-30 10:33:51', NULL, NULL, NULL, NULL);
INSERT INTO `t_clue`
VALUES (1126, 2, 7, '张翔宇', 18, '13876903226', '13876903226', '1298094321', NULL, 26, NULL, 9.00, '天津和平', NULL,
        NULL, NULL, NULL, NULL, '有购车意向，需要跟踪', '2023-11-15 10:30:00', NULL, NULL, NULL, NULL);
INSERT INTO `t_clue`
VALUES (1127, 1, 46, '王世坤', 18, '13700000000', '13700000000', '209836613', 'wangjie@163.com', 32, '工程师', 10.00,
        '北京亦庄', NULL, NULL, NULL, NULL, NULL, '近期在看车', '2023-12-27 09:20:21', NULL, NULL, NULL, NULL);
INSERT INTO `t_clue`
VALUES (1128, 1, 47, '张珊珊', 41, '13700000001', '13700000001', NULL, NULL, 28, NULL, 8.00, '河北廊坊', NULL, NULL,
        NULL, NULL, NULL, '通过打电话获取的线索', '2023-11-30 13:33:51', NULL, NULL, NULL, NULL);
INSERT INTO `t_clue`
VALUES (1129, 1, 46, '王杰', 18, '13700000000', '13700000000', '230989432', 'wangjie@163.com', 32, '工程师', 10.00,
        '北京亦庄', NULL, NULL, NULL, NULL, NULL, '近期在看车', '2023-11-27 20:33:25', NULL, NULL, NULL, NULL);
INSERT INTO `t_clue`
VALUES (1130, 1, 47, '张怡然', 41, '13700000001', '13700000001', NULL, NULL, 28, NULL, 8.00, '河北廊坊', NULL, NULL,
        NULL, NULL, NULL, '通过打电话获取的线索', '2023-11-30 10:33:51', NULL, NULL, NULL, NULL);
INSERT INTO `t_clue`
VALUES (1131, 2, 7, '张翔宇', 18, '13876903226', '13876903226', '1298094321', NULL, 26, NULL, 9.00, '天津和平', NULL,
        NULL, NULL, NULL, NULL, '有购车意向，需要跟踪', '2023-11-15 10:30:00', NULL, NULL, NULL, NULL);
INSERT INTO `t_clue`
VALUES (1132, 1, 46, '王世坤', 18, '13700000000', '13700000000', '209836613', 'wangjie@163.com', 32, '工程师', 10.00,
        '北京亦庄', NULL, NULL, NULL, NULL, NULL, '近期在看车', '2023-12-27 09:20:21', NULL, NULL, NULL, NULL);
INSERT INTO `t_clue`
VALUES (1133, 1, 47, '张珊珊', 41, '13700000001', '13700000001', NULL, NULL, 28, NULL, 8.00, '河北廊坊', NULL, NULL,
        NULL, NULL, NULL, '通过打电话获取的线索', '2023-11-30 13:33:51', NULL, NULL, NULL, NULL);
INSERT INTO `t_clue`
VALUES (1134, 1, 46, '王杰', 18, '13700000000', '13700000000', '230989432', 'wangjie@163.com', 32, '工程师', 10.00,
        '北京亦庄', NULL, NULL, NULL, NULL, NULL, '近期在看车', '2023-11-27 20:33:25', NULL, NULL, NULL, NULL);
INSERT INTO `t_clue`
VALUES (1135, 1, 47, '张怡然', 41, '13700000001', '13700000001', NULL, NULL, 28, NULL, 8.00, '河北廊坊', NULL, NULL,
        NULL, NULL, NULL, '通过打电话获取的线索', '2023-11-30 10:33:51', NULL, NULL, NULL, NULL);
INSERT INTO `t_clue`
VALUES (1136, 2, 7, '张翔宇', 18, '13876903226', '13876903226', '1298094321', NULL, 26, NULL, 9.00, '天津和平', NULL,
        NULL, NULL, NULL, NULL, '有购车意向，需要跟踪', '2023-11-15 10:30:00', NULL, NULL, NULL, NULL);
INSERT INTO `t_clue`
VALUES (1137, 1, 46, '王世坤', 18, '13700000000', '13700000000', '209836613', 'wangjie@163.com', 32, '工程师', 10.00,
        '北京亦庄', NULL, NULL, NULL, NULL, NULL, '近期在看车', '2023-12-27 09:20:21', NULL, NULL, NULL, NULL);
INSERT INTO `t_clue`
VALUES (1138, 1, 47, '张珊珊', 41, '13700000001', '13700000001', NULL, NULL, 28, NULL, 8.00, '河北廊坊', NULL, NULL,
        NULL, NULL, NULL, '通过打电话获取的线索', '2023-11-30 13:33:51', NULL, NULL, NULL, NULL);
INSERT INTO `t_clue`
VALUES (1139, 1, 46, '王杰', 18, '13700000000', '13700000000', '230989432', 'wangjie@163.com', 32, '工程师', 10.00,
        '北京亦庄', NULL, NULL, NULL, NULL, NULL, '近期在看车', '2023-11-27 20:33:25', NULL, NULL, NULL, NULL);
INSERT INTO `t_clue`
VALUES (1140, 1, 47, '张怡然', 41, '13700000001', '13700000001', NULL, NULL, 28, NULL, 8.00, '河北廊坊', NULL, NULL,
        NULL, NULL, NULL, '通过打电话获取的线索', '2023-11-30 10:33:51', NULL, NULL, NULL, NULL);
INSERT INTO `t_clue`
VALUES (1141, 2, 7, '张翔宇', 18, '13876903226', '13876903226', '1298094321', NULL, 26, NULL, 9.00, '天津和平', NULL,
        NULL, NULL, NULL, NULL, '有购车意向，需要跟踪', '2023-11-15 10:30:00', NULL, NULL, NULL, NULL);
INSERT INTO `t_clue`
VALUES (1142, 1, 46, '王世坤', 18, '13700000000', '13700000000', '209836613', 'wangjie@163.com', 32, '工程师', 10.00,
        '北京亦庄', NULL, NULL, NULL, NULL, NULL, '近期在看车', '2023-12-27 09:20:21', NULL, NULL, NULL, NULL);
INSERT INTO `t_clue`
VALUES (1143, 1, 47, '张珊珊', 41, '13700000001', '13700000001', NULL, NULL, 28, NULL, 8.00, '河北廊坊', NULL, NULL,
        NULL, NULL, NULL, '通过打电话获取的线索', '2023-11-30 13:33:51', NULL, NULL, NULL, NULL);

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
VALUES (5, 8, 65, '2143242354', '2023-04-28 14:24:27', 1, NULL, NULL, NULL);
INSERT INTO `t_clue_remark`
VALUES (6, 10, 64, '123412312312', '2023-04-28 14:29:41', 1, NULL, NULL, NULL);
INSERT INTO `t_clue_remark`
VALUES (7, 10, 63, '二位绕弯儿翁人', '2023-04-28 14:30:16', 1, NULL, NULL, NULL);
INSERT INTO `t_clue_remark`
VALUES (8, 21, 61, '12213123123', '2023-05-21 23:43:48', 1, NULL, NULL, NULL);
INSERT INTO `t_clue_remark`
VALUES (9, 21, 62, '3ewrwerewr', '2023-05-21 23:43:52', 1, NULL, NULL, NULL);
INSERT INTO `t_clue_remark`
VALUES (10, 21, 65, 'ewrwerewrewr', '2023-05-21 23:43:55', 1, NULL, NULL, NULL);
INSERT INTO `t_clue_remark`
VALUES (11, 16, 64, '123213123', '2023-05-21 23:54:57', 1, NULL, NULL, NULL);
INSERT INTO `t_clue_remark`
VALUES (12, 16, 63, '23123123', '2023-05-21 23:54:59', 1, NULL, NULL, NULL);
INSERT INTO `t_clue_remark`
VALUES (13, 16, 61, '2312313', '2023-05-21 23:55:02', 1, NULL, NULL, NULL);
INSERT INTO `t_clue_remark`
VALUES (14, 1, 62, NULL, '2023-06-27 22:47:49', 1, NULL, NULL, NULL);
INSERT INTO `t_clue_remark`
VALUES (15, 1, 65, '111111111111111', '2023-06-27 22:47:56', 1, NULL, NULL, NULL);
INSERT INTO `t_clue_remark`
VALUES (16, 1, 64, '1111111111111111111111222222222222222222222222', '2023-06-27 22:48:01', 1, NULL, NULL, NULL);

-- ----------------------------
-- Table structure for t_customer
-- ----------------------------
DROP TABLE IF EXISTS `t_customer`;
CREATE TABLE `t_customer`
(
    `id`                int                                                           NOT NULL AUTO_INCREMENT COMMENT '主键，自动增长，客户ID',
    `clue_id`           int                                                           NULL DEFAULT NULL COMMENT '线索ID',
    `product`           int                                                           NULL DEFAULT NULL COMMENT '选购产品',
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
    INDEX `t_customer_ibfk_4` (`edit_by` ASC) USING BTREE
) ENGINE = InnoDB
  AUTO_INCREMENT = 12
  CHARACTER SET = utf8mb3
  COLLATE = utf8mb3_general_ci COMMENT = '客户表'
  ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of t_customer
-- ----------------------------
INSERT INTO `t_customer`
VALUES (1, 10, 3, '2131231', '2023-05-06 15:36:59', NULL, NULL, NULL, NULL);
INSERT INTO `t_customer`
VALUES (2, 8, 6, '124气味儿群', '2023-05-05 00:00:00', NULL, NULL, NULL, NULL);
INSERT INTO `t_customer`
VALUES (3, 6, 6, '1232强21312', '2023-05-06 00:00:00', '2023-04-28 15:42:15', 1, NULL, NULL);
INSERT INTO `t_customer`
VALUES (4, 2, 3, '阿萨的人', '2023-05-05 00:00:00', '2023-04-28 15:54:04', 1, NULL, NULL);
INSERT INTO `t_customer`
VALUES (5, 10, 1, '234234', '2023-05-05 00:00:00', '2023-04-28 15:56:44', 1, NULL, NULL);
INSERT INTO `t_customer`
VALUES (6, 17, 2, '是的啊所大', '2023-05-05 00:00:00', '2023-04-28 15:59:33', 1, NULL, NULL);
INSERT INTO `t_customer`
VALUES (7, 5, 2, '12312312', '2023-05-12 00:00:00', '2023-05-04 10:03:05', 1, NULL, NULL);
INSERT INTO `t_customer`
VALUES (8, 19, 2, '气味儿群翁', '2023-05-04 10:03:18', '2023-05-04 10:03:20', 1, NULL, NULL);
INSERT INTO `t_customer`
VALUES (9, 14, 4, '沃尔沃二', '2023-06-09 00:00:00', '2023-05-04 10:03:39', 1, NULL, NULL);
INSERT INTO `t_customer`
VALUES (10, 23, 4, '沃尔沃二翁', '2023-05-23 00:00:00', '2023-05-04 10:03:53', 1, NULL, NULL);
INSERT INTO `t_customer`
VALUES (11, 13, 2, '沃尔沃二', '2023-05-13 00:00:00', '2023-05-04 10:04:03', 1, NULL, NULL);

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
VALUES (1, 10, 65, '1111111111111111', 1, '2023-05-04 15:25:51', NULL, NULL, NULL);
INSERT INTO `t_customer_remark`
VALUES (2, 10, 64, '2222222222222', 1, '2023-05-04 15:28:13', NULL, NULL, NULL);
INSERT INTO `t_customer_remark`
VALUES (3, 10, 63, 'EREWREWRWRWR', 1, '2023-05-04 16:21:03', NULL, NULL, NULL);
INSERT INTO `t_customer_remark`
VALUES (4, 7, 61, '13213123', 1, '2023-05-17 17:36:16', NULL, NULL, NULL);
INSERT INTO `t_customer_remark`
VALUES (5, 10, 62, '2342423423423', 1, '2023-05-17 17:36:33', NULL, NULL, NULL);
INSERT INTO `t_customer_remark`
VALUES (6, 1, 65, '3212321321', 1, '2023-05-21 23:50:42', NULL, NULL, NULL);
INSERT INTO `t_customer_remark`
VALUES (7, 1, 64, 'eqwewqeqwe', 1, '2023-05-21 23:50:46', NULL, NULL, NULL);
INSERT INTO `t_customer_remark`
VALUES (8, 1, 63, 'wqeqwewqeqw', 1, '2023-05-21 23:50:48', NULL, NULL, NULL);
INSERT INTO `t_customer_remark`
VALUES (9, 8, 61, 'ewqeqweq', 1, '2023-05-21 23:50:59', NULL, NULL, NULL);
INSERT INTO `t_customer_remark`
VALUES (10, 5, 62, 'weqweqweqweq', 1, '2023-05-21 23:51:05', NULL, NULL, NULL);
INSERT INTO `t_customer_remark`
VALUES (11, 5, 65, 'weqwewqeqw', 1, '2023-05-21 23:51:08', NULL, NULL, NULL);
INSERT INTO `t_customer_remark`
VALUES (12, 5, 64, '23213213123', 1, '2023-05-21 23:51:13', NULL, NULL, NULL);
INSERT INTO `t_customer_remark`
VALUES (13, 5, 65, '23123213213', 1, '2023-05-21 23:51:16', NULL, NULL, NULL);
INSERT INTO `t_customer_remark`
VALUES (14, 5, 64, '23123213213', 1, '2023-05-21 23:51:19', NULL, NULL, NULL);
INSERT INTO `t_customer_remark`
VALUES (15, 5, 63, '3232323123', 1, '2023-05-21 23:51:23', NULL, NULL, NULL);
INSERT INTO `t_customer_remark`
VALUES (16, 1, 61, '12312321321', 1, '2023-05-21 23:56:55', NULL, NULL, NULL);
INSERT INTO `t_customer_remark`
VALUES (17, 2, 62, '1232131231', 1, '2023-05-21 23:57:03', NULL, NULL, NULL);
INSERT INTO `t_customer_remark`
VALUES (18, 10, 65, NULL, 1, '2023-05-22 22:12:52', NULL, NULL, NULL);
INSERT INTO `t_customer_remark`
VALUES (19, 10, 64, '123213214124', 1, '2023-05-22 22:12:58', NULL, NULL, NULL);
INSERT INTO `t_customer_remark`
VALUES (20, 10, 63, '13241242432432', 1, '2023-05-22 22:13:03', NULL, NULL, NULL);
INSERT INTO `t_customer_remark`
VALUES (21, 10, 61, '3423423423', 1, '2023-05-22 22:13:06', NULL, NULL, NULL);
INSERT INTO `t_customer_remark`
VALUES (22, 2, 62, '1242412141', 1, '2023-05-31 20:14:08', NULL, NULL, NULL);

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
    `order`      int                                                          NULL DEFAULT NULL COMMENT '字典值排序',
    `remark`     varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `t_dic_value_ibfk_1` (`type_code` ASC) USING BTREE,
    CONSTRAINT `t_dic_value_ibfk_1` FOREIGN KEY (`type_code`) REFERENCES `t_dic_type` (`type_code`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE = InnoDB
  AUTO_INCREMENT = 66
  CHARACTER SET = utf8mb3
  COLLATE = utf8mb3_general_ci COMMENT = '字典值表'
  ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of t_dic_value
-- ----------------------------
INSERT INTO `t_dic_value`
VALUES (-1, 'clueState', '已转客户', 0, NULL);
INSERT INTO `t_dic_value`
VALUES (1, 'clueState', '虚假线索', 4, NULL);
INSERT INTO `t_dic_value`
VALUES (2, 'source', '知乎', 8, NULL);
INSERT INTO `t_dic_value`
VALUES (3, 'source', '车展会', 11, NULL);
INSERT INTO `t_dic_value`
VALUES (4, 'returnPriority', '最高', 2, NULL);
INSERT INTO `t_dic_value`
VALUES (5, 'appellation', '教授', 5, NULL);
INSERT INTO `t_dic_value`
VALUES (6, 'clueState', '将来联系', 2, NULL);
INSERT INTO `t_dic_value`
VALUES (7, 'clueState', '丢失线索', 5, NULL);
INSERT INTO `t_dic_value`
VALUES (8, 'returnState', '未启动', 1, NULL);
INSERT INTO `t_dic_value`
VALUES (10, 'clueState', '试图联系', 1, NULL);
INSERT INTO `t_dic_value`
VALUES (11, 'appellation', '博士', 4, NULL);
INSERT INTO `t_dic_value`
VALUES (12, 'stage', '01创建交易', 1, NULL);
INSERT INTO `t_dic_value`
VALUES (14, 'source', '汽车之家', 14, NULL);
INSERT INTO `t_dic_value`
VALUES (15, 'returnPriority', '低', 3, NULL);
INSERT INTO `t_dic_value`
VALUES (16, 'source', '网络广告', 1, NULL);
INSERT INTO `t_dic_value`
VALUES (17, 'source', '视频直播', 9, NULL);
INSERT INTO `t_dic_value`
VALUES (18, 'appellation', '先生', 1, NULL);
INSERT INTO `t_dic_value`
VALUES (19, 'returnPriority', '高', 1, NULL);
INSERT INTO `t_dic_value`
VALUES (20, 'appellation', '夫人', 2, NULL);
INSERT INTO `t_dic_value`
VALUES (21, 'stage', '06丢失关闭', 7, NULL);
INSERT INTO `t_dic_value`
VALUES (22, 'source', '地图', 13, NULL);
INSERT INTO `t_dic_value`
VALUES (23, 'source', '合作伙伴', 6, NULL);
INSERT INTO `t_dic_value`
VALUES (24, 'clueState', '未联系', 6, NULL);
INSERT INTO `t_dic_value`
VALUES (25, 'source', '朋友圈', 10, NULL);
INSERT INTO `t_dic_value`
VALUES (26, 'returnState', '进行中', 3, NULL);
INSERT INTO `t_dic_value`
VALUES (27, 'clueState', '已联系', 3, NULL);
INSERT INTO `t_dic_value`
VALUES (28, 'returnState', '推迟', 2, NULL);
INSERT INTO `t_dic_value`
VALUES (29, 'returnState', '完成', 4, NULL);
INSERT INTO `t_dic_value`
VALUES (30, 'clueState', '需要条件', 7, NULL);
INSERT INTO `t_dic_value`
VALUES (32, 'returnState', '等待某人', 5, NULL);
INSERT INTO `t_dic_value`
VALUES (33, 'source', '懂车帝', 2, NULL);
INSERT INTO `t_dic_value`
VALUES (34, 'returnPriority', '常规', 5, NULL);
INSERT INTO `t_dic_value`
VALUES (35, 'stage', '04产品检验', 5, NULL);
INSERT INTO `t_dic_value`
VALUES (36, 'source', '易车网', 12, NULL);
INSERT INTO `t_dic_value`
VALUES (37, 'stage', '02确认清单', 3, NULL);
INSERT INTO `t_dic_value`
VALUES (38, 'returnPriority', '最低', 4, NULL);
INSERT INTO `t_dic_value`
VALUES (39, 'source', '员工介绍', 3, NULL);
INSERT INTO `t_dic_value`
VALUES (40, 'stage', '03交付定金', 4, NULL);
INSERT INTO `t_dic_value`
VALUES (41, 'appellation', '女士', 3, NULL);
INSERT INTO `t_dic_value`
VALUES (42, 'stage', '05付款成交', 6, NULL);
INSERT INTO `t_dic_value`
VALUES (43, 'source', '官方网站', 5, NULL);
INSERT INTO `t_dic_value`
VALUES (44, 'source', '公众号', 7, NULL);
INSERT INTO `t_dic_value`
VALUES (45, 'source', '门店参观', 4, NULL);
INSERT INTO `t_dic_value`
VALUES (46, 'intentionState', '有意向', 1, NULL);
INSERT INTO `t_dic_value`
VALUES (47, 'intentionState', '无意向', 2, NULL);
INSERT INTO `t_dic_value`
VALUES (48, 'intentionState', '意向不明', 3, NULL);
INSERT INTO `t_dic_value`
VALUES (49, 'needLoan', '需要', 1, NULL);
INSERT INTO `t_dic_value`
VALUES (50, 'needLoan', '不需要', 2, NULL);
INSERT INTO `t_dic_value`
VALUES (51, 'sex', '男', 1, NULL);
INSERT INTO `t_dic_value`
VALUES (52, 'sex', '女', 2, NULL);
INSERT INTO `t_dic_value`
VALUES (53, 'educational', '小学', 1, NULL);
INSERT INTO `t_dic_value`
VALUES (54, 'educational', '初中', 2, NULL);
INSERT INTO `t_dic_value`
VALUES (55, 'educational', '高中', 3, NULL);
INSERT INTO `t_dic_value`
VALUES (56, 'educational', '大学', 4, NULL);
INSERT INTO `t_dic_value`
VALUES (57, 'educational', '研究生', 5, NULL);
INSERT INTO `t_dic_value`
VALUES (58, 'userState', '正常', 1, NULL);
INSERT INTO `t_dic_value`
VALUES (59, 'userState', '锁定', 2, NULL);
INSERT INTO `t_dic_value`
VALUES (60, 'userState', '禁用', 3, NULL);
INSERT INTO `t_dic_value`
VALUES (61, 'noteWay', '电话', 1, NULL);
INSERT INTO `t_dic_value`
VALUES (62, 'noteWay', '微信', 2, NULL);
INSERT INTO `t_dic_value`
VALUES (63, 'noteWay', 'QQ', 3, NULL);
INSERT INTO `t_dic_value`
VALUES (64, 'noteWay', '面聊', 4, NULL);
INSERT INTO `t_dic_value`
VALUES (65, 'noteWay', '其他', 5, NULL);

-- ----------------------------
-- Table structure for t_permission
-- ----------------------------
DROP TABLE IF EXISTS `t_permission`;
CREATE TABLE `t_permission`
(
    `id`        int                                                           NOT NULL AUTO_INCREMENT,
    `name`      varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  NULL DEFAULT NULL,
    `code`      varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  NULL DEFAULT NULL,
    `url`       varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  NULL DEFAULT NULL,
    `type`      varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  NULL DEFAULT NULL,
    `parent_id` int                                                           NULL DEFAULT NULL,
    `order_no`  int                                                           NULL DEFAULT NULL,
    `icon`      varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB
  AUTO_INCREMENT = 1112
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT = '权限表'
  ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of t_permission
-- ----------------------------
INSERT INTO `t_permission`
VALUES (1, '市场活动', NULL, NULL, 'menu', 0, 1, 'OfficeBuilding');
INSERT INTO `t_permission`
VALUES (2, '市场活动', NULL, '/dashboard/activity', 'menu', 1, 1, 'CreditCard');
INSERT INTO `t_permission`
VALUES (3, '市场活动-列表', 'activity:list', NULL, 'button', 2, NULL, NULL);
INSERT INTO `t_permission`
VALUES (4, '市场活动-录入', 'activity:add', NULL, 'button', 2, NULL, NULL);
INSERT INTO `t_permission`
VALUES (5, '市场活动-编辑', 'activity:edit', NULL, 'button', 2, NULL, NULL);
INSERT INTO `t_permission`
VALUES (6, '市场活动-查看', 'activity:view', NULL, 'button', 2, NULL, NULL);
INSERT INTO `t_permission`
VALUES (7, '市场活动-删除', 'activity:delete', NULL, 'button', 2, NULL, NULL);
INSERT INTO `t_permission`
VALUES (9, '市场活动-搜索', 'activity:search', NULL, 'button', 2, NULL, NULL);
INSERT INTO `t_permission`
VALUES (10, '线索管理', NULL, NULL, 'menu', 0, 2, 'Magnet');
INSERT INTO `t_permission`
VALUES (12, '线索管理', NULL, '/dashboard/clue', 'menu', 10, 1, 'Paperclip');
INSERT INTO `t_permission`
VALUES (13, '线索管理-列表', 'clue:list', NULL, 'button', 12, NULL, NULL);
INSERT INTO `t_permission`
VALUES (14, '线索管理-录入', 'clue:add', NULL, 'button', 12, NULL, NULL);
INSERT INTO `t_permission`
VALUES (15, '线索管理-编辑', 'clue:edit', NULL, 'button', 12, NULL, NULL);
INSERT INTO `t_permission`
VALUES (16, '线索管理-查看', 'clue:view', NULL, 'button', 12, NULL, NULL);
INSERT INTO `t_permission`
VALUES (17, '线索管理-删除', 'clue:delete', NULL, 'button', 12, NULL, NULL);
INSERT INTO `t_permission`
VALUES (18, '线索管理-导入', 'clue:import', NULL, 'button', 12, NULL, NULL);
INSERT INTO `t_permission`
VALUES (19, '客户管理', NULL, NULL, 'menu', 0, 3, 'User');
INSERT INTO `t_permission`
VALUES (20, '客户管理', NULL, '/dashboard/customer', 'menu', 19, 1, 'UserFilled');
INSERT INTO `t_permission`
VALUES (21, '客户管理-列表', 'customer:list', NULL, 'button', 20, NULL, NULL);
INSERT INTO `t_permission`
VALUES (22, '客户管理-查看', 'customer:view', NULL, 'button', 20, NULL, NULL);
INSERT INTO `t_permission`
VALUES (23, '客户管理-导出', 'customer:export', NULL, 'button', 20, NULL, NULL);
INSERT INTO `t_permission`
VALUES (24, '交易管理', NULL, NULL, 'menu', 0, 4, 'Wallet');
INSERT INTO `t_permission`
VALUES (25, '交易管理', NULL, '/dashboard/tran', 'menu', 24, 1, 'Coin');
INSERT INTO `t_permission`
VALUES (26, '交易管理-列表', 'tran:list', NULL, 'button', 25, NULL, NULL);
INSERT INTO `t_permission`
VALUES (27, '交易管理-查看', 'tran:view', NULL, 'button', 25, NULL, NULL);
INSERT INTO `t_permission`
VALUES (28, '产品管理', NULL, NULL, 'menu', 0, 5, 'Memo');
INSERT INTO `t_permission`
VALUES (29, '产品管理', NULL, '/dashboard/product', 'menu', 28, 1, 'SetUp');
INSERT INTO `t_permission`
VALUES (30, '产品管理-列表', 'product:list', NULL, 'button', 29, NULL, NULL);
INSERT INTO `t_permission`
VALUES (31, '产品管理-录入', 'product:add', NULL, 'button', 29, NULL, NULL);
INSERT INTO `t_permission`
VALUES (32, '产品管理-编辑', 'product:edit', NULL, 'button', 29, NULL, NULL);
INSERT INTO `t_permission`
VALUES (33, '产品管理-查看', 'product:view', NULL, 'button', 29, NULL, NULL);
INSERT INTO `t_permission`
VALUES (34, '产品管理-删除', 'product:delete', NULL, 'button', 29, NULL, NULL);
INSERT INTO `t_permission`
VALUES (35, '字典管理', NULL, NULL, 'menu', 0, 6, 'Grid');
INSERT INTO `t_permission`
VALUES (36, '字典类型', NULL, '/dashboard/dict/type', 'menu', 35, 1, 'Postcard');
INSERT INTO `t_permission`
VALUES (37, '字典类型-列表', 'dictype:list', NULL, 'button', 36, NULL, NULL);
INSERT INTO `t_permission`
VALUES (38, '字典类型-录入', 'dictype:add', NULL, 'button', 36, NULL, NULL);
INSERT INTO `t_permission`
VALUES (39, '字典类型-编辑', 'dictype:edit', NULL, 'button', 36, NULL, NULL);
INSERT INTO `t_permission`
VALUES (40, '字典类型-查看', 'dictype:view', NULL, 'button', 36, NULL, NULL);
INSERT INTO `t_permission`
VALUES (41, '字典类型-删除', 'dictype:delete', NULL, 'button', 36, NULL, NULL);
INSERT INTO `t_permission`
VALUES (42, '字典数据', '', '/dashboard/dict/value', 'menu', 35, 2, 'DataAnalysis');
INSERT INTO `t_permission`
VALUES (43, '字典数据-列表', 'dicvalue:list', NULL, 'button', 42, NULL, NULL);
INSERT INTO `t_permission`
VALUES (44, '字典数据-录入', 'dicvalue:add', NULL, 'button', 42, NULL, NULL);
INSERT INTO `t_permission`
VALUES (45, '字典数据-编辑', 'dicvalue:edit', NULL, 'button', 42, NULL, NULL);
INSERT INTO `t_permission`
VALUES (46, '字典数据-查看', 'dicvalue:view', NULL, 'button', 42, NULL, NULL);
INSERT INTO `t_permission`
VALUES (47, '字典数据-删除', 'dicvalue:delete', NULL, 'button', 42, NULL, NULL);
INSERT INTO `t_permission`
VALUES (48, '用户管理', NULL, NULL, 'menu', 0, 7, 'Stamp');
INSERT INTO `t_permission`
VALUES (49, '用户管理', NULL, '/dashboard/user', 'menu', 48, 1, 'User');
INSERT INTO `t_permission`
VALUES (50, '用户管理-列表', 'user:list', NULL, 'button', 49, NULL, NULL);
INSERT INTO `t_permission`
VALUES (51, '用户管理-录入', 'user:add', NULL, 'button', 49, NULL, NULL);
INSERT INTO `t_permission`
VALUES (52, '用户管理-编辑', 'user:edit', NULL, 'button', 49, NULL, NULL);
INSERT INTO `t_permission`
VALUES (53, '用户管理-查看', 'user:view', NULL, 'button', 49, NULL, NULL);
INSERT INTO `t_permission`
VALUES (54, '用户管理-删除', 'user:delete', NULL, 'button', 49, NULL, NULL);
INSERT INTO `t_permission`
VALUES (55, '系统管理', NULL, NULL, 'menu', 0, 8, 'Setting');
INSERT INTO `t_permission`
VALUES (56, '系统管理', NULL, '/dashboard/system', 'menu', 55, 1, 'Tools');
INSERT INTO `t_permission`
VALUES (57, '系统管理-列表', 'system:list', NULL, 'button', 56, NULL, NULL);
INSERT INTO `t_permission`
VALUES (58, '系统管理-录入', 'system:add', NULL, 'button', 56, NULL, NULL);
INSERT INTO `t_permission`
VALUES (59, '系统管理-编辑', 'system:edit', NULL, 'button', 56, NULL, NULL);
INSERT INTO `t_permission`
VALUES (60, '系统管理-查看', 'system:view', NULL, 'button', 56, NULL, NULL);
INSERT INTO `t_permission`
VALUES (61, '系统管理-删除', 'system:delete', NULL, 'button', 56, NULL, NULL);

-- ----------------------------
-- Table structure for t_product
-- ----------------------------
DROP TABLE IF EXISTS `t_product`;
CREATE TABLE `t_product`
(
    `id`            int                                                           NOT NULL AUTO_INCREMENT COMMENT '主键，自动增长，线索ID',
    `name`          varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '产品名称',
    `guide_price_s` decimal(10, 2)                                                NULL DEFAULT NULL COMMENT '官方指导起始价',
    `guide_price_e` decimal(10, 2)                                                NULL DEFAULT NULL COMMENT '官方指导最高价',
    `quotation`     decimal(10, 2)                                                NULL DEFAULT NULL COMMENT '经销商报价',
    `state`         int                                                           NULL DEFAULT NULL COMMENT '状态 0在售 1售罄',
    `create_time`   datetime                                                      NULL DEFAULT NULL COMMENT '创建时间',
    `create_by`     int                                                           NULL DEFAULT NULL COMMENT '创建人',
    `edit_time`     datetime                                                      NULL DEFAULT NULL COMMENT '编辑时间',
    `edit_by`       int                                                           NULL DEFAULT NULL COMMENT '编辑人',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `t_product_ibfk_1` (`create_by` ASC) USING BTREE,
    INDEX `t_product_ibfk_2` (`edit_by` ASC) USING BTREE
) ENGINE = InnoDB
  AUTO_INCREMENT = 11
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT = '产品表'
  ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of t_product
-- ----------------------------
INSERT INTO `t_product`
VALUES (1, '海鸥', 10.18, 10.58, 9.28, 0, '2023-04-06 18:25:00', 1, NULL, NULL);
INSERT INTO `t_product`
VALUES (2, '比亚迪e2', 10.28, 10.98, 9.78, 0, '2023-04-03 15:26:12', 1, NULL, NULL);
INSERT INTO `t_product`
VALUES (3, '比亚迪e3', 15.48, 15.98, 14.38, 0, '2023-04-03 11:29:08', 1, NULL, NULL);
INSERT INTO `t_product`
VALUES (4, '海豚', 11.68, 13.68, 10.86, 0, '2023-04-09 10:27:47', 1, NULL, NULL);
INSERT INTO `t_product`
VALUES (5, '秦EV', 12.99, 16.98, 11.98, 0, '2023-04-08 15:28:23', 1, NULL, NULL);
INSERT INTO `t_product`
VALUES (6, '秦PLUS DM-i', 9.98, 16.58, 9.06, 0, '2023-04-10 19:29:53', 1, NULL, NULL);
INSERT INTO `t_product`
VALUES (7, '秦PLUS EV', 12.98, 18.08, 12.38, 0, '2023-04-05 09:30:31', 1, NULL, NULL);
INSERT INTO `t_product`
VALUES (8, '海豹', 21.28, 28.98, 20.18, 0, '2023-04-02 10:31:08', 1, NULL, NULL);
INSERT INTO `t_product`
VALUES (9, '汉DM', 21.78, 32.18, 19.88, 0, '2023-04-07 16:31:45', 1, NULL, NULL);
INSERT INTO `t_product`
VALUES (10, '宋PLUS EV', 18.68, 20.38, 17.86, 0, '2023-03-18 21:33:08', 1, NULL, NULL);

-- ----------------------------
-- Table structure for t_role
-- ----------------------------
DROP TABLE IF EXISTS `t_role`;
CREATE TABLE `t_role`
(
    `id`        int                                                          NOT NULL AUTO_INCREMENT,
    `role`      varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
    `role_name` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB
  AUTO_INCREMENT = 6
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT = '角色表'
  ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of t_role
-- ----------------------------
INSERT INTO `t_role`
VALUES (1, 'admin', '管理员');
INSERT INTO `t_role`
VALUES (2, 'saler', '销售员');
INSERT INTO `t_role`
VALUES (3, 'manager', '销售经理');
INSERT INTO `t_role`
VALUES (4, 'marketing ', '市场营销');
INSERT INTO `t_role`
VALUES (5, 'accountant', '会计');

-- ----------------------------
-- Table structure for t_role_permission
-- ----------------------------
DROP TABLE IF EXISTS `t_role_permission`;
CREATE TABLE `t_role_permission`
(
    `id`            int NOT NULL AUTO_INCREMENT,
    `role_id`       int NULL DEFAULT NULL,
    `permission_id` int NULL DEFAULT NULL,
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `t_role_permission_ibfk_1` (`role_id` ASC) USING BTREE,
    INDEX `t_role_permission_ibfk_2` (`permission_id` ASC) USING BTREE
) ENGINE = InnoDB
  AUTO_INCREMENT = 77
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT = '角色权限关系表'
  ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of t_role_permission
-- ----------------------------
INSERT INTO `t_role_permission`
VALUES (1, 1, 1);
INSERT INTO `t_role_permission`
VALUES (2, 1, 2);
INSERT INTO `t_role_permission`
VALUES (3, 1, 3);
INSERT INTO `t_role_permission`
VALUES (4, 1, 4);
INSERT INTO `t_role_permission`
VALUES (5, 1, 5);
INSERT INTO `t_role_permission`
VALUES (6, 1, 6);
INSERT INTO `t_role_permission`
VALUES (7, 1, 7);
INSERT INTO `t_role_permission`
VALUES (8, 1, 9);
INSERT INTO `t_role_permission`
VALUES (9, 1, 10);
INSERT INTO `t_role_permission`
VALUES (10, 1, 12);
INSERT INTO `t_role_permission`
VALUES (11, 1, 13);
INSERT INTO `t_role_permission`
VALUES (12, 1, 14);
INSERT INTO `t_role_permission`
VALUES (13, 1, 15);
INSERT INTO `t_role_permission`
VALUES (14, 1, 16);
INSERT INTO `t_role_permission`
VALUES (15, 1, 17);
INSERT INTO `t_role_permission`
VALUES (16, 1, 18);
INSERT INTO `t_role_permission`
VALUES (17, 1, 19);
INSERT INTO `t_role_permission`
VALUES (18, 1, 20);
INSERT INTO `t_role_permission`
VALUES (19, 1, 21);
INSERT INTO `t_role_permission`
VALUES (20, 1, 22);
INSERT INTO `t_role_permission`
VALUES (21, 1, 23);
INSERT INTO `t_role_permission`
VALUES (22, 1, 24);
INSERT INTO `t_role_permission`
VALUES (23, 1, 25);
INSERT INTO `t_role_permission`
VALUES (24, 1, 26);
INSERT INTO `t_role_permission`
VALUES (25, 1, 27);
INSERT INTO `t_role_permission`
VALUES (26, 1, 28);
INSERT INTO `t_role_permission`
VALUES (27, 1, 29);
INSERT INTO `t_role_permission`
VALUES (28, 1, 30);
INSERT INTO `t_role_permission`
VALUES (29, 1, 31);
INSERT INTO `t_role_permission`
VALUES (30, 1, 32);
INSERT INTO `t_role_permission`
VALUES (31, 1, 33);
INSERT INTO `t_role_permission`
VALUES (32, 1, 34);
INSERT INTO `t_role_permission`
VALUES (33, 1, 35);
INSERT INTO `t_role_permission`
VALUES (34, 1, 36);
INSERT INTO `t_role_permission`
VALUES (35, 1, 37);
INSERT INTO `t_role_permission`
VALUES (36, 1, 38);
INSERT INTO `t_role_permission`
VALUES (37, 1, 39);
INSERT INTO `t_role_permission`
VALUES (38, 1, 40);
INSERT INTO `t_role_permission`
VALUES (39, 1, 41);
INSERT INTO `t_role_permission`
VALUES (40, 1, 42);
INSERT INTO `t_role_permission`
VALUES (41, 1, 43);
INSERT INTO `t_role_permission`
VALUES (42, 1, 44);
INSERT INTO `t_role_permission`
VALUES (43, 1, 45);
INSERT INTO `t_role_permission`
VALUES (44, 1, 46);
INSERT INTO `t_role_permission`
VALUES (45, 1, 47);
INSERT INTO `t_role_permission`
VALUES (46, 1, 48);
INSERT INTO `t_role_permission`
VALUES (47, 1, 49);
INSERT INTO `t_role_permission`
VALUES (48, 1, 50);
INSERT INTO `t_role_permission`
VALUES (49, 1, 51);
INSERT INTO `t_role_permission`
VALUES (50, 1, 52);
INSERT INTO `t_role_permission`
VALUES (51, 1, 53);
INSERT INTO `t_role_permission`
VALUES (52, 1, 54);
INSERT INTO `t_role_permission`
VALUES (53, 1, 55);
INSERT INTO `t_role_permission`
VALUES (54, 1, 56);
INSERT INTO `t_role_permission`
VALUES (55, 1, 57);
INSERT INTO `t_role_permission`
VALUES (56, 1, 58);
INSERT INTO `t_role_permission`
VALUES (57, 1, 59);
INSERT INTO `t_role_permission`
VALUES (58, 1, 60);
INSERT INTO `t_role_permission`
VALUES (59, 1, 61);
INSERT INTO `t_role_permission`
VALUES (60, 2, 10);
INSERT INTO `t_role_permission`
VALUES (61, 2, 12);
INSERT INTO `t_role_permission`
VALUES (62, 2, 13);
INSERT INTO `t_role_permission`
VALUES (63, 2, 14);
INSERT INTO `t_role_permission`
VALUES (64, 2, 15);
INSERT INTO `t_role_permission`
VALUES (65, 2, 16);
INSERT INTO `t_role_permission`
VALUES (66, 2, 57);
INSERT INTO `t_role_permission`
VALUES (67, 2, 18);
INSERT INTO `t_role_permission`
VALUES (68, 2, 19);
INSERT INTO `t_role_permission`
VALUES (69, 2, 20);
INSERT INTO `t_role_permission`
VALUES (70, 2, 21);
INSERT INTO `t_role_permission`
VALUES (71, 2, 22);
INSERT INTO `t_role_permission`
VALUES (72, 2, 23);
INSERT INTO `t_role_permission`
VALUES (73, 2, 24);
INSERT INTO `t_role_permission`
VALUES (74, 2, 25);
INSERT INTO `t_role_permission`
VALUES (75, 2, 26);
INSERT INTO `t_role_permission`
VALUES (76, 2, 27);

-- ----------------------------
-- Table structure for t_system_info
-- ----------------------------
DROP TABLE IF EXISTS `t_system_info`;
CREATE TABLE `t_system_info`
(
    `id`           int                                                           NOT NULL AUTO_INCREMENT,
    `system_code`  varchar(45) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci  NULL DEFAULT NULL,
    `name`         varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL,
    `site`         varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL,
    `logo`         varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
    `title`        varchar(45) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci  NOT NULL,
    `description`  varchar(45) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci  NOT NULL,
    `keywords`     varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL,
    `shortcuticon` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL,
    `tel`          varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
    `weixin`       varchar(25) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci  NULL DEFAULT NULL,
    `email`        varchar(45) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci  NULL DEFAULT NULL,
    `address`      varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
    `version`      varchar(145) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
    `closeMsg`     varchar(500) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
    `isopen`       varchar(8) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci   NULL DEFAULT 'y',
    `create_time`  datetime                                                      NULL DEFAULT NULL,
    `create_by`    int                                                           NULL DEFAULT NULL,
    `edit_time`    datetime                                                      NULL DEFAULT NULL,
    `edit_by`      int                                                           NULL DEFAULT NULL,
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `t_system_info_ibfk_1` (`create_by` ASC) USING BTREE,
    INDEX `t_system_info_ibfk_2` (`edit_by` ASC) USING BTREE
) ENGINE = InnoDB
  AUTO_INCREMENT = 3
  CHARACTER SET = utf8mb3
  COLLATE = utf8mb3_general_ci COMMENT = '系统信息表'
  ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of t_system_info
-- ----------------------------
INSERT INTO `t_system_info`
VALUES (1, 'crm', '动力云客系统', 'http://www.bjpowernode.com', 'http://localhost:8080/image/logo.png', '动力云客系统',
        '动力CRM 企业客户智慧云管理', 'crm, 客户, 客户关系, 客户关系管理', 'http://www.bjpowernode.com/favicon.ico',
        '010-84846003', '123456789', '123456789@qq.com', '北京市大兴区大族企业湾10栋3层', '系统版本:1.1.0.bate',
        '网站维护中 动力云客系统 http://www.bjpowernode.com', 'true', '2023-11-08 13:28:18', 1, NULL, NULL);
INSERT INTO `t_system_info`
VALUES (2, 'call', '动力呼叫系统', 'http://www.bjpowernode.com', 'http://localhost:8080/image/logo.png', '动力呼叫系统',
        '动力CRM 企业客户智慧云管理', 'crm, 客户, 客户关系, 客户关系管理', 'http://www.bjpowernode.com/favicon.ico',
        '010-84846003', '123456789', '123456789@qq.com', '北京市大兴区大族企业湾10栋3层', '系统版本:1.1.0.bate',
        '网站维护中 动力呼叫系统 http://www.bjpowernode.com', 'true', '2023-11-08 13:28:21', 1, NULL, NULL);

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
    `stage`             int                                                           NULL DEFAULT NULL COMMENT '交易所处阶段',
    `description`       varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '交易描述',
    `next_contact_time` datetime                                                      NULL DEFAULT NULL COMMENT '下次联系时间',
    `create_time`       datetime                                                      NULL DEFAULT NULL COMMENT '创建时间',
    `create_by`         int                                                           NULL DEFAULT NULL COMMENT '创建人',
    `edit_time`         datetime                                                      NULL DEFAULT NULL COMMENT '编辑时间',
    `edit_by`           int                                                           NULL DEFAULT NULL COMMENT '编辑人',
    PRIMARY KEY (`id`) USING BTREE,
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
VALUES (5, '202311121932019431', 1, 120000.00, '2023-11-14 00:00:00', 42, '123131', '2023-11-29 00:00:00',
        '2023-11-12 19:32:02', 1, NULL, NULL);
INSERT INTO `t_tran`
VALUES (6, '202311121932019432', 1, 93000.00, '2023-11-12 00:00:00', 12, '123131', '2023-11-29 00:00:00',
        '2023-11-12 19:32:02', 1, NULL, NULL);

-- ----------------------------
-- Table structure for t_tran_history
-- ----------------------------
DROP TABLE IF EXISTS `t_tran_history`;
CREATE TABLE `t_tran_history`
(
    `id`            int            NOT NULL AUTO_INCREMENT COMMENT '主键，自动增长，交易记录ID',
    `tran_id`       int            NULL DEFAULT NULL COMMENT '交易ID',
    `stage`         int            NULL DEFAULT NULL COMMENT '交易阶段',
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
INSERT INTO `t_tran_history` (`id`, `tran_id`, `stage`, `money`, `expected_date`, `create_time`, `create_by`)
VALUES (1, 5, 12, 120000.00, '2023-11-14 00:00:00', '2023-11-12 19:32:02', 1),
       (2, 5, 37, 120000.00, '2023-11-14 00:00:00', '2023-11-13 10:00:00', 1),
       (3, 5, 40, 120000.00, '2023-11-14 00:00:00', '2023-11-14 14:30:00', 1),
       (4, 5, 42, 120000.00, '2023-11-14 00:00:00', '2023-11-15 16:45:00', 1),
       (5, 6, 12, 93000.00, '2023-11-12 00:00:00', '2023-11-12 19:32:02', 1),
       (6, 6, 37, 93000.00, '2023-11-12 00:00:00', '2023-11-13 11:20:00', 1);

-- ----------------------------
-- Table structure for t_tran_remark
-- ----------------------------
DROP TABLE IF EXISTS `t_tran_remark`;
CREATE TABLE `t_tran_remark`
(
    `id`           int                                                           NOT NULL AUTO_INCREMENT COMMENT '主键，自动增长，交易备注ID',
    `tran_id`      int                                                           NULL DEFAULT NULL COMMENT '交易ID',
    `note_way`     int                                                           NULL DEFAULT NULL COMMENT '跟踪方式',
    `note_content` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '跟踪内容',
    `create_time`  datetime                                                      NULL DEFAULT NULL COMMENT '跟踪时间',
    `create_by`    int                                                           NULL DEFAULT NULL COMMENT '跟踪人',
    `edit_time`    datetime                                                      NULL DEFAULT NULL COMMENT '编辑时间',
    `edit_by`      int                                                           NULL DEFAULT NULL COMMENT '编辑人',
    `deleted`      int                                                           NULL DEFAULT NULL COMMENT '删除状态（0正常，1删除）',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `t_tran_remark_ibfk_1` (`tran_id` ASC) USING BTREE,
    INDEX `t_tran_remark_ibfk_2` (`note_way` ASC) USING BTREE,
    INDEX `t_tran_remark_ibfk_3` (`create_by` ASC) USING BTREE,
    INDEX `t_tran_remark_ibfk_4` (`edit_by` ASC) USING BTREE
) ENGINE = InnoDB
  AUTO_INCREMENT = 14
  CHARACTER SET = utf8mb3
  COLLATE = utf8mb3_general_ci COMMENT = '交易跟踪记录表'
  ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of t_tran_remark
-- ----------------------------
INSERT INTO `t_tran_remark`
VALUES (1, 5, 65, '客户对价格很满意', '2023-11-12 19:35:00', 1, NULL, NULL, 0);
INSERT INTO `t_tran_remark`
VALUES (2, 5, 64, '已完成首付款支付', '2023-11-13 10:15:00', 1, NULL, NULL, 0);
INSERT INTO `t_tran_remark`
VALUES (3, 5, 63, '合同已签署', '2023-11-14 15:00:00', 1, NULL, NULL, 0);
INSERT INTO `t_tran_remark`
VALUES (4, 6, 61, '客户需要分期付款', '2023-11-12 19:40:00', 1, NULL, NULL, 0);
INSERT INTO `t_tran_remark`
VALUES (5, 6, 62, '已确认付款计划', '2023-11-13 11:30:00', 1, NULL, NULL, 0);

-- ----------------------------
-- Table structure for t_user
-- ----------------------------
DROP TABLE IF EXISTS `t_user`;
CREATE TABLE `t_user`
(
    `id`                     int                                                          NOT NULL AUTO_INCREMENT COMMENT '主键，自动增长，用户ID',
    `login_act`              varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '登录账号',
    `login_pwd`              varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '登录密码',
    `name`                   varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '用户姓名',
    `phone`                  varchar(18) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '用户手机',
    `email`                  varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '用户邮箱',
    `account_no_expired`     int                                                          NULL DEFAULT NULL COMMENT '账户是否没有过期，0已过期 1正常',
    `credentials_no_expired` int                                                          NULL DEFAULT NULL COMMENT '密码是否没有过期，0已过期 1正常',
    `account_no_locked`      int                                                          NULL DEFAULT NULL COMMENT '账号是否没有锁定，0已锁定 1正常',
    `account_enabled`        int                                                          NULL DEFAULT NULL COMMENT '账号是否启用，0禁用 1启用',
    `create_time`            datetime                                                     NULL DEFAULT NULL COMMENT '创建时间',
    `create_by`              int                                                          NULL DEFAULT NULL COMMENT '创建人',
    `edit_time`              datetime                                                     NULL DEFAULT NULL COMMENT '编辑时间',
    `edit_by`                int                                                          NULL DEFAULT NULL COMMENT '编辑人',
    `last_login_time`        datetime                                                     NULL DEFAULT NULL COMMENT '最近登录时间',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `login_act` (`login_act` ASC) USING BTREE,
    UNIQUE INDEX `phone` (`phone` ASC) USING BTREE,
    UNIQUE INDEX `email` (`email` ASC) USING BTREE
) ENGINE = InnoDB
  AUTO_INCREMENT = 34
  CHARACTER SET = utf8mb3
  COLLATE = utf8mb3_general_ci COMMENT = '用户表'
  ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of t_user
-- ----------------------------
INSERT INTO `t_user`
VALUES (1, 'admin', '$2a$10$Nlhwhtd0BSCBK95CAifv7eWpCjHloPBMZ3Gaehcc56hRAV3DZALJO', '管理员', '13700000000',
        'admin@qq.com', 1, 1, 1, 1, '2023-02-22 09:37:12', NULL, '2023-05-23 00:21:06', NULL, '2023-12-10 21:18:59');
INSERT INTO `t_user`
VALUES (2, 'yuyan', '$2a$10$ZzzAd0nDuUGfGSjJDnZIyOaW7mUZkFzsYgOqiF/b07po/BGxBFjJ.', '于嫣', 'null', 'yuyan@163.com', 1,
        1, 1, 1, '2023-02-28 12:11:40', NULL, '2023-05-23 00:21:14', NULL, '2023-11-29 20:14:31');
INSERT INTO `t_user`
VALUES (3, 'zhangqi', '$2a$10$Q0qTW6QqkabTzFyoilViw..YdrVzZkSKe5RvLmjgPgW/IrcPkBoF.', '张琪', '1362362323',
        'zhangqi@qq.com', 1, 1, 1, 1, '2023-03-02 11:37:34', NULL, '2023-05-23 00:21:02', NULL, NULL);
INSERT INTO `t_user`
VALUES (4, 'suwanting', '$2a$10$3bambNLTCAKtQn2OXPiHb.f0SzH.MucTiLi6GPT6nQrYpsxsdxaFi', '苏婉婷', NULL,
        'suwanting@qq.com', 1, 1, 1, 1, '2023-04-03 15:04:54', NULL, NULL, NULL, NULL);
INSERT INTO `t_user`
VALUES (5, 'wuxiaoxiao', '$2a$10$Lmk5wXYkZzQMFJEcXVZAZegIQhnAm6ONHpz09X/.gbOh5ze5fU6MW', '吴潇潇', NULL,
        'wuxiaoxiao@qq.com', 1, 1, 1, 1, '2023-01-27 12:15:26', NULL, NULL, NULL, NULL);
INSERT INTO `t_user`
VALUES (6, 'mengyan', '$2a$10$6zGT7CfeuJ/6jZPk1pAqcuiMYDnCJstrceThGD5DVVOA5XvOP/sQq', '孟岩', NULL, 'mengyan@163.com',
        1, 1, 1, 1, '2023-03-19 10:17:28', NULL, NULL, NULL, NULL);
INSERT INTO `t_user`
VALUES (7, 'yuanhuimin', '$2a$10$mbsloGtPV7cDwfAVYxuvLemQRWumZKrDxVZxg4fnbfaocnfZFlYuu', '袁慧敏', NULL,
        'yuanhuimin@11.com', 1, 1, 1, 1, '2023-04-11 20:18:50', NULL, NULL, NULL, NULL);
INSERT INTO `t_user`
VALUES (8, 'qinxuwen', '$2a$10$ir8uLlBrPMHRtGiu5Ajkv.UKcRacXWRen7zxelp9iUaco3WhGkJ36', '秦旭文', '13820000000',
        'qinxuwen@163.com', 1, 1, 1, 1, '2023-03-19 21:11:37', NULL, NULL, NULL, NULL);
INSERT INTO `t_user`
VALUES (9, 'chengjie', '$2a$10$jQR8yyF/ailGP/zW6G4JOOffzWOXhe02Rgw7MZLfxL.IGFdM3cjM2', '程杰', '13500000000',
        'chengjie@qq.com', 1, 1, 1, 1, '2023-04-16 07:16:19', NULL, '2023-04-20 21:42:21', NULL, NULL);
INSERT INTO `t_user`
VALUES (10, 'zhouliang', '$2a$10$0yOGdkAcG8JLEcoEmmCnfO8Vp6rcqBnn30k6pGor5Z0.eLMyLEd7.', '周亮', '13800000008',
        'zhouliang@163.com', 1, 1, 1, 1, '2023-03-18 13:13:45', NULL, '2023-06-06 00:06:31', NULL, NULL);
INSERT INTO `t_user`
VALUES (11, 'zhangwei', '$2a$10$BfOgsdSAZ9VYBOzv692BM.oWGPLktcqhhjU3AaWESkGNRcW484N7O', '张伟', NULL, 'zhangwei@qq.com',
        1, 1, 1, 1, '2023-03-06 09:18:23', NULL, NULL, NULL, NULL);
INSERT INTO `t_user`
VALUES (12, 'dengping', '$2a$10$hpN8orfqUFXb.WWbIoZBkOZrr6D8rdSbl/SWXsMQ0zEuqkldlkpW2', '邓萍', NULL, 'dengping@qq.com',
        1, 1, 1, 1, '2023-02-19 20:10:58', NULL, NULL, NULL, NULL);
INSERT INTO `t_user`
VALUES (13, 'zhangxing', '$2a$10$uBVDcCCJQvTfoFCjbjwrf.MhyczNNJfCn76jD61CsAgsUlXjXhxzG', '张欣', NULL,
        'zhangxing@qq.com', 1, 1, 1, 1, '2023-03-17 12:12:11', NULL, NULL, NULL, NULL);
INSERT INTO `t_user`
VALUES (14, 'zhangmeng', '$2a$10$MMHG2cQh4H4YFbdf48SnyO9IZ78F110x3.7IWGNExrgk2rFmhrd/u', '张萌', NULL,
        'zhangmeng@qq.com', 1, 1, 1, 1, '2023-01-13 08:16:02', NULL, NULL, NULL, NULL);
INSERT INTO `t_user`
VALUES (15, 'shixixiang', '$2a$10$zYwq/QfevFPAZxw4b2DkCeQvjVQ52AUU9c4aC0uS0wTJaRr75G74y', '石喜祥', NULL,
        'shixixiang@qq.com', 1, 1, 1, 1, '2023-03-10 15:19:49', NULL, NULL, NULL, NULL);
INSERT INTO `t_user`
VALUES (16, 'chengjiuming', '$2a$10$yNN5TcFkM4OqRsKGNM8CNeqAJhRYKQgXVFqbre5lQPicnIXT7THTu', '陈久明', NULL,
        'chengjiuming@163.com', 1, 1, 1, 1, '2023-04-09 23:17:37', NULL, NULL, NULL, NULL);
INSERT INTO `t_user`
VALUES (17, 'genghao', '$2a$10$rWHo.vUpJCbqWLGMkPj95O5FlhaQLzro.LY7pVQ/UnVVAdvjEAy0K', '耿浩', NULL, 'genghao@qq.com',
        1, 1, 1, 1, '2023-03-19 12:10:22', NULL, '2023-04-10 21:42:21', NULL, NULL);
INSERT INTO `t_user`
VALUES (18, 'hanmingyang', '$2a$10$PRMdG7a8nFIN1A3TD584Xe2BZI7Y0mktDL7Wp5lF88E1D1iPijFc6', '韩明洋', NULL,
        'hanmingyang@163.com', 1, 1, 1, 1, '2023-02-12 18:13:01', NULL, '2023-04-13 23:43:25', NULL, NULL);
INSERT INTO `t_user`
VALUES (19, 'xuyan', '$2a$10$S7MF2dOqFcoOJPqpEH2nu.Muhn2XC0BlBTZ5gAoL3axrQxdJEJNnK', '徐燕', NULL, 'xuyan@qq.com', 1, 1,
        1, 1, '2023-03-29 13:16:15', NULL, NULL, NULL, NULL);
INSERT INTO `t_user`
VALUES (20, 'chengjuan', '$2a$10$m1g5cxikApV05pR7Cx4cy.d4sT3efOl6UvDLvH27WzMjtpymQ5ANi', '程娟', NULL,
        'chengjuan@qq.com', 1, 1, 1, 1, '2023-02-19 15:12:22', NULL, NULL, NULL, NULL);
INSERT INTO `t_user`
VALUES (21, 'huangxiao', '$2a$10$R/RwQd5.3OxYpSZBLIn8DeeYYNF0vgWCrCR4tcyL.c/HtnuIfBRIK', '黄潇', NULL,
        'huangxiao@qq.com', 1, 1, 1, 1, '2023-03-26 22:11:37', NULL, NULL, NULL, NULL);
INSERT INTO `t_user`
VALUES (22, 'yangyuxin', '$2a$10$ucE/By6NLBb4tN5H3CUimOQ2eAtbjXFf2v77SJUPbHXRI9lTF97Ka', '杨雨欣', NULL,
        'yangyuxin@163.com', 1, 1, 1, 1, '2023-04-13 18:14:59', NULL, NULL, NULL, NULL);
INSERT INTO `t_user`
VALUES (23, 'xiaojie', '$2a$10$A215.iFSp7/d99X5M6KE.eu5YvA7nJ5vNEJraxmpA8EUYJN6lx9rW', '肖捷', NULL, 'xiaojie@163.com',
        1, 1, 1, 1, '2023-02-18 09:19:02', NULL, NULL, NULL, NULL);
INSERT INTO `t_user`
VALUES (32, '111', '$2a$10$A215.iFSp7/d99X5M6KE.eu5YvA7nJ5vNEJraxmpA8EUYJN6lx9rW', '1111', '1111', '111', 1, 1, 1, 1,
        '2023-04-25 17:42:36', NULL, NULL, NULL, NULL);
INSERT INTO `t_user`
VALUES (33, '222', '$2a$10$bBskT77XGhKDt1Oqg4aWseHOR2Yngv/Pozh76FprgM1JWDr6WWuFe', '22', '222', '222', 1, 1, 1, 1,
        '2023-04-25 17:46:07', NULL, '2023-06-20 22:23:48', NULL, NULL);

-- ----------------------------
-- Table structure for t_user_role
-- ----------------------------
DROP TABLE IF EXISTS `t_user_role`;
CREATE TABLE `t_user_role`
(
    `id`      int NOT NULL AUTO_INCREMENT,
    `user_id` int NULL DEFAULT NULL,
    `role_id` int NULL DEFAULT NULL,
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `t_user_role_ibfk_1` (`user_id` ASC) USING BTREE,
    INDEX `t_user_role_ibfk_2` (`role_id` ASC) USING BTREE
) ENGINE = InnoDB
  AUTO_INCREMENT = 7
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT = '用户角色关系表'
  ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of t_user_role
-- ----------------------------
INSERT INTO `t_user_role`
VALUES (1, 1, 1);
INSERT INTO `t_user_role`
VALUES (2, 2, 2);
INSERT INTO `t_user_role`
VALUES (3, 3, 2);
INSERT INTO `t_user_role`
VALUES (4, 4, 3);
INSERT INTO `t_user_role`
VALUES (5, 5, 4);
INSERT INTO `t_user_role`
VALUES (6, 6, 5);

-- ----------------------------
-- Table structure for t_tran_product
-- ----------------------------
DROP TABLE IF EXISTS `t_tran_product`;
CREATE TABLE `t_tran_product`
(
    `id`          int            NOT NULL AUTO_INCREMENT COMMENT '主键',
    `tran_id`     int            NOT NULL COMMENT '交易ID',
    `product_id`  int            NOT NULL COMMENT '产品ID',
    `quantity`    int            NOT NULL COMMENT '数量',
    `price`       decimal(10, 2) NOT NULL COMMENT '单价',
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
-- Table structure for t_tran_production
-- ----------------------------
DROP TABLE IF EXISTS `t_tran_production`;
CREATE TABLE `t_tran_production`
(
    `id`              int          NOT NULL AUTO_INCREMENT COMMENT '主键',
    `tran_product_id` int          NOT NULL COMMENT '交易产品ID',
    `status`          varchar(20)  NOT NULL COMMENT '生产状态：PENDING-待生产, IN_PROGRESS-生产中, COMPLETED-已完成',
    `description`     varchar(255) NULL DEFAULT NULL COMMENT '生产状态描述',
    `create_time`     datetime     NULL DEFAULT NULL COMMENT '创建时间',
    `create_by`       int          NULL DEFAULT NULL COMMENT '创建人',
    `update_time`     datetime     NULL DEFAULT NULL COMMENT '更新时间',
    `update_by`       int          NULL DEFAULT NULL COMMENT '更新人',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `t_tran_production_ibfk_1` (`tran_product_id` ASC) USING BTREE,
    INDEX `t_tran_production_ibfk_2` (`create_by` ASC) USING BTREE,
    INDEX `t_tran_production_ibfk_3` (`update_by` ASC) USING BTREE
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  CHARACTER SET = utf8mb3
  COLLATE = utf8mb3_general_ci COMMENT = '交易生产状态表'
  ROW_FORMAT = DYNAMIC;

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
    `update_time`  datetime       NULL DEFAULT NULL COMMENT '更新时间',
    `update_by`    int            NULL DEFAULT NULL COMMENT '更新人',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE KEY `uk_invoice_no` (`invoice_no`),
    INDEX `t_tran_invoice_ibfk_1` (`tran_id` ASC) USING BTREE,
    INDEX `t_tran_invoice_ibfk_2` (`create_by` ASC) USING BTREE,
    INDEX `t_tran_invoice_ibfk_3` (`update_by` ASC) USING BTREE
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  CHARACTER SET = utf8mb3
  COLLATE = utf8mb3_general_ci COMMENT = '交易发票表'
  ROW_FORMAT = DYNAMIC;

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
    `stage`             int                                                           NULL DEFAULT NULL COMMENT '交易所处阶段',
    `description`       varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '交易描述',
    `next_contact_time` datetime                                                      NULL DEFAULT NULL COMMENT '下次联系时间',
    `create_time`       datetime                                                      NULL DEFAULT NULL COMMENT '创建时间',
    `create_by`         int                                                           NULL DEFAULT NULL COMMENT '创建人',
    `edit_time`         datetime                                                      NULL DEFAULT NULL COMMENT '编辑时间',
    `edit_by`           int                                                           NULL DEFAULT NULL COMMENT '编辑人',
    PRIMARY KEY (`id`) USING BTREE,
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
VALUES (5, '202311121932019431', 1, 120000.00, '2023-11-14 00:00:00', 42, '123131', '2023-11-29 00:00:00',
        '2023-11-12 19:32:02', 1, NULL, NULL);
INSERT INTO `t_tran`
VALUES (6, '202311121932019432', 1, 93000.00, '2023-11-12 00:00:00', 12, '123131', '2023-11-29 00:00:00',
        '2023-11-12 19:32:02', 1, NULL, NULL);

-- ----------------------------
-- Table structure for t_tran_history
-- ----------------------------
DROP TABLE IF EXISTS `t_tran_history`;
CREATE TABLE `t_tran_history`
(
    `id`            int            NOT NULL AUTO_INCREMENT COMMENT '主键，自动增长，交易记录ID',
    `tran_id`       int            NULL DEFAULT NULL COMMENT '交易ID',
    `stage`         int            NULL DEFAULT NULL COMMENT '交易阶段',
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
INSERT INTO `t_tran_history` (`id`, `tran_id`, `stage`, `money`, `expected_date`, `create_time`, `create_by`)
VALUES (1, 5, 12, 120000.00, '2023-11-14 00:00:00', '2023-11-12 19:32:02', 1),
       (2, 5, 37, 120000.00, '2023-11-14 00:00:00', '2023-11-13 10:00:00', 1),
       (3, 5, 40, 120000.00, '2023-11-14 00:00:00', '2023-11-14 14:30:00', 1),
       (4, 5, 42, 120000.00, '2023-11-14 00:00:00', '2023-11-15 16:45:00', 1),
       (5, 6, 12, 93000.00, '2023-11-12 00:00:00', '2023-11-12 19:32:02', 1),
       (6, 6, 37, 93000.00, '2023-11-12 00:00:00', '2023-11-13 11:20:00', 1);

-- ----------------------------
-- Table structure for t_tran_product
-- ----------------------------
DROP TABLE IF EXISTS `t_tran_product`;
CREATE TABLE `t_tran_product`
(
    `id`          int            NOT NULL AUTO_INCREMENT COMMENT '主键',
    `tran_id`     int            NOT NULL COMMENT '交易ID',
    `product_id`  int            NOT NULL COMMENT '产品ID',
    `quantity`    int            NOT NULL COMMENT '数量',
    `price`       decimal(10, 2) NOT NULL COMMENT '单价',
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
VALUES (1, 5, 8, 2, 65000.00, '2023-11-12 19:32:02', 1);
INSERT INTO `t_tran_product`
VALUES (2, 6, 7, 3, 32000.00, '2023-11-12 19:32:02', 1);

-- ----------------------------
-- Table structure for t_tran_production
-- ----------------------------
DROP TABLE IF EXISTS `t_tran_production`;
CREATE TABLE `t_tran_production`
(
    `id`              int          NOT NULL AUTO_INCREMENT COMMENT '主键',
    `tran_product_id` int          NOT NULL COMMENT '交易产品ID',
    `status`          varchar(20)  NOT NULL COMMENT '生产状态：PENDING-待生产, IN_PROGRESS-生产中, COMPLETED-已完成',
    `description`     varchar(255) NULL DEFAULT NULL COMMENT '生产状态描述',
    `create_time`     datetime     NULL DEFAULT NULL COMMENT '创建时间',
    `create_by`       int          NULL DEFAULT NULL COMMENT '创建人',
    `update_time`     datetime     NULL DEFAULT NULL COMMENT '更新时间',
    `update_by`       int          NULL DEFAULT NULL COMMENT '更新人',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `t_tran_production_ibfk_1` (`tran_product_id` ASC) USING BTREE,
    INDEX `t_tran_production_ibfk_2` (`create_by` ASC) USING BTREE,
    INDEX `t_tran_production_ibfk_3` (`update_by` ASC) USING BTREE
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  CHARACTER SET = utf8mb3
  COLLATE = utf8mb3_general_ci COMMENT = '交易生产状态表'
  ROW_FORMAT = DYNAMIC;

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
    `update_time`  datetime       NULL DEFAULT NULL COMMENT '更新时间',
    `update_by`    int            NULL DEFAULT NULL COMMENT '更新人',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE KEY `uk_invoice_no` (`invoice_no`),
    INDEX `t_tran_invoice_ibfk_1` (`tran_id` ASC) USING BTREE,
    INDEX `t_tran_invoice_ibfk_2` (`create_by` ASC) USING BTREE,
    INDEX `t_tran_invoice_ibfk_3` (`update_by` ASC) USING BTREE
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  CHARACTER SET = utf8mb3
  COLLATE = utf8mb3_general_ci COMMENT = '交易发票表'
  ROW_FORMAT = DYNAMIC;


-- ----------------------------
-- Table structure for t_product
-- ----------------------------
DROP TABLE IF EXISTS `t_product`;
CREATE TABLE `t_product`
(
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '商品的唯一标识符',
    `sku`           VARCHAR(255)   DEFAULT NULL COMMENT '商品的库存单位',
    `name`          VARCHAR(255) NOT NULL COMMENT '商品名称',
    `category`      VARCHAR(255)   DEFAULT NULL COMMENT '商品类别',
    `specification` VARCHAR(255)   DEFAULT NULL COMMENT '商品规格',
    `price`         DECIMAL(10, 2) DEFAULT NULL COMMENT '商品价格',
    `stock`         INT            DEFAULT NULL COMMENT '当前商品库存量',
    `min_stock`     INT            DEFAULT NULL COMMENT '商品的最低库存警戒值',
    `status`        VARCHAR(50)    DEFAULT NULL COMMENT '商品状态，如上架、下架等',
    `create_time`   DATETIME       DEFAULT NULL COMMENT '商品信息的创建时间',
    `update_time`   DATETIME       DEFAULT NULL COMMENT '商品信息的最后更新时间',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='商品表';

INSERT INTO `t_product` (`sku`, `name`, `category`, `specification`, `price`, `stock`, `min_stock`, `status`, `create_time`, `update_time`)
VALUES ('BMW-X5-2023', '宝马 X5', 'SUV', '2023款 xDrive40i 尊享型', 569800.00, 15, 5, '上架', '2023-05-01 10:00:00', '2023-05-01 10:00:00');

INSERT INTO `t_product` (`sku`, `name`, `category`, `specification`, `price`, `stock`, `min_stock`, `status`, `create_time`, `update_time`)
VALUES ('BMW-X3-2023', '宝马 X3', 'SUV', '2023款 xDrive30i M运动套装', 399900.00, 20, 8, '上架', '2023-05-01 10:15:00', '2023-05-01 10:15:00');

INSERT INTO `t_product` (`sku`, `name`, `category`, `specification`, `price`, `stock`, `min_stock`, `status`, `create_time`, `update_time`)
VALUES ('BMW-3-2023', '宝马 3系', '轿车', '2023款 330i M运动套装', 329800.00, 25, 10, '上架', '2023-05-02 09:30:00', '2023-05-02 09:30:00');

INSERT INTO `t_product` (`sku`, `name`, `category`, `specification`, `price`, `stock`, `min_stock`, `status`, `create_time`, `update_time`)
VALUES ('BMW-5-2023', '宝马 5系', '轿车', '2023款 530Li 行政型', 459800.00, 18, 7, '上架', '2023-05-02 10:45:00', '2023-05-02 10:45:00');

INSERT INTO `t_product` (`sku`, `name`, `category`, `specification`, `price`, `stock`, `min_stock`, `status`, `create_time`, `update_time`)
VALUES ('AUDI-A4-2023', '奥迪 A4L', '轿车', '2023款 40 TFSI 时尚型', 309800.00, 22, 8, '上架', '2023-05-03 11:00:00', '2023-05-03 11:00:00');

INSERT INTO `t_product` (`sku`, `name`, `category`, `specification`, `price`, `stock`, `min_stock`, `status`, `create_time`, `update_time`)
VALUES ('AUDI-Q5-2023', '奥迪 Q5L', 'SUV', '2023款 40 TFSI 荣享时尚型', 399800.00, 16, 6, '上架', '2023-05-03 14:20:00', '2023-05-03 14:20:00');

INSERT INTO `t_product` (`sku`, `name`, `category`, `specification`, `price`, `stock`, `min_stock`, `status`, `create_time`, `update_time`)
VALUES ('AUDI-A6-2023', '奥迪 A6L', '轿车', '2023款 45 TFSI 豪华型', 469800.00, 14, 5, '上架', '2023-05-04 09:15:00', '2023-05-04 09:15:00');

INSERT INTO `t_product` (`sku`, `name`, `category`, `specification`, `price`, `stock`, `min_stock`, `status`, `create_time`, `update_time`)
VALUES ('BENZ-C-2023', '奔驰 C级', '轿车', '2023款 C 260 L 运动版', 339800.00, 20, 8, '上架', '2023-05-04 10:30:00', '2023-05-04 10:30:00');

INSERT INTO `t_product` (`sku`, `name`, `category`, `specification`, `price`, `stock`, `min_stock`, `status`, `create_time`, `update_time`)
VALUES ('BENZ-E-2023', '奔驰 E级', '轿车', '2023款 E 300 L 运动豪华型', 499800.00, 15, 6, '上架', '2023-05-05 11:45:00', '2023-05-05 11:45:00');

INSERT INTO `t_product` (`sku`, `name`, `category`, `specification`, `price`, `stock`, `min_stock`, `status`, `create_time`, `update_time`)
VALUES ('BENZ-GLC-2023', '奔驰 GLC', 'SUV', '2023款 GLC 300 L 4MATIC', 479800.00, 18, 7, '上架', '2023-05-05 13:00:00', '2023-05-05 13:00:00');

INSERT INTO `t_product` (`sku`, `name`, `category`, `specification`, `price`, `stock`, `min_stock`, `status`, `create_time`, `update_time`)
VALUES ('LEXUS-ES-2023', '雷克萨斯 ES', '轿车', '2023款 ES 300h 豪华版', 359800.00, 22, 10, '上架', '2023-05-06 09:00:00', '2023-05-06 09:00:00');

INSERT INTO `t_product` (`sku`, `name`, `category`, `specification`, `price`, `stock`, `min_stock`, `status`, `create_time`, `update_time`)
VALUES ('LEXUS-NX-2023', '雷克萨斯 NX', 'SUV', '2023款 NX 300h 锋尚版', 399800.00, 17, 8, '上架', '2023-05-06 10:30:00', '2023-05-06 10:30:00');

INSERT INTO `t_product` (`sku`, `name`, `category`, `specification`, `price`, `stock`, `min_stock`, `status`, `create_time`, `update_time`)
VALUES ('TOYOTA-CAMRY-2023', '丰田 凯美瑞', '轿车', '2023款 2.5L 豪华版', 219800.00, 30, 12, '上架', '2023-05-07 09:15:00', '2023-05-07 09:15:00');

INSERT INTO `t_product` (`sku`, `name`, `category`, `specification`, `price`, `stock`, `min_stock`, `status`, `create_time`, `update_time`)
VALUES ('TOYOTA-RAV4-2023', '丰田 RAV4', 'SUV', '2023款 2.0L 豪华版', 209800.00, 25, 10, '上架', '2023-05-07 11:00:00', '2023-05-07 11:00:00');

INSERT INTO `t_product` (`sku`, `name`, `category`, `specification`, `price`, `stock`, `min_stock`, `status`, `create_time`, `update_time`)
VALUES ('HONDA-ACCORD-2023', '本田 雅阁', '轿车', '2023款 2.0L 锐·豪华版', 199800.00, 28, 12, '上架', '2023-05-08 10:00:00', '2023-05-08 10:00:00');

INSERT INTO `t_product` (`sku`, `name`, `category`, `specification`, `price`, `stock`, `min_stock`, `status`, `create_time`, `update_time`)
VALUES ('HONDA-CRV-2023', '本田 CR-V', 'SUV', '2023款 240TURBO CVT 两驱都市版', 199800.00, 22, 9, '上架', '2023-05-08 14:00:00', '2023-05-08 14:00:00');

INSERT INTO `t_product` (`sku`, `name`, `category`, `specification`, `price`, `stock`, `min_stock`, `status`, `create_time`, `update_time`)
VALUES ('NISSAN-TEANA-2023', '日产 天籁', '轿车', '2023款 2.0L XL 舒适版', 189800.00, 24, 10, '上架', '2023-05-09 09:30:00', '2023-05-09 09:30:00');

INSERT INTO `t_product` (`sku`, `name`, `category`, `specification`, `price`, `stock`, `min_stock`, `status`, `create_time`, `update_time`)
VALUES ('NISSAN-XTRAIL-2023', '日产 奇骏', 'SUV', '2023款 2.0L CVT 舒适版', 179800.00, 20, 8, '上架', '2023-05-09 11:15:00', '2023-05-09 11:15:00');

INSERT INTO `t_product` (`sku`, `name`, `category`, `specification`, `price`, `stock`, `min_stock`, `status`, `create_time`, `update_time`)
VALUES ('VOLVO-S90-2023', '沃尔沃 S90', '轿车', '2023款 T5 智逸豪华版', 389800.00, 16, 6, '上架', '2023-05-10 09:45:00', '2023-05-10 09:45:00');

INSERT INTO `t_product` (`sku`, `name`, `category`, `specification`, `price`, `stock`, `min_stock`, `status`, `create_time`, `update_time`)
VALUES ('VOLVO-XC60-2023', '沃尔沃 XC60', 'SUV', '2023款 T5 智逸豪华版', 339800.00, 18, 7, '上架', '2023-05-10 11:30:00', '2023-05-10 11:30:00');

INSERT INTO `t_product` (`sku`, `name`, `category`, `specification`, `price`, `stock`, `min_stock`, `status`, `create_time`, `update_time`)
VALUES ('TESLA-MODEL3-2023', '特斯拉 Model 3', '电动轿车', '2023款 后轮驱动版', 259900.00, 30, 15, '上架', '2023-05-11 10:00:00', '2023-05-11 10:00:00');

INSERT INTO `t_product` (`sku`, `name`, `category`, `specification`, `price`, `stock`, `min_stock`, `status`, `create_time`, `update_time`)
VALUES ('TESLA-MODELY-2023', '特斯拉 Model Y', '电动SUV', '2023款 后轮驱动版', 299900.00, 25, 12, '上架', '2023-05-11 13:45:00', '2023-05-11 13:45:00');

INSERT INTO `t_product` (`sku`, `name`, `category`, `specification`, `price`, `stock`, `min_stock`, `status`, `create_time`, `update_time`)
VALUES ('BYD-HAN-2023', '比亚迪 汉', '电动轿车', '2023款 EV 尊贵型', 219800.00, 32, 15, '上架', '2023-05-12 09:15:00', '2023-05-12 09:15:00');

INSERT INTO `t_product` (`sku`, `name`, `category`, `specification`, `price`, `stock`, `min_stock`, `status`, `create_time`, `update_time`)
VALUES ('BYD-TANG-2023', '比亚迪 唐', '电动SUV', '2023款 EV 四驱尊贵型', 279800.00, 28, 12, '上架', '2023-05-12 11:00:00', '2023-05-12 11:00:00');

INSERT INTO `t_product` (`sku`, `name`, `category`, `specification`, `price`, `stock`, `min_stock`, `status`, `create_time`, `update_time`)
VALUES ('XPENG-P7-2023', '小鹏 P7', '电动轿车', '2023款 后驱长续航智享版', 249900.00, 24, 10, '上架', '2023-05-13 10:30:00', '2023-05-13 10:30:00');

INSERT INTO `t_product` (`sku`, `name`, `category`, `specification`, `price`, `stock`, `min_stock`, `status`, `create_time`, `update_time`)
VALUES ('NIO-ES6-2023', '蔚来 ES6', '电动SUV', '2023款 100kWh 首发纪念版', 448000.00, 15, 6, '上架', '2023-05-13 14:15:00', '2023-05-13 14:15:00');

INSERT INTO `t_product` (`sku`, `name`, `category`, `specification`, `price`, `stock`, `min_stock`, `status`, `create_time`, `update_time`)
VALUES ('PORSCHE-911-2023', '保时捷 911', '跑车', '2023款 Carrera 4S', 1458000.00, 8, 3, '上架', '2023-05-14 09:00:00', '2023-05-14 09:00:00');

INSERT INTO `t_product` (`sku`, `name`, `category`, `specification`, `price`, `stock`, `min_stock`, `status`, `create_time`, `update_time`)
VALUES ('PORSCHE-CAYENNE-2023', '保时捷 卡宴', 'SUV', '2023款 Cayenne S', 928000.00, 10, 4, '上架', '2023-05-14 11:30:00', '2023-05-14 11:30:00');

INSERT INTO `t_product` (`sku`, `name`, `category`, `specification`, `price`, `stock`, `min_stock`, `status`, `create_time`, `update_time`)
VALUES ('MASERATI-GT-2023', '玛莎拉蒂 GranTurismo', '跑车', '2023款 MC Stradale', 1680000.00, 5, 2, '上架', '2023-05-15 10:00:00', '2023-05-15 10:00:00');

INSERT INTO `t_product` (`sku`, `name`, `category`, `specification`, `price`, `stock`, `min_stock`, `status`, `create_time`, `update_time`)
VALUES ('FERRARI-F8-2023', '法拉利 F8', '跑车', '2023款 Tributo', 2738000.00, 3, 1, '上架', '2023-05-15 13:45:00', '2023-05-15 13:45:00');
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
-- Table structure for t_product_promotion
-- ----------------------------
DROP TABLE IF EXISTS `t_product_promotion`;
CREATE TABLE `t_product_promotion`
(
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '促销活动的唯一标识符',
    `name`        VARCHAR(255) NOT NULL COMMENT '促销名称',
    `type`        VARCHAR(50)    DEFAULT NULL COMMENT '促销类型',
    `discount`    DECIMAL(10, 2) DEFAULT NULL COMMENT '折扣或金额',
    `start_time`  DATETIME       DEFAULT NULL COMMENT '促销开始时间',
    `end_time`    DATETIME       DEFAULT NULL COMMENT '促销结束时间',
    `status`      VARCHAR(50)    DEFAULT NULL COMMENT '促销状态',
    `create_time` DATETIME       DEFAULT NULL COMMENT '创建时间',
    `update_time` DATETIME       DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='产品促销表';

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
    -- CONSTRAINT `fk_product_stock_record_product` FOREIGN KEY (`product_id`) REFERENCES `t_product` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='产品库存记录表';

-- ----------------------------
-- Records for t_product
-- ----------------------------
INSERT INTO `t_product` (`id`, `sku`, `name`, `category`, `specification`, `price`, `stock`, `min_stock`, `status`,
                         `create_time`, `update_time`)
VALUES (1, 'SKU001', 'Laptop', 'Electronics', '15-inch, 8GB RAM', 5999.00, 50, 10, '上架', NOW(), NOW()),
       (2, 'SKU002', 'Smartphone', 'Electronics', '6.5-inch, 128GB', 2999.50, 150, 20, '上架', NOW(), NOW()),
       (3, 'SKU003', 'T-Shirt (Red)', 'Apparel', 'Large', 99.00, 5, 10, '上架', NOW(), NOW()),
       (4, 'SKU004', 'Jeans (Blue)', 'Apparel', 'Size 32', 299.80, 8, 5, '上架', NOW(), NOW()),
       (5, 'SKU005', 'Coffee Beans 500g', 'Food', 'Medium Roast', 88.50, 30, 5, '上架', NOW(), NOW()),
       (6, 'SKU006', 'Energy Drink', 'Food', '500ml', 8.00, 200, 50, '上架', NOW(), NOW()),
       (7, 'SKU007', 'Tablet', 'Electronics', '10-inch, 64GB', 1999.00, 12, 10, '上架', NOW(), NOW()),
       (8, 'SKU008', 'Headphones', 'Electronics', 'Wireless', 450.00, 70, 15, '上架', NOW(), NOW()),
       (9, 'SKU009', 'Dress Shirt', 'Apparel', 'Medium, White', 180.00, 10, 5, '上架', NOW(), NOW()),
       (10, 'SKU010', 'Sneakers', 'Apparel', 'Size 43', 550.00, 22, 8, '上架', NOW(), NOW()),
       (11, 'SKU011', 'Chocolate Bar', 'Food', 'Milk Chocolate', 5.50, 300, 100, '上架', NOW(), NOW()),
       (12, 'SKU012', 'Pasta (Spaghetti)', 'Food', '500g', 12.00, 80, 20, '上架', NOW(), NOW()),
       (13, 'SKU013', 'Smartwatch', 'Electronics', 'Series 5', 1200.00, 18, 5, '上架', NOW(), NOW()),
       (14, 'SKU014', 'Sweater (Grey)', 'Apparel', 'Small', 350.00, 4, 5, '上架', NOW(), NOW()),
       (15, 'SKU015', 'Bottled Water', 'Food', '1L', 3.00, 500, 200, '下架', NOW(), NOW());

-- ----------------------------
-- Records for t_product_category
-- ----------------------------
INSERT INTO `t_product_category` (`id`, `name`, `code`, `description`, `sort`, `status`, `create_time`, `update_time`)
VALUES (1, 'Electronics', 'ELEC', 'Electronic devices and accessories', 1, '启用', NOW(), NOW()),
       (2, 'Apparel', 'APPA', 'Clothing and fashion items', 2, '启用', NOW(), NOW()),
       (3, 'Food', 'FOOD', 'Groceries and consumables', 3, '启用', NOW(), NOW()),
       (4, 'Books', 'BOOK', 'Printed and digital books', 4, '启用', NOW(), NOW()),
       (5, 'Home Appliances', 'HOME', 'Kitchen and household appliances', 5, '启用', NOW(), NOW()),
       (6, 'Sports & Outdoors', 'SPRT', 'Equipment and gear for sports', 6, '启用', NOW(), NOW()),
       (7, 'Beauty & Personal Care', 'BEAU', 'Cosmetics and personal hygiene', 7, '启用', NOW(), NOW()),
       (8, 'Toys & Games', 'TOYS', 'Children\'s toys and adult games', 8, '启用', NOW(), NOW()),
       (9, 'Automotive', 'AUTO', 'Car parts and accessories', 9, '启用', NOW(), NOW()),
       (10, 'Health', 'HEAL', 'Health and wellness products', 10, '启用', NOW(), NOW()),
       (11, 'Pet Supplies', 'PETS', 'Products for pets', 11, '启用', NOW(), NOW()),
       (12, 'Office Supplies', 'OFFC', 'Stationery and office equipment', 12, '启用', NOW(), NOW()),
       (13, 'Garden & Outdoor', 'GARD', 'Gardening tools and outdoor items', 13, '启用', NOW(), NOW()),
       (14, 'Jewelry', 'JEWL', 'Necklaces, rings, and other jewelry', 14, '禁用', NOW(), NOW()),
       (15, 'Art Supplies', 'ARTS', 'Materials for artists', 15, '启用', NOW(), NOW());

-- ----------------------------
-- Records for t_product_promotion
-- ----------------------------
INSERT INTO `t_product_promotion` (`id`, `name`, `type`, `discount`, `start_time`, `end_time`, `status`, `create_time`,
                                   `update_time`)
VALUES (1, 'Summer Sale', '折扣', 8.50, '2023-07-01 00:00:00', '2023-07-31 23:59:59', '已结束', NOW(), NOW()),
       (2, 'Holiday Discount', '满减', 50.00, '2023-12-01 00:00:00', '2023-12-25 23:59:59', '已结束', NOW(), NOW()),
       (3, 'Flash Deal', '直降', 20.00, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 1 DAY), '进行中',
        NOW(), NOW()),
       (4, 'Back to School', '折扣', 9.00, DATE_ADD(NOW(), INTERVAL 7 DAY), DATE_ADD(NOW(), INTERVAL 30 DAY), '未开始',
        NOW(), NOW()),
       (5, 'Weekend Special', '满减', 25.00, DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY), '已结束',
        NOW(), NOW()),
       (6, 'Clearance', '直降', 10.00, DATE_SUB(NOW(), INTERVAL 10 DAY), DATE_ADD(NOW(), INTERVAL 5 DAY), '进行中',
        NOW(), NOW()),
       (7, 'Early Bird', '折扣', 9.50, DATE_ADD(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 3 DAY), '未开始',
        NOW(), NOW()),
       (8, 'Spring Offer', '满减', 30.00, '2024-03-01 00:00:00', '2024-03-31 23:59:59', '已结束', NOW(), NOW()),
       (9, 'Autumn Promo', '折扣', 8.00, '2024-09-01 00:00:00', '2024-09-30 23:59:59', '未开始', NOW(), NOW()),
       (10, 'New Year Deal', '直降', 15.00, '2024-01-01 00:00:00', '2024-01-15 23:59:59', '已结束', NOW(), NOW()),
       (11, 'Loyalty Bonus', '满减', 100.00, DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_ADD(NOW(), INTERVAL 10 DAY),
        '进行中', NOW(), NOW()),
       (12, 'Referral Special', '折扣', 7.50, DATE_ADD(NOW(), INTERVAL 15 DAY), DATE_ADD(NOW(), INTERVAL 45 DAY),
        '未开始', NOW(), NOW()),
       (13, 'Mega Savings', '直降', 50.00, DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_ADD(NOW(), INTERVAL 2 DAY), '进行中',
        NOW(), NOW()),
       (14, 'Limited Time Offer', '折扣', 8.80, DATE_ADD(NOW(), INTERVAL 2 DAY), DATE_ADD(NOW(), INTERVAL 5 DAY),
        '未开始', NOW(), NOW()),
       (15, 'Seasonal Discount', '满减', 75.00, '2024-06-01 00:00:00', '2024-08-31 23:59:59', '未开始', NOW(), NOW());

-- ----------------------------
-- Records for t_product_stock_record
-- ----------------------------
INSERT INTO `t_product_stock_record` (`id`, `product_id`, `quantity`, `type`, `remark`, `create_time`)
VALUES (1, 1, 100, '入库', 'Initial stock', NOW()),
       (2, 2, 200, '入库', 'Initial stock', NOW()),
       (3, 3, 50, '入库', 'Initial stock', NOW()),
       (4, 4, 30, '入库', 'Initial stock', NOW()),
       (5, 5, 100, '入库', 'Initial stock', NOW()),
       (6, 6, 500, '入库', 'Initial stock', NOW()),
       (7, 1, -20, '出库', 'Sold', DATE_ADD(NOW(), INTERVAL 1 HOUR)),
       (8, 2, -30, '出库', 'Sold', DATE_ADD(NOW(), INTERVAL 2 HOUR)),
       (9, 3, -45, '出库', 'Sold', DATE_ADD(NOW(), INTERVAL 3 HOUR)),
       (10, 4, -22, '出库', 'Sold', DATE_ADD(NOW(), INTERVAL 4 HOUR)),
       (11, 5, -70, '出库', 'Sold', DATE_ADD(NOW(), INTERVAL 5 HOUR)),
       (12, 1, 50, '入库', 'Restock', DATE_ADD(NOW(), INTERVAL 6 HOUR)),
       (13, 2, 80, '入库', 'Restock', DATE_ADD(NOW(), INTERVAL 7 HOUR)),
       (14, 7, 50, '入库', 'Initial stock', NOW()),
       (15, 14, 10, '入库', 'Restock', DATE_ADD(NOW(), INTERVAL 8 HOUR));

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
    INDEX `idx_tran_id` (`tran_id` ASC) USING BTREE,
    INDEX `idx_approve_by` (`approve_by` ASC) USING BTREE
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  CHARACTER SET = utf8mb3
  COLLATE = utf8mb3_general_ci COMMENT = '交易审批表'
  ROW_FORMAT = DYNAMIC;
