package com.autodealer.crm.modules.fulfillment.delivery.application.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SignDeliveryRequest {
    @NotBlank(message = "签收人不能为空")
    private String signerName;

    @NotNull(message = "签收时间不能为空")
    private LocalDateTime signedAt;

    @NotBlank(message = "签收方式不能为空")
    private String signMethod;

    @NotBlank(message = "签收凭证不能为空")
    private String signEvidence;
}
