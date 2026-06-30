package com.autodealer.crm.ai.mapper;

import com.autodealer.crm.ai.model.TAiWorkflowStep;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TAiWorkflowStepMapper {
    int insert(TAiWorkflowStep record);

    List<TAiWorkflowStep> selectByWorkflowId(@Param("workflowId") Long workflowId);

    TAiWorkflowStep selectByWorkflowIdAndStepNo(@Param("workflowId") Long workflowId,
                                                @Param("stepNo") Integer stepNo);

    int updateUnfinishedByWorkflowId(@Param("workflowId") Long workflowId,
                                     @Param("status") String status,
                                     @Param("errorCode") String errorCode,
                                     @Param("editBy") Integer editBy);

    int updateStepStatus(@Param("id") Long id,
                         @Param("status") String status,
                         @Param("proposalId") Long proposalId,
                         @Param("outputSummary") String outputSummary,
                         @Param("errorCode") String errorCode,
                         @Param("editBy") Integer editBy);
}
