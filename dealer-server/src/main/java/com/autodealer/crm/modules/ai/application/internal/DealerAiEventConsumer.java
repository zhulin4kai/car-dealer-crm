package com.autodealer.crm.modules.ai.application.internal;

import com.autodealer.crm.modules.ai.application.api.dto.DealerAiEventResponse;

@FunctionalInterface
public interface DealerAiEventConsumer {
    void accept(DealerAiEventResponse event);
}
