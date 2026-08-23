package com.fininsight.analytics;

import com.fininsight.analytics.dto.BudgetOverviewResponse;
import com.fininsight.analytics.dto.CategorySpendingResponse;
import com.fininsight.analytics.dto.FinancialSummaryAggregation;
import com.fininsight.analytics.dto.FinancialSummaryResponse;
import com.fininsight.analytics.dto.MonthlyAggregation;
import com.fininsight.analytics.dto.MonthlySummaryResponse;
import com.fininsight.analytics.dto.TopCategoryResponse;
import com.fininsight.budget.Budget;
import com.fininsight.budget.BudgetRepository;
import com.fininsight.category.Category;
import com.fininsight.category.CategoryRepository;
import com.fininsight.category.CategoryType;
import com.fininsight.common.exception.BusinessException;
import com.fininsight.common.exception.ResourceNotFoundException;
import com.fininsight.transaction.TransactionRepository;
import com.fininsight.transaction.TransactionType;
import com.fininsight.transaction.dto.CategorySpendingAggregation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AnalyticsServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private BudgetRepository budgetRepository;

    @InjectMocks
    private AnalyticsService analyticsService;

    private UUID userId;
    private UUID cat1Id;
    private UUID cat2Id;
    private Category category1;
    private Category category2;

    @BeforeEach
    public void setUp() {
        userId = UUID.randomUUID();
        cat1Id = UUID.randomUUID();
        cat2Id = UUID.randomUUID();

        category1 = new Category("Groceries", CategoryType.EXPENSE);
        category1.setId(cat1Id);

        category2 = new Category("Dining", CategoryType.EXPENSE);
        category2.setId(cat2Id);
    }

    @Test
    @DisplayName("Get financial summary calculates totals and net balance accurately")
    void testGetFinancialSummarySuccess() {
        LocalDate from = LocalDate.of(2026, 8, 1);
        LocalDate to = LocalDate.of(2026, 8, 31);

        FinancialSummaryAggregation mockAgg = new FinancialSummaryAggregation() {
            @Override
            public BigDecimal getTotalIncome() {
                return new BigDecimal("10000.00");
            }

            @Override
            public BigDecimal getTotalExpense() {
                return new BigDecimal("4500.00");
            }

            @Override
            public Long getTransactionCount() {
                return 15L;
            }
        };

        when(transactionRepository.getFinancialSummary(userId, from, to)).thenReturn(mockAgg);

        FinancialSummaryResponse response = analyticsService.getFinancialSummary(userId, from, to);

        assertThat(response.totalIncome()).isEqualByComparingTo("10000.00");
        assertThat(response.totalExpense()).isEqualByComparingTo("4500.00");
        assertThat(response.netBalance()).isEqualByComparingTo("5500.00");
        assertThat(response.transactionCount()).isEqualTo(15L);
        assertThat(response.from()).isEqualTo(from);
        assertThat(response.to()).isEqualTo(to);
    }

    @Test
    @DisplayName("Get financial summary handles negative net balance when expenses exceed income")
    void testGetFinancialSummaryNegativeBalance() {
        LocalDate from = LocalDate.of(2026, 8, 1);
        LocalDate to = LocalDate.of(2026, 8, 31);

        FinancialSummaryAggregation mockAgg = new FinancialSummaryAggregation() {
            @Override
            public BigDecimal getTotalIncome() {
                return new BigDecimal("3000.00");
            }

            @Override
            public BigDecimal getTotalExpense() {
                return new BigDecimal("5000.00");
            }

            @Override
            public Long getTransactionCount() {
                return 8L;
            }
        };

        when(transactionRepository.getFinancialSummary(userId, from, to)).thenReturn(mockAgg);

        FinancialSummaryResponse response = analyticsService.getFinancialSummary(userId, from, to);

        assertThat(response.totalIncome()).isEqualByComparingTo("3000.00");
        assertThat(response.totalExpense()).isEqualByComparingTo("5000.00");
        assertThat(response.netBalance()).isEqualByComparingTo("-2000.00");
    }

    @Test
    @DisplayName("Get financial summary handles null aggregation values gracefully")
    void testGetFinancialSummaryNullAggregation() {
        LocalDate from = LocalDate.of(2026, 8, 1);
        LocalDate to = LocalDate.of(2026, 8, 31);

        when(transactionRepository.getFinancialSummary(userId, from, to)).thenReturn(null);

        FinancialSummaryResponse response = analyticsService.getFinancialSummary(userId, from, to);

        assertThat(response.totalIncome()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.totalExpense()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.netBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.transactionCount()).isEqualTo(0L);
    }

    @Test
    @DisplayName("Invalid date range where from > to throws 400 Bad Request")
    void testDateRangeValidationFromAfterTo() {
        LocalDate from = LocalDate.of(2026, 8, 31);
        LocalDate to = LocalDate.of(2026, 8, 1);

        assertThatThrownBy(() -> analyticsService.getFinancialSummary(userId, from, to))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(be.getMessage()).contains("Invalid date range");
                });
    }

    @Test
    @DisplayName("Get spending by category calculates proportions and resolves category names")
    void testGetSpendingByCategorySuccess() {
        LocalDate from = LocalDate.of(2026, 8, 1);
        LocalDate to = LocalDate.of(2026, 8, 31);

        CategorySpendingAggregation agg1 = new CategorySpendingAggregation() {
            @Override
            public UUID getCategoryId() {
                return cat1Id;
            }

            @Override
            public BigDecimal getTotalAmount() {
                return new BigDecimal("750.00");
            }
        };

        CategorySpendingAggregation agg2 = new CategorySpendingAggregation() {
            @Override
            public UUID getCategoryId() {
                return cat2Id;
            }

            @Override
            public BigDecimal getTotalAmount() {
                return new BigDecimal("250.00");
            }
        };

        when(transactionRepository.getSpendingByCategory(userId, from, to)).thenReturn(List.of(agg1, agg2));
        when(categoryRepository.findAllById(org.mockito.ArgumentMatchers.<Iterable<UUID>>any())).thenReturn(List.of(category1, category2));

        List<CategorySpendingResponse> responses = analyticsService.getSpendingByCategory(userId, from, to);

        assertThat(responses).hasSize(2);

        CategorySpendingResponse r1 = responses.get(0);
        assertThat(r1.categoryId()).isEqualTo(cat1Id);
        assertThat(r1.categoryName()).isEqualTo("Groceries");
        assertThat(r1.amount()).isEqualByComparingTo("750.00");
        assertThat(r1.percentage()).isEqualByComparingTo("75.000");

        CategorySpendingResponse r2 = responses.get(1);
        assertThat(r2.categoryId()).isEqualTo(cat2Id);
        assertThat(r2.categoryName()).isEqualTo("Dining");
        assertThat(r2.amount()).isEqualByComparingTo("250.00");
        assertThat(r2.percentage()).isEqualByComparingTo("25.000");
    }

    @Test
    @DisplayName("Get spending by category returns empty list when no expense transactions exist")
    void testGetSpendingByCategoryEmpty() {
        LocalDate from = LocalDate.of(2026, 8, 1);
        LocalDate to = LocalDate.of(2026, 8, 31);

        when(transactionRepository.getSpendingByCategory(userId, from, to)).thenReturn(Collections.emptyList());

        List<CategorySpendingResponse> responses = analyticsService.getSpendingByCategory(userId, from, to);

        assertThat(responses).isEmpty();
    }

    @Test
    @DisplayName("Get monthly summary groups transactions by year and month")
    void testGetMonthlySummarySuccess() {
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = LocalDate.of(2026, 12, 31);

        MonthlyAggregation m1 = new MonthlyAggregation() {
            @Override
            public Integer getYear() {
                return 2026;
            }

            @Override
            public Integer getMonth() {
                return 8;
            }

            @Override
            public BigDecimal getTotalIncome() {
                return new BigDecimal("8000.00");
            }

            @Override
            public BigDecimal getTotalExpense() {
                return new BigDecimal("3500.00");
            }
        };

        MonthlyAggregation m2 = new MonthlyAggregation() {
            @Override
            public Integer getYear() {
                return 2026;
            }

            @Override
            public Integer getMonth() {
                return 7;
            }

            @Override
            public BigDecimal getTotalIncome() {
                return new BigDecimal("7000.00");
            }

            @Override
            public BigDecimal getTotalExpense() {
                return new BigDecimal("4000.00");
            }
        };

        when(transactionRepository.getMonthlySummary(userId, from, to)).thenReturn(List.of(m1, m2));

        List<MonthlySummaryResponse> responses = analyticsService.getMonthlySummary(userId, from, to);

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).year()).isEqualTo(2026);
        assertThat(responses.get(0).month()).isEqualTo(8);
        assertThat(responses.get(0).totalIncome()).isEqualByComparingTo("8000.00");
        assertThat(responses.get(0).totalExpense()).isEqualByComparingTo("3500.00");
        assertThat(responses.get(0).netBalance()).isEqualByComparingTo("4500.00");

        assertThat(responses.get(1).month()).isEqualTo(7);
        assertThat(responses.get(1).netBalance()).isEqualByComparingTo("3000.00");
    }

    @Test
    @DisplayName("Get budget overview aggregates multiple budgets and distributes threshold statuses")
    void testGetBudgetOverviewSuccess() {
        int year = 2026;
        int month = 8;

        Budget b1 = new Budget(userId, cat1Id, new BigDecimal("1000.00"), month, year);
        Budget b2 = new Budget(userId, cat2Id, new BigDecimal("2000.00"), month, year);

        when(budgetRepository.findByUserIdAndYearAndMonth(userId, (short) year, (short) month))
                .thenReturn(List.of(b1, b2));

        CategorySpendingAggregation sp1 = new CategorySpendingAggregation() {
            @Override
            public UUID getCategoryId() {
                return cat1Id;
            }

            @Override
            public BigDecimal getTotalAmount() {
                return new BigDecimal("500.00"); // 50% -> NORMAL (<80%)
            }
        };

        CategorySpendingAggregation sp2 = new CategorySpendingAggregation() {
            @Override
            public UUID getCategoryId() {
                return cat2Id;
            }

            @Override
            public BigDecimal getTotalAmount() {
                return new BigDecimal("1800.00"); // 90% -> WARNING (80-99.99%)
            }
        };

        when(transactionRepository.sumAmountGroupedByCategory(
                eq(userId), org.mockito.ArgumentMatchers.<Collection<UUID>>any(), eq(TransactionType.EXPENSE), any(LocalDate.class), any(LocalDate.class)
        )).thenReturn(List.of(sp1, sp2));

        BudgetOverviewResponse response = analyticsService.getBudgetOverview(userId, year, month);

        assertThat(response.totalBudget()).isEqualByComparingTo("3000.00");
        assertThat(response.totalSpent()).isEqualByComparingTo("2300.00");
        assertThat(response.totalRemaining()).isEqualByComparingTo("700.00");
        assertThat(response.overallUsagePercentage()).isEqualByComparingTo("76.667");
        assertThat(response.normalBudgetCount()).isEqualTo(1);
        assertThat(response.warningBudgetCount()).isEqualTo(1);
        assertThat(response.exceededBudgetCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("Get budget overview returns zero metrics when user has no budgets for the period")
    void testGetBudgetOverviewEmpty() {
        when(budgetRepository.findByUserIdAndYearAndMonth(userId, (short) 2026, (short) 8))
                .thenReturn(Collections.emptyList());

        BudgetOverviewResponse response = analyticsService.getBudgetOverview(userId, 2026, 8);

        assertThat(response.totalBudget()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.totalSpent()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.totalRemaining()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.overallUsagePercentage()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.normalBudgetCount()).isEqualTo(0);
        assertThat(response.warningBudgetCount()).isEqualTo(0);
        assertThat(response.exceededBudgetCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("Get budget overview with invalid month throws 400 Bad Request")
    void testGetBudgetOverviewInvalidMonth() {
        assertThatThrownBy(() -> analyticsService.getBudgetOverview(userId, 2026, 13))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(be.getMessage()).contains("Month must be between 1 and 12");
                });
    }

    @Test
    @DisplayName("Get top category returns highest expense category")
    void testGetTopCategorySuccess() {
        LocalDate from = LocalDate.of(2026, 8, 1);
        LocalDate to = LocalDate.of(2026, 8, 31);

        CategorySpendingAggregation agg1 = new CategorySpendingAggregation() {
            @Override
            public UUID getCategoryId() {
                return cat1Id;
            }

            @Override
            public BigDecimal getTotalAmount() {
                return new BigDecimal("3500.00");
            }
        };

        when(transactionRepository.getSpendingByCategory(userId, from, to)).thenReturn(List.of(agg1));
        when(categoryRepository.findById(cat1Id)).thenReturn(Optional.of(category1));

        TopCategoryResponse response = analyticsService.getTopCategory(userId, from, to);

        assertThat(response.categoryId()).isEqualTo(cat1Id);
        assertThat(response.categoryName()).isEqualTo("Groceries");
        assertThat(response.amount()).isEqualByComparingTo("3500.00");
    }

    @Test
    @DisplayName("Get top category throws 404 Not Found when no expense transactions exist")
    void testGetTopCategoryEmptyThrows404() {
        LocalDate from = LocalDate.of(2026, 8, 1);
        LocalDate to = LocalDate.of(2026, 8, 31);

        when(transactionRepository.getSpendingByCategory(userId, from, to)).thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> analyticsService.getTopCategory(userId, from, to))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("No expense transactions found for the specified period");
    }
}
