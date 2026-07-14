package com.autodealer.crm.modules.fulfillment.payment.application.api.dto;

import lombok.Data;

@Data
public class ExecuteRefundRequest {
    private String transactionRef;
    private String remark;
    /**
     * 为空时按执行成功处理；false 表示记录外部退款执行失败。
     */
    private Boolean success;
    private String failureReason;
}
