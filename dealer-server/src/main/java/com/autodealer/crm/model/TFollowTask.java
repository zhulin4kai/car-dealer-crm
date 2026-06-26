package com.autodealer.crm.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TFollowTask {
    private Long id;
    private String title;
    private String taskType;
    private String relatedObjectType;
    private Long relatedObjectId;
    private String relatedObjectName;
    private Integer ownerId;
    private String ownerName;
    private String priority;
    private LocalDateTime dueTime;
    private LocalDateTime remindTime;
    private String status;
    private String result;
    private String postponeReason;
    private LocalDateTime originalDueTime;
    private Integer postponeCount;
    private String cancelReason;
    private Long communicationRecordId;
    private LocalDateTime completedTime;
    private Integer completedBy;
    private Integer version;
    private LocalDateTime createTime;
    private Integer createBy;
    private LocalDateTime updateTime;
    private Integer updateBy;
}
