package com.autodealer.crm.modules.sales.testdrive.application.api.port;

import com.autodealer.crm.modules.sales.testdrive.application.api.model.TTestDrive;
import com.autodealer.crm.modules.sales.testdrive.application.api.query.TestDriveQuery;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface TestDriveDataPort {
    int insert(TTestDrive testDrive);

    TTestDrive selectById(@Param("id") Long id);

    TTestDrive selectByIdForUpdate(@Param("id") Long id);

    List<TTestDrive> selectByQuery(@Param("query") TestDriveQuery query);

    int countOwnerScheduleConflict(@Param("ownerId") Integer ownerId,
                                   @Param("startTime") LocalDateTime startTime,
                                   @Param("endTime") LocalDateTime endTime,
                                   @Param("excludeId") Long excludeId);

    int updateScheduleIfCurrent(@Param("id") Long id,
                                @Param("expectedStatus") String expectedStatus,
                                @Param("targetStatus") String targetStatus,
                                @Param("vehicleId") Long vehicleId,
                                @Param("plannedStartTime") LocalDateTime plannedStartTime,
                                @Param("plannedEndTime") LocalDateTime plannedEndTime,
                                @Param("updateTime") LocalDateTime updateTime,
                                @Param("updateBy") Integer updateBy);

    int updateCancelIfCurrent(@Param("id") Long id,
                              @Param("expectedStatus") String expectedStatus,
                              @Param("targetStatus") String targetStatus,
                              @Param("cancelType") String cancelType,
                              @Param("cancelReason") String cancelReason,
                              @Param("updateTime") LocalDateTime updateTime,
                              @Param("updateBy") Integer updateBy);

    int updateCheckInIfCurrent(@Param("id") Long id,
                               @Param("expectedStatus") String expectedStatus,
                               @Param("arriveTime") LocalDateTime arriveTime,
                               @Param("customerConfirmMethod") String customerConfirmMethod,
                               @Param("updateTime") LocalDateTime updateTime,
                               @Param("updateBy") Integer updateBy);

    int updateCompleteIfCurrent(@Param("id") Long id,
                                @Param("expectedStatus") String expectedStatus,
                                @Param("actualStartTime") LocalDateTime actualStartTime,
                                @Param("actualEndTime") LocalDateTime actualEndTime,
                                @Param("safetyConfirmedAt") LocalDateTime safetyConfirmedAt,
                                @Param("result") String result,
                                @Param("customerFeedback") String customerFeedback,
                                @Param("nextAction") String nextAction,
                                @Param("updateTime") LocalDateTime updateTime,
                                @Param("updateBy") Integer updateBy);
}
