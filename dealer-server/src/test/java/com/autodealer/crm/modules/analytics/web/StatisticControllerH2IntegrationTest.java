package com.autodealer.crm.modules.analytics.web;

import com.autodealer.crm.integration.BackendIntegrationTestBase;
import tools.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class StatisticControllerH2IntegrationTest extends BackendIntegrationTestBase {

    @Test
    @DisplayName("sales manager dashboard summary only includes rows in the same data scope as details")
    void summaryData_salesManager_shouldNotExposeGlobalCounts() throws Exception {
        String token = loginAs("lisi", "123456", 3);

        MvcResult result = mockMvc.perform(get("/api/summary/data")
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8)).path("data");
        assertEquals(0, data.path("totalActivityCount").asInt());
        assertEquals(1, data.path("totalClueCount").asInt());
        assertEquals(0, data.path("totalCustomerCount").asInt());
        assertEquals(0, data.path("successTranAmount").decimalValue().compareTo(BigDecimal.ZERO));
        assertEquals(0, data.path("totalTranAmount").decimalValue().compareTo(BigDecimal.ZERO));
    }

    @Test
    @DisplayName("sales manager funnel and source charts are calculated from the same scoped rows")
    void chartData_salesManager_shouldNotExposeGlobalCounts() throws Exception {
        String token = loginAs("lisi", "123456", 3);

        JsonNode funnel = objectMapper.readTree(mockMvc.perform(get("/api/saleFunnel/data")
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8)).path("data");
        assertEquals(1, funnel.get(0).path("value").asInt());
        assertEquals(0, funnel.get(1).path("value").asInt());
        assertEquals(0, funnel.get(2).path("value").asInt());
        assertEquals(0, funnel.get(3).path("value").asInt());

        JsonNode source = objectMapper.readTree(mockMvc.perform(get("/api/sourcePie/data")
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8)).path("data");
        assertEquals(1, source.size());
        assertEquals("员工介绍", source.get(0).path("name").asText());
        assertEquals(1, source.get(0).path("value").asInt());
    }
}
