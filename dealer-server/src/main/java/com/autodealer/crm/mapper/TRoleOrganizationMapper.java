package com.autodealer.crm.mapper;

import com.autodealer.crm.model.TRoleOrganization;
import java.util.List;

public interface TRoleOrganizationMapper {
    List<TRoleOrganization> selectByRoleId(Integer roleId);
    int insert(TRoleOrganization value);
    int deleteByRoleId(Integer roleId);
}
