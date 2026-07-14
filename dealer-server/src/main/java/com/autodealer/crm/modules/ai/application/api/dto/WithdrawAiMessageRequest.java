package com.autodealer.crm.modules.ai.application.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class WithdrawAiMessageRequest {
    @NotNull
    @Min(1)
    private Integer expectedVersion;
}
