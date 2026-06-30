package com.autodealer.crm.web;

import com.autodealer.crm.ai.dto.AiProactiveEventResponse;
import com.autodealer.crm.ai.dto.AiProactiveSubscriptionResponse;
import com.autodealer.crm.ai.dto.CreateAiProactiveSubscriptionRequest;
import com.autodealer.crm.ai.service.AiProactiveService;
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
@RequestMapping("/api/ai/proactive")
public class AiProactiveController {
    private final AiProactiveService proactiveService;

    public AiProactiveController(AiProactiveService proactiveService) {
        this.proactiveService = proactiveService;
    }

    @PostMapping("/subscriptions")
    @PreAuthorize("hasAuthority('" + PermissionCodes.AI_PROACTIVE_USE + "')")
    public R<AiProactiveSubscriptionResponse> createSubscription(
            @Valid @RequestBody CreateAiProactiveSubscriptionRequest request) {
        return R.OK(proactiveService.createSubscription(request));
    }

    @GetMapping("/subscriptions")
    @PreAuthorize("hasAuthority('" + PermissionCodes.AI_PROACTIVE_VIEW + "')")
    public R<List<AiProactiveSubscriptionResponse>> listSubscriptions() {
        return R.OK(proactiveService.listSubscriptions());
    }

    @GetMapping("/subscriptions/{subscriptionNo}")
    @PreAuthorize("hasAuthority('" + PermissionCodes.AI_PROACTIVE_VIEW + "')")
    public R<AiProactiveSubscriptionResponse> getSubscription(@PathVariable String subscriptionNo) {
        return R.OK(proactiveService.getSubscription(subscriptionNo));
    }

    @PostMapping("/subscriptions/{subscriptionNo}/pause")
    @PreAuthorize("hasAuthority('" + PermissionCodes.AI_PROACTIVE_USE + "')")
    public R<AiProactiveSubscriptionResponse> pauseSubscription(@PathVariable String subscriptionNo) {
        return R.OK(proactiveService.pauseSubscription(subscriptionNo));
    }

    @PostMapping("/subscriptions/{subscriptionNo}/resume")
    @PreAuthorize("hasAuthority('" + PermissionCodes.AI_PROACTIVE_USE + "')")
    public R<AiProactiveSubscriptionResponse> resumeSubscription(@PathVariable String subscriptionNo) {
        return R.OK(proactiveService.resumeSubscription(subscriptionNo));
    }

    @PostMapping("/subscriptions/{subscriptionNo}/cancel")
    @PreAuthorize("hasAuthority('" + PermissionCodes.AI_PROACTIVE_USE + "')")
    public R<AiProactiveSubscriptionResponse> cancelSubscription(@PathVariable String subscriptionNo) {
        return R.OK(proactiveService.cancelSubscription(subscriptionNo));
    }

    @GetMapping("/events")
    @PreAuthorize("hasAuthority('" + PermissionCodes.AI_PROACTIVE_VIEW + "')")
    public R<List<AiProactiveEventResponse>> listEvents(@RequestParam(defaultValue = "1") int page,
                                                        @RequestParam(defaultValue = "20") int size) {
        return R.OK(proactiveService.listEvents(page, size));
    }

    @GetMapping("/events/{eventNo}")
    @PreAuthorize("hasAuthority('" + PermissionCodes.AI_PROACTIVE_VIEW + "')")
    public R<AiProactiveEventResponse> getEvent(@PathVariable String eventNo) {
        return R.OK(proactiveService.getEvent(eventNo));
    }

    @PostMapping("/events/generate")
    @PreAuthorize("hasAuthority('" + PermissionCodes.AI_PROACTIVE_USE + "')")
    public R<List<AiProactiveEventResponse>> generateDueEvents() {
        return R.OK(proactiveService.generateDueEvents());
    }
}
