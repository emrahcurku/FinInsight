package com.fininsight.dashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Top category spending item on the dashboard.
 */
@Schema(description = "Category-wise expense breakdown item on dashboard")
public record CategorySpendingDTO(
        @Schema(description = "Category UUID", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID categoryId,

        @Schema(description = "Category display name", example = "Groceries")
        String categoryName,

        @Schema(description = "Total amount spent in this category", example = "3500.00")
        BigDecimal totalAmount,

        @Schema(description = "Percentage of total spending (0-100%)", example = "41.176")
        BigDecimal percentage
) {
}
