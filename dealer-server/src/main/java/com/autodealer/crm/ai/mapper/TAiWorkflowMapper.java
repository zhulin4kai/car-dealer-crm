package com.autodealer.crm.ai.mapper;

import com.autodealer.crm.ai.model.TAiWorkflow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TAiWorkflowMapper {
    int insert(TAiWorkflow record);

    TAiWorkflow selectOwnedByWorkflowNo(@Param("workflowNo") String workflowNo,
                                        @Param("userId") Integer userId);

    TAiWorkflow selectByWorkflowNo(@Param("workflowNo") String workflowNo);

    List<TAiWorkflow> selectByRunId(@Param("runId") Long runId);

    int updateStatusIfCurrent(@Param("id") Long id,
                              @Param("expectedStatus") String expectedStatus,
                              @Param("status") String status,
                              @Param("pauseReason") String pauseReason,
                              @Param("errorCode") String errorCode,
                              @Param("errorMessage") String errorMessage,
                              @Param("editBy") Integer editBy);

    int updateStatus(@Param("id") Long id,
                     @Param("status") String status,
                     @Param("currentStepNo") Integer currentStepNo,
                     @Param("pauseReason") String pauseReason,
                     @Param("errorCode") String errorCode,
                     @Param("errorMessage") String errorMessage,
                     @Param("editBy") Integer editBy);
}
