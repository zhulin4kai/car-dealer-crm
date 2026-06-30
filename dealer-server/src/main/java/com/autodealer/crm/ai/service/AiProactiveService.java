package com.autodealer.crm.ai.service;

import com.autodealer.crm.ai.dto.AiProactiveEventResponse;
import com.autodealer.crm.ai.dto.AiProactiveSubscriptionResponse;
import com.autodealer.crm.ai.dto.CreateAiProactiveSubscriptionRequest;

import java.util.List;

public interface AiProactiveService {
    AiProactiveSubscriptionResponse createSubscription(CreateAiProactiveSubscriptionRequest request);

    List<AiProactiveSubscriptionResponse> listSubscriptions();

    AiProactiveSubscriptionResponse getSubscription(String subscriptionNo);

    AiProactiveSubscriptionResponse pauseSubscription(String subscriptionNo);

    AiProactiveSubscriptionResponse resumeSubscription(String subscriptionNo);

    AiProactiveSubscriptionResponse cancelSubscription(String subscriptionNo);

    List<AiProactiveEventResponse> listEvents(int page, int size);

    AiProactiveEventResponse getEvent(String eventNo);

    List<AiProactiveEventResponse> generateDueEvents();
}
