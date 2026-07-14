package com.autodealer.crm.shared.infrastructure.time;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * 统一提供服务端可信时钟，避免审计时间接受业务调用方输入。
 */
@Configuration
public class TimeConfig {
    @Bean
    public Clock businessClock() {
        return Clock.systemDefaultZone();
    }
}
