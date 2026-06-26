package com.autodealer.crm.query;

import lombok.Data;

@Data
public class CommunicationRecordQuery {
    private Integer page = 1;
    private Integer size = 10;
    private Long followTaskId;
    private String relatedObjectType;
    private Long relatedObjectId;
    private Integer ownerId;
    private String status;
    private String keyword;
    private Integer dataScopeUserId;
}
