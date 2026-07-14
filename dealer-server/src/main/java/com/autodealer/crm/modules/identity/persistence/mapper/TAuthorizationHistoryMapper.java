package com.autodealer.crm.modules.identity.persistence.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.autodealer.crm.modules.identity.persistence.model.TAuthorizationHistory;

import java.util.List;

@Mapper
public interface TAuthorizationHistoryMapper {
    int insert(TAuthorizationHistory history);

    List<TAuthorizationHistory> selectByTargetUserId(Integer targetUserId);

    List<TAuthorizationHistory> selectOrganizationHistoryByEmployeeId(String employeeId);

}
