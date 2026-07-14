package com.autodealer.crm.modules.sales.opportunity.application.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;

@Data
public class OpportunityResultRequest {
    private Integer orderTranId;

    @NotBlank(message = "原因不能为空")
    private String reason;

    private String competitor;
    private String remark;
    private LocalDate nextActionTime;
}
