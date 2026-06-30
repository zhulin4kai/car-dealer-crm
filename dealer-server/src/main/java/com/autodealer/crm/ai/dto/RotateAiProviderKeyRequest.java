package com.autodealer.crm.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RotateAiProviderKeyRequest {
    @NotBlank
    @Size(max = 500)
    private String apiKey;
}
