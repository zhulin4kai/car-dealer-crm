package com.autodealer.crm.web;

import com.autodealer.crm.result.NameValue;
import com.autodealer.crm.result.R;
import com.autodealer.crm.result.SummaryData;
import com.autodealer.crm.service.StatisticService;
import jakarta.annotation.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 数据统计Controller
 */
@RestController
public class StatisticController {

    @Resource
    private StatisticService statisticService;

    @PreAuthorize("hasAuthority('statistic:view')")
    @GetMapping(value = "/api/summary/data")
    public R<SummaryData> summaryData() {
        SummaryData summaryData = statisticService.loadSummaryData();
        return R.OK(summaryData);
    }

    @PreAuthorize("hasAuthority('statistic:view')")
    @GetMapping(value = "/api/saleFunnel/data")
    public R<List<NameValue>> saleFunnelData() {
        /**
         * [
         *    { value: 20, name: '成交' },
         *    { value: 60, name: '交易' },
         *    { value: 80, name: '客户' },
         *    { value: 100, name: '线索' }
         * ]
         *
         */
        List<NameValue> nameValueList = statisticService.loadSaleFunnelData();
        return R.OK(nameValueList);
    }

    @PreAuthorize("hasAuthority('statistic:view')")
    @GetMapping(value = "/api/sourcePie/data")
    public R<List<NameValue>> sourcePieData() {
        /**
         *   [
         *       { value: 1048, name: 'Search Engine' },
         *       { value: 735, name: 'Direct' },
         *       { value: 580, name: 'Email' },
         *       { value: 484, name: 'Union Ads' },
         *       { value: 300, name: 'Video Ads' }
         *   ]
         *
         */
        List<NameValue> nameValueList = statisticService.loadSourcePieData();
        return R.OK(nameValueList);
    }
}
