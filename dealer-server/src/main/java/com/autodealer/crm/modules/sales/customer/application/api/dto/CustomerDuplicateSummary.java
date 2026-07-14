package com.autodealer.crm.modules.sales.customer.application.api.dto;

import lombok.Data;

@Data
public class CustomerDuplicateSummary {

    private Integer customerId;

    private String customerName;

    private String maskedPhone;

    private String ownerName;
}
