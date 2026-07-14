package com.autodealer.crm.modules.ai.application.api.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CancelAiRunRequest {
    @Size(max = 255)
    private String reason;
}
