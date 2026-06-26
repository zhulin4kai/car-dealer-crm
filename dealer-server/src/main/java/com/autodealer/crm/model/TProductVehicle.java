package com.autodealer.crm.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TProductVehicle {
    private Long id;
    private Long productId;
    private String productSku;
    private String productName;
    private String vin;
    private String color;
    private String configuration;
    private String location;
    private String status;
    private String holdType;
    private String sourceType;
    private Long sourceId;
    private LocalDateTime holdUntil;
    private LocalDateTime createTime;
    private Integer createBy;
    private LocalDateTime updateTime;
    private Integer updateBy;
}
