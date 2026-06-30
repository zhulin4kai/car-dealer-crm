package com.autodealer.crm.ai.tool.executor;

import com.autodealer.crm.ai.ToolDefinition;
import com.autodealer.crm.ai.ToolExecutionContext;
import com.autodealer.crm.ai.ToolExecutionResult;
import com.autodealer.crm.ai.ToolExecutor;
import com.autodealer.crm.ai.ToolRiskLevel;
import com.autodealer.crm.ai.dto.tool.AiToolDtos;
import com.autodealer.crm.ai.tool.AiToolArgumentBinder;
import com.autodealer.crm.constant.PermissionCodes;
import com.autodealer.crm.enums.TranStage;
import com.autodealer.crm.model.TTran;
import com.autodealer.crm.query.TranQuery;
import com.autodealer.crm.service.TranService;
import com.github.pagehelper.PageInfo;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ListPendingTransactionApprovalsToolExecutor implements ToolExecutor {
    private static final ToolDefinition DEFINITION = new ToolDefinition(
            "list_pending_transaction_approvals",
            "查询待审批交易摘要，不执行审批动作",
            PermissionCodes.TRAN_APPROVE,
            ToolRiskLevel.READONLY,
            true,
            false,
            20,
            "AI_TOOL_LIST_PENDING_TRANSACTION_APPROVALS");

    private final TranService tranService;
    private final AiToolArgumentBinder argumentBinder;

    public ListPendingTransactionApprovalsToolExecutor(TranService tranService,
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
        AiToolDtos.ListPendingTransactionApprovalsRequest request =
                argumentBinder.bind(arguments, AiToolDtos.ListPendingTransactionApprovalsRequest.class);
        TranQuery query = new TranQuery();
        query.setStage(TranStage.PENDING);
        int size = Math.min(request.getSize(), DEFINITION.maxResults());
        PageInfo<TTran> page = tranService.getTransactionList(query, request.getPage(), size);
        var items = page.getList().stream()
                .map(tran -> new AiToolDtos.PendingTransactionApproval(
                        tran.getId(), tran.getTranNo(), tran.getCustomerName(), tran.getMoney(),
                        tran.getStage() == null ? null : tran.getStage().getLabel(),
                        tran.getCreateTime()))
                .toList();
        Object data = new AiToolDtos.PageResult<>(items, page.getTotal(), request.getPage(), size);
        return ToolExecutionResult.of(data, "返回待审批交易 " + items.size() + " 条", "TRAN:PENDING");
    }
}
