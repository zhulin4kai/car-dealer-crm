package com.autodealer.crm.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RenameAiConversationRequest {
    @NotBlank
    @Size(max = 128)
    private String title;
}
