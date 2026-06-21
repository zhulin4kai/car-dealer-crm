package com.autodealer.crm.web;

import com.autodealer.crm.constant.PermissionCodes;

import com.autodealer.crm.config.security.CurrentUserProvider;
import com.autodealer.crm.dto.CreateActivityRemarkRequest;
import com.autodealer.crm.model.TActivityRemark;
import com.autodealer.crm.query.ActivityRemarkQuery;
import com.autodealer.crm.result.R;
import com.autodealer.crm.service.ActivityRemarkService;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
public class ActivityRemarkController {

    @Resource
    private ActivityRemarkService activityRemarkService;

    @Resource
    private CurrentUserProvider currentUserProvider;

    @PostMapping(value = "/api/activity/remark")
    @PreAuthorize("hasAuthority('" + PermissionCodes.ACTIVITY_ADD + "')")
    public R addActivityRemark(@Valid @RequestBody CreateActivityRemarkRequest req) {
        ActivityRemarkQuery query = new ActivityRemarkQuery();
        query.setActivityId(req.getActivityId());
        query.setNoteContent(req.getNoteContent());
        int save = activityRemarkService.saveActivityRemark(query);
        return save >= 1 ? R.OK() : R.FAIL();
    }

    @GetMapping(value = "/api/activity/remark")
    @PreAuthorize("hasAuthority('" + PermissionCodes.ACTIVITY_VIEW + "')")
    public R activityRemarkPage(@RequestParam(value = "current", required = false) Integer current,
                                @RequestParam(value = "activityId") Integer activityId) {

        ActivityRemarkQuery activityRemarkQuery = new ActivityRemarkQuery();
        activityRemarkQuery.setActivityId(activityId);

        if (current == null) {
            current = 1;
        }
        PageInfo<TActivityRemark> pageInfo = activityRemarkService.getActivityRemarkByPage(current, activityRemarkQuery);
        return R.OK(pageInfo);
    }

    @GetMapping(value = "/api/activity/remark/{id}")
    @PreAuthorize("hasAuthority('" + PermissionCodes.ACTIVITY_VIEW + "')")
    public R activityRemarkPage(@PathVariable(value = "id") Integer id) {
        TActivityRemark tActivityRemark = activityRemarkService.getActivityRemarkById(id);
        return R.OK(tActivityRemark);
    }

    @PutMapping(value = "/api/activity/remark")
    @PreAuthorize("hasAuthority('" + PermissionCodes.ACTIVITY_EDIT + "')")
    public R editActivityRemark(@RequestBody ActivityRemarkQuery activityRemarkQuery) {
        int update = activityRemarkService.updateActivityRemark(activityRemarkQuery);
        return update >= 1 ? R.OK() : R.FAIL();
    }

    @DeleteMapping(value = "/api/activity/remark/{id}")
    @PreAuthorize("hasAuthority('" + PermissionCodes.ACTIVITY_DELETE + "')")
    public R delActivityRemark(@PathVariable(value = "id") Integer id) {
        int del = activityRemarkService.delActivityRemarkById(id);
        return del >= 1 ? R.OK() : R.FAIL();
    }
}
