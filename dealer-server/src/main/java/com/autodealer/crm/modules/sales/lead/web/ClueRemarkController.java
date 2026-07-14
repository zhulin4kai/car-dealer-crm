package com.autodealer.crm.modules.sales.lead.web;

import com.autodealer.crm.shared.security.PermissionCodes;

import com.autodealer.crm.modules.sales.lead.application.api.dto.CreateClueRemarkRequest;
import com.autodealer.crm.modules.sales.lead.application.api.model.TClueRemark;
import com.autodealer.crm.modules.sales.lead.application.api.query.ClueRemarkQuery;
import com.autodealer.crm.shared.web.Result;
import com.autodealer.crm.modules.sales.lead.application.api.ClueRemarkService;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
public class ClueRemarkController {

    @Resource
    private ClueRemarkService clueRemarkService;

    @PostMapping(value = "/api/clue/remark")
    @PreAuthorize("hasAuthority('" + PermissionCodes.CLUE_ADD + "')")
    public Result addClueRemark(@Valid @RequestBody CreateClueRemarkRequest req) {
        ClueRemarkQuery query = new ClueRemarkQuery();
        query.setClueId(req.getClueId());
        query.setNoteWay(req.getNoteWay());
        query.setNoteContent(req.getNoteContent());
        int save = clueRemarkService.saveClueRemark(query);
        return save >= 1 ? Result.OK() : Result.FAIL();
    }

    @GetMapping(value = "/api/clue/remark")
    @PreAuthorize("hasAuthority('" + PermissionCodes.CLUE_VIEW + "')")
    public Result clueRemarkPage(@RequestParam(value = "page", required = false) Integer page,
                            @RequestParam(value = "size", required = false) Integer size,
                            @RequestParam(value = "clueId") Integer clueId) {

        ClueRemarkQuery clueRemarkQuery = new ClueRemarkQuery();
        clueRemarkQuery.setClueId(clueId);

        PageInfo<TClueRemark> pageInfo = clueRemarkService.getClueRemarkByPage(
                page == null ? 1 : page, size, clueRemarkQuery);
        return Result.OK(pageInfo);
    }
}
