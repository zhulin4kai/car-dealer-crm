package com.autodealer.crm.web;

import com.autodealer.crm.constant.PermissionCodes;
import com.autodealer.crm.dto.CorrectCommunicationRecordRequest;
import com.autodealer.crm.dto.CreateCommunicationRecordRequest;
import com.autodealer.crm.dto.VoidCommunicationRecordRequest;
import com.autodealer.crm.model.TCommunicationRecord;
import com.autodealer.crm.query.CommunicationRecordQuery;
import com.autodealer.crm.result.R;
import com.autodealer.crm.service.CommunicationRecordService;
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
    public R<PageInfo<TCommunicationRecord>> list(CommunicationRecordQuery query) {
        return R.OK(communicationRecordService.getCommunicationRecordPage(query));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('" + PermissionCodes.COMMUNICATION_RECORD_CREATE + "')")
    public R<TCommunicationRecord> create(@Valid @RequestBody CreateCommunicationRecordRequest request) {
        return R.OK(communicationRecordService.createCommunicationRecord(request));
    }

    @PutMapping("/{id}/correct")
    @PreAuthorize("hasAuthority('" + PermissionCodes.COMMUNICATION_RECORD_CORRECT + "')")
    public R<TCommunicationRecord> correct(@PathVariable Long id,
                                           @Valid @RequestBody CorrectCommunicationRecordRequest request) {
        return R.OK(communicationRecordService.correctCommunicationRecord(id, request));
    }

    @PutMapping("/{id}/void")
    @PreAuthorize("hasAuthority('" + PermissionCodes.COMMUNICATION_RECORD_VOID + "')")
    public R<TCommunicationRecord> voidRecord(@PathVariable Long id,
                                              @Valid @RequestBody VoidCommunicationRecordRequest request) {
        return R.OK(communicationRecordService.voidCommunicationRecord(id, request));
    }
}
