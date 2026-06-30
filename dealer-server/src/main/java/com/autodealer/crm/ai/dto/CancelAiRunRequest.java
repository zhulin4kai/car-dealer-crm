package com.autodealer.crm.ai.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CancelAiRunRequest {
    @Size(max = 255)
    private String reason;
}
