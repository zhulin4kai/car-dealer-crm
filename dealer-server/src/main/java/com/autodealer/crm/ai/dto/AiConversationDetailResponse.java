package com.autodealer.crm.ai.dto;

import lombok.Data;

import java.util.List;

@Data
public class AiConversationDetailResponse {
    private AiConversationResponse conversation;
    private List<AiRunTraceResponse.MessageTrace> messages;
    private List<AiConversationTurnResponse> turns;
    private AiRunResponse latestRun;
    private AiRunTraceResponse latestRunTrace;
}
