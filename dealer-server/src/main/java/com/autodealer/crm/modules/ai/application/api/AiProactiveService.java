package com.autodealer.crm.modules.ai.application.api;

import com.autodealer.crm.modules.ai.application.api.dto.AiProactiveEventResponse;
import com.autodealer.crm.modules.ai.application.api.dto.AiProactiveSubscriptionResponse;
import com.autodealer.crm.modules.ai.application.api.dto.CreateAiProactiveSubscriptionRequest;

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
