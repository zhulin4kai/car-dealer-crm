package com.autodealer.crm.modules.ai.application.internal.tool;

import com.autodealer.crm.shared.error.BusinessException;
import com.autodealer.crm.shared.error.CodeEnum;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class AiToolArgumentBinder {
    private final ObjectMapper objectMapper;
    private final Validator validator;

    public AiToolArgumentBinder(ObjectMapper objectMapper, Validator validator) {
        this.objectMapper = objectMapper;
        this.validator = validator;
    }

    public <T> T bind(Map<String, Object> arguments, Class<T> targetType) {
        T request = objectMapper.convertValue(arguments == null ? Map.of() : arguments, targetType);
        Set<ConstraintViolation<T>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            String message = violations.stream()
                    .map(ConstraintViolation::getMessage)
                    .sorted()
                    .collect(Collectors.joining("; "));
            throw new BusinessException(CodeEnum.AI_TOOL_ARGUMENT_INVALID, message);
        }
        return request;
    }
}
