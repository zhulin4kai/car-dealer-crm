package com.autodealer.crm.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TTestDriveStatusHistory {
    private Long id;
    private Long testDriveId;
    private String fromStatus;
    private String toStatus;
    private String actionType;
    private String reason;
    private LocalDateTime oldStartTime;
    private LocalDateTime oldEndTime;
    private LocalDateTime newStartTime;
    private LocalDateTime newEndTime;
    private Integer operateBy;
    private LocalDateTime operateTime;
}
