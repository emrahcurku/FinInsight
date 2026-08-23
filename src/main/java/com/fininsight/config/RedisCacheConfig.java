package com.fininsight.config;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Spring Cache and Redis configuration.
 * Configures JSON serialization, cache-specific TTLs, and a resilient CacheErrorHandler.
 */
@Configuration
@EnableCaching
public class RedisCacheConfig {

    @Value("${application.cache.redis.default-ttl-seconds:300}")
    private long defaultTtlSeconds;

    @Value("${application.cache.redis.dashboard-ttl-seconds:300}")
    private long dashboardTtlSeconds;

    @Value("${application.cache.redis.analytics-ttl-seconds:600}")
    private long analyticsTtlSeconds;

    @Value("${application.cache.redis.categories-ttl-seconds:1800}")
    private long categoriesTtlSeconds;

    @Value("${application.cache.redis.ai-insights-ttl-seconds:600}")
    private long aiInsightsTtlSeconds;

    @Bean
    @ConditionalOnProperty(name = "spring.cache.type", havingValue = "redis", matchIfMissing = true)
    @SuppressWarnings("removal")
    public RedisCacheManager redisCacheManager(RedisConnectionFactory connectionFactory) {
        GenericJackson2JsonRedisSerializer jsonSerializer = 
                new GenericJackson2JsonRedisSerializer(createCacheObjectMapper());

        RedisCacheConfiguration baseConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofSeconds(defaultTtlSeconds))
                .disableCachingNullValues()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer));

        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        cacheConfigurations.put(CacheNames.DASHBOARD, baseConfig.entryTtl(Duration.ofSeconds(dashboardTtlSeconds)));
        cacheConfigurations.put(CacheNames.ANALYTICS_SUMMARY, baseConfig.entryTtl(Duration.ofSeconds(analyticsTtlSeconds)));
        cacheConfigurations.put(CacheNames.ANALYTICS_CATEGORY, baseConfig.entryTtl(Duration.ofSeconds(analyticsTtlSeconds)));
        cacheConfigurations.put(CacheNames.ANALYTICS_MONTHLY, baseConfig.entryTtl(Duration.ofSeconds(analyticsTtlSeconds)));
        cacheConfigurations.put(CacheNames.ANALYTICS_BUDGET_OVERVIEW, baseConfig.entryTtl(Duration.ofSeconds(analyticsTtlSeconds)));
        cacheConfigurations.put(CacheNames.ANALYTICS_TOP_CATEGORIES, baseConfig.entryTtl(Duration.ofSeconds(analyticsTtlSeconds)));
        cacheConfigurations.put(CacheNames.CATEGORIES, baseConfig.entryTtl(Duration.ofSeconds(categoriesTtlSeconds)));
        cacheConfigurations.put(CacheNames.AI_INSIGHTS, baseConfig.entryTtl(Duration.ofSeconds(aiInsightsTtlSeconds)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(baseConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }

    @Bean
    public CacheErrorHandler errorHandler() {
        return new CustomCacheErrorHandler();
    }

    public static ObjectMapper createCacheObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        mapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );
        return mapper;
    }
}
