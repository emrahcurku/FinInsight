package com.fininsight.ai.config;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fininsight.ai.client.AiProviderClient;
import com.fininsight.ai.client.MockAiClient;
import com.fininsight.ai.client.OpenAiCompatibleClient;

import lombok.extern.slf4j.Slf4j;

/**
 * Spring configuration creating the active AiProviderClient bean based on
 * environment properties.
 */
@Slf4j
@Configuration
public class AiConfig {

    @Value("${application.ai.enabled:true}")
    private boolean enabled;

    @Value("${application.ai.provider:openai}")
    private String provider;

    @Value("${application.ai.api-key:}")
    private String apiKey;

    @Value("${application.ai.model:gpt-4o-mini}")
    private String model;

    @Value("${application.ai.base-url:https://api.openai.com/v1}")
    private String baseUrl;

    @Value("${application.ai.timeout-ms:5000}")
    private int timeoutMs;

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    @Primary
    public AiProviderClient aiProviderClient(MockAiClient mockAiClient, ObjectMapper objectMapper) {
        if (!enabled) {
            log.info("AI insights feature is disabled (AI_ENABLED=false). Using offline MockAiClient.");
            return mockAiClient;
        }

        if ("mock".equalsIgnoreCase(provider) || apiKey == null || apiKey.isBlank()) {
            log.info("AI provider is set to 'mock' or API key is not configured. Using MockAiClient.");
            return mockAiClient;
        }

        log.info("Initializing OpenAI-compatible AI client with baseUrl='{}', model='{}', timeoutMs={}",
                baseUrl, model, timeoutMs);

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(timeoutMs));
        requestFactory.setReadTimeout(Duration.ofMillis(timeoutMs));

        RestClient restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .build();

        return new OpenAiCompatibleClient(restClient, objectMapper, model);
    }
}
