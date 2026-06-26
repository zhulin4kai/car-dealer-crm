package com.autodealer.crm.model;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@Data
public class TRefundRequest implements Serializable {
    private Integer id;
    private Integer tranId;
    private Integer originalPaymentId;
    private Integer refundPaymentId;
    private BigDecimal amount;
    private String refundType;
    private String reason;
    private String status;
    private Integer requestedBy;
    private Date requestedTime;
    private Integer approvedBy;
    private Date approvedTime;
    private String approveComment;
    private Integer executedBy;
    private Date executionStartedTime;
    private Date executedTime;
    private String executionRef;
    private String executionRemark;
    private String failureReason;
    private Date createTime;
    private Integer createBy;
    private Date editTime;
    private Integer editBy;

    private static final long serialVersionUID = 1L;
}
