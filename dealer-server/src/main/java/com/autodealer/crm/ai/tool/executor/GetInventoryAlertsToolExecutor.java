package com.autodealer.crm.ai.tool.executor;

import com.autodealer.crm.ai.ToolDefinition;
import com.autodealer.crm.ai.ToolExecutionContext;
import com.autodealer.crm.ai.ToolExecutionResult;
import com.autodealer.crm.ai.ToolExecutor;
import com.autodealer.crm.ai.ToolRiskLevel;
import com.autodealer.crm.ai.dto.tool.AiToolDtos;
import com.autodealer.crm.ai.tool.AiToolArgumentBinder;
import com.autodealer.crm.constant.PermissionCodes;
import com.autodealer.crm.model.TProduct;
import com.autodealer.crm.service.ProductService;
import com.github.pagehelper.PageInfo;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class GetInventoryAlertsToolExecutor implements ToolExecutor {
    private static final ToolDefinition DEFINITION = new ToolDefinition(
            "get_inventory_alerts",
            "查询当前库存预警商品",
            PermissionCodes.PRODUCT_STOCK_VIEW,
            ToolRiskLevel.READONLY,
            true,
            false,
            20,
            "AI_TOOL_GET_INVENTORY_ALERTS");

    private final ProductService productService;
    private final AiToolArgumentBinder argumentBinder;

    public GetInventoryAlertsToolExecutor(ProductService productService,
                                          AiToolArgumentBinder argumentBinder) {
        this.productService = productService;
        this.argumentBinder = argumentBinder;
    }

    @Override
    public ToolDefinition definition() {
        return DEFINITION;
    }

    @Override
    public ToolExecutionResult execute(ToolExecutionContext context, Map<String, Object> arguments) {
        AiToolDtos.GetInventoryAlertsRequest request =
                argumentBinder.bind(arguments, AiToolDtos.GetInventoryAlertsRequest.class);
        int size = Math.min(request.getSize(), DEFINITION.maxResults());
        PageInfo<TProduct> page = productService.getStockAlerts(
                request.getPage(), size, request.getSku(), request.getName(), request.getCategoryId());
        var items = page.getList().stream()
                .map(product -> new AiToolDtos.InventoryAlert(
                        product.getId(), product.getSku(), product.getName(), product.getCategoryName(),
                        product.getStock(), product.getMinStock(), product.getStatus()))
                .toList();
        Object data = new AiToolDtos.PageResult<>(items, page.getTotal(), request.getPage(), size);
        return ToolExecutionResult.of(data, "返回库存预警 " + items.size() + " 条", "PRODUCT_STOCK_ALERT");
    }
}
