package com.autodealer.crm.modules.fulfillment.payment.application.api.dto;

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

    /**
     * 兼容旧客户端字段。当前收款金额由服务端按交易剩余应收计算，不信任客户端提交值。
     */
    private BigDecimal amount;

    @NotBlank(message = "支付方式不能为空")
    private String paymentMethod;

    private String paymentType;

    private String transactionRef;

    private String remark;
}
