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
import com.autodealer.crm.shared.error.BusinessException;
import com.autodealer.crm.modules.fulfillment.delivery.application.api.model.TDelivery;
import com.autodealer.crm.shared.error.CodeEnum;
import com.autodealer.crm.modules.fulfillment.delivery.application.api.DeliveryService;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class GetDeliveryDetailToolExecutor implements ToolExecutor {
    private static final ToolDefinition DEFINITION = new ToolDefinition(
            "get_delivery_detail", "查询当前用户可见的交付进度和异常摘要",
            PermissionCodes.DELIVERY_VIEW, ToolRiskLevel.READONLY,
            true, false, 1, "AI_TOOL_GET_DELIVERY_DETAIL");

    private final DeliveryService deliveryService;
    private final AiToolArgumentBinder argumentBinder;
    private final AiSensitiveDataSanitizer sanitizer;

    public GetDeliveryDetailToolExecutor(DeliveryService deliveryService,
                                         AiToolArgumentBinder argumentBinder,
                                         AiSensitiveDataSanitizer sanitizer) {
        this.deliveryService = deliveryService;
        this.argumentBinder = argumentBinder;
        this.sanitizer = sanitizer;
    }

    @Override
    public ToolDefinition definition() {
        return DEFINITION;
    }

    @Override
    public ToolExecutionResult execute(ToolExecutionContext context, Map<String, Object> arguments) {
        AiToolDtos.GetDeliveryDetailRequest request = argumentBinder.bind(
                arguments, AiToolDtos.GetDeliveryDetailRequest.class);
        // 交付签收证据属于敏感原文，只返回签收方式和脱敏后的业务摘要。
        TDelivery delivery = deliveryService.getDelivery(request.getDeliveryId());
        if (delivery == null) {
            throw new BusinessException(CodeEnum.NOT_FOUND, "交付记录不存在");
        }
        AiToolDtos.DeliveryDetail data = new AiToolDtos.DeliveryDetail(
                delivery.getStatus(), delivery.getPlannedDeliveryTime(), delivery.getActualDeliveryTime(),
                sanitizer.sanitizeDisplayText(delivery.getSignerName(), 64), delivery.getSignedAt(),
                delivery.getSignMethod(), delivery.getExceptionType(),
                sanitizer.sanitizeDisplayText(delivery.getExceptionReason(), 500));
        return ToolExecutionResult.of(data, "返回交付详情", "DELIVERY:" + delivery.getId());
    }
}
