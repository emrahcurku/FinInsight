package com.fininsight.dashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Rule-based deterministic financial insight entry.
 */
@Schema(description = "Actionable financial insight item generated for the user")
public record FinancialInsightDTO(
        @Schema(description = "Insight type identifier", example = "TOP_SPENDING_CATEGORY")
        String type,

        @Schema(description = "Insight title", example = "Top Expense Category")
        String title,

        @Schema(description = "Detailed insight message", example = "Your highest spending category is Groceries ($3,000.00), accounting for 75.0% of total expenses.")
        String message,

        @Schema(description = "Severity level (INFO, WARNING, DANGER)", example = "INFO")
        InsightSeverity severity
) {
}
