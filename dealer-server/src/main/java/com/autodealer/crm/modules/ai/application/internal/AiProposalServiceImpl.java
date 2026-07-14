package com.autodealer.crm.modules.ai.application.internal;

import com.autodealer.crm.modules.ai.application.api.tool.ToolExecutionContext;
import com.autodealer.crm.modules.ai.application.api.dto.AiApprovalCommand;
import com.autodealer.crm.modules.ai.application.api.dto.AiExecutionEventCommand;
import com.autodealer.crm.modules.ai.application.api.dto.AiProposalCommand;
import com.autodealer.crm.modules.ai.application.api.dto.AiProposalConfirmResponse;
import com.autodealer.crm.modules.ai.application.api.dto.tool.AiToolDtos;
import com.autodealer.crm.modules.ai.application.api.enums.AiApprovalDecision;
import com.autodealer.crm.modules.ai.application.api.enums.AiProposalStatus;
import com.autodealer.crm.modules.ai.application.api.enums.AiProposalType;
import com.autodealer.crm.modules.ai.application.api.enums.AiResultStatus;
import com.autodealer.crm.modules.ai.application.api.enums.AiRiskLevel;
import com.autodealer.crm.modules.ai.persistence.mapper.TAiActionProposalMapper;
import com.autodealer.crm.modules.ai.persistence.model.TAiActionProposal;
import com.autodealer.crm.modules.ai.application.api.AiProposalService;
import com.autodealer.crm.modules.ai.application.internal.AiSensitiveDataSanitizer;
import com.autodealer.crm.modules.ai.application.api.AiTraceService;
import com.autodealer.crm.modules.identity.application.api.security.CurrentUserProvider;
import com.autodealer.crm.shared.security.PermissionCodes;
import com.autodealer.crm.modules.sales.followup.application.api.dto.CreateCommunicationRecordRequest;
import com.autodealer.crm.modules.sales.followup.application.api.dto.CreateFollowTaskRequest;
import com.autodealer.crm.modules.sales.followup.application.api.enums.CommunicationMethod;
import com.autodealer.crm.modules.sales.followup.application.api.enums.FollowTaskPriority;
import com.autodealer.crm.modules.sales.followup.application.api.enums.FollowTaskType;
import com.autodealer.crm.shared.error.BusinessException;
import com.autodealer.crm.modules.sales.followup.application.api.model.TCommunicationRecord;
import com.autodealer.crm.modules.sales.followup.application.api.model.TFollowTask;
import com.autodealer.crm.shared.error.CodeEnum;
import com.autodealer.crm.modules.sales.followup.application.api.CommunicationRecordService;
import com.autodealer.crm.modules.sales.followup.application.api.FollowTaskService;
import com.autodealer.crm.modules.sales.followup.application.api.FollowRelatedObjectAccess;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;

@Service
public class AiProposalServiceImpl implements AiProposalService {
    private static final int PROPOSAL_TTL_MINUTES = 30;

    private final TAiActionProposalMapper proposalMapper;
    private final AiTraceService traceService;
    private final CommunicationRecordService communicationRecordService;
    private final FollowTaskService followTaskService;
    private final FollowRelatedObjectAccess relatedObjectResolver;
    private final CurrentUserProvider currentUserProvider;
    private final AiSensitiveDataSanitizer sanitizer;
    private final ObjectMapper objectMapper;
    private final AiProposalFailureRecorder failureRecorder;

    public AiProposalServiceImpl(TAiActionProposalMapper proposalMapper,
                                 AiTraceService traceService,
                                 CommunicationRecordService communicationRecordService,
                                 FollowTaskService followTaskService,
                                 FollowRelatedObjectAccess relatedObjectResolver,
                                 CurrentUserProvider currentUserProvider,
                                 AiSensitiveDataSanitizer sanitizer,
                                 ObjectMapper objectMapper,
                                 AiProposalFailureRecorder failureRecorder) {
        this.proposalMapper = proposalMapper;
        this.traceService = traceService;
        this.communicationRecordService = communicationRecordService;
        this.followTaskService = followTaskService;
        this.relatedObjectResolver = relatedObjectResolver;
        this.currentUserProvider = currentUserProvider;
        this.sanitizer = sanitizer;
        this.objectMapper = objectMapper;
        this.failureRecorder = failureRecorder;
    }

