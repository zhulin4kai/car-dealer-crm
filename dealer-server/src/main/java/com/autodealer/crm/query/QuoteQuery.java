package com.autodealer.crm.query;

import lombok.Data;

@Data
public class QuoteQuery {
    private String quoteNo;
    private Integer customerId;
    private String status;
    private Integer page = 1;
    private Integer size = 10;
}
