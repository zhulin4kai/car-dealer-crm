package com.bjpowernode.service;

import com.bjpowernode.manager.StatisticManager;
import com.bjpowernode.result.NameValue;
import com.bjpowernode.result.SummaryData;
import com.bjpowernode.service.impl.StatisticServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StatisticServiceImplTest {

    @InjectMocks
    private StatisticServiceImpl statisticService;

    @Mock
    private StatisticManager statisticManager;

    @Test
    void testLoadSummaryData() {
        SummaryData summaryData = SummaryData.builder()
                .effectiveActivityCount(5)
                .totalActivityCount(10)
                .totalClueCount(100)
                .totalCustomerCount(50)
                .successTranAmount(BigDecimal.valueOf(500000))
                .totalTranAmount(BigDecimal.valueOf(1000000))
                .build();
        when(statisticManager.loadSummaryData()).thenReturn(summaryData);

        SummaryData result = statisticService.loadSummaryData();

        assertNotNull(result);
        assertEquals(5, result.getEffectiveActivityCount());
        assertEquals(10, result.getTotalActivityCount());
        assertEquals(100, result.getTotalClueCount());
        assertEquals(50, result.getTotalCustomerCount());
        assertEquals(BigDecimal.valueOf(500000), result.getSuccessTranAmount());
        assertEquals(BigDecimal.valueOf(1000000), result.getTotalTranAmount());
    }

    @Test
    void testLoadSummaryDataWithNullValues() {
        SummaryData summaryData = SummaryData.builder()
                .effectiveActivityCount(0)
                .totalActivityCount(0)
                .totalClueCount(0)
                .totalCustomerCount(0)
                .successTranAmount(BigDecimal.ZERO)
                .totalTranAmount(BigDecimal.ZERO)
                .build();
        when(statisticManager.loadSummaryData()).thenReturn(summaryData);

        SummaryData result = statisticService.loadSummaryData();

        assertNotNull(result);
        assertEquals(0, result.getEffectiveActivityCount());
        assertEquals(BigDecimal.ZERO, result.getSuccessTranAmount());
    }

    @Test
    void testLoadSaleFunnelData() {
        List<NameValue> funnelData = Arrays.asList(
                NameValue.builder().name("线索").value(100).build(),
                NameValue.builder().name("客户").value(50).build(),
                NameValue.builder().name("交易").value(20).build(),
                NameValue.builder().name("成交").value(10).build()
        );
        when(statisticManager.loadSaleFunnelData()).thenReturn(funnelData);

        List<NameValue> result = statisticService.loadSaleFunnelData();

        assertNotNull(result);
        assertEquals(4, result.size());
        assertEquals("线索", result.get(0).getName());
        assertEquals(100, result.get(0).getValue());
        assertEquals("成交", result.get(3).getName());
        assertEquals(10, result.get(3).getValue());
    }

    @Test
    void testLoadSaleFunnelDataEmpty() {
        when(statisticManager.loadSaleFunnelData()).thenReturn(Collections.emptyList());

        List<NameValue> result = statisticService.loadSaleFunnelData();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testLoadSourcePieData() {
        List<NameValue> sourceData = Arrays.asList(
                NameValue.builder().name("网站").value(30).build(),
                NameValue.builder().name("电话").value(20).build(),
                NameValue.builder().name("转介绍").value(15).build()
        );
        when(statisticManager.loadSourcePieData()).thenReturn(sourceData);

        List<NameValue> result = statisticService.loadSourcePieData();

        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals("网站", result.get(0).getName());
        assertEquals(30, result.get(0).getValue());
    }

    @Test
    void testLoadSourcePieDataEmpty() {
        when(statisticManager.loadSourcePieData()).thenReturn(Collections.emptyList());

        List<NameValue> result = statisticService.loadSourcePieData();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}
