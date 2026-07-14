package com.autodealer.crm.modules.audit.persistence.model;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 登录审计日志表。
 */
@Data
public class TLoginLog implements Serializable {
    private Integer id;
    private String loginAct;
    private Integer userId;
    private String userName;
    private String result;
    private String reasonCode;
    private String reasonMessage;
    private String ip;
    private String browser;
    private String os;
    private String requestId;
    private Date createTime;

    private static final long serialVersionUID = 1L;
}
