package com.fininsight.ai.controller;

import com.fininsight.ai.dto.AiInsightResponse;
import com.fininsight.ai.service.AiInsightService;
import com.fininsight.auth.UserPrincipal;
import com.fininsight.common.dto.ApiResponse;
import com.fininsight.common.dto.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * REST Controller exposing AI-powered financial insights and behavioral recommendations.
 */
@Tag(name = "AI Insights", description = "AI-powered financial insights, spending recommendations, and behavioral advice")
@SecurityRequirement(name = "Bearer Authentication")
@Validated
@RestController
@RequestMapping("/api/v1/ai/insights")
@RequiredArgsConstructor
public class AiInsightController {

    private final AiInsightService aiInsightService;

    @Operation(
            summary = "Get AI financial insights",
            description = "Analyzes the authenticated user's aggregated financial summaries to produce natural language insights, spending trends, and actionable recommendations with fallback resilience."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "AI insights generated or retrieved from cache successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid date range parameters",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Valid JWT Bearer token required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "429",
                    description = "Too Many Requests - AI rate limit exceeded",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping
    public ResponseEntity<ApiResponse<AiInsightResponse>> getInsights(
            @Parameter(description = "Start date (YYYY-MM-DD), defaults to start of current month")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "End date (YYYY-MM-DD), defaults to end of current month")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        AiInsightResponse response = aiInsightService.getInsights(principal.getId(), from, to);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
