package com.fininsight.transaction.dto;

import com.fininsight.transaction.Transaction;
import com.fininsight.transaction.TransactionType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Public response DTO for a transaction.
 */
@Schema(description = "Financial transaction response payload")
public record TransactionResponse(
        @Schema(description = "Transaction unique identifier", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
        UUID id,

        @Schema(description = "Category unique identifier", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID categoryId,

        @Schema(description = "Category name", example = "Groceries")
        String categoryName,

        @Schema(description = "Monetary amount", example = "1250.5000")
        BigDecimal amount,

        @Schema(description = "Transaction type (INCOME or EXPENSE)", example = "EXPENSE")
        TransactionType type,

        @Schema(description = "Transaction description or notes", example = "Grocery shopping at supermarket")
        String description,

        @Schema(description = "Date the transaction occurred", example = "2026-08-23")
        LocalDate transactionDate,

        @Schema(description = "Record creation timestamp", example = "2026-08-23T10:15:30Z")
        Instant createdAt,

        @Schema(description = "Record last update timestamp", example = "2026-08-23T10:15:30Z")
        Instant updatedAt
) {

    public static TransactionResponse fromEntity(Transaction transaction, String categoryName) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getCategoryId(),
                categoryName,
                transaction.getAmount(),
                transaction.getType(),
                transaction.getDescription(),
                transaction.getTransactionDate(),
                transaction.getCreatedAt(),
                transaction.getUpdatedAt()
        );
    }
}
