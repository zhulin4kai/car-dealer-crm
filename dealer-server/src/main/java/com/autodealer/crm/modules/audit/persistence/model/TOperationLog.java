package com.autodealer.crm.modules.audit.persistence.model;

import lombok.Data;
import java.io.Serializable;
import java.util.Date;

/**
 * 操作日志表
 * t_operation_log
 */
@Data
public class TOperationLog implements Serializable {
    private Integer id;
    private Integer userId;
    private String userName;
    private String actionCode;
    private String moduleName;
    private String objectType;
    private String resourceId;
    private String result;
    private String detail;
    private String ip;
    private String requestId;
    private Date createTime;

    private static final long serialVersionUID = 1L;
}
