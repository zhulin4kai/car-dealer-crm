package com.autodealer.crm.query;

import lombok.Data;

@Data
public class ProductVehicleQuery {
    private Long productId;
    private String status;
    private String vin;
    private Integer page = 1;
    private Integer size = 10;
}
