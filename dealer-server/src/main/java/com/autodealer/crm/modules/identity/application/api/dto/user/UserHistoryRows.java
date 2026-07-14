package com.autodealer.crm.modules.identity.application.api.dto.user;

import com.autodealer.crm.modules.identity.application.api.enums.AuthorizationChangeType;
import com.autodealer.crm.modules.identity.application.api.enums.AuthorizationSubjectType;
import com.autodealer.crm.modules.identity.application.api.enums.DataScopeCode;
import com.autodealer.crm.modules.identity.application.api.enums.PermissionEffect;
import com.autodealer.crm.modules.identity.application.api.enums.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

public final class UserHistoryRows {
    private UserHistoryRows() {}

    /** 数据库统一历史投影的查询条件；不作为外部 API DTO 使用。 */
    @Data
    public static class ProjectionQuery {
        private Integer userId;
        private String resourceId;
        private List<String> operationActionCodes;
        private List<String> lifecycleActions;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private String filterSource;
        private AuthorizationSubjectType filterSubjectType;
        private AuthorizationChangeType filterChangeType;
        private String filterActionCode;
        private long offset;
        private int limit;
    }

    /** 三个权威来源经 UNION ALL 后的安全映射原料。 */
    @Data
    public static class ProjectionRow {
        private Long id;
        private String sourceKey;
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
        private String actionCode;
        private String result;
        private String detail;
        private LocalDateTime occurredTime;
    }

    /** 同一时间窗内实际存在的动作维度，用于保持 actionOptions 契约。 */
    @Data
    public static class ActionFacet {
        private String sourceKey;
        private AuthorizationSubjectType subjectType;
        private AuthorizationChangeType changeType;
        private String actionCode;
    }
}
