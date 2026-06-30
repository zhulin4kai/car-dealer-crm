package com.autodealer.crm.ai.service;

import com.autodealer.crm.ai.dto.DealerAiEventResponse;

@FunctionalInterface
public interface DealerAiEventConsumer {
    void accept(DealerAiEventResponse event);
}
