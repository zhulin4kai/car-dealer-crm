-- ============================================================================
-- V0001: 数据库结构约束与初始化数据治理 - 手工迁移脚本（执行一次）
--
-- 本文件是现有数据库的手工迁移脚本，不通过迁移框架自动执行。
-- 每个 ALTER 前包含检查 SQL，执行一次后通过检查语句确认已完成。
-- 对 MariaDB 手工执行迁移前，先运行第一部分的重复值、孤儿、负数和非法状态检查。
-- 检查有结果时停止添加约束并输出数据修复清单，禁止直接删除历史数据。
--
-- 功能:
--   1. 孤儿数据检查（注释形式，执行前人工审查）
--   2. 唯一约束补充
--   3. 非空约束与默认值修正
--   4. 外键约束（含删除策略）
--   5. t_dic_value 新增 value_code 列
--   6. t_tran_product.product_id 类型对齐
--   7. 新增 t_payment 表（若缺失）
--   8. 移除 t_tran_production（若仍存在）
--
-- 回滚说明: 每一步注释了失败后回滚方法。
-- 执行前确保已在非生产环境验证并备份数据库。
-- ============================================================================

-- ============================================================================
-- 第一部分：孤儿数据检查（手动审查后执行）
-- ============================================================================

-- 1.1 检查 t_customer.clue_id 孤儿（发往不存在的线索）
-- SELECT c.id, c.clue_id FROM t_customer c
-- LEFT JOIN t_clue cl ON c.clue_id = cl.id
-- WHERE c.clue_id IS NOT NULL AND cl.id IS NULL;
-- 修复: UPDATE t_customer SET clue_id = NULL WHERE clue_id IN (...);
-- 或删除孤儿客户记录。

-- 1.2 检查 t_tran.customer_id 孤儿
-- SELECT t.id, t.customer_id FROM t_tran t
-- LEFT JOIN t_customer c ON t.customer_id = c.id
-- WHERE t.customer_id IS NOT NULL AND c.id IS NULL;
-- 修复: UPDATE t_tran SET customer_id = NULL 或修正 customer_id。

-- 1.3 检查 t_tran_product 孤儿（tran_id 或 product_id 不存在）
-- SELECT tp.id, tp.tran_id, tp.product_id FROM t_tran_product tp
-- LEFT JOIN t_tran t ON tp.tran_id = t.id
-- LEFT JOIN t_product p ON tp.product_id = p.id
-- WHERE t.id IS NULL OR p.id IS NULL;
-- 修复: DELETE FROM t_tran_product WHERE id IN (...);

-- 1.4 检查 t_activity_remark.activity_id 孤儿
-- SELECT ar.id, ar.activity_id FROM t_activity_remark ar
-- LEFT JOIN t_activity a ON ar.activity_id = a.id
-- WHERE ar.activity_id IS NOT NULL AND a.id IS NULL;
-- 修复: DELETE FROM t_activity_remark WHERE id IN (...);

-- 1.5 检查 t_clue_remark.clue_id 孤儿
-- SELECT cr.id, cr.clue_id FROM t_clue_remark cr
-- LEFT JOIN t_clue c ON cr.clue_id = c.id
-- WHERE cr.clue_id IS NOT NULL AND c.id IS NULL;
-- 修复: DELETE FROM t_clue_remark WHERE id IN (...);

-- 1.6 检查 t_customer_remark.customer_id 孤儿
-- SELECT cr.id, cr.customer_id FROM t_customer_remark cr
-- LEFT JOIN t_customer c ON cr.customer_id = c.id
-- WHERE cr.customer_id IS NOT NULL AND c.id IS NULL;
-- 修复: DELETE FROM t_customer_remark WHERE id IN (...);

-- 1.7 检查 t_tran_remark.tran_id 孤儿
-- SELECT tr.id, tr.tran_id FROM t_tran_remark tr
-- LEFT JOIN t_tran t ON tr.tran_id = t.id
-- WHERE tr.tran_id IS NOT NULL AND t.id IS NULL;
-- 修复: DELETE FROM t_tran_remark WHERE id IN (...);

