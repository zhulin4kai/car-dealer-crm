package com.autodealer.crm.model;

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
    private String resourceId;
    private String detail;
    private String ip;
    private Date createTime;

    private static final long serialVersionUID = 1L;
}
