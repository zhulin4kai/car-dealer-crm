package com.autodealer.crm.modules.analytics.application.api;

import com.autodealer.crm.modules.analytics.application.api.result.NameValue;
import com.autodealer.crm.modules.analytics.application.api.result.SummaryData;

import java.util.List;

public interface StatisticService {

    SummaryData loadSummaryData();

    List<NameValue> loadSaleFunnelData();

    List<NameValue> loadSourcePieData();
}
