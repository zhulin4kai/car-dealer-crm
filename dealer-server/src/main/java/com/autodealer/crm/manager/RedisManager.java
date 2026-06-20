package com.autodealer.crm.manager;

import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
public class RedisManager {

    private static final Logger log = LoggerFactory.getLogger(RedisManager.class);

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 获取缓存值，自动转换为指定类型。
     *
     * @param key 缓存键
     * @return 缓存值，若不存在返回 null
     */
    public <T> T get(String key) {
        if (key == null) {
            log.warn("Redis get: key 不能为 null");
            return null;
        }
        try {
            return (T) redisTemplate.opsForValue().get(key);
        } catch (DataAccessException e) {
            log.error("Redis get 异常, key: {}", key, e);
            return null;
        }
    }

    /** 设置缓存值，并指定过期时间。
     *
     * @param key     缓存键
     * @param value   缓存值
     * @return 是否设置成功
     */
    public boolean set(String key, Object value) {
        if (key == null) {
            log.warn("Redis set: key 不能为 null");
            return false;
        }
        try {
            redisTemplate.opsForValue().set(key, value);
            return true;
        } catch (DataAccessException e) {
            log.error("Redis set 异常, key: {}", key, e);
            return false;
        }
    }

    /**
     * 设置缓存值，并指定过期时间。
     *
     * @param key     缓存键
     * @param value   缓存值
     * @param seconds 过期时间（秒）
     * @return 是否设置成功
     */
    public boolean set(String key, Object value, long seconds) {
        if (key == null) {
            log.warn("Redis set: key 不能为 null");
            return false;
        }
        if (seconds <= 0) {
            log.warn("Redis set: seconds 必须大于 0, key: {}", key);
            return false;
        }
        try {
            redisTemplate.opsForValue().set(key, value, seconds, TimeUnit.SECONDS);
            return true;
        } catch (DataAccessException e) {
            log.error("Redis set 异常, key: {}", key, e);
            return false;
        }
    }

    /**
     * 删除指定缓存。
     *
     * @param key 缓存键
     * @return 是否删除成功
     */
    public boolean delete(String key) {
        if (key == null) {
            return false;
        }
        try {
            return redisTemplate.delete(key);
        } catch (DataAccessException e) {
            log.error("Redis delete 异常, key: {}", key, e);
            return false;
        }
    }

    /**
     * 根据模式批量删除缓存，支持通配符（如 "user:*"）。
     * 使用 SCAN 代替 KEYS，避免阻塞 Redis。
     *
     * @param pattern 匹配模式
     */
    public void deletePattern(String pattern) {
        if (pattern == null || pattern.isEmpty()) {
            log.warn("Redis deletePattern: pattern 不能为空");
            return;
        }
        try {
            Set<String> keys = redisTemplate.execute((RedisCallback<Set<String>>) connection -> {
                Set<String> result = new java.util.HashSet<>();
                try (Cursor<byte[]> cursor = connection.scan(ScanOptions.scanOptions().match(pattern).count(1000).build())) {
                    while (cursor.hasNext()) {
                        result.add(new String(cursor.next()));
                    }
                }
                return result;
            });
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.debug("Redis deletePattern: 删除 {} 个 key, pattern: {}", keys.size(), pattern);
            }
        } catch (DataAccessException e) {
            log.error("Redis deletePattern 异常, pattern: {}", pattern, e);
        }
    }

    /**
     * 获取 List 类型缓存，返回完整列表。
     *
     * @param key 缓存键
     * @return List 数据
     */
    public Object getList(String key) {
        return redisTemplate.opsForList().range(key, 0, -1);
    }

    /**
     * 设置 List 类型缓存，将集合数据从左侧批量写入。
     *
     * @param key  缓存键
     * @param data 集合数据
     * @return 写入后的列表长度
     */
    public <T> Object setList(String key, Collection<T> data) {
        Object[] t = new Object[data.size()];
        data.toArray(t);
        return redisTemplate.opsForList().leftPushAll(key, t);
    }

    /**
     * 判断缓存是否存在。
     *
     * @param key 缓存键
     * @return 是否存在
     */
    public boolean hasKey(String key) {
        if (key == null) {
            return false;
        }
        try {
            return redisTemplate.hasKey(key);
        } catch (DataAccessException e) {
            log.error("Redis hasKey 异常, key: {}", key, e);
            return false;
        }
    }

    /**
     * 获取缓存剩余过期时间（秒）。
     *
     * @param key 缓存键
     * @return 剩余秒数，-1 表示永不过期，-2 表示不存在或已过期
     */
    public long getExpire(String key) {
        if (key == null) {
            return -2;
        }
        Long expire = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        return expire != null ? expire : -2;
    }

    /**
     * 设置或刷新指定缓存的过期时间。
     *
     * @param key     缓存键
     * @param seconds 过期时间（秒）
     * @return 是否设置成功
     */
    public boolean expire(String key, long seconds) {
        if (key == null) {
            log.warn("Redis expire: key 不能为 null");
            return false;
        }
        if (seconds <= 0) {
            log.warn("Redis expire: seconds 必须大于 0, key: {}", key);
            return false;
        }
        try {
            return redisTemplate.expire(key, seconds, TimeUnit.SECONDS);
        } catch (DataAccessException e) {
            log.error("Redis expire 异常, key: {}", key, e);
            return false;
        }
    }
}
