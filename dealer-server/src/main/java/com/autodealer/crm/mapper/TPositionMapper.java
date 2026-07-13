package com.autodealer.crm.mapper;

import com.autodealer.crm.model.TPosition;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface TPositionMapper {
    TPosition selectByPrimaryKey(Integer positionId);

    TPosition selectByCode(String code);

    List<TPosition> selectAll();

    List<TPosition> selectManageable();

    int insert(TPosition position);

    int updateByIdAndVersion(@Param("position") TPosition position,
                             @Param("expectedVersion") Integer expectedVersion);

    int countEffectiveAssignments(@Param("positionId") Integer positionId,
                                  @Param("effectiveAt") LocalDateTime effectiveAt);
}
