package com.autodealer.crm.modules.ai.application.api.dto;

import com.autodealer.crm.modules.ai.application.api.enums.AiEntryPoint;

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
