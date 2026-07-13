package com.autodealer.crm.mapper;

import com.autodealer.crm.model.TUserPermissionOrganization;

import java.util.List;

public interface TUserPermissionOrganizationMapper {
    List<Integer> selectOrganizationIds(Long userPermissionId);
    int insert(TUserPermissionOrganization value);
    int deleteByUserPermissionId(Long userPermissionId);
}
