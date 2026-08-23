package com.fininsight.category.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request payload for updating an existing custom category name.
 */
@Schema(description = "Payload for updating a custom category")
public record UpdateCategoryRequest(
        @Schema(description = "Updated category display name", example = "Coffee & Beverage")
        @NotBlank(message = "Category name is required")
        @Size(min = 1, max = 100, message = "Category name must be between 1 and 100 characters")
        String name
) {
}
