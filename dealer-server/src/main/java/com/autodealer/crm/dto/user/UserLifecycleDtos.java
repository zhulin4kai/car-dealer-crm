package com.autodealer.crm.dto.user;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.OffsetDateTime;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Task 20 人员生命周期命令与只读投影。 */
public final class UserLifecycleDtos {
    private UserLifecycleDtos() {}

    public enum DirectResourceType { ACTIVITY, CLUE, CUSTOMER, OPPORTUNITY, FOLLOW_TASK, TEST_DRIVE }
    public enum AccountActivationMode { INVITE, RECOVER }

    @Data public static class Transition {
        private String action; private String fromStatus; private String toStatus; private String label;
        private String disabledReason;
    }
    @Data public static class Candidate {
        private Integer id; private String label; private String secondaryLabel;
    }
    @Data public static class HandoverCandidate extends Candidate {
        private boolean eligible; private String qualificationCode; private String qualificationName;
        private String unavailableReason;
    }
    @Data public static class AssignmentSummary {
        private String organizationCode; private String organizationName;
        private String positionCode; private String positionName;
        private String managerEmployeeNo; private String managerName;
        private OffsetDateTime effectiveFrom;
    }
    @Data public static class Context {
        private Integer userId; private Integer employeeId; private String employmentStatus;
        private Integer employeeVersion; private AssignmentSummary currentAssignment;
        private int activeRoleCount; private int activePersonalPermissionCount; private int activeSessionCount;
        private int additionalAssignmentCount; private int reportingRelationCount;
        private List<Candidate> organizationCandidates = new ArrayList<>();
        private List<Candidate> positionCandidates = new ArrayList<>();
        private List<Candidate> managerCandidates = new ArrayList<>();
        private boolean managerRequired = true; private String managerOptionalReason;
        private List<Candidate> handoverCandidates = new ArrayList<>();
        private List<String> allowedActions = new ArrayList<>();
        private Map<String,String> unavailableReasons = new LinkedHashMap<>();
        private List<Transition> statusTransitions = new ArrayList<>();
    }

    @Data public static class AssignmentCommand {
        @NotNull @Min(0) private Integer employeeVersion;
        @NotNull private Integer organizationUnitId;
        @NotNull private Integer positionId;
        private Integer managerEmployeeId;
        @NotNull private OffsetDateTime effectiveFrom;
        @NotBlank @Size(max=500) private String reason;
    }
    @Data public static class DeparturePrecheckRequest {
        @NotNull @Min(0) private Integer employeeVersion;
        @NotBlank @Size(max=500) private String reason;
    }
    @Data public static class SnapshotCommand {
        @NotNull @Min(0) private Integer employeeVersion;
        @NotBlank private String snapshotToken;
        @NotBlank @Size(max=500) private String reason;
    }
    @Data public static class StartDepartureRequest extends SnapshotCommand {}
    @Data public static class CompleteDepartureRequest extends SnapshotCommand {}
    @Data public static class TransferSelection {
        @NotNull private DirectResourceType resourceType;
        @NotNull private Integer targetEmployeeId;
    }
    @Data public static class ConfirmHandoverRequest extends SnapshotCommand {
        @Valid @NotNull @Size(min=1,max=6) private List<TransferSelection> transfers = new ArrayList<>();
    }
    @Data public static class RehireRequest extends AssignmentCommand {
        @NotNull private AccountActivationMode accountActivationMode;
    }

    @Data public static class ResponsibilityConflict {
        private String conflictCode; private String conflictName; private int count; private String reason;
    }
    @Data public static class ResponsibilitySummary {
        private String resourceType; private String resourceName; private String transferMode;
        private int count; private int transferableCount; private int blockedCount;
        private String statusCode; private String statusName; private boolean blocking;
        private List<String> blockingReasons = new ArrayList<>();
        private List<HandoverCandidate> targetCandidates = new ArrayList<>();
        private List<ResponsibilityConflict> conflicts = new ArrayList<>();
    }
    @Data public static class DeparturePrecheck {
        private String snapshotToken; private OffsetDateTime generatedAt; private OffsetDateTime expiresAt;
        private Integer userId; private String employmentStatus; private Integer employeeVersion;
        private List<ResponsibilitySummary> responsibilities = new ArrayList<>();
        private int activeRoleCount; private int activePersonalPermissionCount; private int activeSessionCount;
        private int activeAssignmentCount; private int activeReportingCount;
        private boolean handoverRequired; private boolean handoverCompleted; private boolean readyToComplete;
        private List<String> blockingReasons = new ArrayList<>();
        private List<String> allowedActions = new ArrayList<>();
        private Map<String,String> unavailableReasons = new LinkedHashMap<>();
        private List<Transition> statusTransitions = new ArrayList<>();
    }
    @Data public static class DomainResult {
        private String domainCode; private String domainName; private int expectedCount; private int transferredCount;
        private String resultCode; private String resultName;
    }
    @Data public static class HandoverResult {
        private String operationId; private boolean success; private String resultCode; private String resultName;
        private Integer employeeVersion; private List<DomainResult> domainResults = new ArrayList<>();
    }
    @Data public static class RehireResult {
        private Context context; private int restoredLegacyAuthorizationCount; private String credentialDeliveryStatus;
    }

    /** Mapper 使用的最小责任快照；只含并发谓词和试驾时间冲突字段。 */
    @Data public static class ResponsibilityRow {
        private Long id; private Integer ownerId; private String status; private Integer state;
        private Integer version; private LocalDateTime plannedStartTime; private LocalDateTime plannedEndTime;
    }
    @Data public static class LifecycleEvent {
        private String operationId; private String requestId; private String action;
        private Integer userId; private Integer employeeId; private String beforeValue; private String afterValue;
        private String reason; private Integer operatorId; private OffsetDateTime occurredTime;
    }
    @Data public static class SnapshotFact {
        private Long id; private String tokenDigest; private Integer userId; private Integer employeeId;
        private Integer employeeVersion; private String reasonDigest; private String factDigest;
        private LocalDateTime expiresAt; private LocalDateTime consumedAt; private Integer version; private LocalDateTime createTime;
    }
}
