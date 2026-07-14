package com.autodealer.crm.modules.identity.persistence.model;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户角色关系事实。旧写入可以仅写 userId/roleId，其余字段用于新授权流程。
 */
@Data
public class TUserRole implements Serializable {
    private Long id;
    private Integer userId;
    private Integer roleId;
    private Integer grantedBy;
    private String reason;
    private LocalDateTime effectiveFrom;
    private LocalDateTime effectiveTo;
    private Boolean activeMarker;
    private Integer version;

    private static final long serialVersionUID = 1L;
}
