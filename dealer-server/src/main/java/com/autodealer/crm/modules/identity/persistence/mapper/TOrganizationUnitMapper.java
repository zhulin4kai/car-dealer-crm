package com.autodealer.crm.modules.identity.persistence.mapper;

import com.autodealer.crm.modules.identity.persistence.model.TOrganizationUnit;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface TOrganizationUnitMapper {
    TOrganizationUnit selectByPrimaryKey(Integer organizationUnitId);

    TOrganizationUnit selectByCode(String code);

    List<TOrganizationUnit> selectAll();

    List<TOrganizationUnit> selectAllIncludingPlaceholders();

    /** 返回启用、非占位且无父级的根组织事实。历史禁用根不参与唯一启用根和 bootstrap 判断。 */
    List<TOrganizationUnit> selectRoots();
    int countInitializedRootOrganizations();

    List<TOrganizationUnit> selectByParentId(Integer parentId);

    List<Integer> selectDescendantIds(Integer organizationUnitId);

    List<TOrganizationUnit> selectByIds(@Param("organizationUnitIds") List<Integer> organizationUnitIds);

    int insert(TOrganizationUnit organizationUnit);

    int updateByIdAndVersion(@Param("organizationUnit") TOrganizationUnit organizationUnit,
                             @Param("expectedVersion") Integer expectedVersion);

    int assignInitialRootLeader(@Param("organizationUnitId") Integer organizationUnitId,
                                @Param("leaderEmployeeId") Integer leaderEmployeeId,
                                @Param("expectedVersion") Integer expectedVersion,
                                @Param("operatorId") Integer operatorId,
                                @Param("editTime") LocalDateTime editTime);

    int countEffectiveEmployees(@Param("organizationUnitId") Integer organizationUnitId,
                                @Param("effectiveAt") LocalDateTime effectiveAt);
}
