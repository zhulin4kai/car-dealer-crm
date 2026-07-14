package com.autodealer.crm.modules.ai.web;

import com.autodealer.crm.modules.ai.application.api.dto.AiWorkflowActionRequest;
import com.autodealer.crm.modules.ai.application.api.dto.AiWorkflowResponse;
import com.autodealer.crm.modules.ai.application.api.dto.CreateAiWorkflowRequest;
import com.autodealer.crm.modules.ai.application.api.AiWorkflowService;
import com.autodealer.crm.shared.security.PermissionCodes;
import com.autodealer.crm.shared.web.Result;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/ai/workflows")
public class AiWorkflowController {
    private final AiWorkflowService workflowService;

    public AiWorkflowController(AiWorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('" + PermissionCodes.AI_WORKFLOW_MANAGE + "')")
    public Result<AiWorkflowResponse> create(@Valid @RequestBody CreateAiWorkflowRequest request) {
        return Result.OK(workflowService.create(request));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('" + PermissionCodes.AI_WORKFLOW_VIEW + "')")
    public Result<List<AiWorkflowResponse>> listByRun(@RequestParam String runNo) {
        return Result.OK(workflowService.listByRun(runNo));
    }

    @GetMapping("/{workflowNo}")
    @PreAuthorize("hasAuthority('" + PermissionCodes.AI_WORKFLOW_VIEW + "')")
    public Result<AiWorkflowResponse> detail(@PathVariable String workflowNo) {
        return Result.OK(workflowService.get(workflowNo));
    }

    @PostMapping("/{workflowNo}/pause")
    @PreAuthorize("hasAuthority('" + PermissionCodes.AI_WORKFLOW_MANAGE + "')")
    public Result<AiWorkflowResponse> pause(@PathVariable String workflowNo,
                                       @Valid @RequestBody(required = false) AiWorkflowActionRequest request) {
        return Result.OK(workflowService.pause(workflowNo, request));
    }

    @PostMapping("/{workflowNo}/resume")
    @PreAuthorize("hasAuthority('" + PermissionCodes.AI_WORKFLOW_MANAGE + "')")
    public Result<AiWorkflowResponse> resume(@PathVariable String workflowNo) {
        return Result.OK(workflowService.resume(workflowNo));
    }

    @PostMapping("/{workflowNo}/cancel")
    @PreAuthorize("hasAuthority('" + PermissionCodes.AI_WORKFLOW_MANAGE + "')")
    public Result<AiWorkflowResponse> cancel(@PathVariable String workflowNo,
                                        @Valid @RequestBody(required = false) AiWorkflowActionRequest request) {
        return Result.OK(workflowService.cancel(workflowNo, request));
    }

    @PostMapping("/{workflowNo}/fail")
    @PreAuthorize("hasAuthority('" + PermissionCodes.AI_WORKFLOW_MANAGE + "')")
    public Result<AiWorkflowResponse> fail(@PathVariable String workflowNo,
                                      @Valid @RequestBody(required = false) AiWorkflowActionRequest request) {
        return Result.OK(workflowService.fail(workflowNo, request));
    }
}
