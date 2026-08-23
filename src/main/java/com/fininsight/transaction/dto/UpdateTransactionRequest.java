package com.fininsight.transaction.dto;

import com.fininsight.transaction.TransactionType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Request payload for updating an existing transaction.
 */
@Schema(description = "Payload for updating a financial transaction")
public record UpdateTransactionRequest(
        @Schema(description = "UUID of the category (system or user-owned)", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        @NotNull(message = "Category ID is required")
        UUID categoryId,

        @Schema(description = "Monetary amount (must be positive)", example = "1500.00")
        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.0001", message = "Amount must be greater than 0")
        @Digits(integer = 15, fraction = 4, message = "Amount exceeds maximum precision (15 integer digits, 4 decimal places)")
        BigDecimal amount,

        @Schema(description = "Transaction type (INCOME or EXPENSE)", example = "EXPENSE")
        @NotNull(message = "Transaction type is required")
        TransactionType type,

        @Schema(description = "Optional transaction description or notes", example = "Updated monthly groceries")
        @Size(max = 255, message = "Description cannot exceed 255 characters")
        String description,

        @Schema(description = "Date the transaction occurred", example = "2026-08-23")
        @NotNull(message = "Transaction date is required")
        LocalDate transactionDate
) {
}
