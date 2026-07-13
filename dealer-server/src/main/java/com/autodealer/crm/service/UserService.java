package com.autodealer.crm.service;
import com.autodealer.crm.dto.*;
import com.autodealer.crm.model.TUser;
import com.github.pagehelper.PageInfo;
import org.springframework.security.core.userdetails.UserDetailsService;
import java.util.List;
public interface UserService extends UserDetailsService {
    PageInfo<UserDetailResponse> getUserByPage(UserListQuery query);
    TUser getLoginUserById(Integer id);
    UserDetailResponse getUserById(Integer id);
    UserDetailResponse createUser(CreateUserRequest request);
    UserDetailResponse updateUser(UpdateUserRequest request);
    void disableUser(Integer id);
    void enableUser(Integer id);
    void lockUser(Integer id);
    void unlockUser(Integer id);
    void batchDisableUsers(List<Integer> ids);
    void assignRoles(AssignUserRolesRequest request);
    void changePassword(ChangePasswordRequest request);
    void revokeAuthentication(Integer userId);
    HandoverUserResponsibilitiesResponse handoverResponsibilities(Integer sourceUserId,
                                                                  HandoverUserResponsibilitiesRequest request);
    List<TUser> getOwnerList();
    List<OwnerCandidate> getOwnerCandidates(String permissionCode, String qualificationContext);
    UserDetailResponse toDetailResponse(TUser tUser);
}
