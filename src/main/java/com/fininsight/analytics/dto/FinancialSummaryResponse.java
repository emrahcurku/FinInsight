package com.fininsight.analytics.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Response payload for financial summary over a date range.
 */
@Schema(description = "Overall financial summary metrics for a specified period")
public record FinancialSummaryResponse(
        @Schema(description = "Total income amount in the period", example = "15000.00")
        BigDecimal totalIncome,

        @Schema(description = "Total expense amount in the period", example = "8500.00")
        BigDecimal totalExpense,

        @Schema(description = "Net balance (totalIncome - totalExpense)", example = "6500.00")
        BigDecimal netBalance,

        @Schema(description = "Total count of transactions in the period", example = "42")
        long transactionCount,

        @Schema(description = "Start date of the analyzed period", example = "2026-08-01")
        LocalDate from,

        @Schema(description = "End date of the analyzed period", example = "2026-08-31")
        LocalDate to
) {
}