    @Override
    public AiToolDtos.ProposalCreated createCommunicationRecordProposal(
            ToolExecutionContext context,
            AiToolDtos.CreateCommunicationRecordProposalRequest request) {
        CreateCommunicationRecordRequest businessRequest =
                objectMapper.convertValue(request, CreateCommunicationRecordRequest.class);
        validateCommunicationRecordProposal(businessRequest);
        relatedObjectResolver.requireAccessible(businessRequest.getRelatedObjectType(), businessRequest.getRelatedObjectId());
        String normalized = toJson(businessRequest);
        TAiActionProposal proposal = traceService.saveProposal(new AiProposalCommand(
                context.runId(),
                AiProposalType.CREATE_COMMUNICATION_RECORD,
                AiRiskLevel.LOW,
                PermissionCodes.COMMUNICATION_RECORD_CREATE,
                businessRequest.getRelatedObjectType(),
                String.valueOf(businessRequest.getRelatedObjectId()),
                normalized,
                sha256(normalized),
                "创建沟通记录：" + sanitizer.sanitize(businessRequest.getSummary(), 200),
                "确认后将为关联对象创建一条沟通记录。",
                LocalDateTime.now().plusMinutes(PROPOSAL_TTL_MINUTES)));
        return toCreated(proposal);
    }

