package com.fininsight.budget.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Request payload for creating a monthly category budget.
 */
@Schema(description = "Payload for creating a monthly category budget")
public record CreateBudgetRequest(
        @Schema(description = "Category UUID", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        @NotNull(message = "Category ID is required")
        UUID categoryId,

        @Schema(description = "Monthly budget spending limit", example = "5000.00")
        @NotNull(message = "Budget amount is required")
        @DecimalMin(value = "0.0001", message = "Budget amount must be greater than 0")
        @Digits(integer = 15, fraction = 4, message = "Amount exceeds precision limits (up to 15 digits integer and 4 decimals)")
        BigDecimal amount,

        @Schema(description = "Budget month (1-12)", example = "8")
        @Min(value = 1, message = "Month must be between 1 and 12")
        @Max(value = 12, message = "Month must be between 1 and 12")
        int month,

        @Schema(description = "Budget year (2000-2100)", example = "2026")
        @Min(value = 2000, message = "Year must be between 2000 and 2100")
        @Max(value = 2100, message = "Year must be between 2000 and 2100")
        int year
) {
}
