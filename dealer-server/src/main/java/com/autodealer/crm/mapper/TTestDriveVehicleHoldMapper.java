package com.autodealer.crm.mapper;

import com.autodealer.crm.model.TTestDriveVehicleHold;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

@Mapper
public interface TTestDriveVehicleHoldMapper {
    int insert(TTestDriveVehicleHold hold);

    int countActiveConflict(@Param("vehicleId") Long vehicleId,
                            @Param("startTime") LocalDateTime startTime,
                            @Param("endTime") LocalDateTime endTime,
                            @Param("excludeTestDriveId") Long excludeTestDriveId);

    int releaseActiveByTestDriveId(@Param("testDriveId") Long testDriveId,
                                   @Param("reason") String reason,
                                   @Param("releaseTime") LocalDateTime releaseTime,
                                   @Param("updateBy") Integer updateBy);
}
