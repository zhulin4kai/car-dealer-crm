package com.autodealer.crm.manager;

import com.autodealer.crm.config.security.CurrentUserProvider;
import com.autodealer.crm.enums.TranStage;
import com.autodealer.crm.mapper.TActivityMapper;
import com.autodealer.crm.mapper.TClueMapper;
import com.autodealer.crm.mapper.TCustomerMapper;
import com.autodealer.crm.mapper.TTranMapper;
import com.autodealer.crm.result.NameValue;
import com.autodealer.crm.result.SummaryData;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
public class StatisticManager {

    @Resource
    private TActivityMapper tActivityMapper;

    @Resource
    private TClueMapper tClueMapper;

    @Resource
    private TCustomerMapper tCustomerMapper;

    @Resource
    private TTranMapper tTranMapper;

    @Resource
    private CurrentUserProvider currentUserProvider;

    public SummaryData loadSummaryData() {
        Integer dataScopeUserId = currentUserProvider.getDataScopeUserId();
        CurrentUserProvider.TransactionDataScope transactionScope = currentUserProvider.getTransactionDataScope();

        //有效的市场活动总数
        Integer effectiveActivityCount = tActivityMapper.selecOngoingActivity(dataScopeUserId).size();

        //总的市场活动数
        Integer totalActivityCount = tActivityMapper.selectByCount(dataScopeUserId);

        //线索总数
        Integer totalClueCount = tClueMapper.selectClueByCount(dataScopeUserId);

        //客户总数
        Integer totalCustomerCount = tCustomerMapper.selectByCount(dataScopeUserId);

        //成功的交易额
        BigDecimal successTranAmount = tTranMapper.selectBySuccessTranAmount(
                TranStage.COMPLETED,
                transactionScope.isAll(),
                transactionScope.getSelfUserId());

        //总的交易额（包含成功和不成功的）
        BigDecimal totalTranAmount = tTranMapper.selectByTotalTranAmount(
                transactionScope.isAll(),
                transactionScope.getSelfUserId());

        return SummaryData.builder()
                .effectiveActivityCount(effectiveActivityCount)
                .totalActivityCount(totalActivityCount)
                .totalClueCount(totalClueCount)
                .totalCustomerCount(totalCustomerCount)
                .successTranAmount(defaultZero(successTranAmount))
                .totalTranAmount(defaultZero(totalTranAmount))
                .build();
    }

    public List<NameValue> loadSaleFunnelData() {
        Integer dataScopeUserId = currentUserProvider.getDataScopeUserId();
        CurrentUserProvider.TransactionDataScope transactionScope = currentUserProvider.getTransactionDataScope();
        List<NameValue> resultList = new ArrayList<>();

        /**
         * [
         *    { value: 20, name: '成交' },
         *    { value: 60, name: '交易' },
         *    { value: 80, name: '客户' },
         *    { value: 100, name: '线索' }
         * ]
         *
         */
        int clueCount = tClueMapper.selectClueByCount(dataScopeUserId);
        int customerCount = tCustomerMapper.selectByCount(dataScopeUserId);
        int tranCount = tTranMapper.selectByTotalTranCount(transactionScope.isAll(), transactionScope.getSelfUserId());
        int tranSuccessCount = tTranMapper.selectBySuccessTranCount(
                TranStage.COMPLETED,
                transactionScope.isAll(),
                transactionScope.getSelfUserId());

        NameValue clue = NameValue.builder().name("线索").value(clueCount).build();
        resultList.add(clue);

        NameValue customer = NameValue.builder().name("客户").value(customerCount).build();
        resultList.add(customer);

        NameValue tran = NameValue.builder().name("交易").value(tranCount).build();
        resultList.add(tran);

        NameValue tranSuccess = NameValue.builder().name("成交").value(tranSuccessCount).build();
        resultList.add(tranSuccess);

        return resultList;
    }

    public List<NameValue> loadSourcePieData() {
        return tClueMapper.selectBySource(currentUserProvider.getDataScopeUserId());
    }

    private BigDecimal defaultZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
