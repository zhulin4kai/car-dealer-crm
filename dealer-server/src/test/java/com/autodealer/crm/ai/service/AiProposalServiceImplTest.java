package com.autodealer.crm.ai.service;

import com.autodealer.crm.ai.ToolExecutionContext;
import com.autodealer.crm.ai.dto.AiApprovalCommand;
import com.autodealer.crm.ai.dto.AiExecutionEventCommand;
import com.autodealer.crm.ai.dto.AiProposalCommand;
import com.autodealer.crm.ai.dto.AiProposalConfirmResponse;
import com.autodealer.crm.ai.dto.tool.AiToolDtos;
import com.autodealer.crm.ai.enums.AiApprovalDecision;
import com.autodealer.crm.ai.enums.AiProposalStatus;
import com.autodealer.crm.ai.enums.AiProposalType;
import com.autodealer.crm.ai.enums.AiRiskLevel;
import com.autodealer.crm.ai.mapper.TAiActionProposalMapper;
import com.autodealer.crm.ai.model.TAiActionProposal;
import com.autodealer.crm.ai.model.TAiRun;
import com.autodealer.crm.ai.service.impl.AiProposalServiceImpl;
import com.autodealer.crm.ai.service.impl.AiProposalFailureRecorder;
import com.autodealer.crm.config.security.CurrentUserProvider;
import com.autodealer.crm.constant.PermissionCodes;
import com.autodealer.crm.dto.CreateCommunicationRecordRequest;
import com.autodealer.crm.dto.CreateFollowTaskRequest;
import com.autodealer.crm.exception.BusinessException;
import com.autodealer.crm.model.TCommunicationRecord;
import com.autodealer.crm.model.TFollowTask;
import com.autodealer.crm.result.CodeEnum;
import com.autodealer.crm.service.CommunicationRecordService;
import com.autodealer.crm.service.FollowTaskService;
import com.autodealer.crm.service.impl.FollowRelatedObjectResolver;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiProposalServiceImplTest {
    @Mock private TAiActionProposalMapper proposalMapper;
    @Mock private AiTraceService traceService;
    @Mock private CommunicationRecordService communicationRecordService;
    @Mock private FollowTaskService followTaskService;
    @Mock private FollowRelatedObjectResolver relatedObjectResolver;
    @Mock private CurrentUserProvider currentUserProvider;
    @Mock private AiProposalFailureRecorder failureRecorder;

    private AiProposalServiceImpl service;
    private ObjectMapper objectMapper;
    private TAiRun run;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        service = new AiProposalServiceImpl(
                proposalMapper,
                traceService,
                communicationRecordService,
                followTaskService,
                relatedObjectResolver,
                currentUserProvider,
                new AiSensitiveDataSanitizer(),
                objectMapper,
                failureRecorder);
        run = new TAiRun();
        run.setId(1L);
        run.setRunNo("AIR1");
        run.setUserId(7);
    }

    @Test
    void createFollowTaskProposal_shouldSaveProposalWithoutBusinessWrite() {
        when(traceService.saveProposal(any())).thenAnswer(invocation -> toPersisted(invocation.getArgument(0)));
        AiToolDtos.CreateFollowTaskProposalRequest request = followTaskProposalRequest();

        AiToolDtos.ProposalCreated result = service.createFollowTaskProposal(new ToolExecutionContext(run), request);

        assertEquals("create_follow_task_proposal", result.proposalType());
        ArgumentCaptor<AiProposalCommand> captor = ArgumentCaptor.forClass(AiProposalCommand.class);
        verify(traceService).saveProposal(captor.capture());
        assertEquals(PermissionCodes.FOLLOW_TASK_CREATE, captor.getValue().permissionCode());
        verify(followTaskService, never()).createFollowTask(any());
    }

    @Test
    void createCommunicationRecordProposal_shouldRejectUnsupportedCommunicationMethodBeforeSave() {
        AiToolDtos.CreateCommunicationRecordProposalRequest request = new AiToolDtos.CreateCommunicationRecordProposalRequest();
        request.setRelatedObjectType("CUSTOMER");
        request.setRelatedObjectId(10L);
        request.setCommunicationMethod("UNSUPPORTED_METHOD");
        request.setSummary("沟通记录");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.createCommunicationRecordProposal(new ToolExecutionContext(run), request));

        assertEquals(CodeEnum.AI_TOOL_ARGUMENT_INVALID, ex.getCodeEnum());
        verify(traceService, never()).saveProposal(any());
        verify(communicationRecordService, never()).createCommunicationRecord(any());
    }

    @Test
    void createFollowTaskProposal_shouldRejectUnsupportedTaskTypeBeforeSave() {
        AiToolDtos.CreateFollowTaskProposalRequest request = followTaskProposalRequest();
        request.setTaskType("UNSUPPORTED_TASK");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.createFollowTaskProposal(new ToolExecutionContext(run), request));

        assertEquals(CodeEnum.AI_TOOL_ARGUMENT_INVALID, ex.getCodeEnum());
        verify(traceService, never()).saveProposal(any());
        verify(followTaskService, never()).createFollowTask(any());
    }

    @Test
    void confirmExpiredProposal_shouldRejectWithoutBusinessWrite() throws Exception {
        TAiActionProposal proposal = pendingFollowTaskProposal(LocalDateTime.now().minusMinutes(1));
        when(proposalMapper.selectById(9L)).thenReturn(proposal);
        when(currentUserProvider.getCurrentUserId()).thenReturn(7);
        when(proposalMapper.updateStatusIfCurrent(eq(9L),
                eq(AiProposalStatus.PENDING_CONFIRMATION.name()),
                eq(AiProposalStatus.EXPIRED.name()), any(), any()))
                .thenReturn(1);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.confirm(9L));

        assertEquals(CodeEnum.AI_PROPOSAL_EXPIRED, ex.getCodeEnum());
        verify(traceService).recordApproval(org.mockito.ArgumentMatchers.argThat(command ->
                command.decision() == AiApprovalDecision.EXPIRED));
        verify(traceService).recordExecutionEvent(org.mockito.ArgumentMatchers.argThat(command ->
                "PROPOSAL_EXPIRED".equals(command.eventType())));
        verify(followTaskService, never()).createFollowTask(any());
    }

    @Test
    void confirmHashMismatch_shouldRejectWithoutBusinessWrite() throws Exception {
        TAiActionProposal proposal = pendingFollowTaskProposal(LocalDateTime.now().plusMinutes(20));
        proposal.setParamsHash("tampered");
        when(proposalMapper.selectById(9L)).thenReturn(proposal);
        when(currentUserProvider.getCurrentUserId()).thenReturn(7);
        when(currentUserProvider.hasAuthority(PermissionCodes.FOLLOW_TASK_CREATE)).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.confirm(9L));

        assertEquals(CodeEnum.AI_PROPOSAL_HASH_MISMATCH, ex.getCodeEnum());
        verify(followTaskService, never()).createFollowTask(any());
    }

    @Test
    void confirmCommunicationRecordProposal_shouldAcceptSensitiveExecutableSnapshotWhenHashMatches()
            throws Exception {
        TAiActionProposal proposal = pendingCommunicationRecordProposal(LocalDateTime.now().plusMinutes(20));
        when(proposalMapper.selectById(9L)).thenReturn(proposal);
        when(currentUserProvider.getCurrentUserId()).thenReturn(7);
        when(currentUserProvider.hasAuthority(PermissionCodes.COMMUNICATION_RECORD_CREATE)).thenReturn(true);
        when(proposalMapper.updateStatusIfCurrent(eq(9L), any(), any(), any(), any())).thenReturn(1);
        TCommunicationRecord record = new TCommunicationRecord();
        record.setId(66L);
        when(communicationRecordService.createCommunicationRecord(any())).thenReturn(record);

        AiProposalConfirmResponse response = service.confirm(9L);

        assertEquals(AiProposalStatus.EXECUTED.name(), response.getStatus());
        assertEquals("COMMUNICATION_RECORD", response.getObjectType());
        assertEquals("66", response.getObjectId());
        ArgumentCaptor<CreateCommunicationRecordRequest> requestCaptor =
                ArgumentCaptor.forClass(CreateCommunicationRecordRequest.class);
        verify(communicationRecordService).createCommunicationRecord(requestCaptor.capture());
        assertEquals("客户电话 13812345678 Authorization: Bearer abc.secret",
                requestCaptor.getValue().getSummary());
        verify(traceService).recordApproval(org.mockito.ArgumentMatchers.argThat(command ->
                command.decision() == AiApprovalDecision.CONFIRMED));
    }

    @Test
    void confirmPermissionChanged_shouldRejectWithoutBusinessWrite() throws Exception {
        TAiActionProposal proposal = pendingFollowTaskProposal(LocalDateTime.now().plusMinutes(20));
        when(proposalMapper.selectById(9L)).thenReturn(proposal);
        when(currentUserProvider.getCurrentUserId()).thenReturn(7);
        when(currentUserProvider.hasAuthority(PermissionCodes.FOLLOW_TASK_CREATE)).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.confirm(9L));

        assertEquals(CodeEnum.AI_TOOL_FORBIDDEN, ex.getCodeEnum());
        verify(followTaskService, never()).createFollowTask(any());
    }

    @Test
    void rejectProposal_shouldWriteApprovalAndExecutionEvent() throws Exception {
        TAiActionProposal proposal = pendingFollowTaskProposal(LocalDateTime.now().plusMinutes(20));
        when(proposalMapper.selectById(9L)).thenReturn(proposal);
        when(currentUserProvider.getCurrentUserId()).thenReturn(7);
        when(proposalMapper.updateStatusIfCurrent(eq(9L),
                eq(AiProposalStatus.PENDING_CONFIRMATION.name()),
                eq(AiProposalStatus.REJECTED.name()), any(), any()))
                .thenReturn(1);

        AiProposalConfirmResponse response = service.reject(9L);

        assertEquals(AiProposalStatus.REJECTED.name(), response.getStatus());
        ArgumentCaptor<AiApprovalCommand> approvalCaptor = ArgumentCaptor.forClass(AiApprovalCommand.class);
        verify(traceService).recordApproval(approvalCaptor.capture());
        assertEquals(AiApprovalDecision.REJECTED, approvalCaptor.getValue().decision());
        ArgumentCaptor<AiExecutionEventCommand> eventCaptor = ArgumentCaptor.forClass(AiExecutionEventCommand.class);
        verify(traceService).recordExecutionEvent(eventCaptor.capture());
        assertEquals("PROPOSAL_REJECTED", eventCaptor.getValue().eventType());
        verify(followTaskService, never()).createFollowTask(any());
    }

    @Test
    void confirmFollowTaskProposal_shouldExecuteSavedParams() throws Exception {
        TAiActionProposal proposal = pendingFollowTaskProposal(LocalDateTime.now().plusMinutes(20));
        when(proposalMapper.selectById(9L)).thenReturn(proposal);
        when(currentUserProvider.getCurrentUserId()).thenReturn(7);
        when(currentUserProvider.hasAuthority(PermissionCodes.FOLLOW_TASK_CREATE)).thenReturn(true);
        when(proposalMapper.updateStatusIfCurrent(eq(9L), any(), any(), any(), any())).thenReturn(1);
        TFollowTask task = new TFollowTask();
        task.setId(88L);
        when(followTaskService.createFollowTask(any())).thenReturn(task);

        AiProposalConfirmResponse response = service.confirm(9L);

        assertEquals(AiProposalStatus.EXECUTED.name(), response.getStatus());
        assertEquals("FOLLOW_TASK", response.getObjectType());
        assertEquals("88", response.getObjectId());
        verify(traceService).recordApproval(org.mockito.ArgumentMatchers.argThat(command ->
                command.decision() == AiApprovalDecision.CONFIRMED));
        ArgumentCaptor<CreateFollowTaskRequest> requestCaptor = ArgumentCaptor.forClass(CreateFollowTaskRequest.class);
        verify(followTaskService).createFollowTask(requestCaptor.capture());
        assertEquals("AI 跟进", requestCaptor.getValue().getTitle());
        assertEquals(7, requestCaptor.getValue().getOwnerId());
    }

    @Test
    void confirmExecutedProposal_shouldBeIdempotentWithoutSecondBusinessWrite() throws Exception {
        TAiActionProposal proposal = pendingFollowTaskProposal(LocalDateTime.now().plusMinutes(20));
        proposal.setStatus(AiProposalStatus.EXECUTED.name());
        proposal.setResultSummary("已创建跟进任务");
        when(proposalMapper.selectById(9L)).thenReturn(proposal);
        when(currentUserProvider.getCurrentUserId()).thenReturn(7);

        AiProposalConfirmResponse response = service.confirm(9L);

        assertEquals(AiProposalStatus.EXECUTED.name(), response.getStatus());
        assertEquals("已创建跟进任务", response.getResultSummary());
        verify(followTaskService, never()).createFollowTask(any());
        verify(proposalMapper, never()).updateStatusIfCurrent(any(), any(), any(), any(), any());
    }

    @Test
    void confirmConcurrentStatusChange_shouldRejectWithoutBusinessWrite() throws Exception {
        TAiActionProposal proposal = pendingFollowTaskProposal(LocalDateTime.now().plusMinutes(20));
        when(proposalMapper.selectById(9L)).thenReturn(proposal);
        when(currentUserProvider.getCurrentUserId()).thenReturn(7);
        when(currentUserProvider.hasAuthority(PermissionCodes.FOLLOW_TASK_CREATE)).thenReturn(true);
        when(proposalMapper.updateStatusIfCurrent(eq(9L),
                eq(AiProposalStatus.PENDING_CONFIRMATION.name()),
                eq(AiProposalStatus.CONFIRMED.name()), any(), any()))
                .thenReturn(0);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.confirm(9L));

        assertEquals(CodeEnum.TRAN_STATE_CONFLICT, ex.getCodeEnum());
        verify(followTaskService, never()).createFollowTask(any());
    }

    @Test
    void confirmBusinessFailure_shouldMarkFailedAndRecordExecutionEvent() throws Exception {
        TAiActionProposal proposal = pendingFollowTaskProposal(LocalDateTime.now().plusMinutes(20));
        when(proposalMapper.selectById(9L)).thenReturn(proposal);
        when(currentUserProvider.getCurrentUserId()).thenReturn(7);
        when(currentUserProvider.hasAuthority(PermissionCodes.FOLLOW_TASK_CREATE)).thenReturn(true);
        when(proposalMapper.updateStatusIfCurrent(eq(9L), any(), any(), any(), any())).thenReturn(1);
        when(followTaskService.createFollowTask(any()))
                .thenThrow(new BusinessException(CodeEnum.PARAM_ERROR, "业务状态变化"));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.confirm(9L));

        assertEquals(CodeEnum.PARAM_ERROR, ex.getCodeEnum());
        verify(proposalMapper).updateStatusIfCurrent(eq(9L),
                eq(AiProposalStatus.CONFIRMED.name()),
                eq(AiProposalStatus.FAILED.name()),
                eq(CodeEnum.PARAM_ERROR.name()),
                eq("业务状态变化"));
        verify(traceService).recordExecutionEvent(org.mockito.ArgumentMatchers.argThat(command ->
                "PROPOSAL_EXECUTE_FAILED".equals(command.eventType())
                        && command.resultStatus().name().equals("FAILED")
                        && CodeEnum.PARAM_ERROR.name().equals(command.errorCode())));
    }

    @Test
    void confirmBusinessFailure_shouldPersistFailureAfterTransactionRollback() throws Exception {
        TAiActionProposal proposal = pendingFollowTaskProposal(LocalDateTime.now().plusMinutes(20));
        when(proposalMapper.selectById(9L)).thenReturn(proposal);
        when(currentUserProvider.getCurrentUserId()).thenReturn(7);
        when(currentUserProvider.hasAuthority(PermissionCodes.FOLLOW_TASK_CREATE)).thenReturn(true);
        when(proposalMapper.updateStatusIfCurrent(eq(9L), any(), any(), any(), any())).thenReturn(1);
        when(followTaskService.createFollowTask(any()))
                .thenThrow(new BusinessException(CodeEnum.PARAM_ERROR, "业务状态变化"));

        TransactionSynchronizationManager.initSynchronization();
        try {
            assertThrows(BusinessException.class, () -> service.confirm(9L));
            var synchronizations = TransactionSynchronizationManager.getSynchronizations();
            assertEquals(1, synchronizations.size());

            synchronizations.get(0).afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);

            verify(failureRecorder).recordAfterRollback(
                    proposal, CodeEnum.PARAM_ERROR.name(), "业务状态变化");
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    private TAiActionProposal toPersisted(AiProposalCommand command) {
        TAiActionProposal proposal = new TAiActionProposal();
        proposal.setId(9L);
        proposal.setRunId(command.runId());
        proposal.setProposalType(command.proposalType().getCode());
        proposal.setRiskLevel(command.riskLevel().name());
        proposal.setPermissionCode(command.permissionCode());
        proposal.setRelatedObjectType(command.relatedObjectType());
        proposal.setRelatedObjectId(command.relatedObjectId());
        proposal.setNormalizedParams(command.normalizedParams());
        proposal.setParamsHash(command.paramsHash());
        proposal.setParamsSummary(command.paramsSummary());
        proposal.setImpactSummary(command.impactSummary());
        proposal.setExpiresTime(command.expiresTime());
        return proposal;
    }

    private AiToolDtos.CreateFollowTaskProposalRequest followTaskProposalRequest() {
        AiToolDtos.CreateFollowTaskProposalRequest request = new AiToolDtos.CreateFollowTaskProposalRequest();
        request.setTitle("AI 跟进");
        request.setTaskType("PHONE_FOLLOW_UP");
        request.setRelatedObjectType("CUSTOMER");
        request.setRelatedObjectId(10L);
        request.setOwnerId(7);
        request.setPriority("HIGH");
        request.setDueTime(LocalDateTime.of(2026, 7, 2, 10, 0));
        return request;
    }

    private TAiActionProposal pendingFollowTaskProposal(LocalDateTime expiresTime) throws Exception {
        CreateFollowTaskRequest request = new CreateFollowTaskRequest();
        request.setTitle("AI 跟进");
        request.setTaskType("PHONE_FOLLOW_UP");
        request.setRelatedObjectType("CUSTOMER");
        request.setRelatedObjectId(10L);
        request.setOwnerId(7);
        request.setPriority("HIGH");
        request.setDueTime(LocalDateTime.of(2026, 7, 2, 10, 0));
        String normalized = objectMapper.writeValueAsString(request);
        TAiActionProposal proposal = new TAiActionProposal();
        proposal.setId(9L);
        proposal.setRunId(1L);
        proposal.setProposalType(AiProposalType.CREATE_FOLLOW_TASK.getCode());
        proposal.setStatus(AiProposalStatus.PENDING_CONFIRMATION.name());
        proposal.setRiskLevel(AiRiskLevel.LOW.name());
        proposal.setPermissionCode(PermissionCodes.FOLLOW_TASK_CREATE);
        proposal.setRelatedObjectType("CUSTOMER");
        proposal.setRelatedObjectId("10");
        proposal.setNormalizedParams(normalized);
        proposal.setParamsHash(sha256(normalized));
        proposal.setExpiresTime(expiresTime);
        proposal.setCreateBy(7);
        return proposal;
    }

    private TAiActionProposal pendingCommunicationRecordProposal(LocalDateTime expiresTime) throws Exception {
        CreateCommunicationRecordRequest request = new CreateCommunicationRecordRequest();
        request.setRelatedObjectType("CUSTOMER");
        request.setRelatedObjectId(10L);
        request.setCommunicationMethod("PHONE");
        request.setCommunicationTime(LocalDateTime.of(2026, 7, 2, 10, 0));
        request.setSummary("客户电话 13812345678 Authorization: Bearer abc.secret");
        String normalized = objectMapper.writeValueAsString(request);
        TAiActionProposal proposal = new TAiActionProposal();
        proposal.setId(9L);
        proposal.setRunId(1L);
        proposal.setProposalType(AiProposalType.CREATE_COMMUNICATION_RECORD.getCode());
        proposal.setStatus(AiProposalStatus.PENDING_CONFIRMATION.name());
        proposal.setRiskLevel(AiRiskLevel.LOW.name());
        proposal.setPermissionCode(PermissionCodes.COMMUNICATION_RECORD_CREATE);
        proposal.setRelatedObjectType("CUSTOMER");
        proposal.setRelatedObjectId("10");
        proposal.setNormalizedParams(normalized);
        proposal.setParamsHash(sha256(normalized));
        proposal.setExpiresTime(expiresTime);
        proposal.setCreateBy(7);
        return proposal;
    }

    private String sha256(String value) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
    }
}
