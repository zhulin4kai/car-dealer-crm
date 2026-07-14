package com.autodealer.crm.modules.commerce.inventory.application.api.model;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class TProductStockRecord {
    private Long id;
    private Long productId;
    private Long vehicleId;
    private Integer quantity;
    private String type; // INBOUND/RESERVE/RELEASE/OUTBOUND
    private String sourceType;
    private Long sourceId;
    private String beforeStatus;
    private String afterStatus;
    private Long relatedRecordId;
    private String remark;
    private LocalDateTime createTime;
    private Integer createBy;
}
