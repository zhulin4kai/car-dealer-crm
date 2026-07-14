package com.autodealer.crm.modules.ai.application.internal;

import com.autodealer.crm.modules.ai.persistence.mapper.TAiMessageMapper;
import com.autodealer.crm.modules.ai.persistence.mapper.TAiRunMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AiMapperContextContractTest {
    @Test
    void conversationRecovery_shouldOnlyLoadActiveContextRunsAndMessages() throws IOException {
        String runMapper = Files.readString(Path.of("src/main/resources/mapper/ai/TAiRunMapper.xml"));
        String messageMapper = Files.readString(Path.of("src/main/resources/mapper/ai/TAiMessageMapper.xml"));

        String conversationRuns = statement(runMapper, "selectByConversationId");
        assertTrue(conversationRuns.contains("r.context_active = TRUE"));
        String ownedMessage = statement(messageMapper, "selectOwnedUserMessageByNo");
        assertTrue(ownedMessage.contains("m.status = 'ACTIVE'"));
    }

    private String statement(String xml, String id) {
        int start = xml.indexOf("id=\"" + id + "\"");
        int end = xml.indexOf("</select>", start);
        return xml.substring(start, end);
    }
}
