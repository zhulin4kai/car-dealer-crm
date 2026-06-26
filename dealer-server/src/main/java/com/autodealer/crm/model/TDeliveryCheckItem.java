package com.autodealer.crm.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TDeliveryCheckItem {
    private Long id;
    private Long deliveryId;
    private String itemCode;
    private String itemName;
    private String status;
    private Integer responsibleUserId;
    private LocalDateTime completedTime;
    private String remark;
    private LocalDateTime createTime;
    private Integer createBy;
    private LocalDateTime updateTime;
    private Integer updateBy;
}
