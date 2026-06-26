package com.autodealer.crm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class UpdateOpportunityRequest {
    @NotNull(message = "商机ID不能为空")
    private Long id;

    private Long productId;

    @NotBlank(message = "购车需求不能为空")
    private String requirement;

    @PositiveOrZero(message = "预计金额不能为负数")
    private BigDecimal expectedAmount;
    private LocalDate expectedCloseDate;
    private LocalDate nextActionTime;
}
