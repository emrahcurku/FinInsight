package com.fininsight.dashboard;

import com.fininsight.auth.UserPrincipal;
import com.fininsight.common.dto.ApiResponse;
import com.fininsight.common.dto.ErrorResponse;
import com.fininsight.dashboard.dto.DashboardResponse;
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
 * REST Controller exposing composite dashboard data, financial summaries, and rule-based insights.
 */
@Tag(name = "Dashboard", description = "Composite dashboard summaries, trends, and financial insights endpoints")
@SecurityRequirement(name = "Bearer Authentication")
@Validated
@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @Operation(
            summary = "Get composite user dashboard",
            description = "Retrieves overall financial summary, 6-month monthly trends, top category spending, budget status, recent transactions, and actionable insights in a single call"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Dashboard payload retrieved successfully"
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
            )
    })
    @GetMapping
    public ResponseEntity<ApiResponse<DashboardResponse>> getDashboard(
            @Parameter(description = "Start date (YYYY-MM-DD), defaults to start of current month")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "End date (YYYY-MM-DD), defaults to end of current month")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        DashboardResponse response = dashboardService.getDashboard(principal.getId(), from, to);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
