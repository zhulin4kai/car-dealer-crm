package com.autodealer.crm.dto.user;

import com.autodealer.crm.enums.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Date;

public final class UserHistoryRows {
    private UserHistoryRows() {}

    @Data
    public static class AuthorizationRow {
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
        private String operatorName;
        private String operatorEmployeeNo;
        private LocalDateTime occurredTime;
        private String requestId;
        private String affectedUsersSnapshot;
    }

    @Data
    public static class OperationRow {
        private Integer id;
        private Integer operatorId;
        private String operatorName;
        private String operatorEmployeeNo;
        private String actionCode;
        private String moduleName;
        private String objectType;
        private String resourceId;
        private String result;
        private String detail;
        private String requestId;
        private Date createTime;
    }

    @Data
    public static class LifecycleRow {
        private Long id; private String operationId; private String action;
        private Integer userId; private Integer employeeId; private String beforeValue; private String afterValue;
        private String reason; private Integer operatorId; private String operatorName; private String operatorEmployeeNo;
        private LocalDateTime occurredTime;
    }
}
