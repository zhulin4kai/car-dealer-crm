package com.autodealer.crm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CreateOpportunityRequest {
    @NotNull(message = "客户不能为空")
    private Integer customerId;

    private Long clueId;
    private Long productId;
    private String sourceType;

    @NotBlank(message = "购车需求不能为空")
    private String requirement;

    @PositiveOrZero(message = "预计金额不能为负数")
    private BigDecimal expectedAmount;
    private LocalDate expectedCloseDate;
    private LocalDate nextActionTime;
}
