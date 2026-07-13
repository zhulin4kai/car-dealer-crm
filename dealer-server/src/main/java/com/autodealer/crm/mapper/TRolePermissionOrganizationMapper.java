package com.autodealer.crm.mapper;

import com.autodealer.crm.model.TRolePermissionOrganization;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface TRolePermissionOrganizationMapper {
    List<Integer> selectOrganizationIds(@Param("roleId") Integer roleId,
                                        @Param("permissionId") Integer permissionId);
    int insert(TRolePermissionOrganization value);
    int deleteByRoleId(Integer roleId);
}
