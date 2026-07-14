package com.autodealer.crm.modules.fulfillment.delivery.application.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DeliveryCancelRequest {
    @NotBlank(message = "取消原因不能为空")
    private String reason;
}
