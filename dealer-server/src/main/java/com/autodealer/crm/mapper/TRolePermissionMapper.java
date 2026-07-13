package com.autodealer.crm.mapper;

import com.autodealer.crm.model.TRolePermission;

import java.util.List;

public interface TRolePermissionMapper {
    int insert(TRolePermission rolePermission);

    List<TRolePermission> selectByRoleId(Integer roleId);
    int deleteByRoleId(Integer roleId);
    int updateDataScopeByRoleId(@org.apache.ibatis.annotations.Param("roleId") Integer roleId,
                                @org.apache.ibatis.annotations.Param("dataScopeCode") com.autodealer.crm.enums.DataScopeCode dataScopeCode);
}
