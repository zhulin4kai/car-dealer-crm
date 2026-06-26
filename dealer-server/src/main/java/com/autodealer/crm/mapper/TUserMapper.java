package com.autodealer.crm.mapper;
import com.autodealer.crm.commons.DataScope;
import com.autodealer.crm.dto.UserListQuery;
import com.autodealer.crm.model.TRole;
import com.autodealer.crm.model.TUser;
import org.apache.ibatis.annotations.Param;
import java.util.List;
public interface TUserMapper {
    int deleteByPrimaryKey(Integer id);
    int insert(TUser record);
    int insertSelective(TUser record);
    TUser selectByPrimaryKey(Integer id);
    int updateByPrimaryKeySelective(TUser record);
    int updateByPrimaryKey(TUser record);
    TUser selectByLoginAct(String username);
    @DataScope(tableAlias = "tu", tableField = "id")
    List<TUser> selectUserByPage(UserListQuery query);
    TUser selectAuthUserById(Integer id);
    int deleteByIds(List<Integer> ids);
    List<TUser> selectByOwner();
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
    int countAdminUsers();
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
