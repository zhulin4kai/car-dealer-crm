package com.autodealer.crm.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;

@Data
public class AdvanceOpportunityStageRequest {
    @NotBlank(message = "当前阶段不能为空")
    private String expectedStage;

    @NotBlank(message = "目标阶段不能为空")
    private String targetStage;

    @NotBlank(message = "推进原因不能为空")
    private String reason;

    private LocalDate nextActionTime;
}
