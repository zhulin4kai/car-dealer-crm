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
INSERT INTO `t_activity` (id, owner_id, name, start_time, end_time, cost, description, create_time, create_by, edit_time, edit_by)
VALUES 
(1, 1, '春季汽车展销会', '2025-03-15 09:00:00', '2025-03-17 18:00:00', 50000.00, '春季大型汽车展销活动，展示各品牌新车型', '2025-03-01 10:00:00', 1, '2025-03-10 14:30:00', 1),
(2, 2, '豪华车试驾体验', '2025-04-01 10:00:00', '2025-04-30 17:00:00', 30000.00, '提供豪华车型免费试驾服务，吸引潜在客户', '2025-03-20 09:15:00', 2, NULL, NULL),
(3, 1, '夏季购车优惠节', '2025-06-01 09:00:00', '2025-06-30 20:00:00', 80000.00, '夏季购车大优惠，多款车型享受特价和金融方案', '2025-05-15 11:20:00', 1, '2025-05-25 16:45:00', 3),
(4, 3, '新能源汽车推广', '2025-05-10 08:30:00', '2025-07-10 19:00:00', 60000.00, '推广新能源汽车，环保出行理念宣传活动', '2025-04-28 14:10:00', 3, NULL, NULL),
(5, 2, '金秋购车嘉年华', '2025-09-01 09:00:00', '2025-09-30 18:00:00', 75000.00, '金秋购车季，多重优惠叠加，限时抢购活动', '2025-08-15 10:30:00', 2, '2025-08-20 13:25:00', 1),
(6, 1, '年末清仓大促销', '2025-11-15 10:00:00', '2025-12-31 17:00:00', 100000.00, '年末库存清理，超低价格销售，买车送大礼包', '2025-11-01 09:00:00', 1, NULL, NULL),
(7, 3, '元旦新春购车节', '2025-12-25 09:00:00', '2026-01-15 18:00:00', 90000.00, '跨年购车活动，新年新车新气象，特惠价格限时抢购', '2025-12-10 15:40:00', 3, NULL, NULL),
(8, 2, 'VIP客户专享日', '2025-07-20 14:00:00', '2025-07-20 20:00:00', 25000.00, 'VIP客户专属活动，高端车型展示和一对一服务', '2025-07-10 11:15:00', 2, '2025-07-15 09:20:00', 2),
(9, 1, '周年庆典活动', '2025-08-08 09:00:00', '2025-08-10 21:00:00', 120000.00, '公司成立周年庆典，全场车型大优惠，抽奖送豪礼', '2025-07-25 10:45:00', 1, NULL, NULL),
(10, 3, '学生购车专场', '2025-06-15 10:00:00', '2025-08-31 17:00:00', 40000.00, '面向应届毕业生的购车优惠活动，提供学生专享价格', '2025-06-01 08:30:00', 3, '2025-06-10 14:15:00', 1);
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
INSERT INTO `t_activity_remark` (id, activity_id, note_content, create_time, create_by, edit_time, edit_by, deleted)
VALUES 
(1, 1, '活动场地已确认，位于市中心展览馆A区，面积5000平方米', '2025-03-02 10:30:00', 1, NULL, NULL, 0),
(2, 1, '已联系15家汽车品牌参展，预计展示车辆超过80台', '2025-03-05 14:15:00', 1, '2025-03-08 09:20:00', 1, 0),
(3, 1, '媒体宣传方案已制定，包括电视、网络、户外广告投放', '2025-03-08 16:45:00', 2, NULL, NULL, 0),
(4, 2, '试驾路线规划完成，包含城市道路和高速公路体验段', '2025-03-22 11:20:00', 2, NULL, NULL, 0),
(5, 2, '已培训专业试驾教练10名，确保客户安全体验', '2025-03-25 09:45:00', 3, '2025-03-28 15:30:00', 2, 0),
(6, 3, '夏季优惠政策已获总部批准，最高优惠可达8万元', '2025-05-18 10:15:00', 1, NULL, NULL, 0),
(7, 3, '金融方案已与银行谈妥，提供0息贷款和低首付方案', '2025-05-20 14:30:00', 3, NULL, NULL, 0),
(8, 3, '活动期间增设临时展厅，扩大展示面积至8000平方米', '2025-05-22 16:20:00', 1, '2025-05-26 11:10:00', 1, 0),
(9, 4, '新能源车型补贴政策解读材料已准备完毕', '2025-05-01 09:30:00', 3, NULL, NULL, 0),
(10, 4, '充电桩体验区设置完成，可同时为20台车辆充电演示', '2025-05-05 13:45:00', 2, NULL, NULL, 0),
(11, 4, '环保主题宣传册已印制10000份，准备现场发放', '2025-05-08 11:25:00', 3, '2025-05-12 14:50:00', 3, 0),
(12, 5, '秋季购车节logo设计已完成，开始制作宣传物料', '2025-08-18 10:00:00', 2, NULL, NULL, 0),
(13, 5, '合作商家优惠券已印制，包含餐饮、娱乐等配套服务', '2025-08-20 15:20:00', 1, NULL, NULL, 0),
(14, 6, '库存车辆清单已整理完成，共计158台各类车型', '2025-11-03 09:15:00', 1, '2025-11-05 16:40:00', 1, 0),
(15, 6, '大礼包内容确定：行车记录仪、车载香薰、保养券等', '2025-11-08 14:25:00', 2, NULL, NULL, 0),
(16, 7, '跨年活动现场布置方案已设计，突出新年喜庆氛围', '2025-12-12 11:30:00', 3, NULL, NULL, 0),
(17, 8, 'VIP客户名单已确认，共邀请50位高端客户参与', '2025-07-12 10:45:00', 2, '2025-07-16 13:20:00', 2, 0),
(18, 8, '红酒品鉴环节已安排，提升活动档次和客户体验', '2025-07-15 16:10:00', 1, NULL, NULL, 0),
(19, 9, '周年庆典节目单已确定，包含歌舞表演和抽奖环节', '2025-07-28 09:20:00', 1, NULL, NULL, 0),
(20, 9, '豪礼清单：iPhone、平板电脑、品牌手表等，总价值20万', '2025-07-30 14:35:00', 3, '2025-08-02 10:15:00', 1, 0),
(21, 10, '学生证验证流程已制定，确保优惠政策准确执行', '2025-06-03 11:40:00', 3, NULL, NULL, 0),
(22, 10, '校园推广计划启动，已联系5所大学进行宣传', '2025-06-05 15:50:00', 2, NULL, NULL, 0),
(23, 1, '活动首日参观人数达5000人次，超出预期', '2025-03-15 20:30:00', 1, NULL, NULL, 0),
(24, 2, '试驾活动客户满意度达95%，获得良好口碑', '2025-04-15 17:20:00', 2, NULL, NULL, 0),
(25, 3, '活动期间共成交车辆126台，销售额突破3000万', '2025-06-30 18:45:00', 1, NULL, NULL, 0);
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
INSERT INTO `t_clue` (id, owner_id, activity_id, full_name, appellation, phone, weixin, qq, email, age, job, year_income, address, need_loan, intention_state, intention_product, state, source, description, next_contact_time, create_time, create_by, edit_time, edit_by)
VALUES 
(1, 1, 2, '王杰', 11, '13700000000', 'wx_wangjie', '123456789', 'wangjie@163.com', 32, '软件工程师', 300000.00, '北京市朝阳区建国路88号', 49, 46, 3, 6, 2, '对宝马X5感兴趣，计划近期看车', '2025-06-25 10:00:00', '2025-06-17 09:30:00', 1, '2025-06-18 14:20:00', 1),
(2, 2, 7, '李娜', 20, '13800000001', 'wx_lina', '987654321', 'lina@gmail.com', 28, '金融分析师', 400000.00, '上海市浦东新区陆家嘴金融中心', 50, 47, 5, 10, 3, '想购买豪华轿车，预算充足', '2025-06-27 15:30:00', '2025-06-16 11:40:00', 2, NULL, NULL),
(3, 3, 8, '张伟', 18, '13900000002', 'wx_zhangwei', '456789123', 'zhangwei@outlook.com', 35, '企业高管', 600000.00, '广州市天河区珠江新城', 49, 48, 9, 6, 44, '想要购买奔驰E级作为商务用车', '2025-06-23 09:15:00', '2025-06-15 16:20:00', 1, '2025-06-17 10:05:00', 1),
(4, 1, 10, '刘芳', 41, '13700000003', 'wx_liufang', '135792468', 'liufang@qq.com', 26, '医生', 350000.00, '深圳市南山区科技园', 49, 46, 12, 24, 33, '对雷克萨斯NX感兴趣，周末有时间看车', '2025-06-21 14:00:00', '2025-06-14 08:50:00', 2, NULL, NULL),
(5, 2, 13, '陈明', 18, '13800000004', 'wx_chenming', '246813579', 'chenming@126.com', 42, '大学教授', 450000.00, '成都市高新区天府大道', 50, 47, 8, -1, 2, '已转为客户，购买了奥迪Q5', '2025-06-10 11:30:00', '2025-06-01 09:20:00', 3, '2025-06-15 16:40:00', 1),
(6, 3, 15, '赵静', 20, '13900000005', 'wx_zhaojing', '369258147', 'zhaojing@foxmail.com', 30, '建筑师', 320000.00, '武汉市江汉区解放大道', 50, 48, 6, 30, 3, '对奥迪A6L有兴趣，需要详细了解配置', '2025-06-24 16:45:00', '2025-06-18 09:10:00', 1, NULL, NULL),
(7, 1, 16, '黄强', 18, '13700000006', 'wx_huangqiang', '147258369', 'huangqiang@sina.com', 38, '律师', 520000.00, '南京市鼓楼区中山北路', 49, 46, 4, 10, 44, '想购买宝马5系作为家用车', '2025-06-26 10:30:00', '2025-06-12 14:25:00', 2, '2025-06-16 11:15:00', 2),
(8, 2, 19, '郑丽', 41, '13800000007', 'wx_zhengli', '258369147', 'zhengli@hotmail.com', 29, '市场经理', 380000.00, '杭州市西湖区文三路', 50, 47, 11, 6, 2, '想换一辆更大空间的SUV', '2025-06-22 13:20:00', '2025-06-13 16:30:00', 3, NULL, NULL),
(9, 3, 22, '孙宇', 11, '13900000008', 'wx_sunyu', '369147258', 'sunyu@163.com', 45, '公司CEO', 800000.00, '重庆市渝中区解放碑', 49, 48, 10, 24, 33, '对高端豪华车感兴趣', '2025-06-20 11:00:00', '2025-06-10 10:45:00', 1, '2025-06-15 09:30:00', 3),
(10, 1, 23, '吴佳', 20, '13700000009', 'wx_wujia', '159357246', 'wujia@qq.com', 32, '设计师', 290000.00, '西安市雁塔区高新路', 50, 46, 7, 10, 3, '想找一辆适合城市通勤的车', '2025-06-25 09:40:00', '2025-06-11 13:50:00', 2, NULL, NULL);

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
INSERT INTO `t_clue_remark` (id, clue_id, note_way, note_content, create_time, create_by, edit_time, edit_by, deleted)
VALUES 
(1, 1, 61, '首次电话联系，客户表示对宝马X5很感兴趣，询问了详细配置信息', '2025-06-17 10:30:00', 1, NULL, NULL, 0),
(2, 1, 62, '通过微信发送了宝马X5的产品资料和价格表', '2025-06-17 15:20:00', 1, NULL, NULL, 0),
(3, 1, 61, '二次电话跟进，客户考虑中，计划本周末到店看车', '2025-06-18 09:15:00', 1, NULL, NULL, 0),
(4, 2, 61, '电话联系，客户工作繁忙，约定下周三面谈', '2025-06-16 14:30:00', 2, NULL, NULL, 0),
(5, 2, 64, '客户到店咨询，试驾了几款豪华轿车，表现出强烈购买意向', '2025-06-19 16:45:00', 2, NULL, NULL, 0),
(6, 3, 62, '微信沟通，客户需要商务用车，对奔驰E级比较满意', '2025-06-15 17:20:00', 1, NULL, NULL, 0),
(7, 3, 61, '电话确认客户需求，准备个性化方案', '2025-06-17 11:30:00', 1, NULL, NULL, 0),
(8, 3, 64, '面谈详细介绍奔驰E级商务版配置，客户很满意', '2025-06-18 14:00:00', 1, '2025-06-18 16:30:00', 1, 0),
(9, 4, 61, '电话联系，客户是医生，工作时间较忙，建议微信沟通', '2025-06-14 10:20:00', 2, NULL, NULL, 0),
(10, 4, 62, '微信发送雷克萨斯NX详细资料，客户表示周末有时间看车', '2025-06-14 19:30:00', 2, NULL, NULL, 0),
(11, 4, 61, '电话预约周六看车时间，客户确认下午2点到店', '2025-06-20 09:45:00', 2, NULL, NULL, 0),
(12, 5, 61, '电话沟通，客户已决定购买奥迪Q5，讨论具体配置', '2025-06-01 10:15:00', 3, NULL, NULL, 0),
(13, 5, 64, '客户到店签约，成功转化为正式客户', '2025-06-10 14:20:00', 3, NULL, NULL, 0),
(14, 6, 62, '微信联系，发送奥迪A6L配置对比表', '2025-06-18 11:10:00', 1, NULL, NULL, 0),
(15, 6, 61, '电话详细介绍A6L各版本差异，客户需要考虑', '2025-06-19 15:40:00', 1, NULL, NULL, 0),
(16, 7, 61, '电话联系，客户是律师，对宝马5系家用版感兴趣', '2025-06-12 16:30:00', 2, NULL, NULL, 0),
(17, 7, 62, '微信发送宝马5系家庭版详细介绍和优惠信息', '2025-06-13 09:20:00', 2, NULL, NULL, 0),
(18, 7, 64, '客户到店试驾宝马5系，对驾驶体验很满意', '2025-06-16 13:45:00', 2, '2025-06-16 17:10:00', 2, 0),
(19, 8, 61, '电话沟通，客户是市场经理，需要大空间SUV', '2025-06-13 17:50:00', 3, NULL, NULL, 0),
(20, 8, 62, '微信推荐几款热门SUV车型，客户比较关注油耗', '2025-06-14 10:30:00', 3, NULL, NULL, 0),
(21, 9, 64, '面谈交流，客户是CEO，对高端豪华车要求很高', '2025-06-10 11:30:00', 1, NULL, NULL, 0),
(22, 9, 61, '电话跟进，介绍了几款顶配豪华车型', '2025-06-15 14:15:00', 1, NULL, NULL, 0),
(23, 9, 62, '微信发送豪华车专属服务介绍', '2025-06-16 16:20:00', 1, NULL, NULL, 0),
(24, 10, 61, '电话联系，客户是设计师，注重车辆外观和内饰', '2025-06-11 15:30:00', 2, NULL, NULL, 0),
(25, 10, 62, '微信分享了几款时尚车型的图片和视频', '2025-06-12 09:40:00', 2, NULL, NULL, 0);
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
INSERT INTO `t_customer` (id, clue_id, product, description, next_contact_time, create_time, create_by, edit_time, edit_by)
VALUES 
(1, 5, 8, '大学教授，购买奥迪Q5，已签约成功，需要跟进交付和后续服务', '2025-06-25 10:00:00', '2025-06-10 14:30:00', 3, '2025-06-15 16:45:00', 1),
(2, 1, 3, '软件工程师，购买宝马X5，已签约，选择贷款方案，等待提车', '2025-06-30 14:00:00', '2025-06-25 16:15:00', 1, NULL, NULL),
(3, 7, 4, '律师，购买宝马5系，已付定金，等待银行贷款审批', '2025-07-02 09:30:00', '2025-06-28 10:20:00', 2, '2025-06-29 11:40:00', 2),
(4, 3, 9, '企业高管，购买奔驰E级商务版，已签约，选择全款购车', '2025-07-01 15:00:00', '2025-06-26 11:45:00', 1, NULL, NULL),
(5, 2, 5, '金融分析师，购买奥迪A6L，已交定金，讨论配置升级', '2025-07-05 16:30:00', '2025-06-30 09:50:00', 2, '2025-07-01 14:20:00', 2),
(6, 4, 6, '医生，购买雷克萨斯NX，已试驾，考虑贷款购车', '2025-07-08 11:00:00', '2025-07-03 10:10:00', 2, NULL, NULL),
(7, 6, 7, '建筑师，购买奥迪A6L，已签约，等待交付', '2025-07-10 15:30:00', '2025-07-05 09:20:00', 1, NULL, NULL),
(8, 8, 10, '市场经理，购买奔驰GLC，已交定金，等待审批', '2025-07-12 13:00:00', '2025-07-06 14:40:00', 3, NULL, NULL),
(9, 9, 11, '公司CEO，购买奔驰E级，已签约，等待提车', '2025-07-15 10:00:00', '2025-07-08 11:30:00', 1, NULL, NULL),
(10, 10, 12, '设计师，购买宝马3系，已试驾，考虑全款购车', '2025-07-18 16:00:00', '2025-07-10 12:15:00', 2, NULL, NULL),
(11, 11, 13, '教师，购买丰田凯美瑞，已交定金，等待交付', '2025-07-20 09:30:00', '2025-07-12 10:00:00', 1, NULL, NULL);
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
INSERT INTO `t_customer_remark` (id, customer_id, note_way, note_content, create_by, create_time, edit_time, edit_by, deleted)
VALUES 
(1, 1, 61, '客户签约成功，选择奥迪Q5 2.0T豪华版，全款购车', 3, '2025-06-10 14:30:00', NULL, NULL, 0),
(2, 1, 64, '办理车辆登记手续，客户对服务很满意', 3, '2025-06-12 10:15:00', NULL, NULL, 0),
(3, 1, 61, '车辆已交付，安排首保时间，客户反馈驾驶体验良好', 1, '2025-06-15 16:45:00', NULL, NULL, 0),
(4, 1, 62, '微信发送保养提醒和用车小贴士，维护客户关系', 1, '2025-06-20 09:30:00', NULL, NULL, 0),
(5, 2, 61, '客户决定购买宝马X5，讨论贷款方案和配置选择', 1, '2025-06-25 16:15:00', NULL, NULL, 0),
(6, 2, 64, '客户到店签约，选择宝马X5 xDrive30i豪华套装，贷款50万', 1, '2025-06-26 14:30:00', NULL, NULL, 0),
(7, 2, 61, '银行贷款已审批通过，预计下周提车', 1, '2025-06-28 11:20:00', NULL, NULL, 0),
(8, 2, 62, '微信确认提车时间和相关手续，客户很期待', 1, '2025-06-29 15:40:00', NULL, NULL, 0),
(9, 3, 61, '客户确认购买宝马5系，讨论具体配置和颜色选择', 2, '2025-06-28 10:20:00', NULL, NULL, 0),
(10, 3, 64, '客户到店签约并付定金5万元，选择珠光白外观', 2, '2025-06-28 16:45:00', NULL, NULL, 0),
(11, 3, 61, '协助客户办理银行贷款手续，提交相关材料', 2, '2025-06-29 11:40:00', '2025-06-29 14:20:00', 2, 0),
(12, 3, 62, '微信通知银行审批进度，预计本周完成审批', 2, '2025-07-01 09:15:00', NULL, NULL, 0),
(13, 4, 62, '微信确认客户购车意向，介绍奔驰E级最新优惠政策', 1, '2025-06-26 11:45:00', NULL, NULL, 0),
(14, 4, 64, '客户到店签约，选择奔驰E级商务版，全款购车', 1, '2025-06-26 15:30:00', NULL, NULL, 0),
(15, 4, 61, '车辆调配中，预计10天内到店，客户表示满意', 1, '2025-06-27 10:00:00', NULL, NULL, 0),
(16, 4, 62, '微信发送车辆到店通知，安排交付时间', 1, '2025-06-30 14:30:00', NULL, NULL, 0),
(17, 5, 61, '客户决定购买奥迪A6L，讨论配置和价格', 2, '2025-06-30 09:50:00', NULL, NULL, 0),
(18, 5, 64, '客户到店试驾并交付定金，考虑配置升级', 2, '2025-06-30 16:20:00', NULL, NULL, 0),
(19, 5, 61, '电话沟通配置升级方案，客户同意增加运动套装', 2, '2025-07-01 14:20:00', NULL, NULL, 0),
(20, 5, 62, '微信发送升级后的配置清单和总价，等待客户确认', 2, '2025-07-02 10:45:00', NULL, NULL, 0),
(21, 1, 61, '客户推荐朋友来看车，已安排专人接待', 1, '2025-06-25 11:30:00', NULL, NULL, 0),
(22, 2, 61, '提车当天，客户对车辆和服务都很满意，给予好评', 1, '2025-07-03 16:00:00', NULL, NULL, 0),
(23, 3, 64, '银行审批通过，客户到店完成贷款手续', 2, '2025-07-05 14:15:00', NULL, NULL, 0),
(24, 4, 64, '车辆交付完成，客户对奔驰E级的性能表现很满意', 1, '2025-07-05 10:30:00', NULL, NULL, 0),
(25, 5, 61, '最终确认配置，客户签署补充协议，预计下周提车', 2, '2025-07-08 09:20:00', NULL, NULL, 0);

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
    INDEX `t_dic_value_ibfk_1` (`type_code` ASC) USING BTREE
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
VALUES (37, '字典类型-列表', 'dict/type:list', NULL, 'button', 36, NULL, NULL);
INSERT INTO `t_permission`
VALUES (38, '字典类型-录入', 'dict/type:add', NULL, 'button', 36, NULL, NULL);
INSERT INTO `t_permission`
VALUES (39, '字典类型-编辑', 'dict/type:edit', NULL, 'button', 36, NULL, NULL);
INSERT INTO `t_permission`
VALUES (40, '字典类型-查看', 'dict/type:view', NULL, 'button', 36, NULL, NULL);
INSERT INTO `t_permission`
VALUES (41, '字典类型-删除', 'dict/type:delete', NULL, 'button', 36, NULL, NULL);
INSERT INTO `t_permission`
VALUES (42, '字典数据', '', '/dashboard/dict/value', 'menu', 35, 2, 'DataAnalysis');
INSERT INTO `t_permission`
VALUES (43, '字典数据-列表', 'dict/value:list', NULL, 'button', 42, NULL, NULL);
INSERT INTO `t_permission`
VALUES (44, '字典数据-录入', 'dict/value:add', NULL, 'button', 42, NULL, NULL);
INSERT INTO `t_permission`
VALUES (45, '字典数据-编辑', 'dict/value:edit', NULL, 'button', 42, NULL, NULL);
INSERT INTO `t_permission`
VALUES (46, '字典数据-查看', 'dict/value:view', NULL, 'button', 42, NULL, NULL);
INSERT INTO `t_permission`
VALUES (47, '字典数据-删除', 'dict/value:delete', NULL, 'button', 42, NULL, NULL);
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
VALUES (4, 'marketing', '市场营销');
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
INSERT INTO `t_system_info` (`system_code`, `name`, `site`, `logo`, `title`, `description`, `keywords`, `shortcuticon`, `tel`, `weixin`, `email`, `address`, `version`, `closeMsg`, `isopen`, `create_time`, `create_by`, `edit_time`, `edit_by`) VALUES
('CarSales_001', '豪华汽车销售系统', 'http://www.luxcars.com', '/logos/luxcars_logo.png', '豪华车销售', '提供高端豪华汽车销售服务', '豪华车,销售,宝马,奔驰,奥迪', '/icons/favicon.ico', '400-123-4567', 'luxcars_official', 'contact@luxcars.com', '中国北京市朝阳区', '1.0.0', '系统维护中，请稍后访问。', 'y', '2024-01-01 10:00:00', 1, NULL, NULL),
('EcoCar_002', '新能源汽车平台', 'http://www.ecocar.com', '/logos/ecocar_logo.png', '新能源汽车', '专注于新能源汽车的销售与服务', '新能源,电动车,特斯拉,蔚来', '/icons/ecocar_favicon.ico', '400-987-6543', 'ecocar_service', 'service@ecocar.com', '中国上海市浦东新区', '2.1.0', '系统升级，预计2小时后恢复。', 'y', '2024-02-15 09:00:00', 1, '2024-05-20 14:00:00', 1),
('UsedCar_003', '认证二手车交易市场', 'http://www.usedcars.com', '/logos/usedcar_logo.png', '二手车交易', '提供经过官方认证的二手车交易服务', '二手车,认证,保值', '/icons/usedcar_favicon.ico', '400-222-3333', 'usedcar_trade', 'trade@usedcars.com', '中国广东省深圳市', '3.0.5', NULL, 'y', '2024-03-10 11:30:00', 2, NULL, NULL),
('TruckSales_004', '商用卡车销售网', 'http://www.trucks.com', '/logos/truck_logo.png', '卡车销售', '重型、轻型卡车销售', '卡车,商用车,货运', '/icons/truck_favicon.ico', '400-444-5555', 'trucksales_info', 'info@trucks.com', '中国山东省济南市', '1.2.0', NULL, 'n', '2024-04-01 08:00:00', 3, '2024-05-01 10:00:00', 3),
('MotoWorld_005', '摩托车世界', 'http://www.motoworld.com', '/logos/moto_logo.png', '摩托车销售', '各类品牌摩托车销售', '摩托车,哈雷,杜卡迪', '/icons/moto_favicon.ico', '400-666-7777', 'motoworld_club', 'club@motoworld.com', '中国重庆市', '1.5.0', NULL, 'y', '2024-05-20 16:00:00', 1, NULL, NULL),
('RVLife_006', '房车生活家', 'http://www.rvlife.com', '/logos/rv_logo.png', '房车销售与租赁', '开启您的房车之旅', '房车,旅行,租赁', '/icons/rv_favicon.ico', '400-888-9999', 'rvlife_journey', 'journey@rvlife.com', '中国四川省成都市', '2.0.0', NULL, 'y', '2024-06-01 12:00:00', 2, NULL, NULL),
('ClassicCar_007', '经典老爷车收藏', 'http://www.classiccars.com', '/logos/classic_logo.png', '老爷车收藏', '珍稀经典老爷车交易平台', '老爷车,经典,收藏', '/icons/classic_favicon.ico', '400-111-0000', 'classiccar_collect', 'collect@classiccars.com', '中国香港', '1.0.1', NULL, 'y', '2024-07-04 18:00:00', 3, '2024-07-10 11:00:00', 1),
('SportCar_008', '超跑俱乐部', 'http://www.supercars.com', '/logos/supercar_logo.png', '超级跑车', '法拉利、兰博基尼等超跑销售', '超跑,法拉利,兰博基尼', '/icons/supercar_favicon.ico', '400-999-8888', 'supercar_club', 'club@supercars.com', '中国澳门', '3.3.0', '网站维护中', 'y', '2024-08-08 08:08:08', 1, NULL, NULL),
('FamilyVan_009', '家庭MPV优选', 'http://www.familyvan.com', '/logos/van_logo.png', '家用MPV', '为家庭出行提供最佳MPV选择', 'MPV,家用,七座车', '/icons/van_favicon.ico', '400-321-7654', 'familyvan_select', 'select@familyvan.com', '中国江苏省苏州市', '1.8.0', NULL, 'y', '2024-09-10 10:30:00', 2, NULL, NULL),
('AutoParts_010', '汽车配件商城', 'http://www.autoparts.com', '/logos/parts_logo.png', '汽车配件', '原厂及品牌汽车配件在线销售', '汽车配件,维修,保养', '/icons/parts_favicon.ico', '400-567-1234', 'autoparts_mall', 'mall@autoparts.com', '中国浙江省杭州市', '4.0.0', NULL, 'y', '2024-10-01 00:00:00', 3, '2024-10-10 10:10:10', 1);

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
INSERT INTO `t_tran` (`tran_no`, `customer_id`, `money`, `expected_date`, `stage`, `description`, `next_contact_time`, `create_time`, `create_by`, `edit_time`, `edit_by`) VALUES
('T2025061800001', 1, 15000.00, '2025-07-15 00:00:00', 41, '初步沟通，客户意向采购A产品', '2025-06-25 10:00:00', '2025-06-18 14:00:00', 1, NULL, NULL),
('T2025061800002', 2, 28000.50, '2025-08-01 00:00:00', 42, '已发送报价单，等待客户审批', '2025-06-28 14:00:00', '2025-06-18 15:30:00', 2, '2025-06-18 16:00:00', 2),
('T2025061700003', 3, 5200.75, '2025-07-10 00:00:00', 43, '审批通过，准备合同', '2025-06-20 09:00:00', '2025-06-17 11:00:00', 1, '2025-06-18 09:30:00', 1),
('T2025061700004', 4, 120000.00, '2025-09-01 00:00:00', 45, '合同已签，等待客户付款', '2025-07-01 15:00:00', '2025-06-17 16:20:00', 2, NULL, NULL),
('T2025061600005', 5, 9800.00, '2025-06-30 00:00:00', 46, '款项已收，项目完成', NULL, '2025-06-16 10:00:00', 1, '2025-06-17 14:00:00', 1),
('T2025061600006', 1, 34500.00, '2025-07-20 00:00:00', 41, '客户对B产品感兴趣，待报价', '2025-06-24 11:00:00', '2025-06-16 14:30:00', 2, NULL, NULL),
('T2025061500007', 6, 7600.00, '2025-07-05 00:00:00', 42, '报价已发，客户正在内部流程', '2025-06-25 16:00:00', '2025-06-15 09:45:00', 1, '2025-06-16 10:00:00', 1),
('T2025061500008', 7, 21000.00, '2025-08-10 00:00:00', 41, '新客户，需要上门演示产品', '2025-06-22 14:00:00', '2025-06-15 13:10:00', 2, NULL, NULL),
('T2025061400009', 8, 8900.50, '2025-07-12 00:00:00', 43, '审批通过，客户要求修改合同条款', '2025-06-19 10:30:00', '2025-06-14 17:00:00', 1, '2025-06-15 11:00:00', 1),
('T2025061400010', 9, 45000.00, '2025-08-20 00:00:00', 41, '老客户增购，需求明确', '2025-06-21 09:00:00', '2025-06-14 11:30:00', 2, NULL, NULL),
('T2025061300011', 10, 6600.00, '2025-07-01 00:00:00', 46, '小额订单，已完成交付', NULL, '2025-06-13 10:20:00', 1, '2025-06-14 09:00:00', 1),
('T2025061300012', 2, 19800.00, '2025-07-25 00:00:00', 42, '价格谈判中，客户希望有折扣', '2025-06-20 14:00:00', '2025-06-13 15:00:00', 2, '2025-06-14 14:30:00', 2),
('T2025061200013', 5, 3200.00, '2025-06-28 00:00:00', 41, '客户咨询C产品，待跟进', '2025-06-19 15:00:00', '2025-06-12 16:30:00', 1, NULL, NULL),
('T2025061200014', 7, 75000.00, '2025-09-10 00:00:00', 45, '合同已归档，等待分期付款', '2025-07-12 10:00:00', '2025-06-12 11:00:00', 2, '2025-06-13 10:00:00', 2),
('T2025061100015', 3, 23000.00, '2025-07-30 00:00:00', 41, '客户对价格敏感，需要提供多种方案', '2025-06-18 14:30:00', '2025-06-11 14:50:00', 1, NULL, NULL),
('T2025061100016', 8, 12500.00, '2025-07-18 00:00:00', 42, '客户决策人出差，审批暂停', '2025-06-26 10:00:00', '2025-06-11 10:15:00', 2, '2025-06-12 09:30:00', 2),
('T2025061000017', 4, 5500.80, '2025-07-08 00:00:00', 43, '审批完成，准备发货', '2025-06-18 11:00:00', '2025-06-10 16:00:00', 1, '2025-06-11 15:00:00', 1),
('T2025061000018', 6, 180000.00, '2025-10-01 00:00:00', 41, '大型项目，需要多部门协调报价', '2025-06-28 09:30:00', '2025-06-10 09:30:00', 2, NULL, NULL),
('T2025060900019', 9, 9999.99, '2025-07-15 00:00:00', 46, '售后服务订单，已处理完成', NULL, '2025-06-09 14:00:00', 1, '2025-06-10 11:00:00', 1),
('T2025060900020', 10, 42000.00, '2025-08-15 00:00:00', 45, '客户已付定金，等待尾款', '2025-07-10 14:00:00', '2025-06-09 11:20:00', 2, '2025-06-10 10:00:00', 2),
('TN2025061800001', 1, 5000.00, '2025-07-15 00:00:00', 37, '初步沟通，客户有意向', '2025-06-25 10:00:00', '2025-06-18 14:00:00', 1, '2025-06-18 14:00:00', 1),
('TN2025061800002', 2, 12000.50, '2025-08-01 00:00:00', 38, '已发送报价单', '2025-06-28 15:00:00', '2025-06-18 14:05:00', 2, '2025-06-18 14:05:00', 2),
('TN2025061800003', 3, 8800.00, '2025-07-20 00:00:00', 37, '客户需要更多产品资料', '2025-06-22 11:00:00', '2025-06-18 14:10:00', 1, '2025-06-18 14:10:00', 1),
('TN2025061800004', 4, 25000.00, '2025-09-01 00:00:00', 39, '进入商务谈判阶段', '2025-07-10 09:30:00', '2025-06-18 14:15:00', 3, '2025-06-18 14:15:00', 3),
('TN2025061800005', 5, 7500.75, '2025-07-10 00:00:00', 38, '客户正在内部评估', '2025-06-30 14:00:00', '2025-06-18 14:20:00', 1, '2025-06-18 14:20:00', 1),
('TN2025061800006', 6, 32000.00, '2025-08-15 00:00:00', 40, '合同已发送，等待签署', '2025-07-05 16:00:00', '2025-06-18 14:25:00', 2, '2025-06-18 14:25:00', 2),
('TN2025061800007', 7, 9999.99, '2025-07-05 00:00:00', 37, '新客户，首次接触', '2025-06-24 10:30:00', '2025-06-18 14:30:00', 3, '2025-06-18 14:30:00', 3),
('TN2025061800008', 8, 18000.00, '2025-08-20 00:00:00', 38, '已完成产品演示', '2025-07-01 11:00:00', '2025-06-18 14:35:00', 1, '2025-06-18 14:35:00', 1),
('TN2025061800009', 9, 45000.00, '2025-09-10 00:00:00', 39, '价格谈判中', '2025-07-15 14:30:00', '2025-06-18 14:40:00', 2, '2025-06-18 14:40:00', 2),
('TN2025061800010', 10, 6200.00, '2025-07-18 00:00:00', 37, '客户对A产品感兴趣', '2025-06-29 10:00:00', '2025-06-18 14:45:00', 1, '2025-06-18 14:45:00', 1),
('TN2025061800011', 1, 11000.00, '2025-08-05 00:00:00', 41, '交易失败，客户选择竞品', NULL, '2025-06-18 14:50:00', 3, '2025-06-18 14:50:00', 3),
('TN2025061800012', 2, 9300.00, '2025-07-25 00:00:00', 38, '等待客户技术部门反馈', '2025-07-02 13:00:00', '2025-06-18 14:55:00', 2, '2025-06-18 14:55:00', 2),
('TN2025061800013', 3, 15000.00, '2025-08-10 00:00:00', 39, '准备合同草案', '2025-07-08 10:00:00', '2025-06-18 15:00:00', 1, '2025-06-18 15:00:00', 1),
('TN2025061800014', 4, 55000.00, '2025-09-20 00:00:00', 40, '客户法务审核合同中', '2025-07-20 15:00:00', '2025-06-18 15:05:00', 3, '2025-06-18 15:05:00', 3),
('TN2025061800015', 5, 8200.00, '2025-07-30 00:00:00', 37, '客户预算有限，需调整方案', '2025-07-03 16:30:00', '2025-06-18 15:10:00', 1, '2025-06-18 15:10:00', 1),
('TN2025061800016', 6, 78000.00, '2025-10-01 00:00:00', 42, '交易成功', NULL, '2025-06-18 15:15:00', 2, '2025-06-18 15:15:00', 2),
('TN2025061800017', 7, 21000.00, '2025-08-25 00:00:00', 38, '已提供样品供客户测试', '2025-07-12 11:30:00', '2025-06-18 15:20:00', 3, '2025-06-18 15:20:00', 3),
('TN2025061800018', 8, 33000.00, '2025-09-15 00:00:00', 39, '客户高层意向明确', '2025-07-18 09:00:00', '2025-06-18 15:25:00', 1, '2025-06-18 15:25:00', 1),
('TN2025061800019', 9, 9500.50, '2025-08-08 00:00:00', 37, '跟进中', '2025-07-07 14:00:00', '2025-06-18 15:30:00', 2, '2025-06-18 15:30:00', 2),
('TN2025061800020', 10, 13500.00, '2025-08-18 00:00:00', 38, '客户要求上门演示', '2025-07-11 10:00:00', '2025-06-18 15:35:00', 1, '2025-06-18 15:35:00', 1);
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
INSERT INTO `t_product_category` (`name`, `code`, `description`, `sort`, `status`, `create_time`, `update_time`)
VALUES ('SUV', 'SUV', '运动型多用途汽车，具有较高的底盘和较强的越野能力', 1, '启用', '2023-05-01 09:00:00', '2023-05-01 09:00:00');

