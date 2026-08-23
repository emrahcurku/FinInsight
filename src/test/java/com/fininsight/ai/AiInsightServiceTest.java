package com.fininsight.ai;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import com.fininsight.ai.client.AiProviderClient;
import com.fininsight.ai.dto.AiGenerationResult;
import com.fininsight.ai.dto.AiInsightItem;
import com.fininsight.ai.dto.AiInsightResponse;
import com.fininsight.ai.dto.FinancialContext;
import com.fininsight.ai.service.AiInsightService;
import com.fininsight.ai.service.AiRateLimiter;
import com.fininsight.ai.service.DeterministicInsightEngine;
import com.fininsight.ai.service.FinancialContextBuilder;
import com.fininsight.common.exception.BusinessException;
import com.fininsight.dashboard.dto.InsightSeverity;

@ExtendWith(MockitoExtension.class)
public class AiInsightServiceTest {

    @Mock
    private FinancialContextBuilder financialContextBuilder;

    @Mock
    private AiProviderClient aiProviderClient;

    @Mock
    private DeterministicInsightEngine deterministicInsightEngine;

    @Mock
    private AiRateLimiter aiRateLimiter;

    @InjectMocks
    private AiInsightService aiInsightService;

    @Test
    @DisplayName("Should generate insights successfully via AI provider")
    public void shouldGenerateInsightsSuccessfully() {
        UUID userId = UUID.randomUUID();
        LocalDate from = LocalDate.of(2026, 8, 1);
        LocalDate to = LocalDate.of(2026, 8, 31);

        FinancialContext context = new FinancialContext(
                "2026-08-01", "2026-08-31",
                new BigDecimal("5000"), new BigDecimal("3000"), new BigDecimal("2000"), 12L,
                List.of(), null, null, List.of()
        );

        when(financialContextBuilder.buildContext(eq(userId), eq(from), eq(to))).thenReturn(context);

        AiGenerationResult aiResult = new AiGenerationResult(
                "Healthy financial posture.",
                List.of(new AiInsightItem("SAVINGS", InsightSeverity.INFO, "Good Savings", "Saved 40%", "Keep saving")),
                List.of("Continue current trajectory"),
                "gpt-4o-mini"
        );
        when(aiProviderClient.generateInsights(context)).thenReturn(aiResult);

        AiInsightResponse response = aiInsightService.getInsights(userId, from, to);

        assertThat(response).isNotNull();
        assertThat(response.summary()).isEqualTo("Healthy financial posture.");
        assertThat(response.fallback()).isFalse();
        assertThat(response.model()).isEqualTo("gpt-4o-mini");
        assertThat(response.insights()).hasSize(1);
        verify(aiRateLimiter).checkRateLimit(userId);
    }

    @Test
    @DisplayName("Should fallback gracefully to deterministic engine when AI provider fails")
    public void shouldFallbackToDeterministicEngineOnAiFailure() {
        UUID userId = UUID.randomUUID();
        LocalDate from = LocalDate.of(2026, 8, 1);
        LocalDate to = LocalDate.of(2026, 8, 31);

        FinancialContext context = new FinancialContext(
                "2026-08-01", "2026-08-31",
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0L,
                List.of(), null, null, List.of()
        );

        when(financialContextBuilder.buildContext(eq(userId), eq(from), eq(to))).thenReturn(context);
        when(aiProviderClient.generateInsights(context)).thenThrow(new RuntimeException("Provider connection refused"));
        when(aiProviderClient.getProviderName()).thenReturn("openai-compatible");

        AiGenerationResult fallbackResult = new AiGenerationResult(
                "Fallback financial review.",
                List.of(new AiInsightItem("NO_ACTIVITY", InsightSeverity.INFO, "No Activity", "No tx", "Start logging")),
                List.of("Log expenses"),
                "deterministic-rule-engine"
        );
        when(deterministicInsightEngine.generateFallbackInsights(context)).thenReturn(fallbackResult);

        ReflectionTestUtils.setField(aiInsightService, "maxRetries", 0);

        AiInsightResponse response = aiInsightService.getInsights(userId, from, to);

        assertThat(response).isNotNull();
        assertThat(response.fallback()).isTrue();
        assertThat(response.model()).isEqualTo("deterministic-rule-engine");
        assertThat(response.summary()).isEqualTo("Fallback financial review.");
    }

    @Test
    @DisplayName("Should throw 400 Bad Request when from date is after to date")
    public void shouldThrowWhenFromIsAfterTo() {
        UUID userId = UUID.randomUUID();
        LocalDate from = LocalDate.of(2026, 8, 31);
        LocalDate to = LocalDate.of(2026, 8, 1);

        assertThatThrownBy(() -> aiInsightService.getInsights(userId, from, to))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("cannot be after end date")
                .satisfies(ex -> assertThat(((BusinessException) ex).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
    }
}
