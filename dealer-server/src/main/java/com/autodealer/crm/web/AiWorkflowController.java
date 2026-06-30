package com.autodealer.crm.web;

import com.autodealer.crm.ai.dto.AiWorkflowActionRequest;
import com.autodealer.crm.ai.dto.AiWorkflowResponse;
import com.autodealer.crm.ai.dto.CreateAiWorkflowRequest;
import com.autodealer.crm.ai.service.AiWorkflowService;
import com.autodealer.crm.constant.PermissionCodes;
import com.autodealer.crm.result.R;
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
    public R<AiWorkflowResponse> create(@Valid @RequestBody CreateAiWorkflowRequest request) {
        return R.OK(workflowService.create(request));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('" + PermissionCodes.AI_WORKFLOW_VIEW + "')")
    public R<List<AiWorkflowResponse>> listByRun(@RequestParam String runNo) {
        return R.OK(workflowService.listByRun(runNo));
    }

    @GetMapping("/{workflowNo}")
    @PreAuthorize("hasAuthority('" + PermissionCodes.AI_WORKFLOW_VIEW + "')")
    public R<AiWorkflowResponse> detail(@PathVariable String workflowNo) {
        return R.OK(workflowService.get(workflowNo));
    }

    @PostMapping("/{workflowNo}/pause")
    @PreAuthorize("hasAuthority('" + PermissionCodes.AI_WORKFLOW_MANAGE + "')")
    public R<AiWorkflowResponse> pause(@PathVariable String workflowNo,
                                       @Valid @RequestBody(required = false) AiWorkflowActionRequest request) {
        return R.OK(workflowService.pause(workflowNo, request));
    }

    @PostMapping("/{workflowNo}/resume")
    @PreAuthorize("hasAuthority('" + PermissionCodes.AI_WORKFLOW_MANAGE + "')")
    public R<AiWorkflowResponse> resume(@PathVariable String workflowNo) {
        return R.OK(workflowService.resume(workflowNo));
    }

    @PostMapping("/{workflowNo}/cancel")
    @PreAuthorize("hasAuthority('" + PermissionCodes.AI_WORKFLOW_MANAGE + "')")
    public R<AiWorkflowResponse> cancel(@PathVariable String workflowNo,
                                        @Valid @RequestBody(required = false) AiWorkflowActionRequest request) {
        return R.OK(workflowService.cancel(workflowNo, request));
    }

    @PostMapping("/{workflowNo}/fail")
    @PreAuthorize("hasAuthority('" + PermissionCodes.AI_WORKFLOW_MANAGE + "')")
    public R<AiWorkflowResponse> fail(@PathVariable String workflowNo,
                                      @Valid @RequestBody(required = false) AiWorkflowActionRequest request) {
        return R.OK(workflowService.fail(workflowNo, request));
    }
}
