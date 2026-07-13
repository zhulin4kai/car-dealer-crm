package com.autodealer.crm.mapper;

import com.autodealer.crm.model.TEmployee;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface TEmployeeMapper {
    TEmployee selectByPrimaryKey(Integer employeeId);

    TEmployee selectByUserId(Integer userId);
    TEmployee selectByUserIdForUpdate(Integer userId);

    int countLoginEligibleByUserId(@Param("userId") Integer userId,
                                   @Param("effectiveAt") LocalDateTime effectiveAt);

    TEmployee selectByEmployeeNo(String employeeNo);
    TEmployee selectByPhoneExcludeUserId(@Param("phone") String phone,@Param("userId") Integer userId);
    TEmployee selectByEmailExcludeUserId(@Param("email") String email,@Param("userId") Integer userId);

    List<TEmployee> selectAll();

    List<TEmployee> selectEffectiveByOrganizationUnitId(
            @Param("organizationUnitId") Integer organizationUnitId,
            @Param("effectiveAt") LocalDateTime effectiveAt);

    List<TEmployee> selectEligibleManagerCandidates(
            @Param("excludedEmployeeId") Integer excludedEmployeeId,
            @Param("effectiveAt") LocalDateTime effectiveAt,
            @Param("organizationUnitIds") List<Integer> organizationUnitIds);

    List<Integer> selectUserIdsByOrganizationUnitIds(@Param("organizationUnitIds") List<Integer> organizationUnitIds,
                                                     @Param("effectiveAt") LocalDateTime effectiveAt);

    int insert(TEmployee employee);

    int updateByIdAndVersion(@Param("employee") TEmployee employee,
                             @Param("expectedVersion") Integer expectedVersion);

    int incrementVersionByExpected(@Param("employeeId") Integer employeeId,
                                   @Param("expectedVersion") Integer expectedVersion,
                                   @Param("editTime") LocalDateTime editTime,
                                   @Param("editBy") Integer editBy);

    int activatePendingByUserId(@Param("userId") Integer userId,
                                @Param("activatedAt") LocalDateTime activatedAt);

    int markContactVerifiedByProfileVersion(@Param("employeeId") Integer employeeId,
                                            @Param("expectedProfileVersion") Integer expectedProfileVersion,
                                            @Param("channel") String channel,
                                            @Param("verifiedAt") LocalDateTime verifiedAt);

    int updateProfileByVersion(@Param("employee") TEmployee employee,
                               @Param("expectedProfileVersion") Integer expectedProfileVersion);
}
