package com.autodealer.crm.manager;

import com.autodealer.crm.enums.TranStage;
import com.autodealer.crm.config.security.CurrentUserProvider;
import com.autodealer.crm.mapper.TActivityMapper;
import com.autodealer.crm.mapper.TClueMapper;
import com.autodealer.crm.mapper.TCustomerMapper;
import com.autodealer.crm.mapper.TTranMapper;
import com.autodealer.crm.model.TActivity;
import com.autodealer.crm.result.NameValue;
import com.autodealer.crm.result.SummaryData;
import org.junit.jupiter.api.BeforeEach;
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

    @Mock
    private CurrentUserProvider currentUserProvider;

    @BeforeEach
    void setUpDefaultScope() {
        lenient().when(currentUserProvider.getDataScopeUserId()).thenReturn(null);
        lenient().when(currentUserProvider.getTransactionDataScope())
                .thenReturn(CurrentUserProvider.TransactionDataScope.all());
    }

    @Test
    void testLoadSummaryData() {
        TActivity activity = new TActivity();
        when(tActivityMapper.selecOngoingActivity(null)).thenReturn(Collections.singletonList(activity));
        when(tActivityMapper.selectByCount(null)).thenReturn(10);
        when(tClueMapper.selectClueByCount(null)).thenReturn(100);
        when(tCustomerMapper.selectByCount(null)).thenReturn(50);
        when(tTranMapper.selectBySuccessTranAmount(TranStage.COMPLETED, true, null)).thenReturn(new BigDecimal("500000"));
        when(tTranMapper.selectByTotalTranAmount(true, null)).thenReturn(new BigDecimal("1000000"));

        SummaryData result = statisticManager.loadSummaryData();

        assertNotNull(result);
        assertEquals(1, result.getEffectiveActivityCount());
        assertEquals(10, result.getTotalActivityCount());
        assertEquals(100, result.getTotalClueCount());
        assertEquals(50, result.getTotalCustomerCount());
        assertEquals(new BigDecimal("500000"), result.getSuccessTranAmount());
        assertEquals(new BigDecimal("1000000"), result.getTotalTranAmount());
        verify(tTranMapper).selectBySuccessTranAmount(TranStage.COMPLETED, true, null);
    }

    @Test
    void loadSummaryData_shouldUseCurrentUserDataScope() {
        when(currentUserProvider.getDataScopeUserId()).thenReturn(7);
        when(currentUserProvider.getTransactionDataScope())
                .thenReturn(CurrentUserProvider.TransactionDataScope.limited(7, false, List.of()));
        when(tActivityMapper.selecOngoingActivity(7)).thenReturn(Collections.emptyList());
        when(tActivityMapper.selectByCount(7)).thenReturn(2);
        when(tClueMapper.selectClueByCount(7)).thenReturn(3);
        when(tCustomerMapper.selectByCount(7)).thenReturn(1);
        when(tTranMapper.selectBySuccessTranAmount(TranStage.COMPLETED, false, 7)).thenReturn(BigDecimal.ZERO);
        when(tTranMapper.selectByTotalTranAmount(false, 7)).thenReturn(new BigDecimal("120000"));

        SummaryData result = statisticManager.loadSummaryData();

        assertNotNull(result);
        assertEquals(0, result.getEffectiveActivityCount());
        assertEquals(2, result.getTotalActivityCount());
        assertEquals(3, result.getTotalClueCount());
        assertEquals(1, result.getTotalCustomerCount());
        assertEquals(BigDecimal.ZERO, result.getSuccessTranAmount());
        assertEquals(new BigDecimal("120000"), result.getTotalTranAmount());
    }

    @Test
    void loadSummaryData_shouldDefaultEmptyScopedAmountsToZero() {
        when(currentUserProvider.getTransactionDataScope())
                .thenReturn(CurrentUserProvider.TransactionDataScope.limited(7, false, List.of()));
        when(currentUserProvider.getDataScopeUserId()).thenReturn(7);
        when(tActivityMapper.selecOngoingActivity(7)).thenReturn(Collections.emptyList());
        when(tActivityMapper.selectByCount(7)).thenReturn(0);
        when(tClueMapper.selectClueByCount(7)).thenReturn(0);
        when(tCustomerMapper.selectByCount(7)).thenReturn(0);
        when(tTranMapper.selectBySuccessTranAmount(TranStage.COMPLETED, false, 7)).thenReturn(null);
        when(tTranMapper.selectByTotalTranAmount(false, 7)).thenReturn(null);

        SummaryData result = statisticManager.loadSummaryData();

        assertEquals(BigDecimal.ZERO, result.getSuccessTranAmount());
        assertEquals(BigDecimal.ZERO, result.getTotalTranAmount());
    }

    @Test
    void testLoadSaleFunnelData() {
        when(tClueMapper.selectClueByCount(null)).thenReturn(100);
        when(tCustomerMapper.selectByCount(null)).thenReturn(50);
        when(tTranMapper.selectByTotalTranCount(true, null)).thenReturn(30);
        when(tTranMapper.selectBySuccessTranCount(TranStage.COMPLETED, true, null)).thenReturn(10);

        List<NameValue> result = statisticManager.loadSaleFunnelData();

        assertNotNull(result);
        assertEquals(4, result.size());
        assertEquals("线索", result.get(0).getName());
        assertEquals(100, result.get(0).getValue());
        assertEquals("客户", result.get(1).getName());
        assertEquals(50, result.get(1).getValue());
        assertEquals("交易", result.get(2).getName());
        assertEquals(30, result.get(2).getValue());
        assertEquals("成交客户", result.get(3).getName());
        assertEquals(10, result.get(3).getValue());
        verify(tTranMapper).selectBySuccessTranCount(TranStage.COMPLETED, true, null);
    }

    @Test
    void loadSaleFunnelData_shouldUseCurrentUserDataScope() {
        when(currentUserProvider.getDataScopeUserId()).thenReturn(7);
        when(currentUserProvider.getTransactionDataScope())
                .thenReturn(CurrentUserProvider.TransactionDataScope.limited(7, false, List.of()));
        when(tClueMapper.selectClueByCount(7)).thenReturn(3);
        when(tCustomerMapper.selectByCount(7)).thenReturn(2);
        when(tTranMapper.selectByTotalTranCount(false, 7)).thenReturn(1);
        when(tTranMapper.selectBySuccessTranCount(TranStage.COMPLETED, false, 7)).thenReturn(0);

        List<NameValue> result = statisticManager.loadSaleFunnelData();

        assertEquals(3, result.get(0).getValue());
        assertEquals(2, result.get(1).getValue());
        assertEquals(1, result.get(2).getValue());
        assertEquals(0, result.get(3).getValue());
    }

    @Test
    void testLoadSourcePieData() {
        NameValue source1 = NameValue.builder().name("车展会").value(30).build();
        NameValue source2 = NameValue.builder().name("网络广告").value(70).build();
        when(tClueMapper.selectBySource(null)).thenReturn(Arrays.asList(source1, source2));

        List<NameValue> result = statisticManager.loadSourcePieData();

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("车展会", result.get(0).getName());
        assertEquals("网络广告", result.get(1).getName());
    }

    @Test
    void loadSourcePieData_shouldUseCurrentUserDataScope() {
        when(currentUserProvider.getDataScopeUserId()).thenReturn(7);
        NameValue source = NameValue.builder().name("网络广告").value(1).build();
        when(tClueMapper.selectBySource(7)).thenReturn(List.of(source));

        List<NameValue> result = statisticManager.loadSourcePieData();

        assertEquals(1, result.size());
        assertEquals("网络广告", result.get(0).getName());
    }
}
