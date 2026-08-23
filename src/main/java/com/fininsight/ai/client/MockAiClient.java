package com.fininsight.ai.client;

import com.fininsight.ai.dto.AiGenerationResult;
import com.fininsight.ai.dto.FinancialContext;
import com.fininsight.ai.service.DeterministicInsightEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Mock / Offline AI client implementation for testing and development.
 * Generates rich contextual insights in-memory without external HTTP calls.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MockAiClient implements AiProviderClient {

    private final DeterministicInsightEngine deterministicInsightEngine;

    @Override
    public AiGenerationResult generateInsights(FinancialContext context) {
        log.debug("Generating mock AI financial insights for period [{} to {}]",
                context.periodFrom(), context.periodTo());
        AiGenerationResult fallback = deterministicInsightEngine.generateFallbackInsights(context);
        return new AiGenerationResult(
                fallback.summary(),
                fallback.insights(),
                fallback.recommendations(),
                "mock-ai-engine"
        );
    }

    @Override
    public String getProviderName() {
        return "mock-ai-provider";
    }
}
