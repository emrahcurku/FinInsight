package com.fininsight.budget.dto;

import com.fininsight.budget.ThresholdStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Public response payload for a monthly budget with spending metrics and threshold status.
 */
@Schema(description = "Monthly budget response payload with spending analytics")
public record BudgetResponse(
        @Schema(description = "Budget unique identifier", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID id,

        @Schema(description = "Referenced category UUID", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID categoryId,

        @Schema(description = "Category display name", example = "Groceries")
        String categoryName,

        @Schema(description = "Target budget spending limit", example = "5000.0000")
        BigDecimal amount,

        @Schema(description = "Budget year", example = "2026")
        short year,

        @Schema(description = "Budget month (1-12)", example = "8")
        short month,

        @Schema(description = "Total spent amount for this category in this period (EXPENSE transactions only)", example = "2350.7500")
        BigDecimal spentAmount,

        @Schema(description = "Remaining amount before exceeding budget (negative if exceeded)", example = "2649.2500")
        BigDecimal remainingAmount,

        @Schema(description = "Percentage of budget utilized", example = "47.015")
        BigDecimal usagePercentage,

        @Schema(description = "Budget threshold alert status (NORMAL, WARNING, EXCEEDED)", example = "NORMAL")
        ThresholdStatus thresholdStatus,

        @Schema(description = "Record creation timestamp", example = "2026-08-23T10:15:30Z")
        Instant createdAt,

        @Schema(description = "Record last update timestamp", example = "2026-08-23T10:15:30Z")
        Instant updatedAt
) {
}
