package com.autodealer.crm.modules.identity.persistence.mapper;

import com.autodealer.crm.modules.identity.persistence.model.TRolePermissionOrganization;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TRolePermissionOrganizationMapper {
    List<Integer> selectOrganizationIds(@Param("roleId") Integer roleId,
                                        @Param("permissionId") Integer permissionId);
    int insert(TRolePermissionOrganization value);
    int deleteByRoleId(Integer roleId);
}
