package com.autodealer.crm.modules.fulfillment.delivery.application.api.query;

import lombok.Data;

@Data
public class DeliveryQuery {
    private Integer page = 1;
    private Integer size = 10;
    private Integer tranId;
    private Integer customerId;
    private Long vehicleId;
    private Integer responsibleUserId;
    private String status;
}
