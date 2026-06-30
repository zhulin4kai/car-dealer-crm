package com.autodealer.crm.ai.service;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AiRunCancellationRegistry {
    private final Map<String, AiRunCancellationToken> tokens = new ConcurrentHashMap<>();

    public AiRunCancellationToken register(String runNo) {
        AiRunCancellationToken token = new AiRunCancellationToken();
        tokens.put(runNo, token);
        return token;
    }

    public void cancel(String runNo) {
        AiRunCancellationToken token = tokens.get(runNo);
        if (token != null) {
            token.cancel();
        }
    }

    public void unregister(String runNo, AiRunCancellationToken token) {
        tokens.remove(runNo, token);
    }
}
