package com.autodealer.crm.ai.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateAiProactiveSubscriptionRequest {
    @NotBlank
    @Size(max = 64)
    private String subscriptionType;

    @NotBlank
    @Size(max = 64)
    private String frequency;

    @Pattern(regexp = "^$|^([01]\\d|2[0-3]):[0-5]\\d$")
    private String quietStartTime;

    @Pattern(regexp = "^$|^([01]\\d|2[0-3]):[0-5]\\d$")
    private String quietEndTime;

    @Min(1)
    @Max(20)
    private Integer dailyLimit;

    @Min(1)
    @Max(20)
    private Integer maxResults;

    @Min(5)
    @Max(1440)
    private Integer duplicateWindowMinutes;

    @Size(max = 1000)
    private String configSummary;
}
