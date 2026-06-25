package com.autodealer.crm.integration;

import com.autodealer.crm.model.TCustomer;
import com.autodealer.crm.model.TTranProduct;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DataLayerContractTest {

    private static final Path PROJECT_ROOT = Paths.get(System.getProperty("user.dir")).getParent();
    private static final Path PRODUCTION_SCHEMA = PROJECT_ROOT.resolve(
            "dealer-server/src/main/resources/CarDealerCRM.sql");
    private static final Path H2_SCHEMA = PROJECT_ROOT.resolve(
            "dealer-server/src/main/resources/schema-test.sql");
    private static final Path TRAN_PRODUCT_MAPPER = PROJECT_ROOT.resolve(
            "dealer-server/src/main/resources/mapper/TTranProductMapper.xml");
    private static final Pattern CREATE_TABLE = Pattern.compile(
            "(?is)CREATE\\s+TABLE(?:\\s+IF\\s+NOT\\s+EXISTS)?\\s+`?([a-zA-Z0-9_]+)`?\\s*\\((.*?)\\)\\s*(?:ENGINE\\s*=.*?)?(?=;)");
    private static final Pattern COLUMN = Pattern.compile(
            "^\\s*`?([a-zA-Z_][a-zA-Z0-9_]*)`?\\s+[a-zA-Z]", Pattern.MULTILINE);
    private static final Set<String> NON_COLUMNS = Set.of(
            "PRIMARY", "CONSTRAINT", "UNIQUE", "FOREIGN", "KEY", "CHECK", "INDEX");

    @Test
    @DisplayName("生产与H2的所有表字段集合必须一致")
    void productionAndH2ColumnsMustMatch() throws IOException {
        Map<String, Set<String>> production = extractColumns(Files.readString(PRODUCTION_SCHEMA));
        Map<String, Set<String>> h2 = extractColumns(Files.readString(H2_SCHEMA));

        assertEquals(production.keySet(), h2.keySet(), "生产与H2表集合存在漂移");
        for (String table : production.keySet()) {
            assertEquals(production.get(table), h2.get(table), table + " 字段集合存在漂移");
        }
    }

    @Test
    @DisplayName("生产与H2必须共同声明核心唯一约束")
    void productionAndH2UniqueConstraintsMustMatchCoreBusinessKeys() throws IOException {
        String productionSql = normalizeSql(Files.readString(PRODUCTION_SCHEMA));
        String h2Sql = normalizeSql(Files.readString(H2_SCHEMA));

        assertSqlContainsAll(productionSql,
                "UNIQUE KEY `uk_clue_phone` (`phone`)",
                "UNIQUE KEY `uk_tran_no` (`tran_no`)",
                "UNIQUE KEY `uk_tran_id` (`tran_id`)",
                "UNIQUE INDEX `uk_payment_transaction_ref` (`transaction_ref` ASC)",
                "UNIQUE INDEX `uk_payment_idempotency_key` (`idempotency_key` ASC)");
        assertSqlContainsAll(h2Sql,
                "CONSTRAINT uk_clue_phone UNIQUE (phone)",
                "CONSTRAINT uk_tran_no UNIQUE (tran_no)",
                "CONSTRAINT uk_tran_id UNIQUE (tran_id)",
                "CONSTRAINT uk_payment_transaction_ref UNIQUE (transaction_ref)",
                "CONSTRAINT uk_payment_idempotency_key UNIQUE (idempotency_key)");
    }

    @Test
    @DisplayName("生产与H2必须共同声明核心外键约束")
    void productionAndH2ForeignKeysMustMatchCoreBusinessReferences() throws IOException {
        String productionSql = normalizeSql(Files.readString(PRODUCTION_SCHEMA));
        String h2Sql = normalizeSql(Files.readString(H2_SCHEMA));

        assertSqlContainsAll(productionSql,
                "CONSTRAINT `fk_customer_clue` FOREIGN KEY (`clue_id`) REFERENCES `t_clue` (`id`) ON DELETE RESTRICT",
                "CONSTRAINT `fk_tran_customer` FOREIGN KEY (`customer_id`) REFERENCES `t_customer` (`id`) ON DELETE RESTRICT",
                "CONSTRAINT `fk_tran_history_tran` FOREIGN KEY (`tran_id`) REFERENCES `t_tran` (`id`) ON DELETE RESTRICT",
                "CONSTRAINT `fk_tran_product_tran` FOREIGN KEY (`tran_id`) REFERENCES `t_tran` (`id`) ON DELETE RESTRICT",
                "CONSTRAINT `fk_tran_product_product` FOREIGN KEY (`product_id`) REFERENCES `t_product` (`id`) ON DELETE RESTRICT",
                "CONSTRAINT `fk_tran_invoice_tran` FOREIGN KEY (`tran_id`) REFERENCES `t_tran` (`id`) ON DELETE RESTRICT",
                "CONSTRAINT `fk_tran_approve_tran` FOREIGN KEY (`tran_id`) REFERENCES `t_tran` (`id`) ON DELETE RESTRICT",
                "CONSTRAINT `fk_payment_tran` FOREIGN KEY (`tran_id`) REFERENCES `t_tran` (`id`) ON DELETE RESTRICT",
                "CONSTRAINT `fk_stock_record_product` FOREIGN KEY (`product_id`) REFERENCES `t_product` (`id`) ON DELETE RESTRICT");
        assertSqlContainsAll(h2Sql,
                "CONSTRAINT fk_customer_clue FOREIGN KEY (clue_id) REFERENCES t_clue(id) ON DELETE RESTRICT",
                "CONSTRAINT fk_tran_customer FOREIGN KEY (customer_id) REFERENCES t_customer(id) ON DELETE RESTRICT",
                "CONSTRAINT fk_tran_history_tran FOREIGN KEY (tran_id) REFERENCES t_tran(id) ON DELETE RESTRICT",
                "CONSTRAINT fk_tran_product_tran FOREIGN KEY (tran_id) REFERENCES t_tran(id) ON DELETE RESTRICT",
                "CONSTRAINT fk_tran_product_product FOREIGN KEY (product_id) REFERENCES t_product(id) ON DELETE RESTRICT",
                "CONSTRAINT fk_tran_invoice_tran FOREIGN KEY (tran_id) REFERENCES t_tran(id) ON DELETE RESTRICT",
                "CONSTRAINT fk_tran_approve_tran FOREIGN KEY (tran_id) REFERENCES t_tran(id) ON DELETE RESTRICT",
                "CONSTRAINT fk_payment_tran FOREIGN KEY (tran_id) REFERENCES t_tran(id) ON DELETE RESTRICT",
                "CONSTRAINT fk_stock_record_product FOREIGN KEY (product_id) REFERENCES t_product(id) ON DELETE RESTRICT");
    }

    @Test
    @DisplayName("BIGINT产品外键在Java和MyBatis中必须使用Long/BIGINT")
    void productForeignKeysMustUseBigintAcrossLayers() throws Exception {
        assertEquals(Long.class, TTranProduct.class.getDeclaredField("productId").getType());
        assertEquals(Long.class, TCustomer.class.getDeclaredField("product").getType());
        String mapper = Files.readString(TRAN_PRODUCT_MAPPER);
        assertFalse(mapper.contains("productId,jdbcType=INTEGER"));
    }

    @Test
    @DisplayName("生产商品种子必须使用业务识别的on_sale状态")
    void productionProductStatusMustUseStableCode() throws IOException {
        String productionSql = Files.readString(PRODUCTION_SCHEMA);
        assertFalse(productionSql.contains(", '上架',"), "生产商品种子仍使用中文展示值作为状态编码");
    }

    private Map<String, Set<String>> extractColumns(String sql) {
        Map<String, Set<String>> result = new LinkedHashMap<>();
        Matcher tableMatcher = CREATE_TABLE.matcher(sql);
        while (tableMatcher.find()) {
            Set<String> columns = new LinkedHashSet<>();
            Matcher columnMatcher = COLUMN.matcher(tableMatcher.group(2));
            while (columnMatcher.find()) {
                String column = columnMatcher.group(1);
                if (!NON_COLUMNS.contains(column.toUpperCase())) {
                    columns.add(column.toLowerCase());
                }
            }
            result.put(tableMatcher.group(1).toLowerCase(), columns);
        }
        return result;
    }

    private String normalizeSql(String sql) {
        return sql.replaceAll("\\s+", " ").trim();
    }

    private void assertSqlContainsAll(String sql, String... fragments) {
        for (String fragment : fragments) {
            assertTrue(sql.contains(normalizeSql(fragment)), "缺少约束片段: " + fragment);
        }
    }
}
