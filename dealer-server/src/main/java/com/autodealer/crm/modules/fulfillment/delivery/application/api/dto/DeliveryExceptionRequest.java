package com.autodealer.crm.modules.fulfillment.delivery.application.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DeliveryExceptionRequest {
    @NotBlank(message = "异常类型不能为空")
    private String exceptionType;

    @NotBlank(message = "异常原因不能为空")
    private String reason;
}
