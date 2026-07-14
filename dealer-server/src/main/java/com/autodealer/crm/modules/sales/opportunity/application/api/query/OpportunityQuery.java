package com.autodealer.crm.modules.sales.opportunity.application.api.query;

import lombok.Data;

@Data
public class OpportunityQuery {
    private Integer page = 1;
    private Integer size = 10;
    private Integer customerId;
    private Integer ownerId;
    private String stage;
    private String keyword;
    private Integer dataScopeUserId;
}
