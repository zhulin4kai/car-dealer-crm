package com.autodealer.crm.modules.analytics.application.internal;

import com.autodealer.crm.modules.fulfillment.transaction.application.api.port.TransactionDataPort;
import com.autodealer.crm.modules.sales.customer.application.api.port.CustomerDataPort;
import com.autodealer.crm.modules.sales.lead.application.api.port.LeadDataPort;
import com.autodealer.crm.modules.sales.activity.application.api.port.ActivityDataPort;
import com.autodealer.crm.modules.identity.application.api.security.CurrentUserProvider;
import com.autodealer.crm.modules.fulfillment.transaction.application.api.enums.TranStage;
import com.autodealer.crm.modules.analytics.application.api.result.NameValue;
import com.autodealer.crm.modules.analytics.application.api.result.SummaryData;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
public class StatisticManager {

    @Resource
    private ActivityDataPort tActivityMapper;

    @Resource
    private LeadDataPort tClueMapper;

    @Resource
    private CustomerDataPort tCustomerMapper;

    @Resource
    private TransactionDataPort tTranMapper;

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

        NameValue tranSuccess = NameValue.builder().name("成交客户").value(tranSuccessCount).build();
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
