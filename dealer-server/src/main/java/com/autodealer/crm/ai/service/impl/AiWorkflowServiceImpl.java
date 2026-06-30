package com.autodealer.crm.ai.service.impl;

import com.autodealer.crm.ai.dto.AiExecutionEventCommand;
import com.autodealer.crm.ai.dto.AiWorkflowActionRequest;
import com.autodealer.crm.ai.dto.AiWorkflowResponse;
import com.autodealer.crm.ai.dto.CreateAiWorkflowRequest;
import com.autodealer.crm.ai.enums.AiResultStatus;
import com.autodealer.crm.ai.enums.AiWorkflowStatus;
import com.autodealer.crm.ai.enums.AiWorkflowStepStatus;
import com.autodealer.crm.ai.enums.AiWorkflowType;
import com.autodealer.crm.ai.mapper.TAiRunMapper;
import com.autodealer.crm.ai.mapper.TAiWorkflowMapper;
import com.autodealer.crm.ai.mapper.TAiWorkflowStepMapper;
import com.autodealer.crm.ai.model.TAiRun;
import com.autodealer.crm.ai.model.TAiWorkflow;
import com.autodealer.crm.ai.model.TAiWorkflowStep;
import com.autodealer.crm.ai.service.AiSensitiveDataSanitizer;
import com.autodealer.crm.ai.service.AiTraceService;
import com.autodealer.crm.ai.service.AiWorkflowService;
import com.autodealer.crm.config.security.CurrentUserProvider;
import com.autodealer.crm.exception.BusinessException;
import com.autodealer.crm.result.CodeEnum;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class AiWorkflowServiceImpl implements AiWorkflowService {
    private static final Set<AiWorkflowStatus> TERMINAL_STATUSES = Set.of(
            AiWorkflowStatus.COMPLETED,
            AiWorkflowStatus.FAILED,
            AiWorkflowStatus.CANCELLED,
            AiWorkflowStatus.EXPIRED);

    private final TAiWorkflowMapper workflowMapper;
    private final TAiWorkflowStepMapper stepMapper;
    private final TAiRunMapper runMapper;
    private final AiTraceService traceService;
    private final CurrentUserProvider currentUserProvider;
    private final AiSensitiveDataSanitizer sanitizer;

    public AiWorkflowServiceImpl(TAiWorkflowMapper workflowMapper,
                                 TAiWorkflowStepMapper stepMapper,
                                 TAiRunMapper runMapper,
                                 AiTraceService traceService,
                                 CurrentUserProvider currentUserProvider,
                                 AiSensitiveDataSanitizer sanitizer) {
        this.workflowMapper = workflowMapper;
        this.stepMapper = stepMapper;
        this.runMapper = runMapper;
        this.traceService = traceService;
        this.currentUserProvider = currentUserProvider;
        this.sanitizer = sanitizer;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiWorkflowResponse create(CreateAiWorkflowRequest request) {
        TAiRun run = traceService.getOwnedRun(request.getRunNo());
        AiWorkflowType workflowType = parseType(request.getWorkflowType());
        LocalDateTime now = LocalDateTime.now();
        TAiWorkflow workflow = new TAiWorkflow();
        workflow.setWorkflowNo("AIW" + UUID.randomUUID().toString().replace("-", ""));
        workflow.setRunId(run.getId());
        workflow.setUserId(currentUserProvider.getCurrentUserId());
        workflow.setWorkflowType(workflowType.name());
        workflow.setTitle(title(workflowType));
        workflow.setStatus(initialStatus(workflowType).name());
        workflow.setCurrentStepNo(1);
        workflow.setContextObjectType(firstText(request.getContextObjectType(), run.getContextObjectType()));
        workflow.setContextObjectId(firstText(request.getContextObjectId(), run.getContextObjectId()));
        workflow.setStartedTime(now);
        workflow.setExpiresTime(now.plusHours(1));
        workflow.setCreateTime(now);
        workflow.setCreateBy(currentUserProvider.getCurrentUserId());
        requireOne(workflowMapper.insert(workflow), "AI 工作流写入失败");
        for (TAiWorkflowStep step : stepsFor(workflow, workflowType, now)) {
            requireOne(stepMapper.insert(step), "AI 工作流步骤写入失败");
        }
        recordWorkflowEvent(workflow, "AI_WORKFLOW_STARTED", AiResultStatus.SUCCESS,
                "AI 工作流已启动");
        return toResponse(workflow, run.getRunNo());
    }

    @Override
    public AiWorkflowResponse get(String workflowNo) {
        TAiWorkflow workflow = requireOwned(workflowNo);
        return toResponse(workflow, runNo(workflow.getRunId()));
    }

    @Override
    public List<AiWorkflowResponse> listByRun(String runNo) {
        TAiRun run = traceService.getOwnedRun(runNo);
        return workflowMapper.selectByRunId(run.getId()).stream()
                .map(workflow -> toResponse(workflow, runNo))
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiWorkflowResponse pause(String workflowNo, AiWorkflowActionRequest request) {
        TAiWorkflow workflow = requireOwned(workflowNo);
        requireNotTerminal(workflow);
        updateStatus(workflow, AiWorkflowStatus.PAUSED,
                request == null ? null : request.getReason(), null, null);
        recordWorkflowEvent(workflow, "AI_WORKFLOW_PAUSED", AiResultStatus.SUCCESS,
                "AI 工作流已暂停");
        return get(workflowNo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiWorkflowResponse resume(String workflowNo) {
        TAiWorkflow workflow = requireOwned(workflowNo);
        requireStatus(workflow, AiWorkflowStatus.PAUSED);
        updateStatus(workflow, AiWorkflowStatus.RUNNING, null, null, null);
        recordWorkflowEvent(workflow, "AI_WORKFLOW_RESUMED", AiResultStatus.SUCCESS,
                "AI 工作流已恢复");
        return get(workflowNo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiWorkflowResponse cancel(String workflowNo, AiWorkflowActionRequest request) {
        TAiWorkflow workflow = requireOwned(workflowNo);
        requireNotTerminal(workflow);
        updateStatus(workflow, AiWorkflowStatus.CANCELLED,
                request == null ? null : request.getReason(), null, null);
        stepMapper.updateUnfinishedByWorkflowId(workflow.getId(), AiWorkflowStepStatus.CANCELLED.name(),
                null, currentUserProvider.getCurrentUserId());
        recordWorkflowEvent(workflow, "AI_WORKFLOW_CANCELLED", AiResultStatus.SUCCESS,
                "AI 工作流已取消");
        return get(workflowNo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiWorkflowResponse complete(String workflowNo) {
        TAiWorkflow workflow = requireOwned(workflowNo);
        requireNotTerminal(workflow);
        updateStatus(workflow, AiWorkflowStatus.COMPLETED, null, null, null);
        stepMapper.updateUnfinishedByWorkflowId(workflow.getId(), AiWorkflowStepStatus.COMPLETED.name(),
                null, currentUserProvider.getCurrentUserId());
        recordWorkflowEvent(workflow, "AI_WORKFLOW_COMPLETED", AiResultStatus.SUCCESS,
                "AI 工作流已完成");
        return get(workflowNo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiWorkflowResponse fail(String workflowNo, AiWorkflowActionRequest request) {
        TAiWorkflow workflow = requireOwned(workflowNo);
        requireNotTerminal(workflow);
        updateStatus(workflow, AiWorkflowStatus.FAILED,
                request == null ? null : request.getReason(),
                CodeEnum.AI_WORKFLOW_STATE_CONFLICT.name(),
                request == null ? "AI 工作流执行失败" : request.getReason());
        stepMapper.updateUnfinishedByWorkflowId(workflow.getId(), AiWorkflowStepStatus.FAILED.name(),
                CodeEnum.AI_WORKFLOW_STATE_CONFLICT.name(), currentUserProvider.getCurrentUserId());
        recordWorkflowEvent(workflow, "AI_WORKFLOW_FAILED", AiResultStatus.FAILED,
                request == null ? "AI 工作流执行失败" : request.getReason());
        return get(workflowNo);
    }

    private AiWorkflowType parseType(String value) {
        try {
            return AiWorkflowType.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (RuntimeException ex) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "AI 工作流类型不支持", ex);
        }
    }

    private AiWorkflowStatus initialStatus(AiWorkflowType type) {
        return type == AiWorkflowType.CUSTOMER_FOLLOW_UP
                ? AiWorkflowStatus.WAITING_USER_CONFIRMATION
                : AiWorkflowStatus.RUNNING;
    }

    private String title(AiWorkflowType type) {
        return switch (type) {
            case CUSTOMER_FOLLOW_UP -> "客户跟进辅助工作流";
            case TRANSACTION_GAP_REVIEW -> "交易履约缺口工作流";
            case INVENTORY_RISK_REVIEW -> "库存风险解释工作流";
        };
    }

    private List<TAiWorkflowStep> stepsFor(TAiWorkflow workflow, AiWorkflowType type, LocalDateTime now) {
        return switch (type) {
            case CUSTOMER_FOLLOW_UP -> List.of(
                    step(workflow, 1, "READ_CUSTOMER", "查询客户档案", AiWorkflowStepStatus.COMPLETED,
                            "get_customer_profile", "已读取客户上下文", now),
                    step(workflow, 2, "CREATE_COMMUNICATION_PROPOSAL", "等待确认沟通记录提议",
                            AiWorkflowStepStatus.WAITING_USER_CONFIRMATION,
                            "create_communication_record_proposal", "等待用户确认", now),
                    step(workflow, 3, "CREATE_FOLLOW_TASK_PROPOSAL", "准备跟进任务提议",
                            AiWorkflowStepStatus.PENDING,
                            "create_follow_task_proposal", null, now));
            case TRANSACTION_GAP_REVIEW -> List.of(
                    step(workflow, 1, "READ_TRANSACTION", "查询交易详情", AiWorkflowStepStatus.COMPLETED,
                            "get_transaction_detail", "已读取交易详情", now),
                    step(workflow, 2, "EXPLAIN_GAP", "解释履约缺口", AiWorkflowStepStatus.RUNNING,
                            null, null, now));
            case INVENTORY_RISK_REVIEW -> List.of(
                    step(workflow, 1, "READ_INVENTORY", "查询库存预警", AiWorkflowStepStatus.COMPLETED,
                            "get_inventory_alerts", "已读取库存预警", now),
                    step(workflow, 2, "EXPLAIN_RISK", "解释库存风险", AiWorkflowStepStatus.RUNNING,
                            null, null, now));
        };
    }

    private TAiWorkflowStep step(TAiWorkflow workflow,
                                 int stepNo,
                                 String stepType,
                                 String title,
                                 AiWorkflowStepStatus status,
                                 String toolName,
                                 String outputSummary,
                                 LocalDateTime now) {
        TAiWorkflowStep step = new TAiWorkflowStep();
        step.setWorkflowId(workflow.getId());
        step.setStepNo(stepNo);
        step.setStepType(stepType);
        step.setTitle(title);
        step.setStatus(status.name());
        step.setToolName(toolName);
        step.setOutputSummary(outputSummary);
        step.setStartedTime(now);
        if (status == AiWorkflowStepStatus.COMPLETED) {
            step.setCompletedTime(now);
        }
        step.setCreateTime(now);
        step.setCreateBy(currentUserProvider.getCurrentUserId());
        return step;
    }

    private TAiWorkflow requireOwned(String workflowNo) {
        TAiWorkflow workflow = workflowMapper.selectOwnedByWorkflowNo(
                workflowNo, currentUserProvider.getCurrentUserId());
        if (workflow == null) {
            throw new BusinessException(CodeEnum.AI_WORKFLOW_NOT_FOUND, "AI 工作流不存在或无权访问");
        }
        return workflow;
    }

    private void requireStatus(TAiWorkflow workflow, AiWorkflowStatus expectedStatus) {
        if (!expectedStatus.name().equals(workflow.getStatus())) {
            throw new BusinessException(CodeEnum.AI_WORKFLOW_STATE_CONFLICT, "AI 工作流状态冲突");
        }
    }

    private void requireNotTerminal(TAiWorkflow workflow) {
        if (TERMINAL_STATUSES.contains(AiWorkflowStatus.valueOf(workflow.getStatus()))) {
            throw new BusinessException(CodeEnum.AI_WORKFLOW_STATE_CONFLICT, "AI 工作流已结束");
        }
    }

    private void updateStatus(TAiWorkflow workflow,
                              AiWorkflowStatus status,
                              String pauseReason,
                              String errorCode,
                              String errorMessage) {
        requireOne(workflowMapper.updateStatusIfCurrent(
                        workflow.getId(),
                        workflow.getStatus(),
                        status.name(),
                        sanitizer.sanitize(pauseReason, 500),
                        sanitizer.sanitize(errorCode, 64),
                        sanitizer.sanitize(errorMessage, 255),
                        currentUserProvider.getCurrentUserId()),
                "AI 工作流状态更新失败");
    }

    private void recordWorkflowEvent(TAiWorkflow workflow,
                                     String eventType,
                                     AiResultStatus resultStatus,
                                     String summary) {
        traceService.recordExecutionEvent(new AiExecutionEventCommand(
                workflow.getRunId(),
                null,
                eventType,
                resultStatus,
                "AI_WORKFLOW",
                workflow.getWorkflowNo(),
                summary,
                null,
                LocalDateTime.now()));
    }

    private AiWorkflowResponse toResponse(TAiWorkflow workflow, String runNo) {
        return AiWorkflowResponse.from(workflow, runNo, stepMapper.selectByWorkflowId(workflow.getId()));
    }

    private String runNo(Long runId) {
        TAiRun run = runMapper.selectById(runId);
        return run == null ? null : run.getRunNo();
    }

    private String firstText(String preferred, String fallback) {
        return StringUtils.hasText(preferred) ? preferred : fallback;
    }

    private void requireOne(int rows, String message) {
        if (rows != 1) {
            throw new BusinessException(CodeEnum.OPERATION_FAILED, message);
        }
    }
}
