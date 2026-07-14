package com.autodealer.crm.modules.fulfillment.delivery.application.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DeliveryCheckItemRequest {
    @NotBlank(message = "准备项编码不能为空")
    private String itemCode;

    @NotBlank(message = "准备项名称不能为空")
    private String itemName;

    private Integer responsibleUserId;
}
