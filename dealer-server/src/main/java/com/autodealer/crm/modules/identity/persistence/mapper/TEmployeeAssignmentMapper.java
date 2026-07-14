package com.autodealer.crm.modules.identity.persistence.mapper;

import com.autodealer.crm.modules.identity.persistence.model.TEmployeeAssignment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface TEmployeeAssignmentMapper {
    TEmployeeAssignment selectByPrimaryKey(Integer assignmentId);

    TEmployeeAssignment selectCurrentPrimaryByEmployeeId(@Param("employeeId") Integer employeeId,
                                                         @Param("effectiveAt") LocalDateTime effectiveAt);

    List<TEmployeeAssignment> selectEffectiveByEmployeeId(@Param("employeeId") Integer employeeId,
                                                          @Param("effectiveAt") LocalDateTime effectiveAt);

    List<TEmployeeAssignment> selectHistoryByEmployeeId(Integer employeeId);

    List<TEmployeeAssignment> selectReplaceableByEmployeeId(
            @Param("employeeId") Integer employeeId,
            @Param("effectiveAt") LocalDateTime effectiveAt);

    List<TEmployeeAssignment> selectReplaceablePrimaryByEmployeeId(
            @Param("employeeId") Integer employeeId,@Param("effectiveAt") LocalDateTime effectiveAt);

    int expireElapsedMarkers(@Param("employeeId") Integer employeeId,
                             @Param("effectiveAt") LocalDateTime effectiveAt,
                             @Param("editBy") Integer editBy);

    int insert(TEmployeeAssignment assignment);

    int endByIdAndVersion(@Param("assignmentId") Integer assignmentId,
                          @Param("expectedVersion") Integer expectedVersion,
                          @Param("effectiveTo") LocalDateTime effectiveTo,
                          @Param("editTime") LocalDateTime editTime,
                          @Param("editBy") Integer editBy);

    int cancelFutureByIdAndVersion(@Param("assignmentId") Integer assignmentId,
                                   @Param("expectedVersion") Integer expectedVersion,
                                   @Param("effectiveAt") LocalDateTime effectiveAt,
                                   @Param("editTime") LocalDateTime editTime,
                                   @Param("editBy") Integer editBy);
}