-- 1.8 检查 t_tran_approve.tran_id 孤儿
-- SELECT ta.id, ta.tran_id FROM t_tran_approve ta
-- LEFT JOIN t_tran t ON ta.tran_id = t.id
-- WHERE t.id IS NULL;
-- 修复: DELETE FROM t_tran_approve WHERE id IN (...);

-- 1.9 检查 t_tran_invoice.tran_id 孤儿
-- SELECT ti.id, ti.tran_id FROM t_tran_invoice ti
-- LEFT JOIN t_tran t ON ti.tran_id = t.id
-- WHERE t.id IS NULL;
-- 修复: DELETE FROM t_tran_invoice WHERE id IN (...);

-- 1.10 检查 t_tran_history.tran_id 孤儿
-- SELECT th.id, th.tran_id FROM t_tran_history th
-- LEFT JOIN t_tran t ON th.tran_id = t.id
-- WHERE t.id IS NULL;
-- 修复: DELETE FROM t_tran_history WHERE id IN (...);

-- 1.11 检查 t_product_stock_record.product_id 孤儿
-- SELECT psr.id, psr.product_id FROM t_product_stock_record psr
-- LEFT JOIN t_product p ON psr.product_id = p.id
-- WHERE p.id IS NULL;
-- 修复: DELETE FROM t_product_stock_record WHERE id IN (...);

-- 1.12 检查 t_user_role 孤儿
-- SELECT ur.id, ur.user_id, ur.role_id FROM t_user_role ur
-- LEFT JOIN t_user u ON ur.user_id = u.id
-- LEFT JOIN t_role r ON ur.role_id = r.id
-- WHERE u.id IS NULL OR r.id IS NULL;
-- 修复: DELETE FROM t_user_role WHERE id IN (...);

-- 1.13 检查 t_role_permission 孤儿
-- SELECT rp.id, rp.role_id, rp.permission_id FROM t_role_permission rp
-- LEFT JOIN t_role r ON rp.role_id = r.id
-- LEFT JOIN t_permission p ON rp.permission_id = p.id
-- WHERE r.id IS NULL OR p.id IS NULL;
-- 修复: DELETE FROM t_role_permission WHERE id IN (...);

-- 1.14 检查 t_product 重复 SKU
-- SELECT sku, COUNT(*) FROM t_product WHERE sku IS NOT NULL GROUP BY sku HAVING COUNT(*) > 1;
-- 修复: 合并或删除重复 SKU 产品。

-- 1.15 检查 t_clue 重复手机号
-- SELECT phone, COUNT(*) FROM t_clue WHERE phone IS NOT NULL GROUP BY phone HAVING COUNT(*) > 1;
-- 修复: 合并或标记重复线索。

-- 1.16 检查 t_product 状态非法值（非 on_sale/off_sale）
-- SELECT id, sku, status FROM t_product
-- WHERE status NOT IN ('on_sale', 'off_sale');
-- 修复: UPDATE t_product SET status = 'on_sale' WHERE status = '上架';
--       UPDATE t_product SET status = 'off_sale' WHERE status = '下架';

-- 1.17 检查 t_product 负数价格或库存
-- SELECT id, sku, price, stock FROM t_product WHERE price < 0 OR stock < 0;
-- 修复: 修正为合法正值或 0。


-- ============================================================================
-- 第二部分：t_dic_value 新增 value_code 列
-- ============================================================================

-- 新增 value_code 列（业务稳定 code，用于跨环境引用）
ALTER TABLE t_dic_value ADD COLUMN value_code VARCHAR(64) DEFAULT NULL COMMENT '字典值业务代码' AFTER type_value;

-- 回填: 根据现有 type_value 生成 value_code（需要根据业务确认映射）
-- UPDATE t_dic_value SET value_code = ... WHERE ...;
-- 回滚: ALTER TABLE t_dic_value DROP COLUMN value_code;


