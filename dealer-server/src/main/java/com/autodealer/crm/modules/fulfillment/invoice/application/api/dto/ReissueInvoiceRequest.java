package com.autodealer.crm.modules.fulfillment.invoice.application.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 发票重开请求。
 */
@Data
public class ReissueInvoiceRequest {

    private String type;

    private String title;

    private String taxNumber;

    private String bankName;

    private String bankAccount;

    private String address;

    private String phone;

    @NotNull(message = "发票金额不能为空")
    @DecimalMin(value = "0.01", message = "发票金额必须大于0")
    private BigDecimal amount;

    @NotBlank(message = "重开原因不能为空")
    private String reason;
}
