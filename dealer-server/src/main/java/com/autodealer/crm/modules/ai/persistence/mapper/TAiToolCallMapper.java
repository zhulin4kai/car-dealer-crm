package com.autodealer.crm.modules.ai.persistence.mapper;

import com.autodealer.crm.modules.ai.persistence.model.TAiToolCall;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TAiToolCallMapper {
    int insert(TAiToolCall record);

    List<TAiToolCall> selectByRunId(@Param("runId") Long runId);

    int countByRunId(@Param("runId") Long runId);
}
