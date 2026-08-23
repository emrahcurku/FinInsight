package com.fininsight.common.cache;

import com.fininsight.config.CacheNames;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Service responsible for targeted user-level cache invalidation.
 * Scans and evicts only the specific mutated user's cache namespace without
 * issuing global flushes or affecting other concurrent users.
 */
@Slf4j
@Service
public class CacheEvictionService {

    private final Optional<StringRedisTemplate> redisTemplate;
    private final CacheManager cacheManager;

    @Autowired
    public CacheEvictionService(
            @Autowired(required = false) StringRedisTemplate redisTemplate,
            CacheManager cacheManager
    ) {
        this.redisTemplate = Optional.ofNullable(redisTemplate);
        this.cacheManager = cacheManager;
    }

    /**
     * Evicts analytics, dashboard, and AI insight caches for a user when transactions are created/updated/deleted.
     */
    public void evictUserTransactionCaches(UUID userId) {
        if (userId == null) return;
        evictByPrefix(CacheNames.ANALYTICS_SUMMARY, userId);
        evictByPrefix(CacheNames.ANALYTICS_CATEGORY, userId);
        evictByPrefix(CacheNames.ANALYTICS_MONTHLY, userId);
        evictByPrefix(CacheNames.ANALYTICS_TOP_CATEGORIES, userId);
        evictByPrefix(CacheNames.DASHBOARD, userId);
        evictByPrefix(CacheNames.AI_INSIGHTS, userId);
    }

    /**
     * Evicts budget overview, dashboard, and AI insight caches for a user when budgets are created/updated/deleted.
     */
    public void evictUserBudgetCaches(UUID userId) {
        if (userId == null) return;
        evictByPrefix(CacheNames.ANALYTICS_BUDGET_OVERVIEW, userId);
        evictByPrefix(CacheNames.DASHBOARD, userId);
        evictByPrefix(CacheNames.AI_INSIGHTS, userId);
    }

    /**
     * Evicts category, dashboard, and AI insight caches for a user when categories are created/updated/deleted.
     */
    public void evictUserCategoryCaches(UUID userId) {
        if (userId == null) return;
        evictByPrefix(CacheNames.CATEGORIES, userId);
        evictByPrefix(CacheNames.DASHBOARD, userId);
        evictByPrefix(CacheNames.AI_INSIGHTS, userId);
    }

    private void evictByPrefix(String cacheName, UUID userId) {
        try {
            if (redisTemplate.isPresent()) {
                String pattern = cacheName + "::" + userId + "*";
                Set<String> keysToDelete = new HashSet<>();
                ScanOptions options = ScanOptions.scanOptions().match(pattern).count(100).build();

                try (Cursor<String> cursor = redisTemplate.get().scan(options)) {
                    while (cursor.hasNext()) {
                        keysToDelete.add(cursor.next());
                    }
                }

                if (!keysToDelete.isEmpty()) {
                    redisTemplate.get().delete(keysToDelete);
                    log.debug("Evicted {} keys matching pattern '{}'", keysToDelete.size(), pattern);
                }
            } else if (cacheManager != null && cacheManager.getCache(cacheName) != null) {
                // In non-Redis environments (e.g. test with simple cache), clear cache safely
                cacheManager.getCache(cacheName).clear();
            }
        } catch (Exception ex) {
            log.warn("Failed to evict cache '{}' for user '{}': {}. Invalidation gracefully skipped.",
                    cacheName, userId, ex.getMessage());
        }
    }
}
