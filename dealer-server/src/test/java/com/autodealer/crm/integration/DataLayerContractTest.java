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
}
