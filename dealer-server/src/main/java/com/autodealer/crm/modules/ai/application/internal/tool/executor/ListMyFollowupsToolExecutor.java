package com.autodealer.crm.modules.ai.application.internal.tool.executor;

import com.autodealer.crm.modules.ai.application.api.tool.ToolDefinition;
import com.autodealer.crm.modules.ai.application.api.tool.ToolExecutionContext;
import com.autodealer.crm.modules.ai.application.api.tool.ToolExecutionResult;
import com.autodealer.crm.modules.ai.application.api.tool.ToolExecutor;
import com.autodealer.crm.modules.ai.application.api.tool.ToolRiskLevel;
import com.autodealer.crm.modules.ai.application.api.dto.tool.AiToolDtos;
import com.autodealer.crm.modules.ai.application.internal.tool.AiToolArgumentBinder;
import com.autodealer.crm.shared.security.PermissionCodes;
import com.autodealer.crm.modules.sales.followup.application.api.model.TFollowTask;
import com.autodealer.crm.modules.sales.followup.application.api.query.FollowTaskQuery;
import com.autodealer.crm.modules.sales.followup.application.api.FollowTaskService;
import com.github.pagehelper.PageInfo;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ListMyFollowupsToolExecutor implements ToolExecutor {
    private static final ToolDefinition DEFINITION = new ToolDefinition(
            "list_my_followups",
            "查询当前用户可见的跟进任务列表",
            PermissionCodes.FOLLOW_TASK_LIST,
            ToolRiskLevel.READONLY,
            true,
            false,
            20,
            "AI_TOOL_LIST_MY_FOLLOWUPS");

    private final FollowTaskService followTaskService;
    private final AiToolArgumentBinder argumentBinder;

    public ListMyFollowupsToolExecutor(FollowTaskService followTaskService,
                                       AiToolArgumentBinder argumentBinder) {
        this.followTaskService = followTaskService;
        this.argumentBinder = argumentBinder;
    }

    @Override
    public ToolDefinition definition() {
        return DEFINITION;
    }

    @Override
    public ToolExecutionResult execute(ToolExecutionContext context, Map<String, Object> arguments) {
        AiToolDtos.ListMyFollowupsRequest request =
                argumentBinder.bind(arguments, AiToolDtos.ListMyFollowupsRequest.class);
        FollowTaskQuery query = new FollowTaskQuery();
        query.setPage(request.getPage());
        query.setSize(Math.min(request.getSize(), DEFINITION.maxResults()));
        query.setStatus(request.getStatus());
        query.setOverdueOnly(request.getOverdueOnly());
        query.setKeyword(request.getKeyword());
        PageInfo<TFollowTask> page = followTaskService.getFollowTaskPageReadOnly(query);
        var items = page.getList().stream()
                .map(task -> new AiToolDtos.FollowupItem(
                        task.getId(), task.getTitle(), task.getTaskType(), task.getRelatedObjectType(),
                        task.getRelatedObjectId(), task.getRelatedObjectName(), task.getPriority(),
                        task.getDueTime(), task.getStatus()))
                .toList();
        Object data = new AiToolDtos.PageResult<>(items, page.getTotal(), request.getPage(), query.getSize());
        return ToolExecutionResult.of(data, "返回跟进任务 " + items.size() + " 条", "FOLLOW_TASK");
    }
}
