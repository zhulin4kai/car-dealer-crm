package com.bjpowernode.manager;

import com.bjpowernode.model.TUser;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Objects;

@Component
public class RedisManager {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

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
