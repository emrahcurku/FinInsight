package com.fininsight.common.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CacheEvictionServiceTest {

    private CacheEvictionService evictionService;

    @BeforeEach
    public void setUp() {
        CacheManager cacheManager = mock(CacheManager.class);
        Cache mockCache = mock(Cache.class);
        when(cacheManager.getCache(anyString())).thenReturn(mockCache);
        evictionService = new CacheEvictionService(null, cacheManager);
    }

    @Test
    @DisplayName("evictUserTransactionCaches gracefully handles null or valid userId without throwing")
    void testEvictUserTransactionCaches() {
        UUID userId = UUID.randomUUID();
        assertThatCode(() -> evictionService.evictUserTransactionCaches(userId))
                .doesNotThrowAnyException();
        assertThatCode(() -> evictionService.evictUserTransactionCaches(null))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("evictUserBudgetCaches gracefully handles null or valid userId without throwing")
    void testEvictUserBudgetCaches() {
        UUID userId = UUID.randomUUID();
        assertThatCode(() -> evictionService.evictUserBudgetCaches(userId))
                .doesNotThrowAnyException();
        assertThatCode(() -> evictionService.evictUserBudgetCaches(null))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("evictUserCategoryCaches gracefully handles null or valid userId without throwing")
    void testEvictUserCategoryCaches() {
        UUID userId = UUID.randomUUID();
        assertThatCode(() -> evictionService.evictUserCategoryCaches(userId))
                .doesNotThrowAnyException();
        assertThatCode(() -> evictionService.evictUserCategoryCaches(null))
                .doesNotThrowAnyException();
    }
}
