package com.autodealer.crm.web;

import com.autodealer.crm.constant.PermissionCodes;

import com.autodealer.crm.dto.CreateClueRemarkRequest;
import com.autodealer.crm.model.TClueRemark;
import com.autodealer.crm.query.ClueRemarkQuery;
import com.autodealer.crm.result.R;
import com.autodealer.crm.service.ClueRemarkService;
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
    public R addClueRemark(@Valid @RequestBody CreateClueRemarkRequest req) {
        ClueRemarkQuery query = new ClueRemarkQuery();
        query.setClueId(req.getClueId());
        query.setNoteWay(req.getNoteWay());
        query.setNoteContent(req.getNoteContent());
        int save = clueRemarkService.saveClueRemark(query);
        return save >= 1 ? R.OK() : R.FAIL();
    }

    @GetMapping(value = "/api/clue/remark")
    @PreAuthorize("hasAuthority('" + PermissionCodes.CLUE_VIEW + "')")
    public R clueRemarkPage(@RequestParam(value = "page", required = false) Integer page,
                            @RequestParam(value = "size", required = false) Integer size,
                            @RequestParam(value = "clueId") Integer clueId) {

        ClueRemarkQuery clueRemarkQuery = new ClueRemarkQuery();
        clueRemarkQuery.setClueId(clueId);

        PageInfo<TClueRemark> pageInfo = clueRemarkService.getClueRemarkByPage(
                page == null ? 1 : page, size, clueRemarkQuery);
        return R.OK(pageInfo);
    }
}
