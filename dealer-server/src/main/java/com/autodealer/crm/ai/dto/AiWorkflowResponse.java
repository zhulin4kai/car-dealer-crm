package com.autodealer.crm.ai.dto;

import com.autodealer.crm.ai.model.TAiWorkflow;
import com.autodealer.crm.ai.model.TAiWorkflowStep;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class AiWorkflowResponse {
    private String workflowNo;
    private String runNo;
    private String workflowType;
    private String title;
    private String status;
    private Integer currentStepNo;
    private String contextObjectType;
    private String contextObjectId;
    private String pauseReason;
    private String errorCode;
    private String errorMessage;
    private LocalDateTime startedTime;
    private LocalDateTime pausedTime;
    private LocalDateTime resumedTime;
    private LocalDateTime completedTime;
    private LocalDateTime expiresTime;
    private List<StepResponse> steps;

    public static AiWorkflowResponse from(TAiWorkflow workflow, String runNo, List<TAiWorkflowStep> steps) {
        AiWorkflowResponse response = new AiWorkflowResponse();
        response.setWorkflowNo(workflow.getWorkflowNo());
        response.setRunNo(runNo);
        response.setWorkflowType(workflow.getWorkflowType());
        response.setTitle(workflow.getTitle());
        response.setStatus(workflow.getStatus());
        response.setCurrentStepNo(workflow.getCurrentStepNo());
        response.setContextObjectType(workflow.getContextObjectType());
        response.setContextObjectId(workflow.getContextObjectId());
        response.setPauseReason(workflow.getPauseReason());
        response.setErrorCode(workflow.getErrorCode());
        response.setErrorMessage(workflow.getErrorMessage());
        response.setStartedTime(workflow.getStartedTime());
        response.setPausedTime(workflow.getPausedTime());
        response.setResumedTime(workflow.getResumedTime());
        response.setCompletedTime(workflow.getCompletedTime());
        response.setExpiresTime(workflow.getExpiresTime());
        response.setSteps(steps.stream().map(StepResponse::from).toList());
        return response;
    }

    public record StepResponse(Long id,
                               Integer stepNo,
                               String stepType,
                               String title,
                               String status,
                               String toolName,
                               Long proposalId,
                               String inputSummary,
                               String outputSummary,
                               String errorCode,
                               LocalDateTime startedTime,
                               LocalDateTime completedTime) {
        static StepResponse from(TAiWorkflowStep step) {
            return new StepResponse(step.getId(), step.getStepNo(), step.getStepType(),
                    step.getTitle(), step.getStatus(), step.getToolName(), step.getProposalId(),
                    step.getInputSummary(), step.getOutputSummary(), step.getErrorCode(),
                    step.getStartedTime(), step.getCompletedTime());
        }
    }
}
