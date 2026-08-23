package com.fininsight.category.dto;

import com.fininsight.category.Category;
import com.fininsight.category.CategoryType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

/**
 * Public response DTO for a category.
 */
@Schema(description = "Category response payload")
public record CategoryResponse(
        @Schema(description = "Category unique identifier", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID id,

        @Schema(description = "Category display name", example = "Groceries")
        String name,

        @Schema(description = "Category classification (INCOME or EXPENSE)", example = "EXPENSE")
        CategoryType type,

        @Schema(description = "True if this is a global system default category, false if user custom category", example = "true")
        boolean system,

        @Schema(description = "Record creation timestamp", example = "2026-08-23T10:15:30Z")
        Instant createdAt,

        @Schema(description = "Record last update timestamp", example = "2026-08-23T10:15:30Z")
        Instant updatedAt
) {

    public static CategoryResponse fromEntity(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getType(),
                category.isSystemCategory(),
                category.getCreatedAt(),
                category.getUpdatedAt()
        );
    }
}
