package com.fininsight.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Wrapper for paginated API responses.
 * Converts Spring's Page object into a clean, frontend-friendly structure.
 *
 * @param <T> the type of elements in the content list
 */
@Schema(description = "Paginated list response wrapper")
public record PagedResponse<T>(
        @Schema(description = "Current page elements payload")
        List<T> content,

        @Schema(description = "Zero-indexed current page number", example = "0")
        int page,

        @Schema(description = "Requested page size", example = "20")
        int size,

        @Schema(description = "Total number of elements across all pages", example = "42")
        long totalElements,

        @Schema(description = "Total number of pages available", example = "3")
        int totalPages,

        @Schema(description = "Indicates whether the current page is the last one", example = "false")
        boolean last
) {

    public static <T> PagedResponse<T> of(org.springframework.data.domain.Page<T> page) {
        return new PagedResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }

    public static <T> PagedResponse<T> of(org.springframework.data.domain.Page<?> page, List<T> content) {
        return new PagedResponse<>(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }
}
