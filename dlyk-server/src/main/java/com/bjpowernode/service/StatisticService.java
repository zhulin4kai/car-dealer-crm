package com.bjpowernode.service;

import com.bjpowernode.result.NameValue;
import com.bjpowernode.result.SummaryData;

import java.util.List;

public interface StatisticService {

    SummaryData loadSummaryData();

    List<NameValue> loadSaleFunnelData();

    List<NameValue> loadSourcePieData();
}
