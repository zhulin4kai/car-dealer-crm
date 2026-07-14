package com.autodealer.crm.modules.ai.application.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class UpdateAiAssistantPolicyRequest {
    @NotNull
    private Boolean enabledTools;

    @NotNull
    @Size(max = 100)
    private List<@Size(max = 128) String> allowedToolNames;

    @NotNull
    private Boolean proposalsEnabled;

    @NotNull
    @Min(1)
    @Max(50)
    private Integer maxToolCallsPerRun;

    @NotNull
    @Pattern(regexp = "STRICT|STANDARD")
    private String safetyMode;

    @NotNull
    @Pattern(regexp = "DISABLED|PROVIDER_ONLY")
    private String networkMode;

    @NotNull
    @Min(1)
    @Max(8)
    private Integer contextMessageLimit;

    @NotNull
    @Min(500)
    @Max(8000)
    private Integer summaryMaxChars;

    @NotNull
    @Min(10)
    @Max(600)
    private Integer maxRunSeconds;

    @NotNull
    @Min(1)
    private Integer version;
}
