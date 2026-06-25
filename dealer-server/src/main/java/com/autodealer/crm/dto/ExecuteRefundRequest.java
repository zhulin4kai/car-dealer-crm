package com.autodealer.crm.dto;

import lombok.Data;

@Data
public class ExecuteRefundRequest {
    private String transactionRef;
    private String remark;
}
