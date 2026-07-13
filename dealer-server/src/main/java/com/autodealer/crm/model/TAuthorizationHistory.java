package com.autodealer.crm.model;

import com.autodealer.crm.enums.AuthorizationChangeType;
import com.autodealer.crm.enums.AuthorizationSubjectType;
import com.autodealer.crm.enums.DataScopeCode;
import com.autodealer.crm.enums.PermissionEffect;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 不可变授权变化历史。Mapper 只提供插入和查询，不提供更新或删除入口。
 */
@Data
public class TAuthorizationHistory implements Serializable {
    private Long id;
    private AuthorizationSubjectType subjectType;
    private String subjectId;
    private AuthorizationChangeType changeType;
    private Integer targetUserId;
    private Integer roleId;
    private Integer permissionId;
    private PermissionEffect effect;
    private DataScopeCode dataScopeCode;
    private LocalDateTime effectiveFrom;
    private LocalDateTime effectiveTo;
    private String beforeValue;
    private String afterValue;
    private String reason;
    private Integer operatorId;
    private LocalDateTime occurredTime;
    private String requestId;
    /** 角色矩阵变化发生时的受影响用户 ID 集合，格式为 ,1,2,，只用于历史关联。 */
    private String affectedUserIds;
    /** 角色矩阵变化发生时的用户 code/name 快照 JSON，不在查询时反推当前成员。 */
    private String affectedUsersSnapshot;

    private static final long serialVersionUID = 1L;
}
