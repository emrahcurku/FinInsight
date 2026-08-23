package com.fininsight.analytics.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Response payload for the single highest spending category.
 */
@Schema(description = "Highest expense category details for the period")
public record TopCategoryResponse(
        @Schema(description = "Category UUID", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID categoryId,

        @Schema(description = "Category name", example = "Groceries")
        String categoryName,

        @Schema(description = "Total amount spent on this top category", example = "4200.00")
        BigDecimal amount
) {
}
