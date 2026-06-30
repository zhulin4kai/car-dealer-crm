package com.autodealer.crm.ai.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AiWorkflowActionRequest {
    @Size(max = 500)
    private String reason;
}
