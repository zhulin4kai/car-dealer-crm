package com.autodealer.crm.ai.service;

import com.autodealer.crm.ai.dto.AiExecutionEventCommand;
import com.autodealer.crm.ai.dto.AiWorkflowActionRequest;
import com.autodealer.crm.ai.dto.CreateAiWorkflowRequest;
import com.autodealer.crm.ai.enums.AiWorkflowStatus;
import com.autodealer.crm.ai.enums.AiWorkflowStepStatus;
import com.autodealer.crm.ai.mapper.TAiRunMapper;
import com.autodealer.crm.ai.mapper.TAiWorkflowMapper;
import com.autodealer.crm.ai.mapper.TAiWorkflowStepMapper;
import com.autodealer.crm.ai.model.TAiRun;
import com.autodealer.crm.ai.model.TAiWorkflow;
import com.autodealer.crm.ai.model.TAiWorkflowStep;
import com.autodealer.crm.ai.service.impl.AiWorkflowServiceImpl;
import com.autodealer.crm.config.security.CurrentUserProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiWorkflowServiceImplTest {
    @Mock private TAiWorkflowMapper workflowMapper;
    @Mock private TAiWorkflowStepMapper stepMapper;
    @Mock private TAiRunMapper runMapper;
    @Mock private AiTraceService traceService;
    @Mock private CurrentUserProvider currentUserProvider;

    private AiWorkflowServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AiWorkflowServiceImpl(
                workflowMapper,
                stepMapper,
                runMapper,
                traceService,
                currentUserProvider,
                new AiSensitiveDataSanitizer());
    }

    @Test
    void create_shouldPersistWorkflowStepsAndExecutionEvent() {
        when(currentUserProvider.getCurrentUserId()).thenReturn(7);
        TAiRun run = new TAiRun();
        run.setId(3L);
        run.setRunNo("AIR1");
        run.setContextObjectType("CUSTOMER");
        run.setContextObjectId("12");
        when(traceService.getOwnedRun("AIR1")).thenReturn(run);
        when(workflowMapper.insert(any(TAiWorkflow.class))).thenAnswer(invocation -> {
            TAiWorkflow workflow = invocation.getArgument(0);
            workflow.setId(10L);
            return 1;
        });
        when(stepMapper.insert(any(TAiWorkflowStep.class))).thenReturn(1);
        when(stepMapper.selectByWorkflowId(10L)).thenReturn(List.of());
        ArgumentCaptor<TAiWorkflow> workflowCaptor = ArgumentCaptor.forClass(TAiWorkflow.class);
        ArgumentCaptor<TAiWorkflowStep> stepCaptor = ArgumentCaptor.forClass(TAiWorkflowStep.class);
        ArgumentCaptor<AiExecutionEventCommand> eventCaptor =
                ArgumentCaptor.forClass(AiExecutionEventCommand.class);
        CreateAiWorkflowRequest request = new CreateAiWorkflowRequest();
        request.setRunNo("AIR1");
        request.setWorkflowType("CUSTOMER_FOLLOW_UP");

        service.create(request);

        verify(workflowMapper).insert(workflowCaptor.capture());
        assertEquals(AiWorkflowStatus.WAITING_USER_CONFIRMATION.name(),
                workflowCaptor.getValue().getStatus());
        verify(stepMapper, org.mockito.Mockito.times(3)).insert(stepCaptor.capture());
        assertEquals(AiWorkflowStepStatus.COMPLETED.name(), stepCaptor.getAllValues().get(0).getStatus());
        assertEquals(AiWorkflowStepStatus.WAITING_USER_CONFIRMATION.name(),
                stepCaptor.getAllValues().get(1).getStatus());
        verify(traceService).recordExecutionEvent(eventCaptor.capture());
        assertEquals("AI_WORKFLOW_STARTED", eventCaptor.getValue().eventType());
        assertEquals("AI_WORKFLOW", eventCaptor.getValue().objectType());
    }

    @Test
    void pauseResumeCancel_shouldValidateCurrentStatusAndUpdateByCas() {
        when(currentUserProvider.getCurrentUserId()).thenReturn(7);
        TAiWorkflow running = workflow("AIW1", AiWorkflowStatus.RUNNING);
        TAiWorkflow paused = workflow("AIW1", AiWorkflowStatus.PAUSED);
        when(workflowMapper.selectOwnedByWorkflowNo("AIW1", 7))
                .thenReturn(running)
                .thenReturn(paused)
                .thenReturn(paused)
                .thenReturn(running)
                .thenReturn(running)
                .thenReturn(running);
        when(workflowMapper.updateStatusIfCurrent(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(1);
        when(stepMapper.selectByWorkflowId(10L)).thenReturn(List.of());
        AiWorkflowActionRequest request = new AiWorkflowActionRequest();
        request.setReason("人工暂停核对");

        service.pause("AIW1", request);
        service.resume("AIW1");
        service.cancel("AIW1", request);

        verify(workflowMapper).updateStatusIfCurrent(
                10L, AiWorkflowStatus.RUNNING.name(), AiWorkflowStatus.PAUSED.name(),
                "人工暂停核对", "", "", 7);
        verify(workflowMapper).updateStatusIfCurrent(
                10L, AiWorkflowStatus.PAUSED.name(), AiWorkflowStatus.RUNNING.name(),
                "", "", "", 7);
        verify(workflowMapper).updateStatusIfCurrent(
                10L, AiWorkflowStatus.RUNNING.name(), AiWorkflowStatus.CANCELLED.name(),
                "人工暂停核对", "", "", 7);
    }

    private TAiWorkflow workflow(String workflowNo, AiWorkflowStatus status) {
        TAiWorkflow workflow = new TAiWorkflow();
        workflow.setId(10L);
        workflow.setWorkflowNo(workflowNo);
        workflow.setRunId(3L);
        workflow.setUserId(7);
        workflow.setWorkflowType("INVENTORY_RISK_REVIEW");
        workflow.setTitle("库存风险解释工作流");
        workflow.setStatus(status.name());
        return workflow;
    }
}
