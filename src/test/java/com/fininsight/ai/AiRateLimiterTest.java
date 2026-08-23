package com.fininsight.ai;

import com.fininsight.ai.service.AiRateLimiter;
import com.fininsight.common.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
@ExtendWith(MockitoExtension.class)
public class AiRateLimiterTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Test
    @DisplayName("Should permit request within rate limit")
    public void shouldPermitRequestWithinLimit() {
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.increment(anyString())).thenReturn(3L);

        AiRateLimiter limiter = new AiRateLimiter(redisTemplate);
        ReflectionTestUtils.setField(limiter, "rateLimitPerMinute", 10);

        UUID userId = UUID.randomUUID();
        limiter.checkRateLimit(userId); // should not throw
    }

    @Test
    @DisplayName("Should throw BusinessException with HTTP 429 when quota exceeded")
    public void shouldThrow429WhenQuotaExceeded() {
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.increment(anyString())).thenReturn(11L);

        AiRateLimiter limiter = new AiRateLimiter(redisTemplate);
        ReflectionTestUtils.setField(limiter, "rateLimitPerMinute", 10);

        UUID userId = UUID.randomUUID();
        assertThatThrownBy(() -> limiter.checkRateLimit(userId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("rate limit exceeded")
                .satisfies(ex -> assertThat(((BusinessException) ex).getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS));
    }

    @Test
    @DisplayName("Should permit request gracefully when Redis throws an exception")
    public void shouldPermitRequestWhenRedisThrowsException() {
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.increment(anyString())).thenThrow(new RuntimeException("Redis connection refused"));

        AiRateLimiter limiter = new AiRateLimiter(redisTemplate);
        ReflectionTestUtils.setField(limiter, "rateLimitPerMinute", 10);

        UUID userId = UUID.randomUUID();
        limiter.checkRateLimit(userId); // should log warning and permit
    }

    @Test
    @DisplayName("Should enforce rate limit via in-memory fallback when Redis is absent")
    public void shouldEnforceInMemoryRateLimit() {
        AiRateLimiter limiter = new AiRateLimiter(null);
        ReflectionTestUtils.setField(limiter, "rateLimitPerMinute", 2);

        UUID userId = UUID.randomUUID();
        limiter.checkRateLimit(userId);
        limiter.checkRateLimit(userId);

        assertThatThrownBy(() -> limiter.checkRateLimit(userId))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS));
    }
}
