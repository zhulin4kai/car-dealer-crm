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
    private static final Path TRAN_MAPPER = PROJECT_ROOT.resolve(
            "dealer-server/src/main/resources/mapper/TTranMapper.xml");
    private static final Path CLUE_MAPPER = PROJECT_ROOT.resolve(
            "dealer-server/src/main/resources/mapper/TClueMapper.xml");
    private static final Path ACTIVITY_MAPPER = PROJECT_ROOT.resolve(
            "dealer-server/src/main/resources/mapper/TActivityMapper.xml");
    private static final Path MAPPER_DIR = PROJECT_ROOT.resolve(
            "dealer-server/src/main/resources/mapper");
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
                "CONSTRAINT `fk_clue_owner_history_clue` FOREIGN KEY (`clue_id`) REFERENCES `t_clue` (`id`) ON DELETE RESTRICT",
                "CONSTRAINT `fk_clue_owner_history_to_owner` FOREIGN KEY (`to_owner_id`) REFERENCES `t_user` (`id`) ON DELETE RESTRICT",
                "CONSTRAINT `fk_tran_customer` FOREIGN KEY (`customer_id`) REFERENCES `t_customer` (`id`) ON DELETE RESTRICT",
                "CONSTRAINT `fk_tran_history_tran` FOREIGN KEY (`tran_id`) REFERENCES `t_tran` (`id`) ON DELETE RESTRICT",
                "CONSTRAINT `fk_tran_product_tran` FOREIGN KEY (`tran_id`) REFERENCES `t_tran` (`id`) ON DELETE RESTRICT",
                "CONSTRAINT `fk_tran_product_product` FOREIGN KEY (`product_id`) REFERENCES `t_product` (`id`) ON DELETE RESTRICT",
                "CONSTRAINT `fk_tran_invoice_tran` FOREIGN KEY (`tran_id`) REFERENCES `t_tran` (`id`) ON DELETE RESTRICT",
                "CONSTRAINT `fk_tran_approve_tran` FOREIGN KEY (`tran_id`) REFERENCES `t_tran` (`id`) ON DELETE RESTRICT",
                "CONSTRAINT `fk_payment_tran` FOREIGN KEY (`tran_id`) REFERENCES `t_tran` (`id`) ON DELETE RESTRICT",
                "CONSTRAINT `fk_stock_record_product` FOREIGN KEY (`product_id`) REFERENCES `t_product` (`id`) ON DELETE RESTRICT",
                "CONSTRAINT `fk_activity_remark_activity` FOREIGN KEY (`activity_id`) REFERENCES `t_activity` (`id`) ON DELETE RESTRICT",
                "CONSTRAINT `fk_clue_remark_clue` FOREIGN KEY (`clue_id`) REFERENCES `t_clue` (`id`) ON DELETE RESTRICT",
                "CONSTRAINT `fk_tran_remark_tran` FOREIGN KEY (`tran_id`) REFERENCES `t_tran` (`id`) ON DELETE RESTRICT");
        assertSqlContainsAll(h2Sql,
                "CONSTRAINT fk_customer_clue FOREIGN KEY (clue_id) REFERENCES t_clue(id) ON DELETE RESTRICT",
                "CONSTRAINT fk_clue_owner_history_clue FOREIGN KEY (clue_id) REFERENCES t_clue(id) ON DELETE RESTRICT",
                "CONSTRAINT fk_clue_owner_history_to_owner FOREIGN KEY (to_owner_id) REFERENCES t_user(id) ON DELETE RESTRICT",
                "CONSTRAINT fk_tran_customer FOREIGN KEY (customer_id) REFERENCES t_customer(id) ON DELETE RESTRICT",
                "CONSTRAINT fk_tran_history_tran FOREIGN KEY (tran_id) REFERENCES t_tran(id) ON DELETE RESTRICT",
                "CONSTRAINT fk_tran_product_tran FOREIGN KEY (tran_id) REFERENCES t_tran(id) ON DELETE RESTRICT",
                "CONSTRAINT fk_tran_product_product FOREIGN KEY (product_id) REFERENCES t_product(id) ON DELETE RESTRICT",
                "CONSTRAINT fk_tran_invoice_tran FOREIGN KEY (tran_id) REFERENCES t_tran(id) ON DELETE RESTRICT",
                "CONSTRAINT fk_tran_approve_tran FOREIGN KEY (tran_id) REFERENCES t_tran(id) ON DELETE RESTRICT",
                "CONSTRAINT fk_payment_tran FOREIGN KEY (tran_id) REFERENCES t_tran(id) ON DELETE RESTRICT",
                "CONSTRAINT fk_stock_record_product FOREIGN KEY (product_id) REFERENCES t_product(id) ON DELETE RESTRICT",
                "CONSTRAINT fk_activity_remark_activity FOREIGN KEY (activity_id) REFERENCES t_activity(id) ON DELETE RESTRICT",
                "CONSTRAINT fk_clue_remark_clue FOREIGN KEY (clue_id) REFERENCES t_clue(id) ON DELETE RESTRICT",
                "CONSTRAINT fk_tran_remark_tran FOREIGN KEY (tran_id) REFERENCES t_tran(id) ON DELETE RESTRICT");
    }

    @Test
    @DisplayName("生产与H2必须共同声明权限父级自引用保护")
    void productionAndH2PermissionSelfParentCheckMustMatch() throws IOException {
        String productionSql = normalizeSql(Files.readString(PRODUCTION_SCHEMA));
        String h2Sql = normalizeSql(Files.readString(H2_SCHEMA));

        assertSqlContainsAll(productionSql,
                "CONSTRAINT `chk_permission_parent_self` CHECK (`parent_id` IS NULL OR `parent_id` <> `id`)");
        assertSqlContainsAll(h2Sql,
                "CONSTRAINT chk_permission_parent_self CHECK (parent_id IS NULL OR parent_id <> id)");
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

    @Test
    @DisplayName("经营分析交易数必须按交易记录统计，成交客户数才按客户去重")
    void statisticTranCountSqlMustMatchMetricDefinitions() throws IOException {
        String mapper = normalizeSql(Files.readString(TRAN_MAPPER)).toLowerCase();

        assertTrue(mapper.contains("<select id=\"selectbytotaltrancount\" resulttype=\"java.lang.integer\"> select count(t.id)"),
                "交易数必须按 t_tran 记录数统计，不能按客户去重");
        assertTrue(mapper.contains("<select id=\"selectbysuccesstrancount\" resulttype=\"java.lang.integer\"> select count(distinct t.customer_id)"),
                "成交客户数必须按完成交易客户去重统计");
    }

    @Test
    @DisplayName("分页列表必须包含稳定唯一排序键")
    void pagedListQueriesMustUseStableOrdering() throws IOException {
        assertTrue(normalizeSql(Files.readString(TRAN_MAPPER)).toLowerCase()
                        .contains("order by t.create_time desc, t.id desc"),
                "交易列表必须在 create_time 后追加 id desc");
        assertTrue(normalizeSql(Files.readString(CLUE_MAPPER)).toLowerCase()
                        .contains("order by tc.create_time desc, tc.id desc"),
                "线索列表必须在 create_time 后追加 id desc");
        assertTrue(normalizeSql(Files.readString(ACTIVITY_MAPPER)).toLowerCase()
                        .contains("order by ta.start_time desc, ta.id desc"),
                "活动列表必须在 start_time 后追加 id desc");
    }

    @Test
    @DisplayName("交易列表不得固定JOIN子表和依赖DISTINCT去重")
    void tranListQueryMustUseConditionalExistsForChildFilters() throws IOException {
        String mapper = normalizeSql(Files.readString(TRAN_MAPPER)).toLowerCase();
        String selectByQuery = extractXmlStatement(mapper, "selectbyquery");

        assertFalse(selectByQuery.contains("select distinct"), "交易列表不应依赖 DISTINCT 去重");
        assertFalse(selectByQuery.contains("left join t_tran_product"), "商品筛选应使用 EXISTS，不能固定 JOIN 商品行");
        assertFalse(selectByQuery.contains("left join t_tran_invoice"), "发票筛选应使用 EXISTS，不能固定 JOIN 发票行");
        assertTrue(selectByQuery.contains("exists ( select 1 from t_tran_product"),
                "商品筛选必须使用条件 EXISTS");
        assertTrue(selectByQuery.contains("exists ( select 1 from t_tran_invoice"),
                "发票筛选必须使用条件 EXISTS");
    }

    @Test
    @DisplayName("交易列表搜索必须使用可控前缀搜索")
    void tranListSearchMustUsePrefixSearch() throws IOException {
        String selectByQuery = extractXmlStatement(
                normalizeSql(Files.readString(TRAN_MAPPER)).toLowerCase(), "selectbyquery");

        assertFalse(selectByQuery.contains("like concat('%'"),
                "交易列表不得使用前后通配符模糊搜索");
        assertTrue(selectByQuery.contains("t.tran_no like concat(#{tranno}, '%')"),
                "交易号必须使用前缀搜索");
        assertTrue(selectByQuery.contains("c.customer_name like concat(#{customername}, '%')"),
                "客户名必须使用前缀搜索");
        assertTrue(selectByQuery.contains("tp.product_name like concat(#{productname}, '%')"),
                "商品名必须使用前缀搜索");
    }

    @Test
    @DisplayName("Mapper XML 禁止使用不受控 ${} 占位符")
    void mapperXmlMustNotUseRawTextSubstitution() throws IOException {
        try (var paths = Files.walk(MAPPER_DIR)) {
            for (Path mapper : paths.filter(path -> path.toString().endsWith(".xml")).toList()) {
                assertFalse(Files.readString(mapper).contains("${}"),
                        mapper + " 存在空 ${} 占位符");
                assertFalse(Files.readString(mapper).contains("${"),
                        mapper + " 禁止使用不受控 ${} 占位符");
            }
        }
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

    private String extractXmlStatement(String xml, String statementId) {
        Pattern pattern = Pattern.compile("(?is)<select\\s+id=\"" + Pattern.quote(statementId)
                + "\".*?</select>");
        Matcher matcher = pattern.matcher(xml);
        assertTrue(matcher.find(), "缺少 Mapper 语句: " + statementId);
        return matcher.group();
    }
}
