package com.autodealer.crm.ai.dto;

import com.autodealer.crm.ai.enums.AiMessageRole;

public record AiMessageCommand(
        Long conversationId,
        Long runId,
        AiMessageRole role,
        Integer sequenceNo,
        Boolean visibleToUser,
        String content,
        String messageNo,
        String status,
        Integer revisionNo,
        Long supersedesMessageId,
        Boolean includedInContext,
        Integer version
) {
    public AiMessageCommand(Long conversationId,
                            Long runId,
                            AiMessageRole role,
                            Integer sequenceNo,
                            Boolean visibleToUser,
                            String content) {
        this(conversationId, runId, role, sequenceNo, visibleToUser, content,
                null, "ACTIVE", 1, null, true, 1);
    }
}
