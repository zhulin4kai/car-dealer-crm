package com.autodealer.crm.manager;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisManagerTest {

    @InjectMocks
    private RedisManager redisManager;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private ListOperations<String, Object> listOperations;

    @Test
    void testGet() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("testKey")).thenReturn("testValue");

        Object result = redisManager.get("testKey");

        assertEquals("testValue", result);
    }

    @Test
    void testSet() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        redisManager.set("testKey", "testValue", 60);

        verify(valueOperations).set("testKey", "testValue", 60, TimeUnit.SECONDS);
    }

    @Test
    void testDelete() {
        redisManager.delete("testKey");

        verify(redisTemplate).delete("testKey");
    }

    @Test
    void testDeletePattern() {
        Set<String> keys = new HashSet<>(Arrays.asList("key1", "key2", "key3"));
        when(redisTemplate.keys("pattern*")).thenReturn(keys);

        redisManager.deletePattern("pattern*");

        verify(redisTemplate).delete(keys);
    }

    @Test
    void testDeletePatternWithNoKeys() {
        when(redisTemplate.keys("pattern*")).thenReturn(Collections.emptySet());

        redisManager.deletePattern("pattern*");

        verify(redisTemplate, never()).delete(anyCollection());
    }

    @Test
    void testGetValue() {
        when(redisTemplate.opsForList()).thenReturn(listOperations);
        when(listOperations.range("testKey", 0, -1)).thenReturn(Arrays.asList("item1", "item2"));

        Object result = redisManager.getValue("testKey");

        assertNotNull(result);
    }

    @Test
    void testSetValue() {
        when(redisTemplate.opsForList()).thenReturn(listOperations);
        Collection<String> data = Arrays.asList("item1", "item2");

        redisManager.setValue("testKey", data);

        verify(listOperations).leftPushAll(eq("testKey"), any(Object[].class));
    }
}
