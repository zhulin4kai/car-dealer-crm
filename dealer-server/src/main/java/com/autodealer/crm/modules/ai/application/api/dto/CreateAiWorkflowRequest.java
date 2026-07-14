package com.autodealer.crm.modules.ai.application.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateAiWorkflowRequest {
    @NotBlank
    @Size(max = 64)
    private String runNo;

    @NotBlank
    @Size(max = 64)
    private String workflowType;

    @Size(max = 64)
    private String contextObjectType;

    @Size(max = 64)
    private String contextObjectId;
}
