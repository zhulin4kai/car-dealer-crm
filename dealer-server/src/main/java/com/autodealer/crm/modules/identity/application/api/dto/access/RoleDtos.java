package com.autodealer.crm.modules.identity.application.api.dto.access;

import com.autodealer.crm.modules.identity.application.api.enums.DataScopeCode;
import com.autodealer.crm.modules.identity.application.api.enums.PermissionSensitivityLevel;
import com.autodealer.crm.modules.identity.application.api.enums.RoleScopeType;
import com.autodealer.crm.modules.identity.application.api.enums.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.util.*;

public final class RoleDtos {
    private RoleDtos() {}

    @Data public static class OrganizationOption { private Integer id; private String name; private String pathName; }
    @Data public static class RoleResponse {
        private Integer id; private String code; private String name; private String description;
        private Boolean protectedRole; private String protectedReason; private Integer authorizationLevel;
        private DataScopeCode defaultDataScope; private RoleScopeType scopeType;
        private List<OrganizationOption> applicableOrganizations = new ArrayList<>();
        private Integer memberCount; private Boolean enabled; private Integer version;
        private Boolean editable; private String disabledReason;
        private List<String> allowedActions = new ArrayList<>();
        private Map<String,String> unavailableReasons = new LinkedHashMap<>();
    }
    @Data public static class CreateRoleRequest {
        @NotBlank @Pattern(regexp="[A-Za-z][A-Za-z0-9_-]*") @Size(max=64) private String code;
        @NotBlank @Size(max=64) private String name; @Size(max=255) private String description;
        @NotNull @Min(0) private Integer authorizationLevel; @NotNull private DataScopeCode defaultDataScope;
        @NotNull private RoleScopeType scopeType; @NotNull private List<Integer> organizationUnitIds = new ArrayList<>();
    }
    @Data public static class UpdateRoleRequest {
        @NotNull @Min(0) private Integer expectedVersion; @NotBlank @Size(max=64) private String name;
        @Size(max=255) private String description; @NotNull @Min(0) private Integer authorizationLevel;
        @NotNull private DataScopeCode defaultDataScope; @NotNull private RoleScopeType scopeType;
        @NotNull private List<Integer> organizationUnitIds = new ArrayList<>();
    }
    @Data public static class CopyRoleRequest extends CreateRoleRequest { @NotBlank @Size(max=500) private String reason; }
    @Data public static class ChangeRoleStatusRequest { @NotNull @Min(0) private Integer expectedVersion; @NotBlank @Size(max=500) private String reason; }
    @Data public static class PermissionItem {
        private Integer id; private String name; private String code; private String module; private String type;
        private String description; private PermissionSensitivityLevel sensitivityLevel; private Boolean delegable;
        private Boolean enabled; private Integer orderNo; private Integer parentId; private Boolean assignable;
        private String restrictionReason; private List<PermissionItem> children = new ArrayList<>();
    }
    @Data public static class MatrixResponse {
        private Integer roleId; private String roleName; private Integer expectedVersion;
        private List<Integer> selectedPermissionIds = new ArrayList<>(); private Boolean editable; private String disabledReason;
        private List<PermissionScopeAssignment> permissionScopes = new ArrayList<>();
        private List<PermissionScopeOption> permissionScopeOptions = new ArrayList<>();
    }
    @Data public static class PermissionScopeOption {
        private Integer permissionId; private Boolean editable; private String unavailableReason;
        private List<PermissionDataScopeCandidate> dataScopeCandidates = new ArrayList<>();
    }
    @Data public static class PermissionDataScopeCandidate {
        private DataScopeCode code; private String label;
        private List<OrganizationOption> organizationOptions = new ArrayList<>();
    }
    @Data public static class PermissionScopeAssignment {
        @NotNull private Integer permissionId; @NotNull private DataScopeCode dataScopeCode;
        @NotNull private List<Integer> organizationUnitIds = new ArrayList<>();
    }
    @Data public static class MatrixRequest {
        @NotNull @Min(0) private Integer expectedVersion;
        @NotNull private List<Integer> permissionIds = new ArrayList<>();
        @NotNull @Valid private List<PermissionScopeAssignment> permissionScopes = new ArrayList<>();
    }
    @Data public static class UpdateMatrixRequest extends MatrixRequest { @NotBlank @Size(max=500) private String reason; }
    @Data public static class DifferenceItem { private Integer permissionId; private String code; private String name; private PermissionSensitivityLevel sensitivityLevel; }
    @Data public static class PreviewResponse {
        private Integer roleId; private Integer expectedVersion; private List<DifferenceItem> addedPermissions = new ArrayList<>();
        private List<DifferenceItem> removedPermissions = new ArrayList<>(); private Integer affectedUserCount;
        private Integer affectedOrganizationCount; private Integer sessionRevocationCount; private List<String> warnings = new ArrayList<>();
        private List<PermissionScopeDifference> scopeDifferences = new ArrayList<>();
    }
    @Data public static class PermissionScopeDifference {
        private Integer permissionId; private String permissionCode; private String permissionName;
        private DataScopeCode beforeDataScopeCode; private DataScopeCode afterDataScopeCode;
        private List<String> beforeOrganizationNames = new ArrayList<>();
        private List<String> afterOrganizationNames = new ArrayList<>();
    }
    @Data public static class UpdateMatrixResponse {
        private Integer roleId; private Integer version; private List<Integer> permissionIds = new ArrayList<>();
        private Integer affectedUserCount; private Integer securityVersionUpdatedCount; private Integer sessionCleanupWarningCount;
        private List<PermissionScopeAssignment> permissionScopes = new ArrayList<>();
    }
}
