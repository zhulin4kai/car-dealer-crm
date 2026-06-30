package com.autodealer.crm.ai.dto;

import com.autodealer.crm.ai.enums.AiEntryPoint;

import java.time.LocalDateTime;

public record AiCreateRunCommand(
        Long conversationId,
        Long parentRunId,
        Integer turnNo,
        AiEntryPoint entryPoint,
        String contextObjectType,
        String contextObjectId,
        String prompt,
        LocalDateTime expiresTime
) {
}
