package com.autodealer.crm.ai.tool.executor;

import com.autodealer.crm.ai.ToolDefinition;
import com.autodealer.crm.ai.ToolExecutionContext;
import com.autodealer.crm.ai.ToolExecutionResult;
import com.autodealer.crm.ai.ToolExecutor;
import com.autodealer.crm.ai.ToolRiskLevel;
import com.autodealer.crm.ai.dto.tool.AiToolDtos;
import com.autodealer.crm.ai.service.AiSensitiveDataSanitizer;
import com.autodealer.crm.ai.tool.AiToolArgumentBinder;
import com.autodealer.crm.constant.PermissionCodes;
import com.autodealer.crm.exception.BusinessException;
import com.autodealer.crm.model.TOpportunity;
import com.autodealer.crm.result.CodeEnum;
import com.autodealer.crm.service.OpportunityService;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class GetOpportunityDetailToolExecutor implements ToolExecutor {
    private static final ToolDefinition DEFINITION = new ToolDefinition(
            "get_opportunity_detail", "查询当前用户可见的商机摘要",
            PermissionCodes.OPPORTUNITY_VIEW, ToolRiskLevel.READONLY,
            true, false, 1, "AI_TOOL_GET_OPPORTUNITY_DETAIL");

    private final OpportunityService opportunityService;
    private final AiToolArgumentBinder argumentBinder;
    private final AiSensitiveDataSanitizer sanitizer;

    public GetOpportunityDetailToolExecutor(OpportunityService opportunityService,
                                            AiToolArgumentBinder argumentBinder,
                                            AiSensitiveDataSanitizer sanitizer) {
        this.opportunityService = opportunityService;
        this.argumentBinder = argumentBinder;
        this.sanitizer = sanitizer;
    }

    @Override
    public ToolDefinition definition() {
        return DEFINITION;
    }

    @Override
    public ToolExecutionResult execute(ToolExecutionContext context, Map<String, Object> arguments) {
        AiToolDtos.GetOpportunityDetailRequest request = argumentBinder.bind(
                arguments, AiToolDtos.GetOpportunityDetailRequest.class);
        // Service 详情方法负责 owner 数据范围，工具层只裁剪和脱敏展示字段。
        TOpportunity opportunity = opportunityService.getOpportunity(request.getOpportunityId());
        if (opportunity == null) {
            throw new BusinessException(CodeEnum.NOT_FOUND, "商机不存在");
        }
        AiToolDtos.OpportunityDetail data = new AiToolDtos.OpportunityDetail(
                opportunity.getOpportunityNo(), opportunity.getCustomerName(),
                opportunity.getOwnerName(), opportunity.getProductName(), opportunity.getSourceType(),
                opportunity.getStage(), sanitizer.sanitizeDisplayText(opportunity.getRequirement(), 500),
                opportunity.getExpectedAmount(), opportunity.getExpectedCloseDate(),
                opportunity.getNextActionTime(), opportunity.getLastFollowTime(),
                sanitizer.sanitizeDisplayText(opportunity.getLastFollowSummary(), 500),
                sanitizer.sanitizeDisplayText(opportunity.getLostReason(), 255),
                sanitizer.sanitizeDisplayText(opportunity.getResultRemark(), 500));
        return ToolExecutionResult.of(data, "返回商机详情", "OPPORTUNITY:" + opportunity.getId());
    }
}
