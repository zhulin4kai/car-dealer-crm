package com.autodealer.crm.modules.ai.application.api.dto;

import com.autodealer.crm.modules.ai.persistence.model.TAiActionProposal;
import com.autodealer.crm.modules.ai.persistence.model.TAiApproval;
import com.autodealer.crm.modules.ai.persistence.model.TAiExecutionEvent;
import com.autodealer.crm.modules.ai.persistence.model.TAiMessage;
import com.autodealer.crm.modules.ai.persistence.model.TAiRun;
import com.autodealer.crm.modules.ai.persistence.model.TAiToolCall;
import com.autodealer.crm.modules.ai.persistence.model.TAiWorkflow;
import com.autodealer.crm.modules.ai.persistence.model.TAiWorkflowStep;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class AiRunTraceResponse {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private AiRunResponse run;
    private List<MessageTrace> messages;
    private List<ToolCallTrace> toolCalls;
    private List<ProposalTrace> proposals;
    private List<ApprovalTrace> approvals;
    private List<ExecutionEventTrace> executionEvents;
    private List<WorkflowTrace> workflows;

    public static AiRunTraceResponse from(TAiRun run,
                                          List<TAiMessage> messages,
                                          List<TAiToolCall> toolCalls,
                                          List<TAiActionProposal> proposals,
                                          List<TAiApproval> approvals,
                                          List<TAiExecutionEvent> executionEvents) {
        return from(run, messages, toolCalls, proposals, approvals, executionEvents, List.of());
    }

    public static AiRunTraceResponse from(TAiRun run,
                                          List<TAiMessage> messages,
                                          List<TAiToolCall> toolCalls,
                                          List<TAiActionProposal> proposals,
                                          List<TAiApproval> approvals,
                                          List<TAiExecutionEvent> executionEvents,
                                          List<WorkflowWithSteps> workflows) {
        AiRunTraceResponse response = new AiRunTraceResponse();
        response.setRun(AiRunResponse.from(run));
        response.setMessages(messages.stream().map(MessageTrace::from).toList());
        response.setToolCalls(toolCalls.stream().map(ToolCallTrace::from).toList());
        response.setProposals(proposals.stream().map(ProposalTrace::from).toList());
        response.setApprovals(approvals.stream().map(ApprovalTrace::from).toList());
        response.setExecutionEvents(executionEvents.stream().map(ExecutionEventTrace::from).toList());
        response.setWorkflows(workflows.stream().map(WorkflowTrace::from).toList());
        return response;
    }

    public record MessageTrace(Long id, String messageNo, String role, Integer sequenceNo,
                               Boolean visibleToUser, String status, Integer revisionNo,
                               Boolean includedInContext, Integer version,
                               String contentSummary, LocalDateTime createTime,
                               LocalDateTime editTime, LocalDateTime withdrawnTime) {
        public static MessageTrace from(TAiMessage message) {
            String content = "WITHDRAWN".equals(message.getStatus()) ? null : message.getContentSummary();
            return new MessageTrace(message.getId(), message.getMessageNo(), message.getRole(),
                    message.getSequenceNo(), message.getVisibleToUser(), message.getStatus(),
                    message.getRevisionNo(), message.getIncludedInContext(), message.getVersion(),
                    content, message.getCreateTime(), message.getEditTime(), message.getWithdrawnTime());
        }
    }

    public record ToolCallTrace(Long id, String toolName, String permissionCode, String riskLevel,
                                String inputSummary, String outputSummary, String objectRefs,
                                Object displayPayload, String resultStatus, String errorCode, Integer durationMs,
                                LocalDateTime startedTime, LocalDateTime completedTime) {
        static ToolCallTrace from(TAiToolCall toolCall) {
            return new ToolCallTrace(toolCall.getId(), toolCall.getToolName(), toolCall.getPermissionCode(),
                    toolCall.getRiskLevel(), toolCall.getInputSummary(), toolCall.getOutputSummary(),
                    toolCall.getObjectRefs(), parseDisplayPayload(toolCall.getDisplayPayloadJson()),
                    toolCall.getResultStatus(), toolCall.getErrorCode(),
                    toolCall.getDurationMs(), toolCall.getStartedTime(), toolCall.getCompletedTime());
        }
    }

    private static Object parseDisplayPayload(String displayPayloadJson) {
        if (displayPayloadJson == null || displayPayloadJson.isBlank()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(displayPayloadJson, Object.class);
        } catch (JsonProcessingException ex) {
            return null;
        }
    }

    public record ProposalTrace(Long id, String proposalType, String status, String riskLevel,
                                String permissionCode, String relatedObjectType, String relatedObjectId,
                                String paramsSummary, String impactSummary, LocalDateTime expiresTime,
                                LocalDateTime confirmedTime, LocalDateTime executedTime,
                                String resultSummary, String errorCode, LocalDateTime createTime) {
        static ProposalTrace from(TAiActionProposal proposal) {
            return new ProposalTrace(proposal.getId(), proposal.getProposalType(), proposal.getStatus(),
                    proposal.getRiskLevel(), proposal.getPermissionCode(), proposal.getRelatedObjectType(),
                    proposal.getRelatedObjectId(), proposal.getParamsSummary(), proposal.getImpactSummary(),
                    proposal.getExpiresTime(), proposal.getConfirmedTime(), proposal.getExecutedTime(),
                    proposal.getResultSummary(), proposal.getErrorCode(), proposal.getCreateTime());
        }
    }

    public record ApprovalTrace(Long id, Long proposalId, String decision, String permissionSummary,
                                String reason, String resultStatus, LocalDateTime approvedTime) {
        static ApprovalTrace from(TAiApproval approval) {
            return new ApprovalTrace(approval.getId(), approval.getProposalId(), approval.getDecision(),
                    approval.getPermissionSummary(), approval.getReason(), approval.getResultStatus(),
                    approval.getApprovedTime());
        }
    }

    public record ExecutionEventTrace(Long id, Long proposalId, String eventType, String resultStatus,
                                      String objectType, String objectId, String summary,
                                      String errorCode, LocalDateTime occurredTime) {
        static ExecutionEventTrace from(TAiExecutionEvent event) {
            return new ExecutionEventTrace(event.getId(), event.getProposalId(), event.getEventType(),
                    event.getResultStatus(), event.getObjectType(), event.getObjectId(), event.getSummary(),
                    event.getErrorCode(), event.getOccurredTime());
        }
    }

    public record WorkflowWithSteps(TAiWorkflow workflow, List<TAiWorkflowStep> steps) {
    }

    public record WorkflowTrace(Long id, String workflowNo, String workflowType, String title,
                                String status, Integer currentStepNo, String contextObjectType,
                                String contextObjectId, String pauseReason, String errorCode,
                                String errorMessage, LocalDateTime startedTime, LocalDateTime pausedTime,
                                LocalDateTime resumedTime, LocalDateTime completedTime,
                                LocalDateTime expiresTime, List<WorkflowStepTrace> steps) {
        static WorkflowTrace from(WorkflowWithSteps value) {
            TAiWorkflow workflow = value.workflow();
            return new WorkflowTrace(workflow.getId(), workflow.getWorkflowNo(),
                    workflow.getWorkflowType(), workflow.getTitle(), workflow.getStatus(),
                    workflow.getCurrentStepNo(), workflow.getContextObjectType(),
                    workflow.getContextObjectId(), workflow.getPauseReason(), workflow.getErrorCode(),
                    workflow.getErrorMessage(), workflow.getStartedTime(), workflow.getPausedTime(),
                    workflow.getResumedTime(), workflow.getCompletedTime(), workflow.getExpiresTime(),
                    value.steps().stream().map(WorkflowStepTrace::from).toList());
        }
    }

    public record WorkflowStepTrace(Long id, Integer stepNo, String stepType, String title,
                                    String status, String toolName, Long proposalId,
                                    String inputSummary, String outputSummary, String errorCode,
                                    LocalDateTime startedTime, LocalDateTime completedTime) {
        static WorkflowStepTrace from(TAiWorkflowStep step) {
            return new WorkflowStepTrace(step.getId(), step.getStepNo(), step.getStepType(),
                    step.getTitle(), step.getStatus(), step.getToolName(), step.getProposalId(),
                    step.getInputSummary(), step.getOutputSummary(), step.getErrorCode(),
                    step.getStartedTime(), step.getCompletedTime());
        }
    }
}
