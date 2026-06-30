package com.autodealer.crm.web;

import com.autodealer.crm.ai.dto.AiRunResponse;
import com.autodealer.crm.ai.dto.AiRunTraceResponse;
import com.autodealer.crm.ai.dto.CancelAiRunRequest;
import com.autodealer.crm.ai.dto.CreateAiRunRequest;
import com.autodealer.crm.ai.service.AiConversationService;
import com.autodealer.crm.constant.PermissionCodes;
import com.autodealer.crm.result.R;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/ai/runs")
public class AiRunController {
    private final AiConversationService aiConversationService;

    public AiRunController(AiConversationService aiConversationService) {
        this.aiConversationService = aiConversationService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('" + PermissionCodes.AI_USE + "')")
    public R<AiRunResponse> create(@Valid @RequestBody CreateAiRunRequest request) {
        return R.OK(aiConversationService.createRun(request));
    }

    @GetMapping("/{runNo}")
    @PreAuthorize("hasAuthority('" + PermissionCodes.AI_RUN_VIEW + "')")
    public R<AiRunResponse> detail(@PathVariable String runNo) {
        return R.OK(aiConversationService.getRun(runNo));
    }

    @GetMapping("/{runNo}/trace")
    @PreAuthorize("hasAuthority('" + PermissionCodes.AI_RUN_VIEW + "')")
    public R<AiRunTraceResponse> trace(@PathVariable String runNo) {
        return R.OK(aiConversationService.getRunTrace(runNo));
    }

    @GetMapping(value = "/{runNo}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("hasAuthority('" + PermissionCodes.AI_USE + "')")
    public SseEmitter events(@PathVariable String runNo) {
        return aiConversationService.streamRun(runNo);
    }

    @PostMapping("/{runNo}/cancel")
    @PreAuthorize("hasAuthority('" + PermissionCodes.AI_USE + "')")
    public R<AiRunResponse> cancel(@PathVariable String runNo,
                                   @Valid @RequestBody(required = false) CancelAiRunRequest request) {
        return R.OK(aiConversationService.cancelRun(runNo, request));
    }
}
