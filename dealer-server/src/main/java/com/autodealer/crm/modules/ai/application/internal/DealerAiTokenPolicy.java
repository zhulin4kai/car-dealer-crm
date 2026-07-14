package com.autodealer.crm.modules.ai.application.internal;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Set;

@Component
public class DealerAiTokenPolicy implements InitializingBean {
    private static final String DEFAULT_TOKEN = "dev-internal-token";
    private static final Set<String> LOCAL_ENVIRONMENTS = Set.of("local", "dev", "test", "smoke");

    private final String environment;
    private final String internalToken;
    private final String toolToken;

    public DealerAiTokenPolicy(@Value("${ai.dealer-ai.environment:${DEALER_AI_ENV:${APP_ENV:local}}}") String environment,
                               @Value("${ai.dealer-ai.internal-token:}") String internalToken,
                               @Value("${ai.dealer-ai.tool-token:${ai.dealer-ai.internal-token:}}") String toolToken) {
        this.environment = environment;
        this.internalToken = internalToken;
        this.toolToken = toolToken;
    }

    @Override
    public void afterPropertiesSet() {
        if (isLocalEnvironment(environment)) {
            return;
        }
        requireExplicitSecret("DEALER_AI_INTERNAL_TOKEN", internalToken);
        requireExplicitSecret("DEALER_AI_TOOL_TOKEN", toolToken);
    }

    private boolean isLocalEnvironment(String value) {
        return value != null && LOCAL_ENVIRONMENTS.contains(value.trim().toLowerCase());
    }

    private void requireExplicitSecret(String name, String value) {
        if (!StringUtils.hasText(value) || DEFAULT_TOKEN.equals(value)) {
            throw new IllegalStateException(name + " must be explicitly configured outside local environments");
        }
    }
}
