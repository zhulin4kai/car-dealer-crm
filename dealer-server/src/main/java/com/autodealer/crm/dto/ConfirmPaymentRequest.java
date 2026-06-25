package com.autodealer.crm.dto;

import lombok.Data;

@Data
public class ConfirmPaymentRequest {
    private Boolean approved;
    private String comment;
}
