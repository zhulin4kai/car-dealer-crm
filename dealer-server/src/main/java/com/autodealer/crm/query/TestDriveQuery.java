package com.autodealer.crm.query;

import lombok.Data;

@Data
public class TestDriveQuery {
    private Integer page = 1;
    private Integer size = 10;
    private Integer customerId;
    private Long opportunityId;
    private Long vehicleId;
    private Integer ownerId;
    private String status;
    private String keyword;
    private Integer dataScopeUserId;
}
