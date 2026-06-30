package com.autodealer.crm.ai.mapper;

import com.autodealer.crm.ai.model.TAiApproval;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TAiApprovalMapper {
    int insert(TAiApproval record);

    List<TAiApproval> selectByRunId(@Param("runId") Long runId);
}
