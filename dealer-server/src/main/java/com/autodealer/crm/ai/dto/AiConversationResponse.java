package com.autodealer.crm.ai.dto;

import com.autodealer.crm.ai.model.TAiConversation;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AiConversationResponse {
    private String conversationNo;
    private String title;
    private String status;
    private String entryPoint;
    private String contextObjectType;
    private String contextObjectId;
    private String summaryText;
    private String lastRunNo;
    private LocalDateTime lastMessageTime;
    private LocalDateTime createTime;
    private LocalDateTime editTime;

    public static AiConversationResponse from(TAiConversation conversation) {
        AiConversationResponse response = new AiConversationResponse();
        response.setConversationNo(conversation.getConversationNo());
        response.setTitle(conversation.getTitle());
        response.setStatus(conversation.getStatus());
        response.setEntryPoint(conversation.getEntryPoint());
        response.setContextObjectType(conversation.getContextObjectType());
        response.setContextObjectId(conversation.getContextObjectId());
        response.setSummaryText(conversation.getSummaryText());
        response.setLastRunNo(conversation.getLastRunNo());
        response.setLastMessageTime(conversation.getLastMessageTime());
        response.setCreateTime(conversation.getCreateTime());
        response.setEditTime(conversation.getEditTime());
        return response;
    }
}
