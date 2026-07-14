package com.autodealer.crm.modules.ai.application.api.dto;

import lombok.Data;

import java.util.List;

@Data
public class AiConversationTurnResponse {
    private AiRunResponse run;
    private Integer turnNo;
    private String status;
    private AiRunTraceResponse.MessageTrace userMessage;
    private AiRunTraceResponse.MessageTrace assistantMessage;
    private List<AiRunTraceResponse.MessageTrace> messages;
    private List<AiRunTraceResponse.ToolCallTrace> toolResults;
    private List<AiRunTraceResponse.ProposalTrace> proposals;
    private List<AiRunTraceResponse.ApprovalTrace> approvals;
    private List<AiRunTraceResponse.ExecutionEventTrace> executionEvents;
    private List<AiRunTraceResponse.WorkflowTrace> workflows;

    public static AiConversationTurnResponse from(AiRunTraceResponse trace) {
        AiConversationTurnResponse response = new AiConversationTurnResponse();
        response.setRun(trace.getRun());
        response.setTurnNo(trace.getRun().getTurnNo());
        response.setStatus(trace.getRun().getStatus());
        response.setMessages(trace.getMessages());
        response.setUserMessage(firstByRole(trace.getMessages(), "USER"));
        response.setAssistantMessage(lastByRole(trace.getMessages(), "ASSISTANT"));
        response.setToolResults(trace.getToolCalls());
        response.setProposals(trace.getProposals());
        response.setApprovals(trace.getApprovals());
        response.setExecutionEvents(trace.getExecutionEvents());
        response.setWorkflows(trace.getWorkflows());
        return response;
    }

    private static AiRunTraceResponse.MessageTrace firstByRole(List<AiRunTraceResponse.MessageTrace> messages,
                                                               String role) {
        return messages.stream()
                .filter(message -> role.equals(message.role()))
                .findFirst()
                .orElse(null);
    }

    private static AiRunTraceResponse.MessageTrace lastByRole(List<AiRunTraceResponse.MessageTrace> messages,
                                                              String role) {
        AiRunTraceResponse.MessageTrace match = null;
        for (AiRunTraceResponse.MessageTrace message : messages) {
            if (role.equals(message.role())) {
                match = message;
            }
        }
        return match;
    }
}
