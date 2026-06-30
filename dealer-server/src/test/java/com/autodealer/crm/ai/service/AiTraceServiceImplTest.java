package com.autodealer.crm.ai.service;

import com.autodealer.crm.ai.dto.AiCreateRunCommand;
import com.autodealer.crm.ai.dto.AiMessageCommand;
import com.autodealer.crm.ai.dto.AiProposalCommand;
import com.autodealer.crm.ai.dto.AiRunTraceResponse;
import com.autodealer.crm.ai.enums.AiEntryPoint;
import com.autodealer.crm.ai.enums.AiMessageRole;
import com.autodealer.crm.ai.enums.AiProposalType;
import com.autodealer.crm.ai.enums.AiRiskLevel;
import com.autodealer.crm.ai.enums.AiRunStatus;
import com.autodealer.crm.ai.model.TAiActionProposal;
import com.autodealer.crm.ai.mapper.TAiActionProposalMapper;
import com.autodealer.crm.ai.mapper.TAiApprovalMapper;
import com.autodealer.crm.ai.mapper.TAiConversationMapper;
import com.autodealer.crm.ai.mapper.TAiExecutionEventMapper;
import com.autodealer.crm.ai.mapper.TAiMessageMapper;
import com.autodealer.crm.ai.mapper.TAiRunMapper;
import com.autodealer.crm.ai.mapper.TAiToolCallMapper;
import com.autodealer.crm.ai.mapper.TAiWorkflowMapper;
import com.autodealer.crm.ai.mapper.TAiWorkflowStepMapper;
import com.autodealer.crm.ai.model.TAiMessage;
import com.autodealer.crm.ai.model.TAiRun;
import com.autodealer.crm.ai.model.TAiToolCall;
import com.autodealer.crm.ai.model.TAiWorkflow;
import com.autodealer.crm.ai.model.TAiWorkflowStep;
import com.autodealer.crm.ai.service.impl.AiTraceServiceImpl;
import com.autodealer.crm.config.security.CurrentUserProvider;
import com.autodealer.crm.model.TUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiTraceServiceImplTest {

    @Mock private TAiRunMapper runMapper;
    @Mock private TAiConversationMapper conversationMapper;
    @Mock private TAiMessageMapper messageMapper;
    @Mock private TAiToolCallMapper toolCallMapper;
    @Mock private TAiActionProposalMapper proposalMapper;
    @Mock private TAiApprovalMapper approvalMapper;
    @Mock private TAiExecutionEventMapper executionEventMapper;
    @Mock private TAiWorkflowMapper workflowMapper;
    @Mock private TAiWorkflowStepMapper workflowStepMapper;
    @Mock private CurrentUserProvider currentUserProvider;

    private AiTraceServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AiTraceServiceImpl(
                conversationMapper,
                runMapper,
                messageMapper,
                toolCallMapper,
                proposalMapper,
                approvalMapper,
                executionEventMapper,
                workflowMapper,
                workflowStepMapper,
                currentUserProvider,
                new AiSensitiveDataSanitizer());
    }

    @Test
    void createRun_sanitizesPromptBeforeInsert() {
        TUser user = new TUser();
        user.setId(7);
        user.setName("销售顾问");
        when(currentUserProvider.getCurrentUser()).thenReturn(user);
        when(runMapper.insert(any(TAiRun.class))).thenReturn(1);
        ArgumentCaptor<TAiRun> captor = ArgumentCaptor.forClass(TAiRun.class);

        TAiRun run = service.createRun(new AiCreateRunCommand(
                20L,
                null,
                1,
                AiEntryPoint.SIDE_PANEL,
                "CUSTOMER",
                "12",
                "客户电话 13812345678 Authorization: Bearer abc.secret",
                LocalDateTime.now().plusHours(1)));

        assertNotNull(run.getRunNo());
        org.mockito.Mockito.verify(runMapper).insert(captor.capture());
        TAiRun inserted = captor.getValue();
        assertEquals(7, inserted.getUserId());
        assertEquals(20L, inserted.getConversationId());
        assertEquals(1, inserted.getTurnNo());
        assertEquals("SIDE_PANEL", inserted.getEntryPoint());
        assertFalse(inserted.getPromptSummary().contains("13812345678"));
        assertFalse(inserted.getPromptSummary().contains("abc.secret"));
    }

    @Test
    void appendMessage_sanitizesContentBeforeInsert() {
        when(currentUserProvider.getCurrentUserId()).thenReturn(7);
        when(messageMapper.insert(any(TAiMessage.class))).thenReturn(1);
        ArgumentCaptor<TAiMessage> captor = ArgumentCaptor.forClass(TAiMessage.class);

        service.appendMessage(new AiMessageCommand(
                20L,
                1L,
                AiMessageRole.USER,
                1,
                true,
                "Cookie: sid=raw 6222021234567890123"));

        org.mockito.Mockito.verify(messageMapper).insert(captor.capture());
        assertEquals(20L, captor.getValue().getConversationId());
        assertEquals(true, captor.getValue().getVisibleToUser());
        assertFalse(captor.getValue().getContentSummary().contains("sid=raw"));
        assertFalse(captor.getValue().getContentSummary().contains("6222021234567890123"));
    }

    @Test
    void saveProposal_shouldPersistImpactSummaryAndMoveRunToWaitingForApproval() {
        when(currentUserProvider.getCurrentUserId()).thenReturn(7);
        when(proposalMapper.insert(any(TAiActionProposal.class))).thenReturn(1);
        when(runMapper.updateStatus(1L, AiRunStatus.WAITING_FOR_APPROVAL.name(), "", "")).thenReturn(1);
        ArgumentCaptor<TAiActionProposal> captor = ArgumentCaptor.forClass(TAiActionProposal.class);

        service.saveProposal(new AiProposalCommand(
                1L,
                AiProposalType.CREATE_FOLLOW_TASK,
                AiRiskLevel.LOW,
                "follow-task:create",
                "CUSTOMER",
                "10",
                "{}",
                "hash",
                "创建跟进任务",
                "确认后将创建一条跟进任务。",
                LocalDateTime.now().plusMinutes(30)));

        verify(proposalMapper).insert(captor.capture());
        assertEquals("确认后将创建一条跟进任务。", captor.getValue().getImpactSummary());
        verify(runMapper).updateStatus(1L, AiRunStatus.WAITING_FOR_APPROVAL.name(), "", "");
    }

    @Test
    void saveProposal_shouldKeepExecutableSnapshotAndHashUnsanitized() throws Exception {
        when(currentUserProvider.getCurrentUserId()).thenReturn(7);
        when(proposalMapper.insert(any(TAiActionProposal.class))).thenReturn(1);
        when(runMapper.updateStatus(1L, AiRunStatus.WAITING_FOR_APPROVAL.name(), "", "")).thenReturn(1);
        ArgumentCaptor<TAiActionProposal> captor = ArgumentCaptor.forClass(TAiActionProposal.class);
        String normalized = "{\"summary\":\"客户电话 13812345678\\nAuthorization: Bearer abc.secret\"}";
        String hash = sha256(normalized);

        service.saveProposal(new AiProposalCommand(
                1L,
                AiProposalType.CREATE_COMMUNICATION_RECORD,
                AiRiskLevel.LOW,
                "communication-record:create",
                "CUSTOMER",
                "10",
                normalized,
                hash,
                "客户电话 13812345678",
                "Authorization: Bearer abc.secret",
                LocalDateTime.now().plusMinutes(30)));

        verify(proposalMapper).insert(captor.capture());
        TAiActionProposal inserted = captor.getValue();
        assertEquals(normalized, inserted.getNormalizedParams());
        assertEquals(hash, inserted.getParamsHash());
        assertFalse(inserted.getParamsSummary().contains("13812345678"));
        assertFalse(inserted.getImpactSummary().contains("abc.secret"));
    }

    @Test
    void getOwnedRunTrace_shouldLoadRecoverableRunData() {
        TAiRun run = new TAiRun();
        run.setId(1L);
        run.setRunNo("AIR1");
        run.setStatus(AiRunStatus.WAITING_FOR_APPROVAL.name());
        run.setEntryPoint(AiEntryPoint.PAGE.name());
        when(currentUserProvider.getCurrentUserId()).thenReturn(7);
        when(runMapper.selectOwnedByRunNo("AIR1", 7)).thenReturn(run);
        TAiMessage message = new TAiMessage();
        message.setId(5L);
        message.setRole(AiMessageRole.ASSISTANT.name());
        message.setSequenceNo(2);
        message.setContentSummary("客户摘要");
        when(messageMapper.selectByRunId(1L)).thenReturn(java.util.List.of(message));
        TAiToolCall toolCall = new TAiToolCall();
        toolCall.setId(6L);
        toolCall.setToolName("get_customer_profile");
        toolCall.setPermissionCode("customer:view");
        toolCall.setRiskLevel(AiRiskLevel.READONLY.name());
        toolCall.setInputSummary("{\"customerId\":12}");
        toolCall.setOutputSummary("客户档案摘要");
        toolCall.setObjectRefs("CUSTOMER:12");
        toolCall.setDisplayPayloadJson("{\"customerName\":\"张伟\",\"phoneMasked\":\"138****0000\"}");
        toolCall.setResultStatus("SUCCESS");
        when(toolCallMapper.selectByRunId(1L)).thenReturn(java.util.List.of(toolCall));
        when(proposalMapper.selectByRunId(1L)).thenReturn(java.util.List.of());
        when(approvalMapper.selectByRunId(1L)).thenReturn(java.util.List.of());
        when(executionEventMapper.selectByRunId(1L)).thenReturn(java.util.List.of());
        TAiWorkflow workflow = new TAiWorkflow();
        workflow.setId(8L);
        workflow.setWorkflowNo("AIW1");
        workflow.setWorkflowType("CUSTOMER_FOLLOW_UP");
        workflow.setTitle("客户跟进辅助工作流");
        workflow.setStatus("WAITING_USER_CONFIRMATION");
        when(workflowMapper.selectByRunId(1L)).thenReturn(java.util.List.of(workflow));
        TAiWorkflowStep step = new TAiWorkflowStep();
        step.setId(9L);
        step.setStepNo(1);
        step.setStepType("READ_CUSTOMER");
        step.setTitle("查询客户档案");
        step.setStatus("COMPLETED");
        when(workflowStepMapper.selectByWorkflowId(8L)).thenReturn(java.util.List.of(step));

        AiRunTraceResponse trace = service.getOwnedRunTrace("AIR1");

        assertEquals("AIR1", trace.getRun().getRunNo());
        assertEquals("客户摘要", trace.getMessages().get(0).contentSummary());
        assertEquals("get_customer_profile", trace.getToolCalls().get(0).toolName());
        assertEquals("客户档案摘要", trace.getToolCalls().get(0).outputSummary());
        assertEquals("张伟", ((Map<?, ?>) trace.getToolCalls().get(0).displayPayload()).get("customerName"));
        assertEquals("AIW1", trace.getWorkflows().get(0).workflowNo());
        assertEquals("READ_CUSTOMER", trace.getWorkflows().get(0).steps().get(0).stepType());
    }

    private String sha256(String value) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
    }
}
