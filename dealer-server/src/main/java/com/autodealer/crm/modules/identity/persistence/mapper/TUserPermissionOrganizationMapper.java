package com.autodealer.crm.modules.identity.persistence.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.autodealer.crm.modules.identity.persistence.model.TUserPermissionOrganization;

import java.util.List;

@Mapper
public interface TUserPermissionOrganizationMapper {
    List<Integer> selectOrganizationIds(Long userPermissionId);
    int insert(TUserPermissionOrganization value);
    int deleteByUserPermissionId(Long userPermissionId);
}
