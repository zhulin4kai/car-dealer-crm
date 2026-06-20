package com.autodealer.crm.web;

import com.autodealer.crm.model.TClueRemark;
import com.autodealer.crm.query.ClueRemarkQuery;
import com.autodealer.crm.result.R;
import com.autodealer.crm.service.ClueRemarkService;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
public class ClueRemarkController {

    @Resource
    private ClueRemarkService clueRemarkService;

    @PostMapping(value = "/api/clue/remark")
    @PreAuthorize("hasAuthority('clue:add')")
    public R addActivityRemark(@RequestBody ClueRemarkQuery clueRemarkQuery) {
        //axios提交post请求，提交过来的是json数据，使用@RequestBody注解接收
        int save = clueRemarkService.saveClueRemark(clueRemarkQuery);
        return save >= 1 ? R.OK( ) : R.FAIL();
    }

    @GetMapping(value = "/api/clue/remark")
    @PreAuthorize("hasAuthority('clue:view')")
    public R clueRemarkPage(@RequestParam(value = "current", required = false) Integer current,
                            @RequestParam(value = "clueId") Integer clueId) {

        ClueRemarkQuery clueRemarkQuery = new ClueRemarkQuery();
        clueRemarkQuery.setClueId(clueId);

        if (current == null) {
            current = 1;
        }
        PageInfo<TClueRemark> pageInfo = clueRemarkService.getClueRemarkByPage(current, clueRemarkQuery);
        return R.OK(pageInfo);
    }
}
