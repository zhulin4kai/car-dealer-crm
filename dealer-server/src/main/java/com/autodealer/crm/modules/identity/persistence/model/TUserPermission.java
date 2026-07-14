package com.autodealer.crm.modules.identity.persistence.model;

import com.autodealer.crm.modules.identity.application.api.enums.DataScopeCode;
import com.autodealer.crm.modules.identity.application.api.enums.PermissionEffect;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户个人权限当前态，对应 t_user_permission。
 *
 * <p>同一用户和权限只有一行，替换时必须使用 version 做 CAS；每次变化的
 * 不可变证据由 t_authorization_history 保存。
 */
@Data
public class TUserPermission implements Serializable {
    private Long id;
    private Integer userId;
    private Integer permissionId;
    private PermissionEffect effect;
    private DataScopeCode dataScopeCode;
    private LocalDateTime effectiveFrom;
    private LocalDateTime effectiveTo;
    private Boolean activeMarker;
    private String reason;
    private Integer grantedBy;
    private Integer version;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    private static final long serialVersionUID = 1L;
}
