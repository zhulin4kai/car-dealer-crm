package com.autodealer.crm.mapper;

import com.autodealer.crm.model.TEmployeeReporting;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface TEmployeeReportingMapper {
    TEmployeeReporting selectByPrimaryKey(Integer reportingId);

    TEmployeeReporting selectCurrentDirectBySubordinateId(
            @Param("subordinateEmployeeId") Integer subordinateEmployeeId,
            @Param("effectiveAt") LocalDateTime effectiveAt);

    List<TEmployeeReporting> selectEffectiveManagers(@Param("subordinateEmployeeId") Integer subordinateEmployeeId,
                                                     @Param("effectiveAt") LocalDateTime effectiveAt);
    List<TEmployeeReporting> selectEffectiveSubordinates(@Param("managerEmployeeId") Integer managerEmployeeId,
                                                         @Param("effectiveAt") LocalDateTime effectiveAt);

    List<TEmployeeReporting> selectHistoryBySubordinateId(Integer subordinateEmployeeId);

    List<TEmployeeReporting> selectReplaceableBySubordinateId(
            @Param("subordinateEmployeeId") Integer subordinateEmployeeId,
            @Param("effectiveAt") LocalDateTime effectiveAt);

    List<TEmployeeReporting> selectReplaceableDirectBySubordinateId(
            @Param("subordinateEmployeeId") Integer subordinateEmployeeId,@Param("effectiveAt") LocalDateTime effectiveAt);

    List<TEmployeeReporting> selectCurrentAndFutureActingBySubordinateId(
            @Param("subordinateEmployeeId") Integer subordinateEmployeeId,
            @Param("effectiveAt") LocalDateTime effectiveAt);

    List<TEmployeeReporting> selectOverlappingManagers(
            @Param("subordinateEmployeeId") Integer subordinateEmployeeId,
            @Param("effectiveFrom") LocalDateTime effectiveFrom,
            @Param("effectiveTo") LocalDateTime effectiveTo);

    int expireElapsedMarkers(@Param("subordinateEmployeeId") Integer subordinateEmployeeId,
                             @Param("effectiveAt") LocalDateTime effectiveAt,
                             @Param("editBy") Integer editBy);

    int expireElapsedActingMarkers(@Param("subordinateEmployeeId") Integer subordinateEmployeeId,
                                   @Param("effectiveAt") LocalDateTime effectiveAt,
                                   @Param("editBy") Integer editBy);

    int insert(TEmployeeReporting reporting);

    int endByIdAndVersion(@Param("reportingId") Integer reportingId,
                          @Param("expectedVersion") Integer expectedVersion,
                          @Param("effectiveTo") LocalDateTime effectiveTo,
                          @Param("editTime") LocalDateTime editTime,
                          @Param("editBy") Integer editBy);

    int cancelFutureByIdAndVersion(@Param("reportingId") Integer reportingId,
                                   @Param("expectedVersion") Integer expectedVersion,
                                   @Param("effectiveAt") LocalDateTime effectiveAt,
                                   @Param("editTime") LocalDateTime editTime,
                                   @Param("editBy") Integer editBy);
}
