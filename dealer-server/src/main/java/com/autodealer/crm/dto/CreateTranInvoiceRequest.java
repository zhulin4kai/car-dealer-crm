package com.autodealer.crm.dto;

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

    private String type;

    private String title;

    private String taxNumber;

    private String bankName;

    private String bankAccount;

    private String address;

    private String phone;

    @NotNull(message = "发票金额不能为空")
    private BigDecimal amount;

    private String remark;
}
