package com.fininsight.dashboard.dto;

import com.fininsight.transaction.TransactionType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Recent transaction item displayed on dashboard.
 */
@Schema(description = "Recent transaction entry on dashboard")
public record RecentTransactionDTO(
        @Schema(description = "Transaction UUID", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID id,

        @Schema(description = "Category UUID", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID categoryId,

        @Schema(description = "Category display name", example = "Groceries")
        String categoryName,

        @Schema(description = "Transaction monetary amount", example = "125.50")
        BigDecimal amount,

        @Schema(description = "Transaction type (INCOME or EXPENSE)", example = "EXPENSE")
        TransactionType type,

        @Schema(description = "Optional transaction description/notes", example = "Weekly supermarket shopping")
        String description,

        @Schema(description = "Date when transaction occurred", example = "2026-08-20")
        LocalDate transactionDate
) {
}
