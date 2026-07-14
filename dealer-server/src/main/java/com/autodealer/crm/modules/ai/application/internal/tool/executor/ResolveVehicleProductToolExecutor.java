package com.autodealer.crm.modules.ai.application.internal.tool.executor;

import com.autodealer.crm.modules.ai.application.api.tool.ToolDefinition;
import com.autodealer.crm.modules.ai.application.api.tool.ToolExecutionContext;
import com.autodealer.crm.modules.ai.application.api.tool.ToolExecutionResult;
import com.autodealer.crm.modules.ai.application.api.tool.ToolExecutor;
import com.autodealer.crm.modules.ai.application.api.tool.ToolRiskLevel;
import com.autodealer.crm.modules.ai.application.api.dto.tool.AiToolDtos;
import com.autodealer.crm.modules.ai.application.internal.tool.AiToolArgumentBinder;
import com.autodealer.crm.shared.security.PermissionCodes;
import com.autodealer.crm.shared.error.BusinessException;
import com.autodealer.crm.modules.commerce.catalog.application.api.model.TProduct;
import com.autodealer.crm.shared.error.CodeEnum;
import com.autodealer.crm.modules.commerce.catalog.application.api.ProductService;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Map;

@Component
public class ResolveVehicleProductToolExecutor implements ToolExecutor {
    private static final ToolDefinition DEFINITION = new ToolDefinition(
            "resolve_vehicle_product",
            "按商品 ID 或 SKU 解析车辆商品",
            PermissionCodes.PRODUCT_VIEW,
            ToolRiskLevel.READONLY,
            true,
            false,
            1,
            "AI_TOOL_RESOLVE_VEHICLE_PRODUCT");

    private final ProductService productService;
    private final AiToolArgumentBinder argumentBinder;

    public ResolveVehicleProductToolExecutor(ProductService productService,
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
        AiToolDtos.ResolveVehicleProductRequest request =
                argumentBinder.bind(arguments, AiToolDtos.ResolveVehicleProductRequest.class);
        TProduct product;
        if (request.getProductId() != null) {
            product = productService.getProductById(request.getProductId());
        } else if (StringUtils.hasText(request.getSku())) {
            product = productService.getProductBySku(request.getSku());
        } else {
            throw new BusinessException(CodeEnum.AI_TOOL_ARGUMENT_INVALID, "商品 ID 和 SKU 至少传一个");
        }
        if (product == null) {
            throw new BusinessException(CodeEnum.NOT_FOUND, "商品不存在");
        }
        Object data = new AiToolDtos.ProductSummary(
                product.getId(), product.getSku(), product.getName(), product.getCategoryName(),
                product.getSpecification(), product.getPrice(), product.getStock(),
                product.getMinStock(), product.getStatus());
        return ToolExecutionResult.of(data, "返回车辆商品", "PRODUCT:" + product.getId());
    }
}
