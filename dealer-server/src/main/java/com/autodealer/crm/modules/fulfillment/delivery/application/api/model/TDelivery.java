package com.autodealer.crm.modules.fulfillment.delivery.application.api.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TDelivery {
    private Long id;
    private Integer tranId;
    private Integer customerId;
    private Long vehicleId;
    private String status;
    private LocalDateTime plannedDeliveryTime;
    private LocalDateTime actualDeliveryTime;
    private Integer responsibleUserId;
    private String signerName;
    private LocalDateTime signedAt;
    private String signMethod;
    private String signEvidence;
    private String exceptionType;
    private String exceptionReason;
    private LocalDateTime createTime;
    private Integer createBy;
    private LocalDateTime updateTime;
    private Integer updateBy;
}
