package com.autodealer.crm.modules.ai.persistence.mapper;

import com.autodealer.crm.modules.ai.persistence.model.TAiActionProposal;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TAiActionProposalMapper {
    int insert(TAiActionProposal record);

    TAiActionProposal selectById(@Param("id") Long id);

    List<TAiActionProposal> selectByRunId(@Param("runId") Long runId);

    int updateStatus(@Param("id") Long id,
                     @Param("status") String status,
                     @Param("errorCode") String errorCode,
                     @Param("resultSummary") String resultSummary);

    int updateStatusIfCurrent(@Param("id") Long id,
                              @Param("expectedStatus") String expectedStatus,
                              @Param("status") String status,
                              @Param("errorCode") String errorCode,
                              @Param("resultSummary") String resultSummary);
}
