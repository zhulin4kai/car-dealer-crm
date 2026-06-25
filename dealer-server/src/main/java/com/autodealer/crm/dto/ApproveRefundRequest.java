package com.autodealer.crm.dto;

import lombok.Data;

@Data
public class ApproveRefundRequest {
    private Boolean approved;
    private String comment;
}
