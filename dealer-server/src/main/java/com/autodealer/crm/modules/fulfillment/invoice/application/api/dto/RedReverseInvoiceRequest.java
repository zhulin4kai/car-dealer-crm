package com.autodealer.crm.modules.fulfillment.invoice.application.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 发票红冲请求。
 */
@Data
public class RedReverseInvoiceRequest {

    @NotNull(message = "红冲金额不能为空")
    @DecimalMin(value = "0.01", message = "红冲金额必须大于0")
    private BigDecimal amount;

    @NotBlank(message = "红冲原因不能为空")
    private String reason;
}