-- ============================================================================
-- 第三部分：唯一约束
-- ============================================================================

-- 3.1 t_dic_type.type_code 唯一
-- 回滚: ALTER TABLE t_dic_type DROP INDEX uk_type_code;
ALTER TABLE t_dic_type ADD CONSTRAINT uk_type_code UNIQUE (type_code);

-- 3.2 t_dic_value (type_code, value_code) 唯一
-- 前置: 确保 value_code 已回填。
-- 回滚: ALTER TABLE t_dic_value DROP INDEX uk_type_value_code;
ALTER TABLE t_dic_value ADD CONSTRAINT uk_type_value_code UNIQUE (type_code, value_code);

-- 3.3 t_product.sku 唯一
-- 回滚: ALTER TABLE t_product DROP INDEX uk_sku;
ALTER TABLE t_product ADD CONSTRAINT uk_sku UNIQUE (sku);

-- 3.4 t_clue.phone 唯一（MySQL 中 NULL 不参与唯一约束比较）
-- 回滚: ALTER TABLE t_clue DROP INDEX uk_phone;
ALTER TABLE t_clue ADD CONSTRAINT uk_phone UNIQUE (phone);

-- 3.5 t_tran_invoice.invoice_no 唯一（若未创建）
-- 回滚: ALTER TABLE t_tran_invoice DROP INDEX uk_invoice_no;
-- 注: 当前生产 Schema 已有此约束，使用 IF NOT EXISTS 语义跳过。
CREATE UNIQUE INDEX IF NOT EXISTS uk_invoice_no ON t_tran_invoice (invoice_no);

-- 3.6 t_payment.payment_no 唯一（若 t_payment 表存在）
-- 注: 如果表不存在则跳过此语句。
-- 回滚: ALTER TABLE t_payment DROP INDEX uk_payment_no;


-- ============================================================================
-- 第四部分：非空约束与默认值
-- ============================================================================

-- 4.1 t_product.sku NOT NULL
-- 前置: 检查无 NULL sku: SELECT COUNT(*) FROM t_product WHERE sku IS NULL;
-- 回滚: ALTER TABLE t_product MODIFY sku VARCHAR(255) NULL DEFAULT NULL;
ALTER TABLE t_product MODIFY sku VARCHAR(255) NOT NULL COMMENT '商品的库存单位';

-- 4.2 t_product.price NOT NULL，默认值 0
-- 回滚: ALTER TABLE t_product MODIFY price DECIMAL(10,2) NULL DEFAULT NULL;
ALTER TABLE t_product MODIFY price DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '商品价格';

-- 4.3 t_product.stock NOT NULL，默认值 0
-- 回滚: ALTER TABLE t_product MODIFY stock INT NULL DEFAULT NULL;
ALTER TABLE t_product MODIFY stock INT NOT NULL DEFAULT 0 COMMENT '当前商品库存量';

-- 4.4 t_product.status NOT NULL
-- 前置: 确保无 NULL status: SELECT COUNT(*) FROM t_product WHERE status IS NULL;
-- 回滚: ALTER TABLE t_product MODIFY status VARCHAR(50) NULL DEFAULT NULL;
ALTER TABLE t_product MODIFY status VARCHAR(50) NOT NULL DEFAULT 'off_sale' COMMENT '商品状态: on_sale上架, off_sale下架';

-- 4.5 t_product_stock_record.quantity 默认值 0
ALTER TABLE t_product_stock_record MODIFY quantity INT NULL DEFAULT 0 COMMENT '变动数量';


-- ============================================================================
-- 第五部分: CHECK 约束（非负价格和库存）
-- 注: MariaDB 10.2+ 和 MySQL 8.0.16+ 支持 CHECK 约束。
-- ============================================================================

