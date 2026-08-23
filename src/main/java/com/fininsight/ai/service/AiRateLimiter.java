package com.fininsight.ai.service;

import com.fininsight.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Rate limiter for AI insight generation.
 * Enforces per-user request quotas using Redis (with in-memory fallback for non-Redis environments).
 */
@Slf4j
@Component
public class AiRateLimiter {

    private final Optional<StringRedisTemplate> redisTemplate;

    @Value("${application.ai.rate-limit-per-minute:10}")
    private int rateLimitPerMinute;

    // In-memory sliding fallback for non-Redis environments (e.g. tests)
    private final Map<String, AtomicInteger> inMemoryCounters = new ConcurrentHashMap<>();
    private volatile long currentMinuteBucket = 0L;

    @Autowired
    public AiRateLimiter(@Autowired(required = false) StringRedisTemplate redisTemplate) {
        this.redisTemplate = Optional.ofNullable(redisTemplate);
    }

    /**
     * Checks whether the user is permitted to make an AI insight generation request.
     * Throws BusinessException with HTTP 429 if the quota is exceeded.
     *
     * @param userId authenticated user UUID
     */
    public void checkRateLimit(UUID userId) {
        if (userId == null) {
            return;
        }

        long epochMinute = Instant.now().getEpochSecond() / 60;
        String rateLimitKey = "ai:ratelimit:" + userId + ":" + epochMinute;

        if (redisTemplate.isPresent()) {
            try {
                StringRedisTemplate template = redisTemplate.get();
                Long currentCount = template.opsForValue().increment(rateLimitKey);
                if (currentCount != null && currentCount == 1L) {
                    template.expire(rateLimitKey, Duration.ofSeconds(65));
                }

                if (currentCount != null && currentCount > rateLimitPerMinute) {
                    log.warn("Rate limit exceeded for user {} on AI insights (count: {}, limit: {})",
                            userId, currentCount, rateLimitPerMinute);
                    throw new BusinessException(
                            "AI insights rate limit exceeded. Please wait a minute before requesting insights again.",
                            HttpStatus.TOO_MANY_REQUESTS
                    );
                }
                return;
            } catch (BusinessException be) {
                throw be;
            } catch (Exception ex) {
                log.warn("Redis rate limit check failed for user {}: {}. Allowing request as fallback.",
                        userId, ex.getMessage());
                return;
            }
        }

        // In-memory fallback
        checkInMemoryRateLimit(userId, epochMinute);
    }

    private void checkInMemoryRateLimit(UUID userId, long epochMinute) {
        if (epochMinute != currentMinuteBucket) {
            inMemoryCounters.clear();
            currentMinuteBucket = epochMinute;
        }

        String userKey = userId + ":" + epochMinute;
        int count = inMemoryCounters.computeIfAbsent(userKey, k -> new AtomicInteger(0)).incrementAndGet();

        if (count > rateLimitPerMinute) {
            throw new BusinessException(
                    "AI insights rate limit exceeded. Please wait a minute before requesting insights again.",
                    HttpStatus.TOO_MANY_REQUESTS
            );
        }
    }
}
