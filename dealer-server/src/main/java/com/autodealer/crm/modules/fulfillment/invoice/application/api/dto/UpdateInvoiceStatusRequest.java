package com.autodealer.crm.modules.fulfillment.invoice.application.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 更新发票状态请求。
 */
@Data
public class UpdateInvoiceStatusRequest {

    @NotBlank(message = "发票状态不能为空")
    private String status;

    private String reason;
}
