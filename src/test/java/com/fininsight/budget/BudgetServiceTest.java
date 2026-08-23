package com.fininsight.budget;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;

import com.fininsight.budget.dto.BudgetResponse;
import com.fininsight.budget.dto.CreateBudgetRequest;
import com.fininsight.budget.dto.UpdateBudgetRequest;
import com.fininsight.category.Category;
import com.fininsight.category.CategoryRepository;
import com.fininsight.category.CategoryType;
import com.fininsight.common.cache.CacheEvictionService;
import com.fininsight.common.dto.PagedResponse;
import com.fininsight.common.event.BudgetEvent;
import com.fininsight.common.event.DomainEventPublisher;
import com.fininsight.common.exception.BusinessException;
import com.fininsight.common.exception.ResourceNotFoundException;
import com.fininsight.transaction.TransactionRepository;
import com.fininsight.transaction.TransactionType;
import com.fininsight.transaction.dto.CategorySpendingAggregation;

@ExtendWith(MockitoExtension.class)
public class BudgetServiceTest {

    @Mock
    private BudgetRepository budgetRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private CacheEvictionService cacheEvictionService;

    @Mock
    private DomainEventPublisher domainEventPublisher;

    @InjectMocks
    private BudgetService budgetService;

    private UUID userId;
    private UUID systemCategoryId;
    private UUID userCategoryId;
    private UUID otherUserCategoryId;
    private Category systemCategory;
    private Category userCategory;
    private Category otherUserCategory;

    @BeforeEach
    public void setUp() {
        userId = UUID.randomUUID();

        systemCategoryId = UUID.randomUUID();
        systemCategory = new Category("Groceries", CategoryType.EXPENSE);
        systemCategory.setId(systemCategoryId);

        userCategoryId = UUID.randomUUID();
        userCategory = new Category(userId, "My Custom Category", CategoryType.EXPENSE);
        userCategory.setId(userCategoryId);

        otherUserCategoryId = UUID.randomUUID();
        otherUserCategory = new Category(UUID.randomUUID(), "Secret Category", CategoryType.EXPENSE);
        otherUserCategory.setId(otherUserCategoryId);
    }

    @Test
    @DisplayName("Create budget with system category succeeds")
    void testCreateBudgetWithSystemCategory() {
        CreateBudgetRequest request = new CreateBudgetRequest(
                systemCategoryId,
                new BigDecimal("5000.00"),
                8,
                2026
        );

        when(categoryRepository.findById(systemCategoryId)).thenReturn(Optional.of(systemCategory));
        when(budgetRepository.existsByUserIdAndCategoryIdAndYearAndMonth(userId, systemCategoryId, (short) 2026, (short) 8))
                .thenReturn(false);
        when(budgetRepository.save(any(Budget.class))).thenAnswer(invocation -> {
            Budget b = invocation.getArgument(0);
            b.setId(UUID.randomUUID());
            return b;
        });
        when(transactionRepository.sumAmountByUserIdAndCategoryIdAndTypeAndDateRange(
                eq(userId), eq(systemCategoryId), eq(TransactionType.EXPENSE), any(LocalDate.class), any(LocalDate.class)
        )).thenReturn(new BigDecimal("1500.00"));

        BudgetResponse response = budgetService.createBudget(userId, request);

        assertThat(response).isNotNull();
        assertThat(response.categoryId()).isEqualTo(systemCategoryId);
        assertThat(response.categoryName()).isEqualTo("Groceries");
        assertThat(response.amount()).isEqualByComparingTo("5000.00");
        assertThat(response.spentAmount()).isEqualByComparingTo("1500.00");
        assertThat(response.remainingAmount()).isEqualByComparingTo("3500.00");
        assertThat(response.usagePercentage()).isEqualByComparingTo("30.000");
        assertThat(response.thresholdStatus()).isEqualTo(ThresholdStatus.NORMAL);
        verify(cacheEvictionService).evictUserBudgetCaches(userId);
        verify(domainEventPublisher).publish(any(BudgetEvent.class));
    }

