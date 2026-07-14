package com.autodealer.crm.shared.security.config;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

@Component
public class JwtSecretValidator {

    @PostConstruct
    public void validateJwtSecret() {
        String secret = System.getenv("JWT_SECRET");
        if (secret == null || secret.trim().isEmpty()) {
            throw new IllegalStateException("JWT_SECRET 环境变量未配置，请先设置后再启动后端服务");
        }
    }
}
