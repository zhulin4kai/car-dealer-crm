package com.autodealer.crm.modules.ai.application.internal;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import com.autodealer.crm.modules.ai.application.api.dto.AiSseEventResponse;
import com.autodealer.crm.modules.ai.persistence.mapper.TAiRunEventMapper;
import com.autodealer.crm.modules.ai.persistence.model.TAiRunEvent;
import com.autodealer.crm.shared.error.BusinessException;
import com.autodealer.crm.shared.error.CodeEnum;

import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Component
public class AiRunEventStore {
    private final TAiRunEventMapper mapper;
    private final ObjectMapper objectMapper;

    public AiRunEventStore(TAiRunEventMapper mapper, ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.objectMapper = objectMapper;
    }

    public void append(Long runId, AiSseEventResponse response) {
        TAiRunEvent event = new TAiRunEvent();
        event.setRunId(runId);
        event.setEventId(response.getEventId());
        event.setSequenceNo(response.getSequence());
        event.setEventType(response.getType());
        event.setPayloadJson(writePayload(response.getPayload()));
        event.setOccurredTime(response.getOccurredAt());
        event.setCreateTime(LocalDateTime.now());
        try {
            if (mapper.insert(event) != 1) {
                throw new BusinessException(CodeEnum.OPERATION_FAILED, "AI Run 事件写入失败");
            }
        } catch (DataIntegrityViolationException exception) {
            // 只吞掉相同 Run 的重复事件；外键、非空等其他数据错误必须继续失败。
            if (mapper.countSameEvent(runId, response.getEventId(), response.getSequence()) == 0) {
                throw exception;
            }
        }
    }

    public List<AiSseEventResponse> listAfter(Long runId, String runNo, int afterSequence) {
        return mapper.selectAfterSequence(runId, Math.max(0, afterSequence)).stream()
                .map(event -> toResponse(runNo, event))
                .toList();
    }

    private AiSseEventResponse toResponse(String runNo, TAiRunEvent event) {
        AiSseEventResponse response = new AiSseEventResponse();
        response.setEventId(event.getEventId());
        response.setRunNo(runNo);
        response.setSequence(event.getSequenceNo());
        response.setType(event.getEventType());
        response.setPayload(readPayload(event.getPayloadJson()));
        response.setOccurredAt(event.getOccurredTime());
        return response;
    }

    private String writePayload(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload == null ? Map.of() : payload);
        } catch (JacksonException ex) {
            throw new BusinessException(CodeEnum.AI_SSE_FAILED, "AI Run 事件序列化失败", ex);
        }
    }

    private Map<String, Object> readPayload(String payload) {
        try {
            return objectMapper.readValue(payload, new TypeReference<>() { });
        } catch (JacksonException ex) {
            throw new BusinessException(CodeEnum.AI_SSE_FAILED, "AI Run 事件恢复失败", ex);
        }
    }
}
