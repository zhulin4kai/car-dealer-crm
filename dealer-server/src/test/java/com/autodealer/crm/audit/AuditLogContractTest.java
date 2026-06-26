package com.autodealer.crm.audit;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AuditLogContractTest {

    @Test
    void schemasAndSeeds_shouldDeclareAuditLogFactsAndPermissions() throws Exception {
        String testSchema = Files.readString(Path.of("src/main/resources/schema-test.sql"));
        String productionSchema = Files.readString(Path.of("src/main/resources/CarDealerCRM.sql"));
        String seedData = Files.readString(Path.of("src/main/resources/data.sql"));

        assertAuditSql(testSchema);
        assertAuditSql(productionSchema);
        assertTrue(seedData.contains("menu:audit"));
        assertTrue(seedData.contains("audit:login:list"));
        assertTrue(seedData.contains("audit:operation:list"));
        assertTrue(seedData.contains("audit:login:export"));
        assertTrue(seedData.contains("audit:operation:export"));
    }

    private void assertAuditSql(String sql) {
        assertTrue(sql.contains("t_login_log"));
        assertTrue(sql.contains("login_act"));
        assertTrue(sql.contains("reason_code"));
        assertTrue(sql.contains("request_id"));
        assertTrue(sql.contains("object_type"));
        assertTrue(sql.contains("idx_login_log_time"));
        assertTrue(sql.contains("idx_operation_log_time"));
    }
}
