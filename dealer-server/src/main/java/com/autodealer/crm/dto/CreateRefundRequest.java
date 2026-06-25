package com.autodealer.crm.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateRefundRequest {
    @NotBlank(message = "退款类型不能为空")
    private String refundType;

    @NotNull(message = "退款金额不能为空")
    @DecimalMin(value = "0", inclusive = false, message = "退款金额必须大于0")
    private BigDecimal amount;

    @NotBlank(message = "退款原因不能为空")
    private String reason;
}
