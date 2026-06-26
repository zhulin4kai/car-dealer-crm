package com.autodealer.crm.dto;

import lombok.Data;

@Data
public class CustomerMergeResponse {

    private Integer targetCustomerId;

    private Integer sourceCustomerId;

    private int migratedRemarkCount;

    private int migratedTranCount;

    private int migratedQuoteCount;
}
