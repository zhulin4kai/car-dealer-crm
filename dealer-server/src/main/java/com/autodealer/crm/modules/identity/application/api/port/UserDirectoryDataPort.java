package com.autodealer.crm.modules.identity.application.api.port;

import com.autodealer.crm.modules.identity.application.api.security.DataScope;
import com.autodealer.crm.modules.identity.application.api.dto.UserListQuery;
import com.autodealer.crm.modules.identity.persistence.model.TRole;
import com.autodealer.crm.modules.identity.application.api.model.TUser;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import java.time.LocalDateTime;
import com.autodealer.crm.modules.identity.application.api.dto.user.ManagedUserDtos.FilterOption;
import com.autodealer.crm.modules.identity.application.api.dto.user.ManagedUserDtos.RoleNameRow;
import com.autodealer.crm.modules.identity.application.api.dto.user.ManagedUserDtos.Summary;

public interface UserDirectoryDataPort {
    int deleteByPrimaryKey(Integer id);
    int insert(TUser record);
    int insertSelective(TUser record);
    TUser selectByPrimaryKey(Integer id);
    TUser selectByPrimaryKeyForUpdate(Integer id);
    int updateByPrimaryKeySelective(TUser record);
    int updateByPrimaryKey(TUser record);
    TUser selectByLoginAct(String username);
    @DataScope(tableAlias = "tu", tableField = "id")
    List<TUser> selectUserByPage(UserListQuery query);
    List<Summary> selectManagedUserPage(UserListQuery query);
    List<RoleNameRow> selectRoleNamesByUserIds(@Param("userIds") List<Integer> userIds,
                                               @Param("effectiveAt") LocalDateTime effectiveAt);
    List<FilterOption> selectVisibleOrganizationOptions(UserListQuery query);
    List<FilterOption> selectVisiblePositionOptions(UserListQuery query);
    List<FilterOption> selectVisibleManagerOptions(UserListQuery query);
    List<FilterOption> selectVisibleRoleOptions(UserListQuery query);
    TUser selectAuthUserById(Integer id);
    int deleteByIds(List<Integer> ids);
    List<TUser> selectByOwner();
    List<TUser> selectEligibleOwners(@Param("visibleUserIds") List<Integer> visibleUserIds,
                                     @Param("permissionCode") String permissionCode);
    TUser selectByPhone(String phone);
    TUser selectByEmail(String email);
    TUser selectByLoginActExcludeId(@Param("loginAct") String loginAct, @Param("excludeId") Integer excludeId);
    TUser selectByPhoneExcludeId(@Param("phone") String phone, @Param("excludeId") Integer excludeId);
    TUser selectByEmailExcludeId(@Param("email") String email, @Param("excludeId") Integer excludeId);
    int disableById(Integer id);
    int disableByIds(List<Integer> ids);
    int enableById(Integer id);
    int lockById(Integer id);
    int unlockById(Integer id);
    int updatePassword(@Param("id") Integer id, @Param("encodedPassword") String encodedPassword);
    int incrementAuthVersion(Integer id);
    int incrementAuthorizationVersionsByExpected(@Param("id") Integer id,
                                                 @Param("expectedVersion") Integer expectedVersion);
    int incrementAuthVersionByIds(List<Integer> ids);
    int incrementSessionRevisionByExpected(@Param("id") Integer id,
                                           @Param("expectedRevision") Long expectedRevision);
    int incrementSessionRevision(Integer id);
    int recordLoginFailureByExpected(@Param("id") Integer id,
                                     @Param("expectedFailedCount") Integer expectedFailedCount,
                                     @Param("autoLockedUntil") LocalDateTime autoLockedUntil,
                                     @Param("lockAccount") boolean lockAccount);
    int recordLoginSuccess(@Param("id") Integer id, @Param("loginTime") LocalDateTime loginTime);
    int activateCredentialState(@Param("id") Integer id,
                                @Param("expectedVersion") Integer expectedVersion,
                                @Param("encodedPassword") String encodedPassword,
                                @Param("mustChangePassword") Boolean mustChangePassword,
                                @Param("passwordExpiresAt") LocalDateTime passwordExpiresAt);
    int updatePasswordCredentialState(@Param("id") Integer id,
                                      @Param("expectedVersion") Integer expectedVersion,
                                      @Param("encodedPassword") String encodedPassword,
                                      @Param("mustChangePassword") Boolean mustChangePassword,
                                      @Param("passwordExpiresAt") LocalDateTime passwordExpiresAt);
    int recoverProtectedAccountByExpected(@Param("id") Integer id,
                                          @Param("expectedVersion") Integer expectedVersion,
                                          @Param("encodedPassword") String encodedPassword,
                                          @Param("passwordExpiresAt") LocalDateTime passwordExpiresAt);
    int updateMustChangePassword(@Param("id") Integer id,
                                 @Param("mustChangePassword") Boolean mustChangePassword);
    int updateMustChangePasswordByExpected(@Param("id") Integer id,@Param("expectedVersion") Integer expectedVersion,
                                           @Param("mustChangePassword") Boolean mustChangePassword);
    int incrementAccountVersionIfInvited(@Param("id") Integer id,@Param("expectedVersion") Integer expectedVersion);
    int updateManualLockByExpected(@Param("id") Integer id,
                                   @Param("expectedVersion") Integer expectedVersion,
                                   @Param("manualLocked") Boolean manualLocked,
                                   @Param("reason") String reason,
                                   @Param("operatorId") Integer operatorId,
                                   @Param("lockedAt") LocalDateTime lockedAt);
    int updateProfileProjection(@Param("id") Integer id, @Param("name") String name,
                                @Param("phone") String phone, @Param("email") String email,
                                @Param("editBy") Integer editBy);
    int updateSystemProfileByVersion(@Param("id") Integer id,@Param("expectedProfileVersion") Integer expectedProfileVersion,
                                     @Param("name") String name,@Param("phone") String phone,
                                     @Param("email") String email,@Param("avatarUrl") String avatarUrl,
                                     @Param("editBy") Integer editBy);
    int updateAccountStatusByExpected(@Param("id") Integer id,@Param("expectedVersion") Integer expectedVersion,
                                      @Param("accountStatus") String accountStatus,@Param("enabled") Boolean enabled,
                                      @Param("operatorId") Integer operatorId);
    int updateLoginActByExpected(@Param("id") Integer id,@Param("expectedVersion") Integer expectedVersion,
                                 @Param("loginAct") String loginAct,@Param("operatorId") Integer operatorId);
    int updateSecurityExpirationByExpected(@Param("id") Integer id,@Param("expectedVersion") Integer expectedVersion,
                                            @Param("accountNoExpired") Integer accountNoExpired,
                                            @Param("accountExpiresAt") LocalDateTime accountExpiresAt,
                                            @Param("credentialsNoExpired") Integer credentialsNoExpired,
                                            @Param("passwordExpiresAt") LocalDateTime passwordExpiresAt,
                                            @Param("operatorId") Integer operatorId);
    int countAdminUsers();
    int countPendingAdminUsers();
    List<TUser> selectRecoverableAdminCandidatesForUpdate();
    int countAvailableAdminUsersExcluding(@Param("userId") Integer userId);
    int countQualifiedSecurityAdministrator(@Param("userId") Integer userId);
    int recoverOrdinaryAdminSecurityByExpected(@Param("id") Integer id,@Param("expectedVersion") Integer expectedVersion,
                                               @Param("operatorId") Integer operatorId,@Param("editTime") LocalDateTime editTime);
    int recoverInvitedAdminSecurityByExpected(@Param("id") Integer id,@Param("expectedVersion") Integer expectedVersion,
                                              @Param("operatorId") Integer operatorId,@Param("editTime") LocalDateTime editTime);
    int countBusinessReferences(Integer userId);
    List<Integer> selectOwnedActivityIds(Integer userId);
    List<Integer> selectOwnedClueIds(Integer userId);
    List<Integer> selectOwnedCustomerIds(Integer userId);
    int transferOwnedActivities(@Param("fromUserId") Integer fromUserId,
                                @Param("toUserId") Integer toUserId,
                                @Param("operatorId") Integer operatorId);
    int transferOwnedClues(@Param("fromUserId") Integer fromUserId,
                           @Param("toUserId") Integer toUserId,
                           @Param("operatorId") Integer operatorId);
    int transferOwnedCustomers(@Param("fromUserId") Integer fromUserId,
                               @Param("toUserId") Integer toUserId,
                               @Param("operatorId") Integer operatorId);
    List<TRole> selectRolesByUserId(Integer userId);
    int deleteUserRoles(Integer userId);
    int insertUserRoles(@Param("userId") Integer userId, @Param("roleIds") List<Integer> roleIds);
}
