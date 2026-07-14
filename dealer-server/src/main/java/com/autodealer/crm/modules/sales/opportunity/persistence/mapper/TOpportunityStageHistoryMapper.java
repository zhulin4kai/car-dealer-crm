package com.autodealer.crm.modules.sales.opportunity.persistence.mapper;

import com.autodealer.crm.modules.sales.opportunity.application.api.model.TOpportunityStageHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TOpportunityStageHistoryMapper {
    int insert(TOpportunityStageHistory history);

    List<TOpportunityStageHistory> selectByOpportunityId(@Param("opportunityId") Long opportunityId);
}
