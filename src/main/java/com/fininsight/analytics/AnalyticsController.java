package com.fininsight.analytics;

import com.fininsight.analytics.dto.BudgetOverviewResponse;
import com.fininsight.analytics.dto.CategorySpendingResponse;
import com.fininsight.analytics.dto.FinancialSummaryResponse;
import com.fininsight.analytics.dto.MonthlySummaryResponse;
import com.fininsight.analytics.dto.TopCategoryResponse;
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
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
import java.util.List;

/**
 * REST Controller exposing financial analytics, dashboards, and spending summaries.
 */
@Tag(name = "Analytics", description = "Financial analytics, dashboards, and aggregated reporting endpoints")
@SecurityRequirement(name = "Bearer Authentication")
@Validated
@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @Operation(summary = "Get overall financial summary", description = "Calculates total income, total expense, net balance, and transaction count for the authenticated user")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Financial summary retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid date range parameters",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Valid JWT required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<FinancialSummaryResponse>> getFinancialSummary(
            @Parameter(description = "Start date (YYYY-MM-DD), defaults to start of current month")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "End date (YYYY-MM-DD), defaults to end of current month")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        FinancialSummaryResponse response = analyticsService.getFinancialSummary(principal.getId(), from, to);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "Get spending breakdown by category", description = "Aggregates total expenses grouped by category with percentages of total spending")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Category spending breakdown retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid date range parameters",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Valid JWT required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/spending-by-category")
    public ResponseEntity<ApiResponse<List<CategorySpendingResponse>>> getSpendingByCategory(
            @Parameter(description = "Start date (YYYY-MM-DD), defaults to start of current month")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "End date (YYYY-MM-DD), defaults to end of current month")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        List<CategorySpendingResponse> response = analyticsService.getSpendingByCategory(principal.getId(), from, to);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "Get monthly financial summary trends", description = "Groups transaction income, expense, and net balance by month")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Monthly summaries retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid date range parameters",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Valid JWT required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/monthly-summary")
    public ResponseEntity<ApiResponse<List<MonthlySummaryResponse>>> getMonthlySummary(
            @Parameter(description = "Start date (YYYY-MM-DD), defaults to start of current month")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "End date (YYYY-MM-DD), defaults to end of current month")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        List<MonthlySummaryResponse> response = analyticsService.getMonthlySummary(principal.getId(), from, to);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "Get budget overview and threshold statistics", description = "Calculates total budget, total spent against budgets, remaining amount, usage percentage, and threshold status distribution")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Budget overview retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid month or year parameters",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Valid JWT required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/budget-overview")
    public ResponseEntity<ApiResponse<BudgetOverviewResponse>> getBudgetOverview(
            @Parameter(description = "Year (2000-2100), defaults to current year", example = "2026")
            @RequestParam(required = false) @Min(value = 2000, message = "Year must be between 2000 and 2100") @Max(value = 2100, message = "Year must be between 2000 and 2100") Integer year,
            @Parameter(description = "Month (1-12), defaults to current month", example = "8")
            @RequestParam(required = false) @Min(value = 1, message = "Month must be between 1 and 12") @Max(value = 12, message = "Month must be between 1 and 12") Integer month,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        BudgetOverviewResponse response = analyticsService.getBudgetOverview(principal.getId(), year, month);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "Get top expense category", description = "Returns the single highest spending category for the specified period")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Top expense category retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid date range parameters",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Valid JWT required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No expense transactions found in the specified period",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/top-category")
    public ResponseEntity<ApiResponse<TopCategoryResponse>> getTopCategory(
            @Parameter(description = "Start date (YYYY-MM-DD), defaults to start of current month")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "End date (YYYY-MM-DD), defaults to end of current month")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        TopCategoryResponse response = analyticsService.getTopCategory(principal.getId(), from, to);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
