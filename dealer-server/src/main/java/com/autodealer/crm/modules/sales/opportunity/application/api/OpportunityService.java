package com.autodealer.crm.modules.sales.opportunity.application.api;

import com.autodealer.crm.modules.sales.opportunity.application.api.dto.AdvanceOpportunityStageRequest;
import com.autodealer.crm.modules.sales.opportunity.application.api.dto.CreateOpportunityRequest;
import com.autodealer.crm.modules.sales.opportunity.application.api.dto.OpportunityResultRequest;
import com.autodealer.crm.modules.sales.opportunity.application.api.dto.UpdateOpportunityRequest;
import com.autodealer.crm.modules.sales.opportunity.application.api.model.TOpportunity;
import com.autodealer.crm.modules.sales.opportunity.application.api.model.TOpportunityStageHistory;
import com.autodealer.crm.modules.sales.opportunity.application.api.query.OpportunityQuery;
import com.github.pagehelper.PageInfo;

import java.util.List;

public interface OpportunityService {
    PageInfo<TOpportunity> getOpportunityPage(OpportunityQuery query);

    TOpportunity createOpportunity(CreateOpportunityRequest request);

    TOpportunity getOpportunity(Long id);

    TOpportunity updateOpportunity(Long id, UpdateOpportunityRequest request);

    List<TOpportunityStageHistory> getStageHistory(Long id);

    TOpportunity advanceStage(Long id, AdvanceOpportunityStageRequest request);

    TOpportunity markWon(Long id, OpportunityResultRequest request);

    TOpportunity markLost(Long id, OpportunityResultRequest request);

    TOpportunity shelve(Long id, OpportunityResultRequest request);

    TOpportunity restore(Long id, OpportunityResultRequest request);
}
