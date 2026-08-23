package com.fininsight.analytics.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * Response payload for monthly aggregated financial summary.
 */
@Schema(description = "Monthly aggregated income, expense, and net balance summary")
public record MonthlySummaryResponse(
        @Schema(description = "Year", example = "2026")
        int year,

        @Schema(description = "Month (1-12)", example = "8")
        int month,

        @Schema(description = "Total income for the month", example = "12000.00")
        BigDecimal totalIncome,

        @Schema(description = "Total expense for the month", example = "7500.00")
        BigDecimal totalExpense,

        @Schema(description = "Net balance for the month (totalIncome - totalExpense)", example = "4500.00")
        BigDecimal netBalance
) {
}
