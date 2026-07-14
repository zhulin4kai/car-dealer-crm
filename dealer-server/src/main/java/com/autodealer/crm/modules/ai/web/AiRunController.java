package com.autodealer.crm.modules.ai.web;

import com.autodealer.crm.modules.ai.application.api.dto.AiRunResponse;
import com.autodealer.crm.modules.ai.application.api.dto.AiRunTraceResponse;
import com.autodealer.crm.modules.ai.application.api.dto.CancelAiRunRequest;
import com.autodealer.crm.modules.ai.application.api.dto.CreateAiRunRequest;
import com.autodealer.crm.modules.ai.application.api.AiConversationService;
import com.autodealer.crm.shared.security.PermissionCodes;
import com.autodealer.crm.shared.web.Result;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
    public Result<AiRunResponse> create(@Valid @RequestBody CreateAiRunRequest request) {
        return Result.OK(aiConversationService.createRun(request));
    }

    @GetMapping("/{runNo}")
    @PreAuthorize("hasAuthority('" + PermissionCodes.AI_RUN_VIEW + "')")
    public Result<AiRunResponse> detail(@PathVariable String runNo) {
        return Result.OK(aiConversationService.getRun(runNo));
    }

    @GetMapping("/{runNo}/trace")
    @PreAuthorize("hasAuthority('" + PermissionCodes.AI_RUN_VIEW + "')")
    public Result<AiRunTraceResponse> trace(@PathVariable String runNo) {
        return Result.OK(aiConversationService.getRunTrace(runNo));
    }

    @GetMapping(value = "/{runNo}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("hasAuthority('" + PermissionCodes.AI_USE + "')")
    public SseEmitter events(@PathVariable String runNo,
                             @RequestParam(defaultValue = "0") int afterSequence) {
        return aiConversationService.streamRun(runNo, afterSequence);
    }

    @PostMapping("/{runNo}/cancel")
    @PreAuthorize("hasAuthority('" + PermissionCodes.AI_USE + "')")
    public Result<AiRunResponse> cancel(@PathVariable String runNo,
                                   @Valid @RequestBody(required = false) CancelAiRunRequest request) {
        return Result.OK(aiConversationService.cancelRun(runNo, request));
    }
}
