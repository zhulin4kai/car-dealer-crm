package com.autodealer.crm.mapper;

import com.autodealer.crm.model.TAuthorizationHistory;

import java.util.List;

public interface TAuthorizationHistoryMapper {
    int insert(TAuthorizationHistory history);

    List<TAuthorizationHistory> selectByTargetUserId(Integer targetUserId);

    List<TAuthorizationHistory> selectOrganizationHistoryByEmployeeId(String employeeId);

}