-- 5.1 t_product 价格非负
-- 前置: 检查负数: SELECT id, price FROM t_product WHERE price < 0;
-- 回滚: ALTER TABLE t_product DROP CHECK chk_product_price_nonneg;
ALTER TABLE t_product ADD CONSTRAINT chk_product_price_nonneg CHECK (price >= 0);

-- 5.2 t_product 库存非负
-- 回滚: ALTER TABLE t_product DROP CHECK chk_product_stock_nonneg;
ALTER TABLE t_product ADD CONSTRAINT chk_product_stock_nonneg CHECK (stock >= 0);


-- ============================================================================
-- 第六部分: 外键约束（含删除策略）
-- 注释说明:
--   - RESTRICT: 财务/库存/审批/历史/审计表，禁止级联删除父记录
--   - CASCADE: 备注类从属记录随父记录删除，用户/角色关联表随主表删除
-- ============================================================================

-- 6.1 t_customer.clue_id → t_clue.id (RESTRICT: 线索转客户后不应删线索)
-- 回滚: ALTER TABLE t_customer DROP FOREIGN KEY fk_customer_clue;
ALTER TABLE t_customer ADD CONSTRAINT fk_customer_clue
    FOREIGN KEY (clue_id) REFERENCES t_clue(id) ON DELETE RESTRICT;

-- 6.2 t_tran.customer_id → t_customer.id (RESTRICT)
-- 回滚: ALTER TABLE t_tran DROP FOREIGN KEY fk_tran_customer;
ALTER TABLE t_tran ADD CONSTRAINT fk_tran_customer
    FOREIGN KEY (customer_id) REFERENCES t_customer(id) ON DELETE RESTRICT;

-- 6.3 t_tran_product.tran_id → t_tran.id (RESTRICT)
-- 回滚: ALTER TABLE t_tran_product DROP FOREIGN KEY fk_tran_product_tran;
ALTER TABLE t_tran_product ADD CONSTRAINT fk_tran_product_tran
    FOREIGN KEY (tran_id) REFERENCES t_tran(id) ON DELETE RESTRICT;

-- 6.4 t_tran_product.product_id → t_product.id (RESTRICT)
-- 前置: 确保 product_id 类型为 BIGINT 以匹配 t_product.id
-- SELECT DATA_TYPE FROM INFORMATION_SCHEMA.COLUMNS
-- WHERE TABLE_NAME='t_tran_product' AND COLUMN_NAME='product_id';
-- 若为 INT，先执行: ALTER TABLE t_tran_product MODIFY product_id BIGINT NOT NULL COMMENT '产品ID';
-- 回滚: ALTER TABLE t_tran_product DROP FOREIGN KEY fk_tran_product_product;
ALTER TABLE t_tran_product ADD CONSTRAINT fk_tran_product_product
    FOREIGN KEY (product_id) REFERENCES t_product(id) ON DELETE RESTRICT;

-- 6.5 t_activity_remark.activity_id → t_activity.id (CASCADE: 备注随活动删除)
-- 回滚: ALTER TABLE t_activity_remark DROP FOREIGN KEY fk_activity_remark_activity;
ALTER TABLE t_activity_remark ADD CONSTRAINT fk_activity_remark_activity
    FOREIGN KEY (activity_id) REFERENCES t_activity(id) ON DELETE CASCADE;

-- 6.6 t_clue_remark.clue_id → t_clue.id (CASCADE: 备注随线索删除)
-- 回滚: ALTER TABLE t_clue_remark DROP FOREIGN KEY fk_clue_remark_clue;
ALTER TABLE t_clue_remark ADD CONSTRAINT fk_clue_remark_clue
    FOREIGN KEY (clue_id) REFERENCES t_clue(id) ON DELETE CASCADE;

-- 6.7 t_customer_remark.customer_id → t_customer.id (CASCADE: 备注随客户删除)
-- 回滚: ALTER TABLE t_customer_remark DROP FOREIGN KEY fk_customer_remark_customer;
ALTER TABLE t_customer_remark ADD CONSTRAINT fk_customer_remark_customer
    FOREIGN KEY (customer_id) REFERENCES t_customer(id) ON DELETE CASCADE;

