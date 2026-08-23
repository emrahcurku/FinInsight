package com.fininsight.ai;

import com.fininsight.ai.controller.AiInsightController;
import com.fininsight.ai.dto.AiInsightItem;
import com.fininsight.ai.dto.AiInsightResponse;
import com.fininsight.ai.service.AiInsightService;
import com.fininsight.auth.UserPrincipal;
import com.fininsight.common.dto.ApiResponse;
import com.fininsight.dashboard.dto.InsightSeverity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AiInsightControllerTest {

    @Mock
    private AiInsightService aiInsightService;

    @InjectMocks
    private AiInsightController aiInsightController;

    @Test
    @DisplayName("Should return 200 OK with ApiResponse containing AiInsightResponse")
    public void shouldReturnInsightsSuccessfully() {
        UUID userId = UUID.randomUUID();
        UserPrincipal principal = new UserPrincipal(
                userId, "user@example.com", "password", true,
                List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER"))
        );
        LocalDate from = LocalDate.of(2026, 8, 1);
        LocalDate to = LocalDate.of(2026, 8, 31);

        AiInsightResponse expectedResponse = new AiInsightResponse(
                Instant.now(),
                "Overall good financial management.",
                List.of(new AiInsightItem("TREND", InsightSeverity.INFO, "Stable Spending", "No spikes", "Keep it up")),
                List.of("Maintain emergency fund"),
                "gpt-4o-mini",
                false,
                AiInsightResponse.DEFAULT_DISCLAIMER,
                "corr-123"
        );

        when(aiInsightService.getInsights(userId, from, to)).thenReturn(expectedResponse);

        ResponseEntity<ApiResponse<AiInsightResponse>> responseEntity =
                aiInsightController.getInsights(from, to, principal);

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseEntity.getBody()).isNotNull();
        assertThat(responseEntity.getBody().data()).isEqualTo(expectedResponse);
        verify(aiInsightService).getInsights(userId, from, to);
    }
}
