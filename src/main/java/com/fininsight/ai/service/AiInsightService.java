package com.fininsight.ai.service;

import com.fininsight.ai.client.AiProviderClient;
import com.fininsight.ai.dto.AiGenerationResult;
import com.fininsight.ai.dto.AiInsightResponse;
import com.fininsight.ai.dto.FinancialContext;
import com.fininsight.common.exception.BusinessException;
import com.fininsight.config.CacheNames;
import com.fininsight.config.CorrelationIdFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Service orchestrating AI financial insight generation, caching, rate limiting, and fallback resilience.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiInsightService {

    private final FinancialContextBuilder financialContextBuilder;
    private final AiProviderClient aiProviderClient;
    private final DeterministicInsightEngine deterministicInsightEngine;
    private final AiRateLimiter aiRateLimiter;

    @Value("${application.ai.max-retries:2}")
    private int maxRetries;

    /**
     * Generates or retrieves cached AI insights for the authenticated user.
     */
    @Cacheable(
            value = CacheNames.AI_INSIGHTS,
            key = "#userId + ':' + (#from != null ? #from.toString() : 'DEFAULT') + ':' + (#to != null ? #to.toString() : 'DEFAULT')"
    )
    public AiInsightResponse getInsights(UUID userId, LocalDate from, LocalDate to) {
        validateDateRange(from, to);

        // 1. Rate Limiting Check
        aiRateLimiter.checkRateLimit(userId);

        // 2. Build Aggregated Financial Context (Zero PII)
        FinancialContext context = financialContextBuilder.buildContext(userId, from, to);
        String correlationId = CorrelationIdFilter.getCurrentCorrelationId();

        // 3. Attempt AI Provider Generation with Resilient Fallback
        AiGenerationResult result = executeWithRetryAndFallback(context);

        boolean isFallback = "deterministic-rule-engine".equalsIgnoreCase(result.model());

        return new AiInsightResponse(
                Instant.now(),
                result.summary(),
                result.insights(),
                result.recommendations(),
                result.model(),
                isFallback,
                AiInsightResponse.DEFAULT_DISCLAIMER,
                correlationId
        );
    }

    private AiGenerationResult executeWithRetryAndFallback(FinancialContext context) {
        int attempts = 0;
        int maxAttempts = Math.max(1, maxRetries + 1);

        while (attempts < maxAttempts) {
            attempts++;
            try {
                return aiProviderClient.generateInsights(context);
            } catch (Exception ex) {
                log.warn("AI generation attempt {}/{} failed with provider '{}': {}.",
                        attempts, maxAttempts, aiProviderClient.getProviderName(), ex.getMessage());

                if (attempts < maxAttempts) {
                    try {
                        Thread.sleep(200L * attempts);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        log.info("AI provider generation unavailable. Gracefully falling back to deterministic insight engine.");
        return deterministicInsightEngine.generateFallbackInsights(context);
    }

    private void validateDateRange(LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new BusinessException(
                    "Start date (" + from + ") cannot be after end date (" + to + ")",
                    HttpStatus.BAD_REQUEST
            );
        }
    }
}
