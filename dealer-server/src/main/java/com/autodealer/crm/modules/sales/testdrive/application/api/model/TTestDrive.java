package com.autodealer.crm.modules.sales.testdrive.application.api.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TTestDrive {
    private Long id;
    private String testDriveNo;
    private Integer customerId;
    private String customerName;
    private Long opportunityId;
    private String opportunityNo;
    private Long vehicleId;
    private String vin;
    private String vehicleName;
    private Integer ownerId;
    private String ownerName;
    private LocalDateTime plannedStartTime;
    private LocalDateTime plannedEndTime;
    private LocalDateTime actualArriveTime;
    private LocalDateTime actualStartTime;
    private LocalDateTime actualEndTime;
    private LocalDateTime safetyConfirmedAt;
    private Integer safetyConfirmedBy;
    private Integer checkInBy;
    private String customerConfirmMethod;
    private String status;
    private String contactName;
    private String contactPhone;
    private String result;
    private String customerFeedback;
    private String nextAction;
    private String cancelType;
    private String cancelReason;
    private String remark;
    private Integer rescheduleCount;
    private Integer version;
    private LocalDateTime createTime;
    private Integer createBy;
    private LocalDateTime updateTime;
    private Integer updateBy;
}