    @Override
    public AiToolDtos.ProposalCreated createFollowTaskProposal(
            ToolExecutionContext context,
            AiToolDtos.CreateFollowTaskProposalRequest request) {
        CreateFollowTaskRequest businessRequest = objectMapper.convertValue(request, CreateFollowTaskRequest.class);
        if (businessRequest.getOwnerId() == null) {
            businessRequest.setOwnerId(currentUserProvider.getCurrentUserId());
        }
        validateFollowTaskProposal(businessRequest);
        relatedObjectResolver.requireAccessible(businessRequest.getRelatedObjectType(), businessRequest.getRelatedObjectId());
        relatedObjectResolver.validateAssignableOwner(businessRequest.getOwnerId());
        String normalized = toJson(businessRequest);
        TAiActionProposal proposal = traceService.saveProposal(new AiProposalCommand(
                context.runId(),
                AiProposalType.CREATE_FOLLOW_TASK,
                AiRiskLevel.LOW,
                PermissionCodes.FOLLOW_TASK_CREATE,
                businessRequest.getRelatedObjectType(),
                String.valueOf(businessRequest.getRelatedObjectId()),
                normalized,
                sha256(normalized),
                "创建跟进任务：" + sanitizer.sanitize(businessRequest.getTitle(), 200),
                "确认后将创建一条跟进任务，并按保存的负责人和计划时间进入跟进队列。",
                LocalDateTime.now().plusMinutes(PROPOSAL_TTL_MINUTES)));
        return toCreated(proposal);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiProposalConfirmResponse confirm(Long proposalId) {
        TAiActionProposal proposal = requireOwnedProposal(proposalId);
        if (AiProposalStatus.EXECUTED.name().equals(proposal.getStatus())) {
            return toConfirmResponse(proposal, proposal.getRelatedObjectType(), proposal.getRelatedObjectId());
        }
        requirePendingAndValid(proposal);
        requirePermission(proposal.getPermissionCode());
        requireHashValid(proposal);
        updateProposalStatusIfCurrent(proposal.getId(), AiProposalStatus.PENDING_CONFIRMATION,
                AiProposalStatus.CONFIRMED, null, "用户已确认");
        traceService.recordApproval(new AiApprovalCommand(
                proposal.getRunId(), proposal.getId(), AiApprovalDecision.CONFIRMED,
                proposal.getPermissionCode(), null, AiResultStatus.SUCCESS, LocalDateTime.now()));
        try {
            AiProposalConfirmResponse response = executeSavedProposal(proposal);
            updateProposalStatusIfCurrent(proposal.getId(), AiProposalStatus.CONFIRMED,
                    AiProposalStatus.EXECUTED, null, response.getResultSummary());
            traceService.recordExecutionEvent(new AiExecutionEventCommand(
                    proposal.getRunId(), proposal.getId(), "PROPOSAL_EXECUTED",
                    AiResultStatus.SUCCESS, response.getObjectType(), response.getObjectId(),
                    response.getResultSummary(), null, LocalDateTime.now()));
            response.setStatus(AiProposalStatus.EXECUTED.name());
            return response;
        } catch (BusinessException ex) {
            recordFailureAfterRollback(proposal, ex.getCodeEnum().name(), ex.getMessage());
            updateProposalStatusIfCurrent(proposal.getId(), AiProposalStatus.CONFIRMED,
                    AiProposalStatus.FAILED, ex.getCodeEnum().name(), ex.getMessage());
            traceService.recordExecutionEvent(new AiExecutionEventCommand(
                    proposal.getRunId(), proposal.getId(), "PROPOSAL_EXECUTE_FAILED",
                    AiResultStatus.FAILED, proposal.getRelatedObjectType(), proposal.getRelatedObjectId(),
                    "Proposal 执行失败", ex.getCodeEnum().name(), LocalDateTime.now()));
            throw ex;
        }
    }

    private void recordFailureAfterRollback(TAiActionProposal proposal, String errorCode, String errorMessage) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == TransactionSynchronization.STATUS_ROLLED_BACK) {
                    // 业务写入与 CONFIRMED 状态一起回滚后，再独立保存失败终态和审计事件。
                    failureRecorder.recordAfterRollback(proposal, errorCode, errorMessage);
                }
            }
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiProposalConfirmResponse reject(Long proposalId) {
        TAiActionProposal proposal = requireOwnedProposal(proposalId);
        if (AiProposalStatus.REJECTED.name().equals(proposal.getStatus())) {
            return toConfirmResponse(proposal, proposal.getRelatedObjectType(), proposal.getRelatedObjectId());
        }
        requirePendingAndValid(proposal);
        updateProposalStatusIfCurrent(proposal.getId(), AiProposalStatus.PENDING_CONFIRMATION,
                AiProposalStatus.REJECTED, null, "用户已拒绝");
        traceService.recordApproval(new AiApprovalCommand(
                proposal.getRunId(), proposal.getId(), AiApprovalDecision.REJECTED,
                proposal.getPermissionCode(), null, AiResultStatus.SUCCESS, LocalDateTime.now()));
        traceService.recordExecutionEvent(new AiExecutionEventCommand(
                proposal.getRunId(), proposal.getId(), "PROPOSAL_REJECTED",
                AiResultStatus.SKIPPED, proposal.getRelatedObjectType(), proposal.getRelatedObjectId(),
                "用户已拒绝 Proposal", null, LocalDateTime.now()));
        AiProposalConfirmResponse response = toConfirmResponse(proposal, proposal.getRelatedObjectType(), proposal.getRelatedObjectId());
        response.setStatus(AiProposalStatus.REJECTED.name());
        response.setResultSummary("用户已拒绝");
        return response;
    }

    private AiProposalConfirmResponse executeSavedProposal(TAiActionProposal proposal) {
        if (AiProposalType.CREATE_COMMUNICATION_RECORD.getCode().equals(proposal.getProposalType())) {
            CreateCommunicationRecordRequest request = fromJson(
                    proposal.getNormalizedParams(), CreateCommunicationRecordRequest.class);
            relatedObjectResolver.requireAccessible(request.getRelatedObjectType(), request.getRelatedObjectId());
            TCommunicationRecord record = communicationRecordService.createCommunicationRecord(request);
            return toConfirmResponse(proposal, "COMMUNICATION_RECORD", String.valueOf(record.getId()),
                    "已创建沟通记录");
        }
        if (AiProposalType.CREATE_FOLLOW_TASK.getCode().equals(proposal.getProposalType())) {
            CreateFollowTaskRequest request = fromJson(proposal.getNormalizedParams(), CreateFollowTaskRequest.class);
            if (request.getOwnerId() == null) {
                request.setOwnerId(currentUserProvider.getCurrentUserId());
            }
            relatedObjectResolver.requireAccessible(request.getRelatedObjectType(), request.getRelatedObjectId());
            relatedObjectResolver.validateAssignableOwner(request.getOwnerId());
            TFollowTask task = followTaskService.createFollowTask(request);
            return toConfirmResponse(proposal, "FOLLOW_TASK", String.valueOf(task.getId()), "已创建跟进任务");
        }
        throw new BusinessException(CodeEnum.AI_TOOL_ARGUMENT_INVALID, "AI 提议类型不支持");
    }

    private TAiActionProposal requireOwnedProposal(Long proposalId) {
        if (proposalId == null) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "AI 提议 ID 不能为空");
        }
        TAiActionProposal proposal = proposalMapper.selectById(proposalId);
        if (proposal == null) {
            throw new BusinessException(CodeEnum.AI_RUN_NOT_FOUND, "AI 提议不存在");
        }
        Integer currentUserId = currentUserProvider.getCurrentUserId();
        if (!currentUserId.equals(proposal.getCreateBy())) {
            throw new BusinessException(CodeEnum.ACCESS_DENIED, "无权访问 AI 提议");
        }
        return proposal;
    }

    private void requirePendingAndValid(TAiActionProposal proposal) {
        if (!AiProposalStatus.PENDING_CONFIRMATION.name().equals(proposal.getStatus())) {
            throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "AI 提议状态已变化");
        }
        if (proposal.getExpiresTime() != null && proposal.getExpiresTime().isBefore(LocalDateTime.now())) {
            updateProposalStatusIfCurrent(proposal.getId(), AiProposalStatus.PENDING_CONFIRMATION,
                    AiProposalStatus.EXPIRED, CodeEnum.AI_PROPOSAL_EXPIRED.name(), "AI 提议已过期");
            traceService.recordApproval(new AiApprovalCommand(
                    proposal.getRunId(), proposal.getId(), AiApprovalDecision.EXPIRED,
                    proposal.getPermissionCode(), "AI 提议已过期", AiResultStatus.IGNORED, LocalDateTime.now()));
            traceService.recordExecutionEvent(new AiExecutionEventCommand(
                    proposal.getRunId(), proposal.getId(), "PROPOSAL_EXPIRED",
                    AiResultStatus.SKIPPED, proposal.getRelatedObjectType(), proposal.getRelatedObjectId(),
                    "AI 提议已过期，未执行业务写入", CodeEnum.AI_PROPOSAL_EXPIRED.name(), LocalDateTime.now()));
            throw new BusinessException(CodeEnum.AI_PROPOSAL_EXPIRED, "AI 提议已过期");
        }
    }

    private void requirePermission(String permissionCode) {
        if (!currentUserProvider.hasAuthority(permissionCode)) {
            throw new BusinessException(CodeEnum.AI_TOOL_FORBIDDEN, "AI 提议执行权限已变化");
        }
    }

    private void requireHashValid(TAiActionProposal proposal) {
        String currentHash = sha256(proposal.getNormalizedParams());
        if (!currentHash.equals(proposal.getParamsHash())) {
            throw new BusinessException(CodeEnum.AI_PROPOSAL_HASH_MISMATCH, "AI 提议参数校验失败");
        }
    }

    private void updateProposalStatusIfCurrent(Long id,
                                               AiProposalStatus expectedStatus,
                                               AiProposalStatus status,
                                               String errorCode,
                                               String resultSummary) {
        int rows = proposalMapper.updateStatusIfCurrent(id, expectedStatus.name(), status.name(),
                sanitizer.sanitize(errorCode, 64), sanitizer.sanitize(resultSummary, 1000));
        if (rows != 1) {
            throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "AI 提议状态已变化");
        }
    }

    private AiToolDtos.ProposalCreated toCreated(TAiActionProposal proposal) {
        return new AiToolDtos.ProposalCreated(
                proposal.getId(), proposal.getProposalType(), proposal.getRiskLevel(),
                proposal.getPermissionCode(), proposal.getRelatedObjectType(),
                proposal.getRelatedObjectId(), proposal.getParamsSummary(),
                proposal.getImpactSummary(),
                proposal.getExpiresTime());
    }

    private void validateCommunicationRecordProposal(CreateCommunicationRecordRequest request) {
        try {
            CommunicationMethod.parse(request.getCommunicationMethod());
            if (Boolean.TRUE.equals(request.getCreateNextTask())) {
                if (StringUtils.hasText(request.getNextTaskType())) {
                    FollowTaskType.parse(request.getNextTaskType());
                }
                FollowTaskPriority.parseOrDefault(request.getNextTaskPriority());
            }
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(CodeEnum.AI_TOOL_ARGUMENT_INVALID, "AI 提议参数校验失败", ex);
        }
    }

    private void validateFollowTaskProposal(CreateFollowTaskRequest request) {
        try {
            FollowTaskType.parse(request.getTaskType());
            FollowTaskPriority.parseOrDefault(request.getPriority());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(CodeEnum.AI_TOOL_ARGUMENT_INVALID, "AI 提议参数校验失败", ex);
        }
    }

    private AiProposalConfirmResponse toConfirmResponse(TAiActionProposal proposal,
                                                        String objectType,
                                                        String objectId) {
        return toConfirmResponse(proposal, objectType, objectId, proposal.getResultSummary());
    }

    private AiProposalConfirmResponse toConfirmResponse(TAiActionProposal proposal,
                                                        String objectType,
                                                        String objectId,
                                                        String resultSummary) {
        AiProposalConfirmResponse response = new AiProposalConfirmResponse();
        response.setProposalId(proposal.getId());
        response.setStatus(proposal.getStatus());
        response.setResultSummary(resultSummary);
        response.setObjectType(objectType);
        response.setObjectId(objectId);
        return response;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new BusinessException(CodeEnum.AI_TOOL_ARGUMENT_INVALID, "AI 提议参数序列化失败", ex);
        }
    }

    private <T> T fromJson(String value, Class<T> targetType) {
        try {
            return objectMapper.readValue(value, targetType);
        } catch (JsonProcessingException ex) {
            throw new BusinessException(CodeEnum.AI_TOOL_ARGUMENT_INVALID, "AI 提议参数解析失败", ex);
        }
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException ex) {
            throw new BusinessException(CodeEnum.SYSTEM_ERROR, "AI 提议参数校验失败", ex);
        }
    }
}
