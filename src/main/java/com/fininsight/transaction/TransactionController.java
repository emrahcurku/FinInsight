package com.fininsight.transaction;

import com.fininsight.auth.UserPrincipal;
import com.fininsight.common.dto.ApiResponse;
import com.fininsight.common.dto.PagedResponse;
import com.fininsight.transaction.dto.CreateTransactionRequest;
import com.fininsight.transaction.dto.TransactionResponse;
import com.fininsight.transaction.dto.UpdateTransactionRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

/**
 * REST controller for financial transaction CRUD operations,
 * pagination, and dynamic multi-criteria filtering.
 */
@Tag(name = "Transactions", description = "Financial transaction management endpoints")
@SecurityRequirement(name = "Bearer Authentication")
@Validated
@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @Operation(
            summary = "Create transaction",
            description = "Creates a new financial transaction for the authenticated user."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Transaction created successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthenticated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Category access forbidden"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Category not found")
    })
    @PostMapping
    public ResponseEntity<ApiResponse<TransactionResponse>> createTransaction(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateTransactionRequest request
    ) {
        TransactionResponse response = transactionService.createTransaction(principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Transaction created successfully", response));
    }

    @Operation(
            summary = "Get transaction by ID",
            description = "Retrieves a single transaction by its ID for the authenticated user."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Transaction retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthenticated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Transaction not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TransactionResponse>> getTransaction(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id
    ) {
        TransactionResponse response = transactionService.getTransaction(principal.getId(), id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(
            summary = "List transactions",
            description = "Retrieves a paginated list of transactions with optional date range, type, and category filters."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Transactions retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid date range or parameters"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthenticated")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<TransactionResponse>>> listTransactions(
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "Start date filter (inclusive, YYYY-MM-DD)", example = "2026-01-01")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "End date filter (inclusive, YYYY-MM-DD)", example = "2026-08-23")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @Parameter(description = "Transaction type filter (INCOME or EXPENSE)", example = "EXPENSE")
            @RequestParam(required = false) TransactionType type,
            @Parameter(description = "Category UUID filter", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            @RequestParam(required = false) UUID categoryId,
            @Parameter(description = "Zero-based page index", example = "0")
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "Page index must be 0 or greater") int page,
            @Parameter(description = "Page size (1 to 100)", example = "20")
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "Page size must be at least 1") @Max(value = 100, message = "Page size cannot exceed 100") int size
    ) {
        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Order.desc("transactionDate"), Sort.Order.desc("id"))
        );

        PagedResponse<TransactionResponse> response = transactionService.listTransactions(
                principal.getId(), from, to, type, categoryId, pageable
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(
            summary = "Update transaction",
            description = "Updates an existing transaction for the authenticated user."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Transaction updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthenticated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Category access forbidden"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Transaction or Category not found")
    })
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TransactionResponse>> updateTransaction(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTransactionRequest request
    ) {
        TransactionResponse response = transactionService.updateTransaction(principal.getId(), id, request);
        return ResponseEntity.ok(ApiResponse.success("Transaction updated successfully", response));
    }

    @Operation(
            summary = "Delete transaction",
            description = "Deletes an existing transaction for the authenticated user."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "Transaction deleted successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthenticated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Transaction not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTransaction(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id
    ) {
        transactionService.deleteTransaction(principal.getId(), id);
        return ResponseEntity.noContent().build();
    }
}
