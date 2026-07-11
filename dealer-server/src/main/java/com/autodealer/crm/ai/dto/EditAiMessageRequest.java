package com.autodealer.crm.ai.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class EditAiMessageRequest {
    @NotBlank
    @Size(max = 4000)
    private String content;

    @NotNull
    @Min(1)
    private Integer expectedVersion;
}
