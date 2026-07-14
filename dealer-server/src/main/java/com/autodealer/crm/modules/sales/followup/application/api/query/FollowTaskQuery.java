package com.autodealer.crm.modules.sales.followup.application.api.query;

import lombok.Data;

@Data
public class FollowTaskQuery {
    private Integer page = 1;
    private Integer size = 10;
    private String status;
    private String taskType;
    private String relatedObjectType;
    private Long relatedObjectId;
    private Integer ownerId;
    private Boolean overdueOnly;
    private String keyword;
    private Integer dataScopeUserId;
}
