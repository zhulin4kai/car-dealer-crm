package com.autodealer.crm.modules.sales.followup.web;

import com.autodealer.crm.shared.security.PermissionCodes;
import com.autodealer.crm.modules.sales.followup.application.api.dto.CorrectCommunicationRecordRequest;
import com.autodealer.crm.modules.sales.followup.application.api.dto.CreateCommunicationRecordRequest;
import com.autodealer.crm.modules.sales.followup.application.api.dto.VoidCommunicationRecordRequest;
import com.autodealer.crm.modules.sales.followup.application.api.model.TCommunicationRecord;
import com.autodealer.crm.modules.sales.followup.application.api.query.CommunicationRecordQuery;
import com.autodealer.crm.shared.web.Result;
import com.autodealer.crm.modules.sales.followup.application.api.CommunicationRecordService;
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

@RestController
@RequestMapping("/api/communication-records")
public class CommunicationRecordController {

    private final CommunicationRecordService communicationRecordService;

    public CommunicationRecordController(CommunicationRecordService communicationRecordService) {
        this.communicationRecordService = communicationRecordService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('" + PermissionCodes.COMMUNICATION_RECORD_LIST + "')")
    public Result<PageInfo<TCommunicationRecord>> list(CommunicationRecordQuery query) {
        return Result.OK(communicationRecordService.getCommunicationRecordPage(query));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('" + PermissionCodes.COMMUNICATION_RECORD_CREATE + "')")
    public Result<TCommunicationRecord> create(@Valid @RequestBody CreateCommunicationRecordRequest request) {
        return Result.OK(communicationRecordService.createCommunicationRecord(request));
    }

    @PutMapping("/{id}/correct")
    @PreAuthorize("hasAuthority('" + PermissionCodes.COMMUNICATION_RECORD_CORRECT + "')")
    public Result<TCommunicationRecord> correct(@PathVariable Long id,
                                           @Valid @RequestBody CorrectCommunicationRecordRequest request) {
        return Result.OK(communicationRecordService.correctCommunicationRecord(id, request));
    }

    @PutMapping("/{id}/void")
    @PreAuthorize("hasAuthority('" + PermissionCodes.COMMUNICATION_RECORD_VOID + "')")
    public Result<TCommunicationRecord> voidRecord(@PathVariable Long id,
                                              @Valid @RequestBody VoidCommunicationRecordRequest request) {
        return Result.OK(communicationRecordService.voidCommunicationRecord(id, request));
    }
}
