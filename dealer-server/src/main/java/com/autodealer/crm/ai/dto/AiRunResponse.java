package com.autodealer.crm.ai.dto;

import com.autodealer.crm.ai.model.TAiRun;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AiRunResponse {
    private String runNo;
    private String conversationNo;
    private Integer turnNo;
    private String status;
    private String entryPoint;
    private String contextObjectType;
    private String contextObjectId;
    private String promptSummary;
    private String errorCode;
    private String errorMessage;
    private LocalDateTime createTime;
    private LocalDateTime startedTime;
    private LocalDateTime completedTime;
    private LocalDateTime expiresTime;
    private Boolean contextActive;
    private String invalidationReason;

    public static AiRunResponse from(TAiRun run) {
        AiRunResponse response = new AiRunResponse();
        response.setRunNo(run.getRunNo());
        response.setConversationNo(run.getConversationNo());
        response.setTurnNo(run.getTurnNo());
        response.setStatus(run.getStatus());
        response.setEntryPoint(run.getEntryPoint());
        response.setContextObjectType(run.getContextObjectType());
        response.setContextObjectId(run.getContextObjectId());
        response.setPromptSummary(run.getPromptSummary());
        response.setErrorCode(run.getErrorCode());
        response.setErrorMessage(run.getErrorMessage());
        response.setCreateTime(run.getCreateTime());
        response.setStartedTime(run.getStartedTime());
        response.setCompletedTime(run.getCompletedTime());
        response.setExpiresTime(run.getExpiresTime());
        response.setContextActive(run.getContextActive());
        response.setInvalidationReason(run.getInvalidationReason());
        return response;
    }
}