-- 6.8 t_tran_remark.tran_id → t_tran.id (CASCADE: 备注随交易删除)
-- 回滚: ALTER TABLE t_tran_remark DROP FOREIGN KEY fk_tran_remark_tran;
ALTER TABLE t_tran_remark ADD CONSTRAINT fk_tran_remark_tran
    FOREIGN KEY (tran_id) REFERENCES t_tran(id) ON DELETE CASCADE;

-- 6.9 t_tran_approve.tran_id → t_tran.id (RESTRICT: 审批记录保护)
-- 回滚: ALTER TABLE t_tran_approve DROP FOREIGN KEY fk_tran_approve_tran;
ALTER TABLE t_tran_approve ADD CONSTRAINT fk_tran_approve_tran
    FOREIGN KEY (tran_id) REFERENCES t_tran(id) ON DELETE RESTRICT;

-- 6.10 t_tran_invoice.tran_id → t_tran.id (RESTRICT: 财务数据保护)
-- 回滚: ALTER TABLE t_tran_invoice DROP FOREIGN KEY fk_tran_invoice_tran;
ALTER TABLE t_tran_invoice ADD CONSTRAINT fk_tran_invoice_tran
    FOREIGN KEY (tran_id) REFERENCES t_tran(id) ON DELETE RESTRICT;

-- 6.11 t_tran_history.tran_id → t_tran.id (RESTRICT: 历史记录保护)
-- 回滚: ALTER TABLE t_tran_history DROP FOREIGN KEY fk_tran_history_tran;
ALTER TABLE t_tran_history ADD CONSTRAINT fk_tran_history_tran
    FOREIGN KEY (tran_id) REFERENCES t_tran(id) ON DELETE RESTRICT;

-- 6.12 t_product_stock_record.product_id → t_product.id (RESTRICT: 库存流水保护)
-- 回滚: ALTER TABLE t_product_stock_record DROP FOREIGN KEY fk_stock_record_product;
ALTER TABLE t_product_stock_record ADD CONSTRAINT fk_stock_record_product
    FOREIGN KEY (product_id) REFERENCES t_product(id) ON DELETE RESTRICT;

-- 6.13 t_user_role.user_id → t_user.id (CASCADE: 用户删除时清理角色关联)
-- 回滚: ALTER TABLE t_user_role DROP FOREIGN KEY fk_user_role_user;
ALTER TABLE t_user_role ADD CONSTRAINT fk_user_role_user
    FOREIGN KEY (user_id) REFERENCES t_user(id) ON DELETE CASCADE;

-- 6.14 t_user_role.role_id → t_role.id (CASCADE: 角色删除时清理用户关联)
-- 回滚: ALTER TABLE t_user_role DROP FOREIGN KEY fk_user_role_role;
ALTER TABLE t_user_role ADD CONSTRAINT fk_user_role_role
    FOREIGN KEY (role_id) REFERENCES t_role(id) ON DELETE CASCADE;

-- 6.15 t_role_permission.role_id → t_role.id (CASCADE)
-- 回滚: ALTER TABLE t_role_permission DROP FOREIGN KEY fk_role_permission_role;
ALTER TABLE t_role_permission ADD CONSTRAINT fk_role_permission_role
    FOREIGN KEY (role_id) REFERENCES t_role(id) ON DELETE CASCADE;

-- 6.16 t_role_permission.permission_id → t_permission.id (CASCADE)
-- 回滚: ALTER TABLE t_role_permission DROP FOREIGN KEY fk_role_permission_permission;
ALTER TABLE t_role_permission ADD CONSTRAINT fk_role_permission_permission
    FOREIGN KEY (permission_id) REFERENCES t_permission(id) ON DELETE CASCADE;

