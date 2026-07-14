package com.autodealer.crm.modules.commerce.inventory.application.api.query;

import lombok.Data;

@Data
public class ProductVehicleQuery {
    private Long productId;
    private String status;
    private String vin;
    private Integer page = 1;
    private Integer size = 10;
}
