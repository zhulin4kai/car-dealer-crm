package com.bjpowernode.manager;

import com.bjpowernode.mapper.TActivityMapper;
import com.bjpowernode.mapper.TClueMapper;
import com.bjpowernode.mapper.TCustomerMapper;
import com.bjpowernode.mapper.TTranMapper;
import com.bjpowernode.result.NameValue;
import com.bjpowernode.result.SummaryData;
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

    public SummaryData loadSummaryData() {
        //有效的市场活动总数
        Integer effectiveActivityCount = tActivityMapper.selecOngoingActivity().size(); //偷懒了一下

        //总的市场活动数
        Integer totalActivityCount = tActivityMapper.selectByCount();

        //线索总数
        Integer totalClueCount = tClueMapper.selectClueByCount();

        //客户总数
        Integer totalCustomerCount = tCustomerMapper.selectByCount();

        //成功的交易额
        BigDecimal successTranAmount = tTranMapper.selectBySuccessTranAmount();

        //总的交易额（包含成功和不成功的）
        BigDecimal totalTranAmount = tTranMapper.selectByTotalTranAmount();

        return SummaryData.builder()
                .effectiveActivityCount(effectiveActivityCount)
                .totalActivityCount(totalActivityCount)
                .totalClueCount(totalClueCount)
                .totalCustomerCount(totalCustomerCount)
                .successTranAmount(successTranAmount)
                .totalTranAmount(totalTranAmount)
                .build();
    }

    public List<NameValue> loadSaleFunnelData() {
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
        int clueCount = tClueMapper.selectClueByCount();
        int customerCount = tCustomerMapper.selectByCount();
        int tranCount = tTranMapper.selectByTotalTranCount();
        int tranSuccessCount = tTranMapper.selectBySuccessTranCount();

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
        return tClueMapper.selectBySource();
    }
}
