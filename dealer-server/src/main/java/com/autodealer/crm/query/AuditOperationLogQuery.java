package com.autodealer.crm.query;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = true)
public class AuditOperationLogQuery extends BaseQuery {

    private Integer userId;
    private String userName;
    private String actionCode;
    private String moduleName;
    private String objectType;
    private String resourceId;
    private String result;
    private String ip;
    private String requestId;
    private Date startTime;
    private Date endTime;
}
