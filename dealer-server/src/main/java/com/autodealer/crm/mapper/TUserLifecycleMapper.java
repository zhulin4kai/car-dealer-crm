package com.autodealer.crm.mapper;

import com.autodealer.crm.dto.user.UserLifecycleDtos.HandoverCandidate;
import com.autodealer.crm.dto.user.UserLifecycleDtos.LifecycleEvent;
import com.autodealer.crm.dto.user.UserLifecycleDtos.ResponsibilityRow;
import com.autodealer.crm.dto.user.UserLifecycleDtos.SnapshotFact;
import com.autodealer.crm.model.TEmployee;
import com.autodealer.crm.model.TUser;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface TUserLifecycleMapper {
    TEmployee lockEmployeeByUserId(Integer userId);
    TUser lockUserById(Integer userId);
    int transitionEmployee(@Param("employeeId") Integer employeeId,@Param("expectedVersion") Integer expectedVersion,
                           @Param("fromStatus") String fromStatus,@Param("toStatus") String toStatus,
                           @Param("effectiveAt") LocalDateTime effectiveAt,@Param("operatorId") Integer operatorId,
                           @Param("setHireDate") boolean setHireDate,@Param("setLeaveDate") boolean setLeaveDate);

    List<ResponsibilityRow> selectActivities(@Param("ownerId") Integer ownerId,@Param("lockRows") boolean lockRows);
    List<ResponsibilityRow> selectClues(@Param("ownerId") Integer ownerId,@Param("lockRows") boolean lockRows);
    List<ResponsibilityRow> selectCustomers(@Param("ownerId") Integer ownerId,@Param("lockRows") boolean lockRows);
    List<ResponsibilityRow> selectOpportunities(@Param("ownerId") Integer ownerId,@Param("lockRows") boolean lockRows);
    List<ResponsibilityRow> selectFollowTasks(@Param("ownerId") Integer ownerId,@Param("lockRows") boolean lockRows);
    List<ResponsibilityRow> selectTestDrives(@Param("ownerId") Integer ownerId,@Param("lockRows") boolean lockRows);

    int transferActivity(@Param("id") Long id,@Param("fromUserId") Integer fromUserId,@Param("toUserId") Integer toUserId,@Param("status") String status);
    int transferClue(@Param("id") Long id,@Param("fromUserId") Integer fromUserId,@Param("toUserId") Integer toUserId,@Param("state") Integer state);
    int transferCustomer(@Param("id") Long id,@Param("fromUserId") Integer fromUserId,@Param("toUserId") Integer toUserId,@Param("status") String status);
    int transferOpportunity(@Param("id") Long id,@Param("fromUserId") Integer fromUserId,@Param("toUserId") Integer toUserId,@Param("stage") String stage,@Param("version") Integer version);
    int transferFollowTask(@Param("id") Long id,@Param("fromUserId") Integer fromUserId,@Param("toUserId") Integer toUserId,@Param("status") String status,@Param("version") Integer version);
    int transferTestDrive(@Param("id") Long id,@Param("fromUserId") Integer fromUserId,@Param("toUserId") Integer toUserId,@Param("status") String status,@Param("version") Integer version);

    int countTargetScheduleConflicts(@Param("targetUserId") Integer targetUserId,@Param("excludedId") Long excludedId,
                                     @Param("startTime") LocalDateTime startTime,@Param("endTime") LocalDateTime endTime);
    int countActiveQuotesByOwner(Integer ownerId);
    int countActiveTransactionsByOwner(Integer ownerId);
    int countActiveRoles(@Param("userId") Integer userId,@Param("at") LocalDateTime at);
    int countActivePersonalPermissions(@Param("userId") Integer userId,@Param("at") LocalDateTime at);
    int countActiveSessions(@Param("userId") Integer userId,@Param("at") LocalDateTime at);
    int countActiveAssignments(@Param("employeeId") Integer employeeId,@Param("at") LocalDateTime at);
    int countAdditionalAssignments(@Param("employeeId") Integer employeeId,@Param("at") LocalDateTime at);
    int countActiveReporting(@Param("employeeId") Integer employeeId,@Param("at") LocalDateTime at);
    int countActiveSubordinates(@Param("employeeId") Integer employeeId,@Param("at") LocalDateTime at);
    int countCurrentAndFutureSubordinates(@Param("employeeId") Integer employeeId,@Param("at") LocalDateTime at);
    int countCurrentAndFutureRoles(@Param("userId") Integer userId,@Param("at") LocalDateTime at);
    int countCurrentAndFuturePermissions(@Param("userId") Integer userId,@Param("at") LocalDateTime at);
    int countEnabledLedOrganizations(Integer employeeId);
    List<String> selectLifecycleFacts(@Param("userId") Integer userId,@Param("employeeId") Integer employeeId,@Param("at") LocalDateTime at);

    List<HandoverCandidate> selectQualifiedCandidates(@Param("excludedUserId") Integer excludedUserId,
                                                       @Param("permissionCodes") List<String> permissionCodes,
                                                       @Param("at") LocalDateTime at);
    int insertEvent(LifecycleEvent event);
    int insertSnapshot(SnapshotFact snapshot);
    SnapshotFact lockSnapshotByDigest(String tokenDigest);
    int consumeSnapshot(@Param("id") Long id,@Param("expectedVersion") Integer expectedVersion,@Param("consumedAt") LocalDateTime consumedAt);
}