    @Test
    @DisplayName("Create budget with another user's category throws 403 Forbidden")
    void testCreateBudgetWithAnotherUserCategoryForbidden() {
        CreateBudgetRequest request = new CreateBudgetRequest(
                otherUserCategoryId,
                new BigDecimal("5000.00"),
                8,
                2026
        );

        when(categoryRepository.findById(otherUserCategoryId)).thenReturn(Optional.of(otherUserCategory));

        assertThatThrownBy(() -> budgetService.createBudget(userId, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(be.getMessage()).contains("Access to this category is forbidden");
                });
    }

    @Test
    @DisplayName("Create duplicate budget for same category and period throws 409 Conflict")
    void testCreateDuplicateBudgetConflict() {
        CreateBudgetRequest request = new CreateBudgetRequest(
                systemCategoryId,
                new BigDecimal("5000.00"),
                8,
                2026
        );

        when(categoryRepository.findById(systemCategoryId)).thenReturn(Optional.of(systemCategory));
        when(budgetRepository.existsByUserIdAndCategoryIdAndYearAndMonth(userId, systemCategoryId, (short) 2026, (short) 8))
                .thenReturn(true);

        assertThatThrownBy(() -> budgetService.createBudget(userId, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(be.getMessage()).contains("already exists");
                });
    }

    @Test
    @DisplayName("Get own budget by ID returns 200 OK")
    void testGetOwnBudgetSuccess() {
        UUID budgetId = UUID.randomUUID();
        Budget budget = new Budget(userId, systemCategoryId, new BigDecimal("5000.00"), 8, 2026);
        budget.setId(budgetId);

        when(budgetRepository.findByIdAndUserId(budgetId, userId)).thenReturn(Optional.of(budget));
        when(categoryRepository.findById(systemCategoryId)).thenReturn(Optional.of(systemCategory));
        when(transactionRepository.sumAmountByUserIdAndCategoryIdAndTypeAndDateRange(
                eq(userId), eq(systemCategoryId), eq(TransactionType.EXPENSE), any(LocalDate.class), any(LocalDate.class)
        )).thenReturn(new BigDecimal("4200.00"));

        BudgetResponse response = budgetService.getBudget(userId, budgetId);

        assertThat(response.amount()).isEqualByComparingTo("5000.00");
        assertThat(response.spentAmount()).isEqualByComparingTo("4200.00");
        assertThat(response.remainingAmount()).isEqualByComparingTo("800.00");
        assertThat(response.usagePercentage()).isEqualByComparingTo("84.000");
        assertThat(response.thresholdStatus()).isEqualTo(ThresholdStatus.WARNING);
    }

