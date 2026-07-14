package com.autodealer.crm.integration;

import com.autodealer.crm.shared.infrastructure.cache.RedisManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Optional verification against a real Redis instance.
 *
 * Run only when CRM_REAL_REDIS_HOST is provided, for example:
 * CRM_REAL_REDIS_HOST=127.0.0.1 CRM_REAL_REDIS_PORT=16379 \
 * ./mvnw -Dtest=ExternalRedisVerificationTest test
 */
@EnabledIfEnvironmentVariable(named = "CRM_REAL_REDIS_HOST", matches = ".+")
class ExternalRedisVerificationTest {

    @Test
    @DisplayName("真实 Redis 必须支持普通缓存 TTL、删除和模式失效")
    void realRedisMustSupportTtlDeleteAndPatternEviction() {
        LettuceConnectionFactory factory = connectionFactory();
        try {
            RedisManager redisManager = redisManager(factory);
            String prefix = "cdrm:external-verification:" + UUID.randomUUID() + ":";
            String key = prefix + "single";
            String keyA = prefix + "a";
            String keyB = prefix + "b";

            assertTrue(redisManager.set(key, "value", 60));
            assertEquals("value", redisManager.get(key));
            long ttl = redisManager.getExpire(key);
            assertTrue(ttl > 0 && ttl <= 60, "真实 Redis 未设置 TTL");
            assertTrue(redisManager.delete(key));
            assertFalse(redisManager.hasKey(key));

            assertTrue(redisManager.set(keyA, "1", 60));
            assertTrue(redisManager.set(keyB, "2", 60));
            assertTrue(redisManager.deletePattern(prefix + "*"));
            assertFalse(redisManager.hasKey(keyA));
            assertFalse(redisManager.hasKey(keyB));
        } finally {
            factory.destroy();
        }
    }

    private LettuceConnectionFactory connectionFactory() {
        String host = System.getenv("CRM_REAL_REDIS_HOST");
        int port = Integer.parseInt(System.getenv().getOrDefault("CRM_REAL_REDIS_PORT", "6379"));
        int database = Integer.parseInt(System.getenv().getOrDefault("CRM_REAL_REDIS_DATABASE", "0"));
        RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration(host, port);
        configuration.setDatabase(database);
        String password = System.getenv("CRM_REAL_REDIS_PASSWORD");
        if (password != null && !password.isBlank()) {
            configuration.setPassword(password);
        }
        LettuceConnectionFactory factory = new LettuceConnectionFactory(configuration);
        factory.afterPropertiesSet();
        return factory;
    }

    private RedisManager redisManager(LettuceConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        template.afterPropertiesSet();

        RedisManager redisManager = new RedisManager();
        ReflectionTestUtils.setField(redisManager, "redisTemplate", template);
        return redisManager;
    }
}
