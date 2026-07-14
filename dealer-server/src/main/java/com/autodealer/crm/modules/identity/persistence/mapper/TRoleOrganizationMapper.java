package com.autodealer.crm.modules.identity.persistence.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.autodealer.crm.modules.identity.persistence.model.TRoleOrganization;
import java.util.List;

@Mapper
public interface TRoleOrganizationMapper {
    List<TRoleOrganization> selectByRoleId(Integer roleId);
    int insert(TRoleOrganization value);
    int deleteByRoleId(Integer roleId);
}
