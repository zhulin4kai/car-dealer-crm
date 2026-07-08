package com.autodealer.crm.ai.tool.executor;

import com.autodealer.crm.ai.ToolExecutionContext;
import com.autodealer.crm.ai.ToolExecutionResult;
import com.autodealer.crm.ai.dto.tool.AiToolDtos;
import com.autodealer.crm.ai.model.TAiRun;
import com.autodealer.crm.ai.tool.AiToolArgumentBinder;
import com.autodealer.crm.enums.FollowTaskStatus;
import com.autodealer.crm.model.TFollowTask;
import com.autodealer.crm.query.FollowTaskQuery;
import com.autodealer.crm.service.FollowTaskService;
import com.github.pagehelper.PageInfo;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ListMyFollowupsToolExecutorTest {

    @Test
    void execute_shouldUseReadOnlyFollowTaskQuery() {
        FollowTaskService followTaskService = mock(FollowTaskService.class);
        AiToolArgumentBinder argumentBinder = mock(AiToolArgumentBinder.class);
        AiToolDtos.ListMyFollowupsRequest request = new AiToolDtos.ListMyFollowupsRequest();
        request.setPage(2);
        request.setSize(30);
        request.setStatus(FollowTaskStatus.PENDING.name());
        request.setKeyword("回访");
        when(argumentBinder.bind(any(), eq(AiToolDtos.ListMyFollowupsRequest.class))).thenReturn(request);
        TFollowTask task = new TFollowTask();
        task.setId(10L);
        task.setTitle("电话回访");
        task.setStatus(FollowTaskStatus.PENDING.name());
        when(followTaskService.getFollowTaskPageReadOnly(any()))
                .thenReturn(new PageInfo<>(List.of(task)));
        ListMyFollowupsToolExecutor executor =
                new ListMyFollowupsToolExecutor(followTaskService, argumentBinder);
        TAiRun run = new TAiRun();
        run.setId(1L);

        ToolExecutionResult result = executor.execute(new ToolExecutionContext(run), Map.of());

        assertEquals("返回跟进任务 1 条", result.outputSummary());
        verify(followTaskService, never()).getFollowTaskPage(any());
        ArgumentCaptor<FollowTaskQuery> queryCaptor = ArgumentCaptor.forClass(FollowTaskQuery.class);
        verify(followTaskService).getFollowTaskPageReadOnly(queryCaptor.capture());
        assertEquals(2, queryCaptor.getValue().getPage());
        assertEquals(20, queryCaptor.getValue().getSize());
        assertEquals(FollowTaskStatus.PENDING.name(), queryCaptor.getValue().getStatus());
        assertEquals("回访", queryCaptor.getValue().getKeyword());
    }
}
