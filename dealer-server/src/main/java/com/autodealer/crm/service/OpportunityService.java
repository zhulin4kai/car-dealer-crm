package com.autodealer.crm.service;

import com.autodealer.crm.dto.AdvanceOpportunityStageRequest;
import com.autodealer.crm.dto.CreateOpportunityRequest;
import com.autodealer.crm.dto.OpportunityResultRequest;
import com.autodealer.crm.dto.UpdateOpportunityRequest;
import com.autodealer.crm.model.TOpportunity;
import com.autodealer.crm.model.TOpportunityStageHistory;
import com.autodealer.crm.query.OpportunityQuery;
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
