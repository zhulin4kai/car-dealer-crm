package com.autodealer.crm.shared.infrastructure.cache;

import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class RedisManager {

    public enum KeyPresence {
        PRESENT,
        ABSENT,
        UNAVAILABLE
    }

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
    public boolean deletePattern(String pattern) {
        if (pattern == null || pattern.isEmpty()) {
            log.warn("Redis deletePattern: pattern 不能为空");
            return false;
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
            return true;
        } catch (DataAccessException e) {
            log.error("Redis deletePattern 异常, pattern: {}", pattern, e);
            return false;
        }
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

    /**
     * 返回缓存键的三态存在性，供安全命令区分“键不存在”和“Redis 查询失败”。
     */
    public KeyPresence keyPresence(String key) {
        if (key == null) {
            return KeyPresence.UNAVAILABLE;
        }
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(key))
                    ? KeyPresence.PRESENT : KeyPresence.ABSENT;
        } catch (DataAccessException exception) {
            log.error("Redis keyPresence 异常, key: {}", key, exception);
            return KeyPresence.UNAVAILABLE;
        }
    }

    /** 用户会话索引使用 Redis Set，禁止 KEYS/SCAN 发现用户会话。 */
    public boolean addToSet(String key, String member, long seconds) {
        if (key == null || member == null || seconds <= 0) return false;
        try {
            Long added = redisTemplate.opsForSet().add(key, member);
            Boolean expires = redisTemplate.expire(key, seconds, TimeUnit.SECONDS);
            return added != null && Boolean.TRUE.equals(expires);
        } catch (DataAccessException exception) {
            log.error("Redis set add 异常, key: {}", key, exception);
            return false;
        }
    }

    public Set<String> setMembers(String key) {
        if (key == null) return null;
        try {
            Set<Object> values = redisTemplate.opsForSet().members(key);
            if (values == null) return null;
            Set<String> result = new java.util.LinkedHashSet<>();
            for (Object value : values) if (value != null) result.add(String.valueOf(value));
            return result;
        } catch (DataAccessException exception) {
            log.error("Redis set members 异常, key: {}", key, exception);
            return null;
        }
    }

    public boolean removeFromSet(String key, String member) {
        if (key == null || member == null) return false;
        try {
            Long removed = redisTemplate.opsForSet().remove(key, member);
            return removed != null && removed > 0;
        } catch (DataAccessException exception) {
            log.error("Redis set remove 异常, key: {}", key, exception);
            return false;
        }
    }

    /**
     * 在一个 Redis 事务内递增计数并刷新滑动窗口 TTL。
     *
     * @return 当前窗口计数；Redis 不可用或事务结果异常时返回 {@code null}，安全调用方必须 fail-close。
     */
    public Long incrementSlidingWindow(String key, long seconds) {
        if (key == null || seconds <= 0) return null;
        try {
            List<Object> result = redisTemplate.execute(new SessionCallback<>() {
                @Override
                @SuppressWarnings({"rawtypes", "unchecked"})
                public List<Object> execute(RedisOperations operations) {
                    operations.multi();
                    operations.opsForValue().increment(key);
                    operations.expire(key, seconds, TimeUnit.SECONDS);
                    return operations.exec();
                }
            });
            if (result == null || result.size() < 2 || !(result.get(0) instanceof Number number)
                    || !Boolean.TRUE.equals(result.get(1))) return null;
            return number.longValue();
        } catch (DataAccessException exception) {
            log.error("Redis security counter 异常", exception);
            return null;
        }
    }
}
