package com.fininsight.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CustomCacheErrorHandlerTest {

    private CustomCacheErrorHandler errorHandler;
    private Cache cache;

    @BeforeEach
    public void setUp() {
        errorHandler = new CustomCacheErrorHandler();
        cache = mock(Cache.class);
        when(cache.getName()).thenReturn("analytics:summary");
    }

    @Test
    @DisplayName("handleCacheGetError suppresses exception and allows graceful degradation")
    void testHandleCacheGetErrorDoesNotThrow() {
        RuntimeException ex = new RuntimeException("Redis connection refused");
        assertThatCode(() -> errorHandler.handleCacheGetError(ex, cache, "key-123"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("handleCachePutError suppresses exception")
    void testHandleCachePutErrorDoesNotThrow() {
        RuntimeException ex = new RuntimeException("Redis write timeout");
        assertThatCode(() -> errorHandler.handleCachePutError(ex, cache, "key-123", "value"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("handleCacheEvictError suppresses exception")
    void testHandleCacheEvictErrorDoesNotThrow() {
        RuntimeException ex = new RuntimeException("Redis evict timeout");
        assertThatCode(() -> errorHandler.handleCacheEvictError(ex, cache, "key-123"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("handleCacheClearError suppresses exception")
    void testHandleCacheClearErrorDoesNotThrow() {
        RuntimeException ex = new RuntimeException("Redis clear timeout");
        assertThatCode(() -> errorHandler.handleCacheClearError(ex, cache))
                .doesNotThrowAnyException();
    }
}
