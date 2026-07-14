package com.autodealer.crm.modules.sales.activity.web;

import com.alibaba.excel.EasyExcel;
import com.autodealer.crm.shared.security.PermissionCodes;
import com.autodealer.crm.modules.sales.activity.application.api.dto.ActivityLifecycleRequest;
import com.autodealer.crm.modules.sales.activity.application.api.dto.CreateActivityRequest;
import com.autodealer.crm.modules.sales.activity.application.api.dto.ReviewActivityRequest;
import com.autodealer.crm.modules.sales.activity.application.api.dto.UpdateActivityRequest;
import com.autodealer.crm.modules.sales.activity.application.api.model.TActivity;
import com.autodealer.crm.modules.sales.activity.application.api.query.ActivityQuery;
import com.autodealer.crm.modules.sales.activity.application.api.result.ActivityExportRow;
import com.autodealer.crm.shared.web.Result;
import com.autodealer.crm.modules.sales.activity.application.api.ActivityService;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
public class ActivityController {

    private static final String EXCEL_FILE_NAME_PREFIX = "活动ROI数据";

    @Resource
    private ActivityService activityService;

    @GetMapping(value = "/api/activities")
    @PreAuthorize("hasAuthority('" + PermissionCodes.ACTIVITY_LIST + "')")
    public Result<PageInfo<TActivity>> activityPage(@RequestParam(value = "page", required = false) Integer page,
                                               @RequestParam(value = "size", required = false) Integer size,
                                               ActivityQuery activityQuery) {
        if (size != null) {
            activityQuery.setPageSize(size);
        }
        return Result.OK(activityService.getActivityByPage(page, activityQuery));
    }

    @PostMapping(value = "/api/activity")
    @PreAuthorize("hasAuthority('" + PermissionCodes.ACTIVITY_ADD + "')")
    public Result<Void> addActivity(@Valid @RequestBody CreateActivityRequest request) {
        activityService.saveActivity(request);
        return Result.OK();
    }

    @PutMapping(value = "/api/activity")
    @PreAuthorize("hasAuthority('" + PermissionCodes.ACTIVITY_EDIT + "')")
    public Result<Void> editActivity(@Valid @RequestBody UpdateActivityRequest request) {
        activityService.updateActivity(request);
        return Result.OK();
    }

    @GetMapping(value = "/api/activity/export")
    @PreAuthorize("hasAuthority('" + PermissionCodes.ACTIVITY_EXPORT + "')")
    public void exportActivities(ActivityQuery query, HttpServletResponse response) throws IOException {
        List<ActivityExportRow> rows = activityService.exportActivityRoi(query);
        setExcelResponseHeaders(response);
        EasyExcel.write(response.getOutputStream(), ActivityExportRow.class).sheet("活动ROI").doWrite(rows);
    }

    @GetMapping(value = "/api/activity/{id}/roi")
    @PreAuthorize("hasAuthority('" + PermissionCodes.ACTIVITY_VIEW + "')")
    public Result<?> activityRoi(@PathVariable(value = "id") Integer id) {
        return Result.OK(activityService.getActivityRoi(id));
    }

    @PutMapping(value = "/api/activity/{id}/publish")
    @PreAuthorize("hasAuthority('" + PermissionCodes.ACTIVITY_EDIT + "')")
    public Result<TActivity> publishActivity(@PathVariable(value = "id") Integer id) {
        return Result.OK(activityService.publishActivity(id));
    }

    @PutMapping(value = "/api/activity/{id}/start")
    @PreAuthorize("hasAuthority('" + PermissionCodes.ACTIVITY_EDIT + "')")
    public Result<TActivity> startActivity(@PathVariable(value = "id") Integer id) {
        return Result.OK(activityService.startActivity(id));
    }

    @PutMapping(value = "/api/activity/{id}/end")
    @PreAuthorize("hasAuthority('" + PermissionCodes.ACTIVITY_EDIT + "')")
    public Result<TActivity> endActivity(@PathVariable(value = "id") Integer id) {
        return Result.OK(activityService.endActivity(id));
    }

    @PutMapping(value = "/api/activity/{id}/review")
    @PreAuthorize("hasAuthority('" + PermissionCodes.ACTIVITY_REVIEW + "')")
    public Result<TActivity> reviewActivity(@PathVariable(value = "id") Integer id,
                                       @Valid @RequestBody ReviewActivityRequest request) {
        return Result.OK(activityService.reviewActivity(id, request));
    }

    @PutMapping(value = "/api/activity/{id}/cancel")
    @PreAuthorize("hasAuthority('" + PermissionCodes.ACTIVITY_CLOSE + "')")
    public Result<TActivity> cancelActivity(@PathVariable(value = "id") Integer id,
                                       @Valid @RequestBody ActivityLifecycleRequest request) {
        return Result.OK(activityService.cancelActivity(id, request));
    }

    @PutMapping(value = "/api/activity/{id}/close")
    @PreAuthorize("hasAuthority('" + PermissionCodes.ACTIVITY_CLOSE + "')")
    public Result<TActivity> closeActivity(@PathVariable(value = "id") Integer id,
                                      @Valid @RequestBody ActivityLifecycleRequest request) {
        return Result.OK(activityService.closeActivity(id, request));
    }

    @GetMapping(value = "/api/activity/{id}")
    @PreAuthorize("hasAuthority('" + PermissionCodes.ACTIVITY_VIEW + "')")
    public Result<TActivity> loadActivity(@PathVariable(value = "id") Integer id) {
        return Result.OK(activityService.getActivityById(id));
    }

    @PostMapping(value = "/api/activity/batch")
    @PreAuthorize("hasAuthority('" + PermissionCodes.ACTIVITY_DELETE + "')")
    public Result<Void> batchDeleteActivities(@RequestBody List<Integer> ids) {
        activityService.batchDeleteActivities(ids);
        return Result.OK();
    }

    @DeleteMapping(value = "/api/activity/{id}")
    @PreAuthorize("hasAuthority('" + PermissionCodes.ACTIVITY_DELETE + "')")
    public Result<Void> deleteActivity(@PathVariable(value = "id") Integer id) {
        activityService.deleteActivity(id);
        return Result.OK();
    }

    private void setExcelResponseHeaders(HttpServletResponse response) {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode(EXCEL_FILE_NAME_PREFIX + System.currentTimeMillis(),
                StandardCharsets.UTF_8).replace("+", "%20");
        response.setHeader("Content-Disposition",
                "attachment; filename=\"" + fileName + ".xlsx\"; filename*=UTF-8''" + fileName + ".xlsx");
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
    }
}
