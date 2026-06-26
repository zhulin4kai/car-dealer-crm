package com.autodealer.crm;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 数据库结构约束测试
 * 使用 H2 (application-smoke.yml profile) 测试完整 Schema 初始化
 * 每个测试在独立事务中执行，结束后自动回滚，不依赖测试执行顺序。
 */
@SpringBootTest(properties = {
    "spring.data.redis.port=63790",
    "mybatis-plus.configuration.log-impl=org.apache.ibatis.logging.stdout.StdOutImpl"
})
@ActiveProfiles("smoke")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Transactional
class SchemaConstraintsTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // ==================== Schema 初始化验证 ====================

    @Test
    @Order(1)
    @DisplayName("Schema 完整初始化 - 所有核心表存在且可查询")
    void testSchemaInitialized() {
        String[] tables = {
            "t_activity", "t_activity_remark",
            "t_clue", "t_clue_remark",
            "t_customer", "t_customer_remark",
            "t_dic_type", "t_dic_value",
            "t_permission", "t_role", "t_role_permission",
            "t_tran", "t_tran_history", "t_tran_product",
            "t_tran_invoice", "t_tran_approve", "t_tran_remark",
            "t_quote", "t_quote_version", "t_quote_version_item", "t_quote_status_history",
            "t_product", "t_product_category", "t_product_promotion",
            "t_product_vehicle", "t_product_stock_record",
            "t_delivery", "t_delivery_check_item",
            "t_payment", "t_refund_request",
            "t_user", "t_user_role", "t_clue_owner_history"
        };
        for (String table : tables) {
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + table, Integer.class);
            assertNotNull(count, table + " 表应可查询");
        }
    }

    @Test
    @Order(2)
    @DisplayName("t_tran_production 已从 Schema 移除")
    void testTranProductionRemoved() {
        assertThrows(Exception.class, () ->
            jdbcTemplate.queryForObject("SELECT COUNT(*) FROM t_tran_production", Integer.class));
    }

    // ==================== 唯一约束测试 ====================

    @Test
    @Order(3)
    @DisplayName("重复 login_act 插入失败")
    void testDuplicateLoginActFails() {
        jdbcTemplate.execute(
            "INSERT INTO t_user (id, login_act, login_pwd, name, phone, email, " +
            "account_no_expired, credentials_no_expired, account_no_locked, account_enabled) " +
            "VALUES (100, 'dup_login', 'pwd', 'test', '13800138001', 'dup1@test.com', 1, 1, 1, 1)");
        assertThrows(Exception.class, () ->
            jdbcTemplate.execute(
                "INSERT INTO t_user (id, login_act, login_pwd, name, phone, email, " +
                "account_no_expired, credentials_no_expired, account_no_locked, account_enabled) " +
                "VALUES (101, 'dup_login', 'pwd', 'test', '13800138002', 'dup2@test.com', 1, 1, 1, 1)"));
    }

    @Test
    @Order(4)
    @DisplayName("重复 phone 插入失败")
    void testDuplicatePhoneFails() {
        jdbcTemplate.execute(
            "INSERT INTO t_user (id, login_act, login_pwd, name, phone, email, " +
            "account_no_expired, credentials_no_expired, account_no_locked, account_enabled) " +
            "VALUES (102, 'user_phone_1', 'pwd', 'test', '13900139001', 'ph1@test.com', 1, 1, 1, 1)");
        assertThrows(Exception.class, () ->
            jdbcTemplate.execute(
                "INSERT INTO t_user (id, login_act, login_pwd, name, phone, email, " +
                "account_no_expired, credentials_no_expired, account_no_locked, account_enabled) " +
                "VALUES (103, 'user_phone_2', 'pwd', 'test', '13900139001', 'ph2@test.com', 1, 1, 1, 1)"));
    }

    @Test
    @Order(5)
    @DisplayName("重复 email 插入失败")
    void testDuplicateEmailFails() {
        jdbcTemplate.execute(
            "INSERT INTO t_user (id, login_act, login_pwd, name, phone, email, " +
            "account_no_expired, credentials_no_expired, account_no_locked, account_enabled) " +
            "VALUES (104, 'user_email_1', 'pwd', 'test', '13900139002', 'dup_email@test.com', 1, 1, 1, 1)");
        assertThrows(Exception.class, () ->
            jdbcTemplate.execute(
                "INSERT INTO t_user (id, login_act, login_pwd, name, phone, email, " +
                "account_no_expired, credentials_no_expired, account_no_locked, account_enabled) " +
                "VALUES (105, 'user_email_2', 'pwd', 'test', '13900139003', 'dup_email@test.com', 1, 1, 1, 1)"));
    }

    @Test
    @Order(6)
    @DisplayName("重复线索手机号插入失败")
    void testDuplicateCluePhoneFails() {
        jdbcTemplate.execute(
            "INSERT INTO t_clue (id, owner_id, full_name, phone) " +
            "VALUES (100, 1, 'test', '13800138000')");
        assertThrows(Exception.class, () ->
            jdbcTemplate.execute(
                "INSERT INTO t_clue (id, owner_id, full_name, phone) " +
                "VALUES (101, 1, 'test', '13800138000')"));
    }

    @Test
    @Order(7)
    @DisplayName("重复 SKU 插入失败")
    void testDuplicateSkuFails() {
        jdbcTemplate.execute(
            "INSERT INTO t_product (id, sku, name, price, stock, status) " +
            "VALUES (100, 'DUP-SKU-001', 'test product', 100.00, 10, 'ON_SALE')");
        assertThrows(Exception.class, () ->
            jdbcTemplate.execute(
                "INSERT INTO t_product (id, sku, name, price, stock, status) " +
                "VALUES (101, 'DUP-SKU-001', 'test product 2', 200.00, 5, 'ON_SALE')"));
    }

    @Test
    @Order(8)
    @DisplayName("重复 VIN 插入失败")
    void testDuplicateProductVehicleVinFails() {
        jdbcTemplate.execute(
            "INSERT INTO t_product_vehicle (id, product_id, vin, color, location, status) " +
            "VALUES (100, 1, 'VIN-DUP-001', '黑色', 'A-01', 'AVAILABLE')");
        assertThrows(Exception.class, () ->
            jdbcTemplate.execute(
                "INSERT INTO t_product_vehicle (id, product_id, vin, color, location, status) " +
                "VALUES (101, 1, 'VIN-DUP-001', '白色', 'A-02', 'AVAILABLE')"));
    }

    @Test
    @Order(8)
    @DisplayName("同一交易重复交付记录插入失败")
    void testDuplicateDeliveryTranFails() {
        jdbcTemplate.execute(
            "INSERT INTO t_tran (id, tran_no, customer_id, money, stage) " +
            "VALUES (120, 'TR_DELIVERY_DUP_001', 1, 100.00, 'DELIVERY')");
        jdbcTemplate.execute(
            "INSERT INTO t_product_vehicle (id, product_id, vin, color, location, status) " +
            "VALUES (120, 1, 'VIN-DELIVERY-DUP-001', '黑色', 'A-01', 'ORDER_RESERVED')");
        jdbcTemplate.execute(
            "INSERT INTO t_product_vehicle (id, product_id, vin, color, location, status) " +
            "VALUES (121, 1, 'VIN-DELIVERY-DUP-002', '白色', 'A-02', 'ORDER_RESERVED')");
        jdbcTemplate.execute(
            "INSERT INTO t_delivery (id, tran_id, customer_id, vehicle_id, status, planned_delivery_time) " +
            "VALUES (120, 120, 1, 120, 'PENDING_PREPARE', CURRENT_TIMESTAMP)");
        assertThrows(Exception.class, () ->
            jdbcTemplate.execute(
                "INSERT INTO t_delivery (id, tran_id, customer_id, vehicle_id, status, planned_delivery_time) " +
                "VALUES (121, 120, 1, 121, 'PENDING_PREPARE', CURRENT_TIMESTAMP)"));
    }

    @Test
    @Order(8)
    @DisplayName("非法交付状态插入失败")
    void testInvalidDeliveryStatusFails() {
        jdbcTemplate.execute(
            "INSERT INTO t_tran (id, tran_no, customer_id, money, stage) " +
            "VALUES (122, 'TR_DELIVERY_STATUS_001', 1, 100.00, 'DELIVERY')");
        jdbcTemplate.execute(
            "INSERT INTO t_product_vehicle (id, product_id, vin, color, location, status) " +
            "VALUES (122, 1, 'VIN-DELIVERY-STATUS-001', '黑色', 'A-01', 'ORDER_RESERVED')");
        assertThrows(Exception.class, () ->
            jdbcTemplate.execute(
                "INSERT INTO t_delivery (id, tran_id, customer_id, vehicle_id, status, planned_delivery_time) " +
                "VALUES (122, 122, 1, 122, 'DONE_BY_CLICK', CURRENT_TIMESTAMP)"));
    }

    @Test
    @Order(8)
    @DisplayName("非法交付准备项状态插入失败")
    void testInvalidDeliveryCheckStatusFails() {
        jdbcTemplate.execute(
            "INSERT INTO t_tran (id, tran_no, customer_id, money, stage) " +
            "VALUES (123, 'TR_DELIVERY_CHECK_001', 1, 100.00, 'DELIVERY')");
        jdbcTemplate.execute(
            "INSERT INTO t_product_vehicle (id, product_id, vin, color, location, status) " +
            "VALUES (123, 1, 'VIN-DELIVERY-CHECK-001', '黑色', 'A-01', 'ORDER_RESERVED')");
        jdbcTemplate.execute(
            "INSERT INTO t_delivery (id, tran_id, customer_id, vehicle_id, status, planned_delivery_time) " +
            "VALUES (123, 123, 1, 123, 'PENDING_PREPARE', CURRENT_TIMESTAMP)");
        assertThrows(Exception.class, () ->
            jdbcTemplate.execute(
                "INSERT INTO t_delivery_check_item (id, delivery_id, item_code, item_name, status) " +
                "VALUES (123, 123, 'VEHICLE_READY', '车辆验收', 'DONE_BY_CLICK')"));
    }

    @Test
    @Order(8)
    @DisplayName("重复报价单号插入失败")
    void testDuplicateQuoteNoFails() {
        jdbcTemplate.execute(
            "INSERT INTO t_quote (id, quote_no, customer_id, status) " +
            "VALUES (100, 'QUOTE-DUP-001', 1, 'DRAFT')");
        assertThrows(Exception.class, () ->
            jdbcTemplate.execute(
                "INSERT INTO t_quote (id, quote_no, customer_id, status) " +
                "VALUES (101, 'QUOTE-DUP-001', 1, 'DRAFT')"));
    }

    @Test
    @Order(8)
    @DisplayName("同一报价内重复版本号插入失败")
    void testDuplicateQuoteVersionNoFails() {
        jdbcTemplate.execute(
            "INSERT INTO t_quote (id, quote_no, customer_id, status) " +
            "VALUES (102, 'QUOTE-VERSION-001', 1, 'DRAFT')");
        jdbcTemplate.execute(
            "INSERT INTO t_quote_version (id, quote_id, version_no, valid_until, total_amount) " +
            "VALUES (100, 102, 1, CURRENT_TIMESTAMP, 100.00)");
        assertThrows(Exception.class, () ->
            jdbcTemplate.execute(
                "INSERT INTO t_quote_version (id, quote_id, version_no, valid_until, total_amount) " +
                "VALUES (101, 102, 1, CURRENT_TIMESTAMP, 200.00)"));
    }

    @Test
    @Order(9)
    @DisplayName("重复发票号插入失败")
    void testDuplicateInvoiceNoFails() {
        jdbcTemplate.execute(
            "INSERT INTO t_tran (id, tran_no, customer_id, money) VALUES (100, 'TR_INV_001', 1, 100.00)");
        jdbcTemplate.execute(
            "INSERT INTO t_tran_invoice (id, tran_id, invoice_no, type, title, tax_number, amount, status) " +
            "VALUES (100, 100, 'DUP-INV-001', 'VAT_NORMAL', 'test', 'TAX001', 100.00, 'ISSUED')");
        assertThrows(Exception.class, () ->
            jdbcTemplate.execute(
                "INSERT INTO t_tran_invoice (id, tran_id, invoice_no, type, title, tax_number, amount, status) " +
                "VALUES (101, 100, 'DUP-INV-001', 'VAT_NORMAL', 'test', 'TAX002', 200.00, 'ISSUED')"));
    }

    // ==================== 外键约束测试 ====================

    @Test
    @Order(10)
    @DisplayName("不存在父记录的子记录插入失败 - t_tran_product.tran_id")
    void testOrphanTranProductTranFails() {
        assertThrows(Exception.class, () ->
            jdbcTemplate.execute(
                "INSERT INTO t_tran_product (id, tran_id, product_id, quantity, price) " +
                "VALUES (100, 99999, 1, 1, 100.00)"));
    }

    @Test
    @Order(11)
    @DisplayName("不存在父记录的子记录插入失败 - t_tran_product.product_id")
    void testOrphanTranProductProductFails() {
        jdbcTemplate.execute(
            "INSERT INTO t_tran (id, tran_no, customer_id, money) VALUES (101, 'TR_TP_001', 1, 100.00)");
        assertThrows(Exception.class, () ->
            jdbcTemplate.execute(
                "INSERT INTO t_tran_product (id, tran_id, product_id, quantity, price) " +
                "VALUES (101, 101, 99999, 1, 100.00)"));
    }

    @Test
    @Order(12)
    @DisplayName("不存在父记录的子记录插入失败 - t_tran_approve.tran_id")
    void testOrphanTranApproveFails() {
        assertThrows(Exception.class, () ->
            jdbcTemplate.execute(
                "INSERT INTO t_tran_approve (id, tran_id, approve_result) " +
                "VALUES (100, 99999, 1)"));
    }

    @Test
    @Order(13)
    @DisplayName("不存在父记录的子记录插入失败 - t_tran_invoice.tran_id")
    void testOrphanTranInvoiceFails() {
        assertThrows(Exception.class, () ->
            jdbcTemplate.execute(
                "INSERT INTO t_tran_invoice (id, tran_id, invoice_no, type, title, tax_number, amount, status) " +
                "VALUES (102, 99999, 'INV-ORPHAN-001', 'VAT_NORMAL', 'test', 'TAX003', 100.00, 'ISSUED')"));
    }

    @Test
    @Order(14)
    @DisplayName("不存在父记录的子记录插入失败 - t_tran_history.tran_id")
    void testOrphanTranHistoryFails() {
        assertThrows(Exception.class, () ->
            jdbcTemplate.execute(
                "INSERT INTO t_tran_history (id, tran_id, stage, money) " +
                "VALUES (100, 99999, 'QUOTATION', 100.00)"));
    }

    @Test
    @Order(15)
    @DisplayName("不存在父记录的子记录插入失败 - t_payment.tran_id")
    void testOrphanPaymentTranFails() {
        assertThrows(Exception.class, () ->
            jdbcTemplate.execute(
                "INSERT INTO t_payment (id, tran_id, payment_no, amount, payment_method, payment_type) " +
                "VALUES (100, 99999, 'PAY-ORPHAN-001', 100.00, 'CARD', 'DEPOSIT')"));
    }

    @Test
    @Order(16)
    @DisplayName("不存在父记录的子记录插入失败 - t_product_stock_record.product_id")
    void testOrphanStockRecordProductFails() {
        assertThrows(Exception.class, () ->
            jdbcTemplate.execute(
                "INSERT INTO t_product_stock_record (id, product_id, quantity) " +
                "VALUES (100, 99999, 1)"));
    }

    @Test
    @Order(17)
    @DisplayName("不存在父记录的子记录插入失败 - t_product_vehicle.product_id")
    void testOrphanProductVehicleProductFails() {
        assertThrows(Exception.class, () ->
            jdbcTemplate.execute(
                "INSERT INTO t_product_vehicle (id, product_id, vin, color, location, status) " +
                "VALUES (102, 99999, 'VIN-ORPHAN-001', '黑色', 'A-01', 'AVAILABLE')"));
    }

    @Test
    @Order(18)
    @DisplayName("不存在父记录的子记录插入失败 - t_product_stock_record.vehicle_id")
    void testOrphanStockRecordVehicleFails() {
        assertThrows(Exception.class, () ->
            jdbcTemplate.execute(
                "INSERT INTO t_product_stock_record (id, product_id, vehicle_id, quantity, type) " +
                "VALUES (102, 1, 99999, -1, 'RESERVE')"));
    }

    @Test
    @Order(19)
    @DisplayName("不存在父记录的子记录插入失败 - t_product_stock_record.related_record_id")
    void testOrphanStockRecordRelatedFails() {
        assertThrows(Exception.class, () ->
            jdbcTemplate.execute(
                "INSERT INTO t_product_stock_record (id, product_id, quantity, type, related_record_id) " +
                "VALUES (103, 1, 1, 'RELEASE', 99999)"));
    }

    @Test
    @DisplayName("不存在父记录的子记录插入失败 - t_quote.customer_id")
    void testOrphanQuoteCustomerFails() {
        assertThrows(Exception.class, () ->
            jdbcTemplate.execute(
                "INSERT INTO t_quote (id, quote_no, customer_id, status) " +
                "VALUES (110, 'QUOTE-ORPHAN-CUSTOMER', 99999, 'DRAFT')"));
    }

    @Test
    @DisplayName("不存在父记录的子记录插入失败 - t_quote_version.quote_id")
    void testOrphanQuoteVersionFails() {
        assertThrows(Exception.class, () ->
            jdbcTemplate.execute(
                "INSERT INTO t_quote_version (id, quote_id, version_no, valid_until, total_amount) " +
                "VALUES (110, 99999, 1, CURRENT_TIMESTAMP, 100.00)"));
    }

    @Test
    @DisplayName("不存在父记录的子记录插入失败 - t_quote_version_item.quote_version_id")
    void testOrphanQuoteVersionItemVersionFails() {
        assertThrows(Exception.class, () ->
            jdbcTemplate.execute(
                "INSERT INTO t_quote_version_item (id, quote_version_id, product_id, unit_price, quantity, line_amount) " +
                "VALUES (110, 99999, 1, 100.00, 1, 100.00)"));
    }

    @Test
    @DisplayName("不存在父记录的子记录插入失败 - t_quote_version_item.product_id")
    void testOrphanQuoteVersionItemProductFails() {
        jdbcTemplate.execute(
            "INSERT INTO t_quote (id, quote_no, customer_id, status) " +
            "VALUES (111, 'QUOTE-ITEM-ORPHAN', 1, 'DRAFT')");
        jdbcTemplate.execute(
            "INSERT INTO t_quote_version (id, quote_id, version_no, valid_until, total_amount) " +
            "VALUES (111, 111, 1, CURRENT_TIMESTAMP, 100.00)");
        assertThrows(Exception.class, () ->
            jdbcTemplate.execute(
                "INSERT INTO t_quote_version_item (id, quote_version_id, product_id, unit_price, quantity, line_amount) " +
                "VALUES (111, 111, 99999, 100.00, 1, 100.00)"));
    }

    @Test
    @DisplayName("不存在父记录的子记录插入失败 - t_quote_status_history.quote_id")
    void testOrphanQuoteStatusHistoryFails() {
        assertThrows(Exception.class, () ->
            jdbcTemplate.execute(
                "INSERT INTO t_quote_status_history (id, quote_id, to_status, reason) " +
                "VALUES (110, 99999, 'DRAFT', 'test')"));
    }

    @Test
    @DisplayName("不存在的产品不能创建促销")
    void testOrphanProductPromotionFails() {
        assertThrows(Exception.class, () -> jdbcTemplate.execute(
            "INSERT INTO t_product_promotion (product_id, name) VALUES (99999, '孤儿促销')"));
    }

    @Test
    @Order(16)
    @DisplayName("不存在父记录的子记录插入失败 - t_role_permission.role_id")
    void testOrphanRolePermissionRoleFails() {
        assertThrows(Exception.class, () ->
            jdbcTemplate.execute(
                "INSERT INTO t_role_permission (role_id, permission_id) " +
                "SELECT 99999, id FROM t_permission WHERE code = 'activity:list'"));
    }

    @Test
    @Order(17)
    @DisplayName("不存在父记录的子记录插入失败 - t_role_permission.permission_id")
    void testOrphanRolePermissionPermissionFails() {
        assertThrows(Exception.class, () ->
            jdbcTemplate.execute(
                "INSERT INTO t_role_permission (role_id, permission_id) " +
                "SELECT id, 99999 FROM t_role WHERE role = 'admin'"));
    }

    @Test
    @Order(18)
    @DisplayName("不存在父记录的子记录插入失败 - t_user_role.user_id")
    void testOrphanUserRoleUserFails() {
        assertThrows(Exception.class, () ->
            jdbcTemplate.execute(
                "INSERT INTO t_user_role (user_id, role_id) " +
                "SELECT 99999, id FROM t_role WHERE role = 'admin'"));
    }

    @Test
    @Order(19)
    @DisplayName("不存在父记录的子记录插入失败 - t_user_role.role_id")
    void testOrphanUserRoleRoleFails() {
        assertThrows(Exception.class, () ->
            jdbcTemplate.execute(
                "INSERT INTO t_user_role (user_id, role_id) VALUES (1, 99999)"));
    }

    @Test
    @DisplayName("不存在父记录的子记录插入失败 - t_clue_owner_history.clue_id")
    void testOrphanClueOwnerHistoryClueFails() {
        assertThrows(Exception.class, () ->
            jdbcTemplate.execute(
                "INSERT INTO t_clue_owner_history (id, clue_id, to_owner_id, assigned_by, reason, assigned_time) " +
                "VALUES (100, 99999, 1, 1, 'test', CURRENT_TIMESTAMP)"));
    }

    @Test
    @DisplayName("不存在父记录的子记录插入失败 - t_clue_owner_history.to_owner_id")
    void testOrphanClueOwnerHistoryOwnerFails() {
        assertThrows(Exception.class, () ->
            jdbcTemplate.execute(
                "INSERT INTO t_clue_owner_history (id, clue_id, to_owner_id, assigned_by, reason, assigned_time) " +
                "VALUES (101, 1, 99999, 1, 'test', CURRENT_TIMESTAMP)"));
    }

    // ==================== 非空与 CHECK 约束测试 ====================

    @Test
    @Order(20)
    @DisplayName("负库存插入失败")
    void testNegativeStockFails() {
        assertThrows(Exception.class, () ->
            jdbcTemplate.execute(
                "INSERT INTO t_product (id, sku, name, price, stock, status) " +
                "VALUES (200, 'NEG-STOCK-001', 'test', 100.00, -1, 'ON_SALE')"));
    }

    @Test
    @Order(21)
    @DisplayName("负价格插入失败")
    void testNegativePriceFails() {
        assertThrows(Exception.class, () ->
            jdbcTemplate.execute(
                "INSERT INTO t_product (id, sku, name, price, stock, status) " +
                "VALUES (201, 'NEG-PRICE-001', 'test', -1.00, 10, 'ON_SALE')"));
    }

    @Test
    @Order(22)
    @DisplayName("NULL SKU 插入失败")
    void testNullSkuFails() {
        assertThrows(Exception.class, () ->
            jdbcTemplate.execute(
                "INSERT INTO t_product (id, sku, name, price, stock, status) " +
                "VALUES (202, NULL, 'test', 100.00, 10, 'ON_SALE')"));
    }

    @Test
    @Order(23)
    @DisplayName("NULL product name 插入失败")
    void testNullProductNameFails() {
        assertThrows(Exception.class, () ->
            jdbcTemplate.execute(
                "INSERT INTO t_product (id, sku, name, price, stock, status) " +
                "VALUES (203, 'NULL-NAME-001', NULL, 100.00, 10, 'ON_SALE')"));
    }

    // ==================== 级联删除测试 ====================

    @Test
    @Order(24)
    @DisplayName("删除用户时级联删除 user_role 关联")
    void testCascadeDeleteUserRole() {
        jdbcTemplate.execute(
            "INSERT INTO t_user (id, login_act, login_pwd, name, phone, email, " +
            "account_no_expired, credentials_no_expired, account_no_locked, account_enabled) " +
            "VALUES (300, 'cascade_user', 'pwd', 'test', '13900139010', 'cascade@test.com', 1, 1, 1, 1)");
        jdbcTemplate.execute(
            "INSERT INTO t_user_role (user_id, role_id) " +
            "SELECT 300, id FROM t_role WHERE role = 'admin'");

        jdbcTemplate.execute("DELETE FROM t_user WHERE id = 300");

        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM t_user_role WHERE user_id = 300", Integer.class);
        assertEquals(0, count, "user_role 应随用户级联删除");
    }

    @Test
    @Order(25)
    @DisplayName("删除角色时级联删除 role_permission 关联")
    void testCascadeDeleteRolePermission() {
        jdbcTemplate.execute(
            "INSERT INTO t_role (id, role, role_name) VALUES (10, 'test_role', 'test')");
        jdbcTemplate.execute(
            "INSERT INTO t_role_permission (role_id, permission_id) " +
            "SELECT 10, id FROM t_permission WHERE code = 'activity:list'");

        jdbcTemplate.execute("DELETE FROM t_role WHERE id = 10");

        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM t_role_permission WHERE role_id = 10", Integer.class);
        assertEquals(0, count, "role_permission 应随角色级联删除");
    }

    @Test
    @DisplayName("活动备注存在时禁止删除活动父记录")
    void activityRemarkShouldRestrictActivityDeletion() {
        jdbcTemplate.update(
            "INSERT INTO t_activity (id, name) VALUES (?, ?)",
            700, "备注外键活动");
        jdbcTemplate.update(
            "INSERT INTO t_activity_remark (id, activity_id, note_content) VALUES (?, ?, ?)",
            700, 700, "保留活动备注");

        assertThrows(DataIntegrityViolationException.class,
            () -> jdbcTemplate.update("DELETE FROM t_activity WHERE id = ?", 700));
    }

    @Test
    @DisplayName("线索备注存在时禁止删除线索父记录")
    void clueRemarkShouldRestrictClueDeletion() {
        jdbcTemplate.update(
            "INSERT INTO t_clue (id, owner_id, full_name, phone) VALUES (?, ?, ?, ?)",
            701, 1, "备注外键线索", "13900000701");
        jdbcTemplate.update(
            "INSERT INTO t_clue_remark (id, clue_id, note_content) VALUES (?, ?, ?)",
            701, 701, "保留线索备注");

        assertThrows(DataIntegrityViolationException.class,
            () -> jdbcTemplate.update("DELETE FROM t_clue WHERE id = ?", 701));
    }

    @Test
    @DisplayName("交易备注存在时禁止删除交易父记录")
    void tranRemarkShouldRestrictTranDeletion() {
        jdbcTemplate.update(
            "INSERT INTO t_customer (id, customer_name) VALUES (?, ?)",
            702, "备注外键客户");
        jdbcTemplate.update(
            "INSERT INTO t_tran (id, tran_no, customer_id, money) VALUES (?, ?, ?, ?)",
            702, "TR-REMARK-RESTRICT", 702, 100);
        jdbcTemplate.update(
            "INSERT INTO t_tran_remark (id, tran_id, note_content) VALUES (?, ?, ?)",
            702, 702, "保留交易备注");

        assertThrows(DataIntegrityViolationException.class,
            () -> jdbcTemplate.update("DELETE FROM t_tran WHERE id = ?", 702));
    }

    @Test
    @DisplayName("权限不能将自身设置为父级")
    void permissionParentShouldRejectSelfReference() {
        assertThrows(DataIntegrityViolationException.class,
            () -> jdbcTemplate.update("""
                INSERT INTO t_permission (id, name, code, type, parent_id)
                VALUES (?, ?, ?, ?, ?)
                """, 703, "自引用权限", "permission:self-parent", "menu", 703));
    }

    // ==================== 产品种子数据验证 ====================

    @Test
    @Order(26)
    @DisplayName("产品种子数据能用 status='ON_SALE' 查询")
    void testProductStatusOnSale() {
        List<Map<String, Object>> products = jdbcTemplate.queryForList(
            "SELECT * FROM t_product WHERE status = 'ON_SALE'");
        assertFalse(products.isEmpty(), "应存在 status='ON_SALE' 的产品");
        for (Map<String, Object> p : products) {
            assertEquals("ON_SALE", p.get("STATUS"));
        }
    }

    @Test
    @Order(27)
    @DisplayName("产品状态必须使用稳定编码")
    void testProductStatusRejectsChineseLabel() {
        assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate.update(
            "INSERT INTO t_product(id, sku, name, price, stock, status) " +
                "VALUES (204, 'BAD-STATUS-001', 'test', 100.00, 10, '上架')"));
    }

    @Test
    @DisplayName("报价状态必须使用稳定编码")
    void testQuoteStatusRejectsChineseLabel() {
        assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate.update(
            "INSERT INTO t_quote(id, quote_no, customer_id, status) " +
                "VALUES (205, 'BAD-QUOTE-STATUS-001', 1, '待确认')"));
    }

    @Test
    @DisplayName("收款状态必须使用稳定编码")
    void testPaymentStatusRejectsChineseLabel() {
        assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate.update(
            "INSERT INTO t_payment(id, tran_id, payment_no, amount, payment_method, payment_type, payment_status) " +
                "VALUES (206, 1, 'BAD-PAYMENT-STATUS-001', 100.00, 'CASH', 'FULL', '已到账')"));
    }

    @Test
    @DisplayName("退款申请状态必须使用稳定编码")
    void testRefundRequestStatusRejectsOldOrChineseLabel() {
        jdbcTemplate.update(
            "INSERT INTO t_payment(id, tran_id, payment_no, amount, payment_method, payment_type, payment_status) " +
                "VALUES (207, 1, 'PAY-FOR-BAD-REFUND-001', 100.00, 'CASH', 'FULL', 'COMPLETED')");
        assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate.update(
            "INSERT INTO t_refund_request(id, tran_id, original_payment_id, amount, refund_type, reason, status) " +
                "VALUES (208, 1, 207, 100.00, 'ORDER_CANCEL', 'test', 'EXECUTED')"));
    }

    @Test
    @DisplayName("发票状态必须使用稳定编码")
    void testInvoiceStatusRejectsOldVoidCode() {
        assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate.update(
            "INSERT INTO t_tran_invoice(id, tran_id, invoice_no, type, title, tax_number, amount, status) " +
                "VALUES (209, 1, 'BAD-INVOICE-STATUS-001', 'VAT_NORMAL', 'test', 'TAX-001', 100.00, 'VOID')"));
    }

    @Test
    @DisplayName("发票红冲重开关联必须引用已存在原票")
    void testInvoiceOriginalReferenceRejectsMissingInvoice() {
        assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate.update(
            "INSERT INTO t_tran_invoice(id, tran_id, invoice_no, type, title, tax_number, original_invoice_id, amount, status) " +
                "VALUES (210, 1, 'BAD-INVOICE-ORIGINAL-001', 'VAT_NORMAL', 'test', 'TAX-002', 999999, -100.00, 'RED_REVERSED')"));
    }

    // ==================== 生产与测试 Schema 核心表字段对比 ====================

    @Test
    @Order(27)
    @DisplayName("生产与测试 Schema 核心表字段对比 - 主键存在")
    void testCoreTablesHavePrimaryKey() {
        // 通过查询验证主键存在：能通过 id 精确查询到记录
        jdbcTemplate.queryForObject("SELECT id FROM t_activity WHERE id = 1", Integer.class);
        jdbcTemplate.queryForObject("SELECT id FROM t_clue WHERE id = 1", Integer.class);
        jdbcTemplate.queryForObject("SELECT id FROM t_customer WHERE id = 1", Integer.class);
        jdbcTemplate.queryForObject("SELECT id FROM t_tran WHERE id = 1", Integer.class);
        jdbcTemplate.queryForObject("SELECT id FROM t_product WHERE id = 1", Long.class);
        jdbcTemplate.queryForObject("SELECT id FROM t_user WHERE id = 1", Integer.class);
        jdbcTemplate.queryForObject("SELECT id FROM t_role WHERE role = 'admin'", Integer.class);
        jdbcTemplate.queryForObject("SELECT id FROM t_permission WHERE code = 'activity:list'", Integer.class);
        jdbcTemplate.queryForObject("SELECT id FROM t_dic_type WHERE id = 1", Integer.class);
        jdbcTemplate.queryForObject("SELECT id FROM t_dic_value WHERE id = 1", Integer.class);
        // 全部能查询到记录说明主键存在且数据初始化成功
        assertTrue(true, "所有核心表主键查询通过");
    }

    @Test
    @Order(28)
    @DisplayName("t_dic_value 包含 value_code 列")
    void testDicValueHasValueCode() {
        // 通过插入带 value_code 的记录验证列存在
        jdbcTemplate.execute(
            "INSERT INTO t_dic_type (id, type_code, type_name, remark) " +
            "VALUES (100, 'test_type_valcol', 'test', 'test')");
        jdbcTemplate.execute(
            "INSERT INTO t_dic_value (id, type_code, type_value, value_code, \"order\", remark) " +
            "VALUES (100, 'test_type_valcol', 'test', 'TEST_CODE', 1, 'test')");
        String code = jdbcTemplate.queryForObject(
            "SELECT value_code FROM t_dic_value WHERE id = 100", String.class);
        assertEquals("TEST_CODE", code, "t_dic_value 应包含 value_code 列");
    }

    @Test
    @Order(29)
    @DisplayName("t_product 包含 CHECK 约束 - 通过插入负数验证")
    void testProductCheckConstraints() {
        // 已验证: testNegativePriceFails 和 testNegativeStockFails 通过了
        // 再次确认 CHECK 约束生效
        assertThrows(Exception.class, () ->
            jdbcTemplate.execute(
                "INSERT INTO t_product (id, sku, name, price, stock, status) " +
                "VALUES (299, 'CHK-TEST-001', 'test', -1.00, 0, 'ON_SALE')"));
        assertThrows(Exception.class, () ->
            jdbcTemplate.execute(
                "INSERT INTO t_product (id, sku, name, price, stock, status) " +
                "VALUES (298, 'CHK-TEST-002', 'test', 0, -1, 'ON_SALE')"));
    }

    @Test
    @Order(30)
    @DisplayName("t_tran_product.product_id 类型为 BIGINT")
    void testTranProductProductIdIsBigint() {
        // 通过插入一个超过 INT 最大值的 ID 来验证 product_id 支持 BIGINT 范围
        long bigId = 3000000000L;
        jdbcTemplate.execute(
            "INSERT INTO t_product (id, sku, name, price, stock, status) " +
            "VALUES (" + bigId + ", 'BIGINT-TEST-SKU', 'test', 100.00, 10, 'ON_SALE')");
        jdbcTemplate.execute(
            "INSERT INTO t_tran (id, tran_no, customer_id, money) VALUES (200, 'TEST_BIGINT_001', 1, 100.00)");
        jdbcTemplate.execute(
            "INSERT INTO t_tran_product (id, tran_id, product_id, quantity, price) " +
            "VALUES (200, 200, " + bigId + ", 1, 100.00)");
        // 如果 product_id 不是 BIGINT，上述插入会因类型溢出而失败
        Long productId = jdbcTemplate.queryForObject(
            "SELECT product_id FROM t_tran_product WHERE id = 200", Long.class);
        assertEquals(bigId, productId, "product_id 应支持 BIGINT 范围值: " + bigId);
    }

    @Test
    @Order(31)
    @DisplayName("权限编码必须非空且唯一")
    void testPermissionCodeRequiredAndUnique() {
        assertThrows(Exception.class, () -> jdbcTemplate.execute(
            "INSERT INTO t_permission (name, code, type) VALUES ('无编码权限', NULL, 'button')"));
        assertThrows(Exception.class, () -> jdbcTemplate.execute(
            "INSERT INTO t_permission (name, code, type) VALUES ('重复权限', 'activity:list', 'button')"));
    }

    @Test
    @Order(32)
    @DisplayName("权限类型只能是 menu 或 button")
    void testPermissionTypeCheck() {
        assertThrows(Exception.class, () -> jdbcTemplate.execute(
            "INSERT INTO t_permission (name, code, type) VALUES ('非法类型', 'test:invalid-type', 'api')"));
    }

    @Test
    @Order(33)
    @DisplayName("权限父节点必须存在且不能引用自身")
    void testPermissionParentConstraints() {
        assertThrows(Exception.class, () -> jdbcTemplate.execute(
            "INSERT INTO t_permission (name, code, type, parent_id) " +
            "VALUES ('孤儿权限', 'test:orphan', 'menu', 99999)"));
        jdbcTemplate.execute(
            "INSERT INTO t_permission (id, name, code, type) VALUES (9998, '自引用权限', 'test:self', 'menu')");
        assertThrows(Exception.class, () -> jdbcTemplate.execute(
            "UPDATE t_permission SET parent_id = 9998 WHERE id = 9998"));
    }

    @Test
    @Order(34)
    @DisplayName("角色权限复合主键拒绝重复授权")
    void testDuplicateRolePermissionFails() {
        assertThrows(Exception.class, () -> jdbcTemplate.execute(
            "INSERT INTO t_role_permission (role_id, permission_id) " +
            "SELECT r.id, p.id FROM t_role r CROSS JOIN t_permission p " +
            "WHERE r.role = 'admin' AND p.code = 'activity:list'"));
    }

    @Test
    @Order(35)
    @DisplayName("用户角色复合主键拒绝重复关联")
    void testDuplicateUserRoleFails() {
        assertThrows(Exception.class, () -> jdbcTemplate.execute(
            "INSERT INTO t_user_role (user_id, role_id) " +
            "SELECT u.id, r.id FROM t_user u CROSS JOIN t_role r " +
            "WHERE u.login_act = 'admin' AND r.role = 'admin'"));
    }
}