INSERT INTO `t_product_category` (`name`, `code`, `description`, `sort`, `status`, `create_time`, `update_time`)
VALUES ('轿车', 'SEDAN', '三厢式乘用车，适合日常通勤和商务用途', 2, '启用', '2023-05-01 09:00:00', '2023-05-01 09:00:00');

INSERT INTO `t_product_category` (`name`, `code`, `description`, `sort`, `status`, `create_time`, `update_time`)
VALUES ('电动轿车', 'ELECTRIC_SEDAN', '纯电动驱动的三厢式乘用车，环保节能', 3, '启用', '2023-05-01 09:00:00', '2023-05-01 09:00:00');

INSERT INTO `t_product_category` (`name`, `code`, `description`, `sort`, `status`, `create_time`, `update_time`)
VALUES ('电动SUV', 'ELECTRIC_SUV', '纯电动驱动的运动型多用途汽车', 4, '启用', '2023-05-01 09:00:00', '2023-05-01 09:00:00');

INSERT INTO `t_product_category` (`name`, `code`, `description`, `sort`, `status`, `create_time`, `update_time`)
VALUES ('跑车', 'SPORTS_CAR', '高性能运动型汽车，注重驾驶乐趣和外观设计', 5, '启用', '2023-05-01 09:00:00', '2023-05-01 09:00:00');

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
-- Records of t_product_promotion
-- ----------------------------
INSERT INTO `t_product_promotion` (`name`, `type`, `discount`, `start_time`, `end_time`, `status`, `create_time`, `update_time`)
VALUES ('豪华车型五一促销', 'PERCENTAGE', 0.95, '2023-04-28 00:00:00', '2023-05-05 23:59:59', '进行中', '2023-04-25 10:00:00', '2023-04-25 10:00:00');

