package com.autodealer.crm.ai.dto;

import com.autodealer.crm.ai.enums.AiMessageRole;

public record AiMessageCommand(
        Long conversationId,
        Long runId,
        AiMessageRole role,
        Integer sequenceNo,
        Boolean visibleToUser,
        String content
) {
}
