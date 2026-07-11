package com.autodealer.crm.ai.mapper;

import com.autodealer.crm.ai.model.TAiToolCall;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TAiToolCallMapper {
    int insert(TAiToolCall record);

    List<TAiToolCall> selectByRunId(@Param("runId") Long runId);

    int countByRunId(@Param("runId") Long runId);
}
