package com.fininsight.budget.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Request payload for updating an existing monthly budget limit.
 */
@Schema(description = "Payload for updating a budget limit")
public record UpdateBudgetRequest(
        @Schema(description = "Updated monthly budget spending limit", example = "6000.00")
        @NotNull(message = "Budget amount is required")
        @DecimalMin(value = "0.0001", message = "Budget amount must be greater than 0")
        @Digits(integer = 15, fraction = 4, message = "Amount exceeds precision limits (up to 15 digits integer and 4 decimals)")
        BigDecimal amount
) {
}
