package com.bjpowernode.manager;

import com.bjpowernode.mapper.TActivityMapper;
import com.bjpowernode.mapper.TClueMapper;
import com.bjpowernode.mapper.TCustomerMapper;
import com.bjpowernode.mapper.TTranMapper;
import com.bjpowernode.model.TActivity;
import com.bjpowernode.result.NameValue;
import com.bjpowernode.result.SummaryData;
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
class StatisticManagerTest {

    @InjectMocks
    private StatisticManager statisticManager;

    @Mock
    private TActivityMapper tActivityMapper;

    @Mock
    private TClueMapper tClueMapper;

    @Mock
    private TCustomerMapper tCustomerMapper;

    @Mock
    private TTranMapper tTranMapper;

    @Test
    void testLoadSummaryData() {
        TActivity activity = new TActivity();
        when(tActivityMapper.selecOngoingActivity()).thenReturn(Collections.singletonList(activity));
        when(tActivityMapper.selectByCount()).thenReturn(10);
        when(tClueMapper.selectClueByCount()).thenReturn(100);
        when(tCustomerMapper.selectByCount()).thenReturn(50);
        when(tTranMapper.selectBySuccessTranAmount()).thenReturn(new BigDecimal("500000"));
        when(tTranMapper.selectByTotalTranAmount()).thenReturn(new BigDecimal("1000000"));

        SummaryData result = statisticManager.loadSummaryData();

        assertNotNull(result);
        assertEquals(1, result.getEffectiveActivityCount());
        assertEquals(10, result.getTotalActivityCount());
        assertEquals(100, result.getTotalClueCount());
        assertEquals(50, result.getTotalCustomerCount());
        assertEquals(new BigDecimal("500000"), result.getSuccessTranAmount());
        assertEquals(new BigDecimal("1000000"), result.getTotalTranAmount());
    }

    @Test
    void testLoadSaleFunnelData() {
        when(tClueMapper.selectClueByCount()).thenReturn(100);
        when(tCustomerMapper.selectByCount()).thenReturn(50);
        when(tTranMapper.selectByTotalTranCount()).thenReturn(30);
        when(tTranMapper.selectBySuccessTranCount()).thenReturn(10);

        List<NameValue> result = statisticManager.loadSaleFunnelData();

        assertNotNull(result);
        assertEquals(4, result.size());
        assertEquals("线索", result.get(0).getName());
        assertEquals(100, result.get(0).getValue());
        assertEquals("客户", result.get(1).getName());
        assertEquals(50, result.get(1).getValue());
        assertEquals("交易", result.get(2).getName());
        assertEquals(30, result.get(2).getValue());
        assertEquals("成交", result.get(3).getName());
        assertEquals(10, result.get(3).getValue());
    }

    @Test
    void testLoadSourcePieData() {
        NameValue source1 = NameValue.builder().name("车展会").value(30).build();
        NameValue source2 = NameValue.builder().name("网络广告").value(70).build();
        when(tClueMapper.selectBySource()).thenReturn(Arrays.asList(source1, source2));

        List<NameValue> result = statisticManager.loadSourcePieData();

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("车展会", result.get(0).getName());
        assertEquals("网络广告", result.get(1).getName());
    }
}
