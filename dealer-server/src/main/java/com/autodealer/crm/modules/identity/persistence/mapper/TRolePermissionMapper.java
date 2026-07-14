package com.autodealer.crm.modules.identity.persistence.mapper;

import com.autodealer.crm.modules.identity.application.api.enums.DataScopeCode;
import org.apache.ibatis.annotations.Mapper;

import com.autodealer.crm.modules.identity.persistence.model.TRolePermission;

import java.util.List;

@Mapper
public interface TRolePermissionMapper {
    int insert(TRolePermission rolePermission);

    List<TRolePermission> selectByRoleId(Integer roleId);
    int deleteByRoleId(Integer roleId);
    int updateDataScopeByRoleId(@org.apache.ibatis.annotations.Param("roleId") Integer roleId,
                                @org.apache.ibatis.annotations.Param("dataScopeCode") com.autodealer.crm.modules.identity.application.api.enums.DataScopeCode dataScopeCode);
}
