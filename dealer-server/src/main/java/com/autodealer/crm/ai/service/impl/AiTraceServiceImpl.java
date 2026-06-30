package com.autodealer.crm.ai.service.impl;

import com.autodealer.crm.ai.dto.AiApprovalCommand;
import com.autodealer.crm.ai.dto.AiCreateRunCommand;
import com.autodealer.crm.ai.dto.AiExecutionEventCommand;
import com.autodealer.crm.ai.dto.AiMessageCommand;
import com.autodealer.crm.ai.dto.AiProposalCommand;
import com.autodealer.crm.ai.dto.AiRunTraceResponse;
import com.autodealer.crm.ai.dto.AiToolCallCommand;
import com.autodealer.crm.ai.enums.AiConversationStatus;
import com.autodealer.crm.ai.enums.AiEntryPoint;
import com.autodealer.crm.ai.enums.AiProposalStatus;
import com.autodealer.crm.ai.enums.AiRunStatus;
import com.autodealer.crm.ai.mapper.TAiActionProposalMapper;
import com.autodealer.crm.ai.mapper.TAiApprovalMapper;
import com.autodealer.crm.ai.mapper.TAiConversationMapper;
import com.autodealer.crm.ai.mapper.TAiExecutionEventMapper;
import com.autodealer.crm.ai.mapper.TAiMessageMapper;
import com.autodealer.crm.ai.mapper.TAiRunMapper;
import com.autodealer.crm.ai.mapper.TAiToolCallMapper;
import com.autodealer.crm.ai.mapper.TAiWorkflowMapper;
import com.autodealer.crm.ai.mapper.TAiWorkflowStepMapper;
import com.autodealer.crm.ai.model.TAiActionProposal;
import com.autodealer.crm.ai.model.TAiApproval;
import com.autodealer.crm.ai.model.TAiConversation;
import com.autodealer.crm.ai.model.TAiExecutionEvent;
import com.autodealer.crm.ai.model.TAiMessage;
import com.autodealer.crm.ai.model.TAiRun;
import com.autodealer.crm.ai.model.TAiToolCall;
import com.autodealer.crm.ai.model.TAiWorkflow;
import com.autodealer.crm.ai.service.AiSensitiveDataSanitizer;
import com.autodealer.crm.ai.service.AiTraceService;
import com.autodealer.crm.config.security.CurrentUserProvider;
import com.autodealer.crm.exception.BusinessException;
import com.autodealer.crm.model.TUser;
import com.autodealer.crm.result.CodeEnum;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class AiTraceServiceImpl implements AiTraceService {
    private static final int MAX_RUN_PAGE_SIZE = 100;

    private final TAiConversationMapper conversationMapper;
    private final TAiRunMapper runMapper;
    private final TAiMessageMapper messageMapper;
    private final TAiToolCallMapper toolCallMapper;
    private final TAiActionProposalMapper proposalMapper;
    private final TAiApprovalMapper approvalMapper;
    private final TAiExecutionEventMapper executionEventMapper;
    private final TAiWorkflowMapper workflowMapper;
    private final TAiWorkflowStepMapper workflowStepMapper;
    private final CurrentUserProvider currentUserProvider;
    private final AiSensitiveDataSanitizer sanitizer;

    public AiTraceServiceImpl(TAiConversationMapper conversationMapper,
                              TAiRunMapper runMapper,
                              TAiMessageMapper messageMapper,
                              TAiToolCallMapper toolCallMapper,
                              TAiActionProposalMapper proposalMapper,
                              TAiApprovalMapper approvalMapper,
                              TAiExecutionEventMapper executionEventMapper,
                              TAiWorkflowMapper workflowMapper,
                              TAiWorkflowStepMapper workflowStepMapper,
                              CurrentUserProvider currentUserProvider,
                              AiSensitiveDataSanitizer sanitizer) {
        this.conversationMapper = conversationMapper;
        this.runMapper = runMapper;
        this.messageMapper = messageMapper;
        this.toolCallMapper = toolCallMapper;
        this.proposalMapper = proposalMapper;
        this.approvalMapper = approvalMapper;
        this.executionEventMapper = executionEventMapper;
        this.workflowMapper = workflowMapper;
        this.workflowStepMapper = workflowStepMapper;
        this.currentUserProvider = currentUserProvider;
        this.sanitizer = sanitizer;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TAiConversation createConversation(AiEntryPoint entryPoint, String contextObjectType,
                                              String contextObjectId, String title) {
        TUser currentUser = currentUserProvider.getCurrentUser();
        LocalDateTime now = LocalDateTime.now();
        TAiConversation conversation = new TAiConversation();
        conversation.setConversationNo("AIC" + UUID.randomUUID().toString().replace("-", ""));
        conversation.setUserId(currentUser.getId());
        conversation.setTitle(conversationTitle(title));
        conversation.setStatus(AiConversationStatus.ACTIVE.name());
        conversation.setEntryPoint(entryPoint.name());
        conversation.setContextObjectType(normalizeContext(contextObjectType));
        conversation.setContextObjectId(normalizeContext(contextObjectId));
        conversation.setSummaryText("");
        conversation.setCreateTime(now);
        conversation.setCreateBy(currentUser.getId());
        conversation.setEditTime(now);
        conversation.setEditBy(currentUser.getId());
        requireOne(conversationMapper.insert(conversation), "AI Conversation 写入失败");
        return conversation;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TAiConversation findOrCreateConversation(AiEntryPoint entryPoint, String contextObjectType,
                                                    String contextObjectId, String title) {
        TAiConversation existing = conversationMapper.selectActiveByContext(
                currentUserProvider.getCurrentUserId(),
                normalizeContext(contextObjectType),
                normalizeContext(contextObjectId));
        if (existing != null) {
            return existing;
        }
        return createConversation(entryPoint, contextObjectType, contextObjectId, title);
    }

    @Override
    public TAiConversation getOwnedConversation(String conversationNo) {
        TAiConversation conversation = conversationMapper.selectOwnedByConversationNo(
                conversationNo, currentUserProvider.getCurrentUserId());
        if (conversation == null) {
            throw new BusinessException(CodeEnum.AI_CONVERSATION_NOT_FOUND, "AI 会话不存在或无权访问");
        }
        return conversation;
    }

    @Override
    public TAiConversation getConversationById(Long conversationId) {
        TAiConversation conversation = conversationMapper.selectById(conversationId);
        if (conversation == null) {
            throw new BusinessException(CodeEnum.AI_CONVERSATION_NOT_FOUND, "AI 会话不存在");
        }
        return conversation;
    }

    @Override
    public List<TAiConversation> listOwnedConversations(boolean includeArchived) {
        return conversationMapper.selectByUserId(currentUserProvider.getCurrentUserId(), includeArchived);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TAiConversation renameConversation(String conversationNo, String title) {
        TAiConversation conversation = getOwnedConversation(conversationNo);
        if (AiConversationStatus.ARCHIVED.name().equals(conversation.getStatus())) {
            throw new BusinessException(CodeEnum.AI_CONVERSATION_ARCHIVED, "AI 会话已归档");
        }
        requireOne(conversationMapper.updateTitle(conversation.getId(), conversationTitle(title),
                LocalDateTime.now(), currentUserProvider.getCurrentUserId()), "AI Conversation 重命名失败");
        return getOwnedConversation(conversationNo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TAiConversation archiveConversation(String conversationNo) {
        TAiConversation conversation = getOwnedConversation(conversationNo);
        if (!AiConversationStatus.ARCHIVED.name().equals(conversation.getStatus())) {
            requireOne(conversationMapper.archive(conversation.getId(), LocalDateTime.now(),
                    currentUserProvider.getCurrentUserId()), "AI Conversation 归档失败");
        }
        return getOwnedConversation(conversationNo);
    }

    @Override
    public List<TAiMessage> listConversationMessages(Long conversationId) {
        return messageMapper.selectByConversationId(conversationId);
    }

    @Override
    public List<TAiMessage> listRecentVisibleMessages(Long conversationId, Long excludeRunId, int limit) {
        return messageMapper.selectRecentVisibleByConversationId(conversationId, excludeRunId, Math.max(1, limit));
    }

    @Override
    public TAiRun getLatestRunByConversationId(Long conversationId) {
        return runMapper.selectLatestByConversationId(conversationId);
    }

    @Override
    public List<TAiRun> listRunsByConversationId(Long conversationId) {
        return runMapper.selectByConversationId(conversationId);
    }

    @Override
    public int nextTurnNo(Long conversationId) {
        Integer maxTurnNo = runMapper.selectMaxTurnNoByConversationId(conversationId);
        return (maxTurnNo == null ? 0 : maxTurnNo) + 1;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateConversationAfterRun(Long conversationId, String lastRunNo, String summaryText) {
        LocalDateTime now = LocalDateTime.now();
        requireOne(conversationMapper.updateAfterRun(conversationId,
                sanitizer.sanitize(lastRunNo, 64),
                sanitizer.sanitize(summaryText, 2000),
                now,
                now,
                currentUserProvider.getCurrentUserId()), "AI Conversation 更新失败");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TAiRun createRun(AiCreateRunCommand command) {
        TUser currentUser = currentUserProvider.getCurrentUser();
        LocalDateTime now = LocalDateTime.now();
        TAiRun run = new TAiRun();
        run.setRunNo("AIR" + UUID.randomUUID().toString().replace("-", ""));
        run.setConversationId(command.conversationId());
        run.setParentRunId(command.parentRunId());
        run.setTurnNo(command.turnNo());
        run.setUserId(currentUser.getId());
        run.setUserName(sanitizer.sanitize(currentUser.getName(), 64));
        run.setEntryPoint(command.entryPoint().name());
        run.setContextObjectType(sanitizer.sanitize(command.contextObjectType(), 64));
        run.setContextObjectId(sanitizer.sanitize(command.contextObjectId(), 64));
        run.setPromptSummary(sanitizer.sanitize(command.prompt(), 500));
        run.setStatus(AiRunStatus.CREATED.name());
        run.setExpiresTime(command.expiresTime());
        run.setCreateTime(now);
        run.setCreateBy(currentUser.getId());
        requireOne(runMapper.insert(run), "AI Run 写入失败");
        return run;
    }

    @Override
    public TAiRun getOwnedRun(String runNo) {
        TAiRun run = runMapper.selectOwnedByRunNo(runNo, currentUserProvider.getCurrentUserId());
        if (run == null) {
            throw new BusinessException(CodeEnum.ACCESS_DENIED, "AI Run 不存在或无权访问");
        }
        return run;
    }

    @Override
    public TAiRun getRunById(Long runId) {
        TAiRun run = runMapper.selectById(runId);
        if (run == null) {
            throw new BusinessException(CodeEnum.AI_RUN_NOT_FOUND, "AI Run 不存在");
        }
        return run;
    }

    @Override
    public List<TAiRun> listMyRuns(int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.max(1, Math.min(size, MAX_RUN_PAGE_SIZE));
        int offset = (safePage - 1) * safeSize;
        return runMapper.selectByUserId(currentUserProvider.getCurrentUserId(), offset, safeSize);
    }

    @Override
    public AiRunTraceResponse getOwnedRunTrace(String runNo) {
        TAiRun run = getOwnedRun(runNo);
        return getRunTrace(run);
    }

    @Override
    public AiRunTraceResponse getRunTrace(TAiRun run) {
        return AiRunTraceResponse.from(
                run,
                messageMapper.selectByRunId(run.getId()),
                toolCallMapper.selectByRunId(run.getId()),
                proposalMapper.selectByRunId(run.getId()),
                approvalMapper.selectByRunId(run.getId()),
                executionEventMapper.selectByRunId(run.getId()),
                workflowMapper.selectByRunId(run.getId()).stream()
                        .map(this::workflowWithSteps)
                        .toList());
    }

    private AiRunTraceResponse.WorkflowWithSteps workflowWithSteps(TAiWorkflow workflow) {
        return new AiRunTraceResponse.WorkflowWithSteps(
                workflow,
                workflowStepMapper.selectByWorkflowId(workflow.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRunStatus(Long runId, AiRunStatus status, String errorCode, String errorMessage) {
        requireOne(runMapper.updateStatus(runId, status.name(),
                sanitizer.sanitize(errorCode, 64), sanitizer.sanitize(errorMessage, 255)),
                "AI Run 状态更新失败");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateRunStatusIfNotTerminal(Long runId, AiRunStatus status, String errorCode, String errorMessage) {
        return runMapper.updateStatusIfNotTerminal(runId, status.name(),
                sanitizer.sanitize(errorCode, 64), sanitizer.sanitize(errorMessage, 255)) == 1;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean cancelRunIfCancellable(Long runId, String reason) {
        return runMapper.cancelIfCancellable(runId,
                CodeEnum.AI_RUN_CANCELLED.name(),
                sanitizer.sanitize(reason == null ? "用户停止生成" : reason, 255)) == 1;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TAiMessage appendMessage(AiMessageCommand command) {
        TAiMessage message = new TAiMessage();
        message.setConversationId(command.conversationId());
        message.setRunId(command.runId());
        message.setRole(command.role().name());
        message.setSequenceNo(command.sequenceNo());
        message.setVisibleToUser(command.visibleToUser() == null || command.visibleToUser());
        message.setContentSummary(sanitizer.sanitizeDisplayText(command.content(), 2000));
        message.setCreateTime(LocalDateTime.now());
        message.setCreateBy(currentUserProvider.getCurrentUserId());
        requireOne(messageMapper.insert(message), "AI Message 写入失败");
        return message;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TAiToolCall recordToolCall(AiToolCallCommand command) {
        TAiToolCall toolCall = new TAiToolCall();
        toolCall.setRunId(command.runId());
        toolCall.setToolName(sanitizer.sanitize(command.toolName(), 128));
        toolCall.setPermissionCode(sanitizer.sanitize(command.permissionCode(), 128));
        toolCall.setRiskLevel(command.riskLevel().name());
        toolCall.setInputSummary(sanitizer.sanitize(command.inputSummary(), 1000));
        toolCall.setOutputSummary(sanitizer.sanitize(command.outputSummary(), 1000));
        toolCall.setObjectRefs(sanitizer.sanitize(command.objectRefs(), 1000));
        toolCall.setResultStatus(command.resultStatus().name());
        toolCall.setErrorCode(sanitizer.sanitize(command.errorCode(), 64));
        toolCall.setDurationMs(command.durationMs());
        toolCall.setStartedTime(command.startedTime() == null ? LocalDateTime.now() : command.startedTime());
        toolCall.setCompletedTime(command.completedTime());
        toolCall.setCreateBy(currentUserProvider.getCurrentUserId());
        requireOne(toolCallMapper.insert(toolCall), "AI ToolCall 写入失败");
        return toolCall;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TAiActionProposal saveProposal(AiProposalCommand command) {
        TAiActionProposal proposal = new TAiActionProposal();
        proposal.setRunId(command.runId());
        proposal.setProposalType(command.proposalType().getCode());
        proposal.setStatus(AiProposalStatus.PENDING_CONFIRMATION.name());
        proposal.setRiskLevel(command.riskLevel().name());
        proposal.setPermissionCode(sanitizer.sanitize(command.permissionCode(), 128));
        proposal.setRelatedObjectType(sanitizer.sanitize(command.relatedObjectType(), 64));
        proposal.setRelatedObjectId(sanitizer.sanitize(command.relatedObjectId(), 64));
        // normalizedParams 是确认执行快照，必须与 paramsHash 指向同一份原始参数。
        proposal.setNormalizedParams(command.normalizedParams());
        proposal.setParamsHash(command.paramsHash());
        proposal.setParamsSummary(sanitizer.sanitize(command.paramsSummary(), 1000));
        proposal.setImpactSummary(sanitizer.sanitize(command.impactSummary(), 1000));
        proposal.setExpiresTime(command.expiresTime());
        proposal.setCreateTime(LocalDateTime.now());
        proposal.setCreateBy(currentUserProvider.getCurrentUserId());
        requireOne(proposalMapper.insert(proposal), "AI Proposal 写入失败");
        updateRunStatus(command.runId(), AiRunStatus.WAITING_FOR_APPROVAL, null, null);
        return proposal;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TAiApproval recordApproval(AiApprovalCommand command) {
        TAiApproval approval = new TAiApproval();
        approval.setRunId(command.runId());
        approval.setProposalId(command.proposalId());
        approval.setDecision(command.decision().name());
        approval.setPermissionSummary(sanitizer.sanitize(command.permissionSummary(), 500));
        approval.setReason(sanitizer.sanitize(command.reason(), 500));
        approval.setResultStatus(command.resultStatus().name());
        approval.setApprovedTime(command.approvedTime() == null ? LocalDateTime.now() : command.approvedTime());
        approval.setApprovedBy(currentUserProvider.getCurrentUserId());
        requireOne(approvalMapper.insert(approval), "AI Approval 写入失败");
        return approval;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TAiExecutionEvent recordExecutionEvent(AiExecutionEventCommand command) {
        TAiExecutionEvent event = new TAiExecutionEvent();
        event.setRunId(command.runId());
        event.setProposalId(command.proposalId());
        event.setEventType(sanitizer.sanitize(command.eventType(), 64));
        event.setResultStatus(command.resultStatus().name());
        event.setObjectType(sanitizer.sanitize(command.objectType(), 64));
        event.setObjectId(sanitizer.sanitize(command.objectId(), 64));
        event.setSummary(sanitizer.sanitize(command.summary(), 1000));
        event.setErrorCode(sanitizer.sanitize(command.errorCode(), 64));
        event.setOccurredTime(command.occurredTime() == null ? LocalDateTime.now() : command.occurredTime());
        event.setCreateBy(currentUserProvider.getCurrentUserId());
        requireOne(executionEventMapper.insert(event), "AI ExecutionEvent 写入失败");
        return event;
    }

    private void requireOne(int rows, String message) {
        if (rows != 1) {
            throw new BusinessException(CodeEnum.OPERATION_FAILED, message);
        }
    }

    private String normalizeContext(String value) {
        return StringUtils.hasText(value) ? sanitizer.sanitize(value, 64) : null;
    }

    private String conversationTitle(String title) {
        if (!StringUtils.hasText(title)) {
            return "新的 AI 会话";
        }
        return sanitizer.sanitize(title, 128);
    }
}
