package com.autodealer.crm.modules.sales.customer.application.api.dto;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class CustomerDuplicateResponse {

    private boolean duplicated;

    private boolean hiddenConflict;

    private List<CustomerDuplicateSummary> visibleCustomers = new ArrayList<>();
}
