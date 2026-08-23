package com.fininsight.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fininsight.ai.client.OpenAiCompatibleClient;
import com.fininsight.ai.dto.AiGenerationResult;
import com.fininsight.ai.dto.FinancialContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

public class OpenAiCompatibleClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("Should successfully parse AI chat completion JSON response")
    public void shouldParseAiChatResponse() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.openai.com/v1");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();

        OpenAiCompatibleClient client = new OpenAiCompatibleClient(restClient, objectMapper, "gpt-4o-mini");

        String mockResponseBody = """
                {
                  "id": "chatcmpl-123",
                  "choices": [
                    {
                      "message": {
                        "role": "assistant",
                        "content": "{\\"summary\\": \\"Positive cash flow with moderate dining expenses.\\", \\"insights\\": [{\\"type\\": \\"TOP_SPENDING_CATEGORY\\", \\"severity\\": \\"INFO\\", \\"title\\": \\"Dining Dominance\\", \\"description\\": \\"Dining accounted for 35% of total expenses.\\", \\"recommendation\\": \\"Set a weekly dining budget.\\"}], \\"recommendations\\": [\\"Build an emergency fund\\"]}"
                      }
                    }
                  ]
                }
                """;

        server.expect(requestTo("https://api.openai.com/v1/chat/completions"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(mockResponseBody, MediaType.APPLICATION_JSON));

        FinancialContext context = new FinancialContext(
                "2026-08-01", "2026-08-31",
                new BigDecimal("5000"), new BigDecimal("3000"), new BigDecimal("2000"), 10L,
                List.of(), null, null, List.of()
        );

        AiGenerationResult result = client.generateInsights(context);

        assertThat(result.summary()).isEqualTo("Positive cash flow with moderate dining expenses.");
        assertThat(result.insights()).hasSize(1);
        assertThat(result.insights().get(0).title()).isEqualTo("Dining Dominance");
        assertThat(result.recommendations()).containsExactly("Build an emergency fund");
        assertThat(result.model()).isEqualTo("gpt-4o-mini");
    }

    @Test
    @DisplayName("Should throw exception on HTTP 500 server error")
    public void shouldThrowOnServerError() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.openai.com/v1");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();

        OpenAiCompatibleClient client = new OpenAiCompatibleClient(restClient, objectMapper, "gpt-4o-mini");

        server.expect(requestTo("https://api.openai.com/v1/chat/completions"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withServerError());

        FinancialContext context = new FinancialContext(
                "2026-08-01", "2026-08-31",
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0L,
                List.of(), null, null, List.of()
        );

        assertThatThrownBy(() -> client.generateInsights(context))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("AI provider generation error");
    }
}
