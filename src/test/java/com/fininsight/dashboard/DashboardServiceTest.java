package com.fininsight.dashboard;

import com.fininsight.analytics.dto.FinancialSummaryAggregation;
import com.fininsight.analytics.dto.MonthlyAggregation;
import com.fininsight.budget.Budget;
import com.fininsight.budget.BudgetRepository;
import com.fininsight.category.Category;
import com.fininsight.category.CategoryRepository;
import com.fininsight.category.CategoryType;
import com.fininsight.common.exception.BusinessException;
import com.fininsight.dashboard.dto.DashboardResponse;
import com.fininsight.dashboard.dto.InsightSeverity;
import com.fininsight.dashboard.dto.TrendDirection;
import com.fininsight.transaction.Transaction;
import com.fininsight.transaction.TransactionRepository;
import com.fininsight.transaction.TransactionType;
import com.fininsight.transaction.dto.CategorySpendingAggregation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DashboardServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private BudgetRepository budgetRepository;

    @InjectMocks
    private DashboardService dashboardService;

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
    @DisplayName("Get dashboard aggregates all composite sections successfully")
    void testDashboardFullSuccess() {
        LocalDate from = LocalDate.of(2026, 8, 1);
        LocalDate to = LocalDate.of(2026, 8, 31);

        // 1. Summary Mock
        FinancialSummaryAggregation summaryAgg = new FinancialSummaryAggregation() {
            @Override
            public BigDecimal getTotalIncome() {
                return new BigDecimal("10000.00");
            }

            @Override
            public BigDecimal getTotalExpense() {
                return new BigDecimal("4000.00");
            }

            @Override
            public Long getTransactionCount() {
                return 5L;
            }
        };
        when(transactionRepository.getFinancialSummary(userId, from, to)).thenReturn(summaryAgg);

        // 2. 6-Month Monthly Trend Mock
        MonthlyAggregation mAgg = new MonthlyAggregation() {
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
                return new BigDecimal("10000.00");
            }

            @Override
            public BigDecimal getTotalExpense() {
                return new BigDecimal("4000.00");
            }
        };
        when(transactionRepository.getMonthlySummary(eq(userId), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(mAgg));

        // 3. Category Spending Mock
        CategorySpendingAggregation sp1 = new CategorySpendingAggregation() {
            @Override
            public UUID getCategoryId() {
                return cat1Id;
            }

            @Override
            public BigDecimal getTotalAmount() {
                return new BigDecimal("3000.00");
            }
        };
        CategorySpendingAggregation sp2 = new CategorySpendingAggregation() {
            @Override
            public UUID getCategoryId() {
                return cat2Id;
            }

            @Override
            public BigDecimal getTotalAmount() {
                return new BigDecimal("1000.00");
            }
        };
        when(transactionRepository.getSpendingByCategory(userId, from, to)).thenReturn(List.of(sp1, sp2));
        when(categoryRepository.findAllById(ArgumentMatchers.<Iterable<UUID>>any()))
                .thenReturn(List.of(category1, category2));

        // 4. Budget Overview Mock
        Budget budget1 = new Budget(userId, cat1Id, new BigDecimal("5000.00"), 8, 2026);
        when(budgetRepository.findByUserIdAndYearAndMonth(userId, (short) 2026, (short) 8))
                .thenReturn(List.of(budget1));
        when(transactionRepository.sumAmountGroupedByCategory(
                eq(userId), ArgumentMatchers.<Collection<UUID>>any(), eq(TransactionType.EXPENSE), any(LocalDate.class), any(LocalDate.class)
        )).thenReturn(List.of(sp1));

        // 5. Recent Transactions Mock
        Transaction tx1 = new Transaction(userId, cat1Id, new BigDecimal("250.00"), TransactionType.EXPENSE, "Groceries", LocalDate.of(2026, 8, 25));
        tx1.setId(UUID.randomUUID());
        when(transactionRepository.findByUserId(eq(userId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(tx1)));

        // 6. Previous Month Summary (for comparison)
        FinancialSummaryAggregation prevSummaryAgg = new FinancialSummaryAggregation() {
            @Override
            public BigDecimal getTotalIncome() {
                return new BigDecimal("8000.00");
            }

            @Override
            public BigDecimal getTotalExpense() {
                return new BigDecimal("3200.00");
            }

            @Override
            public Long getTransactionCount() {
                return 4L;
            }
        };
        when(transactionRepository.getFinancialSummary(userId, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31)))
                .thenReturn(prevSummaryAgg);

        DashboardResponse response = dashboardService.getDashboard(userId, from, to);

        assertThat(response).isNotNull();
        // Summary
        assertThat(response.financialSummary().totalIncome()).isEqualByComparingTo("10000.00");
        assertThat(response.financialSummary().totalExpense()).isEqualByComparingTo("4000.00");
        assertThat(response.financialSummary().netBalance()).isEqualByComparingTo("6000.00");
        assertThat(response.financialSummary().transactionCount()).isEqualTo(5L);

        // 6-Month Trend
        assertThat(response.monthlyTrend()).hasSize(6);
        assertThat(response.monthlyTrend().get(5).month()).isEqualTo(8);
        assertThat(response.monthlyTrend().get(5).totalIncome()).isEqualByComparingTo("10000.00");
        assertThat(response.monthlyTrend().get(0).totalIncome()).isEqualByComparingTo(BigDecimal.ZERO); // missing month zero-filled

        // Category Spending (Top 5)
        assertThat(response.categorySpending()).hasSize(2);
        assertThat(response.categorySpending().get(0).categoryName()).isEqualTo("Groceries");
        assertThat(response.categorySpending().get(0).percentage()).isEqualByComparingTo("75.000");
        assertThat(response.categorySpending().get(1).percentage()).isEqualByComparingTo("25.000");

        // Budget Overview
        assertThat(response.budgetOverview().totalBudget()).isEqualByComparingTo("5000.00");
        assertThat(response.budgetOverview().totalSpent()).isEqualByComparingTo("3000.00");
        assertThat(response.budgetOverview().totalRemaining()).isEqualByComparingTo("2000.00");
        assertThat(response.budgetOverview().overallUsagePercentage()).isEqualByComparingTo("60.000");
        assertThat(response.budgetOverview().normalBudgetCount()).isEqualTo(1);

        // Recent Transactions
        assertThat(response.recentTransactions()).hasSize(1);
        assertThat(response.recentTransactions().get(0).categoryName()).isEqualTo("Groceries");

        // Previous Month Comparison (4000 vs 3200 -> +800 = +25%)
        assertThat(response.previousMonthComparison().currentMonthExpense()).isEqualByComparingTo("4000.00");
        assertThat(response.previousMonthComparison().previousMonthExpense()).isEqualByComparingTo("3200.00");
        assertThat(response.previousMonthComparison().expenseChangeAmount()).isEqualByComparingTo("800.00");
        assertThat(response.previousMonthComparison().expenseChangePercentage()).isEqualByComparingTo("25.000");
        assertThat(response.previousMonthComparison().trend()).isEqualTo(TrendDirection.INCREASED);

        // Insights Generated
        assertThat(response.insights()).isNotEmpty();
    }

    @Test
    @DisplayName("Get dashboard handles empty user data gracefully with zero-filled trend and NO_TRANSACTIONS insight")
    void testDashboardEmptyData() {
        LocalDate from = LocalDate.of(2026, 8, 1);
        LocalDate to = LocalDate.of(2026, 8, 31);

        when(transactionRepository.getFinancialSummary(any(), any(), any())).thenReturn(null);
        when(transactionRepository.getMonthlySummary(any(), any(), any())).thenReturn(Collections.emptyList());
        when(transactionRepository.getSpendingByCategory(any(), any(), any())).thenReturn(Collections.emptyList());
        when(budgetRepository.findByUserIdAndYearAndMonth(any(), any(Short.class), any(Short.class))).thenReturn(Collections.emptyList());
        when(transactionRepository.findByUserId(any(), any())).thenReturn(new PageImpl<>(Collections.emptyList()));

        DashboardResponse response = dashboardService.getDashboard(userId, from, to);

        assertThat(response.financialSummary().totalIncome()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.financialSummary().totalExpense()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.financialSummary().transactionCount()).isEqualTo(0L);

        assertThat(response.monthlyTrend()).hasSize(6);
        for (int i = 0; i < 6; i++) {
            assertThat(response.monthlyTrend().get(i).totalIncome()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(response.monthlyTrend().get(i).totalExpense()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        assertThat(response.categorySpending()).isEmpty();
        assertThat(response.budgetOverview().totalBudget()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.recentTransactions()).isEmpty();
        assertThat(response.previousMonthComparison().trend()).isEqualTo(TrendDirection.UNCHANGED);

        assertThat(response.insights()).hasSize(1);
        assertThat(response.insights().get(0).type()).isEqualTo("NO_TRANSACTIONS");
        assertThat(response.insights().get(0).severity()).isEqualTo(InsightSeverity.INFO);
    }

    @Test
    @DisplayName("Invalid date range where from > to throws 400 Bad Request")
    void testDashboardInvalidDateRange() {
        LocalDate from = LocalDate.of(2026, 8, 31);
        LocalDate to = LocalDate.of(2026, 8, 1);

        assertThatThrownBy(() -> dashboardService.getDashboard(userId, from, to))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(be.getMessage()).contains("Invalid date range");
                });
    }

    @Test
    @DisplayName("Budget exceeding limit triggers BUDGET_EXCEEDED insight with DANGER severity")
    void testDashboardBudgetExceededInsight() {
        LocalDate from = LocalDate.of(2026, 8, 1);
        LocalDate to = LocalDate.of(2026, 8, 31);

        FinancialSummaryAggregation summaryAgg = new FinancialSummaryAggregation() {
            @Override
            public BigDecimal getTotalIncome() {
                return new BigDecimal("5000.00");
            }

            @Override
            public BigDecimal getTotalExpense() {
                return new BigDecimal("6000.00");
            }

            @Override
            public Long getTransactionCount() {
                return 3L;
            }
        };
        when(transactionRepository.getFinancialSummary(userId, from, to)).thenReturn(summaryAgg);
        when(transactionRepository.getMonthlySummary(any(), any(), any())).thenReturn(Collections.emptyList());
        when(transactionRepository.getSpendingByCategory(any(), any(), any())).thenReturn(Collections.emptyList());
        when(transactionRepository.findByUserId(any(), any())).thenReturn(new PageImpl<>(Collections.emptyList()));

        Budget exceededBudget = new Budget(userId, cat1Id, new BigDecimal("1000.00"), 8, 2026);
        when(budgetRepository.findByUserIdAndYearAndMonth(userId, (short) 2026, (short) 8))
                .thenReturn(List.of(exceededBudget));

        CategorySpendingAggregation sp1 = new CategorySpendingAggregation() {
            @Override
            public UUID getCategoryId() {
                return cat1Id;
            }

            @Override
            public BigDecimal getTotalAmount() {
                return new BigDecimal("1500.00"); // 150% -> EXCEEDED
            }
        };
        when(transactionRepository.sumAmountGroupedByCategory(
                eq(userId), ArgumentMatchers.<Collection<UUID>>any(), eq(TransactionType.EXPENSE), any(LocalDate.class), any(LocalDate.class)
        )).thenReturn(List.of(sp1));

        DashboardResponse response = dashboardService.getDashboard(userId, from, to);

        assertThat(response.budgetOverview().exceededBudgetCount()).isEqualTo(1);
        assertThat(response.insights()).anyMatch(i -> i.type().equals("BUDGET_EXCEEDED") && i.severity() == InsightSeverity.DANGER);
        assertThat(response.insights()).anyMatch(i -> i.type().equals("NEGATIVE_NET_BALANCE") && i.severity() == InsightSeverity.WARNING);
    }

    @Test
    @DisplayName("Category spending limits results to exactly top 5 categories")
    void testDashboardTop5CategorySpendingLimit() {
        LocalDate from = LocalDate.of(2026, 8, 1);
        LocalDate to = LocalDate.of(2026, 8, 31);

        when(transactionRepository.getFinancialSummary(any(), any(), any())).thenReturn(null);
        when(transactionRepository.getMonthlySummary(any(), any(), any())).thenReturn(Collections.emptyList());
        when(budgetRepository.findByUserIdAndYearAndMonth(any(), any(Short.class), any(Short.class))).thenReturn(Collections.emptyList());
        when(transactionRepository.findByUserId(any(), any())).thenReturn(new PageImpl<>(Collections.emptyList()));

        List<CategorySpendingAggregation> sevenCategories = new java.util.ArrayList<>();
        for (int i = 1; i <= 7; i++) {
            final UUID cId = UUID.randomUUID();
            final BigDecimal amt = BigDecimal.valueOf(1000 - i * 100);
            sevenCategories.add(new CategorySpendingAggregation() {
                @Override
                public UUID getCategoryId() {
                    return cId;
                }

                @Override
                public BigDecimal getTotalAmount() {
                    return amt;
                }
            });
        }

        when(transactionRepository.getSpendingByCategory(userId, from, to)).thenReturn(sevenCategories);
        when(categoryRepository.findAllById(ArgumentMatchers.<Iterable<UUID>>any()))
                .thenReturn(Collections.emptyList());

        DashboardResponse response = dashboardService.getDashboard(userId, from, to);

        assertThat(response.categorySpending()).hasSize(5);
    }

    @Test
    @DisplayName("Budget with warning threshold triggers BUDGET_WARNING insight with WARNING severity")
    void testDashboardBudgetWarningInsight() {
        LocalDate from = LocalDate.of(2026, 8, 1);
        LocalDate to = LocalDate.of(2026, 8, 31);

        FinancialSummaryAggregation summaryAgg = new FinancialSummaryAggregation() {
            @Override
            public BigDecimal getTotalIncome() {
                return new BigDecimal("5000.00");
            }

            @Override
            public BigDecimal getTotalExpense() {
                return new BigDecimal("850.00");
            }

            @Override
            public Long getTransactionCount() {
                return 2L;
            }
        };
        when(transactionRepository.getFinancialSummary(userId, from, to)).thenReturn(summaryAgg);
        when(transactionRepository.getMonthlySummary(any(), any(), any())).thenReturn(Collections.emptyList());
        when(transactionRepository.getSpendingByCategory(any(), any(), any())).thenReturn(Collections.emptyList());
        when(transactionRepository.findByUserId(any(), any())).thenReturn(new PageImpl<>(Collections.emptyList()));

        Budget warningBudget = new Budget(userId, cat1Id, new BigDecimal("1000.00"), 8, 2026);
        when(budgetRepository.findByUserIdAndYearAndMonth(userId, (short) 2026, (short) 8))
                .thenReturn(List.of(warningBudget));

        CategorySpendingAggregation sp1 = new CategorySpendingAggregation() {
            @Override
            public UUID getCategoryId() {
                return cat1Id;
            }

            @Override
            public BigDecimal getTotalAmount() {
                return new BigDecimal("850.00"); // 85% -> WARNING
            }
        };
        when(transactionRepository.sumAmountGroupedByCategory(
                eq(userId), ArgumentMatchers.<Collection<UUID>>any(), eq(TransactionType.EXPENSE), any(LocalDate.class), any(LocalDate.class)
        )).thenReturn(List.of(sp1));

        DashboardResponse response = dashboardService.getDashboard(userId, from, to);

        assertThat(response.budgetOverview().warningBudgetCount()).isEqualTo(1);
        assertThat(response.insights()).anyMatch(i -> i.type().equals("BUDGET_WARNING") && i.severity() == InsightSeverity.WARNING);
    }

    @Test
    @DisplayName("Previous month comparison detects DECREASED spending trend and generates EXPENSE_DECREASED insight")
    void testDashboardPreviousMonthComparisonDecreased() {
        LocalDate from = LocalDate.of(2026, 8, 1);
        LocalDate to = LocalDate.of(2026, 8, 31);

        FinancialSummaryAggregation currAgg = new FinancialSummaryAggregation() {
            @Override
            public BigDecimal getTotalIncome() {
                return new BigDecimal("5000.00");
            }

            @Override
            public BigDecimal getTotalExpense() {
                return new BigDecimal("2000.00");
            }

            @Override
            public Long getTransactionCount() {
                return 2L;
            }
        };
        FinancialSummaryAggregation prevAgg = new FinancialSummaryAggregation() {
            @Override
            public BigDecimal getTotalIncome() {
                return new BigDecimal("5000.00");
            }

            @Override
            public BigDecimal getTotalExpense() {
                return new BigDecimal("4000.00");
            }

            @Override
            public Long getTransactionCount() {
                return 4L;
            }
        };

        when(transactionRepository.getFinancialSummary(userId, from, to)).thenReturn(currAgg);
        when(transactionRepository.getFinancialSummary(userId, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31)))
                .thenReturn(prevAgg);
        when(transactionRepository.getMonthlySummary(any(), any(), any())).thenReturn(Collections.emptyList());
        when(transactionRepository.getSpendingByCategory(any(), any(), any())).thenReturn(Collections.emptyList());
        when(budgetRepository.findByUserIdAndYearAndMonth(any(), any(Short.class), any(Short.class))).thenReturn(Collections.emptyList());
        when(transactionRepository.findByUserId(any(), any())).thenReturn(new PageImpl<>(Collections.emptyList()));

        DashboardResponse response = dashboardService.getDashboard(userId, from, to);

        assertThat(response.previousMonthComparison().trend()).isEqualTo(TrendDirection.DECREASED);
        assertThat(response.previousMonthComparison().expenseChangePercentage()).isEqualByComparingTo("50.000");
        assertThat(response.insights()).anyMatch(i -> i.type().equals("EXPENSE_DECREASED") && i.severity() == InsightSeverity.INFO);
    }

    @Test
    @DisplayName("January date range correctly transitions to previous year December for comparison and 6-month trend")
    void testDashboardJanuaryYearTransition() {
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = LocalDate.of(2026, 1, 31);

        FinancialSummaryAggregation janAgg = new FinancialSummaryAggregation() {
            @Override
            public BigDecimal getTotalIncome() {
                return new BigDecimal("5000.00");
            }

            @Override
            public BigDecimal getTotalExpense() {
                return new BigDecimal("2000.00");
            }

            @Override
            public Long getTransactionCount() {
                return 3L;
            }
        };

        when(transactionRepository.getFinancialSummary(userId, from, to)).thenReturn(janAgg);
        when(transactionRepository.getFinancialSummary(userId, LocalDate.of(2025, 12, 1), LocalDate.of(2025, 12, 31)))
                .thenReturn(null);
        when(transactionRepository.getMonthlySummary(eq(userId), eq(LocalDate.of(2025, 8, 1)), eq(LocalDate.of(2026, 1, 31))))
                .thenReturn(Collections.emptyList());
        when(transactionRepository.getSpendingByCategory(any(), any(), any())).thenReturn(Collections.emptyList());
        when(budgetRepository.findByUserIdAndYearAndMonth(userId, (short) 2026, (short) 1)).thenReturn(Collections.emptyList());
        when(transactionRepository.findByUserId(any(), any())).thenReturn(new PageImpl<>(Collections.emptyList()));

        DashboardResponse response = dashboardService.getDashboard(userId, from, to);

        assertThat(response.monthlyTrend()).hasSize(6);
        assertThat(response.monthlyTrend().get(0).year()).isEqualTo(2025);
        assertThat(response.monthlyTrend().get(0).month()).isEqualTo(8);
        assertThat(response.monthlyTrend().get(5).year()).isEqualTo(2026);
        assertThat(response.monthlyTrend().get(5).month()).isEqualTo(1);

        assertThat(response.previousMonthComparison().previousMonthExpense()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.previousMonthComparison().trend()).isEqualTo(TrendDirection.INCREASED);
    }

    @Test
    @DisplayName("Previous month comparison detects UNCHANGED spending when current equals previous")
    void testDashboardPreviousMonthComparisonUnchanged() {
        LocalDate from = LocalDate.of(2026, 8, 1);
        LocalDate to = LocalDate.of(2026, 8, 31);

        FinancialSummaryAggregation currAgg = new FinancialSummaryAggregation() {
            @Override
            public BigDecimal getTotalIncome() {
                return new BigDecimal("5000.00");
            }

            @Override
            public BigDecimal getTotalExpense() {
                return new BigDecimal("3000.00");
            }

            @Override
            public Long getTransactionCount() {
                return 3L;
            }
        };

        when(transactionRepository.getFinancialSummary(userId, from, to)).thenReturn(currAgg);
        when(transactionRepository.getFinancialSummary(userId, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31)))
                .thenReturn(currAgg);
        when(transactionRepository.getMonthlySummary(any(), any(), any())).thenReturn(Collections.emptyList());
        when(transactionRepository.getSpendingByCategory(any(), any(), any())).thenReturn(Collections.emptyList());
        when(budgetRepository.findByUserIdAndYearAndMonth(any(), any(Short.class), any(Short.class))).thenReturn(Collections.emptyList());
        when(transactionRepository.findByUserId(any(), any())).thenReturn(new PageImpl<>(Collections.emptyList()));

        DashboardResponse response = dashboardService.getDashboard(userId, from, to);

        assertThat(response.previousMonthComparison().trend()).isEqualTo(TrendDirection.UNCHANGED);
        assertThat(response.previousMonthComparison().expenseChangeAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.previousMonthComparison().expenseChangePercentage()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Dashboard handles large BigDecimal amounts without precision loss or overflow")
    void testDashboardLargeValues() {
        LocalDate from = LocalDate.of(2026, 8, 1);
        LocalDate to = LocalDate.of(2026, 8, 31);

        BigDecimal bigIncome = new BigDecimal("1000000000.0000");
        BigDecimal bigExpense = new BigDecimal("750000000.0000");

        FinancialSummaryAggregation bigAgg = new FinancialSummaryAggregation() {
            @Override
            public BigDecimal getTotalIncome() {
                return bigIncome;
            }

            @Override
            public BigDecimal getTotalExpense() {
                return bigExpense;
            }

            @Override
            public Long getTransactionCount() {
                return 1000000L;
            }
        };

        when(transactionRepository.getFinancialSummary(userId, from, to)).thenReturn(bigAgg);
        when(transactionRepository.getMonthlySummary(any(), any(), any())).thenReturn(Collections.emptyList());
        when(transactionRepository.getSpendingByCategory(any(), any(), any())).thenReturn(Collections.emptyList());
        when(budgetRepository.findByUserIdAndYearAndMonth(any(), any(Short.class), any(Short.class))).thenReturn(Collections.emptyList());
        when(transactionRepository.findByUserId(any(), any())).thenReturn(new PageImpl<>(Collections.emptyList()));

        DashboardResponse response = dashboardService.getDashboard(userId, from, to);

        assertThat(response.financialSummary().totalIncome()).isEqualByComparingTo(bigIncome);
        assertThat(response.financialSummary().totalExpense()).isEqualByComparingTo(bigExpense);
        assertThat(response.financialSummary().netBalance()).isEqualByComparingTo("250000000.0000");
        assertThat(response.financialSummary().transactionCount()).isEqualTo(1000000L);
    }
}
