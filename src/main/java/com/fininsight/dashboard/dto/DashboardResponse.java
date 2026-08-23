package com.fininsight.dashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Composite root dashboard response uniting all analytical views and metrics.
 */
@Schema(description = "Composite financial dashboard payload for authenticated user")
public record DashboardResponse(
        @Schema(description = "Overall financial summary for the selected period")
        FinancialSummaryDTO financialSummary,

        @Schema(description = "Continuous 6-month historical monthly trend (zero-filled for missing months)")
        List<MonthlyTrendDTO> monthlyTrend,

        @Schema(description = "Top 5 expense categories with proportional percentages")
        List<CategorySpendingDTO> categorySpending,

        @Schema(description = "Budget status overview and threshold distribution")
        BudgetOverviewDTO budgetOverview,

        @Schema(description = "Recent 5 transactions for the user")
        List<RecentTransactionDTO> recentTransactions,

        @Schema(description = "Comparison of current month spending against previous month")
        PreviousMonthComparisonDTO previousMonthComparison,

        @Schema(description = "Deterministic rule-based financial insights and alerts")
        List<FinancialInsightDTO> insights
) {
}
