package com.bjpowernode.web;

import com.bjpowernode.result.NameValue;
import com.bjpowernode.result.SummaryData;
import com.bjpowernode.service.StatisticService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@WithMockUser(authorities = {"statistic:view"})
class StatisticControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StatisticService statisticService;

    @Test
    void summaryData_returnsSummaryData() throws Exception {
        SummaryData summaryData = SummaryData.builder()
                .effectiveActivityCount(10)
                .totalActivityCount(20)
                .totalClueCount(100)
                .totalCustomerCount(50)
                .successTranAmount(new BigDecimal("50000"))
                .totalTranAmount(new BigDecimal("100000"))
                .build();

        when(statisticService.loadSummaryData()).thenReturn(summaryData);

        mockMvc.perform(get("/api/summary/data"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.effectiveActivityCount").value(10))
                .andExpect(jsonPath("$.data.totalClueCount").value(100));
    }

    @Test
    void saleFunnelData_returnsNameValueList() throws Exception {
        List<NameValue> nameValueList = Arrays.asList(
                NameValue.builder().name("成交").value(20).build(),
                NameValue.builder().name("交易").value(60).build(),
                NameValue.builder().name("客户").value(80).build(),
                NameValue.builder().name("线索").value(100).build()
        );

        when(statisticService.loadSaleFunnelData()).thenReturn(nameValueList);

        mockMvc.perform(get("/api/saleFunnel/data"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.length()").value(4))
                .andExpect(jsonPath("$.data[0].name").value("成交"));
    }

    @Test
    void saleFunnelData_emptyList() throws Exception {
        when(statisticService.loadSaleFunnelData()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/saleFunnel/data"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void sourcePieData_returnsNameValueList() throws Exception {
        List<NameValue> nameValueList = Arrays.asList(
                NameValue.builder().name("Search Engine").value(1048).build(),
                NameValue.builder().name("Direct").value(735).build(),
                NameValue.builder().name("Email").value(580).build()
        );

        when(statisticService.loadSourcePieData()).thenReturn(nameValueList);

        mockMvc.perform(get("/api/sourcePie/data"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.data[0].name").value("Search Engine"));
    }

    @Test
    void sourcePieData_emptyList() throws Exception {
        when(statisticService.loadSourcePieData()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/sourcePie/data"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void summaryData_withZeroValues() throws Exception {
        SummaryData summaryData = SummaryData.builder()
                .effectiveActivityCount(0)
                .totalActivityCount(0)
                .totalClueCount(0)
                .totalCustomerCount(0)
                .successTranAmount(BigDecimal.ZERO)
                .totalTranAmount(BigDecimal.ZERO)
                .build();

        when(statisticService.loadSummaryData()).thenReturn(summaryData);

        mockMvc.perform(get("/api/summary/data"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.effectiveActivityCount").value(0));
    }
}