INSERT INTO `t_product_promotion` (`name`, `type`, `discount`, `start_time`, `end_time`, `status`, `create_time`, `update_time`)
VALUES ('电动车购车补贴', 'AMOUNT', 20000.00, '2023-05-01 00:00:00', '2023-06-30 23:59:59', '进行中', '2023-04-30 09:00:00', '2023-04-30 09:00:00');

INSERT INTO `t_product_promotion` (`name`, `type`, `discount`, `start_time`, `end_time`, `status`, `create_time`, `update_time`)
VALUES ('SUV车型夏季特惠', 'PERCENTAGE', 0.92, '2023-06-01 00:00:00', '2023-08-31 23:59:59', '待开始', '2023-05-15 14:00:00', '2023-05-15 14:00:00');

INSERT INTO `t_product_promotion` (`name`, `type`, `discount`, `start_time`, `end_time`, `status`, `create_time`, `update_time`)
VALUES ('日系车型年中大促', 'PERCENTAGE', 0.88, '2023-07-01 00:00:00', '2023-07-31 23:59:59', '待开始', '2023-06-01 11:00:00', '2023-06-01 11:00:00');

INSERT INTO `t_product_promotion` (`name`, `type`, `discount`, `start_time`, `end_time`, `status`, `create_time`, `update_time`)
VALUES ('新能源汽车置换补贴', 'AMOUNT', 30000.00, '2023-05-15 00:00:00', '2023-12-31 23:59:59', '进行中', '2023-05-10 16:00:00', '2023-05-10 16:00:00');

