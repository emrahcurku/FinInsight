package com.fininsight.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * Standard API response wrapper.
 * All REST endpoints return this structure for consistency.
 *
 * @param <T> the type of the response data payload
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Standard API response envelope")
public record ApiResponse<T>(
        @Schema(description = "Indicates whether the request was successful", example = "true")
        boolean success,

        @Schema(description = "Optional informative response message", example = "Operation completed successfully")
        String message,

        @Schema(description = "Response data payload")
        T data,

        @Schema(description = "Response generation timestamp in ISO-8601 format", example = "2026-08-23T10:15:30Z")
        Instant timestamp) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, null, data, Instant.now());
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data, Instant.now());
    }

    public static <T> ApiResponse<T> success(String message) {
        return new ApiResponse<>(true, message, null, Instant.now());
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, message, null, Instant.now());
    }
}
