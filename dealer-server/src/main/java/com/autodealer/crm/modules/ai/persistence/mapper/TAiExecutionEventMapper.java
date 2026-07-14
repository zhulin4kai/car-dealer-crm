package com.autodealer.crm.modules.ai.persistence.mapper;

import com.autodealer.crm.modules.ai.persistence.model.TAiExecutionEvent;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TAiExecutionEventMapper {
    int insert(TAiExecutionEvent record);

    List<TAiExecutionEvent> selectByRunId(@Param("runId") Long runId);
}
