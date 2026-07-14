package com.autodealer.crm.modules.identity.persistence.mapper;

import com.autodealer.crm.modules.identity.application.api.dto.user.UserHistoryRows.ActionFacet;
import com.autodealer.crm.modules.identity.application.api.dto.user.UserHistoryRows.ProjectionQuery;
import com.autodealer.crm.modules.identity.application.api.dto.user.UserHistoryRows.ProjectionRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserHistoryProjectionMapper {
    long count(@Param("query") ProjectionQuery query);

    List<ProjectionRow> selectPage(@Param("query") ProjectionQuery query);

    List<ActionFacet> selectActionFacets(@Param("query") ProjectionQuery query);
}
