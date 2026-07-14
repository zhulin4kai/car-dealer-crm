package com.autodealer.crm.modules.ai.application.internal;

import java.util.concurrent.atomic.AtomicBoolean;

public class AiRunCancellationToken {
    private final AtomicBoolean cancelled = new AtomicBoolean(false);

    public void cancel() {
        cancelled.set(true);
    }

    public boolean isCancelled() {
        return cancelled.get();
    }
}
