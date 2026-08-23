package com.fininsight.analytics.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Response payload for spending breakdown per category.
 */
@Schema(description = "Category-wise expense breakdown with relative percentages")
public record CategorySpendingResponse(
        @Schema(description = "Category UUID", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID categoryId,

        @Schema(description = "Category name", example = "Groceries")
        String categoryName,

        @Schema(description = "Total expense amount for this category", example = "3500.00")
        BigDecimal amount,

        @Schema(description = "Percentage of total expense (0-100%)", example = "41.176")
        BigDecimal percentage
) {
}