    @Test
    @DisplayName("Get another user's budget returns 404 Not Found (IDOR)")
    void testGetAnotherUserBudgetReturns404() {
        UUID budgetId = UUID.randomUUID();
        when(budgetRepository.findByIdAndUserId(budgetId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> budgetService.getBudget(userId, budgetId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Budget not found");
    }

    @Test
    @DisplayName("Update own budget limit succeeds")
    void testUpdateOwnBudgetSuccess() {
        UUID budgetId = UUID.randomUUID();
        Budget budget = new Budget(userId, systemCategoryId, new BigDecimal("5000.00"), 8, 2026);
        budget.setId(budgetId);

        UpdateBudgetRequest request = new UpdateBudgetRequest(new BigDecimal("6000.00"));

        when(budgetRepository.findByIdAndUserId(budgetId, userId)).thenReturn(Optional.of(budget));
        when(budgetRepository.save(any(Budget.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(categoryRepository.findById(systemCategoryId)).thenReturn(Optional.of(systemCategory));
        when(transactionRepository.sumAmountByUserIdAndCategoryIdAndTypeAndDateRange(
                eq(userId), eq(systemCategoryId), eq(TransactionType.EXPENSE), any(LocalDate.class), any(LocalDate.class)
        )).thenReturn(new BigDecimal("6500.00"));

        BudgetResponse response = budgetService.updateBudget(userId, budgetId, request);

        assertThat(response.amount()).isEqualByComparingTo("6000.00");
        assertThat(response.spentAmount()).isEqualByComparingTo("6500.00");
        assertThat(response.remainingAmount()).isEqualByComparingTo("-500.00");
        assertThat(response.usagePercentage()).isEqualByComparingTo("108.333");
        assertThat(response.thresholdStatus()).isEqualTo(ThresholdStatus.EXCEEDED);
        verify(cacheEvictionService).evictUserBudgetCaches(userId);
        verify(domainEventPublisher).publish(any(BudgetEvent.class));
    }

    @Test
    @DisplayName("Delete own budget succeeds")
    void testDeleteOwnBudgetSuccess() {
        UUID budgetId = UUID.randomUUID();
        Budget budget = new Budget(userId, systemCategoryId, new BigDecimal("5000.00"), 8, 2026);
        budget.setId(budgetId);

        when(budgetRepository.findByIdAndUserId(budgetId, userId)).thenReturn(Optional.of(budget));

        budgetService.deleteBudget(userId, budgetId);

        verify(budgetRepository).delete(budget);
        verify(cacheEvictionService).evictUserBudgetCaches(userId);
        verify(domainEventPublisher).publish(any(BudgetEvent.class));
    }

    @Test
    @DisplayName("List budgets resolves categories and spending in batch without N+1 queries")
    void testListBudgetsBatchResolution() {
        Pageable pageable = PageRequest.of(0, 20);
        Budget b1 = new Budget(userId, systemCategoryId, new BigDecimal("1000.00"), 8, 2026);
        b1.setId(UUID.randomUUID());
        Budget b2 = new Budget(userId, userCategoryId, new BigDecimal("2000.00"), 8, 2026);
        b2.setId(UUID.randomUUID());

        Page<Budget> mockPage = new PageImpl<>(List.of(b1, b2), pageable, 2);

        when(budgetRepository.findAll(ArgumentMatchers.<Specification<Budget>>any(), eq(pageable))).thenReturn(mockPage);
        when(categoryRepository.findAllById(ArgumentMatchers.<Iterable<UUID>>any())).thenReturn(List.of(systemCategory, userCategory));

        CategorySpendingAggregation agg1 = new CategorySpendingAggregation() {
            @Override
            public UUID getCategoryId() {
                return systemCategoryId;
            }

            @Override
            public BigDecimal getTotalAmount() {
                return new BigDecimal("500.00");
            }
        };

        when(transactionRepository.sumAmountGroupedByCategory(
                eq(userId),
                ArgumentMatchers.<Collection<UUID>>any(),
                eq(TransactionType.EXPENSE),
                any(LocalDate.class),
                any(LocalDate.class)
        )).thenReturn(List.of(agg1));

        PagedResponse<BudgetResponse> response = budgetService.listBudgets(userId, 2026, 8, null, pageable);

        assertThat(response.content()).hasSize(2);
        assertThat(response.content().get(0).categoryName()).isEqualTo("Groceries");
        assertThat(response.content().get(0).spentAmount()).isEqualByComparingTo("500.00");
        assertThat(response.content().get(0).thresholdStatus()).isEqualTo(ThresholdStatus.NORMAL);

        assertThat(response.content().get(1).categoryName()).isEqualTo("My Custom Category");
        assertThat(response.content().get(1).spentAmount()).isEqualByComparingTo("0.0000");
    }

    @Test
    @DisplayName("Threshold status boundary evaluation: <80% NORMAL, 80-99.999% WARNING, >=100% EXCEEDED")
    void testThresholdStatusBoundaries() {
        assertThat(ThresholdStatus.fromPercentage(null)).isEqualTo(ThresholdStatus.NORMAL);
        assertThat(ThresholdStatus.fromPercentage(BigDecimal.ZERO)).isEqualTo(ThresholdStatus.NORMAL);
        assertThat(ThresholdStatus.fromPercentage(new BigDecimal("79.999"))).isEqualTo(ThresholdStatus.NORMAL);
        assertThat(ThresholdStatus.fromPercentage(new BigDecimal("80.000"))).isEqualTo(ThresholdStatus.WARNING);
        assertThat(ThresholdStatus.fromPercentage(new BigDecimal("99.999"))).isEqualTo(ThresholdStatus.WARNING);
        assertThat(ThresholdStatus.fromPercentage(new BigDecimal("100.000"))).isEqualTo(ThresholdStatus.EXCEEDED);
        assertThat(ThresholdStatus.fromPercentage(new BigDecimal("100.001"))).isEqualTo(ThresholdStatus.EXCEEDED);
        assertThat(ThresholdStatus.fromPercentage(new BigDecimal("150.000"))).isEqualTo(ThresholdStatus.EXCEEDED);
    }
}
