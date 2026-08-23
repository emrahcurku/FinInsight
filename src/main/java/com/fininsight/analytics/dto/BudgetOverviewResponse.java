package com.fininsight.analytics.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * Response payload for budget overview and threshold statistics.
 */
@Schema(description = "Overall status and distribution of user budgets for a given period")
public record BudgetOverviewResponse(
        @Schema(description = "Total allocated budget amount across all categories", example = "15000.00")
        BigDecimal totalBudget,

        @Schema(description = "Total spent amount against budgeted categories", example = "9200.00")
        BigDecimal totalSpent,

        @Schema(description = "Total remaining budget amount (totalBudget - totalSpent)", example = "5800.00")
        BigDecimal totalRemaining,

        @Schema(description = "Overall budget usage percentage across all budgets (0-100+%)", example = "61.333")
        BigDecimal overallUsagePercentage,

        @Schema(description = "Number of budgets with NORMAL status (<80% used)", example = "4")
        int normalBudgetCount,

        @Schema(description = "Number of budgets with WARNING status (80-99.99% used)", example = "1")
        int warningBudgetCount,

        @Schema(description = "Number of budgets with EXCEEDED status (>=100% used)", example = "0")
        int exceededBudgetCount
) {
}
