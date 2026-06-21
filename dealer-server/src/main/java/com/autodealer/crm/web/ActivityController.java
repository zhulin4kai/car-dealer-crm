package com.autodealer.crm.web;

import com.autodealer.crm.constant.PermissionCodes;

import com.autodealer.crm.model.TActivity;
import com.autodealer.crm.query.ActivityQuery;
import com.autodealer.crm.result.R;
import com.autodealer.crm.service.ActivityService;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.management.Query;
import java.util.List;

@RestController
public class ActivityController {

    @Resource
    private ActivityService activityService;

    /**
     * 用户列表分页查询
     *
     * @param current
     * @return
     */
    @GetMapping(value = "/api/activitys")
    @PreAuthorize("hasAuthority('" + PermissionCodes.ACTIVITY_LIST + "')")
    public R activityPage(@RequestParam(value = "current", required = false) Integer current, ActivityQuery activityQuery) {

        //required = false 表示参数可以传，也可以不传；
        //required = true 表示参数必须要传，不传会报错；
        if (current == null) {
            current = 1;
        }
        PageInfo<TActivity> pageInfo = activityService.getActivityByPage(current, activityQuery);
        return R.OK(pageInfo);
    }

    @PostMapping(value = "/api/activity")
    @PreAuthorize("hasAuthority('" + PermissionCodes.ACTIVITY_ADD + "')")
    public R addActivity(ActivityQuery activityQuery) {
        int save = activityService.saveActivity(activityQuery);
        return save >= 1 ? R.OK() : R.FAIL();
    }

    @GetMapping(value = "/api/activity/{id}")
    @PreAuthorize("hasAuthority('" + PermissionCodes.ACTIVITY_VIEW + "')")
    public R loadActivity(@PathVariable(value = "id") Integer id) {
        TActivity tActivity = activityService.getActivityById(id);
        return R.OK(tActivity);
    }

    @PutMapping(value = "/api/activity")
    @PreAuthorize("hasAuthority('" + PermissionCodes.ACTIVITY_EDIT + "')")
    public R editActivity(ActivityQuery activityQuery) {
        int update = activityService.updateActivity(activityQuery);
        return update >= 1 ? R.OK() : R.FAIL();
    }

    @PostMapping(value = "/api/activity/batch")
    @PreAuthorize("hasAuthority('" + PermissionCodes.ACTIVITY_DELETE + "')")
    public R batchDeleteActivities(@RequestBody List<Integer> ids) {
        int result = activityService.batchDeleteActivities(ids);
        return result > 0 ? R.OK() : R.FAIL("批量删除失败");
    }

    @DeleteMapping(value = "/api/activity/{id}")
    @PreAuthorize("hasAuthority('" + PermissionCodes.ACTIVITY_DELETE + "')")
    public R deleteActivity(@PathVariable(value = "id") Integer id) {
        int result = activityService.deleteActivity(id);
        return result > 0 ? R.OK() : R.FAIL("删除失败");
    }
}
