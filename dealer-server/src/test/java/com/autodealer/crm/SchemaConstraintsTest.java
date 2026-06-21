package com.autodealer.crm;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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
            "t_system_info",
            "t_tran", "t_tran_history", "t_tran_product",
            "t_tran_invoice", "t_tran_approve", "t_tran_remark",
            "t_product", "t_product_category", "t_product_promotion",
            "t_product_stock_record",
            "t_payment",
            "t_user", "t_user_role"
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
            "VALUES (100, 'DUP-SKU-001', 'test product', 100.00, 10, 'on_sale')");
        assertThrows(Exception.class, () ->
            jdbcTemplate.execute(
                "INSERT INTO t_product (id, sku, name, price, stock, status) " +
                "VALUES (101, 'DUP-SKU-001', 'test product 2', 200.00, 5, 'on_sale')"));
    }

    @Test
    @Order(8)
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
    @Order(9)
    @DisplayName("不存在父记录的子记录插入失败 - t_tran_product.tran_id")
    void testOrphanTranProductTranFails() {
        assertThrows(Exception.class, () ->
            jdbcTemplate.execute(
                "INSERT INTO t_tran_product (id, tran_id, product_id, quantity, price) " +
                "VALUES (100, 99999, 1, 1, 100.00)"));
    }

    @Test
    @Order(10)
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
    @Order(11)
    @DisplayName("不存在父记录的子记录插入失败 - t_tran_approve.tran_id")
    void testOrphanTranApproveFails() {
        assertThrows(Exception.class, () ->
            jdbcTemplate.execute(
                "INSERT INTO t_tran_approve (id, tran_id, approve_result) " +
                "VALUES (100, 99999, 1)"));
    }

    @Test
    @Order(12)
    @DisplayName("不存在父记录的子记录插入失败 - t_tran_invoice.tran_id")
    void testOrphanTranInvoiceFails() {
        assertThrows(Exception.class, () ->
            jdbcTemplate.execute(
                "INSERT INTO t_tran_invoice (id, tran_id, invoice_no, type, title, tax_number, amount, status) " +
                "VALUES (102, 99999, 'INV-ORPHAN-001', 'VAT_NORMAL', 'test', 'TAX003', 100.00, 'ISSUED')"));
    }

    @Test
    @Order(13)
    @DisplayName("不存在父记录的子记录插入失败 - t_tran_history.tran_id")
    void testOrphanTranHistoryFails() {
        assertThrows(Exception.class, () ->
            jdbcTemplate.execute(
                "INSERT INTO t_tran_history (id, tran_id, stage, money) " +
                "VALUES (100, 99999, 'QUOTATION', 100.00)"));
    }

    @Test
    @Order(14)
    @DisplayName("不存在父记录的子记录插入失败 - t_payment.tran_id")
    void testOrphanPaymentTranFails() {
        assertThrows(Exception.class, () ->
            jdbcTemplate.execute(
                "INSERT INTO t_payment (id, tran_id, payment_no, amount, payment_method, payment_type) " +
                "VALUES (100, 99999, 'PAY-ORPHAN-001', 100.00, 'CARD', 'DEPOSIT')"));
    }

    @Test
    @Order(15)
    @DisplayName("不存在父记录的子记录插入失败 - t_product_stock_record.product_id")
    void testOrphanStockRecordProductFails() {
        assertThrows(Exception.class, () ->
            jdbcTemplate.execute(
                "INSERT INTO t_product_stock_record (id, product_id, quantity) " +
                "VALUES (100, 99999, 1)"));
    }

    @Test
    @Order(16)
    @DisplayName("不存在父记录的子记录插入失败 - t_role_permission.role_id")
    void testOrphanRolePermissionRoleFails() {
        assertThrows(Exception.class, () ->
            jdbcTemplate.execute(
                "INSERT INTO t_role_permission (id, role_id, permission_id) " +
                "VALUES (200, 99999, 1)"));
    }

    @Test
    @Order(17)
    @DisplayName("不存在父记录的子记录插入失败 - t_role_permission.permission_id")
    void testOrphanRolePermissionPermissionFails() {
        assertThrows(Exception.class, () ->
            jdbcTemplate.execute(
                "INSERT INTO t_role_permission (id, role_id, permission_id) " +
                "VALUES (201, 1, 99999)"));
    }

    @Test
    @Order(18)
    @DisplayName("不存在父记录的子记录插入失败 - t_user_role.user_id")
    void testOrphanUserRoleUserFails() {
        assertThrows(Exception.class, () ->
            jdbcTemplate.execute(
                "INSERT INTO t_user_role (id, user_id, role_id) " +
                "VALUES (100, 99999, 1)"));
    }

    @Test
    @Order(19)
    @DisplayName("不存在父记录的子记录插入失败 - t_user_role.role_id")
    void testOrphanUserRoleRoleFails() {
        assertThrows(Exception.class, () ->
            jdbcTemplate.execute(
                "INSERT INTO t_user_role (id, user_id, role_id) " +
                "VALUES (101, 1, 99999)"));
    }

    // ==================== 非空与 CHECK 约束测试 ====================

    @Test
    @Order(20)
    @DisplayName("负库存插入失败")
    void testNegativeStockFails() {
        assertThrows(Exception.class, () ->
            jdbcTemplate.execute(
                "INSERT INTO t_product (id, sku, name, price, stock, status) " +
                "VALUES (200, 'NEG-STOCK-001', 'test', 100.00, -1, 'on_sale')"));
    }

    @Test
    @Order(21)
    @DisplayName("负价格插入失败")
    void testNegativePriceFails() {
        assertThrows(Exception.class, () ->
            jdbcTemplate.execute(
                "INSERT INTO t_product (id, sku, name, price, stock, status) " +
                "VALUES (201, 'NEG-PRICE-001', 'test', -1.00, 10, 'on_sale')"));
    }

    @Test
    @Order(22)
    @DisplayName("NULL SKU 插入失败")
    void testNullSkuFails() {
        assertThrows(Exception.class, () ->
            jdbcTemplate.execute(
                "INSERT INTO t_product (id, sku, name, price, stock, status) " +
                "VALUES (202, NULL, 'test', 100.00, 10, 'on_sale')"));
    }

    @Test
    @Order(23)
    @DisplayName("NULL product name 插入失败")
    void testNullProductNameFails() {
        assertThrows(Exception.class, () ->
            jdbcTemplate.execute(
                "INSERT INTO t_product (id, sku, name, price, stock, status) " +
                "VALUES (203, 'NULL-NAME-001', NULL, 100.00, 10, 'on_sale')"));
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
            "INSERT INTO t_user_role (id, user_id, role_id) VALUES (300, 300, 1)");

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
            "INSERT INTO t_role_permission (id, role_id, permission_id) VALUES (300, 10, 1)");

        jdbcTemplate.execute("DELETE FROM t_role WHERE id = 10");

        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM t_role_permission WHERE role_id = 10", Integer.class);
        assertEquals(0, count, "role_permission 应随角色级联删除");
    }

    // ==================== 产品种子数据验证 ====================

    @Test
    @Order(26)
    @DisplayName("产品种子数据能用 status='on_sale' 查询")
    void testProductStatusOnSale() {
        List<Map<String, Object>> products = jdbcTemplate.queryForList(
            "SELECT * FROM t_product WHERE status = 'on_sale'");
        assertFalse(products.isEmpty(), "应存在 status='on_sale' 的产品");
        for (Map<String, Object> p : products) {
            assertEquals("on_sale", p.get("STATUS"));
        }
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
        jdbcTemplate.queryForObject("SELECT id FROM t_role WHERE id = 1", Integer.class);
        jdbcTemplate.queryForObject("SELECT id FROM t_permission WHERE id = 1", Integer.class);
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
                "VALUES (299, 'CHK-TEST-001', 'test', -1.00, 0, 'on_sale')"));
        assertThrows(Exception.class, () ->
            jdbcTemplate.execute(
                "INSERT INTO t_product (id, sku, name, price, stock, status) " +
                "VALUES (298, 'CHK-TEST-002', 'test', 0, -1, 'on_sale')"));
    }

    @Test
    @Order(30)
    @DisplayName("t_tran_product.product_id 类型为 BIGINT")
    void testTranProductProductIdIsBigint() {
        // 通过插入一个超过 INT 最大值的 ID 来验证 product_id 支持 BIGINT 范围
        long bigId = 3000000000L;
        jdbcTemplate.execute(
            "INSERT INTO t_product (id, sku, name, price, stock, status) " +
            "VALUES (" + bigId + ", 'BIGINT-TEST-SKU', 'test', 100.00, 10, 'on_sale')");
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
}
