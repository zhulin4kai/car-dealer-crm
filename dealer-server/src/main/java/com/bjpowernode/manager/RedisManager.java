package com.bjpowernode.manager;

import com.bjpowernode.model.TUser;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import java.util.Collection;
import java.util.Objects;

@Component
public class RedisManager {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 获取缓存值
     */
    public <T> T get(String key) {
        return (T) redisTemplate.opsForValue().get(key);
    }

    /**
     * 设置缓存值
     */
    public void set(String key, Object value, long seconds) {
        redisTemplate.opsForValue().set(key, value, seconds, TimeUnit.SECONDS);
    }

    /**
     * 删除缓存
     */
    public void delete(String key) {
        redisTemplate.delete(key);
    }

    /**
     * 模式删除缓存
     */
    public void deletePattern(String pattern) {
        Set<String> keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    public Object getValue(String key) {
        //string
        //list
        //hash
        //set
        //zset
        return redisTemplate.opsForList().range(key, 0, -1);
    }

    public <T> Object setValue(String key,  Collection<T> data) {
        //string
        //list
        //hash
        //set
        //zset
        Object[] t  = new Object[data.size()];
        data.toArray(t);
        return redisTemplate.opsForList().leftPushAll(key, t);
    }
}
