package com.autodealer.crm.modules.analytics.web;

import com.autodealer.crm.shared.security.PermissionCodes;
import com.autodealer.crm.shared.web.Result;
import com.autodealer.crm.modules.analytics.application.api.StatisticService;
import com.autodealer.crm.modules.analytics.application.api.result.NameValue;
import com.autodealer.crm.modules.analytics.application.api.result.SummaryData;
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

    @PreAuthorize("hasAuthority('" + PermissionCodes.STATISTIC_VIEW + "')")
    @GetMapping(value = "/api/summary/data")
    public Result<SummaryData> summaryData() {
        SummaryData summaryData = statisticService.loadSummaryData();
        return Result.OK(summaryData);
    }

    @PreAuthorize("hasAuthority('" + PermissionCodes.STATISTIC_VIEW + "')")
    @GetMapping(value = "/api/saleFunnel/data")
    public Result<List<NameValue>> saleFunnelData() {
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
        return Result.OK(nameValueList);
    }

    @PreAuthorize("hasAuthority('" + PermissionCodes.STATISTIC_VIEW + "')")
    @GetMapping(value = "/api/sourcePie/data")
    public Result<List<NameValue>> sourcePieData() {
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
        return Result.OK(nameValueList);
    }
}
