package com.autodealer.crm.modules.audit.application.api.query;

import com.autodealer.crm.shared.pagination.BaseQuery;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = true)
public class AuditLoginLogQuery extends BaseQuery {

    private String loginAct;
    private Integer userId;
    private String userName;
    private String result;
    private String reasonCode;
    private String ip;
    private String requestId;
    private Date startTime;
    private Date endTime;
}
