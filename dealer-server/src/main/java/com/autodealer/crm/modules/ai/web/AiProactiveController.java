package com.autodealer.crm.modules.ai.web;

import com.autodealer.crm.modules.ai.application.api.dto.AiProactiveEventResponse;
import com.autodealer.crm.modules.ai.application.api.dto.AiProactiveSubscriptionResponse;
import com.autodealer.crm.modules.ai.application.api.dto.CreateAiProactiveSubscriptionRequest;
import com.autodealer.crm.modules.ai.application.api.AiProactiveService;
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
@RequestMapping("/api/ai/proactive")
public class AiProactiveController {
    private final AiProactiveService proactiveService;

    public AiProactiveController(AiProactiveService proactiveService) {
        this.proactiveService = proactiveService;
    }

    @PostMapping("/subscriptions")
    @PreAuthorize("hasAuthority('" + PermissionCodes.AI_PROACTIVE_USE + "')")
    public Result<AiProactiveSubscriptionResponse> createSubscription(
            @Valid @RequestBody CreateAiProactiveSubscriptionRequest request) {
        return Result.OK(proactiveService.createSubscription(request));
    }

    @GetMapping("/subscriptions")
    @PreAuthorize("hasAuthority('" + PermissionCodes.AI_PROACTIVE_VIEW + "')")
    public Result<List<AiProactiveSubscriptionResponse>> listSubscriptions() {
        return Result.OK(proactiveService.listSubscriptions());
    }

    @GetMapping("/subscriptions/{subscriptionNo}")
    @PreAuthorize("hasAuthority('" + PermissionCodes.AI_PROACTIVE_VIEW + "')")
    public Result<AiProactiveSubscriptionResponse> getSubscription(@PathVariable String subscriptionNo) {
        return Result.OK(proactiveService.getSubscription(subscriptionNo));
    }

    @PostMapping("/subscriptions/{subscriptionNo}/pause")
    @PreAuthorize("hasAuthority('" + PermissionCodes.AI_PROACTIVE_USE + "')")
    public Result<AiProactiveSubscriptionResponse> pauseSubscription(@PathVariable String subscriptionNo) {
        return Result.OK(proactiveService.pauseSubscription(subscriptionNo));
    }

    @PostMapping("/subscriptions/{subscriptionNo}/resume")
    @PreAuthorize("hasAuthority('" + PermissionCodes.AI_PROACTIVE_USE + "')")
    public Result<AiProactiveSubscriptionResponse> resumeSubscription(@PathVariable String subscriptionNo) {
        return Result.OK(proactiveService.resumeSubscription(subscriptionNo));
    }

    @PostMapping("/subscriptions/{subscriptionNo}/cancel")
    @PreAuthorize("hasAuthority('" + PermissionCodes.AI_PROACTIVE_USE + "')")
    public Result<AiProactiveSubscriptionResponse> cancelSubscription(@PathVariable String subscriptionNo) {
        return Result.OK(proactiveService.cancelSubscription(subscriptionNo));
    }

    @GetMapping("/events")
    @PreAuthorize("hasAuthority('" + PermissionCodes.AI_PROACTIVE_VIEW + "')")
    public Result<List<AiProactiveEventResponse>> listEvents(@RequestParam(defaultValue = "1") int page,
                                                        @RequestParam(defaultValue = "20") int size) {
        return Result.OK(proactiveService.listEvents(page, size));
    }

    @GetMapping("/events/{eventNo}")
    @PreAuthorize("hasAuthority('" + PermissionCodes.AI_PROACTIVE_VIEW + "')")
    public Result<AiProactiveEventResponse> getEvent(@PathVariable String eventNo) {
        return Result.OK(proactiveService.getEvent(eventNo));
    }

    @PostMapping("/events/generate")
    @PreAuthorize("hasAuthority('" + PermissionCodes.AI_PROACTIVE_USE + "')")
    public Result<List<AiProactiveEventResponse>> generateDueEvents() {
        return Result.OK(proactiveService.generateDueEvents());
    }
}
