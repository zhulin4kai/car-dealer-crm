package com.autodealer.crm.manager;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.dao.DataAccessResourceFailureException;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
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
    void keyPresenceDistinguishesAbsentFromInfrastructureFailure() {
        when(redisTemplate.hasKey("present")).thenReturn(true);
        when(redisTemplate.hasKey("absent")).thenReturn(false);
        when(redisTemplate.hasKey("failed")).thenThrow(new DataAccessResourceFailureException("redis"));

        assertEquals(RedisManager.KeyPresence.PRESENT, redisManager.keyPresence("present"));
        assertEquals(RedisManager.KeyPresence.ABSENT, redisManager.keyPresence("absent"));
        assertEquals(RedisManager.KeyPresence.UNAVAILABLE, redisManager.keyPresence("failed"));
    }

    @Test
    void incrementSlidingWindowReturnsTransactionalCounterAndFailsClosed() {
        when(redisTemplate.execute(any(SessionCallback.class)))
                .thenReturn(List.of(3L, true))
                .thenThrow(new DataAccessResourceFailureException("redis"));

        assertEquals(3L, redisManager.incrementSlidingWindow("security-key", 900));
        assertNull(redisManager.incrementSlidingWindow("security-key", 900));
    }

    @Test
    void testDeletePattern() {
        Set<String> keys = new HashSet<>(Arrays.asList("key1", "key2", "key3"));
        when(redisTemplate.execute(any(RedisCallback.class))).thenReturn(keys);

        redisManager.deletePattern("pattern*");

        verify(redisTemplate).delete(keys);
    }

    @Test
    void testDeletePatternWithNoKeys() {
        when(redisTemplate.execute(any(RedisCallback.class))).thenReturn(Collections.emptySet());

        redisManager.deletePattern("pattern*");

        verify(redisTemplate, never()).delete(anyCollection());
    }

}
