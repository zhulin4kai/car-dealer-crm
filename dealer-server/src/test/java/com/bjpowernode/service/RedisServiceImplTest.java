package com.bjpowernode.service;

import com.bjpowernode.service.impl.RedisServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisServiceImplTest {

    @InjectMocks
    private RedisServiceImpl redisService;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Test
    void testSetValue() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        redisService.setValue("testKey", "testValue");

        verify(valueOperations).set("testKey", "testValue");
    }

    @Test
    void testSetValueWithObject() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        redisService.setValue("user:1", 12345);

        verify(valueOperations).set("user:1", 12345);
    }

    @Test
    void testGetValue() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("testKey")).thenReturn("testValue");

        Object result = redisService.getValue("testKey");

        assertEquals("testValue", result);
    }

    @Test
    void testGetValueNotFound() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("nonExistentKey")).thenReturn(null);

        Object result = redisService.getValue("nonExistentKey");

        assertNull(result);
    }

    @Test
    void testRemoveValue() {
        when(redisTemplate.delete("testKey")).thenReturn(true);

        Boolean result = redisService.removeValue("testKey");

        assertTrue(result);
        verify(redisTemplate).delete("testKey");
    }

    @Test
    void testRemoveValueNotFound() {
        when(redisTemplate.delete("nonExistentKey")).thenReturn(false);

        Boolean result = redisService.removeValue("nonExistentKey");

        assertFalse(result);
    }

    @Test
    void testExpire() {
        when(redisTemplate.expire("testKey", 60L, TimeUnit.SECONDS)).thenReturn(true);

        Boolean result = redisService.expire("testKey", 60L, TimeUnit.SECONDS);

        assertTrue(result);
        verify(redisTemplate).expire("testKey", 60L, TimeUnit.SECONDS);
    }

    @Test
    void testExpireWithDifferentTimeUnit() {
        when(redisTemplate.expire("testKey", 1L, TimeUnit.HOURS)).thenReturn(true);

        Boolean result = redisService.expire("testKey", 1L, TimeUnit.HOURS);

        assertTrue(result);
        verify(redisTemplate).expire("testKey", 1L, TimeUnit.HOURS);
    }
}
