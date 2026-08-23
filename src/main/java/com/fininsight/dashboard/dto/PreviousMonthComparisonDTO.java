package com.fininsight.dashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * Comparison metrics between current month spending and previous month spending.
 */
@Schema(description = "Comparison of current month spending against the preceding month")
public record PreviousMonthComparisonDTO(
        @Schema(description = "Total spending in the current month", example = "4200.00")
        BigDecimal currentMonthExpense,

        @Schema(description = "Total spending in the previous month", example = "3500.00")
        BigDecimal previousMonthExpense,

        @Schema(description = "Absolute change in expense amount (current - previous)", example = "700.00")
        BigDecimal expenseChangeAmount,

        @Schema(description = "Percentage change in spending ((change / previous) * 100)", example = "20.000")
        BigDecimal expenseChangePercentage,

        @Schema(description = "Trend direction (INCREASED, DECREASED, UNCHANGED)", example = "INCREASED")
        TrendDirection trend
) {
}
