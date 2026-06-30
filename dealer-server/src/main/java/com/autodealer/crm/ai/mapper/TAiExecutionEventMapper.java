package com.autodealer.crm.ai.mapper;

import com.autodealer.crm.ai.model.TAiExecutionEvent;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TAiExecutionEventMapper {
    int insert(TAiExecutionEvent record);

    List<TAiExecutionEvent> selectByRunId(@Param("runId") Long runId);
}
