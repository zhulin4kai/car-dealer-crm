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
import com.autodealer.crm.modules.fulfillment.transaction.application.api.model.TTran;
import com.autodealer.crm.modules.fulfillment.transaction.application.api.model.TTranProduct;
import com.autodealer.crm.shared.error.CodeEnum;
import com.autodealer.crm.modules.fulfillment.transaction.application.api.TranService;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class GetTransactionDetailToolExecutor implements ToolExecutor {
    private static final ToolDefinition DEFINITION = new ToolDefinition(
            "get_transaction_detail",
            "查询当前用户可见交易摘要和商品明细",
            PermissionCodes.TRAN_VIEW,
            ToolRiskLevel.READONLY,
            true,
            false,
            1,
            "AI_TOOL_GET_TRANSACTION_DETAIL");

    private final TranService tranService;
    private final AiToolArgumentBinder argumentBinder;

    public GetTransactionDetailToolExecutor(TranService tranService,
                                            AiToolArgumentBinder argumentBinder) {
        this.tranService = tranService;
        this.argumentBinder = argumentBinder;
    }

    @Override
    public ToolDefinition definition() {
        return DEFINITION;
    }

    @Override
    public ToolExecutionResult execute(ToolExecutionContext context, Map<String, Object> arguments) {
        AiToolDtos.GetTransactionDetailRequest request =
                argumentBinder.bind(arguments, AiToolDtos.GetTransactionDetailRequest.class);
        TTran tran = tranService.getTransactionById(request.getTranId());
        if (tran == null) {
            throw new BusinessException(CodeEnum.NOT_FOUND, "交易不存在");
        }
        var products = tranService.getTransactionProductDetails(request.getTranId()).stream()
                .map(this::toProduct)
                .toList();
        Object data = new AiToolDtos.TransactionDetail(
                tran.getId(), tran.getTranNo(), tran.getCustomerId(), tran.getCustomerName(),
                tran.getMoney(), tran.getStage() == null ? null : tran.getStage().name(),
                tran.getStage() == null ? null : tran.getStage().getLabel(),
                tran.getExpectedDate(), tran.getDescription(), products);
        return ToolExecutionResult.of(data, "返回交易详情", "TRAN:" + tran.getId());
    }

    private AiToolDtos.TransactionProduct toProduct(TTranProduct product) {
        return new AiToolDtos.TransactionProduct(
                product.getProductId(), product.getProductSku(), product.getProductName(),
                product.getProductSpecification(), product.getQuantity(), product.getPrice());
    }
}
