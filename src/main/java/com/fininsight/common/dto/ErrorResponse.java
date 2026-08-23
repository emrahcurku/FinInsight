package com.fininsight.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import org.slf4j.MDC;

import java.time.Instant;
import java.util.Map;

/**
 * Standard error response structure across the entire application.
 * Automatically attaches the correlation ID from the tracing context.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Standard application error response payload")
public record ErrorResponse(
        @Schema(description = "Indicates request failure", example = "false")
        boolean success,

        @Schema(description = "High-level error explanation", example = "Validation failed")
        String message,

        @Schema(description = "HTTP status code", example = "400")
        int status,

        @Schema(description = "Request URI path where the error originated", example = "/api/v1/transactions")
        String path,

        @Schema(description = "Distributed trace correlation ID for diagnostics", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
        String correlationId,

        @Schema(description = "Map of field-level validation errors (if applicable)", example = "{\"amount\": \"Amount must be greater than 0\"}")
        Map<String, String> errors,

        @Schema(description = "Timestamp when error was captured", example = "2026-08-23T10:15:30Z")
        Instant timestamp) {

    public static ErrorResponse of(String message, int status, String path) {
        String correlationId = MDC.get("correlationId");
        return new ErrorResponse(false, message, status, path, correlationId, null, Instant.now());
    }

    public static ErrorResponse of(String message, int status, String path, Map<String, String> errors) {
        String correlationId = MDC.get("correlationId");
        return new ErrorResponse(false, message, status, path, correlationId, errors, Instant.now());
    }
}
