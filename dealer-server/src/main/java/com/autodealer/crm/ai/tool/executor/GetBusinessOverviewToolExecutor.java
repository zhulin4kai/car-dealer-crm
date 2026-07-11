package com.autodealer.crm.ai.tool.executor;

import com.autodealer.crm.ai.ToolDefinition;
import com.autodealer.crm.ai.ToolExecutionContext;
import com.autodealer.crm.ai.ToolExecutionResult;
import com.autodealer.crm.ai.ToolExecutor;
import com.autodealer.crm.ai.ToolRiskLevel;
import com.autodealer.crm.ai.dto.tool.AiToolDtos;
import com.autodealer.crm.ai.tool.AiToolArgumentBinder;
import com.autodealer.crm.constant.PermissionCodes;
import com.autodealer.crm.result.NameValue;
import com.autodealer.crm.result.SummaryData;
import com.autodealer.crm.service.StatisticService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class GetBusinessOverviewToolExecutor implements ToolExecutor {
    private static final int MAX_METRICS = 20;
    private static final ToolDefinition DEFINITION = new ToolDefinition(
            "get_business_overview", "查询当前用户数据范围内的经营概览",
            PermissionCodes.STATISTIC_VIEW, ToolRiskLevel.READONLY,
            true, false, MAX_METRICS, "AI_TOOL_GET_BUSINESS_OVERVIEW");

    private final StatisticService statisticService;
    private final AiToolArgumentBinder argumentBinder;

    public GetBusinessOverviewToolExecutor(StatisticService statisticService,
                                           AiToolArgumentBinder argumentBinder) {
        this.statisticService = statisticService;
        this.argumentBinder = argumentBinder;
    }

    @Override
    public ToolDefinition definition() {
        return DEFINITION;
    }

    @Override
    public ToolExecutionResult execute(ToolExecutionContext context, Map<String, Object> arguments) {
        argumentBinder.bind(arguments, AiToolDtos.GetBusinessOverviewRequest.class);
        // 三类统计均通过 StatisticService 计算，保持与普通仪表盘一致的数据范围口径。
        SummaryData summary = statisticService.loadSummaryData();
        AiToolDtos.BusinessSummary safeSummary = summary == null ? null : new AiToolDtos.BusinessSummary(
                summary.getEffectiveActivityCount(), summary.getTotalActivityCount(),
                summary.getTotalClueCount(), summary.getTotalCustomerCount(),
                summary.getSuccessTranAmount(), summary.getTotalTranAmount());
        AiToolDtos.BusinessOverview data = new AiToolDtos.BusinessOverview(
                safeSummary, metrics(statisticService.loadSaleFunnelData()),
                metrics(statisticService.loadSourcePieData()));
        return ToolExecutionResult.of(data, "返回经营概览", "BUSINESS_OVERVIEW");
    }

    private List<AiToolDtos.MetricItem> metrics(List<NameValue> source) {
        if (source == null) {
            return List.of();
        }
        return source.stream().limit(MAX_METRICS)
                .map(item -> new AiToolDtos.MetricItem(item.getName(), item.getValue()))
                .toList();
    }
}
