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
import com.autodealer.crm.modules.sales.customer.application.api.dto.CustomerDetailResponse;
import com.autodealer.crm.shared.error.BusinessException;
import com.autodealer.crm.shared.error.CodeEnum;
import com.autodealer.crm.modules.sales.customer.application.api.CustomerService;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class GetCustomerProfileToolExecutor implements ToolExecutor {
    private static final ToolDefinition DEFINITION = new ToolDefinition(
            "get_customer_profile",
            "查询当前用户可见客户的摘要档案",
            PermissionCodes.CUSTOMER_VIEW,
            ToolRiskLevel.READONLY,
            true,
            false,
            1,
            "AI_TOOL_GET_CUSTOMER_PROFILE");

    private final CustomerService customerService;
    private final AiToolArgumentBinder argumentBinder;
    private final AiSensitiveDataSanitizer sanitizer;

    public GetCustomerProfileToolExecutor(CustomerService customerService,
                                          AiToolArgumentBinder argumentBinder,
                                          AiSensitiveDataSanitizer sanitizer) {
        this.customerService = customerService;
        this.argumentBinder = argumentBinder;
        this.sanitizer = sanitizer;
    }

    @Override
    public ToolDefinition definition() {
        return DEFINITION;
    }

    @Override
    public ToolExecutionResult execute(ToolExecutionContext context, Map<String, Object> arguments) {
        AiToolDtos.GetCustomerProfileRequest request =
                argumentBinder.bind(arguments, AiToolDtos.GetCustomerProfileRequest.class);
        CustomerDetailResponse customer = customerService.getCustomerById(request.getCustomerId());
        if (customer == null) {
            throw new BusinessException(CodeEnum.NOT_FOUND, "客户不存在");
        }
        Object data = new AiToolDtos.CustomerProfile(
                customer.getId(), customer.getCustomerName(), sanitizer.sanitize(customer.getPhone(), 32),
                sanitizer.sanitize(customer.getWeixin(), 64), customer.getOwnerName(),
                customer.getProductName(), customer.getCustomerStatusName(),
                customer.getDescription(), customer.getNextContactTime());
        return ToolExecutionResult.of(data, "返回客户档案", "CUSTOMER:" + customer.getId());
    }
}
