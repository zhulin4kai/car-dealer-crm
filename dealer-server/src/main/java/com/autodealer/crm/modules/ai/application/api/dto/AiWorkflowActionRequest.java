package com.autodealer.crm.modules.ai.application.api.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AiWorkflowActionRequest {
    @Size(max = 500)
    private String reason;
}
