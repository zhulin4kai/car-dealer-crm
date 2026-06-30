package com.autodealer.crm.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateAiRunRequest {
    @NotBlank
    @Size(max = 4000)
    private String prompt;

    @NotBlank
    @Pattern(regexp = "PAGE|SIDE_PANEL")
    private String entryPoint;

    @Size(max = 64)
    private String conversationNo;

    @Size(max = 64)
    private String contextObjectType;

    @Size(max = 64)
    private String contextObjectId;
}
