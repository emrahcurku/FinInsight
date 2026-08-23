package com.fininsight.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.CacheErrorHandler;

/**
 * Custom cache error handler that catches and logs Redis connectivity or deserialization
 * errors without disrupting business logic or propagating 500 errors to clients.
 * Guarantees graceful degradation to direct database execution.
 */
@Slf4j
public class CustomCacheErrorHandler implements CacheErrorHandler {

    @Override
    public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
        log.warn("Redis cache GET failed for cache '{}' and key '{}': {}. Falling back to database.",
                cache != null ? cache.getName() : "unknown", key, exception.getMessage());
    }

    @Override
    public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
        log.warn("Redis cache PUT failed for cache '{}' and key '{}': {}. Skipping cache write.",
                cache != null ? cache.getName() : "unknown", key, exception.getMessage());
    }

    @Override
    public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
        log.warn("Redis cache EVICT failed for cache '{}' and key '{}': {}. Invalidation skipped.",
                cache != null ? cache.getName() : "unknown", key, exception.getMessage());
    }

    @Override
    public void handleCacheClearError(RuntimeException exception, Cache cache) {
        log.warn("Redis cache CLEAR failed for cache '{}': {}. Invalidation skipped.",
                cache != null ? cache.getName() : "unknown", exception.getMessage());
    }
}
