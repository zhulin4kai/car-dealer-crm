package com.autodealer.crm.mapper;

import com.autodealer.crm.model.TRole;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface TRoleMapper {
    int insert(TRole record);
    int insertSelective(TRole record);
    TRole selectByPrimaryKey(Integer id);
    int updateMutableByIdAndVersion(@Param("role") TRole role,
                                    @Param("expectedVersion") Integer expectedVersion);
    List<TRole> selectByUserId(Integer userId);
    List<TRole> selectAll();
    List<TRole> selectFiltered(@Param("keyword") String keyword, @Param("enabled") Boolean enabled);
    List<TRole> selectFilteredVisible(@Param("keyword") String keyword, @Param("enabled") Boolean enabled,
                                     @Param("authorizationLevel") Integer authorizationLevel,
                                     @Param("organizationUnitIds") List<Integer> organizationUnitIds);
    TRole selectByCode(String code);
    int countMembers(Integer roleId);
    int incrementVersionByExpected(@Param("roleId") Integer roleId,
                                   @Param("expectedVersion") Integer expectedVersion);
}
