package com.autodealer.crm.modules.ai.persistence.mapper;

import com.autodealer.crm.modules.ai.persistence.model.TAiApproval;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TAiApprovalMapper {
    int insert(TAiApproval record);

    List<TAiApproval> selectByRunId(@Param("runId") Long runId);
}
