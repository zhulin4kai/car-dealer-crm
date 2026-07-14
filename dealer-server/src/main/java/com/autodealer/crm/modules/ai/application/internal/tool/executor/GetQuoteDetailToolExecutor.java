package com.autodealer.crm.modules.ai.application.internal.tool.executor;

import com.autodealer.crm.modules.ai.application.api.tool.ToolDefinition;
import com.autodealer.crm.modules.ai.application.api.tool.ToolExecutionContext;
import com.autodealer.crm.modules.ai.application.api.tool.ToolExecutionResult;
import com.autodealer.crm.modules.ai.application.api.tool.ToolExecutor;
import com.autodealer.crm.modules.ai.application.api.tool.ToolRiskLevel;
import com.autodealer.crm.modules.ai.application.api.dto.tool.AiToolDtos;
import com.autodealer.crm.modules.ai.application.internal.AiSensitiveDataSanitizer;
import com.autodealer.crm.modules.ai.application.internal.tool.AiToolArgumentBinder;
import com.autodealer.crm.shared.security.PermissionCodes;
import com.autodealer.crm.modules.commerce.quote.application.api.dto.QuoteDetailResponse;
import com.autodealer.crm.shared.error.BusinessException;
import com.autodealer.crm.modules.commerce.quote.application.api.model.TQuoteVersionItem;
import com.autodealer.crm.shared.error.CodeEnum;
import com.autodealer.crm.modules.commerce.quote.application.api.QuoteService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class GetQuoteDetailToolExecutor implements ToolExecutor {
    private static final int MAX_ITEMS = 20;
    private static final ToolDefinition DEFINITION = new ToolDefinition(
            "get_quote_detail", "查询当前用户可见的报价摘要和有限行项",
            PermissionCodes.QUOTE_VIEW, ToolRiskLevel.READONLY,
            true, false, MAX_ITEMS, "AI_TOOL_GET_QUOTE_DETAIL");

    private final QuoteService quoteService;
    private final AiToolArgumentBinder argumentBinder;
    private final AiSensitiveDataSanitizer sanitizer;

    public GetQuoteDetailToolExecutor(QuoteService quoteService,
                                      AiToolArgumentBinder argumentBinder,
                                      AiSensitiveDataSanitizer sanitizer) {
        this.quoteService = quoteService;
        this.argumentBinder = argumentBinder;
        this.sanitizer = sanitizer;
    }

    @Override
    public ToolDefinition definition() {
        return DEFINITION;
    }

    @Override
    public ToolExecutionResult execute(ToolExecutionContext context, Map<String, Object> arguments) {
        AiToolDtos.GetQuoteDetailRequest request = argumentBinder.bind(
                arguments, AiToolDtos.GetQuoteDetailRequest.class);
        // QuoteService 在读取主记录前校验数据范围，AI 只接收裁剪后的快照。
        QuoteDetailResponse detail = quoteService.getQuoteDetail(request.getQuoteId());
        if (detail == null || detail.getQuote() == null) {
            throw new BusinessException(CodeEnum.NOT_FOUND, "报价不存在");
        }
        List<TQuoteVersionItem> sourceItems = detail.getItems() == null ? List.of() : detail.getItems();
        List<AiToolDtos.QuoteItem> items = sourceItems.stream().limit(MAX_ITEMS)
                .map(item -> new AiToolDtos.QuoteItem(
                        item.getProductSku(), item.getProductName(), item.getProductSpecification(),
                        item.getGuidePrice(), item.getUnitPrice(), item.getQuantity(), item.getLineAmount(),
                        sanitizer.sanitizeDisplayText(item.getPromotionName(), 128),
                        item.getPromotionAmount()))
                .toList();
        var quote = detail.getQuote();
        var version = detail.getCurrentVersion();
        AiToolDtos.QuoteDetail data = new AiToolDtos.QuoteDetail(
                quote.getQuoteNo(), quote.getStatus(), sanitizer.sanitizeDisplayText(quote.getRemark(), 500),
                version == null ? null : version.getVersionNo(),
                version == null ? null : version.getValidUntil(),
                version == null ? null : version.getTotalAmount(), sourceItems.size(), items);
        return ToolExecutionResult.of(data, "返回报价详情", "QUOTE:" + quote.getId());
    }
}
