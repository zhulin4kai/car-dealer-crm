package com.bjpowernode.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * 测试配置，提供Redis相关组件
 */
@TestConfiguration
public class TestConfig {
    
    /**
     * 提供一个Redis连接工厂
     */
    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        // 创建一个用于测试的Redis连接工厂
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName("localhost");
        config.setPort(56379); // 使用一个不太可能被占用的端口
        LettuceConnectionFactory factory = new LettuceConnectionFactory(config);
        // 不自动初始化连接
        factory.setShareNativeConnection(false);
        factory.setValidateConnection(false);
        return factory;
    }
    
    /**
     * 提供RedisTemplate
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setEnableTransactionSupport(false);
        template.afterPropertiesSet();
        return template;
    }
} 