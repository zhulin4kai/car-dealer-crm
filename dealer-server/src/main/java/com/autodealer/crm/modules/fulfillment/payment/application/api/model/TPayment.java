package com.autodealer.crm.modules.fulfillment.payment.application.api.model;

import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 支付记录表
 * t_payment
 */
@Data
public class TPayment implements Serializable {
    private Integer id;
    private Integer tranId;
    private String paymentNo;
    private BigDecimal amount;
    private String paymentMethod;
    private String paymentType;
    private String paymentStatus;
    private Date paymentTime;
    private String transactionRef;
    private String idempotencyKey;
    private String remark;
    private Date createTime;
    private Integer createBy;
    private Date editTime;
    private Integer editBy;

    private static final long serialVersionUID = 1L;
}
