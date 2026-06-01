package com.autodealer.crm.service;

import com.autodealer.crm.result.NameValue;
import com.autodealer.crm.result.SummaryData;

import java.util.List;

public interface StatisticService {

    SummaryData loadSummaryData();

    List<NameValue> loadSaleFunnelData();

    List<NameValue> loadSourcePieData();
}
