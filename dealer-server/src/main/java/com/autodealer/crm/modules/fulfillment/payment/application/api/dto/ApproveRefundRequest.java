package com.autodealer.crm.modules.fulfillment.payment.application.api.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ApproveRefundRequest {
    @NotNull(message = "审批结果不能为空")
    private Boolean approved;
    private String comment;
}
