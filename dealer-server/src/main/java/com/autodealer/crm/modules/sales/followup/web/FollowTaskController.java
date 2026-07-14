package com.autodealer.crm.modules.sales.followup.web;

import com.autodealer.crm.shared.security.PermissionCodes;
import com.autodealer.crm.modules.sales.followup.application.api.dto.CancelFollowTaskRequest;
import com.autodealer.crm.modules.sales.followup.application.api.dto.CompleteFollowTaskRequest;
import com.autodealer.crm.modules.sales.followup.application.api.dto.CreateFollowTaskRequest;
import com.autodealer.crm.modules.sales.followup.application.api.dto.PostponeFollowTaskRequest;
import com.autodealer.crm.modules.sales.followup.application.api.model.TFollowTask;
import com.autodealer.crm.modules.sales.followup.application.api.query.FollowTaskQuery;
import com.autodealer.crm.shared.web.Result;
import com.autodealer.crm.modules.sales.followup.application.api.FollowTaskService;
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
    public Result<PageInfo<TFollowTask>> list(FollowTaskQuery query) {
        return Result.OK(followTaskService.getFollowTaskPage(query));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('" + PermissionCodes.FOLLOW_TASK_CREATE + "')")
    public Result<TFollowTask> create(@Valid @RequestBody CreateFollowTaskRequest request) {
        return Result.OK(followTaskService.createFollowTask(request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('" + PermissionCodes.FOLLOW_TASK_VIEW + "')")
    public Result<TFollowTask> detail(@PathVariable Long id) {
        return Result.OK(followTaskService.getFollowTask(id));
    }

    @PutMapping("/{id}/start")
    @PreAuthorize("hasAuthority('" + PermissionCodes.FOLLOW_TASK_UPDATE + "')")
    public Result<TFollowTask> start(@PathVariable Long id) {
        return Result.OK(followTaskService.startFollowTask(id));
    }

    @PutMapping("/{id}/postpone")
    @PreAuthorize("hasAuthority('" + PermissionCodes.FOLLOW_TASK_UPDATE + "')")
    public Result<TFollowTask> postpone(@PathVariable Long id,
                                   @Valid @RequestBody PostponeFollowTaskRequest request) {
        return Result.OK(followTaskService.postponeFollowTask(id, request));
    }

    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('" + PermissionCodes.FOLLOW_TASK_CANCEL + "')")
    public Result<TFollowTask> cancel(@PathVariable Long id,
                                 @Valid @RequestBody CancelFollowTaskRequest request) {
        return Result.OK(followTaskService.cancelFollowTask(id, request));
    }

    @PutMapping("/{id}/complete")
    @PreAuthorize("hasAuthority('" + PermissionCodes.FOLLOW_TASK_COMPLETE + "')")
    public Result<TFollowTask> complete(@PathVariable Long id,
                                   @Valid @RequestBody CompleteFollowTaskRequest request) {
        return Result.OK(followTaskService.completeFollowTask(id, request));
    }
}
