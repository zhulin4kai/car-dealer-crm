package com.autodealer.crm.modules.fulfillment.payment.application.api.dto;

import lombok.Data;

@Data
public class ConfirmPaymentRequest {
    private Boolean approved;
    private String comment;
}
