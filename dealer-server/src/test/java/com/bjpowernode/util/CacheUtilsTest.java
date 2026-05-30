package com.bjpowernode.util;

import org.junit.jupiter.api.Test;

import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CacheUtilsTest {

    @Test
    void testGetCacheDataReturnsCachedValue() {
        Supplier<String> cacheSelector = () -> "cachedValue";
        Supplier<String> databaseSelector = () -> "dbValue";
        Consumer<String> cacheSave = mock(Consumer.class);

        String result = CacheUtils.getCacheData(cacheSelector, databaseSelector, cacheSave);

        assertEquals("cachedValue", result);
        verify(cacheSave, never()).accept(any());
    }

    @Test
    void testGetCacheDataReturnsDbValueWhenCacheEmpty() {
        Supplier<String> cacheSelector = () -> null;
        Supplier<String> databaseSelector = () -> "dbValue";
        Consumer<String> cacheSave = mock(Consumer.class);

        String result = CacheUtils.getCacheData(cacheSelector, databaseSelector, cacheSave);

        assertEquals("dbValue", result);
        verify(cacheSave).accept("dbValue");
    }

    @Test
    void testGetCacheDataReturnsNullWhenBothEmpty() {
        Supplier<String> cacheSelector = () -> null;
        Supplier<String> databaseSelector = () -> null;
        Consumer<String> cacheSave = mock(Consumer.class);

        String result = CacheUtils.getCacheData(cacheSelector, databaseSelector, cacheSave);

        assertNull(result);
        verify(cacheSave, never()).accept(any());
    }

    @Test
    void testGenerateKeyWithMultipleParams() {
        String key = CacheUtils.generateKey("prefix", "module", "func", 123);

        assertEquals("prefix:module:func:123", key);
    }

    @Test
    void testGenerateKeyWithNullParams() {
        String key = CacheUtils.generateKey("prefix", null, "func");

        assertEquals("prefix:func", key);
    }

    @Test
    void testGenerateKeyWithSingleParam() {
        String key = CacheUtils.generateKey("single");

        assertEquals("single", key);
    }

    @Test
    void testGenerateKeyWithNoParams() {
        String key = CacheUtils.generateKey();

        assertEquals("", key);
    }
}
