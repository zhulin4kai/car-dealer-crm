package com.autodealer.crm.modules.fulfillment.invoice.application.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 创建发票请求。
 */
@Data
public class CreateTranInvoiceRequest {

    @NotNull(message = "交易ID不能为空")
    private Integer tranId;

    @NotBlank(message = "发票类型不能为空")
    private String type;

    @NotBlank(message = "发票抬头不能为空")
    private String title;

    @NotBlank(message = "纳税人识别号不能为空")
    private String taxNumber;

    private String bankName;

    private String bankAccount;

    private String address;

    private String phone;

    @NotNull(message = "发票金额不能为空")
    @DecimalMin(value = "0.01", message = "发票金额必须大于0")
    private BigDecimal amount;

    private String remark;
}
