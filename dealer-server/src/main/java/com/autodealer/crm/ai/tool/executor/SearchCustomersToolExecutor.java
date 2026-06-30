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
import com.autodealer.crm.dto.CustomerListResponse;
import com.autodealer.crm.query.CustomerListQuery;
import com.autodealer.crm.service.CustomerService;
import com.github.pagehelper.PageInfo;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class SearchCustomersToolExecutor implements ToolExecutor {
    private static final ToolDefinition DEFINITION = new ToolDefinition(
            "search_customers",
            "按客户名称检索当前用户可见客户",
            PermissionCodes.CUSTOMER_LIST,
            ToolRiskLevel.READONLY,
            true,
            false,
            20,
            "AI_TOOL_SEARCH_CUSTOMERS");

    private final CustomerService customerService;
    private final AiToolArgumentBinder argumentBinder;
    private final AiSensitiveDataSanitizer sanitizer;

    public SearchCustomersToolExecutor(CustomerService customerService,
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
        AiToolDtos.SearchCustomersRequest request =
                argumentBinder.bind(arguments, AiToolDtos.SearchCustomersRequest.class);
        CustomerListQuery query = new CustomerListQuery();
        query.setCustomerName(request.getKeyword());
        PageInfo<CustomerListResponse> page = customerService.getCustomerList(
                query, request.getPage(), Math.min(request.getSize(), DEFINITION.maxResults()));
        var items = page.getList().stream()
                .map(customer -> new AiToolDtos.CustomerSummary(
                        customer.getId(), customer.getCustomerName(), sanitizer.sanitize(customer.getPhone(), 32),
                        customer.getOwnerName(), customer.getIntentionProductName(),
                        customer.getCustomerStatusName()))
                .toList();
        Object data = new AiToolDtos.PageResult<>(items, page.getTotal(), request.getPage(), request.getSize());
        return ToolExecutionResult.of(data, "返回客户 " + items.size() + " 条", "CUSTOMER");
    }
}
