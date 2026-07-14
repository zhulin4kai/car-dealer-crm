package com.autodealer.crm.modules.sales.opportunity.application.api.port;

import com.autodealer.crm.modules.sales.opportunity.application.api.model.TOpportunity;
import com.autodealer.crm.modules.sales.opportunity.application.api.query.OpportunityQuery;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface OpportunityDataPort {
    int insert(TOpportunity opportunity);

    TOpportunity selectById(@Param("id") Long id);

    TOpportunity selectByIdForUpdate(@Param("id") Long id);

    List<TOpportunity> selectByQuery(@Param("query") OpportunityQuery query);

    int updateBasic(@Param("record") TOpportunity opportunity,
                    @Param("dataScopeUserId") Integer dataScopeUserId);

    int updateStageIfCurrent(@Param("id") Long id,
                             @Param("expectedStage") String expectedStage,
                             @Param("targetStage") String targetStage,
                             @Param("reason") String reason,
                             @Param("competitor") String competitor,
                             @Param("remark") String remark,
                             @Param("nextActionTime") LocalDate nextActionTime,
                             @Param("orderTranId") Integer orderTranId,
                             @Param("updateTime") LocalDateTime updateTime,
                             @Param("updateBy") Integer updateBy);

    int updateRecentFollowFact(@Param("id") Long id,
                               @Param("lastFollowTime") LocalDateTime lastFollowTime,
                               @Param("lastFollowSummary") String lastFollowSummary,
                               @Param("nextActionTime") LocalDate nextActionTime,
                               @Param("updateBy") Integer updateBy,
                               @Param("dataScopeUserId") Integer dataScopeUserId);
}