INSERT INTO `t_product_promotion` (`name`, `type`, `discount`, `start_time`, `end_time`, `status`, `create_time`, `update_time`)
VALUES ('保时捷限时优惠', 'AMOUNT', 50000.00, '2023-05-20 00:00:00', '2023-05-31 23:59:59', '待开始', '2023-05-16 13:00:00', '2023-05-16 13:00:00');

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

INSERT INTO t_user (id, login_act, login_pwd, name, phone, email, account_no_expired, credentials_no_expired, account_no_locked, account_enabled, create_time, create_by, edit_time, edit_by, last_login_time) VALUES (1, 'admin', '$2a$10$Nlhwhtd0BSCBK95CAifv7eWpCjHloPBMZ3Gaehcc56hRAV3DZALJO', '管理员', '13700000000', 'admin@qq.com', 1, 1, 1, 1, '2023-02-22 09:37:12', null, '2023-05-23 00:21:06', null, '2023-12-10 21:18:59');
INSERT INTO t_user (id, login_act, login_pwd, name, phone, email, account_no_expired, credentials_no_expired, account_no_locked, account_enabled, create_time, create_by, edit_time, edit_by, last_login_time) VALUES (2, 'yuyan', '$2a$10$ZzzAd0nDuUGfGSjJDnZIyOaW7mUZkFzsYgOqiF/b07po/BGxBFjJ.', '于嫣', 'null', 'yuyan@163.com', 1, 1, 1, 1, '2023-02-28 12:11:40', null, '2023-05-23 00:21:14', null, '2023-11-29 20:14:31');
INSERT INTO t_user (id, login_act, login_pwd, name, phone, email, account_no_expired, credentials_no_expired, account_no_locked, account_enabled, create_time, create_by, edit_time, edit_by, last_login_time) VALUES (3, 'zhangqi', '$2a$10$Q0qTW6QqkabTzFyoilViw..YdrVzZkSKe5RvLmjgPgW/IrcPkBoF.', '张琪', '1362362323', 'zhangqi@qq.com', 1, 1, 1, 1, '2023-03-02 11:37:34', null, '2023-05-23 00:21:02', null, null);
INSERT INTO t_user (id, login_act, login_pwd, name, phone, email, account_no_expired, credentials_no_expired, account_no_locked, account_enabled, create_time, create_by, edit_time, edit_by, last_login_time) VALUES (4, 'suwanting', '$2a$10$3bambNLTCAKtQn2OXPiHb.f0SzH.MucTiLi6GPT6nQrYpsxsdxaFi', '苏婉婷', null, 'suwanting@qq.com', 1, 1, 1, 1, '2023-04-03 15:04:54', null, null, null, null);
INSERT INTO t_user (id, login_act, login_pwd, name, phone, email, account_no_expired, credentials_no_expired, account_no_locked, account_enabled, create_time, create_by, edit_time, edit_by, last_login_time) VALUES (5, 'wuxiaoxiao', '$2a$10$Lmk5wXYkZzQMFJEcXVZAZegIQhnAm6ONHpz09X/.gbOh5ze5fU6MW', '吴潇潇', null, 'wuxiaoxiao@qq.com', 1, 1, 1, 1, '2023-01-27 12:15:26', null, null, null, null);
INSERT INTO t_user (id, login_act, login_pwd, name, phone, email, account_no_expired, credentials_no_expired, account_no_locked, account_enabled, create_time, create_by, edit_time, edit_by, last_login_time) VALUES (6, 'mengyan', '$2a$10$6zGT7CfeuJ/6jZPk1pAqcuiMYDnCJstrceThGD5DVVOA5XvOP/sQq', '孟岩', null, 'mengyan@163.com', 1, 1, 1, 1, '2023-03-19 10:17:28', null, null, null, null);
INSERT INTO t_user (id, login_act, login_pwd, name, phone, email, account_no_expired, credentials_no_expired, account_no_locked, account_enabled, create_time, create_by, edit_time, edit_by, last_login_time) VALUES (7, 'yuanhuimin', '$2a$10$mbsloGtPV7cDwfAVYxuvLemQRWumZKrDxVZxg4fnbfaocnfZFlYuu', '袁慧敏', null, 'yuanhuimin@11.com', 1, 1, 1, 1, '2023-04-11 20:18:50', null, null, null, null);
INSERT INTO t_user (id, login_act, login_pwd, name, phone, email, account_no_expired, credentials_no_expired, account_no_locked, account_enabled, create_time, create_by, edit_time, edit_by, last_login_time) VALUES (8, 'qinxuwen', '$2a$10$ir8uLlBrPMHRtGiu5Ajkv.UKcRacXWRen7zxelp9iUaco3WhGkJ36', '秦旭文', '13820000000', 'qinxuwen@163.com', 1, 1, 1, 1, '2023-03-19 21:11:37', null, null, null, null);
INSERT INTO t_user (id, login_act, login_pwd, name, phone, email, account_no_expired, credentials_no_expired, account_no_locked, account_enabled, create_time, create_by, edit_time, edit_by, last_login_time) VALUES (9, 'chengjie', '$2a$10$jQR8yyF/ailGP/zW6G4JOOffzWOXhe02Rgw7MZLfxL.IGFdM3cjM2', '程杰', '13500000000', 'chengjie@qq.com', 1, 1, 1, 1, '2023-04-16 07:16:19', null, '2023-04-20 21:42:21', null, null);
INSERT INTO t_user (id, login_act, login_pwd, name, phone, email, account_no_expired, credentials_no_expired, account_no_locked, account_enabled, create_time, create_by, edit_time, edit_by, last_login_time) VALUES (10, 'zhouliang', '$2a$10$0yOGdkAcG8JLEcoEmmCnfO8Vp6rcqBnn30k6pGor5Z0.eLMyLEd7.', '周亮', '13800000008', 'zhouliang@163.com', 1, 1, 1, 1, '2023-03-18 13:13:45', null, '2023-06-06 00:06:31', null, null);
INSERT INTO t_user (id, login_act, login_pwd, name, phone, email, account_no_expired, credentials_no_expired, account_no_locked, account_enabled, create_time, create_by, edit_time, edit_by, last_login_time) VALUES (11, 'zhangwei', '$2a$10$BfOgsdSAZ9VYBOzv692BM.oWGPLktcqhhjU3AaWESkGNRcW484N7O', '张伟', null, 'zhangwei@qq.com', 1, 1, 1, 1, '2023-03-06 09:18:23', null, null, null, null);
INSERT INTO t_user (id, login_act, login_pwd, name, phone, email, account_no_expired, credentials_no_expired, account_no_locked, account_enabled, create_time, create_by, edit_time, edit_by, last_login_time) VALUES (12, 'dengping', '$2a$10$hpN8orfqUFXb.WWbIoZBkOZrr6D8rdSbl/SWXsMQ0zEuqkldlkpW2', '邓萍', null, 'dengping@qq.com', 1, 1, 1, 1, '2023-02-19 20:10:58', null, null, null, null);
INSERT INTO t_user (id, login_act, login_pwd, name, phone, email, account_no_expired, credentials_no_expired, account_no_locked, account_enabled, create_time, create_by, edit_time, edit_by, last_login_time) VALUES (13, 'zhangxing', '$2a$10$uBVDcCCJQvTfoFCjbjwrf.MhyczNNJfCn76jD61CsAgsUlXjXhxzG', '张欣', null, 'zhangxing@qq.com', 1, 1, 1, 1, '2023-03-17 12:12:11', null, null, null, null);
INSERT INTO t_user (id, login_act, login_pwd, name, phone, email, account_no_expired, credentials_no_expired, account_no_locked, account_enabled, create_time, create_by, edit_time, edit_by, last_login_time) VALUES (14, 'zhangmeng', '$2a$10$MMHG2cQh4H4YFbdf48SnyO9IZ78F110x3.7IWGNExrgk2rFmhrd/u', '张萌', null, 'zhangmeng@qq.com', 1, 1, 1, 1, '2023-01-13 08:16:02', null, null, null, null);
INSERT INTO t_user (id, login_act, login_pwd, name, phone, email, account_no_expired, credentials_no_expired, account_no_locked, account_enabled, create_time, create_by, edit_time, edit_by, last_login_time) VALUES (15, 'shixixiang', '$2a$10$zYwq/QfevFPAZxw4b2DkCeQvjVQ52AUU9c4aC0uS0wTJaRr75G74y', '石喜祥', null, 'shixixiang@qq.com', 1, 1, 1, 1, '2023-03-10 15:19:49', null, null, null, null);
INSERT INTO t_user (id, login_act, login_pwd, name, phone, email, account_no_expired, credentials_no_expired, account_no_locked, account_enabled, create_time, create_by, edit_time, edit_by, last_login_time) VALUES (16, 'chengjiuming', '$2a$10$yNN5TcFkM4OqRsKGNM8CNeqAJhRYKQgXVFqbre5lQPicnIXT7THTu', '陈久明', null, 'chengjiuming@163.com', 1, 1, 1, 1, '2023-04-09 23:17:37', null, null, null, null);
INSERT INTO t_user (id, login_act, login_pwd, name, phone, email, account_no_expired, credentials_no_expired, account_no_locked, account_enabled, create_time, create_by, edit_time, edit_by, last_login_time) VALUES (17, 'genghao', '$2a$10$rWHo.vUpJCbqWLGMkPj95O5FlhaQLzro.LY7pVQ/UnVVAdvjEAy0K', '耿浩', null, 'genghao@qq.com', 1, 1, 1, 1, '2023-03-19 12:10:22', null, '2023-04-10 21:42:21', null, null);
INSERT INTO t_user (id, login_act, login_pwd, name, phone, email, account_no_expired, credentials_no_expired, account_no_locked, account_enabled, create_time, create_by, edit_time, edit_by, last_login_time) VALUES (18, 'hanmingyang', '$2a$10$PRMdG7a8nFIN1A3TD584Xe2BZI7Y0mktDL7Wp5lF88E1D1iPijFc6', '韩明洋', null, 'hanmingyang@163.com', 1, 1, 1, 1, '2023-02-12 18:13:01', null, '2023-04-13 23:43:25', null, null);
INSERT INTO t_user (id, login_act, login_pwd, name, phone, email, account_no_expired, credentials_no_expired, account_no_locked, account_enabled, create_time, create_by, edit_time, edit_by, last_login_time) VALUES (19, 'xuyan', '$2a$10$S7MF2dOqFcoOJPqpEH2nu.Muhn2XC0BlBTZ5gAoL3axrQxdJEJNnK', '徐燕', null, 'xuyan@qq.com', 1, 1, 1, 1, '2023-03-29 13:16:15', null, null, null, null);
INSERT INTO t_user (id, login_act, login_pwd, name, phone, email, account_no_expired, credentials_no_expired, account_no_locked, account_enabled, create_time, create_by, edit_time, edit_by, last_login_time) VALUES (20, 'chengjuan', '$2a$10$m1g5cxikApV05pR7Cx4cy.d4sT3efOl6UvDLvH27WzMjtpymQ5ANi', '程娟', null, 'chengjuan@qq.com', 1, 1, 1, 1, '2023-02-19 15:12:22', null, null, null, null);
INSERT INTO t_user (id, login_act, login_pwd, name, phone, email, account_no_expired, credentials_no_expired, account_no_locked, account_enabled, create_time, create_by, edit_time, edit_by, last_login_time) VALUES (21, 'huangxiao', '$2a$10$R/RwQd5.3OxYpSZBLIn8DeeYYNF0vgWCrCR4tcyL.c/HtnuIfBRIK', '黄潇', null, 'huangxiao@qq.com', 1, 1, 1, 1, '2023-03-26 22:11:37', null, null, null, null);
INSERT INTO t_user (id, login_act, login_pwd, name, phone, email, account_no_expired, credentials_no_expired, account_no_locked, account_enabled, create_time, create_by, edit_time, edit_by, last_login_time) VALUES (22, 'yangyuxin', '$2a$10$ucE/By6NLBb4tN5H3CUimOQ2eAtbjXFf2v77SJUPbHXRI9lTF97Ka', '杨雨欣', null, 'yangyuxin@163.com', 1, 1, 1, 1, '2023-04-13 18:14:59', null, null, null, null);

create table t_user_role
(
    id      int auto_increment
        primary key,
    user_id int null,
    role_id int null
)ENGINE = InnoDB
 AUTO_INCREMENT = 1
 CHARACTER SET = utf8mb3
 COLLATE = utf8mb3_general_ci COMMENT = '用户角色关系表'
 ROW_FORMAT = DYNAMIC;

create index t_user_role_ibfk_1
    on t_user_role (user_id);

create index t_user_role_ibfk_2
    on t_user_role (role_id);

INSERT INTO t_user_role (id, user_id, role_id) VALUES (1, 1, 1);
INSERT INTO t_user_role (id, user_id, role_id) VALUES (2, 2, 2);
INSERT INTO t_user_role (id, user_id, role_id) VALUES (3, 3, 2);
INSERT INTO t_user_role (id, user_id, role_id) VALUES (4, 4, 3);
INSERT INTO t_user_role (id, user_id, role_id) VALUES (5, 5, 4);
INSERT INTO t_user_role (id, user_id, role_id) VALUES (6, 6, 5);

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


