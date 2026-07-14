package com.autodealer.crm.modules.sales.opportunity.web;

import com.autodealer.crm.shared.security.PermissionCodes;
import com.autodealer.crm.modules.sales.opportunity.application.api.dto.AdvanceOpportunityStageRequest;
import com.autodealer.crm.modules.sales.opportunity.application.api.dto.CreateOpportunityRequest;
import com.autodealer.crm.modules.sales.opportunity.application.api.dto.OpportunityResultRequest;
import com.autodealer.crm.modules.sales.opportunity.application.api.dto.UpdateOpportunityRequest;
import com.autodealer.crm.modules.sales.opportunity.application.api.model.TOpportunity;
import com.autodealer.crm.modules.sales.opportunity.application.api.model.TOpportunityStageHistory;
import com.autodealer.crm.modules.sales.opportunity.application.api.query.OpportunityQuery;
import com.autodealer.crm.shared.web.Result;
import com.autodealer.crm.modules.sales.opportunity.application.api.OpportunityService;
import com.github.pagehelper.PageInfo;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/opportunities")
public class OpportunityController {

    private final OpportunityService opportunityService;

    public OpportunityController(OpportunityService opportunityService) {
        this.opportunityService = opportunityService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('" + PermissionCodes.OPPORTUNITY_LIST + "')")
    public Result<PageInfo<TOpportunity>> list(OpportunityQuery query) {
        return Result.OK(opportunityService.getOpportunityPage(query));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('" + PermissionCodes.OPPORTUNITY_CREATE + "')")
    public Result<TOpportunity> create(@Valid @RequestBody CreateOpportunityRequest request) {
        return Result.OK(opportunityService.createOpportunity(request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('" + PermissionCodes.OPPORTUNITY_VIEW + "')")
    public Result<TOpportunity> detail(@PathVariable Long id) {
        return Result.OK(opportunityService.getOpportunity(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('" + PermissionCodes.OPPORTUNITY_EDIT + "')")
    public Result<TOpportunity> update(@PathVariable Long id,
                                  @Valid @RequestBody UpdateOpportunityRequest request) {
        return Result.OK(opportunityService.updateOpportunity(id, request));
    }

    @GetMapping("/{id}/stage-history")
    @PreAuthorize("hasAuthority('" + PermissionCodes.OPPORTUNITY_VIEW + "')")
    public Result<List<TOpportunityStageHistory>> stageHistory(@PathVariable Long id) {
        return Result.OK(opportunityService.getStageHistory(id));
    }

    @PutMapping("/{id}/stage")
    @PreAuthorize("hasAuthority('" + PermissionCodes.OPPORTUNITY_ADVANCE + "')")
    public Result<TOpportunity> advanceStage(@PathVariable Long id,
                                        @Valid @RequestBody AdvanceOpportunityStageRequest request) {
        return Result.OK(opportunityService.advanceStage(id, request));
    }

    @PutMapping("/{id}/won")
    @PreAuthorize("hasAuthority('" + PermissionCodes.OPPORTUNITY_WIN + "')")
    public Result<TOpportunity> markWon(@PathVariable Long id,
                                   @Valid @RequestBody OpportunityResultRequest request) {
        return Result.OK(opportunityService.markWon(id, request));
    }

    @PutMapping("/{id}/lost")
    @PreAuthorize("hasAuthority('" + PermissionCodes.OPPORTUNITY_LOSE + "')")
    public Result<TOpportunity> markLost(@PathVariable Long id,
                                    @Valid @RequestBody OpportunityResultRequest request) {
        return Result.OK(opportunityService.markLost(id, request));
    }

    @PutMapping("/{id}/shelve")
    @PreAuthorize("hasAuthority('" + PermissionCodes.OPPORTUNITY_SHELVE + "')")
    public Result<TOpportunity> shelve(@PathVariable Long id,
                                  @Valid @RequestBody OpportunityResultRequest request) {
        return Result.OK(opportunityService.shelve(id, request));
    }

    @PutMapping("/{id}/restore")
    @PreAuthorize("hasAuthority('" + PermissionCodes.OPPORTUNITY_RESTORE + "')")
    public Result<TOpportunity> restore(@PathVariable Long id,
                                   @Valid @RequestBody OpportunityResultRequest request) {
        return Result.OK(opportunityService.restore(id, request));
    }
}
