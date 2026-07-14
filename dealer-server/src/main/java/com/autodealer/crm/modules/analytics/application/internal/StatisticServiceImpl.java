package com.autodealer.crm.modules.analytics.application.internal;

import com.autodealer.crm.modules.analytics.application.api.StatisticService;
import com.autodealer.crm.modules.analytics.application.api.result.NameValue;
import com.autodealer.crm.modules.analytics.application.api.result.SummaryData;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StatisticServiceImpl implements StatisticService {

    @Resource
    private StatisticManager statisticManager;

    @Override
    public SummaryData loadSummaryData() {
        return statisticManager.loadSummaryData();
    }

    @Override
    public List<NameValue> loadSaleFunnelData() {
        return statisticManager.loadSaleFunnelData();
    }

    @Override
    public List<NameValue> loadSourcePieData() {
        return statisticManager.loadSourcePieData();
    }
}
