package com.fininsight.budget;

import com.fininsight.auth.UserPrincipal;
import com.fininsight.budget.dto.BudgetResponse;
import com.fininsight.budget.dto.CreateBudgetRequest;
import com.fininsight.budget.dto.UpdateBudgetRequest;
import com.fininsight.common.dto.ApiResponse;
import com.fininsight.common.dto.PagedResponse;
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

import java.util.UUID;

/**
 * REST Controller for monthly category budget management and threshold analytics.
 */
@Tag(name = "Budgets", description = "Monthly category budget management endpoints")
@SecurityRequirement(name = "Bearer Authentication")
@Validated
@RestController
@RequestMapping("/api/v1/budgets")
public class BudgetController {

    private final BudgetService budgetService;

    public BudgetController(BudgetService budgetService) {
        this.budgetService = budgetService;
    }

    @Operation(
            summary = "Create monthly budget",
            description = "Creates a new category budget for a specific month and year."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Budget created successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthenticated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Category access forbidden"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Category not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Budget already exists for this period")
    })
    @PostMapping
    public ResponseEntity<ApiResponse<BudgetResponse>> createBudget(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateBudgetRequest request
    ) {
        BudgetResponse response = budgetService.createBudget(principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Budget created successfully", response));
    }

    @Operation(
            summary = "Get budget by ID",
            description = "Retrieves a single budget along with live spending calculation."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Budget retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthenticated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Budget not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BudgetResponse>> getBudget(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id
    ) {
        BudgetResponse response = budgetService.getBudget(principal.getId(), id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(
            summary = "List budgets",
            description = "Retrieves a paginated list of budgets with optional year, month, and category filters."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Budgets retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthenticated")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<BudgetResponse>>> listBudgets(
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "Filter by year (e.g. 2026)", example = "2026")
            @RequestParam(required = false) Integer year,
            @Parameter(description = "Filter by month (1-12)", example = "8")
            @RequestParam(required = false) Integer month,
            @Parameter(description = "Filter by category UUID", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            @RequestParam(required = false) UUID categoryId,
            @Parameter(description = "Zero-based page index", example = "0")
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "Page index must be 0 or greater") int page,
            @Parameter(description = "Page size (1 to 100)", example = "20")
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "Page size must be at least 1") @Max(value = 100, message = "Page size cannot exceed 100") int size
    ) {
        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Order.desc("year"), Sort.Order.desc("month"), Sort.Order.asc("id"))
        );

        PagedResponse<BudgetResponse> response = budgetService.listBudgets(principal.getId(), year, month, categoryId, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(
            summary = "Update budget limit",
            description = "Updates the amount limit of an existing budget."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Budget updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthenticated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Budget not found")
    })
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<BudgetResponse>> updateBudget(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateBudgetRequest request
    ) {
        BudgetResponse response = budgetService.updateBudget(principal.getId(), id, request);
        return ResponseEntity.ok(ApiResponse.success("Budget updated successfully", response));
    }

    @Operation(
            summary = "Delete budget",
            description = "Deletes a budget owned by the authenticated user."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "Budget deleted successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthenticated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Budget not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBudget(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id
    ) {
        budgetService.deleteBudget(principal.getId(), id);
        return ResponseEntity.noContent().build();
    }
}
