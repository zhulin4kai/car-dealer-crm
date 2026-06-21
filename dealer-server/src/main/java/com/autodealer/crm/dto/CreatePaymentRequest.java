package com.autodealer.crm.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 创建收款请求。
 */
@Data
public class CreatePaymentRequest {

    @NotNull(message = "交易ID不能为空")
    private Integer tranId;

    @NotNull(message = "收款金额不能为空")
    @DecimalMin(value = "0", inclusive = false, message = "收款金额必须大于0")
    private BigDecimal amount;

    @NotBlank(message = "支付方式不能为空")
    private String paymentMethod;

    @NotBlank(message = "支付类型不能为空")
    private String paymentType;

    private String transactionRef;

    private String remark;
}
