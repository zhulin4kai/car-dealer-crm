package com.autodealer.crm.dto.access;

import com.autodealer.crm.enums.DataScopeCode;
import com.autodealer.crm.enums.PermissionSensitivityLevel;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class UserAuthorizationDtos {
    private UserAuthorizationDtos() {}

    public enum PersonalState { INHERIT, GRANT, DENY }
    public enum SourceType { ROLE, PERSONAL_GRANT, PERSONAL_DENY }
    public enum BatchRoleOperation { ASSIGN, UNASSIGN }

    @Data public static class TargetUser {
        private Integer id; private String loginAct; private String name; private String employeeNo;
        private String organizationName; private String positionName;
        private boolean accountEnabled; private boolean protectedAccount;
    }
    @Data public static class RoleAssignment {
        private Integer roleId; private String roleCode; private String roleName; private String source;
        private String sourceDescription; private OffsetDateTime effectiveFrom; private OffsetDateTime effectiveTo;
    }
    @Data public static class RoleCandidate {
        private Integer roleId; private String roleCode; private String roleName; private Integer authorizationLevel;
        private DataScopeCode defaultDataScope; private boolean selected; private boolean editable;
        private String unavailableReason;
    }
    @Data public static class ScopeCandidate {
        private String candidateKey; private DataScopeCode code; private String label; private String description;
        private List<Integer> organizationIds = new ArrayList<>();
        private List<String> organizationNames = new ArrayList<>();
    }
    @Data public static class PermissionSource {
        private SourceType type; private Integer sourceId; private String sourceName; private String dataScopeLabel;
        private List<Integer> organizationIds = new ArrayList<>();
        private List<String> organizationNames = new ArrayList<>();
        private OffsetDateTime effectiveFrom; private OffsetDateTime effectiveTo; private boolean active;
    }
    @Data public static class PermissionItem {
        private Integer permissionId; private String code; private String name; private String module; private String description;
        private PermissionSensitivityLevel sensitivityLevel; private boolean delegable; private boolean effective;
        private PersonalState personalState; private String personalDataScopeCandidateKey;
        private List<Integer> personalOrganizationIds = new ArrayList<>();
        private OffsetDateTime personalEffectiveFrom; private OffsetDateTime personalEffectiveTo;
        private boolean editable; private String unavailableReason;
        private List<PermissionSource> sources = new ArrayList<>();
        private List<ScopeCandidate> dataScopeCandidates = new ArrayList<>();
    }
    @Data public static class Detail {
        private TargetUser user; private Integer authorizationVersion;
        private List<String> allowedActions = new ArrayList<>();
        private Map<String, String> unavailableReasons = new LinkedHashMap<>();
        private List<RoleAssignment> roleAssignments = new ArrayList<>();
        private List<RoleCandidate> roleCandidates = new ArrayList<>();
        private List<PermissionItem> permissions = new ArrayList<>();
    }
    @Data public static class UpdateRolesRequest {
        @NotNull private Integer authorizationVersion;
        @NotNull @Size(max = 100) private List<Integer> roleIds;
        @NotBlank @Size(max = 500) private String reason;
    }
    @Data public static class PermissionChange {
        @NotNull private Integer permissionId;
        @NotNull private PersonalState state;
        private String dataScopeCandidateKey;
        private List<Integer> customOrganizationUnitIds = new ArrayList<>();
        /** GRANT/DENY 为空表示立即生效；显式时间只能从当前起预约，且不得超过一年。 */
        private OffsetDateTime effectiveFrom;
        /** 必须严格晚于本次实际生效时间；INHERIT 不得携带有效期。 */
        private OffsetDateTime effectiveTo;
    }
    @Data public static class UpdatePermissionsRequest {
        @NotNull private Integer authorizationVersion;
        @NotNull @Size(max = 200) private List<@NotNull @Valid PermissionChange> changes;
        @NotBlank @Size(max = 500) private String reason;
    }
    @Data public static class BatchTarget {
        @NotNull private Integer userId;
        @NotNull private Integer authorizationVersion;
    }
    @Data public static class BatchUpdateRolesRequest {
        @NotNull @Size(min = 1, max = 50) private List<@NotNull @Valid BatchTarget> targets;
        @NotNull private BatchRoleOperation operation;
        @NotNull @Size(min = 1, max = 100) private List<Integer> roleIds;
        @NotBlank @Size(max = 500) private String reason;
    }
    @Data public static class BatchUpdatePermissionsRequest {
        @NotNull @Size(min = 1, max = 50) private List<@NotNull @Valid BatchTarget> targets;
        @NotNull @Size(min = 1, max = 200) private List<@NotNull @Valid PermissionChange> changes;
        @NotBlank @Size(max = 500) private String reason;
    }
    @Data public static class BatchTargetResult {
        private Integer userId;
        private Integer authorizationVersion;
        private boolean changed;
    }
    @Data public static class BatchResult {
        private int targetCount;
        private int changedTargetCount;
        private List<BatchTargetResult> targets = new ArrayList<>();
    }
}
