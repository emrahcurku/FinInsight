package com.fininsight.category.dto;

import com.fininsight.category.CategoryType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request payload for creating a custom category.
 */
@Schema(description = "Payload for creating a custom category")
public record CreateCategoryRequest(
        @Schema(description = "Category display name", example = "Coffee & Snacks")
        @NotBlank(message = "Category name is required")
        @Size(min = 1, max = 100, message = "Category name must be between 1 and 100 characters")
        String name,

        @Schema(description = "Category type (INCOME or EXPENSE)", example = "EXPENSE")
        @NotNull(message = "Category type is required")
        CategoryType type
) {
}
