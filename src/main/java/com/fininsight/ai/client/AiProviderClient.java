package com.fininsight.ai.client;

import com.fininsight.ai.dto.AiGenerationResult;
import com.fininsight.ai.dto.FinancialContext;

/**
 * Interface abstracting external AI providers (OpenAI, Gemini, Ollama, Mock, etc.).
 */
public interface AiProviderClient {

    /**
     * Generates financial insights and recommendations based on aggregated context.
     *
     * @param context the sanitized financial context
     * @return structured generation result
     */
    AiGenerationResult generateInsights(FinancialContext context);

    /**
     * Identifies provider name for logging and auditing.
     */
    String getProviderName();
}
