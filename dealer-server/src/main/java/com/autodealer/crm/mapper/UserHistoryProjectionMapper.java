package com.autodealer.crm.mapper;

import com.autodealer.crm.dto.user.UserHistoryRows.ActionFacet;
import com.autodealer.crm.dto.user.UserHistoryRows.ProjectionQuery;
import com.autodealer.crm.dto.user.UserHistoryRows.ProjectionRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserHistoryProjectionMapper {
    long count(@Param("query") ProjectionQuery query);

    List<ProjectionRow> selectPage(@Param("query") ProjectionQuery query);

    List<ActionFacet> selectActionFacets(@Param("query") ProjectionQuery query);
}
