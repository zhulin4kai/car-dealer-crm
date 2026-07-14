package com.autodealer.crm.modules.sales.testdrive.application.api.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TTestDriveVehicleHold {
    private Long id;
    private Long testDriveId;
    private Long vehicleId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;
    private String releaseReason;
    private LocalDateTime releaseTime;
    private LocalDateTime createTime;
    private Integer createBy;
    private LocalDateTime updateTime;
    private Integer updateBy;
}
