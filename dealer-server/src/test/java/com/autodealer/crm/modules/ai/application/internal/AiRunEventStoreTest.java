package com.autodealer.crm.modules.ai.application.internal;

import com.autodealer.crm.modules.ai.application.api.dto.AiSseEventResponse;
import com.autodealer.crm.modules.ai.persistence.mapper.TAiRunEventMapper;
import com.autodealer.crm.modules.ai.persistence.model.TAiRunEvent;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiRunEventStoreTest {
    @Test
    void appendAndReplay_shouldKeepStableSequenceAndPayload() {
        TAiRunEventMapper mapper = mock(TAiRunEventMapper.class);
        AiRunEventStore store = new AiRunEventStore(mapper, new ObjectMapper());
        when(mapper.insert(org.mockito.ArgumentMatchers.any())).thenReturn(1);
        TAiRunEvent persisted = new TAiRunEvent();
        persisted.setRunId(1L);
        persisted.setEventId("evt-2");
        persisted.setSequenceNo(2);
        persisted.setEventType("message_delta");
        persisted.setPayloadJson("{\"content_delta\":\"好\"}");
        persisted.setOccurredTime(LocalDateTime.now());
        when(mapper.selectAfterSequence(1L, 1)).thenReturn(List.of(persisted));

        AiSseEventResponse event = new AiSseEventResponse();
        event.setEventId("evt-2");
        event.setRunNo("AIR1");
        event.setSequence(2);
        event.setType("message_delta");
        event.setPayload(Map.of("content_delta", "好"));
        event.setOccurredAt(persisted.getOccurredTime());
        store.append(1L, event);

        verify(mapper).insert(argThat(saved -> saved.getSequenceNo() == 2
                && saved.getPayloadJson().contains("content_delta")));
        assertEquals("好", store.listAfter(1L, "AIR1", 1).get(0).getPayload().get("content_delta"));
    }

    @Test
    void append_shouldOnlyIgnoreAConfirmedDuplicateEvent() {
        TAiRunEventMapper mapper = mock(TAiRunEventMapper.class);
        AiRunEventStore store = new AiRunEventStore(mapper, new ObjectMapper());
        AiSseEventResponse event = event();
        when(mapper.insert(org.mockito.ArgumentMatchers.any()))
                .thenThrow(new DataIntegrityViolationException("duplicate"));
        when(mapper.countSameEvent(1L, "evt-2", 2)).thenReturn(1);

        assertDoesNotThrow(() -> store.append(1L, event));
    }

    @Test
    void append_shouldNotHideOtherDataIntegrityFailures() {
        TAiRunEventMapper mapper = mock(TAiRunEventMapper.class);
        AiRunEventStore store = new AiRunEventStore(mapper, new ObjectMapper());
        AiSseEventResponse event = event();
        when(mapper.insert(org.mockito.ArgumentMatchers.any()))
                .thenThrow(new DataIntegrityViolationException("foreign key"));
        when(mapper.countSameEvent(1L, "evt-2", 2)).thenReturn(0);

        assertThrows(DataIntegrityViolationException.class, () -> store.append(1L, event));
    }

    private AiSseEventResponse event() {
        AiSseEventResponse event = new AiSseEventResponse();
        event.setEventId("evt-2");
        event.setRunNo("AIR1");
        event.setSequence(2);
        event.setType("message_delta");
        event.setPayload(Map.of("content_delta", "好"));
        event.setOccurredAt(LocalDateTime.now());
        return event;
    }
}