-- 6.17 t_payment.tran_id → t_tran.id (RESTRICT: 支付数据保护)
-- 仅当 t_payment 表存在时执行
-- 回滚: ALTER TABLE t_payment DROP FOREIGN KEY fk_payment_tran;
-- ALTER TABLE t_payment ADD CONSTRAINT fk_payment_tran
--     FOREIGN KEY (tran_id) REFERENCES t_tran(id) ON DELETE RESTRICT;

-- 6.18 t_product_promotion.product_id → t_product.id (RESTRICT)
-- 仅当 t_product_promotion.product_id 不为 NULL 的记录存在时有效
-- 回滚: ALTER TABLE t_product_promotion DROP FOREIGN KEY fk_promotion_product;
-- ALTER TABLE t_product_promotion ADD CONSTRAINT fk_promotion_product
--     FOREIGN KEY (product_id) REFERENCES t_product(id) ON DELETE RESTRICT;


-- ============================================================================
-- 第七部分: t_tran_product.product_id 类型对齐为 BIGINT
-- ============================================================================

-- 检查当前类型:
-- SELECT DATA_TYPE FROM INFORMATION_SCHEMA.COLUMNS
-- WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 't_tran_product' AND COLUMN_NAME = 'product_id';
-- 回滚: ALTER TABLE t_tran_product MODIFY product_id INT NOT NULL COMMENT '产品ID';
ALTER TABLE t_tran_product MODIFY product_id BIGINT NOT NULL COMMENT '产品ID';


-- ============================================================================
-- 第八部分: 新增 t_payment 表（若缺失）
-- ============================================================================
-- 仅在 MariaDB/MySQL 中不存在 t_payment 表时创建。
-- 回滚: DROP TABLE IF EXISTS t_payment;

CREATE TABLE IF NOT EXISTS t_payment (
    id              INT NOT NULL AUTO_INCREMENT COMMENT '主键',
    tran_id         INT NOT NULL COMMENT '交易ID',
    payment_no      VARCHAR(64) NOT NULL COMMENT '支付流水号',
    amount          DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '支付金额',
    payment_method  VARCHAR(32) NOT NULL COMMENT '支付方式',
    payment_type    VARCHAR(32) NOT NULL COMMENT '支付类型',
    payment_status  VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT '支付状态',
    payment_time    DATETIME NULL DEFAULT NULL COMMENT '支付时间',
    transaction_ref VARCHAR(128) NULL DEFAULT NULL COMMENT '第三方交易参考号',
    remark          VARCHAR(255) NULL DEFAULT NULL COMMENT '备注',
    create_time     DATETIME NULL DEFAULT NULL COMMENT '创建时间',
    create_by       INT NULL DEFAULT NULL COMMENT '创建人',
    edit_time       DATETIME NULL DEFAULT NULL COMMENT '编辑时间',
    edit_by         INT NULL DEFAULT NULL COMMENT '编辑人',
    PRIMARY KEY (id),
    UNIQUE KEY uk_payment_no (payment_no),
    INDEX idx_tran_id (tran_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付表';


-- ============================================================================
-- 第九部分: 移除 t_tran_production 表
-- ============================================================================
-- 仅当该表无业务数据且确认无代码引用时执行。
-- 前置检查: SELECT COUNT(*) FROM t_tran_production;
-- 回滚: 从备份恢复。
-- DROP TABLE IF EXISTS t_tran_production;


-- ============================================================================
-- 第十部分: 产品状态数据修正
-- ============================================================================
-- 将中文状态值改为英文 code
-- 前置: SELECT id, sku, status FROM t_product WHERE status IN ('上架', '下架');
-- 回滚: UPDATE t_product SET status = '上架' WHERE status = 'on_sale';
--       UPDATE t_product SET status = '下架' WHERE status = 'off_sale';
-- UPDATE t_product SET status = 'on_sale' WHERE status = '上架';
-- UPDATE t_product SET status = 'off_sale' WHERE status = '下架';
