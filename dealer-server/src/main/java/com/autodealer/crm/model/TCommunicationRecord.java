package com.autodealer.crm.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TCommunicationRecord {
    private Long id;
    private Long followTaskId;
    private Long parentRecordId;
    private String relatedObjectType;
    private Long relatedObjectId;
    private String relatedObjectName;
    private Integer ownerId;
    private String ownerName;
    private String communicationMethod;
    private LocalDateTime communicationTime;
    private String summary;
    private String customerFeedback;
    private String nextAction;
    private LocalDateTime nextFollowTime;
    private String status;
    private String correctionReason;
    private String voidReason;
    private Integer version;
    private LocalDateTime createTime;
    private Integer createBy;
    private LocalDateTime updateTime;
    private Integer updateBy;
}
