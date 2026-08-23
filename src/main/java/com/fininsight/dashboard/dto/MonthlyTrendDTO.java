package com.fininsight.dashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * Monthly trend item representing continuous historical performance.
 */
@Schema(description = "Monthly historical financial trend entry")
public record MonthlyTrendDTO(
        @Schema(description = "Year", example = "2026")
        int year,

        @Schema(description = "Month (1-12)", example = "8")
        int month,

        @Schema(description = "Total income for the month", example = "12000.00")
        BigDecimal totalIncome,

        @Schema(description = "Total expense for the month", example = "7500.00")
        BigDecimal totalExpense,

        @Schema(description = "Net balance for the month", example = "4500.00")
        BigDecimal netBalance
) {
}
