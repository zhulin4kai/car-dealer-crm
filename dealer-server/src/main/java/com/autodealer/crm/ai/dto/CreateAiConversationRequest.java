package com.autodealer.crm.ai.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateAiConversationRequest {
    @Size(max = 128)
    private String title;

    @Pattern(regexp = "PAGE|SIDE_PANEL")
    private String entryPoint = "PAGE";

    @Size(max = 64)
    private String contextObjectType;

    @Size(max = 64)
    private String contextObjectId;
}
