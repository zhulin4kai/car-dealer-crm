package com.autodealer.crm.modules.sales.activity.web;

import com.autodealer.crm.shared.security.PermissionCodes;

import com.autodealer.crm.modules.identity.application.api.security.CurrentUserProvider;
import com.autodealer.crm.modules.sales.activity.application.api.dto.CreateActivityRemarkRequest;
import com.autodealer.crm.modules.sales.activity.application.api.model.TActivityRemark;
import com.autodealer.crm.modules.sales.activity.application.api.query.ActivityRemarkQuery;
import com.autodealer.crm.shared.web.Result;
import com.autodealer.crm.modules.sales.activity.application.api.ActivityRemarkService;
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
    public Result addActivityRemark(@Valid @RequestBody CreateActivityRemarkRequest req) {
        ActivityRemarkQuery query = new ActivityRemarkQuery();
        query.setActivityId(req.getActivityId());
        query.setNoteContent(req.getNoteContent());
        int save = activityRemarkService.saveActivityRemark(query);
        return save >= 1 ? Result.OK() : Result.FAIL();
    }

    @GetMapping(value = "/api/activity/remark")
    @PreAuthorize("hasAuthority('" + PermissionCodes.ACTIVITY_VIEW + "')")
    public Result activityRemarkPage(@RequestParam(value = "page", required = false) Integer page,
                                @RequestParam(value = "size", required = false) Integer size,
                                @RequestParam(value = "activityId") Integer activityId) {

        ActivityRemarkQuery activityRemarkQuery = new ActivityRemarkQuery();
        activityRemarkQuery.setActivityId(activityId);

        PageInfo<TActivityRemark> pageInfo = activityRemarkService.getActivityRemarkByPage(
                page == null ? 1 : page, size, activityRemarkQuery);
        return Result.OK(pageInfo);
    }

    @GetMapping(value = "/api/activity/remark/{id}")
    @PreAuthorize("hasAuthority('" + PermissionCodes.ACTIVITY_VIEW + "')")
    public Result activityRemarkPage(@PathVariable(value = "id") Integer id) {
        TActivityRemark tActivityRemark = activityRemarkService.getActivityRemarkById(id);
        return Result.OK(tActivityRemark);
    }

    @PutMapping(value = "/api/activity/remark")
    @PreAuthorize("hasAuthority('" + PermissionCodes.ACTIVITY_EDIT + "')")
    public Result editActivityRemark(@RequestBody ActivityRemarkQuery activityRemarkQuery) {
        int update = activityRemarkService.updateActivityRemark(activityRemarkQuery);
        return update >= 1 ? Result.OK() : Result.FAIL();
    }

    @DeleteMapping(value = "/api/activity/remark/{id}")
    @PreAuthorize("hasAuthority('" + PermissionCodes.ACTIVITY_DELETE + "')")
    public Result delActivityRemark(@PathVariable(value = "id") Integer id) {
        int del = activityRemarkService.delActivityRemarkById(id);
        return del >= 1 ? Result.OK() : Result.FAIL();
    }
}
