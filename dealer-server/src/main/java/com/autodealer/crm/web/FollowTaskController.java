package com.autodealer.crm.web;

import com.autodealer.crm.constant.PermissionCodes;
import com.autodealer.crm.dto.CancelFollowTaskRequest;
import com.autodealer.crm.dto.CompleteFollowTaskRequest;
import com.autodealer.crm.dto.CreateFollowTaskRequest;
import com.autodealer.crm.dto.PostponeFollowTaskRequest;
import com.autodealer.crm.model.TFollowTask;
import com.autodealer.crm.query.FollowTaskQuery;
import com.autodealer.crm.result.R;
import com.autodealer.crm.service.FollowTaskService;
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
@RequestMapping("/api/follow-tasks")
public class FollowTaskController {

    private final FollowTaskService followTaskService;

    public FollowTaskController(FollowTaskService followTaskService) {
        this.followTaskService = followTaskService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('" + PermissionCodes.FOLLOW_TASK_LIST + "')")
    public R<PageInfo<TFollowTask>> list(FollowTaskQuery query) {
        return R.OK(followTaskService.getFollowTaskPage(query));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('" + PermissionCodes.FOLLOW_TASK_CREATE + "')")
    public R<TFollowTask> create(@Valid @RequestBody CreateFollowTaskRequest request) {
        return R.OK(followTaskService.createFollowTask(request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('" + PermissionCodes.FOLLOW_TASK_VIEW + "')")
    public R<TFollowTask> detail(@PathVariable Long id) {
        return R.OK(followTaskService.getFollowTask(id));
    }

    @PutMapping("/{id}/start")
    @PreAuthorize("hasAuthority('" + PermissionCodes.FOLLOW_TASK_UPDATE + "')")
    public R<TFollowTask> start(@PathVariable Long id) {
        return R.OK(followTaskService.startFollowTask(id));
    }

    @PutMapping("/{id}/postpone")
    @PreAuthorize("hasAuthority('" + PermissionCodes.FOLLOW_TASK_UPDATE + "')")
    public R<TFollowTask> postpone(@PathVariable Long id,
                                   @Valid @RequestBody PostponeFollowTaskRequest request) {
        return R.OK(followTaskService.postponeFollowTask(id, request));
    }

    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('" + PermissionCodes.FOLLOW_TASK_CANCEL + "')")
    public R<TFollowTask> cancel(@PathVariable Long id,
                                 @Valid @RequestBody CancelFollowTaskRequest request) {
        return R.OK(followTaskService.cancelFollowTask(id, request));
    }

    @PutMapping("/{id}/complete")
    @PreAuthorize("hasAuthority('" + PermissionCodes.FOLLOW_TASK_COMPLETE + "')")
    public R<TFollowTask> complete(@PathVariable Long id,
                                   @Valid @RequestBody CompleteFollowTaskRequest request) {
        return R.OK(followTaskService.completeFollowTask(id, request));
    }
}
