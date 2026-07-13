package com.autodealer.crm.mapper;

import com.autodealer.crm.model.TAuthorizationHistory;

import java.util.List;
import java.time.LocalDateTime;
import com.autodealer.crm.dto.user.UserHistoryRows.AuthorizationRow;
import org.apache.ibatis.annotations.Param;

public interface TAuthorizationHistoryMapper {
    int insert(TAuthorizationHistory history);

    List<TAuthorizationHistory> selectByTargetUserId(Integer targetUserId);

    List<TAuthorizationHistory> selectOrganizationHistoryByEmployeeId(String employeeId);

    List<AuthorizationRow> selectUserHistoryRows(@Param("userId") Integer userId,
                                                 @Param("startTime") LocalDateTime startTime,
                                                 @Param("endTime") LocalDateTime endTime);
}
