package com.autodealer.crm.integration;

import com.autodealer.crm.modules.fulfillment.transaction.persistence.mapper.TTranMapper;
import com.autodealer.crm.modules.fulfillment.transaction.persistence.mapper.TTranProductMapper;
import com.autodealer.crm.modules.sales.activity.persistence.mapper.TActivityMapper;
import com.autodealer.crm.modules.sales.lead.persistence.mapper.TClueMapper;
import com.autodealer.crm.modules.sales.customer.application.api.model.TCustomer;
import com.autodealer.crm.modules.fulfillment.transaction.application.api.model.TTranProduct;
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
    private static final Path H2_DATA = PROJECT_ROOT.resolve(
            "dealer-server/src/main/resources/data.sql");
    private static final Path TASK09_MIGRATION = PROJECT_ROOT.resolve(
            "dealer-server/src/main/resources/migration/20260711_task09_organization_foundation.sql");
    private static final Path TASK03_MIGRATION = PROJECT_ROOT.resolve(
            "dealer-server/src/main/resources/migration/20260711_task03_auth_version.sql");
    private static final Path TASK10_MIGRATION = PROJECT_ROOT.resolve(
            "dealer-server/src/main/resources/migration/20260711_task10_authorization_history.sql");
    private static final Path TRAN_PRODUCT_MAPPER = PROJECT_ROOT.resolve(
            "dealer-server/src/main/resources/mapper/fulfillment/transaction/TTranProductMapper.xml");
    private static final Path TRAN_MAPPER = PROJECT_ROOT.resolve(
            "dealer-server/src/main/resources/mapper/fulfillment/transaction/TTranMapper.xml");
    private static final Path CLUE_MAPPER = PROJECT_ROOT.resolve(
            "dealer-server/src/main/resources/mapper/sales/lead/TClueMapper.xml");
    private static final Path ACTIVITY_MAPPER = PROJECT_ROOT.resolve(
            "dealer-server/src/main/resources/mapper/sales/activity/TActivityMapper.xml");
    private static final Path MAPPER_DIR = PROJECT_ROOT.resolve(
            "dealer-server/src/main/resources/mapper");
    private static final Pattern CREATE_TABLE = Pattern.compile(
            "(?is)CREATE\\s+TABLE(?:\\s+IF\\s+NOT\\s+EXISTS)?\\s+`?([a-zA-Z0-9_]+)`?\\s*\\((.*?)\\)\\s*(?:ENGINE\\s*=.*?)?(?=;)");
    private static final Pattern COLUMN = Pattern.compile(
            "^\\s*`?([a-zA-Z_][a-zA-Z0-9_]*)`?\\s+[a-zA-Z]", Pattern.MULTILINE);
    private static final Set<String> NON_COLUMNS = Set.of(
            "PRIMARY", "CONSTRAINT", "UNIQUE", "FOREIGN", "KEY", "CHECK", "INDEX", "REFERENCES");

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
    @DisplayName("生产与H2必须共同声明组织员工核心约束")
    void productionAndH2MustDeclareOrganizationFoundationConstraints() throws IOException {
        String productionSql = normalizeSql(Files.readString(PRODUCTION_SCHEMA));
        String h2Sql = normalizeSql(Files.readString(H2_SCHEMA));

        assertSqlContainsAll(productionSql,
                "UNIQUE KEY `uk_organization_unit_code` (`code`)",
                "UNIQUE KEY `uk_employee_user` (`user_id`)",
                "UNIQUE KEY `uk_employee_no` (`employee_no`)",
                "UNIQUE KEY `uk_employee_active_primary` (`employee_id`, `active_primary_marker`)",
                "UNIQUE KEY `uk_employee_active_direct_manager` (`subordinate_employee_id`, `active_direct_marker`)",
                "CONSTRAINT `chk_employee_status` CHECK (`employment_status` IN ('PENDING', 'ACTIVE', 'HANDOVER', 'LEFT'))",
                "CONSTRAINT `fk_employee_assignment_org` FOREIGN KEY (`organization_unit_id`) REFERENCES `t_organization_unit` (`id`) ON DELETE RESTRICT",
                "CONSTRAINT `chk_employee_reporting_not_self` CHECK (`subordinate_employee_id` <> `manager_employee_id`)");
        assertSqlContainsAll(h2Sql,
                "CONSTRAINT uk_organization_unit_code UNIQUE (code)",
                "CONSTRAINT uk_employee_user UNIQUE (user_id)",
                "CONSTRAINT uk_employee_no UNIQUE (employee_no)",
                "CONSTRAINT uk_employee_active_primary UNIQUE (employee_id, active_primary_marker)",
                "CONSTRAINT uk_employee_active_direct_manager UNIQUE (subordinate_employee_id, active_direct_marker)",
                "CONSTRAINT chk_employee_status CHECK (employment_status IN ('PENDING', 'ACTIVE', 'HANDOVER', 'LEFT'))",
                "CONSTRAINT fk_employee_assignment_org FOREIGN KEY (organization_unit_id) REFERENCES t_organization_unit(id) ON DELETE RESTRICT",
                "CONSTRAINT chk_employee_reporting_not_self CHECK (subordinate_employee_id <> manager_employee_id)");
    }

    @Test
    @DisplayName("认证安全版本在生产、H2和人工迁移中保持一致")
    void authVersionSchemaAndMigrationMustRemainCompatible() throws IOException {
        String productionSql = normalizeSql(Files.readString(PRODUCTION_SCHEMA));
        String h2Sql = normalizeSql(Files.readString(H2_SCHEMA));
        String migrationSql = normalizeSql(Files.readString(TASK03_MIGRATION));
        String lowerMigrationSql = migrationSql.toLowerCase();

        assertSqlContainsAll(productionSql,
                "auth_version           bigint      not null default 0",
                "constraint chk_user_auth_version check (auth_version >= 0)");
        assertSqlContainsAll(h2Sql,
                "auth_version           BIGINT NOT NULL DEFAULT 0",
                "CONSTRAINT chk_user_auth_version CHECK (auth_version >= 0)");
        assertTrue(lowerMigrationSql.contains("information_schema.columns"));
        assertTrue(lowerMigrationSql.contains("add column auth_version bigint not null default 0"));
        assertFalse(lowerMigrationSql.contains("update t_user set login_pwd"));
        assertFalse(lowerMigrationSql.contains("delete from t_user_role"));
    }

    @Test
    @DisplayName("Task09人工迁移必须保持新增回填和占位组织边界")
    void task09MigrationMustBeAdditiveAndMarkPlaceholderScope() throws IOException {
        String migrationSql = normalizeSql(Files.readString(TASK09_MIGRATION));
        String lowerMigrationSql = migrationSql.toLowerCase();

        assertTrue(lowerMigrationSql.contains("information_schema.columns"),
                "t_user 兼容字段必须按存在性判断后新增");
        assertTrue(lowerMigrationSql.contains("create table if not exists t_organization_unit"),
                "迁移必须以可重复执行方式新增组织表");
        assertTrue(lowerMigrationSql.contains("migration_placeholder"),
                "待分配组织必须有明确迁移占位标识");
        assertFalse(lowerMigrationSql.contains("default_store"),
                "迁移不得伪造默认门店");
        assertFalse(lowerMigrationSql.contains("rehired"),
                "返聘是动作而不是员工持久状态");
        assertTrue(lowerMigrationSql.contains("where u.account_type = 'human'"),
                "只允许普通人员账号回填员工档案");
        assertTrue(migrationSql.contains("SIGNAL SQLSTATE '45000'"),
                "恢复账号和占位种子漂移必须主动中止迁移");
        assertTrue(migrationSql.contains("未找到固定恢复账号 id=1/login_act=admin"),
                "必须保留恢复账号身份校验");
        assertTrue(migrationSql.contains("DEFAULT_COMPANY与预期根公司不一致"));
        assertTrue(migrationSql.contains("UNASSIGNED_ORG与预期占位组织不一致"));
        assertTrue(migrationSql.contains("UNASSIGNED_POSITION与预期占位岗位不一致"));
        assertFalse(lowerMigrationSql.contains("update t_user set login_pwd"),
                "迁移禁止修改原用户密码");
        assertFalse(lowerMigrationSql.contains("delete from t_user_role"),
                "迁移禁止改变原用户角色关系");
        assertFalse(lowerMigrationSql.contains("drop column"),
                "兼容迁移禁止删除旧字段");
        assertTrue(lowerMigrationSql.contains("task09_validate_recovery_account"));
        assertTrue(lowerMigrationSql.contains("task09_validate_seed_objects"));
        assertTrue(lowerMigrationSql.contains("signal sqlstate '45000'"));
    }

    @Test
    @DisplayName("Task10迁移重跑不得覆盖后续角色和权限矩阵配置")
    void task10MigrationBackfillMustOnlyRunForNewColumns() throws IOException {
        String productionSql = normalizeSql(Files.readString(PRODUCTION_SCHEMA));
        String h2Sql = normalizeSql(Files.readString(H2_SCHEMA));
        String h2DataSql = normalizeSql(Files.readString(H2_DATA));
        String migrationSql = normalizeSql(Files.readString(TASK10_MIGRATION));
        String lowerMigrationSql = migrationSql.toLowerCase();

        assertSqlContainsAll(productionSql,
                "CREATE TABLE `t_user_management_migration`",
                "'20260711_task10_authorization_history'");
        assertSqlContainsAll(h2Sql,
                "CREATE TABLE IF NOT EXISTS t_user_management_migration");
        assertSqlContainsAll(h2DataSql,
                "'20260711_task03_auth_version'",
                "'20260711_task09_organization_foundation'",
                "'20260711_task10_authorization_history'");
        assertTrue(lowerMigrationSql.contains(
                "call crm_require_migration_context('20260711_task10_authorization_history')"));
        assertTrue(lowerMigrationSql.contains("@task10_backfill_required"));
        assertTrue(lowerMigrationSql.contains(
                "from t_user_management_migration_step where migration_key='20260711_task10_authorization_history'"));
        assertTrue(lowerMigrationSql.contains(
                "step_code='first_run_compatibility_backfill_ready'"));
        assertTrue(lowerMigrationSql.contains(
                "call crm_migration_mark_step('20260711_task10_authorization_history', 'first_run_compatibility_backfill_ready')"));
    }

    @Test
    @DisplayName("生产与H2必须共同声明权限父级自引用保护")
    void productionAndH2PermissionSelfParentProtectionMustMatch() throws IOException {
        String productionSql = normalizeSql(Files.readString(PRODUCTION_SCHEMA));
        String h2Sql = normalizeSql(Files.readString(H2_SCHEMA));

        assertFalse(productionSql.contains("chk_permission_parent_self"),
                "生产 MySQL Schema 不能用 CHECK 引用 AUTO_INCREMENT id");
        assertSqlContainsAll(productionSql,
                "CREATE TRIGGER `trg_permission_no_self_parent_bi` BEFORE INSERT ON `t_permission`",
                "CREATE TRIGGER `trg_permission_no_self_parent_bu` BEFORE UPDATE ON `t_permission`",
                "IF NEW.`parent_id` IS NOT NULL AND NEW.`parent_id` = NEW.`id` THEN",
                "SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'permission parent cannot reference itself'");
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
