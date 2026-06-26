package com.autodealer.crm.web;

import com.autodealer.crm.constant.PermissionCodes;
import com.autodealer.crm.dto.AdvanceOpportunityStageRequest;
import com.autodealer.crm.dto.CreateOpportunityRequest;
import com.autodealer.crm.dto.OpportunityResultRequest;
import com.autodealer.crm.dto.UpdateOpportunityRequest;
import com.autodealer.crm.model.TOpportunity;
import com.autodealer.crm.model.TOpportunityStageHistory;
import com.autodealer.crm.query.OpportunityQuery;
import com.autodealer.crm.result.R;
import com.autodealer.crm.service.OpportunityService;
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
    public R<PageInfo<TOpportunity>> list(OpportunityQuery query) {
        return R.OK(opportunityService.getOpportunityPage(query));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('" + PermissionCodes.OPPORTUNITY_CREATE + "')")
    public R<TOpportunity> create(@Valid @RequestBody CreateOpportunityRequest request) {
        return R.OK(opportunityService.createOpportunity(request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('" + PermissionCodes.OPPORTUNITY_VIEW + "')")
    public R<TOpportunity> detail(@PathVariable Long id) {
        return R.OK(opportunityService.getOpportunity(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('" + PermissionCodes.OPPORTUNITY_EDIT + "')")
    public R<TOpportunity> update(@PathVariable Long id,
                                  @Valid @RequestBody UpdateOpportunityRequest request) {
        return R.OK(opportunityService.updateOpportunity(id, request));
    }

    @GetMapping("/{id}/stage-history")
    @PreAuthorize("hasAuthority('" + PermissionCodes.OPPORTUNITY_VIEW + "')")
    public R<List<TOpportunityStageHistory>> stageHistory(@PathVariable Long id) {
        return R.OK(opportunityService.getStageHistory(id));
    }

    @PutMapping("/{id}/stage")
    @PreAuthorize("hasAuthority('" + PermissionCodes.OPPORTUNITY_ADVANCE + "')")
    public R<TOpportunity> advanceStage(@PathVariable Long id,
                                        @Valid @RequestBody AdvanceOpportunityStageRequest request) {
        return R.OK(opportunityService.advanceStage(id, request));
    }

    @PutMapping("/{id}/won")
    @PreAuthorize("hasAuthority('" + PermissionCodes.OPPORTUNITY_WIN + "')")
    public R<TOpportunity> markWon(@PathVariable Long id,
                                   @Valid @RequestBody OpportunityResultRequest request) {
        return R.OK(opportunityService.markWon(id, request));
    }

    @PutMapping("/{id}/lost")
    @PreAuthorize("hasAuthority('" + PermissionCodes.OPPORTUNITY_LOSE + "')")
    public R<TOpportunity> markLost(@PathVariable Long id,
                                    @Valid @RequestBody OpportunityResultRequest request) {
        return R.OK(opportunityService.markLost(id, request));
    }

    @PutMapping("/{id}/shelve")
    @PreAuthorize("hasAuthority('" + PermissionCodes.OPPORTUNITY_SHELVE + "')")
    public R<TOpportunity> shelve(@PathVariable Long id,
                                  @Valid @RequestBody OpportunityResultRequest request) {
        return R.OK(opportunityService.shelve(id, request));
    }

    @PutMapping("/{id}/restore")
    @PreAuthorize("hasAuthority('" + PermissionCodes.OPPORTUNITY_RESTORE + "')")
    public R<TOpportunity> restore(@PathVariable Long id,
                                   @Valid @RequestBody OpportunityResultRequest request) {
        return R.OK(opportunityService.restore(id, request));
    }
}
