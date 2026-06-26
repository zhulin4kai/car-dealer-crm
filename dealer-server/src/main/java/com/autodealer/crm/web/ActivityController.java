package com.autodealer.crm.web;

import com.alibaba.excel.EasyExcel;
import com.autodealer.crm.constant.PermissionCodes;
import com.autodealer.crm.dto.ActivityLifecycleRequest;
import com.autodealer.crm.dto.CreateActivityRequest;
import com.autodealer.crm.dto.ReviewActivityRequest;
import com.autodealer.crm.dto.UpdateActivityRequest;
import com.autodealer.crm.model.TActivity;
import com.autodealer.crm.query.ActivityQuery;
import com.autodealer.crm.result.ActivityExportRow;
import com.autodealer.crm.result.R;
import com.autodealer.crm.service.ActivityService;
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
    public R<PageInfo<TActivity>> activityPage(@RequestParam(value = "page", required = false) Integer page,
                                               @RequestParam(value = "size", required = false) Integer size,
                                               @RequestParam(value = "current", required = false) Integer current,
                                               @RequestParam(value = "pageSize", required = false) Integer pageSize,
                                               ActivityQuery activityQuery) {
        return activityPageInternal(page, size, current, pageSize, activityQuery);
    }

    @Deprecated
    @GetMapping(value = "/api/activitys")
    @PreAuthorize("hasAuthority('" + PermissionCodes.ACTIVITY_LIST + "')")
    public R<PageInfo<TActivity>> legacyActivityPage(@RequestParam(value = "page", required = false) Integer page,
                                                     @RequestParam(value = "size", required = false) Integer size,
                                                     @RequestParam(value = "current", required = false) Integer current,
                                                     @RequestParam(value = "pageSize", required = false) Integer pageSize,
                                                     ActivityQuery activityQuery) {
        return activityPageInternal(page, size, current, pageSize, activityQuery);
    }

    private R<PageInfo<TActivity>> activityPageInternal(Integer page,
                                                        Integer size,
                                                        Integer current,
                                                        Integer pageSize,
                                                        ActivityQuery activityQuery) {
        Integer resolvedPage = page != null ? page : current;
        Integer resolvedPageSize = size != null ? size : pageSize;
        if (resolvedPageSize != null) {
            activityQuery.setPageSize(resolvedPageSize);
        }
        return R.OK(activityService.getActivityByPage(resolvedPage, activityQuery));
    }

    @PostMapping(value = "/api/activity")
    @PreAuthorize("hasAuthority('" + PermissionCodes.ACTIVITY_ADD + "')")
    public R<Void> addActivity(@Valid @RequestBody CreateActivityRequest request) {
        activityService.saveActivity(request);
        return R.OK();
    }

    @PutMapping(value = "/api/activity")
    @PreAuthorize("hasAuthority('" + PermissionCodes.ACTIVITY_EDIT + "')")
    public R<Void> editActivity(@Valid @RequestBody UpdateActivityRequest request) {
        activityService.updateActivity(request);
        return R.OK();
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
    public R<?> activityRoi(@PathVariable(value = "id") Integer id) {
        return R.OK(activityService.getActivityRoi(id));
    }

    @PutMapping(value = "/api/activity/{id}/publish")
    @PreAuthorize("hasAuthority('" + PermissionCodes.ACTIVITY_EDIT + "')")
    public R<TActivity> publishActivity(@PathVariable(value = "id") Integer id) {
        return R.OK(activityService.publishActivity(id));
    }

    @PutMapping(value = "/api/activity/{id}/start")
    @PreAuthorize("hasAuthority('" + PermissionCodes.ACTIVITY_EDIT + "')")
    public R<TActivity> startActivity(@PathVariable(value = "id") Integer id) {
        return R.OK(activityService.startActivity(id));
    }

    @PutMapping(value = "/api/activity/{id}/end")
    @PreAuthorize("hasAuthority('" + PermissionCodes.ACTIVITY_EDIT + "')")
    public R<TActivity> endActivity(@PathVariable(value = "id") Integer id) {
        return R.OK(activityService.endActivity(id));
    }

    @PutMapping(value = "/api/activity/{id}/review")
    @PreAuthorize("hasAuthority('" + PermissionCodes.ACTIVITY_REVIEW + "')")
    public R<TActivity> reviewActivity(@PathVariable(value = "id") Integer id,
                                       @Valid @RequestBody ReviewActivityRequest request) {
        return R.OK(activityService.reviewActivity(id, request));
    }

    @PutMapping(value = "/api/activity/{id}/cancel")
    @PreAuthorize("hasAuthority('" + PermissionCodes.ACTIVITY_CLOSE + "')")
    public R<TActivity> cancelActivity(@PathVariable(value = "id") Integer id,
                                       @Valid @RequestBody ActivityLifecycleRequest request) {
        return R.OK(activityService.cancelActivity(id, request));
    }

    @PutMapping(value = "/api/activity/{id}/close")
    @PreAuthorize("hasAuthority('" + PermissionCodes.ACTIVITY_CLOSE + "')")
    public R<TActivity> closeActivity(@PathVariable(value = "id") Integer id,
                                      @Valid @RequestBody ActivityLifecycleRequest request) {
        return R.OK(activityService.closeActivity(id, request));
    }

    @GetMapping(value = "/api/activity/{id}")
    @PreAuthorize("hasAuthority('" + PermissionCodes.ACTIVITY_VIEW + "')")
    public R<TActivity> loadActivity(@PathVariable(value = "id") Integer id) {
        return R.OK(activityService.getActivityById(id));
    }

    @PostMapping(value = "/api/activity/batch")
    @PreAuthorize("hasAuthority('" + PermissionCodes.ACTIVITY_DELETE + "')")
    public R<Void> batchDeleteActivities(@RequestBody List<Integer> ids) {
        activityService.batchDeleteActivities(ids);
        return R.OK();
    }

    @DeleteMapping(value = "/api/activity/{id}")
    @PreAuthorize("hasAuthority('" + PermissionCodes.ACTIVITY_DELETE + "')")
    public R<Void> deleteActivity(@PathVariable(value = "id") Integer id) {
        activityService.deleteActivity(id);
        return R.OK();
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
